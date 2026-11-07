package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;

/** The shared code every stream builds on. */
public final class ScaffoldSelfTest implements SelfTest {

    @Override
    public String name() {
        return "scaffold";
    }

    @Override
    public void run(Check check) {
        BlockPos area = check.area();
        check.ok(check.world.isAirBlock(area.up()), "test area is clear");

        // Ocean temperatures: deterministic, all four noise kinds present over a large area.
        long seed = 12345L;
        int[] counts = new int[OceanKind.values().length];
        for (int x = -20000; x < 20000; x += 97) {
            for (int z = -20000; z < 20000; z += 97) {
                // The noise itself: with the ocean stream on, vanilla's ocean is registered as NORMAL.
                counts[OceanKind.fromNoise(seed, x, z).ordinal()]++;
            }
        }
        int total = 0;
        for (int c : counts) {
            total += c;
        }
        for (OceanKind kind : new OceanKind[] {OceanKind.WARM, OceanKind.LUKEWARM, OceanKind.NORMAL, OceanKind.COLD}) {
            check.ok(counts[kind.ordinal()] > total / 50, "noise gives some " + kind + " ocean: " + counts[kind.ordinal()] + "/" + total);
        }
        check.eq(0, counts[OceanKind.FROZEN.ordinal()], "noise never gives frozen");
        check.eq(OceanKind.fromNoise(seed, 100, 100), OceanKind.fromNoise(seed, 100, 100), "same column, same kind");
        check.eq(OceanKind.FROZEN, OceanKind.of(Biomes.FROZEN_OCEAN, seed, 0, 0), "frozen ocean biome");
        check.eq(OceanKind.NONE, OceanKind.of(Biomes.PLAINS, seed, 0, 0), "plains is not an ocean");
        check.eq(OceanKind.NONE, OceanKind.of(Biomes.RIVER, seed, 0, 0), "a river is not an ocean");
    }
}
