package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.backportcore.worldgen.MonumentTerrain;

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
 * ocean or river, both squares measured from the chunk's block (9, 9). Everything else - the spacing (32 chunks, 5 apart), the seed, the structure, its
 * guardians and its saved name ("Monument") - is vanilla's own, so existing monuments are untouched.
 *
 * <p>Where the terrain does not follow the biome map, the monument also needs deep water where it stands (Backport
 * Core's {@link MonumentTerrain}, from Core 1.4.5): in Realistic Terrain Generation worlds, whose terrain is built
 * apart from the biome map, the biome rule alone put monuments on land and in lakes.
 */
final class OceanMonuments extends StructureOceanMonument {

    /** Vanilla's defaults; the overworld generator always builds the monument with them. */
    private static final int SPACING = 32, SEPARATION = 5;

    private final List<Biome> spawnBiomes = new ArrayList<Biome>();
    private final List<Biome> waterBiomes = new ArrayList<Biome>();

    /** False once an older Backport Core turned out to have no {@link MonumentTerrain}. */
    private static boolean coreTerrain = true;

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
        // 1.13 measures both squares from the chunk's block (9, 9); for the 29-block square that starts one 1:4
        // cell later than 1.12.2's (8, 8) does.
        return this.world.getBiomeProvider().areBiomesViable(chunkX * 16 + 9, chunkZ * 16 + 9, 16, this.spawnBiomes)
                && this.world.getBiomeProvider().areBiomesViable(chunkX * 16 + 9, chunkZ * 16 + 9, 29, this.waterBiomes)
                && deepEnough(chunkX, chunkZ);
    }

    private boolean deepEnough(int chunkX, int chunkZ) {
        if (coreTerrain) {
            try {
                return MonumentTerrain.deepEnough(this.world, chunkX, chunkZ);
            } catch (NoClassDefFoundError e) {
                coreTerrain = false;
            }
        }
        return true;
    }
}
