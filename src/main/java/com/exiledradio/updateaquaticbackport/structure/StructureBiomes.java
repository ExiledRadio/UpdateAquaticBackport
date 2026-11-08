package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.worldgen.StructurePlacements;
import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/**
 * 1.13's biome rules for the ocean structures, read where the newest installed version reads them (1.13: the
 * block-scale biome at block (9, 9) of the start chunk; 1.16: the 1:4 cell at (4x + 2, 4z + 2)), through the
 * biome provider, so no chunk is loaded. The biome lists are the same in 1.13 and today:
 *
 * <ul>
 *   <li>{@code has_structure/shipwreck} is {@code #is_ocean}: every ocean, by {@link OceanKind} (the
 *       ocean stream's biomes, vanilla's and other mods' oceans by the biome dictionary);</li>
 *   <li>{@code has_structure/shipwreck_beached} and {@code buried_treasure} are {@code #is_beach}: beach and
 *       snowy beach, so the dictionary's beaches without the stone beach and the mushroom island shore,
 *       neither of which today's tag has;</li>
 *   <li>{@code ocean_ruin_warm}: lukewarm, warm and deep lukewarm oceans; {@code ocean_ruin_cold}: ocean,
 *       cold and frozen, and their deep forms.</li>
 * </ul>
 */
public final class StructureBiomes {

    private StructureBiomes() {
    }

    /**
     * @return the biome a start in this chunk reads: the newest installed version's point (Backport Core's
     *         placement table), the block biome at (9, 9) for 1.13 to 1.15, the 1:4 cell at (4x + 2, 4z + 2) from 1.16
     */
    public static Biome middle(World world, int chunkX, int chunkZ) {
        return StructurePlacements.biomeAt(world.getBiomeProvider(), chunkX, chunkZ);
    }

    public static OceanKind ocean(World world, int chunkX, int chunkZ) {
        // With the ocean stream's map, a vanilla ocean is a normal-temperature one, as 1.13's MixOceans makes it.
        return OceanKind.of(world, middle(world, chunkX, chunkZ), (chunkX << 4) + 9, (chunkZ << 4) + 9);
    }

    public static boolean isBeach(Biome biome) {
        return biome != null && biome != Biomes.STONE_BEACH && biome != Biomes.MUSHROOM_ISLAND_SHORE
                && BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)
                && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.MUSHROOM);
    }

    public static boolean isBeach(World world, int chunkX, int chunkZ) {
        return isBeach(middle(world, chunkX, chunkZ));
    }

    /** @return true for a warm ruin, false for a cold one, null where no ruin goes */
    public static Boolean warmRuin(OceanKind kind) {
        if (!kind.isOcean()) {
            return null;
        }
        return kind.isWarmish();
    }
}
