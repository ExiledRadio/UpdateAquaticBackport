package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.mojang.authlib.GameProfile;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Assertions and a scratch area for the self-tests. */
public final class Check {

    public final WorldServer world;

    private final List<String> failures = new ArrayList<String>();
    private int passed;
    private String section = "";

    /** Each test area is a fresh 32x32 column, east of the last, high above the terrain. */
    private int nextArea;

    Check(WorldServer world) {
        this.world = world;
    }

    void section(String name) {
        this.section = name;
        UpdateAquaticBackport.LOGGER.info("SELFTEST --- {} ---", name);
    }

    public boolean ok(boolean condition, String what) {
        if (condition) {
            this.passed++;
        } else {
            fail(what);
        }
        return condition;
    }

    public boolean eq(Object expected, Object actual, String what) {
        return ok(Objects.equals(expected, actual), what + ": expected " + expected + ", got " + actual);
    }

    public void fail(String what) {
        String line = "[" + this.section + "] " + what;
        this.failures.add(line);
        UpdateAquaticBackport.LOGGER.error("SELFTEST FAIL {}", line);
    }

    int passed() {
        return this.passed;
    }

    int failed() {
        return this.failures.size();
    }

    List<String> failures() {
        return this.failures;
    }

    /**
     * @return the corner of a new empty 32x32 area at y=100, cleared from y=60 to y=160 with a stone floor
     * at y=99 and its chunks loaded
     *
     * <p>Population never writes into a test area. A chunk populates the 16x16 square offset by 8 once it
     * and its +x, +z and +xz neighbors are loaded, so the area's own chunks and the ring west and north of
     * them can all reach it. Each is marked populated as it loads, west to east, so none of them ever
     * populates. Without this, a test that touches the chunk next to its area mid-test (a mob's
     * wandering, a ray trace) populates it then: in a cold biome its freezing pass turns the test's pool
     * surface to ice, and trees, lakes or reefs land in the test.
     *
     * <p>The ring is loaded after the area is cleared: with it loaded, every block cleared would also
     * relight across the chunk borders, which makes clearing many times slower. If loading the ring did
     * populate something (a ring chunk whose other neighbors an earlier test had loaded), the area is
     * cleared again.
     */
    public BlockPos area() {
        int x = 20000 + 48 * this.nextArea++;
        int z = 20000;
        BlockPos corner = new BlockPos(x, 100, z);
        int cx0 = x >> 4;
        int cz0 = z >> 4;
        int cx1 = (x + 31) >> 4;
        int cz1 = (z + 31) >> 4;
        boolean populated = this.loadUnpopulated(cx0, cx1, cz0, cz1, cx0, cz0);
        this.clear(x, z);
        if (this.loadUnpopulated(cx0 - 1, cx1, cz0 - 1, cz1, cx0, cz0) || populated) {
            this.clear(x, z);
        }
        return corner;
    }

    /**
     * Loads the chunks in a range, west to east, marking each populated as it loads. A range that starts
     * west or north of the area's first chunk {@code (areaX, areaZ)} is the ring, and skips the area's own
     * chunks.
     *
     * @return true if loading made any of them populate after all
     */
    private boolean loadUnpopulated(int fromX, int toX, int fromZ, int toZ, int areaX, int areaZ) {
        boolean populated = false;
        for (int cx = fromX; cx <= toX; cx++) {
            for (int cz = fromZ; cz <= toZ; cz++) {
                boolean ring = fromX < areaX || fromZ < areaZ;
                if (ring && cx >= areaX && cz >= areaZ) {
                    continue;
                }
                boolean wasLoaded = this.world.getChunkProvider().getLoadedChunk(cx, cz) != null;
                Chunk chunk = this.world.getChunk(cx, cz);
                if (chunk.isTerrainPopulated()) {
                    populated |= !wasLoaded;
                } else {
                    chunk.setTerrainPopulated(true);
                }
            }
        }
        return populated;
    }

    private void clear(int x, int z) {
        for (int dx = 0; dx < 32; dx++) {
            for (int dz = 0; dz < 32; dz++) {
                for (int y = 60; y < 160; y++) {
                    BlockPos p = new BlockPos(x + dx, y, z + dz);
                    IBlockState want = y == 99 ? Blocks.STONE.getDefaultState() : Blocks.AIR.getDefaultState();
                    if (this.world.getBlockState(p) != want) {
                        this.world.setBlockState(p, want, 2 | 16);
                    }
                }
            }
        }
    }

    /** Fills a box (inclusive) with a state, without neighbor updates. */
    public void fill(BlockPos from, BlockPos to, IBlockState state) {
        for (BlockPos p : BlockPos.getAllInBox(from, to)) {
            this.world.setBlockState(p, state, 2 | 16);
        }
    }

    /** A fake player for this test, standing at a position, survival mode. */
    public FakePlayer player(String name, BlockPos at) {
        FakePlayer player = FakePlayerFactory.get(this.world,
                new GameProfile(UUID.nameUUIDFromBytes(("selftest:" + name).getBytes()), "[selftest " + name + "]"));
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.inventory.clear();
        return player;
    }

    public static boolean isFake(EntityPlayerMP player) {
        return player instanceof FakePlayer;
    }
}
