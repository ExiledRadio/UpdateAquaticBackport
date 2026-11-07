package com.exiledradio.updateaquaticbackport.kelp;

import com.exiledradio.updateaquaticbackport.common.BlockUnderwater;
import com.exiledradio.updateaquaticbackport.common.SharedSounds;

import net.minecraft.block.Block;
import net.minecraft.block.IGrowable;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Seagrass and tall seagrass, from 1.14.4's SeaGrassBlock and TallSeaGrassBlock.
 *
 * <ul>
 *   <li>Placed only into full water (a source, or falling water, which becomes a source) on a block with a
 *       solid top, not magma. Always waterlogged ({@link Need#REQUIRED}): drained, it goes, with no drop; an
 *       empty bucket takes nothing from it.</li>
 *   <li>Replaceable, like grass: placing a block into it takes its place.</li>
 *   <li>Drops nothing unless cut with shears: one seagrass, or two from tall seagrass.</li>
 *   <li>Bone meal turns seagrass into tall seagrass when the water above is a still source.</li>
 *   <li>Tall seagrass is two blocks, {@link #HALF} lower and upper; either half going takes the other.</li>
 * </ul>
 *
 * <p>Shears are handled in {@link #harvestBlock}, not through Forge's {@code IShearable}: Forge's shears
 * set an {@code IShearable} block to air directly, which takes its water with it, and seagrass has to leave
 * its water.
 */
public class BlockSeagrass extends BlockUnderwater implements IGrowable {

    public enum Half implements IStringSerializable {
        LOWER, UPPER;

        @Override
        public String getName() {
            return this == LOWER ? "lower" : "upper";
        }
    }

    public static final PropertyEnum<Half> HALF = PropertyEnum.create("half", Half.class);

    private static final AxisAlignedBB SHORT_BOX = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 12 / 16.0, 14 / 16.0);
    private static final AxisAlignedBB TALL_BOX = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 1, 14 / 16.0);

    /** True for {@code tall_seagrass}. */
    public final boolean tall;

    /** The short seagrass, for the tall one's drops and bone meal; set by {@link KelpContent}. */
    BlockSeagrass shortForm;
    BlockSeagrass tallForm;

    public BlockSeagrass(boolean tall) {
        super(SEA_GRASS, MapColor.WATER);
        this.tall = tall;
        this.setDefaultState(this.blockState.getBaseState().withProperty(HALF, Half.LOWER));
        this.setHardness(0.0F);
        this.setSoundType(SharedSounds.WET_GRASS);
    }

    @Override
    public Need waterNeed(IBlockState state) {
        return Need.REQUIRED;
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HALF);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(HALF, this.tall && (meta & 1) == 1 ? Half.UPPER : Half.LOWER);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return this.tall && state.getValue(HALF) == Half.UPPER ? 1 : 0;
    }

    /** Like grass: placing a block here takes its place. */
    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Where it can be
    // ------------------------------------------------------------------------------------------

    /** @return true if the block below has a solid top and is not magma */
    static boolean solidGround(IBlockAccess world, BlockPos ground) {
        IBlockState state = world.getBlockState(ground);
        return state.getBlock() != Blocks.MAGMA && state.getBlockFaceShape(world, ground, EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    /** @return true if this state may stay where it is */
    public boolean canStay(World world, BlockPos pos, IBlockState state) {
        if (this.tall && state.getValue(HALF) == Half.UPPER) {
            IBlockState below = world.getBlockState(pos.down());
            return below.getBlock() == this && below.getValue(HALF) == Half.LOWER;
        }
        if (!solidGround(world, pos.down())) {
            return false;
        }
        if (this.tall) {
            IBlockState above = world.getBlockState(pos.up());
            return above.getBlock() == this && above.getValue(HALF) == Half.UPPER;
        }
        return true;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        boolean water = (fullWater(world, pos) && !(here.getBlock() instanceof BlockSeagrass)) || here.getBlock() == this.shortForm;
        if (!water || !solidGround(world, pos.down())) {
            return false;
        }
        return !this.tall || fullWater(world, pos.up());
    }

    /** Tall seagrass placed by hand or by a command gets its upper half too. */
    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, net.minecraft.entity.EntityLivingBase placer, ItemStack stack) {
        if (this.tall) {
            world.setBlockState(pos.up(), this.getDefaultState().withProperty(HALF, Half.UPPER), 2);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!this.canStay(world, pos, state)) {
            if (this.tall) {
                // The other half went: this one goes with it, quietly, as a double plant's does, leaving its water.
                world.setBlockToAir(pos);
            } else {
                breakLeavingWater(world, pos);
            }
        }
    }

    // ------------------------------------------------------------------------------------------
    // Bone meal: seagrass grows tall
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean canGrow(World world, BlockPos pos, IBlockState state, boolean isClient) {
        return !this.tall;
    }

    @Override
    public boolean canUseBonemeal(World world, Random rand, BlockPos pos, IBlockState state) {
        return true;
    }

    @Override
    public void grow(World world, Random rand, BlockPos pos, IBlockState state) {
        growTall(world, pos);
    }

    /** Short seagrass to tall, if the water above is a still source. @return true if it grew */
    boolean growTall(World world, BlockPos pos) {
        if (this.tall || this.tallForm == null || !BlockKelp.isVanillaWaterSource(world, pos.up())) {
            return false;
        }
        world.setBlockState(pos, this.tallForm.getDefaultState().withProperty(HALF, Half.LOWER), 2);
        world.setBlockState(pos.up(), this.tallForm.getDefaultState().withProperty(HALF, Half.UPPER), 2);
        return true;
    }

    // ------------------------------------------------------------------------------------------
    // Drops: shears only
    // ------------------------------------------------------------------------------------------

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack tool) {
        if (!world.isRemote && tool.getItem() instanceof ItemShears) {
            player.addStat(StatList.getBlockStats(this));
            player.addExhaustion(0.005F);
            spawnAsEntity(world, pos, new ItemStack(this.shortForm, this.tall ? 2 : 1));
        }
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.shortForm);
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.tall ? TALL_BOX : SHORT_BOX;
    }
}
