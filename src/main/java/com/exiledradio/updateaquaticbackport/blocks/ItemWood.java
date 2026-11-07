package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.13's wood ({@code oak_wood} and the rest): the bark-on-every-side log, as an item.
 *
 * <h2>1.12.2 already has the block</h2>
 * Vanilla's logs have a hidden {@code axis=none} state, bark on all six faces, drawn by vanilla's own
 * {@code oak_bark} models and reachable only by commands. That state is 1.13's wood, so this item places
 * it rather than adding a block: worlds keep vanilla's block, other mods see a vanilla log, and stripping
 * it already gives stripped wood.
 *
 * <h2>Not an ItemBlock</h2>
 * An {@code ItemBlock} for vanilla's log would replace the log's own item in the block-to-item map, so this
 * is a plain item that places the state the way {@code ItemBlock.onItemUse} does (same checks, sound,
 * advancement trigger). Breaking the block gives this item back ({@link BlocksEvents#harvestDrops}), and
 * picking it with the middle button gives this item too ({@link BlocksHooks#pickBlock}, patched into
 * {@code BlockLog}).
 */
public final class ItemWood extends Item {

    final BlockPlanks.EnumType type;

    ItemWood(BlockPlanks.EnumType type) {
        this.type = type;
    }

    /** @return vanilla's bark log state for this wood */
    public IBlockState state() {
        return state(this.type);
    }

    static IBlockState state(BlockPlanks.EnumType type) {
        IBlockState log = type.getMetadata() < 4
                ? Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, type)
                : Blocks.LOG2.getDefaultState().withProperty(BlockNewLog.VARIANT, type);
        return log.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.NONE);
    }

    /** @return the log item (metadata included) the crafting recipe takes for this wood */
    static ItemStack log(BlockPlanks.EnumType type, int count) {
        return type.getMetadata() < 4 ? new ItemStack(Blocks.LOG, count, type.getMetadata())
                : new ItemStack(Blocks.LOG2, count, type.getMetadata() - 4);
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos)) {
            pos = pos.offset(facing);
        }
        ItemStack stack = player.getHeldItem(hand);
        IBlockState state = state();
        if (stack.isEmpty() || !player.canPlayerEdit(pos, facing, stack) || !world.mayPlace(state.getBlock(), pos, false, facing, player)) {
            return EnumActionResult.FAIL;
        }
        if (world.setBlockState(pos, state, 11)) {
            IBlockState now = world.getBlockState(pos);
            now.getBlock().onBlockPlacedBy(world, pos, now, player, stack);
            if (player instanceof EntityPlayerMP) {
                CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos, stack);
            }
            SoundType sound = now.getBlock().getSoundType(now, world, pos, player);
            world.playSound(player, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                    sound.getPitch() * 0.8F);
            stack.shrink(1);
        }
        return EnumActionResult.SUCCESS;
    }
}
