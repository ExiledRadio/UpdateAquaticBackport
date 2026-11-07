package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/** 1.14.4's {@code RandomSwimmingGoal}: wander to a random spot within 10 (7 up or down) that is water. */
final class AIRandomSwim extends EntityAIWander {

    AIRandomSwim(EntityCreature creature, double speed, int chance) {
        super(creature, speed, chance);
    }

    @Override
    @Nullable
    protected Vec3d getPosition() {
        Vec3d target = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
        for (int i = 0; target != null && !DolphinNodeProcessor.isSwimmable(this.entity.world.getBlockState(new BlockPos(target))) && i++ < 10;
             target = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7)) {
            // try again
        }
        return target;
    }
}
