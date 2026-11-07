package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14.4's {@code DolphinLookController}: the head turns toward what it looks at (aimed a little past it,
 * +20 yaw and +10 pitch, as 1.14.4 does), pitch eases back to level when the dolphin has no path, and
 * the body follows the head 4 degrees a tick once they are more than {@code maxTurn} apart.
 *
 * <p>1.12.2's look helper keeps its state private, so this one keeps its own copy.
 */
final class DolphinLookHelper extends EntityLookHelper {

    private final EntityLiving mob;
    private final int maxTurn;
    private float deltaYaw;
    private float deltaPitch;
    private boolean looking;
    private double x;
    private double y;
    private double z;

    DolphinLookHelper(EntityLiving mob, int maxTurn) {
        super(mob);
        this.mob = mob;
        this.maxTurn = maxTurn;
    }

    @Override
    public void setLookPositionWithEntity(Entity entity, float deltaYaw, float deltaPitch) {
        double eyes = entity instanceof EntityLivingBase
                ? entity.posY + entity.getEyeHeight()
                : (entity.getEntityBoundingBox().minY + entity.getEntityBoundingBox().maxY) / 2.0D;
        this.setLookPosition(entity.posX, eyes, entity.posZ, deltaYaw, deltaPitch);
    }

    @Override
    public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
        super.setLookPosition(x, y, z, deltaYaw, deltaPitch);
        this.x = x;
        this.y = y;
        this.z = z;
        this.deltaYaw = deltaYaw;
        this.deltaPitch = deltaPitch;
        this.looking = true;
    }

    @Override
    public void onUpdateLook() {
        if (this.looking) {
            this.looking = false;
            this.mob.rotationYawHead = rotateTowards(this.mob.rotationYawHead, this.targetYaw() + 20.0F, this.deltaYaw);
            this.mob.rotationPitch = rotateTowards(this.mob.rotationPitch, this.targetPitch() + 10.0F, this.deltaPitch);
        } else {
            if (this.mob.getNavigator().noPath()) {
                this.mob.rotationPitch = rotateTowards(this.mob.rotationPitch, 0.0F, 5.0F);
            }
            this.mob.rotationYawHead = rotateTowards(this.mob.rotationYawHead, this.mob.renderYawOffset, this.deltaYaw);
        }
        float apart = MathHelper.wrapDegrees(this.mob.rotationYawHead - this.mob.renderYawOffset);
        if (apart < -this.maxTurn) {
            this.mob.renderYawOffset -= 4.0F;
        } else if (apart > this.maxTurn) {
            this.mob.renderYawOffset += 4.0F;
        }
    }

    @Override
    public boolean getIsLooking() {
        return this.looking;
    }

    private float targetPitch() {
        double dx = this.x - this.mob.posX;
        double dy = this.y - (this.mob.posY + this.mob.getEyeHeight());
        double dz = this.z - this.mob.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        return (float) -(MathHelper.atan2(dy, horizontal) * (180D / Math.PI));
    }

    private float targetYaw() {
        double dx = this.x - this.mob.posX;
        double dz = this.z - this.mob.posZ;
        return (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
    }

    /** 1.14.4's {@code LookController.clampedRotate}. */
    private static float rotateTowards(float from, float to, float maxDelta) {
        float delta = MathHelper.wrapDegrees(to - from);
        float step = MathHelper.clamp(delta, -maxDelta, maxDelta);
        return from + step;
    }
}
