package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.NoiseGeneratorPerlin;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The frozen ocean and the deep frozen ocean.
 *
 * <h2>Temperature</h2>
 * 1.13 gives both oceans a patchy temperature (today's game calls it the "frozen" temperature modifier):
 * where two noises agree, the temperature is 0.2 instead of the biome's own, so a frozen ocean (0.0) has
 * open water in patches and a deep frozen ocean (0.5) none frozen at all. 1.12.2's
 * {@code Biome.getTemperature} is final, so {@link #temperatureAt} computes it, and this stream uses it
 * wherever the world is generated: the surface ice laid with the terrain, the ice and snow laid at
 * population ({@link OceanTerrain#freeze}). During play vanilla's weather sees the biome's own
 * temperature, so open patches of a frozen ocean can freeze over in time; a deep frozen ocean never does.
 *
 * <h2>Icebergs</h2>
 * Two kinds, as 1.13:
 * <ul>
 *   <li>the large packed ice masses with snow on top raised by 1.14.4's {@code FrozenOceanSurfaceBuilder}
 *       as the terrain is laid ({@link #genTerrainBlocks});</li>
 *   <li>decorated icebergs of packed ice (one chunk in 16) and of blue ice (one in 200), and blue ice
 *       growing on packed ice under water ({@link Icebergs}), at decoration.</li>
 * </ul>
 * Blue ice is the owning mod's when it is installed, else this mod's copy; with neither there is none.
 */
public final class BiomeFrozenOcean extends BiomeAquaticOcean {

    /** 1.14.4's FrozenOceanBiome noise: three octaves, seed 3456. */
    private static final NoiseGeneratorPerlin PATCH_NOISE = new NoiseGeneratorPerlin(new Random(3456L), 3);

    private static final IBlockState PACKED_ICE = Blocks.PACKED_ICE.getDefaultState();
    private static final IBlockState SNOW_BLOCK = Blocks.SNOW.getDefaultState();

    /** The surface builder's two noises, seeded by the world seed; rebuilt when the seed changes. */
    private static long noiseSeed;
    @Nullable
    private static NoiseGeneratorPerlin heightNoise;
    @Nullable
    private static NoiseGeneratorPerlin roofNoise;

    BiomeFrozenOcean(BiomeProperties properties, boolean deep, int squidWeight, int squidMin, int squidMax) {
        super(properties, OceanKind.FROZEN, deep, Blocks.GRASS.getDefaultState(), Blocks.DIRT.getDefaultState(),
                GRAVEL, squidWeight, squidMin, squidMax);
    }

    /** @return 1.13's temperature at a position: the patch rule, then vanilla's cooling with height */
    public float temperatureAt(BlockPos pos) {
        float t = this.getDefaultTemperature();
        double d0 = PATCH_NOISE.getValue(pos.getX() * 0.05D, pos.getZ() * 0.05D);
        double d1 = GRASS_COLOR_NOISE.getValue(pos.getX() * 0.2D, pos.getZ() * 0.2D);
        if (d0 + d1 < 0.3D && GRASS_COLOR_NOISE.getValue(pos.getX() * 0.09D, pos.getZ() * 0.09D) < 0.8D) {
            t = 0.2F;
        }
        if (pos.getY() > 64) {
            float f = (float) (TEMPERATURE_NOISE.getValue(pos.getX() / 8.0F, pos.getZ() / 8.0F) * 4.0D);
            return t - (f + pos.getY() - 64.0F) * 0.05F / 30.0F;
        }
        return t;
    }

    private static synchronized void seed(long seed) {
        if (heightNoise == null || roofNoise == null || noiseSeed != seed) {
            Random random = new Random(seed);
            heightNoise = new NoiseGeneratorPerlin(random, 4);
            roofNoise = new NoiseGeneratorPerlin(random, 1);
            noiseSeed = seed;
        }
    }

    /**
     * 1.14.4's {@code FrozenOceanSurfaceBuilder}: the terrain as vanilla lays it, plus packed ice raised
     * above and below the surface where the noise is high, capped with a few blocks of snow above the
     * height a snow line would be.
     *
     * <p>Written at {@code (z & 15, y, x & 15)} like vanilla's, so the columns line up with their
     * neighbors; bedrock is laid here as vanilla lays it.
     */
    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise) {
        seed(world.getSeed());
        int sea = world.getSeaLevel();
        double top = 0.0D;
        double bottom = 0.0D;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        float surfaceTemperature = temperatureAt(pos.setPos(x, 63, z));
        double d2 = Math.min(Math.abs(noise), heightNoise.getValue(x * 0.1D, z * 0.1D));
        if (d2 > 1.8D) {
            double roof = Math.abs(roofNoise.getValue(x * 0.09765625D, z * 0.09765625D));
            top = d2 * d2 * 1.2D;
            double cap = Math.ceil(roof * 40.0D) + 14.0D;
            if (top > cap) {
                top = cap;
            }
            if (surfaceTemperature > 0.1F) {
                top -= 2.0D;
            }
            if (top > 2.0D) {
                bottom = sea - top - 7.0D;
                top = top + sea;
            } else {
                top = 0.0D;
            }
        }

        int lx = x & 15;
        int lz = z & 15;
        IBlockState under = this.fillerBlock;
        IBlockState surface = this.topBlock;
        int thickness = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int depth = -1;
        int snow = 0;
        int snowLayers = 2 + rand.nextInt(4);
        int snowLine = sea + 18 + rand.nextInt(10);

        for (int y = 255; y >= 0; --y) {
            if (y <= rand.nextInt(5)) {
                primer.setBlockState(lz, y, lx, BEDROCK);
                continue;
            }
            IBlockState here = primer.getBlockState(lz, y, lx);
            if (here.getMaterial() == Material.AIR && y < (int) top && rand.nextDouble() > 0.01D) {
                primer.setBlockState(lz, y, lx, PACKED_ICE);
            } else if (here.getMaterial() == Material.WATER && y > (int) bottom && y < sea && bottom != 0.0D
                    && rand.nextDouble() > 0.15D) {
                primer.setBlockState(lz, y, lx, PACKED_ICE);
            }

            here = primer.getBlockState(lz, y, lx);
            if (here.getMaterial() == Material.AIR) {
                depth = -1;
            } else if (here.getBlock() != Blocks.STONE) {
                if (here.getBlock() == Blocks.PACKED_ICE && snow <= snowLayers && y > snowLine) {
                    primer.setBlockState(lz, y, lx, SNOW_BLOCK);
                    ++snow;
                }
            } else if (depth == -1) {
                if (thickness <= 0) {
                    surface = AIR;
                    under = STONE;
                } else if (y >= sea - 4 && y <= sea + 1) {
                    surface = this.topBlock;
                    under = this.fillerBlock;
                }
                if (y < sea && (surface == null || surface.getMaterial() == Material.AIR)) {
                    surface = temperatureAt(pos.setPos(x, y, z)) < FREEZING ? ICE : WATER;
                }
                depth = thickness;
                if (y >= sea - 1) {
                    primer.setBlockState(lz, y, lx, surface);
                } else if (y < sea - 7 - thickness) {
                    surface = AIR;
                    under = STONE;
                    primer.setBlockState(lz, y, lx, GRAVEL);
                } else {
                    primer.setBlockState(lz, y, lx, under);
                }
            } else if (depth > 0) {
                --depth;
                primer.setBlockState(lz, y, lx, under);
                if (depth == 0 && under.getBlock() == Blocks.SAND && thickness > 1) {
                    depth = rand.nextInt(4) + Math.max(0, y - 63);
                    under = under.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
                }
            }
        }
    }

    /**
     * Icebergs and blue ice, then vanilla's decoration (ores, disks), in 1.13's order. Positions keep inside
     * the area vanilla decorates (the chunk offset by 8), so nothing reaches an unloaded neighbor.
     */
    @Override
    public void decorate(World world, Random rand, BlockPos pos) {
        Block blueIce = Owners.block(Owners.VILLAGE_AND_PILLAGE, "blue_ice");
        if (rand.nextFloat() < 1.0F / 16.0F) {
            Icebergs.iceberg(world, rand, pos.add(12 + rand.nextInt(8), 0, 12 + rand.nextInt(8)), PACKED_ICE, blueIce);
        }
        if (blueIce != null && rand.nextFloat() < 1.0F / 200.0F) {
            Icebergs.iceberg(world, rand, pos.add(12 + rand.nextInt(8), 0, 12 + rand.nextInt(8)),
                    blueIce.getDefaultState(), blueIce);
        }
        if (blueIce != null) {
            int count = rand.nextInt(20);
            for (int i = 0; i < count; i++) {
                Icebergs.blueIce(world, rand, pos.add(8 + rand.nextInt(16), 30 + rand.nextInt(32), 8 + rand.nextInt(16)), blueIce);
            }
        }
        super.decorate(world, rand, pos);
    }
}
