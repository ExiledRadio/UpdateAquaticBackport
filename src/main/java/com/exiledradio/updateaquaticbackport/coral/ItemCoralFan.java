package com.exiledradio.updateaquaticbackport.coral;

import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * A coral fan's item, as 1.13's wall-or-floor item: against the side of a block it places the wall fan,
 * facing out from that side; anywhere else, the floor fan.
 */
public class ItemCoralFan extends ItemBlock {

    private final BlockCoralWallFan wall;

    public ItemCoralFan(BlockCoral floor, BlockCoralWallFan wall) {
        super(floor);
        this.wall = wall;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        IBlockState clicked = world.getBlockState(pos);
        if (facing.getAxis().isHorizontal() && !clicked.getBlock().isReplaceable(world, pos)) {
            BlockPos target = pos.offset(facing);
            ItemStack stack = player.getHeldItem(hand);
            IBlockState there = world.getBlockState(target);
            IBlockState fan = this.wall.placedIn(this.wall.getDefaultState().withProperty(BlockCoralWallFan.FACING, facing), world, target);
            if (!stack.isEmpty() && player.canPlayerEdit(target, facing, stack)
                    && (there.getMaterial().isReplaceable() || there.getBlock().isReplaceable(world, target))
                    && this.wall.hasSupport(world, target, fan)) {
                if (this.placeBlockAt(stack, player, world, target, facing, hitX, hitY, hitZ, fan)) {
                    SoundType sound = this.wall.getSoundType(fan, world, target, player);
                    world.playSound(player, target, sound.getPlaceSound(), SoundCategory.BLOCKS,
                            (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
                    stack.shrink(1);
                }
                return EnumActionResult.SUCCESS;
            }
        }
        return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }
}
