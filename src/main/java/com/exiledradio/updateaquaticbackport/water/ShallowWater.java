package com.exiledradio.updateaquaticbackport.water;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.common.fluidlogged.FluidloggedAccess;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fluids.IFluidBlock;

/**
 * The jump out of shallow water (1.13: "pressing jump in flowing water at level 1, 2 and 3 now does normal
 * jumps instead of swimming up").
 *
 * <p>1.14.4's {@code LivingEntity.livingTick} jumps normally when the entity stands on the ground and is
 * submerged no deeper than 0.4 blocks, and swims up otherwise. 1.12.2 swims up whenever any water touches
 * the feet, so a player standing in a thin layer of flowing water cannot jump onto the next block.
 * {@link #submergedHeight} is 1.14.4's {@code submergedHeight}; the client applies the jump
 * ({@code client/WaterClient}), where 1.12.2 moves the player.
 */
public final class ShallowWater {

    /** 1.14.4's limit for a normal jump. */
    public static final double JUMP_DEPTH = 0.4D;
    /** 1.14.4's jump cooldown, in ticks. */
    public static final int JUMP_TICKS = 10;
    /** 1.12.2's {@code handleJumpWater}: what vanilla adds on top of a jump while in water. */
    public static final double SWIM_UP = 0.03999999910593033D;

    private ShallowWater() {
    }

    public static boolean on() {
        return WaterContent.enabled() && WaterContent.config().shallowJump;
    }

    /**
     * @return how far above the bottom of the entity's box the highest water surface it touches is (0 when
     * none), as 1.14.4's {@code handleFluidAcceleration} works it out
     */
    public static double submergedHeight(Entity entity) {
        AxisAlignedBB box = entity.getEntityBoundingBox().shrink(0.001D);
        World world = entity.world;
        int x0 = MathHelper.floor(box.minX), x1 = MathHelper.ceil(box.maxX);
        int y0 = MathHelper.floor(box.minY), y1 = MathHelper.ceil(box.maxY);
        int z0 = MathHelper.floor(box.minZ), z1 = MathHelper.ceil(box.maxZ);
        double depth = 0.0D;
        BlockPos.PooledMutableBlockPos pos = BlockPos.PooledMutableBlockPos.retain();
        try {
            for (int x = x0; x < x1; x++) {
                for (int y = y0; y < y1; y++) {
                    for (int z = z0; z < z1; z++) {
                        pos.setPos(x, y, z);
                        IBlockState state = water(world, pos);
                        if (state == null) {
                            continue;
                        }
                        double surface = y + surfaceHeight(world, pos, state);
                        if (surface >= box.minY) {
                            depth = Math.max(depth, surface - box.minY);
                        }
                    }
                }
            }
        } finally {
            pos.release();
        }
        return depth;
    }

    /**
     * @return the water at a position as a water block's state: the block itself if it is water, the water kept
     * in it if it is waterlogged, or null if there is none
     */
    @javax.annotation.Nullable
    static IBlockState water(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == Material.WATER) {
            return state;
        }
        if (!Waterlogging.hasWater(world, pos)) {
            return null;
        }
        return Waterlogging.isAvailable() ? FluidloggedAccess.fluidState(world, pos, state) : null;
    }

    /**
     * @param state the water there, as {@link #water} gives it
     * @return the height of a water block's surface inside its own space, 1.14.4's {@code getActualHeight}
     */
    static double surfaceHeight(World world, BlockPos pos, IBlockState state) {
        if (Waterlogging.hasWater(world, pos.up())) {
            return 1.0D;
        }
        // Vanilla water first: Fluidlogged API makes BlockLiquid an IFluidBlock too, and Forge's filled
        // percentage counts in eighths, where 1.14.4's water height counts in ninths.
        if (state.getBlock() instanceof BlockLiquid && state.getPropertyKeys().contains(BlockLiquid.LEVEL)) {
            int level = state.getValue(BlockLiquid.LEVEL);
            // 1.12.2's level 0 is a source and 1-7 thinner and thinner; 8 and up are falling, as tall as a source.
            return (8 - (level >= 8 ? 0 : level)) / 9.0D;
        }
        if (state.getBlock() instanceof IFluidBlock) {
            return Math.abs(((IFluidBlock) state.getBlock()).getFilledPercentage(world, pos));
        }
        if (state.getPropertyKeys().contains(BlockLiquid.LEVEL)) {
            int level = state.getValue(BlockLiquid.LEVEL);
            return (8 - (level >= 8 ? 0 : level)) / 9.0D;
        }
        return 1.0D;
    }

    /**
     * @return true if 1.13 would jump normally here: on the ground, in water no deeper than
     * {@link #JUMP_DEPTH}, not flying or riding
     */
    public static boolean canJumpNormally(EntityPlayer player) {
        if (!player.onGround || !player.isInWater() || player.isInLava() || player.capabilities.isFlying || player.isRiding()) {
            return false;
        }
        double depth = submergedHeight(player);
        return depth > 0.0D && depth <= JUMP_DEPTH;
    }
}
