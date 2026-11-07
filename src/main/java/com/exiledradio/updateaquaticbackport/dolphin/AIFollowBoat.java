package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;

/**
 * 1.14.4's {@code FollowBoatGoal}: a boat within 5 blocks being steered draws the dolphin. It swims to
 * just behind and below the driver, then, within 4 blocks, races 10 blocks ahead in the boat's direction;
 * more than 12 behind, it closes in again. It pushes itself along each tick (0.135, or 0.18 racing a
 * moving boat). It shares no control with other goals and lets any of them take over.
 */
final class AIFollowBoat extends EntityAIBase {

    private enum Phase { GO_TO_BOAT, GO_IN_BOAT_DIRECTION }

    private final EntityCreature mob;
    @Nullable
    private EntityLivingBase driver;
    private Phase phase;
    private int timer;

    AIFollowBoat(EntityCreature mob) {
        this.mob = mob;
    }

    private static boolean steering(@Nullable EntityLivingBase driver) {
        return driver != null && (MathHelper.abs(driver.moveStrafing) > 0.0F || MathHelper.abs(driver.moveForward) > 0.0F);
    }

    @Override
    public boolean shouldExecute() {
        boolean any = false;
        for (EntityBoat boat : this.mob.world.getEntitiesWithinAABB(EntityBoat.class, this.mob.getEntityBoundingBox().grow(5.0D))) {
            Entity passenger = boat.getControllingPassenger();
            if (passenger instanceof EntityLivingBase && steering((EntityLivingBase) passenger)) {
                any = true;
                break;
            }
        }
        return steering(this.driver) || any;
    }

    @Override
    public boolean isInterruptible() {
        return true;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.driver != null && this.driver.isRiding() && steering(this.driver);
    }

    @Override
    public void startExecuting() {
        for (EntityBoat boat : this.mob.world.getEntitiesWithinAABB(EntityBoat.class, this.mob.getEntityBoundingBox().grow(5.0D))) {
            Entity passenger = boat.getControllingPassenger();
            if (passenger instanceof EntityLivingBase) {
                this.driver = (EntityLivingBase) passenger;
                break;
            }
        }
        this.timer = 0;
        this.phase = Phase.GO_TO_BOAT;
    }

    @Override
    public void resetTask() {
        this.driver = null;
    }

    @Override
    public void updateTask() {
        if (this.driver == null) {
            return;
        }
        boolean moving = steering(this.driver);
        float push = this.phase == Phase.GO_IN_BOAT_DIRECTION ? (moving ? 0.17999999F : 0.0F) : 0.135F;
        this.mob.moveRelative(this.mob.moveStrafing, this.mob.moveVertical, this.mob.moveForward, push);
        this.mob.move(MoverType.SELF, this.mob.motionX, this.mob.motionY, this.mob.motionZ);
        if (--this.timer > 0) {
            return;
        }
        this.timer = 10;
        if (this.phase == Phase.GO_TO_BOAT) {
            BlockPos behind = new BlockPos(this.driver).offset(this.driver.getHorizontalFacing().getOpposite()).add(0, -1, 0);
            this.mob.getNavigator().tryMoveToXYZ(behind.getX(), behind.getY(), behind.getZ(), 1.0D);
            if (this.mob.getDistance(this.driver) < 4.0F) {
                this.timer = 0;
                this.phase = Phase.GO_IN_BOAT_DIRECTION;
            }
        } else {
            EnumFacing facing = this.driver.getAdjustedHorizontalFacing();
            BlockPos ahead = new BlockPos(this.driver).offset(facing, 10);
            this.mob.getNavigator().tryMoveToXYZ(ahead.getX(), ahead.getY() - 1, ahead.getZ(), 1.0D);
            if (this.mob.getDistance(this.driver) > 12.0F) {
                this.timer = 0;
                this.phase = Phase.GO_TO_BOAT;
            }
        }
    }

    /** For the self-test: who the dolphin follows, or null. */
    @Nullable
    EntityLivingBase driver() {
        return this.driver;
    }
}
