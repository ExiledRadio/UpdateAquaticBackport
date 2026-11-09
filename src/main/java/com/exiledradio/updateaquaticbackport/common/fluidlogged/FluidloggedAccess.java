package com.exiledradio.updateaquaticbackport.common.fluidlogged;

import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * The few direct calls into Fluidlogged API. Only ever reached when it is installed
 * ({@code Waterlogging.isAvailable()}): no other class names a Fluidlogged API type, so the mod loads without it.
 */
public final class FluidloggedAccess {

    private FluidloggedAccess() {
    }

    /** @return the fluid Fluidlogged API keeps at the position, as a block state, or the block there if none */
    public static IBlockState fluidOrReal(World world, BlockPos pos) {
        return FluidloggedUtils.getFluidOrReal(world, pos);
    }

    /** @return the fluid kept in this block, as a block state; ask only where there is some */
    public static IBlockState fluidState(World world, BlockPos pos, IBlockState here) {
        return FluidloggedUtils.getFluidState(world, pos, here).getState();
    }
}
