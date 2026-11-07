package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.PotionEffect;

import javax.annotation.Nullable;

/**
 * 1.14.4's {@code SwimWithPlayerGoal}: the nearest player within 10 blocks that the dolphin can see and
 * who is swimming gets Dolphin's Grace for 5 seconds; the dolphin swims alongside (stopping within 2.5
 * blocks), and while the player keeps swimming within 16 blocks the effect is renewed one tick in six.
 *
 * <p>"Swimming" is 1.13's sprint-swimming: sprinting in water ({@link #isSwimming}). Invisibility and
 * sneaking shorten the 10 blocks as they shorten a mob's sight of a player (1.14.4's visibility rule).
 */
final class AISwimWithPlayer extends EntityAIBase {

    /** 1.14.4's: 100 ticks of Dolphin's Grace. */
    static final int GRACE_TICKS = 100;
    static final double START_RANGE = 10.0D;
    static final double KEEP_RANGE_SQ = 256.0D;

    private final EntityDolphin dolphin;
    private final double speed;
    @Nullable
    private EntityPlayer player;

    AISwimWithPlayer(EntityDolphin dolphin, double speed) {
        this.dolphin = dolphin;
        this.speed = speed;
        this.setMutexBits(3);
    }

    /**
     * 1.13's swimming is sprinting in water. When this mod gains a swimming pose of its own, this is the
     * one place to ask it instead.
     */
    static boolean isSwimming(EntityPlayer player) {
        return player.isInWater() && player.isSprinting() && !player.isSpectator();
    }

    @Override
    public boolean shouldExecute() {
        this.player = this.closestVisiblePlayer();
        return this.player != null && isSwimming(this.player);
    }

    @Nullable
    private EntityPlayer closestVisiblePlayer() {
        EntityPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (EntityPlayer candidate : this.dolphin.world.playerEntities) {
            if (candidate.isSpectator() || !candidate.isEntityAlive()) {
                continue;
            }
            double range = START_RANGE * visibility(candidate);
            double d = this.dolphin.getDistanceSq(candidate);
            if (d > range * range || d >= bestDistance || !this.dolphin.getEntitySenses().canSee(candidate)) {
                continue;
            }
            best = candidate;
            bestDistance = d;
        }
        return best;
    }

    /** 1.14.4's {@code getVisibilityMultiplier} for a player: sneaking 0.8; invisible 0.7 x armor cover (at least 0.1). */
    static double visibility(EntityPlayer player) {
        double v = 1.0D;
        if (player.isSneaking()) {
            v *= 0.8D;
        }
        if (player.isInvisible()) {
            float cover = player.getArmorVisibility();
            if (cover < 0.1F) {
                cover = 0.1F;
            }
            v *= 0.7D * cover;
        }
        return v;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.player != null && isSwimming(this.player) && this.dolphin.getDistanceSq(this.player) < KEEP_RANGE_SQ;
    }

    @Override
    public void startExecuting() {
        this.player.addPotionEffect(new PotionEffect(DolphinContent.dolphinsGrace(), GRACE_TICKS));
    }

    @Override
    public void resetTask() {
        this.player = null;
        this.dolphin.getNavigator().clearPath();
    }

    @Override
    public void updateTask() {
        this.dolphin.getLookHelper().setLookPositionWithEntity(this.player,
                this.dolphin.getHorizontalFaceSpeed() + 20, this.dolphin.getVerticalFaceSpeed());
        if (this.dolphin.getDistanceSq(this.player) < 6.25D) {
            this.dolphin.getNavigator().clearPath();
        } else {
            this.dolphin.getNavigator().tryMoveToEntityLiving(this.player, this.speed);
        }
        if (isSwimming(this.player) && this.player.world.rand.nextInt(6) == 0) {
            this.player.addPotionEffect(new PotionEffect(DolphinContent.dolphinsGrace(), GRACE_TICKS));
        }
    }
}
