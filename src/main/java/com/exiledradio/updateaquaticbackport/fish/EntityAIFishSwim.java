package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.block.material.Material;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;

/**
 * 1.13's {@code RandomSwimmingGoal} for fish: now and then (1 in 40 checks) swim to a random point within
 * 10 blocks across and 7 up or down that is water, trying up to 11 points. A schooling fish with a
 * leader does not wander.
 */
final class EntityAIFishSwim extends EntityAIWander {

    private final EntityAbstractFish fish;

    EntityAIFishSwim(EntityAbstractFish fish) {
        super(fish, 1.0D, 40);
        this.fish = fish;
    }

    @Override
    public boolean shouldExecute() {
        return this.fish.canRandomSwim() && super.shouldExecute();
    }

    @Override
    @Nullable
    protected Vec3d getPosition() {
        Vec3d target = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
        for (int i = 0; target != null && this.entity.world.getBlockState(new BlockPos(target)).getMaterial() != Material.WATER && i++ < 10; ) {
            target = RandomPositionGenerator.findRandomTarget(this.entity, 10, 7);
        }
        return target;
    }
}
