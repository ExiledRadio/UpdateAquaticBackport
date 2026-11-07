package com.exiledradio.updateaquaticbackport.turtle;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITargetNonTamed;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.AbstractSkeleton;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityTameable;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * What other mobs and the player get from this stream, added from events, never from inside a goal.
 *
 * <ul>
 *   <li>Zombies of every kind (husks, zombie villagers, zombie pigmen, and any drowned that is a zombie)
 *       seek and stomp turtle eggs (1.14.4: goal priority 4, 3 blocks up or down).</li>
 *   <li>Baby turtles out of the water are hunted by zombies except zombie pigmen (target priority 5), skeletons
 *       of every kind (3), wild wolves (4), wild ocelots (1), and Village &amp; Pillage's stray cats (1) and
 *       foxes (4) when it is installed, all as 1.14.4.</li>
 *   <li>The turtle shell gives its wearer Water Breathing for 10 seconds, refreshed every tick while their
 *       head is out of the water, as today.</li>
 * </ul>
 *
 * Goals go on as the mob joins the world (before it first ticks, so never while its goal list is being
 * run), and only once per mob.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class TurtleEvents {

    /** Water Breathing from the shell: today's 200 ticks, no particles. */
    public static final int SHELL_BREATHING_TICKS = 200;

    private static final ResourceLocation VP_CAT = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "cat");
    private static final ResourceLocation VP_FOX = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "fox");
    private static final ResourceLocation DROWNED = new ResourceLocation(UpdateAquaticBackport.MOD_ID, "drowned");

    private TurtleEvents() {
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        if (!TurtleContent.enabled() || event.getWorld().isRemote || !(event.getEntity() instanceof EntityCreature)) {
            return;
        }
        addGoals((EntityCreature) event.getEntity());
    }

    /** Adds this stream's goals to a mob that should have them; does nothing if it has them already. */
    public static void addGoals(EntityCreature mob) {
        if (huntsBabyTurtles(mob) || seeksEggs(mob)) {
            return;
        }
        ResourceLocation id = EntityList.getKey(mob);
        boolean drowned = DROWNED.equals(id);
        if (mob instanceof EntityZombie || drowned) {
            if (TurtleContent.turtleEgg() != null) {
                mob.tasks.addTask(4, new BreakEggGoal(TurtleContent.turtleEgg(), mob, 1.0D, 3));
            }
            if (!(mob instanceof EntityPigZombie)) {
                mob.targetTasks.addTask(5, new BabyTurtleTarget(mob, true));
            }
        } else if (mob instanceof AbstractSkeleton) {
            mob.targetTasks.addTask(3, new BabyTurtleTarget(mob, true));
        } else if (mob instanceof EntityWolf) {
            mob.targetTasks.addTask(4, new WildBabyTurtleTarget((EntityTameable) mob));
        } else if (mob instanceof EntityOcelot) {
            // A tamed ocelot is 1.12.2's cat; 1.14's tamed cats leave turtles alone.
            mob.targetTasks.addTask(1, new WildBabyTurtleTarget((EntityTameable) mob));
        } else if (VP_CAT.equals(id)) {
            mob.targetTasks.addTask(1, mob instanceof EntityTameable ? new WildBabyTurtleTarget((EntityTameable) mob) : new BabyTurtleTarget(mob, false));
        } else if (VP_FOX.equals(id)) {
            mob.targetTasks.addTask(4, new BabyTurtleTarget(mob, false));
        }
    }

    private static boolean hasGoal(EntityAITasks tasks, Class<? extends EntityAIBase> type) {
        for (EntityAITasks.EntityAITaskEntry entry : tasks.taskEntries) {
            if (type.isInstance(entry.action)) {
                return true;
            }
        }
        return false;
    }

    /** 1.14.4's {@code NearestAttackableTargetGoal<TurtleEntity>(mob, 10, checkSight, false, TARGET_DRY_BABY)}. */
    public static final class BabyTurtleTarget extends EntityAINearestAttackableTarget<EntityTurtle> {
        BabyTurtleTarget(EntityCreature mob, boolean checkSight) {
            super(mob, EntityTurtle.class, 10, checkSight, false, EntityTurtle.TARGET_DRY_BABY);
        }
    }

    /** 1.14.4's {@code NonTamedTargetGoal<TurtleEntity>(mob, false, TARGET_DRY_BABY)}. */
    public static final class WildBabyTurtleTarget extends EntityAITargetNonTamed<EntityTurtle> {
        WildBabyTurtleTarget(EntityTameable mob) {
            super(mob, EntityTurtle.class, false, EntityTurtle.TARGET_DRY_BABY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || TurtleContent.turtleHelmet() == null) {
            return;
        }
        shellTick(event.player);
    }

    /** Today's {@code turtleHelmetTick}: worn on the head with the eyes out of water. */
    public static void shellTick(EntityPlayer player) {
        ItemStack head = player.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!head.isEmpty() && head.getItem() == TurtleContent.turtleHelmet() && !player.isInsideOfMaterial(Material.WATER)) {
            player.addPotionEffect(new PotionEffect(MobEffects.WATER_BREATHING, SHELL_BREATHING_TICKS, 0, false, false));
        }
    }

    /** @return true if the mob has been given a baby turtle target */
    public static boolean huntsBabyTurtles(Entity mob) {
        return mob instanceof EntityCreature && (hasGoal(((EntityCreature) mob).targetTasks, BabyTurtleTarget.class)
                || hasGoal(((EntityCreature) mob).targetTasks, WildBabyTurtleTarget.class));
    }

    /** @return true if the mob has been given the egg-stomping goal */
    public static boolean seeksEggs(Entity mob) {
        return mob instanceof EntityCreature && hasGoal(((EntityCreature) mob).tasks, BreakEggGoal.class);
    }
}
