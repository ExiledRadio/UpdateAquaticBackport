package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.StructurePlacements;
import com.exiledradio.backportcore.worldgen.StructurePlacements.BiomePoint;

/**
 * 1.13's structure placement, for Backport Core's placement table (the newest installed version's numbers
 * win; the table's {@code docs/API.md} section says how). Every number is 1.13.2's own, read from its
 * decompiled code: {@code ChunkGenSettings} for the spacings and separations, each structure's
 * {@code getSeedModifier} (or its {@code setLargeFeatureSeedWithSalt} call) for the salts.
 *
 * <ul>
 *   <li>Villages 32/8, salt 10387312; desert pyramid, igloo, jungle temple, witch hut 32/8 on salts 14357617,
 *       14357618, 14357619 and 14357620 (four grids; 1.12.2 has one); monument 32/5, salt 10387313, and
 *       mansion 80/20, salt 10387319, both triangular.</li>
 *   <li>Shipwrecks and ocean ruins 16/8 (salts 165745295 and 14357621); 1.16 widened them to 24/4 and 20/8.</li>
 *   <li>Every single-biome check reads the block-scale biome at block (9, 0, 9) of the chunk
 *       ({@code AbstractChunkGenerator}'s structure starts).</li>
 *   <li>Strongholds also go in the mushroom field shore (1.13's "has stronghold"; 1.12.2's rule is "above sea
 *       level").</li>
 * </ul>
 * Backport Core applies the rest of 1.13's overworld rules once a 1.13 backport is installed: the temples on
 * their four grids, the monument's squares, the villages' biome point, the strongholds' and spawn point's
 * biome search and the hills layer's seeding.
 */
public final class Placement113 {

    private Placement113() {
    }

    /** Pre-init. */
    public static void register() {
        StructurePlacements.version("1.13")
                .biomePoint(BiomePoint.BLOCK_9_9)
                .grid(Structures.VILLAGE, 32, 8, 10387312)
                .grid(StructurePlacements.DESERT_PYRAMID, 32, 8, 14357617)
                .grid(StructurePlacements.IGLOO, 32, 8, 14357618)
                .grid(StructurePlacements.JUNGLE_PYRAMID, 32, 8, 14357619)
                .grid(StructurePlacements.SWAMP_HUT, 32, 8, 14357620)
                .grid(StructurePlacements.OCEAN_MONUMENT, 32, 5, 10387313, true)
                .grid(StructurePlacements.MANSION, 80, 20, 10387319, true)
                .grid(Structures.SHIPWRECK, 16, 8, 165745295)
                .grid(Structures.OCEAN_RUIN, 16, 8, 14357621)
                .strongholdBiome("minecraft:mushroom_island_shore", true);
    }
}
