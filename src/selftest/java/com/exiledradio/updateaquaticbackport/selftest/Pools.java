package com.exiledradio.updateaquaticbackport.selftest;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

/** Water pools for the underwater tests. */
final class Pools {

    private Pools() {
    }

    /**
     * Builds a stone-walled pool of still water in a fresh test area: inside 12x12, floor at y=99 (stone),
     * water y=100 to 100+depth-1, open air above.
     *
     * @return the floor block at the pool's north-west inner corner; water starts at {@code up()}
     */
    static BlockPos pool(Check c, int depth) {
        BlockPos corner = c.area();
        WorldServer world = c.world;
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState water = Blocks.WATER.getDefaultState();
        for (int x = 0; x < 14; x++) {
            for (int z = 0; z < 14; z++) {
                for (int y = 0; y < depth; y++) {
                    boolean wall = x == 0 || z == 0 || x == 13 || z == 13;
                    world.setBlockState(corner.add(x, y, z), wall ? stone : water, 2 | 16);
                }
            }
        }
        return corner.add(1, -1, 1);
    }

    /** Runs every pending block tick now, a few rounds, so chains of 1-tick updates finish. */
    static void settle(WorldServer world) {
        for (int i = 0; i < 40; i++) {
            world.tickUpdates(true);
        }
    }
}
