package com.exiledradio.updateaquaticbackport.blocks;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.google.common.base.Predicate;

import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Endermen are calmed by a carved pumpkin on a player's head, not a plain one (1.13).
 *
 * <h2>Why the task is replaced</h2>
 * 1.12.2's stare check, {@code EntityEnderman.shouldAttackPlayer}, is private and names
 * {@code Blocks.PUMPKIN}, and the only thing that calls it is the enderman's own private target task,
 * {@code EntityEnderman.AIFindPlayer}. So that task is swapped for {@link FindPlayer}, a port of 1.14.4's
 * {@code EndermanEntity.FindPlayerGoal} with 1.14.4's {@code shouldAttackPlayer}, which asks for the
 * carved pumpkin. The rest of the enderman (its other tasks, teleporting, screaming, block carrying) is
 * vanilla's; teleporting goes through its own protected methods, so a subclass that overrides them is
 * still asked.
 *
 * <h2>When</h2>
 * On {@link EntityJoinWorldEvent}, server side, for any {@link EntityEnderman} (another mod's subclass
 * included) whose target tasks still hold vanilla's task: an entity that is joining a world is not
 * running its tasks, so its list can be edited there. The list is rebuilt in its own order with the one
 * task swapped, so the tasks run in the order vanilla gave them. A task another mod put there instead is
 * left alone.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class EndermanStare {

    @Nullable
    private static Class<?> vanillaTask;
    private static Method teleportRandomly;
    private static Method teleportToEntity;
    private static boolean broken;

    private EndermanStare() {
    }

    @SubscribeEvent
    public static void join(EntityJoinWorldEvent event) {
        if (broken || event.getWorld().isRemote || !(event.getEntity() instanceof EntityEnderman)
                || BlocksContent.carvedPumpkin() == null || !lookUp()) {
            return;
        }
        EntityEnderman enderman = (EntityEnderman) event.getEntity();
        EntityAITasks tasks = enderman.targetTasks;
        List<EntityAITasks.EntityAITaskEntry> entries = new ArrayList<EntityAITasks.EntityAITaskEntry>(tasks.taskEntries);
        boolean found = false;
        for (EntityAITasks.EntityAITaskEntry entry : entries) {
            found |= entry.action.getClass() == vanillaTask;
        }
        if (!found) {
            return;
        }
        for (EntityAITasks.EntityAITaskEntry entry : entries) {
            tasks.removeTask(entry.action);
        }
        for (EntityAITasks.EntityAITaskEntry entry : entries) {
            EntityAIBase action = entry.action.getClass() == vanillaTask ? new FindPlayer(enderman) : entry.action;
            tasks.addTask(entry.priority, action);
        }
    }

    private static boolean lookUp() {
        if (vanillaTask != null) {
            return true;
        }
        try {
            vanillaTask = Class.forName("net.minecraft.entity.monster.EntityEnderman$AIFindPlayer", false,
                    EntityEnderman.class.getClassLoader());
            teleportRandomly = ReflectionHelper.findMethod(EntityEnderman.class, "teleportRandomly", "func_70820_n");
            teleportToEntity = ReflectionHelper.findMethod(EntityEnderman.class, "teleportToEntity", "func_70816_c", Entity.class);
            return true;
        } catch (Throwable t) {
            broken = true;
            vanillaTask = null;
            UpdateAquaticBackport.LOGGER.error("Could not find the enderman's stare task; endermen are still calmed by a plain pumpkin.", t);
            return false;
        }
    }

    /** @return the carved pumpkin's item, or null without this stream */
    @Nullable
    private static Item mask() {
        BlockCarvedPumpkin carved = BlocksContent.carvedPumpkin();
        return carved == null ? null : Item.getItemFromBlock(carved);
    }

    /**
     * 1.14.4's {@code EndermanEntity.shouldAttackPlayer}: not with a carved pumpkin on the head; otherwise
     * only if the player looks straight at the enderman's eyes and can see it.
     */
    public static boolean shouldAttackPlayer(EntityEnderman enderman, EntityPlayer player) {
        ItemStack head = player.inventory.armorInventory.get(3);
        if (head.getItem() == mask()) {
            return false;
        }
        Vec3d look = player.getLook(1.0F).normalize();
        Vec3d toEyes = new Vec3d(enderman.posX - player.posX,
                enderman.getEntityBoundingBox().minY + (double) enderman.getEyeHeight() - (player.posY + (double) player.getEyeHeight()),
                enderman.posZ - player.posZ);
        double distance = toEyes.length();
        toEyes = toEyes.normalize();
        double dot = look.dotProduct(toEyes);
        return dot > 1.0D - 0.025D / distance && player.canEntityBeSeen(enderman);
    }

    private static boolean teleport(Method method, EntityEnderman enderman, Object... args) {
        try {
            return (Boolean) method.invoke(enderman, args);
        } catch (Exception e) {
            UpdateAquaticBackport.LOGGER.error("Enderman teleport failed", e);
            return false;
        }
    }

    /**
     * 1.14.4's {@code EndermanEntity.FindPlayerGoal}, on 1.12.2's target task. Beside the carved pumpkin it
     * keeps 1.14.4's two other changes to 1.12.2's task: an enderman riding something does not teleport
     * at its target, and a target it has locked onto is kept only while it is alive, not a spectator, not
     * invulnerable and not on its team (else the ordinary target rules decide).
     */
    static final class FindPlayer extends EntityAINearestAttackableTarget<EntityPlayer> {

        private final EntityEnderman enderman;
        /** The player staring at it, before the five ticks it waits to turn on them. */
        @Nullable
        private EntityPlayer player;
        private int aggroTime;
        private int teleportTime;

        FindPlayer(EntityEnderman enderman) {
            super(enderman, EntityPlayer.class, false);
            this.enderman = enderman;
        }

        @Override
        public boolean shouldExecute() {
            double range = this.getTargetDistance();
            this.player = this.enderman.world.getNearestAttackablePlayer(this.enderman.posX, this.enderman.posY, this.enderman.posZ,
                    range, range, null, new Predicate<EntityPlayer>() {
                        @Override
                        public boolean apply(@Nullable EntityPlayer candidate) {
                            return candidate != null && shouldAttackPlayer(FindPlayer.this.enderman, candidate);
                        }
                    });
            return this.player != null;
        }

        @Override
        public void startExecuting() {
            this.aggroTime = 5;
            this.teleportTime = 0;
        }

        @Override
        public void resetTask() {
            this.player = null;
            super.resetTask();
        }

        @Override
        public boolean shouldContinueExecuting() {
            if (this.player != null) {
                if (!shouldAttackPlayer(this.enderman, this.player)) {
                    return false;
                }
                this.enderman.faceEntity(this.player, 10.0F, 10.0F);
                return true;
            }
            EntityPlayer target = this.targetEntity;
            return target != null && target.isEntityAlive() && !target.isSpectator() && !target.getIsInvulnerable()
                    && !this.enderman.isOnSameTeam(target) || super.shouldContinueExecuting();
        }

        @Override
        public void updateTask() {
            if (this.player != null) {
                if (--this.aggroTime <= 0) {
                    this.targetEntity = this.player;
                    this.player = null;
                    super.startExecuting();
                }
                return;
            }
            EntityPlayer target = this.targetEntity;
            if (target != null && !this.enderman.isRiding()) {
                if (shouldAttackPlayer(this.enderman, target)) {
                    if (target.getDistanceSq(this.enderman) < 16.0D) {
                        teleport(teleportRandomly, this.enderman);
                    }
                    this.teleportTime = 0;
                } else if (target.getDistanceSq(this.enderman) > 256.0D && this.teleportTime++ >= 30
                        && teleport(teleportToEntity, this.enderman, target)) {
                    this.teleportTime = 0;
                }
            }
            super.updateTask();
        }
    }
}
