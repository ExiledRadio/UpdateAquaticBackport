package com.exiledradio.updateaquaticbackport.common;

import net.minecraft.block.state.IBlockState;

import javax.annotation.Nullable;

/**
 * A block that is the water in its space when Fluidlogged API is not installed ({@link PseudoWater}): kelp,
 * seagrass, coral, coral fans, bubble columns ({@link BlockUnderwater}) and the conduit.
 */
public interface PseudoWaterBlock {

    /** @return true if this block was made as the water in its space; false when Fluidlogged API holds its water */
    boolean isPseudoWater();

    /** @return true if this state is water (only meaningful when {@link #isPseudoWater()}) */
    default boolean isWater(IBlockState state) {
        return PseudoWater.isWater(state);
    }

    /** @return this state without its water, as an empty bucket leaves it, or null if a bucket cannot take it */
    @Nullable
    default IBlockState drained(IBlockState state) {
        return state.getPropertyKeys().contains(PseudoWater.WATERLOGGED) && state.getValue(PseudoWater.WATERLOGGED)
                ? state.withProperty(PseudoWater.WATERLOGGED, false) : null;
    }

    /** @return this state filled with water, as a water bucket fills it, or null if it cannot be filled */
    @Nullable
    default IBlockState filled(IBlockState state) {
        return state.getPropertyKeys().contains(PseudoWater.WATERLOGGED) && !state.getValue(PseudoWater.WATERLOGGED)
                ? state.withProperty(PseudoWater.WATERLOGGED, true) : null;
    }
}
