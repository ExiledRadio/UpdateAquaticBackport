package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14.4's {@code BreatheAirGoal}: below 140 air, nothing else matters until the dolphin reaches air:
 * it heads for the first breathable block in a 3x3 column up to 8 above (or straight up), and pushes
 * itself along every tick. It cannot be interrupted.
 */
final class AIBreatheAir extends EntityAIBase {

    private final EntityDolphin dolphin;

    AIBreatheAir(EntityDolphin dolphin) {
        this.dolphin = dolphin;
        this.setMutexBits(3);
    }

    @Override
    public boolean shouldExecute() {
        return this.dolphin.getAir() < EntityDolphin.LOW_AIR;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.shouldExecute();
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public void startExecuting() {
        this.navigate();
    }

    @Override
    public void updateTask() {
        this.navigate();
        this.dolphin.moveRelative(this.dolphin.moveStrafing, this.dolphin.moveVertical, this.dolphin.moveForward, 0.02F);
        this.dolphin.move(MoverType.SELF, this.dolphin.motionX, this.dolphin.motionY, this.dolphin.motionZ);
    }

    private void navigate() {
        BlockPos found = null;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(
                new BlockPos(MathHelper.floor(this.dolphin.posX - 1.0D), MathHelper.floor(this.dolphin.posY), MathHelper.floor(this.dolphin.posZ - 1.0D)),
                new BlockPos(MathHelper.floor(this.dolphin.posX + 1.0D), MathHelper.floor(this.dolphin.posY + 8.0D), MathHelper.floor(this.dolphin.posZ + 1.0D)))) {
            if (this.canBreatheAt(pos)) {
                found = pos.toImmutable();
                break;
            }
        }
        if (found == null) {
            found = new BlockPos(this.dolphin.posX, this.dolphin.posY + 8.0D, this.dolphin.posZ);
        }
        this.dolphin.getNavigator().tryMoveToXYZ(found.getX(), found.getY() + 1, found.getZ(), 1.0D);
    }

    /** No liquid there, waterlogged blocks included (except a bubble column of this mod), and nothing solid. */
    private boolean canBreatheAt(BlockPos pos) {
        Block block = this.dolphin.world.getBlockState(pos).getBlock();
        Material material = this.dolphin.world.getBlockState(pos).getMaterial();
        Block column = com.exiledradio.updateaquaticbackport.common.Owners.ourBlock("bubble_column");
        boolean fluid = material.isLiquid() || Waterlogging.hasWater(this.dolphin.world, pos);
        return (!fluid || (column != null && block == column)) && !material.blocksMovement();
    }
}
