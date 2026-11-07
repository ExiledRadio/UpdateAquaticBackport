package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.block.material.Material;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.math.MathHelper;

/**
 * 1.13's fish movement controller: turns toward the next path point (at most 90 degrees a tick), eases
 * the swim speed toward the wanted one, climbs or dives along the path, and floats up a little while
 * its eyes are under water.
 */
final class FishMoveHelper extends EntityMoveHelper {

    private final EntityAbstractFish fish;

    FishMoveHelper(EntityAbstractFish fish) {
        super(fish);
        this.fish = fish;
    }

    @Override
    public void onUpdateMoveHelper() {
        if (this.fish.isInsideOfMaterial(Material.WATER)) {
            this.fish.motionY += 0.005D;
        }
        if (this.action == EntityMoveHelper.Action.MOVE_TO && !this.fish.getNavigator().noPath()) {
            double dx = this.posX - this.fish.posX;
            double dy = this.posY - this.fish.posY;
            double dz = this.posZ - this.fish.posZ;
            double d = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
            // 1.13 divides without a check; at the exact target that is NaN motion.
            dy = d < 1.0E-7D ? 0.0D : dy / d;
            float yaw = (float) (MathHelper.atan2(dz, dx) * (180.0D / Math.PI)) - 90.0F;
            this.fish.rotationYaw = this.limitAngle(this.fish.rotationYaw, yaw, 90.0F);
            this.fish.renderYawOffset = this.fish.rotationYaw;
            float wanted = (float) (this.speed * this.fish.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
            float current = this.fish.getAIMoveSpeed();
            this.fish.setAIMoveSpeed(current + 0.125F * (wanted - current));
            this.fish.motionY += this.fish.getAIMoveSpeed() * dy * 0.1D;
        } else {
            this.fish.setAIMoveSpeed(0.0F);
        }
    }
}
