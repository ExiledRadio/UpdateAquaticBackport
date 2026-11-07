package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

/** 1.14.4's {@code FindWaterGoal}: on the ground out of water, head for any water within 2 blocks. */
final class AIFindWater extends EntityAIBase {

    private final EntityDolphin dolphin;

    AIFindWater(EntityDolphin dolphin) {
        this.dolphin = dolphin;
    }

    @Override
    public boolean shouldExecute() {
        return this.dolphin.onGround && !Waterlogging.hasWater(this.dolphin.world, new BlockPos(this.dolphin));
    }

    @Override
    public void startExecuting() {
        BlockPos found = null;
        for (BlockPos pos : BlockPos.getAllInBoxMutable(
                new BlockPos(MathHelper.floor(this.dolphin.posX - 2.0D), MathHelper.floor(this.dolphin.posY - 2.0D), MathHelper.floor(this.dolphin.posZ - 2.0D)),
                new BlockPos(MathHelper.floor(this.dolphin.posX + 2.0D), MathHelper.floor(this.dolphin.posY), MathHelper.floor(this.dolphin.posZ + 2.0D)))) {
            if (Waterlogging.hasWater(this.dolphin.world, pos)) {
                found = pos.toImmutable();
                break;
            }
        }
        if (found != null) {
            this.dolphin.getMoveHelper().setMoveTo(found.getX(), found.getY(), found.getZ(), 1.0D);
        }
    }
}
