package com.exiledradio.updateaquaticbackport.bubble;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Where bubble columns can be and how they change: today's {@code BubbleColumnBlock.updateColumn}, and the
 * 20-tick wait soul sand and magma blocks have before they start one.
 *
 * <h2>Soul sand and magma without replacing them</h2>
 * 1.13's soul sand and magma block schedule a tick of their own when placed or when water appears above
 * them, and start the column in it. Ours are vanilla's 1.12.2 blocks, so the same happens here from the
 * outside: {@link BubbleEvents} sees the block change ({@code NeighborNotifyEvent}) and puts the base on
 * this class's own list, which the world tick works through 20 ticks later. Vanilla's scheduled tick cannot
 * be used: it would run the magma block's own 1.12.2 tick, which boils the water above it away.
 *
 * <p>The list is not saved. A base waiting when the server stops starts its column at the next change
 * next to it, or when the chunk is generated ({@link BubbleGenerator}).
 */
public final class BubbleColumns {

    /** 1.14.4's soul sand and magma block tick rate. */
    public static final int BASE_DELAY = 20;

    /** Bases waiting to start a column, per world: position -> world time it is due. */
    private static final Map<World, Map<BlockPos, Long>> PENDING = new WeakHashMap<World, Map<BlockPos, Long>>();

    private BubbleColumns() {
    }

    /** @return true for the blocks that start a column: soul sand (upward) and magma (downward) */
    public static boolean isBase(Block block) {
        return block == Blocks.SOUL_SAND || block == Blocks.MAGMA;
    }

    /** @return true for plain still or flowing water at level 0: a source, nothing else in it */
    public static boolean isVanillaSource(IBlockState state) {
        Block block = state.getBlock();
        return (block == Blocks.WATER || block == Blocks.FLOWING_WATER) && state.getValue(BlockLiquid.LEVEL) == 0;
    }

    /** Today's {@code canExistIn}: a column or a plain water source. Never flowing water, kelp or coral. */
    public static boolean canExistIn(IBlockState state) {
        BlockBubbleColumn column = BubbleContent.column();
        return (column != null && state.getBlock() == column) || isVanillaSource(state);
    }

    /**
     * Today's {@code getColumnState}.
     *
     * @return the state a block over {@code below} must be: the same column, an upward column over soul
     * sand, a whirlpool over magma, else still water
     */
    public static IBlockState columnFor(IBlockState below) {
        BlockBubbleColumn column = BubbleContent.column();
        Block block = below.getBlock();
        if (column == null) {
            return Blocks.WATER.getDefaultState();
        }
        if (block == column) {
            return column.withDrag(below.getValue(BlockBubbleColumn.DRAG));
        }
        if (block == Blocks.SOUL_SAND) {
            return column.withDrag(false);
        }
        if (block == Blocks.MAGMA) {
            return column.withDrag(true);
        }
        return Blocks.WATER.getDefaultState();
    }

    /**
     * Today's {@code updateColumn}: sets {@code pos} and every column or water source block above it, in
     * one go, to what the block below {@code pos} asks for; stops at the first block that is neither, or
     * that already is.
     *
     * <p>Columns are set with flag 2 (clients and observers, no neighbor updates), as today's. Blocks that
     * go back to still water are then given a neighbor update of their own so that water flows on from
     * them: a column never flows, still water does.
     *
     * @param below the state under {@code pos}
     * @return how many blocks changed
     */
    public static int updateColumn(World world, BlockPos pos, IBlockState below) {
        if (!canExistIn(world.getBlockState(pos))) {
            return 0;
        }
        IBlockState target = columnFor(below);
        List<BlockPos> changed = new ArrayList<BlockPos>();
        if (world.setBlockState(pos, target, 2)) {
            changed.add(pos);
        }
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos(pos);
        for (m.move(net.minecraft.util.EnumFacing.UP); m.getY() < world.getHeight() && canExistIn(world.getBlockState(m));
             m.move(net.minecraft.util.EnumFacing.UP)) {
            if (!world.setBlockState(m, target, 2)) {
                break;
            }
            changed.add(m.toImmutable());
        }
        if (target.getBlock() == Blocks.WATER) {
            for (BlockPos p : changed) {
                world.neighborChanged(p, Blocks.WATER, p);
            }
        }
        return changed.size();
    }

    // ------------------------------------------------------------------------------------------
    // Bases waiting their 20 ticks
    // ------------------------------------------------------------------------------------------

    /** Puts a base on the list, due in {@link #BASE_DELAY} ticks, unless it is already waiting. */
    public static void scheduleBase(World world, BlockPos base) {
        Map<BlockPos, Long> pending = PENDING.get(world);
        if (pending == null) {
            pending = new LinkedHashMap<BlockPos, Long>();
            PENDING.put(world, pending);
        }
        BlockPos key = base.toImmutable();
        if (!pending.containsKey(key)) {
            pending.put(key, world.getTotalWorldTime() + BASE_DELAY);
        }
    }

    /** @return the world time a waiting base is due, or null if it is not waiting */
    @Nullable
    public static Long due(World world, BlockPos base) {
        Map<BlockPos, Long> pending = PENDING.get(world);
        return pending == null ? null : pending.get(base);
    }

    /**
     * Starts the column over every base due by {@code now} that is still soul sand or magma and loaded.
     *
     * @return how many bases were due
     */
    public static int runDue(World world, long now) {
        Map<BlockPos, Long> pending = PENDING.get(world);
        if (pending == null || pending.isEmpty()) {
            return 0;
        }
        List<BlockPos> due = new ArrayList<BlockPos>();
        for (Iterator<Map.Entry<BlockPos, Long>> it = pending.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<BlockPos, Long> e = it.next();
            if (e.getValue() <= now) {
                due.add(e.getKey());
                it.remove();
            }
        }
        for (BlockPos base : due) {
            if (world.isBlockLoaded(base)) {
                IBlockState state = world.getBlockState(base);
                if (isBase(state.getBlock())) {
                    updateColumn(world, base.up(), state);
                }
            }
        }
        return due.size();
    }
}
