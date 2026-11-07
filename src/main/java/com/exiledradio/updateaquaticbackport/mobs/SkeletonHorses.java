package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.EntityMountEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Skeleton horses under water (1.13): they keep their rider, and they are fast there.
 *
 * <h2>Keeping the rider</h2>
 * 1.12.2 throws any rider off a living mount once the rider's eyes are under water: the check is in the
 * rider's own tick ({@code EntityLivingBase.onEntityUpdate}) and asks the mount
 * ({@code shouldDismountInWater}), which a vanilla horse cannot answer differently without a patch.
 * Current Java's skeleton horse says no ({@code canBeRiddenInWater}). So that one dismount is cancelled
 * through Forge's mount event: when the mount is a skeleton horse, the rider is under water, and the
 * dismount was called straight from that check. The last part is read from the call stack, only in that
 * already rare case, so a rider still gets off by sneaking, dying, teleporting or anything else.
 *
 * <h2>Speed</h2>
 * Current Java's skeleton horse keeps 96% of its speed each tick in water ({@code getWaterSlowDown}) where
 * other mobs keep 80%. The slow-down is applied at the end of a tick's movement; at the start of the next
 * tick, before it moves again, the horse's horizontal motion is scaled by 0.96 / 0.8 - the same as if the
 * slow-down had been 0.96. This runs where the horse's movement is worked out: on the server, or on the
 * client of the player steering it.
 *
 * <p>Sinking (no swim goal) is {@link UndeadInWater}'s; the underwater sounds are the client's
 * ({@code client/MobsClient}).
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class SkeletonHorses {

    /** 1.14.4's {@code SkeletonHorseEntity.getWaterSlowDown}. */
    public static final float WATER_SLOW_DOWN = 0.96F;
    /** 1.12.2's {@code EntityLivingBase.getWaterSlowDown}, which horses do not change. */
    public static final float VANILLA_WATER_SLOW_DOWN = 0.8F;

    private static final String DISMOUNT_MCP = "dismountRidingEntity";
    private static final String DISMOUNT_SRG = "func_184210_p";
    private static final String UPDATE_MCP = "onEntityUpdate";
    private static final String UPDATE_SRG = "func_70030_z";
    private static final String LIVING_BASE = "net.minecraft.entity.EntityLivingBase";

    private SkeletonHorses() {
    }

    private static boolean on() {
        return MobsContent.enabled() && ModConfig.mobs.skeletonHorsesUnderwater;
    }

    @SubscribeEvent
    public static void onMount(EntityMountEvent event) {
        if (!event.isDismounting() || !(event.getEntityBeingMounted() instanceof EntitySkeletonHorse)
                || event.getWorldObj().isRemote || !on()) {
            return;
        }
        Entity rider = event.getEntityMounting();
        if (rider instanceof EntityLivingBase && rider.isEntityAlive() && event.getEntityBeingMounted().isEntityAlive()
                && rider.isInsideOfMaterial(Material.WATER) && calledFromWaterCheck()) {
            event.setCanceled(true);
        }
    }

    /**
     * @return true if the dismount in progress was called from {@code EntityLivingBase.onEntityUpdate},
     * where 1.12.2's only call is the under-water check
     */
    static boolean calledFromWaterCheck() {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        boolean inDismount = false;
        for (StackTraceElement frame : stack) {
            String method = frame.getMethodName();
            if (DISMOUNT_MCP.equals(method) || DISMOUNT_SRG.equals(method)) {
                inDismount = true;
            } else if (inDismount) {
                return LIVING_BASE.equals(frame.getClassName()) && (UPDATE_MCP.equals(method) || UPDATE_SRG.equals(method));
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        if (!(event.getEntityLiving() instanceof EntitySkeletonHorse) || !on()) {
            return;
        }
        EntitySkeletonHorse horse = (EntitySkeletonHorse) event.getEntityLiving();
        // isInWater is still last tick's here, the value last tick's movement used.
        if (horse.isInWater() && movesHere(horse)) {
            double scale = (double) (WATER_SLOW_DOWN / VANILLA_WATER_SLOW_DOWN);
            horse.motionX *= scale;
            horse.motionZ *= scale;
        }
    }

    /** @return true if this side works out the horse's movement (1.12.2's {@code AbstractHorse.travel}) */
    static boolean movesHere(EntitySkeletonHorse horse) {
        boolean playerSteers = horse.isHorseSaddled() && horse.getControllingPassenger() instanceof EntityPlayer;
        return horse.world.isRemote ? playerSteers && horse.canPassengerSteer() : !playerSteers && !horse.isAIDisabled();
    }
}
