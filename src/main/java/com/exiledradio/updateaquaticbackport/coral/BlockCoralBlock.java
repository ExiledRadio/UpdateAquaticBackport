package com.exiledradio.updateaquaticbackport.coral;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * A coral block, living or dead, from 1.14.4's {@code CoralBlock}.
 *
 * <ul>
 *   <li>Stone-like: hardness 1.5, blast resistance 6, a pickaxe to mine.</li>
 *   <li>A living one with no water touching any of its six faces dies 3-5 seconds (60-99 ticks) later,
 *       unless water has come back by then. Dead ones stay dead.</li>
 *   <li>A living one drops its dead form, or itself with Silk Touch.</li>
 * </ul>
 */
public class BlockCoralBlock extends Block {

    /** The dead form, or null if this is dead. */
    @Nullable
    final Block dead;

    public BlockCoralBlock(MapColor color, @Nullable Block dead, SoundType sound) {
        super(Material.ROCK, color);
        this.dead = dead;
        this.setHardness(1.5F);
        // 1.14's blast resistance 6 is 6 * 5 / 3 in 1.12.2's units.
        this.setResistance(6.0F * 5.0F / 3.0F);
        this.setSoundType(sound);
    }

    public boolean isLiving() {
        return this.dead != null;
    }

    /** @return true if water (a water block, or a waterlogged block) touches any face */
    public static boolean touchesWater(IBlockAccess world, BlockPos pos) {
        for (EnumFacing face : EnumFacing.values()) {
            if (Waterlogging.hasWater(world, pos.offset(face))) {
                return true;
            }
        }
        return false;
    }

    private void checkLife(World world, BlockPos pos) {
        if (this.isLiving() && !touchesWater(world, pos)) {
            world.scheduleUpdate(pos, this, 60 + world.rand.nextInt(40));
        }
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, net.minecraft.item.ItemStack stack) {
        this.checkLife(world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        this.checkLife(world, pos);
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (this.isLiving() && !touchesWater(world, pos)) {
            world.setBlockState(pos, this.dead.getDefaultState(), 2);
        }
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(this.isLiving() ? this.dead : this);
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, net.minecraft.entity.player.EntityPlayer player) {
        return true;
    }
}
