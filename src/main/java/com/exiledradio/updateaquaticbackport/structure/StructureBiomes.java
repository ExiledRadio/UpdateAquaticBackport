package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Today's biome rules for the ocean structures, read at the start chunk's middle (block 8, 8, as today's
 * {@code onTopOfChunkCenter}) through the biome provider, so no chunk is loaded:
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

    public static Biome middle(World world, int chunkX, int chunkZ) {
        return world.getBiomeProvider().getBiome(new BlockPos((chunkX << 4) + 8, 0, (chunkZ << 4) + 8));
    }

    public static OceanKind ocean(World world, int chunkX, int chunkZ) {
        return OceanKind.of(middle(world, chunkX, chunkZ), world.getSeed(), (chunkX << 4) + 8, (chunkZ << 4) + 8);
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
