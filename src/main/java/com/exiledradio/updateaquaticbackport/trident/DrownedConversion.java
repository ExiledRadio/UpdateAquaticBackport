package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.network.NetworkHandler;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Zombies turn into drowned, and husks into zombies, after 30 seconds with their eyes under water, as
 * 1.13. The conversion then takes 15 seconds, during which the mob shakes, and goes on even if it leaves
 * the water.
 *
 * <p>1.12.2's zombie has no such clock, so it is kept in the mob's own persistent data (it survives saving
 * and reloading) and advanced at the start of the mob's tick. While the clock runs the mob does not run
 * out of air, so it never drowns before it converts. Only plain zombies and husks convert: not zombie
 * villagers, zombie pigmen, drowned or other mods' zombie kinds.
 *
 * <p>The new mob is made as current Java's conversion makes it: same position, baby state, name,
 * equipment with its drop chances, persistence, invulnerability, silence, AI switch and loot pickup, with
 * new random attribute bonuses and full health.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class DrownedConversion {

    public static final String IN_WATER_TIME = UpdateAquaticBackport.MOD_ID + ":InWaterTime";
    public static final String CONVERSION_TIME = UpdateAquaticBackport.MOD_ID + ":DrownedConversionTime";

    /** 1.14.4: 600 ticks under water start it, 300 more finish it. */
    public static final int UNDERWATER_TICKS = 600;
    public static final int CONVERSION_TICKS = 300;

    private DrownedConversion() {
    }

    /** @return true for the mobs that convert: plain zombies and husks */
    public static boolean converts(Entity entity) {
        return entity.getClass() == EntityZombie.class || entity.getClass() == EntityHusk.class;
    }

    public static boolean isConverting(Entity entity) {
        return entity.getEntityData().hasKey(CONVERSION_TIME);
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        Entity entity = event.getEntityLiving();
        if (entity.world.isRemote || !converts(entity) || !TridentContent.enabled() || !ModConfig.trident.zombiesConvert) {
            return;
        }
        if (tick((EntityZombie) entity)) {
            // The mob is gone; skip the rest of its tick.
            event.setCanceled(true);
        }
    }

    /**
     * One tick of the clock (1.14.4's {@code ZombieEntity.tick} part).
     *
     * @return true if the mob converted this tick and is now dead
     */
    public static boolean tick(EntityZombie zombie) {
        if (!zombie.isEntityAlive()) {
            return false;
        }
        NBTTagCompound data = zombie.getEntityData();
        if (data.hasKey(CONVERSION_TIME)) {
            int left = data.getInteger(CONVERSION_TIME) - 1;
            zombie.setAir(300);
            if (left < 0) {
                convert(zombie);
                return true;
            }
            data.setInteger(CONVERSION_TIME, left);
        } else if (zombie.isInsideOfMaterial(Material.WATER)) {
            int time = (data.hasKey(IN_WATER_TIME) ? data.getInteger(IN_WATER_TIME) : -1) + 1;
            zombie.setAir(300);
            if (time >= UNDERWATER_TICKS) {
                data.removeTag(IN_WATER_TIME);
                data.setInteger(CONVERSION_TIME, CONVERSION_TICKS);
                send(zombie, true);
            } else {
                data.setInteger(IN_WATER_TIME, time);
            }
        } else if (data.hasKey(IN_WATER_TIME)) {
            data.removeTag(IN_WATER_TIME);
        }
        return false;
    }

    /** Replaces the mob with its converted form: a drowned for a zombie, a zombie for a husk. */
    public static EntityZombie convert(EntityZombie from) {
        World world = from.world;
        boolean husk = from instanceof EntityHusk;
        EntityZombie to = husk ? new EntityZombie(world) : new EntityDrowned(world);
        copyInto(from, to);
        world.spawnEntity(to);
        from.setDead();
        SoundEvent sound = husk ? TridentSounds.HUSK_CONVERTED_TO_ZOMBIE : TridentSounds.ZOMBIE_CONVERTED_TO_DROWNED;
        world.playSound(null, new BlockPos(from), sound, SoundCategory.HOSTILE, 2.0F,
                (world.rand.nextFloat() - world.rand.nextFloat()) * 0.2F + 1.0F);
        return to;
    }

    /** Current Java's {@code Mob.convertTo} with equipment, plus the zombie's new attribute bonuses. */
    static void copyInto(EntityZombie from, EntityZombie to) {
        to.copyLocationAndAnglesFrom(from);
        to.setChild(from.isChild());
        to.setNoAI(from.isAIDisabled());
        if (from.hasCustomName()) {
            to.setCustomNameTag(from.getCustomNameTag());
            to.setAlwaysRenderNameTag(from.getAlwaysRenderNameTag());
        }
        if (from.isNoDespawnRequired()) {
            to.enablePersistence();
        }
        to.setEntityInvulnerable(from.getIsInvulnerable());
        to.setSilent(from.isSilent());
        to.setCanPickUpLoot(from.canPickUpLoot());

        // Drop chances are protected on the mob; its saved form has them.
        NBTTagCompound saved = new NBTTagCompound();
        from.writeEntityToNBT(saved);
        NBTTagList hands = saved.getTagList("HandDropChances", 5);
        NBTTagList armor = saved.getTagList("ArmorDropChances", 5);
        for (EntityEquipmentSlot slot : EntityEquipmentSlot.values()) {
            ItemStack stack = from.getItemStackFromSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            to.setItemStackToSlot(slot, stack.copy());
            NBTTagList chances = slot.getSlotType() == EntityEquipmentSlot.Type.HAND ? hands : armor;
            if (slot.getIndex() < chances.tagCount()) {
                to.setDropChance(slot, chances.getFloatAt(slot.getIndex()));
            }
            from.setItemStackToSlot(slot, ItemStack.EMPTY);
        }
        EntityDrowned.applyAttributeBonuses(to,
                to.world.getDifficultyForLocation(new BlockPos(to)).getClampedAdditionalDifficulty());
        if (!(to instanceof EntityDrowned) && from.isBreakDoorsTaskSet()) {
            to.setBreakDoorsAItask(true);
        }
        to.setHealth(to.getMaxHealth());
    }

    private static void send(Entity entity, boolean converting) {
        NetworkHandler.channel.sendToAllTracking(new PacketDrowning(entity.getEntityId(), converting), entity);
    }

    /** A player who starts seeing a converting mob is told it is shaking. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (converts(target) && isConverting(target) && event.getEntityPlayer() instanceof EntityPlayerMP
                && TridentContent.connected((EntityPlayerMP) event.getEntityPlayer())) {
            NetworkHandler.channel.sendTo(new PacketDrowning(target.getEntityId(), true), (EntityPlayerMP) event.getEntityPlayer());
        }
    }
}
