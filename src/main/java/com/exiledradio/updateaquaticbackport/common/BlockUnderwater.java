package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * A plant-like block that lives in water: kelp, seagrass, coral and coral fans, the bubble column.
 *
 * <h2>Its water is waterlogging</h2>
 * The water in the block's own space is Fluidlogged API's, through Backport Core's {@link Waterloggable}: it
 * is kept beside the block, flows, renders, carries boats and drowns mobs as any water does, and light goes
 * through it as through water. The block itself is not water and has no water property: ask
 * {@link Waterlogging#hasWater}.
 *
 * <ul>
 *   <li>{@link Waterloggable.Need#REQUIRED} blocks (kelp, seagrass, bubble columns) only exist in water. Drained,
 *       Backport Core removes them with nothing left. Placed anywhere without a water source, by a command or
 *       another mod, they get one at the end of the tick ({@link RequiredWater}), as today's game gives them
 *       one: their water is part of them.</li>
 *   <li>Broken, blown up or losing their support, they leave their water as a source
 *       ({@code World.setBlockToAir} and {@code destroyBlock} keep it, under Fluidlogged API).</li>
 * </ul>
 *
 * <p>The shape is a plant's: no collision, not opaque, nothing can stand on it, destroyed by pistons.
 */
public abstract class BlockUnderwater extends Block implements Waterloggable {

    /** 1.13's {@code OCEAN_PLANT}: kelp and living coral. Water does not wash it away (it holds the water). */
    public static final Material OCEAN_PLANT = new MaterialLogic(MapColor.WATER) {
        {
            this.setNoPushMobility();
        }
    };

    /** 1.13's {@code SEA_GRASS}: as {@link #OCEAN_PLANT}, and a block placed there replaces it. */
    public static final Material SEA_GRASS = new MaterialLogic(MapColor.WATER) {
        {
            this.setNoPushMobility();
            this.setReplaceable();
        }
    };

    /**
     * 1.13's {@code BUBBLE_COLUMN} without its liquid flag: 1.12.2 code that sees a liquid material reads the
     * block as {@code BlockLiquid}, and the column's water is real water here anyway.
     */
    public static final Material BUBBLE_COLUMN = SEA_GRASS;

    protected BlockUnderwater(Material material, MapColor color) {
        super(material, color);
    }

    // ------------------------------------------------------------------------------------------
    // Water
    // ------------------------------------------------------------------------------------------

    /**
     * 1.14.4's placement test for kelp and seagrass ({@code isTagged(WATER) && getLevel() == 8}): a water
     * source, plain or kept in a block, or falling water.
     */
    public static boolean fullWater(IBlockAccess world, BlockPos pos) {
        if (Waterlogging.hasWaterSource(world, pos)) {
            return true;
        }
        IBlockState state = world.getBlockState(pos);
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && state.getValue(BlockLiquid.LEVEL) >= 8;
    }

    /** @return true for a plain water block that is a source: water with nothing else in it */
    public static boolean plainWaterSource(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    /** A block that needs water checks, once this tick is over, that it has a source. */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && this.waterNeed(state) == Need.REQUIRED) {
            RequiredWater.check(world, pos);
        }
    }

    /** Breaks the block with its drops, leaving its water, as today's game does when its support goes. */
    public static void breakLeavingWater(World world, BlockPos pos) {
        world.destroyBlock(pos, true);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    // ------------------------------------------------------------------------------------------
    // Shape and drawing: a plant
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    /** Cut-out, as 1.13's plants; the water around it is Fluidlogged API's to draw. */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
