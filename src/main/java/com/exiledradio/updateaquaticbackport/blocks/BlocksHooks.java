package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockPattern;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * What this stream's patches to vanilla's classes call (package {@code asm}, see {@code docs/PATCHES.md}).
 * Each answers as vanilla 1.12.2 does when the stream is off.
 */
public final class BlocksHooks {

    private BlocksHooks() {
    }

    /**
     * {@code EntityLiving.getSlotForItemStack} asks for this where 1.12.2 names {@code Blocks.PUMPKIN}: the
     * block whose item goes on the head. 1.13 moved that to the carved pumpkin (whose item says so itself),
     * so the plain pumpkin is an ordinary item: held, not worn.
     */
    public static Block wornPumpkin() {
        BlockCarvedPumpkin carved = BlocksContent.carvedPumpkin();
        return carved != null ? carved : Blocks.PUMPKIN;
    }

    /**
     * {@code BlockPumpkin.trySpawnGolem} matches its golem patterns through this: no match for the plain
     * pumpkin (1.13's plain pumpkin builds nothing, however it was placed), vanilla's match for anything
     * else (the carved pumpkin, jack o'lanterns, other mods' subclasses).
     */
    @Nullable
    public static BlockPattern.PatternHelper golemMatch(BlockPattern pattern, World world, BlockPos pos, BlockPumpkin block) {
        if (block == Blocks.PUMPKIN && BlocksContent.carvedPumpkin() != null) {
            return null;
        }
        return pattern.match(world, pos);
    }

    /**
     * {@code BlockLog.getPickBlock} (added by the patch) passes vanilla's answer through this: a bark log -
     * 1.13's wood - picks the wood item rather than the log.
     */
    public static ItemStack pickBlock(Block block, IBlockState state, ItemStack vanilla) {
        if ((block != Blocks.LOG && block != Blocks.LOG2) || state.getBlock() != block
                || state.getValue(BlockLog.LOG_AXIS) != BlockLog.EnumAxis.NONE) {
            return vanilla;
        }
        BlockPlanks.EnumType type = block == Blocks.LOG ? state.getValue(BlockOldLog.VARIANT) : state.getValue(BlockNewLog.VARIANT);
        ItemWood wood = BlocksContent.wood(type);
        return wood == null ? vanilla : new ItemStack(wood);
    }
}
