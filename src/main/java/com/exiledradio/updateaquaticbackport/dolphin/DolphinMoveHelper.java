package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14.4's dolphin movement controller: turns 10 degrees a tick toward the target, pitches up to 5 a
 * tick (within 85 either way) and swims along its pitch; a little lift in water, which travel's sink
 * balances while the dolphin has no target.
 */
final class DolphinMoveHelper extends EntityMoveHelper {

    private final EntityDolphin dolphin;

    DolphinMoveHelper(EntityDolphin dolphin) {
        super(dolphin);
        this.dolphin = dolphin;
    }

    @Override
    public void onUpdateMoveHelper() {
        if (this.dolphin.isInWater()) {
            this.dolphin.motionY += 0.005D;
        }
        if (this.action == EntityMoveHelper.Action.MOVE_TO && !this.dolphin.getNavigator().noPath()) {
            double dx = this.posX - this.dolphin.posX;
            double dy = this.posY - this.dolphin.posY;
            double dz = this.posZ - this.dolphin.posZ;
            double d = dx * dx + dy * dy + dz * dz;
            if (d < 2.5000003E-7D) {
                this.entity.setMoveForward(0.0F);
            } else {
                float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                this.dolphin.rotationYaw = this.limitAngle(this.dolphin.rotationYaw, yaw, 10.0F);
                this.dolphin.renderYawOffset = this.dolphin.rotationYaw;
                this.dolphin.rotationYawHead = this.dolphin.rotationYaw;
                float speed = (float) (this.speed * this.dolphin.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                if (this.dolphin.isInWater()) {
                    this.dolphin.setAIMoveSpeed(speed * 0.02F);
                    float pitch = -((float) (MathHelper.atan2(dy, MathHelper.sqrt(dx * dx + dz * dz)) * (180D / Math.PI)));
                    pitch = MathHelper.clamp(MathHelper.wrapDegrees(pitch), -85.0F, 85.0F);
                    this.dolphin.rotationPitch = this.limitAngle(this.dolphin.rotationPitch, pitch, 5.0F);
                    float cos = MathHelper.cos(this.dolphin.rotationPitch * 0.017453292F);
                    float sin = MathHelper.sin(this.dolphin.rotationPitch * 0.017453292F);
                    this.dolphin.moveForward = cos * speed;
                    this.dolphin.moveVertical = -sin * speed;
                } else {
                    this.dolphin.setAIMoveSpeed(speed * 0.1F);
                }
            }
        } else {
            this.dolphin.setAIMoveSpeed(0.0F);
            this.dolphin.setMoveStrafing(0.0F);
            this.dolphin.moveVertical = 0.0F;
            this.dolphin.setMoveForward(0.0F);
        }
    }
}
