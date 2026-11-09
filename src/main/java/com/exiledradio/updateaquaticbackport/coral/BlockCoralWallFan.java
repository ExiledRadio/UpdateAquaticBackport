package com.exiledradio.updateaquaticbackport.coral;

import com.exiledradio.updateaquaticbackport.common.PseudoWater;

import net.minecraft.block.SoundType;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * A coral fan on the side of a block, from 1.14.4's {@code DeadCoralWallFanBlock} and
 * {@code CoralWallFanBlock}. {@link #FACING} points away from the block it hangs on.
 *
 * <p>It has no item of its own: placing a fan item against a side places this ({@link ItemCoralFan}), and it
 * drops (with Silk Touch) and picks as the floor fan.
 */
public class BlockCoralWallFan extends BlockCoral {

    public static final PropertyDirection FACING = PropertyDirection.create("facing", EnumFacing.Plane.HORIZONTAL);

    private static final AxisAlignedBB NORTH = new AxisAlignedBB(0, 4 / 16.0, 5 / 16.0, 1, 12 / 16.0, 1);
    private static final AxisAlignedBB SOUTH = new AxisAlignedBB(0, 4 / 16.0, 0, 1, 12 / 16.0, 11 / 16.0);
    private static final AxisAlignedBB WEST = new AxisAlignedBB(5 / 16.0, 4 / 16.0, 0, 1, 12 / 16.0, 1);
    private static final AxisAlignedBB EAST = new AxisAlignedBB(0, 4 / 16.0, 0, 11 / 16.0, 12 / 16.0, 1);

    public BlockCoralWallFan(CoralType type, boolean living, SoundType sound) {
        super(type, true, living, sound);
        this.setDefaultState(this.getDefaultState().withProperty(FACING, EnumFacing.NORTH));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return this.container(FACING);
    }

    /**
     * Meta 0-3: the facing. Bit 4 is the {@code waterlogged} flag without Fluidlogged API; with it, the flag is
     * ignored (see {@code common.LegacyWater}).
     */
    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return PseudoWater.fromBit(this.getDefaultState().withProperty(FACING, EnumFacing.byHorizontalIndex(meta & 3)), meta, 4);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getHorizontalIndex() | PseudoWater.waterloggedBit(state, 4);
    }

    /** @return true if the block behind (opposite FACING) has a solid face toward this */
    @Override
    public boolean hasSupport(IBlockAccess world, BlockPos pos, IBlockState state) {
        EnumFacing facing = state.getValue(FACING);
        BlockPos behind = pos.offset(facing.getOpposite());
        return world.getBlockState(behind).getBlockFaceShape(world, behind, facing) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        for (EnumFacing facing : EnumFacing.Plane.HORIZONTAL) {
            if (this.hasSupport(world, pos, this.getDefaultState().withProperty(FACING, facing))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected IBlockState deadState(IBlockState state) {
        return super.deadState(state).withProperty(FACING, state.getValue(FACING));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withRotation(IBlockState state, Rotation rot) {
        return state.withProperty(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState withMirror(IBlockState state, Mirror mirror) {
        return state.withRotation(mirror.toRotation(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        switch (state.getValue(FACING)) {
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            case EAST:
                return EAST;
            default:
                return NORTH;
        }
    }
}
