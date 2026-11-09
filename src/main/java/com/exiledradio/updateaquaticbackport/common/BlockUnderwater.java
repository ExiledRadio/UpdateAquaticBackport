package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.MaterialLogic;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
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
 *
 * <h2>Without Fluidlogged API</h2>
 * Nothing can hold water, so a block made then is the water in its space instead ({@link PseudoWater}): water
 * material with {@code LEVEL} 0 while it is in water, as this mod's blocks were before waterlogging. Every
 * override below that differs between the two asks {@link #isPseudoWater()} and otherwise does what it did.
 */
public abstract class BlockUnderwater extends Block implements Waterloggable, PseudoWaterBlock {

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

    /** True if this block is the water in its space ({@link PseudoWater}); fixed when it is made. */
    private final boolean pseudoWater;

    /** What the block is made of when dry, as the water in its space; with Fluidlogged API, its material. */
    private final Material dryMaterial;

    protected BlockUnderwater(Material material, MapColor color) {
        this(material, material, color);
    }

    /**
     * @param material    the block's material with Fluidlogged API
     * @param dryMaterial what it is made of when dry without Fluidlogged API (every state starts as water then)
     */
    protected BlockUnderwater(Material material, Material dryMaterial, MapColor color) {
        super(PseudoWater.active() ? Material.WATER : material, color);
        this.pseudoWater = PseudoWater.isPseudoWater(this);
        this.dryMaterial = dryMaterial;
    }

    @Override
    public final boolean isPseudoWater() {
        return this.pseudoWater;
    }

    /**
     * @return true if this block may be dry (coral, coral fans), so that without Fluidlogged API it has
     * {@link PseudoWater#WATERLOGGED}. Asked from {@code Block}'s constructor: return a constant.
     */
    protected boolean canBeDry() {
        return false;
    }

    /** @return the state container: these properties, plus {@link PseudoWater}'s when it is used */
    protected BlockStateContainer container(IProperty<?>... own) {
        return PseudoWater.container(this, this.canBeDry(), own);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return this.container();
    }

    /** @return the base state, with {@link PseudoWater}'s properties at their defaults when it is used */
    protected IBlockState baseState() {
        return PseudoWater.base(this.blockState.getBaseState());
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

    /**
     * A block that needs water checks, once this tick is over, that it has a source. Without Fluidlogged API it
     * is the water itself.
     */
    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (!world.isRemote && !this.pseudoWater && this.waterNeed(state) == Need.REQUIRED) {
            RequiredWater.check(world, pos);
        }
    }

    /** Breaks the block with its drops, leaving its water, as today's game does when its support goes. */
    public static void breakLeavingWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockUnderwater && ((BlockUnderwater) state.getBlock()).pseudoWater) {
            PseudoWater.breakNaturally(world, pos, state);
            return;
        }
        world.destroyBlock(pos, true);
    }

    /** Removes the block with no drops, leaving its water. */
    public static void removeLeavingWater(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockUnderwater && ((BlockUnderwater) state.getBlock()).pseudoWater) {
            world.setBlockState(pos, PseudoWater.leftBehind(state), 3);
            return;
        }
        world.setBlockToAir(pos);
    }

    /**
     * @return a state placed at this position: without Fluidlogged API, in water if it replaces a plain water
     * source (blocks that can be dry); with it, unchanged, as Fluidlogged API keeps the water itself
     */
    public IBlockState placedIn(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.pseudoWater ? PseudoWater.withWater(state, plainWaterSource(world, pos)) : state;
    }

    /** @return the state without its water, without Fluidlogged API; unchanged with it */
    public static IBlockState dry(IBlockState state) {
        return PseudoWater.withWater(state, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        IBlockState state = super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer);
        return this.placedIn(state, world, pos);
    }

    // ------------------------------------------------------------------------------------------
    // Without Fluidlogged API: the block is the water
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public Material getMaterial(IBlockState state) {
        return this.pseudoWater ? PseudoWater.material(state, this.dryMaterial) : super.getMaterial(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getLightOpacity(IBlockState state) {
        return this.pseudoWater ? PseudoWater.lightOpacity(state) : super.getLightOpacity(state);
    }

    @Override
    public int getLightOpacity(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.pseudoWater ? PseudoWater.lightOpacity(state) : super.getLightOpacity(state, world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return this.pseudoWater ? PseudoWater.actualState(state, world, pos) : super.getActualState(state, world, pos);
    }

    /** As water, placing another block here does not replace it (its material would say so); seagrass overrides. */
    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return !this.pseudoWater && super.isReplaceable(world, pos);
    }

    @Override
    public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        if (this.pseudoWater) {
            return PseudoWater.removedByPlayer(this, state, world, pos, player);
        }
        return super.removedByPlayer(state, world, pos, player, willHarvest);
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if (this.pseudoWater) {
            PseudoWater.exploded(this, world, pos, explosion);
            return;
        }
        super.onBlockExploded(world, pos, explosion);
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

    /**
     * Cut-out, as 1.13's plants; the water around it is Fluidlogged API's to draw. Without Fluidlogged API,
     * translucent, the water's layer: the surface it draws is translucent, and the plant's cut-out pixels draw
     * correctly in it too.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return this.pseudoWater ? BlockRenderLayer.TRANSLUCENT : BlockRenderLayer.CUTOUT;
    }
}
