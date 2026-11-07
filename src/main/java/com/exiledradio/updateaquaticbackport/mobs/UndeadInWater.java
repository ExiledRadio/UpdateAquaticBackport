package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Undead mobs and water, and baby zombies in the sun, as 1.13 and current Java have them.
 *
 * <h2>Sinking</h2>
 * 1.12.2's zombies, skeletons and horses keep afloat with a swim goal that jumps them up whenever they are
 * in water; current Java's zombies (with husks, zombie villagers and zombie pigmen), skeletons (with strays
 * and wither skeletons) and skeleton and zombie horses have none, so they sink and walk along the bottom.
 * When such a mob joins a world its swim goal is taken out and its ground navigator stops planning paths
 * along the water's surface. Other mods' subclasses of these mobs are included; other undead mobs and the
 * drowned (which has its own swimming) are not touched.
 *
 * <h2>Breathing</h2>
 * Current Java's undead never run out of air. 1.12.2's do, which a sinking zombie would quickly show: every
 * undead mob (by creature attribute, so other mods' too) has its air held full at the start of its tick,
 * before vanilla takes a breath away.
 *
 * <h2>Baby zombies</h2>
 * 1.12.2 leaves baby zombies out of burning in daylight; 1.13 burns them like adults. A baby that would
 * burn as an adult (its kind burns at all, so not a husk) gets the adult's exact check at the start of its
 * tick: bright sky above its eyes, chance by brightness, the helmet worn down instead if it has one.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class UndeadInWater {

    /** {@code EntityZombie.shouldBurnInDay}, protected: false for husks and any kind that never burns. */
    private static final Method SHOULD_BURN_IN_DAY =
            ReflectionHelper.findMethod(EntityZombie.class, "shouldBurnInDay", "func_190730_o");

    /** Air an entity starts with in 1.12.2. */
    private static final int FULL_AIR = 300;

    private UndeadInWater() {
    }

    /** @return true for the mobs that sink: zombies, skeletons, skeleton and zombie horses, and their subclasses */
    public static boolean sinks(Entity entity) {
        return entity instanceof EntityZombie || entity instanceof AbstractSkeleton
                || entity instanceof EntitySkeletonHorse || entity instanceof EntityZombieHorse;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !MobsContent.enabled() || !ModConfig.mobs.undeadSink || !sinks(entity)) {
            return;
        }
        stopSwimming((EntityLiving) entity);
    }

    /**
     * Takes every swim goal out of a mob and, if there was one, stops its ground navigator from swimming.
     * Called only when the mob joins a world, never from inside a goal.
     *
     * @return the number of swim goals removed
     */
    public static int stopSwimming(EntityLiving mob) {
        List<EntityAIBase> swims = new ArrayList<EntityAIBase>();
        for (EntityAITasks.EntityAITaskEntry entry : mob.tasks.taskEntries) {
            if (entry.action instanceof EntityAISwimming) {
                swims.add(entry.action);
            }
        }
        for (EntityAIBase swim : swims) {
            // removeTask also stops it and takes it out of the running set if it was running.
            mob.tasks.removeTask(swim);
        }
        if (!swims.isEmpty() && mob.getNavigator() instanceof PathNavigateGround) {
            ((PathNavigateGround) mob.getNavigator()).setCanSwim(false);
        }
        return swims.size();
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity.world.isRemote || !MobsContent.enabled()) {
            return;
        }
        if (ModConfig.mobs.undeadSink && entity.getCreatureAttribute() == EnumCreatureAttribute.UNDEAD
                && entity.getAir() < FULL_AIR && entity.isInsideOfMaterial(Material.WATER)) {
            entity.setAir(FULL_AIR);
        }
        if (ModConfig.mobs.babyZombiesBurn && entity instanceof EntityZombie && entity.isChild()) {
            burnInDaylight((EntityZombie) entity);
        }
    }

    /** An undead mob never takes drowning damage, whatever set its air to nothing. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource() == DamageSource.DROWN && MobsContent.enabled() && ModConfig.mobs.undeadSink
                && event.getEntityLiving().getCreatureAttribute() == EnumCreatureAttribute.UNDEAD) {
            event.setCanceled(true);
        }
    }

    /** 1.12.2's adult zombie daylight check ({@code EntityZombie.onLivingUpdate}), for a baby. */
    static void burnInDaylight(EntityZombie zombie) {
        if (!zombie.world.isDaytime() || !shouldBurnInDay(zombie)) {
            return;
        }
        float brightness = zombie.getBrightness();
        if (brightness > 0.5F && zombie.getRNG().nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F
                && zombie.world.canSeeSky(new BlockPos(zombie.posX, zombie.posY + (double) zombie.getEyeHeight(), zombie.posZ))) {
            ItemStack helmet = zombie.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            if (helmet.isEmpty()) {
                zombie.setFire(8);
                return;
            }
            if (helmet.isItemStackDamageable()) {
                helmet.setItemDamage(helmet.getItemDamage() + zombie.getRNG().nextInt(2));
                if (helmet.getItemDamage() >= helmet.getMaxDamage()) {
                    zombie.renderBrokenItemStack(helmet);
                    zombie.setItemStackToSlot(EntityEquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            }
        }
    }

    static boolean shouldBurnInDay(EntityZombie zombie) {
        try {
            return (Boolean) SHOULD_BURN_IN_DAY.invoke(zombie);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
