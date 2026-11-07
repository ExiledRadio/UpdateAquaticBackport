package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The turtle egg item: placed like a block, or added to a block of fewer than four eggs it is aimed at, as
 * 1.13's egg block lets itself be replaced by its own item.
 */
public class ItemTurtleEgg extends ItemBlock {

    private final BlockTurtleEgg egg;

    public ItemTurtleEgg(BlockTurtleEgg egg) {
        super(egg);
        this.egg = egg;
    }

    /** @return the egg block this use adds an egg to (the one clicked, or the one beside the clicked face), or null */
    @Nullable
    public BlockPos addTarget(World world, BlockPos pos, EnumFacing facing) {
        IBlockState clicked = world.getBlockState(pos);
        if (clicked.getBlock() == this.egg) {
            return clicked.getValue(BlockTurtleEgg.EGGS) < 4 ? pos : null;
        }
        if (clicked.getBlock().isReplaceable(world, pos)) {
            return null;
        }
        BlockPos beside = pos.offset(facing);
        IBlockState there = world.getBlockState(beside);
        return there.getBlock() == this.egg && there.getValue(BlockTurtleEgg.EGGS) < 4 ? beside : null;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        BlockPos target = addTarget(world, pos, facing);
        if (target == null) {
            return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
        }
        ItemStack stack = player.getHeldItem(hand);
        if (stack.isEmpty() || !player.canPlayerEdit(target, facing, stack)) {
            return EnumActionResult.FAIL;
        }
        IBlockState state = world.getBlockState(target);
        IBlockState more = state.withProperty(BlockTurtleEgg.EGGS, state.getValue(BlockTurtleEgg.EGGS) + 1);
        if (!world.setBlockState(target, more, 11)) {
            return EnumActionResult.FAIL;
        }
        SoundType sound = this.egg.getSoundType(more, world, target, player);
        world.playSound(player, target, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player instanceof EntityPlayerMP) {
            CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, target, stack);
        }
        stack.shrink(1);
        return EnumActionResult.SUCCESS;
    }

    /** The client asks this before sending the click; adding to a cluster is always allowed. */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        return addTarget(world, pos, side) != null || super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }
}
