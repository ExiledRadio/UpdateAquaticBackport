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
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

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
        IBlockState fan = wallFan(world, pos, facing);
        if (fan != null) {
            BlockPos target = pos.offset(facing);
            ItemStack stack = player.getHeldItem(hand);
            if (!stack.isEmpty() && player.canPlayerEdit(target, facing, stack)) {
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

    /**
     * The wall fan a click places in front of the clicked side: the side is horizontal, the clicked block stays, the
     * block in front can be replaced and the clicked block holds the fan up; null when the click places the floor fan
     * (or nothing).
     */
    @Nullable
    private IBlockState wallFan(World world, BlockPos pos, EnumFacing facing) {
        if (!facing.getAxis().isHorizontal() || world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            return null;
        }
        BlockPos target = pos.offset(facing);
        IBlockState there = world.getBlockState(target);
        IBlockState fan = this.wall.placedIn(this.wall.getDefaultState().withProperty(BlockCoralWallFan.FACING, facing), world, target);
        return (there.getMaterial().isReplaceable() || there.getBlock().isReplaceable(world, target))
                && this.wall.hasSupport(world, target, fan) ? fan : null;
    }

    /**
     * The client asks this before it sends a click to the server (as it does for slabs, which {@code ItemSlab} answers
     * the same way): it would ask for the floor fan, so a fan on a wall with nothing solid under it (most walls under
     * water) is refused on the client and never placed in a real game.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean canPlaceBlockOnSide(World world, BlockPos pos, EnumFacing side, EntityPlayer player, ItemStack stack) {
        return wallFan(world, pos, side) != null || super.canPlaceBlockOnSide(world, pos, side, player, stack);
    }
}
