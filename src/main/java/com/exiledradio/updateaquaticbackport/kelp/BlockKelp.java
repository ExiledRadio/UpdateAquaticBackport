package com.exiledradio.updateaquaticbackport.kelp;

import com.exiledradio.updateaquaticbackport.common.BlockUnderwater;
import com.exiledradio.updateaquaticbackport.common.SharedSounds;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;

import java.util.Random;

/**
 * Kelp: {@code kelp}, the growing top, and {@code kelp_plant}, every piece under it.
 *
 * <h2>From 1.14.4's KelpTopBlock and KelpBlock, with today's additions</h2>
 * <ul>
 *   <li>Placed only into full water (a source, or falling water, which becomes a source), on a block with a
 *       solid top (not magma) or on kelp.</li>
 *   <li>Always waterlogged ({@link Need#REQUIRED}): drained, it goes, with no drop; an empty bucket takes nothing
 *       from it ({@link KelpContent#onBucket}).</li>
 *   <li>The top grows 14% of random ticks, one block up, while the block above is a water source and it
 *       is not fully grown. The piece under a new top becomes {@code kelp_plant}; a plant left with no kelp
 *       above becomes a top again.</li>
 *   <li>A piece that loses its support breaks a tick later, with its drop, so a column breaks from the
 *       bottom up one piece per tick, as 1.13's.</li>
 *   <li>Every piece drops one kelp. Breaking leaves its water.</li>
 *   <li><b>Today's:</b> bone meal grows the top one block (1.16); shears on the top stop its growth for good
 *       (1.18).</li>
 * </ul>
 *
 * <h2>Age in sixteen steps</h2>
 * 1.13's top has 26 ages (0-25); a 1.12.2 block has 16 metadata values. So {@link #AGE} runs 0-15, and a
 * new top takes the next age only 15 times in 25 (otherwise it keeps its parent's). Starting from a
 * random age 0-14, the expected height a placed kelp grows to is then the same as 1.13's from its random
 * 0-24, and age 15 is "fully grown", which is also what shears set.
 */
public class BlockKelp extends BlockUnderwater implements IGrowable {

    public static final PropertyInteger AGE = PropertyInteger.create("age", 0, 15);

    /** 1.13's growth chance per random tick. */
    static final double GROW_CHANCE = 0.14;

    /** Chance that a new top is one age older: 15 of 1.13's 25 steps fit in 16 ages. */
    static final double AGE_STEP_CHANCE = 15.0 / 25.0;

    private static final AxisAlignedBB TOP_BOX = new AxisAlignedBB(0, 0, 0, 1, 9 / 16.0, 1);

    /** True for the top ({@code kelp}), false for {@code kelp_plant}. */
    public final boolean top;

    /** The other half of the pair; set by {@link KelpContent} after both are made. */
    BlockKelp other;

    public BlockKelp(boolean top) {
        super(OCEAN_PLANT, MapColor.WATER);
        this.top = top;
        this.setDefaultState(this.baseState().withProperty(AGE, 0));
        this.setHardness(0.0F);
        this.setSoundType(SharedSounds.WET_GRASS);
        this.setTickRandomly(top);
    }

    @Override
    public Need waterNeed(IBlockState state) {
        return Need.REQUIRED;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        // The plant ignores its age; one property list keeps the two blocks' states alike.
        return this.container(AGE);
    }

    BlockKelp topBlock() {
        return this.top ? this : this.other;
    }

    BlockKelp plantBlock() {
        return this.top ? this.other : this;
    }

    // ------------------------------------------------------------------------------------------
    // Metadata: the top's age; the plant has none
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(AGE, this.top ? meta & 15 : 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return this.top ? state.getValue(AGE) : 0;
    }

    /** @return a top with 1.13's random starting age (0-24), in this block's steps */
    public IBlockState randomTop(Random random) {
        return this.topBlock().getDefaultState().withProperty(AGE, random.nextInt(15));
    }

    // ------------------------------------------------------------------------------------------
    // Where it can be
    // ------------------------------------------------------------------------------------------

    /** @return true if a kelp piece at this position has something to stand on */
    public boolean hasSupport(IBlockAccess world, BlockPos pos) {
        BlockPos below = pos.down();
        IBlockState state = world.getBlockState(below);
        Block block = state.getBlock();
        if (block == Blocks.MAGMA) {
            return false;
        }
        return block == this.topBlock() || block == this.plantBlock()
                || state.getBlockFaceShape(world, below, EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return fullWater(world, pos) && world.getBlockState(pos).getBlock() != this.plantBlock()
                && world.getBlockState(pos).getBlock() != this.topBlock() && this.hasSupport(world, pos);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return this.randomTop(world.rand);
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        super.onBlockAdded(world, pos, state);
        this.settle(world, pos);
        // The piece below, if kelp, stops being a top.
        BlockPos below = pos.down();
        IBlockState under = world.getBlockState(below);
        if (under.getBlock() == this.topBlock()) {
            world.setBlockState(below, this.plantBlock().getDefaultState(), 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!this.hasSupport(world, pos)) {
            world.scheduleUpdate(pos, this, 1);
            return;
        }
        if (from.equals(pos.up())) {
            this.settle(world, pos);
        }
    }

    /** Makes this piece a top or a plant according to what is above it. */
    private void settle(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        Block above = world.getBlockState(pos.up()).getBlock();
        boolean kelpAbove = above == this.topBlock() || above == this.plantBlock();
        if (this.top && kelpAbove) {
            world.setBlockState(pos, this.plantBlock().getDefaultState(), 2);
        } else if (!this.top && !kelpAbove && state.getBlock() == this) {
            world.setBlockState(pos, this.randomTop(world.rand), 2);
        }
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!this.hasSupport(world, pos)) {
            breakLeavingWater(world, pos);
        }
    }

    // ------------------------------------------------------------------------------------------
    // Growing
    // ------------------------------------------------------------------------------------------

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!this.top || !this.hasSupport(world, pos)) {
            return;
        }
        BlockPos up = pos.up();
        if (state.getValue(AGE) < 15 && isVanillaWaterSource(world, up)
                && ForgeHooks.onCropsGrowPre(world, up, state, random.nextDouble() < GROW_CHANCE)) {
            this.growFrom(world, pos, state, random);
            ForgeHooks.onCropsGrowPost(world, pos, state, world.getBlockState(pos));
        }
    }

    /** Puts a new top above this top. */
    private void growFrom(World world, BlockPos pos, IBlockState state, Random random) {
        int age = state.getValue(AGE);
        int next = random.nextDouble() < AGE_STEP_CHANCE ? Math.min(15, age + 1) : age;
        world.setBlockState(pos.up(), this.topBlock().getDefaultState().withProperty(AGE, next), 3);
    }

    /**
     * @return true for a plain water source block: still or flowing water at level 0, with nothing in it - the
     * only thing kelp grows into, as 1.14.4's {@code == Blocks.WATER}
     */
    static boolean isVanillaWaterSource(IBlockAccess world, BlockPos pos) {
        return plainWaterSource(world, pos);
    }

    /** @return the top of the kelp column this piece belongs to */
    BlockPos findTop(World world, BlockPos pos) {
        BlockPos p = pos;
        while (world.getBlockState(p.up()).getBlock() == this.plantBlock() || world.getBlockState(p.up()).getBlock() == this.topBlock()) {
            p = p.up();
        }
        return p;
    }

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return isVanillaWaterSource(world, this.findTop(world, pos).up());
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    /** Today's bone meal: one more block on top, whatever the age. */
    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        BlockPos top = this.findTop(world, pos);
        IBlockState topState = world.getBlockState(top);
        if (topState.getBlock() == this.topBlock() && isVanillaWaterSource(world, top.up())) {
            this.growFrom(world, top, topState, rand);
        }
    }

    /** Shears on the top: fully grown from now on, as today's game does. */
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
                                    EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (!this.top || !(held.getItem() instanceof ItemShears) || state.getValue(AGE) == 15) {
            return false;
        }
        world.playSound(player, pos, SoundEvents.ENTITY_SHEEP_SHEAR, SoundCategory.BLOCKS, 1.0F, 1.0F);
        if (!world.isRemote) {
            world.setBlockState(pos, state.withProperty(AGE, 15), 2);
            held.damageItem(1, player);
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Drops and shape
    // ------------------------------------------------------------------------------------------

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.topBlock());
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.topBlock());
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.top ? TOP_BOX : FULL_BLOCK_AABB;
    }
}
