package com.exiledradio.updateaquaticbackport.common;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A block that stands in water without 1.13's waterlogging: kelp, seagrass, coral and coral fans.
 *
 * <h2>The block is the water</h2>
 * 1.12.2 cannot keep a block and water in one position. So a block of this kind <em>is</em> the water
 * there whenever it is "in water": its material is then {@link Material#WATER}, and it carries vanilla
 * water's {@link BlockLiquid#LEVEL} property, always 0 (a still source). That is enough for everything
 * that asks about water to treat it as water - swimming, drowning, underwater fog, squid and fish, water
 * flowing round it and forming sources beside it, a boat floating over it - and it has to be: vanilla
 * reads {@code LEVEL} from any block whose material is water (boats, buckets, fishing hooks, lily pads,
 * the camera's fog), and would throw on a water block without it.
 *
 * <p>{@code LEVEL} is never stored or varied: metadata ignores it, every state has it at 0, and the
 * client's state mapper leaves it out of model names.
 *
 * <h2>Waterlogged or always water</h2>
 * Kelp and seagrass only exist in water, so they are always water. Coral and coral fans can be placed
 * dry; they have 1.13's {@link #WATERLOGGED} property and are water only when it is true, and some
 * other material when dry. A bucket empties or fills them as in 1.13 ({@link PseudoWaterEvents}).
 *
 * <h2>The surface</h2>
 * Under water nothing needs drawing: vanilla never draws water between two water blocks. At the
 * surface, where 1.13 would draw the water's top over a waterlogged block, {@link #SURFACE} (worked out
 * when drawn, never stored) adds a water surface to the model, tinted by the biome like water's.
 * Known limit, until real waterlogging replaces this class: seen through glass from the side, the
 * water in the block's own space is not drawn.
 *
 * <h2>Breaking</h2>
 * A block that is water leaves still water behind when it is broken, blown up or loses its support,
 * as 1.13's do.
 */
public abstract class BlockPseudoWater extends Block {

    /** Vanilla water's level, here always 0. */
    public static final PropertyInteger LEVEL = BlockLiquid.LEVEL;

    /** 1.13's waterlogged property, on blocks that can also be dry. */
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    /** Drawn-only: this block is water and nothing water-like is above it. */
    public static final PropertyBool SURFACE = PropertyBool.create("surface");

    /** Vanilla water's light opacity in 1.12.2. */
    private static final int WATER_OPACITY = 3;

    /** What the block is made of when dry; unused by blocks that are always water. */
    protected final Material dryMaterial;

    /**
     * @param dryMaterial what the block is when not in water; the constructor's material is water so
     *                    every state starts as water
     */
    protected BlockPseudoWater(Material dryMaterial, MapColor color) {
        super(Material.WATER, color);
        this.dryMaterial = dryMaterial;
    }

    /** @return true if this block has {@link #WATERLOGGED}; false if it is always water */
    public abstract boolean canBeDry();

    /** @return the block's own properties, besides LEVEL, SURFACE and WATERLOGGED */
    protected abstract IProperty<?>[] ownProperties();

    /**
     * Builds the state container. Called from {@link Block}'s constructor, before any subclass field
     * is set, so {@link #ownProperties} and {@link #canBeDry} must return constants.
     */
    @Override
    protected BlockStateContainer createBlockState() {
        List<IProperty<?>> all = new ArrayList<IProperty<?>>(Arrays.asList(this.ownProperties()));
        all.add(LEVEL);
        all.add(SURFACE);
        if (this.canBeDry()) {
            all.add(WATERLOGGED);
        }
        return new BlockStateContainer(this, all.toArray(new IProperty[0]));
    }

    /** @return the default state with LEVEL 0, not at the surface, and waterlogged if it can be dry */
    protected IBlockState baseState() {
        IBlockState state = this.blockState.getBaseState().withProperty(LEVEL, 0).withProperty(SURFACE, false);
        return this.canBeDry() ? state.withProperty(WATERLOGGED, true) : state;
    }

    /** @return true if this state is water */
    public boolean isWater(IBlockState state) {
        return !this.canBeDry() || state.getValue(WATERLOGGED);
    }

    /** @return this state without its water, or null if it cannot be drained */
    @Nullable
    public IBlockState drained(IBlockState state) {
        return this.canBeDry() && state.getValue(WATERLOGGED) ? state.withProperty(WATERLOGGED, false) : null;
    }

    /** @return this state filled with water, or null if it cannot be filled */
    @Nullable
    public IBlockState filled(IBlockState state) {
        return this.canBeDry() && !state.getValue(WATERLOGGED) ? state.withProperty(WATERLOGGED, true) : null;
    }

    /** @return true if the block at a position is water, ours or vanilla's, at any level */
    public static boolean isWaterAt(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.WATER;
    }

    /** @return true if the block at a position is a water source block: vanilla's still or flowing at level 0, or ours */
    public static boolean isWaterSourceAt(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() != Material.WATER) {
            return false;
        }
        if (state.getBlock() instanceof BlockPseudoWater) {
            return true;
        }
        return state.getBlock() instanceof BlockLiquid && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    // ------------------------------------------------------------------------------------------
    // Water or not
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public Material getMaterial(IBlockState state) {
        return this.isWater(state) ? Material.WATER : this.dryMaterial;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightOpacity(IBlockState state) {
        return this.isWater(state) ? WATER_OPACITY : 0;
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.getLightOpacity(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        boolean surface = this.isWater(state) && world.getBlockState(pos.up()).getMaterial() != Material.WATER;
        return state.withProperty(LEVEL, 0).withProperty(SURFACE, surface);
    }

    /** Placing another block into this one does not replace it, as 1.13's; seagrass overrides. */
    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    // ------------------------------------------------------------------------------------------
    // Leaving water behind
    // ------------------------------------------------------------------------------------------

    /** @return what is left in this position when the block goes: still water if it was water, else air */
    public IBlockState leftBehind(IBlockState state) {
        return this.isWater(state) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        this.onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, this.leftBehind(state), world.isRemote ? 11 : 3);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        IBlockState state = world.getBlockState(pos);
        world.setBlockState(pos, this.leftBehind(state), 3);
        this.onExplosionDestroy(world, pos, explosion);
    }

    /** Drops the block and leaves water or air, as when its support goes. */
    public void breakNaturally(World world, BlockPos pos, IBlockState state) {
        this.dropBlockAsItem(world, pos, state, 0);
        world.setBlockState(pos, this.leftBehind(state), 3);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    // ------------------------------------------------------------------------------------------
    // Shape and drawing: a plant by default
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return NULL_AABB;
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

    /**
     * The translucent layer, the water's: the surface is translucent, and the plant's own cut-out
     * pixels draw correctly in it too.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.TRANSLUCENT;
    }

    /** Metadata of the waterlogged bit, for blocks that can be dry; 0 for those that cannot. */
    protected int waterloggedBit(IBlockState state, int bit) {
        return this.canBeDry() && state.getValue(WATERLOGGED) ? bit : 0;
    }
}
