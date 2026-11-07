package com.exiledradio.updateaquaticbackport.coral;

import com.exiledradio.updateaquaticbackport.common.BlockPseudoWater;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Coral and coral fans, living or dead, standing on the floor (wall fans are {@link BlockCoralWallFan}).
 * From 1.14.4's {@code AbstractCoralPlantBlock}, {@code CoralPlantBlock}, {@code CoralFanBlock} and
 * {@code CoralFinBlock}.
 *
 * <ul>
 *   <li>Waterlogged when placed into a water source, dry otherwise; a bucket empties or fills it.</li>
 *   <li>Stands on a block with a solid top; goes (leaving its water) when that support goes.</li>
 *   <li>A living one that is neither waterlogged nor touching water on any face dies 60-99 ticks later
 *       unless water has come back. The dead form keeps its place, its facing, and is dry.</li>
 *   <li>Drops only with Silk Touch. Breaks instantly; no collision.</li>
 * </ul>
 *
 * <p>Dry, its material blocks liquids, so flowing water does not wash it away - as in 1.13, where flowing
 * water cannot enter a waterloggable block either.
 */
public class BlockCoral extends BlockPseudoWater {

    /** What a dry coral plant or fan is made of: no collision of its own, but water cannot flow into it. */
    static final Material DRY = new Material(MapColor.GRAY) {
        {
            this.setNoPushMobility();
        }

        @Override
        public boolean isSolid() {
            return false;
        }

        @Override
        public boolean blocksLight() {
            return false;
        }

        @Override
        public boolean isOpaque() {
            return false;
        }
    };

    private static final AxisAlignedBB PLANT_BOX = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 15 / 16.0, 14 / 16.0);
    private static final AxisAlignedBB FAN_BOX = new AxisAlignedBB(2 / 16.0, 0, 2 / 16.0, 14 / 16.0, 4 / 16.0, 14 / 16.0);

    public final CoralType type;
    /** True for a coral fan, false for a coral plant. */
    public final boolean fan;
    /** The dead form, or null if this is dead. */
    @Nullable
    Block dead;
    /** The item this drops with Silk Touch and picks as: its own, or the floor fan's for a wall fan. */
    Block itemBlock = this;

    public BlockCoral(CoralType type, boolean fan, boolean living, SoundType sound) {
        super(DRY, living ? type.color : MapColor.GRAY);
        this.type = type;
        this.fan = fan;
        this.setDefaultState(this.baseState());
        this.setHardness(0.0F);
        this.setSoundType(sound);
    }

    @Override
    public boolean canBeDry() {
        return true;
    }

    @Override
    protected IProperty<?>[] ownProperties() {
        return new IProperty<?>[0];
    }

    public boolean isLiving() {
        return this.dead != null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState().withProperty(WATERLOGGED, (meta & 1) == 1);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return this.waterloggedBit(state, 1);
    }

    // ------------------------------------------------------------------------------------------
    // Support and placement
    // ------------------------------------------------------------------------------------------

    /** @return true if this state has what it stands on */
    public boolean hasSupport(IBlockAccess world, BlockPos pos, IBlockState state) {
        return world.getBlockState(pos.down()).getBlockFaceShape(world, pos.down(), EnumFacing.UP) == BlockFaceShape.SOLID;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        IBlockState here = world.getBlockState(pos);
        return (here.getMaterial().isReplaceable() || here.getBlock().isReplaceable(world, pos))
                && this.hasSupport(world, pos, this.getDefaultState());
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ,
                                            int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(WATERLOGGED, isVanillaWaterSource(world, pos));
    }

    /** @return true for still or flowing vanilla water at level 0 */
    static boolean isVanillaWaterSource(IBlockAccess world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return (state.getBlock() == net.minecraft.init.Blocks.WATER || state.getBlock() == net.minecraft.init.Blocks.FLOWING_WATER)
                && state.getValue(net.minecraft.block.BlockLiquid.LEVEL) == 0;
    }

    // ------------------------------------------------------------------------------------------
    // Life and death
    // ------------------------------------------------------------------------------------------

    /** 1.14.4's isInWater: waterlogged, or water touching any face. */
    public boolean isInWater(IBlockAccess world, BlockPos pos, IBlockState state) {
        return state.getValue(WATERLOGGED) || BlockCoralBlock.touchesWater(world, pos);
    }

    private void checkLife(World world, BlockPos pos, IBlockState state) {
        if (this.isLiving() && !this.isInWater(world, pos, state)) {
            world.scheduleUpdate(pos, this, 60 + world.rand.nextInt(40));
        }
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        this.checkLife(world, pos, state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (!this.hasSupport(world, pos, state)) {
            // 1.13 removes it without drops.
            world.setBlockState(pos, this.leftBehind(state), 3);
            return;
        }
        this.checkLife(world, pos, state);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (this.isLiving() && !this.isInWater(world, pos, state)) {
            world.setBlockState(pos, this.deadState(state), 2);
        }
    }

    /** @return the dead form of this state, dry, facing the same way */
    protected IBlockState deadState(IBlockState state) {
        return this.dead.getDefaultState().withProperty(WATERLOGGED, false);
    }

    // ------------------------------------------------------------------------------------------
    // Drops: Silk Touch only
    // ------------------------------------------------------------------------------------------

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(this.itemBlock);
    }

    @Override
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return new ItemStack(this.itemBlock);
    }

    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.DESTROY;
    }

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return this.fan ? FAN_BOX : PLANT_BOX;
    }
}
