package com.exiledradio.updateaquaticbackport.kelp;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Kelp forests and seagrass meadows in newly generated chunks, with 1.14.4's numbers.
 *
 * <h2>Where</h2>
 * By {@link OceanKind}, so it follows 1.13's ocean biomes when they are installed and the temperature
 * noise when they are not:
 * <table>
 *   <tr><th></th><th>kelp (per chunk, noise-biased)</th><th>seagrass tries, tall share</th></tr>
 *   <tr><td>warm</td><td>none</td><td>80, 30%</td></tr>
 *   <tr><td>lukewarm</td><td>up to 80</td><td>80, 30% (deep: 80%)</td></tr>
 *   <tr><td>normal</td><td>up to 80</td><td>48, 30% (deep: 80%)</td></tr>
 *   <tr><td>cold</td><td>up to 120</td><td>32, 30% (deep: 40, 80%)</td></tr>
 *   <tr><td>frozen</td><td>none</td><td>none</td></tr>
 *   <tr><td>river</td><td>none</td><td>48, 40%</td></tr>
 *   <tr><td>swamp</td><td>none</td><td>64, 60%</td></tr>
 * </table>
 * Warm oceans have seagrass as today's game does (1.14.4 had none there).
 *
 * <h2>How</h2>
 * Kelp: as 1.14.4's {@code KelpFeature} and {@code TOP_SOLID_HEIGHTMAP_NOISE_BIASED} placement - the
 * number of tries is {@code ceil(noise(x/80, z/80) * ratio)}, so kelp comes in forests with bare floor
 * between; each try grows a column of 1-11 from the ocean floor that stops below the surface. Seagrass:
 * as {@code SeaGrassFeature}, scattered within 8 blocks of the chunk's center. Positions stay inside the
 * area vanilla decorates (the chunk offset by 8), so nothing generates into an unloaded neighbor.
 */
public final class KelpGenerator implements IWorldGenerator {

    /** 1.14.4's Biome.INFO_NOISE: one octave, seed 2345. */
    private static final NoiseGeneratorPerlin INFO_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!KelpContent.enabled() || !ModConfig.kelp.generate || !world.provider.isSurfaceWorld()) {
            return;
        }
        BlockPos center = new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
        Biome biome = world.getBiome(center.add(8, 0, 8));
        OceanKind kind = OceanKind.at(world, center.add(8, 0, 8));
        boolean deep = biome.getBaseHeight() < -1.5F;

        int kelpRatio = 0;
        int seagrass = 0;
        double tall = 0.3;
        switch (kind) {
            case WARM:
                seagrass = 80;
                break;
            case LUKEWARM:
                kelpRatio = 80;
                seagrass = 80;
                tall = deep ? 0.8 : 0.3;
                break;
            case NORMAL:
                kelpRatio = 80;
                seagrass = 48;
                tall = deep ? 0.8 : 0.3;
                break;
            case COLD:
                kelpRatio = 120;
                seagrass = deep ? 40 : 32;
                tall = deep ? 0.8 : 0.3;
                break;
            case FROZEN:
                break;
            default:
                if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                    seagrass = 48;
                    tall = 0.4;
                } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SWAMP)) {
                    seagrass = 64;
                    tall = 0.6;
                }
                break;
        }

        if (kelpRatio > 0) {
            int tries = (int) Math.ceil(INFO_NOISE.getValue(center.getX() / 80.0, center.getZ() / 80.0) * kelpRatio);
            for (int i = 0; i < tries; i++) {
                BlockPos floor = oceanFloor(world, center.getX() + random.nextInt(16), center.getZ() + random.nextInt(16));
                OceanKind here = OceanKind.at(world, floor);
                if (here != OceanKind.WARM && here != OceanKind.FROZEN && here.isOcean()) {
                    kelpColumn(world, floor, random);
                }
            }
        }
        for (int i = 0; i < seagrass; i++) {
            int x = center.getX() + 8 + random.nextInt(8) - random.nextInt(8);
            int z = center.getZ() + 8 + random.nextInt(8) - random.nextInt(8);
            seagrassAt(world, oceanFloor(world, x, z), random, tall);
        }
    }

    /** @return the first position above the highest block that blocks movement, under any water */
    public static BlockPos oceanFloor(World world, int x, int z) {
        int y = world.getHeight(x, z);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, y, z);
        while (pos.getY() > 0) {
            pos.setY(pos.getY() - 1);
            if (world.getBlockState(pos).getMaterial().blocksMovement()) {
                return pos.up();
            }
        }
        return new BlockPos(x, 0, z);
    }

    /** 1.14.4's KelpFeature at one floor position. @return true if anything was placed */
    public static boolean kelpColumn(World world, BlockPos floor, Random random) {
        BlockKelp top = KelpContent.kelp();
        BlockKelp plant = KelpContent.kelpPlant();
        if (!BlockKelp.isVanillaWaterSource(world, floor)) {
            return false;
        }
        int height = 1 + random.nextInt(10);
        BlockPos pos = floor;
        boolean placed = false;
        for (int l = 0; l <= height; l++) {
            if (BlockKelp.isVanillaWaterSource(world, pos) && BlockKelp.isVanillaWaterSource(world, pos.up()) && plant.hasSupport(world, pos)) {
                if (l == height) {
                    // 1.14.4 gives a generated top age 0-22; here 0-13 of 16.
                    world.setBlockState(pos, top.getDefaultState().withProperty(BlockKelp.AGE, random.nextInt(14)), 2);
                } else {
                    world.setBlockState(pos, plant.getDefaultState(), 2);
                }
                placed = true;
            } else {
                if (l > 0) {
                    BlockPos below = pos.down();
                    if (top.hasSupport(world, below) && world.getBlockState(below.down()).getBlock() != top) {
                        world.setBlockState(below, top.getDefaultState().withProperty(BlockKelp.AGE, random.nextInt(14)), 2);
                    }
                }
                break;
            }
            pos = pos.up();
        }
        return placed;
    }

    /** 1.14.4's SeaGrassFeature at one floor position. */
    public static boolean seagrassAt(World world, BlockPos floor, Random random, double tallChance) {
        BlockSeagrass shortForm = KelpContent.seagrass();
        BlockSeagrass tallForm = KelpContent.tallSeagrass();
        if (!BlockKelp.isVanillaWaterSource(world, floor) || !BlockSeagrass.solidGround(world, floor.down())) {
            return false;
        }
        if (random.nextDouble() < tallChance) {
            if (BlockKelp.isVanillaWaterSource(world, floor.up())) {
                IBlockState lower = tallForm.getDefaultState().withProperty(BlockSeagrass.HALF, BlockSeagrass.Half.LOWER);
                world.setBlockState(floor, lower, 2);
                world.setBlockState(floor.up(), lower.withProperty(BlockSeagrass.HALF, BlockSeagrass.Half.UPPER), 2);
                return true;
            }
            return false;
        }
        world.setBlockState(floor, shortForm.getDefaultState(), 2);
        return true;
    }
}
