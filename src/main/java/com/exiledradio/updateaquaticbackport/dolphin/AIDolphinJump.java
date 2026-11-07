package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14.4's {@code DolphinJumpGoal}: one try in {@code chance}, if the water ahead (0, 1 and 4-7 blocks
 * along the dolphin's facing) is open with two blocks of air above each, the dolphin leaps out: 0.6
 * forward and 0.7 up. In the air its pitch follows its motion; the jump sound plays as it leaves the water.
 * It cannot be interrupted until it is level in water again or on the ground.
 */
final class AIDolphinJump extends EntityAIBase {

    private static final int[] AHEAD = {0, 1, 4, 5, 6, 7};

    private final EntityDolphin dolphin;
    private final int chance;
    private boolean inWater;

    AIDolphinJump(EntityDolphin dolphin, int chance) {
        this.dolphin = dolphin;
        this.chance = chance;
        this.setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (this.dolphin.getRNG().nextInt(this.chance) != 0) {
            return false;
        }
        EnumFacing facing = this.dolphin.getAdjustedHorizontalFacing();
        int dx = facing.getXOffset();
        int dz = facing.getZOffset();
        BlockPos at = new BlockPos(this.dolphin);
        for (int k : AHEAD) {
            if (!this.openWater(at, dx, dz, k) || !this.airAbove(at, dx, dz, k)) {
                return false;
            }
        }
        return true;
    }

    private boolean openWater(BlockPos at, int dx, int dz, int k) {
        BlockPos pos = at.add(dx * k, 0, dz * k);
        return Waterlogging.hasWater(this.dolphin.world, pos) && !this.dolphin.world.getBlockState(pos).getMaterial().blocksMovement();
    }

    private boolean airAbove(BlockPos at, int dx, int dz, int k) {
        return this.dolphin.world.isAirBlock(at.add(dx * k, 1, dz * k)) && this.dolphin.world.isAirBlock(at.add(dx * k, 2, dz * k));
    }

    @Override
    public boolean shouldContinueExecuting() {
        double vy = this.dolphin.motionY;
        boolean settled = vy * vy < 0.03F && this.dolphin.rotationPitch != 0.0F && Math.abs(this.dolphin.rotationPitch) < 10.0F
                && this.dolphin.isInWater();
        return !settled && !this.dolphin.onGround;
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void startExecuting() {
        EnumFacing facing = this.dolphin.getAdjustedHorizontalFacing();
        this.dolphin.motionX += facing.getXOffset() * 0.6D;
        this.dolphin.motionY += 0.7D;
        this.dolphin.motionZ += facing.getZOffset() * 0.6D;
        this.dolphin.getNavigator().clearPath();
        // Vanilla never clears this, so only a dolphin's first leap makes a sound; every leap does here.
        this.inWater = false;
    }

    @Override
    public void resetTask() {
        this.dolphin.rotationPitch = 0.0F;
    }

    @Override
    public void updateTask() {
        boolean was = this.inWater;
        if (!was) {
            this.inWater = Waterlogging.hasWater(this.dolphin.world, new BlockPos(this.dolphin));
        }
        if (this.inWater && !was) {
            this.dolphin.playSound(DolphinContent.JUMP, 1.0F, 1.0F);
        }
        double vx = this.dolphin.motionX;
        double vy = this.dolphin.motionY;
        double vz = this.dolphin.motionZ;
        if (vy * vy < 0.03F && this.dolphin.rotationPitch != 0.0F) {
            this.dolphin.rotationPitch = rotlerp(this.dolphin.rotationPitch, 0.0F, 0.2F);
        } else {
            double horizontal = Math.sqrt(vx * vx + vz * vz);
            double length = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (length > 0.0D) {
                this.dolphin.rotationPitch = (float) (Math.signum(-vy) * Math.acos(horizontal / length) * (180D / Math.PI));
            }
        }
    }

    /** 1.14.4's {@code JumpGoal.updateRotation}. */
    private static float rotlerp(float from, float to, float step) {
        float delta = MathHelper.wrapDegrees(to - from);
        return from + step * delta;
    }
}
