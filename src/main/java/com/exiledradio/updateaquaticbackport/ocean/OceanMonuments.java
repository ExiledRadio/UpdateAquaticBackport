package com.exiledradio.updateaquaticbackport.ocean;

import net.minecraft.init.Biomes;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Vanilla's ocean monument, placed by 1.13's biome rule.
 *
 * <p>1.12.2 only builds a monument on vanilla's deep ocean, with nothing but vanilla's water biomes for
 * 29 blocks around; with deep oceans split by temperature it would hardly ever find one. 1.14.4's
 * {@code OceanMonumentStructure} asks instead that the center be any deep ocean and the surroundings any
 * ocean or river. Everything else - the spacing (32 chunks, 5 apart), the seed, the structure, its
 * guardians and its saved name ("Monument") - is vanilla's own, so existing monuments are untouched.
 */
final class OceanMonuments extends StructureOceanMonument {

    /** Vanilla's defaults; the overworld generator always builds the monument with them. */
    private static final int SPACING = 32, SEPARATION = 5;

    private final List<Biome> spawnBiomes = new ArrayList<Biome>();
    private final List<Biome> waterBiomes = new ArrayList<Biome>();

    OceanMonuments() {
        this.spawnBiomes.add(Biomes.DEEP_OCEAN);
        for (Biome biome : OceanContent.biomes()) {
            if (biome instanceof BiomeAquaticOcean && ((BiomeAquaticOcean) biome).isDeep()) {
                this.spawnBiomes.add(biome);
            }
        }
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                this.waterBiomes.add(biome);
            }
        }
    }

    List<Biome> spawnBiomes() {
        return this.spawnBiomes;
    }

    @Override
    protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
        int x = chunkX < 0 ? chunkX - (SPACING - 1) : chunkX;
        int z = chunkZ < 0 ? chunkZ - (SPACING - 1) : chunkZ;
        int cellX = x / SPACING;
        int cellZ = z / SPACING;
        Random random = this.world.setRandomSeed(cellX, cellZ, 10387313);
        int startX = cellX * SPACING + (random.nextInt(SPACING - SEPARATION) + random.nextInt(SPACING - SEPARATION)) / 2;
        int startZ = cellZ * SPACING + (random.nextInt(SPACING - SEPARATION) + random.nextInt(SPACING - SEPARATION)) / 2;
        if (chunkX != startX || chunkZ != startZ) {
            return false;
        }
        return this.world.getBiomeProvider().areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 16, this.spawnBiomes)
                && this.world.getBiomeProvider().areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 29, this.waterBiomes);
    }
}
