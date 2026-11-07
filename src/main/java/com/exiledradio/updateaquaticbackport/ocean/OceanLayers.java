package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerVoronoiZoom;
import net.minecraft.world.gen.layer.GenLayerZoom;
import net.minecraft.world.gen.layer.IntCache;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.terraingen.WorldTypeEvent;

import java.util.Random;

/**
 * Puts 1.13's ocean temperatures into the biome map, through Forge's biome generation event.
 *
 * <h2>How 1.13 does it</h2>
 * Beside the land layers runs an ocean temperature map: improved noise sampled once per 256 blocks
 * ({@code OceanLayer}: above 0.4 warm, above 0.2 lukewarm, below -0.4 frozen, below -0.2 cold, else
 * normal), zoomed six times to the biome map's 4-block scale. At the very end, after rivers are mixed in,
 * {@code MixOceansLayer} gives every ocean cell the temperature under it, with two exceptions within 8
 * cells (32 blocks) of land: warm becomes lukewarm and frozen becomes cold, so warm and frozen water never
 * touch a coast. Deep oceans take the deep form of their temperature, except warm, which stays a
 * (shallow) warm ocean: 1.13 never generated its deep warm ocean, and today's game has removed it.
 *
 * <h2>How this does it</h2>
 * The same two layers, appended to whatever map the world type built: 1.12.2's final map is the same
 * river-mixed map 1.13 mixes oceans into, so nothing else in the chain changes and other mods' changes
 * to it survive. Only vanilla's own ocean, deep ocean and frozen ocean cells are replaced; other mods'
 * ocean biomes are left as they are, and count as ocean (not land) for the coast rule. The block-scale map
 * is rebuilt from the result with vanilla's Voronoi zoom so the two always agree; if another mod has
 * replaced that layer with its own, its output is mixed the same way at block scale instead.
 */
final class OceanLayers {

    /** Temperature codes in the private map. */
    static final int WARM = 1, LUKEWARM = 2, NORMAL = 3, COLD = 4, FROZEN = 5;

    /** 1.14.4's seeds: the temperature layer's context, its zooms and the mixing layer's. */
    private static final long TEMPERATURE_SEED = 2L, ZOOM_SEED = 2001L, MIX_SEED = 100L;

    /** Zoom steps from the 256-block temperature cells to the biome map's 4-block cells. */
    private static final int ZOOMS = 6;

    private OceanLayers() {
    }

    static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        GenLayer[] layers = event.getNewBiomeGens();
        if (layers == null || layers.length < 2 || event.getWorldType() == WorldType.FLAT
                || event.getWorldType() == WorldType.DEBUG_ALL_BLOCK_STATES) {
            return;
        }
        Ids ids = new Ids();
        GenLayer temperature = GenLayerZoom.magnify(ZOOM_SEED, new Temperature(TEMPERATURE_SEED), ZOOMS);
        Mix biomes = new Mix(MIX_SEED, layers[0], temperature, 1, ids);
        biomes.initWorldGenSeed(event.getSeed());

        GenLayer[] replaced = layers.clone();
        replaced[0] = biomes;
        if (layers[1].getClass() == GenLayerVoronoiZoom.class) {
            replaced[1] = new GenLayerVoronoiZoom(10L, biomes);
        } else {
            // Somebody else's block-scale layer: mix at block scale, two zooms finer and four times wider.
            GenLayer fine = GenLayerZoom.magnify(ZOOM_SEED, new Temperature(TEMPERATURE_SEED), ZOOMS + 2);
            replaced[1] = new Mix(MIX_SEED, layers[1], fine, 4, ids);
        }
        replaced[1].initWorldGenSeed(event.getSeed());
        if (replaced.length > 2 && layers[2] == layers[0]) {
            replaced[2] = biomes;
        }
        event.setNewBiomeGens(replaced);
        UpdateAquaticBackport.LOGGER.debug("Ocean temperatures added to the biome map ({})", event.getWorldType().getName());
    }

    /** The biome ids the mixing layer reads and writes, resolved once per world. */
    static final class Ids {
        final int ocean = Biome.getIdForBiome(Biomes.OCEAN);
        final int deepOcean = Biome.getIdForBiome(Biomes.DEEP_OCEAN);
        final int frozenOcean = Biome.getIdForBiome(Biomes.FROZEN_OCEAN);
        final int warm = Biome.getIdForBiome(OceanContent.warm());
        final int lukewarm = Biome.getIdForBiome(OceanContent.lukewarm());
        final int deepLukewarm = Biome.getIdForBiome(OceanContent.deepLukewarm());
        final int cold = Biome.getIdForBiome(OceanContent.cold());
        final int deepCold = Biome.getIdForBiome(OceanContent.deepCold());
        final int frozen = Biome.getIdForBiome(OceanContent.frozen());
        final int deepFrozen = Biome.getIdForBiome(OceanContent.deepFrozen());

        /** Per biome id: 0 land, 1 another mod's ocean, 2 vanilla shallow ocean, 3 vanilla deep ocean. */
        private final byte[] kinds = new byte[256];

        Ids() {
            for (int id = 0; id < this.kinds.length; id++) {
                this.kinds[id] = classify(id);
            }
        }

        private byte classify(int id) {
            if (id == this.deepOcean) {
                return 3;
            }
            if (id == this.ocean || id == this.frozenOcean) {
                return 2;
            }
            Biome biome = Biome.getBiome(id);
            return biome != null && BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) ? (byte) 1 : 0;
        }

        int kind(int id) {
            return id >= 0 && id < this.kinds.length ? this.kinds[id] : classify(id);
        }

        /** @return 1.14.4's MixOceansLayer result for an ocean cell of this kind and temperature */
        int mixed(int land, int temperature, boolean nearLand) {
            if (nearLand) {
                if (temperature == WARM) {
                    return this.lukewarm;
                }
                if (temperature == FROZEN) {
                    return this.cold;
                }
            }
            boolean deep = kind(land) == 3;
            switch (temperature) {
                case WARM:
                    return this.warm;
                case LUKEWARM:
                    return deep ? this.deepLukewarm : this.lukewarm;
                case COLD:
                    return deep ? this.deepCold : this.cold;
                case FROZEN:
                    return deep ? this.deepFrozen : this.frozen;
                default:
                    return deep ? this.deepOcean : this.ocean;
            }
        }
    }

    /** 1.14.4's OceanLayer: the temperature of each 256-block cell, from improved noise seeded by the world seed. */
    static final class Temperature extends GenLayer {

        private ImprovedNoise noise = new ImprovedNoise(new Random(0L));

        Temperature(long seed) {
            super(seed);
        }

        @Override
        public void initWorldGenSeed(long seed) {
            super.initWorldGenSeed(seed);
            this.noise = new ImprovedNoise(new Random(seed));
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int[] out = IntCache.getIntCache(areaWidth * areaHeight);
            for (int z = 0; z < areaHeight; z++) {
                for (int x = 0; x < areaWidth; x++) {
                    out[x + z * areaWidth] = classify(this.noise.sample((areaX + x) / 8.0D, (areaY + z) / 8.0D, 0.0D));
                }
            }
            return out;
        }

        static int classify(double t) {
            if (t > 0.4D) {
                return WARM;
            }
            if (t > 0.2D) {
                return LUKEWARM;
            }
            if (t < -0.4D) {
                return FROZEN;
            }
            return t < -0.2D ? COLD : NORMAL;
        }
    }

    /** 1.14.4's MixOceansLayer over a finished map; {@code scale} 1 at the 4-block map, 4 at block scale. */
    static final class Mix extends GenLayer {

        private final GenLayer temperature;
        private final int scale;
        private final Ids ids;

        Mix(long seed, GenLayer biomes, GenLayer temperature, int scale, Ids ids) {
            super(seed);
            this.parent = biomes;
            this.temperature = temperature;
            this.scale = scale;
            this.ids = ids;
        }

        @Override
        public void initWorldGenSeed(long seed) {
            super.initWorldGenSeed(seed);
            this.temperature.initWorldGenSeed(seed);
        }

        @Override
        public int[] getInts(int areaX, int areaY, int areaWidth, int areaHeight) {
            int reach = 8 * this.scale;
            int step = 4 * this.scale;
            int wide = areaWidth + 2 * reach;
            int[] land = this.parent.getInts(areaX - reach, areaY - reach, wide, areaHeight + 2 * reach);
            int[] temps = this.temperature.getInts(areaX, areaY, areaWidth, areaHeight);
            int[] out = IntCache.getIntCache(areaWidth * areaHeight);
            for (int z = 0; z < areaHeight; z++) {
                for (int x = 0; x < areaWidth; x++) {
                    int id = land[(x + reach) + (z + reach) * wide];
                    int kind = this.ids.kind(id);
                    if (kind < 2) {
                        out[x + z * areaWidth] = id;
                        continue;
                    }
                    int t = temps[x + z * areaWidth];
                    boolean nearLand = false;
                    if (t == WARM || t == FROZEN) {
                        for (int dx = -reach; dx <= reach && !nearLand; dx += step) {
                            for (int dz = -reach; dz <= reach; dz += step) {
                                if (this.ids.kind(land[(x + reach + dx) + (z + reach + dz) * wide]) == 0) {
                                    nearLand = true;
                                    break;
                                }
                            }
                        }
                    }
                    out[x + z * areaWidth] = this.ids.mixed(id, t, nearLand);
                }
            }
            return out;
        }
    }

    /** 1.14.4's ImprovedNoiseGenerator (Ken Perlin's improved noise), as OceanLayer samples it. */
    static final class ImprovedNoise {

        private static final int[][] GRADIENTS = {{1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0}, {1, 0, 1}, {-1, 0, 1},
                {1, 0, -1}, {-1, 0, -1}, {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1}, {1, 1, 0}, {0, -1, 1},
                {-1, 1, 0}, {0, -1, -1}};

        private final byte[] permutations = new byte[256];
        private final double xo;
        private final double yo;
        private final double zo;

        ImprovedNoise(Random random) {
            this.xo = random.nextDouble() * 256.0D;
            this.yo = random.nextDouble() * 256.0D;
            this.zo = random.nextDouble() * 256.0D;
            for (int i = 0; i < 256; ++i) {
                this.permutations[i] = (byte) i;
            }
            for (int k = 0; k < 256; ++k) {
                int j = random.nextInt(256 - k);
                byte b = this.permutations[k];
                this.permutations[k] = this.permutations[k + j];
                this.permutations[k + j] = b;
            }
        }

        double sample(double x, double y, double z) {
            double d0 = x + this.xo;
            double d1 = y + this.yo;
            double d2 = z + this.zo;
            int i = MathHelper.floor(d0);
            int j = MathHelper.floor(d1);
            int k = MathHelper.floor(d2);
            double fx = d0 - i;
            double fy = d1 - j;
            double fz = d2 - k;
            return noise(i, j, k, fx, fy, fz, fade(fx), fade(fy), fade(fz));
        }

        private int perm(int i) {
            return this.permutations[i & 255] & 255;
        }

        private static double grad(int hash, double x, double y, double z) {
            int[] g = GRADIENTS[hash & 15];
            return g[0] * x + g[1] * y + g[2] * z;
        }

        private double noise(int x, int y, int z, double fx, double fy, double fz, double u, double v, double w) {
            int i = perm(x) + y;
            int j = perm(i) + z;
            int k = perm(i + 1) + z;
            int l = perm(x + 1) + y;
            int i1 = perm(l) + z;
            int j1 = perm(l + 1) + z;
            double d0 = grad(perm(j), fx, fy, fz);
            double d1 = grad(perm(i1), fx - 1.0D, fy, fz);
            double d2 = grad(perm(k), fx, fy - 1.0D, fz);
            double d3 = grad(perm(j1), fx - 1.0D, fy - 1.0D, fz);
            double d4 = grad(perm(j + 1), fx, fy, fz - 1.0D);
            double d5 = grad(perm(i1 + 1), fx - 1.0D, fy, fz - 1.0D);
            double d6 = grad(perm(k + 1), fx, fy - 1.0D, fz - 1.0D);
            double d7 = grad(perm(j1 + 1), fx - 1.0D, fy - 1.0D, fz - 1.0D);
            return lerp(w, lerp(v, lerp(u, d0, d1), lerp(u, d2, d3)), lerp(v, lerp(u, d4, d5), lerp(u, d6, d7)));
        }

        private static double lerp(double t, double a, double b) {
            return a + t * (b - a);
        }

        private static double fade(double t) {
            return t * t * t * (t * (t * 6.0D - 15.0D) + 10.0D);
        }
    }
}
