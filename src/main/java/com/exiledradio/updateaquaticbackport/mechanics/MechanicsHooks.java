package com.exiledradio.updateaquaticbackport.mechanics;

import com.exiledradio.updateaquaticbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;

import java.util.List;

/**
 * What the mechanics patches (package {@code asm}, {@code docs/PATCHES.md} P7 to P10) call. Each answers as
 * 1.12.2 does when its setting is off.
 */
public final class MechanicsHooks {

    /** 1.13's {@code LeavesBlock}: distance 7 decays, so up to 6 steps from a log the leaves stay (1.12.2: 4). */
    public static final int LEAF_DISTANCE = 6;

    /** The cube searched: every leaf up to {@link #LEAF_DISTANCE} steps away lies inside it. */
    private static final int SIDE = 2 * LEAF_DISTANCE + 1;

    private MechanicsHooks() {
    }

    // ------------------------------------------------------------------------------------------- P7 leaves

    /**
     * Run first in {@code BlockLeaves.updateTick}. Returns false to let 1.12.2's own check run (the setting
     * off, or a leaves block without vanilla's two properties); true when this has done the tick.
     *
     * <p>1.12.2's check, with 1.13's range: a leaf marked to check (its {@code check_decay}, set when a log or
     * leaf near it goes) that can decay stays if a log, or any block that sustains leaves, is at most 6 steps
     * away through leaves, else drops as vanilla's does. Unloaded surroundings leave the mark set, so a later
     * random tick checks again (1.12.2 decided on stale data there).
     */
    public static boolean leavesTick(BlockLeaves block, World world, BlockPos pos, IBlockState state) {
        if (!ModConfig.mechanics.leafDistance || !state.getPropertyKeys().contains(BlockLeaves.CHECK_DECAY)
                || !state.getPropertyKeys().contains(BlockLeaves.DECAYABLE)) {
            return false;
        }
        if (world.isRemote || !state.getValue(BlockLeaves.CHECK_DECAY) || !state.getValue(BlockLeaves.DECAYABLE)) {
            return true;
        }
        if (!world.isAreaLoaded(pos, LEAF_DISTANCE + 1)) {
            return true;
        }
        if (distanceToLog(world, pos) <= LEAF_DISTANCE) {
            world.setBlockState(pos, state.withProperty(BlockLeaves.CHECK_DECAY, Boolean.FALSE), 4);
        } else {
            block.dropBlockAsItem(world, pos, world.getBlockState(pos), 0);
            world.setBlockToAir(pos);
        }
        return true;
    }

    /**
     * @return the steps from a leaf to the nearest block that sustains leaves, through leaves only, as 1.13's
     * {@code distance} counts them (1 beside a log), or {@code LEAF_DISTANCE + 1} when none is that close
     */
    public static int distanceToLog(World world, BlockPos pos) {
        IBlockState self = world.getBlockState(pos);
        if (self.getBlock().canSustainLeaves(self, world, pos)) {
            return 0;
        }
        boolean[] seen = new boolean[SIDE * SIDE * SIDE];
        int[] queue = new int[SIDE * SIDE * SIDE];
        int head = 0;
        int tail = 0;
        queue[tail++] = index(0, 0, 0);
        seen[index(0, 0, 0)] = true;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int depth = 0; depth < LEAF_DISTANCE && head < tail; depth++) {
            int end = tail;
            while (head < end) {
                int i = queue[head++];
                int dx = i / (SIDE * SIDE) - LEAF_DISTANCE;
                int dy = i / SIDE % SIDE - LEAF_DISTANCE;
                int dz = i % SIDE - LEAF_DISTANCE;
                for (EnumFacing facing : EnumFacing.VALUES) {
                    int nx = dx + facing.getXOffset();
                    int ny = dy + facing.getYOffset();
                    int nz = dz + facing.getZOffset();
                    if (Math.abs(nx) > LEAF_DISTANCE || Math.abs(ny) > LEAF_DISTANCE || Math.abs(nz) > LEAF_DISTANCE) {
                        continue;
                    }
                    int n = index(nx, ny, nz);
                    if (seen[n]) {
                        continue;
                    }
                    seen[n] = true;
                    at.setPos(pos.getX() + nx, pos.getY() + ny, pos.getZ() + nz);
                    IBlockState state = world.getBlockState(at);
                    Block b = state.getBlock();
                    if (b.canSustainLeaves(state, world, at)) {
                        return depth + 1;
                    }
                    if (b.isLeaves(state, world, at)) {
                        queue[tail++] = n;
                    }
                }
            }
        }
        return LEAF_DISTANCE + 1;
    }

    private static int index(int dx, int dy, int dz) {
        return ((dx + LEAF_DISTANCE) * SIDE + dy + LEAF_DISTANCE) * SIDE + dz + LEAF_DISTANCE;
    }

    /**
     * Run first in {@code BlockLog.breakBlock}: marks the leaves up to 6 blocks around a broken log to check
     * (1.12.2 marks 4; vanilla's own marking follows and changes nothing more). In 1.13 a broken log
     * reaches every leaf it held up through their distances; here the leaves 5 and 6 away it held up are
     * marked too.
     */
    public static void logBroken(World world, BlockPos pos) {
        if (!ModConfig.mechanics.leafDistance || world.isRemote
                || !world.isAreaLoaded(pos.add(-LEAF_DISTANCE - 1, -LEAF_DISTANCE - 1, -LEAF_DISTANCE - 1),
                pos.add(LEAF_DISTANCE + 1, LEAF_DISTANCE + 1, LEAF_DISTANCE + 1))) {
            return;
        }
        for (BlockPos.MutableBlockPos at : BlockPos.getAllInBoxMutable(pos.add(-LEAF_DISTANCE, -LEAF_DISTANCE, -LEAF_DISTANCE),
                pos.add(LEAF_DISTANCE, LEAF_DISTANCE, LEAF_DISTANCE))) {
            IBlockState state = world.getBlockState(at);
            if (state.getBlock().isLeaves(state, world, at)) {
                state.getBlock().beginLeavesDecay(state, world, at.toImmutable());
            }
        }
    }

    // ------------------------------------------------------------------------------- P8 cactus and signs

    /**
     * {@code BlockSign} and {@code BlockBanner}'s {@code canPlaceBlockAt} pass their "a cactus is beside"
     * answer through this. 1.13 lets them be placed there; the cactus then breaks, as it does beside any
     * solid block.
     */
    public static boolean cactusBeside(boolean vanilla) {
        return vanilla && !ModConfig.mechanics.cactusBreaksSigns;
    }

    // ------------------------------------------------------------------------------------ P9 witch hut pot

    /**
     * Run in the swamp hut's {@code addComponentParts} right after it places its flower pot. 1.12.2 asks for a
     * pot "with a red mushroom" by a block state, but a pot's plant is in its tile entity, so the pot comes
     * out empty; 1.13's has the red mushroom. Fills the empty pots in the hut's part of this chunk.
     */
    public static void swampHutPot(StructureComponent hut, World world, StructureBoundingBox chunkBox) {
        if (!ModConfig.mechanics.witchHutMushroom) {
            return;
        }
        StructureBoundingBox box = hut.getBoundingBox();
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int x = Math.max(box.minX, chunkBox.minX); x <= Math.min(box.maxX, chunkBox.maxX); x++) {
            for (int z = Math.max(box.minZ, chunkBox.minZ); z <= Math.min(box.maxZ, chunkBox.maxZ); z++) {
                for (int y = Math.max(box.minY, chunkBox.minY); y <= Math.min(box.maxY, chunkBox.maxY); y++) {
                    at.setPos(x, y, z);
                    if (world.getBlockState(at).getBlock() != Blocks.FLOWER_POT) {
                        continue;
                    }
                    TileEntity tile = world.getTileEntity(at);
                    if (tile instanceof TileEntityFlowerPot && ((TileEntityFlowerPot) tile).getFlowerItemStack().isEmpty()) {
                        ((TileEntityFlowerPot) tile).setItemStack(new ItemStack(Blocks.RED_MUSHROOM));
                        tile.markDirty();
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------------------- P10 creative sleep

    /**
     * {@code EntityPlayer.trySleep} asks this instead of "are there no monsters near the bed": 1.13 lets a
     * player in Creative sleep with monsters nearby.
     */
    public static boolean noMonstersNear(List<?> monsters, EntityPlayer player) {
        return monsters.isEmpty() || ModConfig.mechanics.creativeSleep && player.isCreative();
    }
}
