package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.gen.MapGenBase;

import java.util.BitSet;
import java.util.Random;

/**
 * 1.13's underwater caves and underwater ravines, after whatever carves the ordinary ones.
 *
 * <h2>How 1.13 does it</h2>
 * Vanilla's caves and ravines refuse to cut anywhere near water, so ocean floors are never breached. 1.13
 * gives every ocean biome two more carvers, {@code UnderwaterCaveWorldCarver} (chance 1 in 15 per start
 * chunk) and {@code UnderwaterCanyonWorldCarver} (1 in 50), run after the ordinary ones in chunks whose
 * biome is an ocean. They take the same shapes as caves and ravines but carve through sand, gravel and
 * water as well, and fill what they carve with water. At y = 10 they leave obsidian, or a magma block one
 * time in four; below that, lava. Nothing at or above sea level is touched.
 *
 * <h2>Here</h2>
 * The ravine generator is wrapped (Forge's map generator event), so this runs right after the world's
 * caves and ravines, whoever made them. The shapes, seeds and numbers are 1.14.4's. Magma is only placed
 * when this mod's bubble columns exist: 1.12.2's magma block deletes the water above it on a random tick,
 * which would drain the caves; without them it is obsidian. Water carved next to air (an ordinary cave,
 * or the chunk edge) is noted and made to flow once the chunk is populated ({@link OceanTerrain}), as
 * 1.13 schedules a fluid tick there.
 */
public final class UnderwaterCarvers extends MapGenBase {

    /** 1.14.4's ocean carver chances (ProbabilityConfig). */
    private static final float CANYON_CHANCE = 0.02F, CAVE_CHANCE = 0.06666667F;

    /** 1.14.4's carver range, in chunks. */
    private static final int RANGE = 8;

    private final MapGenBase wrapped;

    UnderwaterCarvers(MapGenBase wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public void generate(World world, int chunkX, int chunkZ, ChunkPrimer primer) {
        this.wrapped.generate(world, chunkX, chunkZ, primer);
        if (!ModConfig.oceans.underwaterCaves) {
            return;
        }
        Biome biome = world.getBiomeProvider().getBiome(new BlockPos(chunkX * 16, 0, chunkZ * 16));
        if (OceanKind.of(biome, world.getSeed(), chunkX * 16, chunkZ * 16).isOcean()) {
            carve(world, chunkX, chunkZ, primer);
        }
    }

    /** Carves one chunk's primer with every underwater carver that starts within range. */
    public static void carve(World world, int chunkX, int chunkZ, ChunkPrimer primer) {
        long seed = world.getSeed();
        Target target = new Target(primer, chunkX, chunkZ, world.getSeaLevel(), Owners.ourBlock("bubble_column") != null);
        Random random = new Random();
        Canyon canyon = new Canyon();
        Cave cave = new Cave();
        for (int x = chunkX - RANGE; x <= chunkX + RANGE; ++x) {
            for (int z = chunkZ - RANGE; z <= chunkZ + RANGE; ++z) {
                // 1.14.4's liquid carver list for an ocean: the canyon (index 0), then the cave (index 1).
                largeFeatureSeed(random, seed, x, z);
                if (random.nextFloat() <= CANYON_CHANCE) {
                    canyon.carve(target, random, x, z);
                }
                largeFeatureSeed(random, seed + 1L, x, z);
                if (random.nextFloat() <= CAVE_CHANCE) {
                    cave.carve(target, random, x, z);
                }
            }
        }
        OceanTerrain.noteFlowing(world, chunkX, chunkZ, target.flowing, target.flowingCount);
    }

    /** 1.14.4's SharedSeedRandom.setLargeFeatureSeed. */
    static void largeFeatureSeed(Random random, long seed, int x, int z) {
        random.setSeed(seed);
        long a = random.nextLong();
        long b = random.nextLong();
        random.setSeed((long) x * a ^ (long) z * b ^ seed);
    }

    /** The chunk being carved, its carving mask, and the water it has placed next to air. */
    static final class Target {
        final ChunkPrimer primer;
        final int chunkX;
        final int chunkZ;
        final int seaLevel;
        final boolean magma;
        final BitSet mask = new BitSet(16 * 16 * 256);
        int[] flowing = new int[64];
        int flowingCount;

        Target(ChunkPrimer primer, int chunkX, int chunkZ, int seaLevel, boolean magma) {
            this.primer = primer;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.seaLevel = seaLevel;
            this.magma = magma;
        }

        void noteFlowing(int x, int y, int z) {
            if (this.flowingCount == this.flowing.length) {
                int[] grown = new int[this.flowing.length * 2];
                System.arraycopy(this.flowing, 0, grown, 0, this.flowingCount);
                this.flowing = grown;
            }
            this.flowing[this.flowingCount++] = x | z << 4 | y << 8;
        }
    }

    /** 1.14.4's WorldCarver with the underwater carvers' block rule. */
    abstract static class Carver {

        /** @return true if this point of the shape is outside it (1.14.4's func_222708_a) */
        abstract boolean skip(double dx, double dy, double dz, int y);

        /** @return true for blocks this carver may replace */
        abstract boolean carvable(IBlockState state);

        /** 1.14.4's func_222702_a: whether a tunnel step can still reach the target chunk. */
        static boolean inRange(Target target, double x, double z, int step, int steps, float radius) {
            double dx = x - (target.chunkX * 16 + 8);
            double dz = z - (target.chunkZ * 16 + 8);
            double left = steps - step;
            double reach = radius + 2.0F + 16.0F;
            return dx * dx + dz * dz - left * left <= reach * reach;
        }

        /** 1.14.4's func_222705_a: carves one ellipsoid of a tunnel where it overlaps the target chunk. */
        final boolean carveRegion(Target target, long seed, double x, double y, double z, double hRadius, double vRadius) {
            Random random = new Random(seed + (long) target.chunkX + (long) target.chunkZ);
            double cx = target.chunkX * 16 + 8;
            double cz = target.chunkZ * 16 + 8;
            if (x < cx - 16.0D - hRadius * 2.0D || z < cz - 16.0D - hRadius * 2.0D
                    || x > cx + 16.0D + hRadius * 2.0D || z > cz + 16.0D + hRadius * 2.0D) {
                return false;
            }
            int minX = Math.max(MathHelper.floor(x - hRadius) - target.chunkX * 16 - 1, 0);
            int maxX = Math.min(MathHelper.floor(x + hRadius) - target.chunkX * 16 + 1, 16);
            int minY = Math.max(MathHelper.floor(y - vRadius) - 1, 1);
            int maxY = Math.min(MathHelper.floor(y + vRadius) + 1, 256 - 8);
            int minZ = Math.max(MathHelper.floor(z - hRadius) - target.chunkZ * 16 - 1, 0);
            int maxZ = Math.min(MathHelper.floor(z + hRadius) - target.chunkZ * 16 + 1, 16);
            // The underwater carvers never refuse a region for the water in it.
            boolean carved = false;
            for (int lx = minX; lx < maxX; ++lx) {
                double dx = (lx + target.chunkX * 16 + 0.5D - x) / hRadius;
                for (int lz = minZ; lz < maxZ; ++lz) {
                    double dz = (lz + target.chunkZ * 16 + 0.5D - z) / hRadius;
                    if (dx * dx + dz * dz >= 1.0D) {
                        continue;
                    }
                    for (int ly = maxY; ly > minY; --ly) {
                        double dy = (ly - 0.5D - y) / vRadius;
                        if (!skip(dx, dy, dz, ly)) {
                            carved |= carveBlock(target, random, lx, ly, lz);
                        }
                    }
                }
            }
            return carved;
        }

        /** 1.14.4's UnderwaterCaveWorldCarver.func_222728_a. */
        private boolean carveBlock(Target target, Random random, int x, int y, int z) {
            if (y >= target.seaLevel) {
                return false;
            }
            int index = x | z << 4 | y << 8;
            if (target.mask.get(index)) {
                return false;
            }
            target.mask.set(index);
            ChunkPrimer primer = target.primer;
            if (!carvable(primer.getBlockState(x, y, z))) {
                return false;
            }
            if (y == 10) {
                boolean magma = random.nextFloat() < 0.25F && target.magma;
                primer.setBlockState(x, y, z, (magma ? Blocks.MAGMA : Blocks.OBSIDIAN).getDefaultState());
                return true;
            }
            if (y < 10) {
                primer.setBlockState(x, y, z, Blocks.LAVA.getDefaultState());
                return false;
            }
            primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState());
            if (x == 0 || x == 15 || z == 0 || z == 15 || isAir(primer, x - 1, y, z) || isAir(primer, x + 1, y, z)
                    || isAir(primer, x, y, z - 1) || isAir(primer, x, y, z + 1) || isAir(primer, x, y - 1, z)) {
                target.noteFlowing(x, y, z);
            }
            return true;
        }

        private static boolean isAir(ChunkPrimer primer, int x, int y, int z) {
            return y >= 0 && primer.getBlockState(x, y, z).getMaterial() == Material.AIR;
        }

        /** 1.14.4's carvable block list, as 1.12.2 names them. */
        static boolean carvableBase(Block block) {
            return block == Blocks.STONE || block == Blocks.DIRT || block == Blocks.GRASS || block == Blocks.HARDENED_CLAY
                    || block == Blocks.STAINED_HARDENED_CLAY || block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE
                    || block == Blocks.MYCELIUM || block == Blocks.SNOW_LAYER || block == Blocks.SAND
                    || block == Blocks.GRAVEL || block == Blocks.WATER || block == Blocks.FLOWING_WATER
                    || block == Blocks.LAVA || block == Blocks.FLOWING_LAVA || block == Blocks.OBSIDIAN
                    || block == Blocks.AIR;
        }
    }

    /** 1.14.4's CaveWorldCarver shapes, carving as the underwater cave does. */
    static final class Cave extends Carver {

        @Override
        boolean carvable(IBlockState state) {
            Block block = state.getBlock();
            return carvableBase(block) || block == Blocks.PACKED_ICE;
        }

        @Override
        boolean skip(double dx, double dy, double dz, int y) {
            return dy <= -0.7D || dx * dx + dy * dy + dz * dz >= 1.0D;
        }

        void carve(Target target, Random rand, int chunkX, int chunkZ) {
            int length = (4 * 2 - 1) * 16;
            int count = rand.nextInt(rand.nextInt(rand.nextInt(15) + 1) + 1);
            for (int k = 0; k < count; ++k) {
                double x = chunkX * 16 + rand.nextInt(16);
                double y = rand.nextInt(rand.nextInt(120) + 8);
                double z = chunkZ * 16 + rand.nextInt(16);
                int tunnels = 1;
                if (rand.nextInt(4) == 0) {
                    float size = 1.0F + rand.nextFloat() * 6.0F;
                    double h = 1.5D + MathHelper.sin((float) Math.PI / 2F) * size;
                    carveRegion(target, rand.nextLong(), x + 1.0D, y, z, h, h * 0.5D);
                    tunnels += rand.nextInt(4);
                }
                for (int t = 0; t < tunnels; ++t) {
                    float yaw = rand.nextFloat() * ((float) Math.PI * 2F);
                    float pitch = (rand.nextFloat() - 0.5F) / 4.0F;
                    float radius = rand.nextFloat() * 2.0F + rand.nextFloat();
                    if (rand.nextInt(10) == 0) {
                        radius *= rand.nextFloat() * rand.nextFloat() * 3.0F + 1.0F;
                    }
                    int steps = length - rand.nextInt(length / 4);
                    tunnel(target, rand.nextLong(), x, y, z, radius, yaw, pitch, 0, steps, 1.0D);
                }
            }
        }

        private void tunnel(Target target, long seed, double x, double y, double z, float radius, float yaw, float pitch,
                            int start, int steps, double squash) {
            Random random = new Random(seed);
            int branchAt = random.nextInt(steps / 2) + steps / 4;
            boolean steep = random.nextInt(6) == 0;
            float yawTurn = 0.0F;
            float pitchTurn = 0.0F;
            for (int step = start; step < steps; ++step) {
                double h = 1.5D + MathHelper.sin((float) Math.PI * step / steps) * radius;
                double v = h * squash;
                float cos = MathHelper.cos(pitch);
                x += MathHelper.cos(yaw) * cos;
                y += MathHelper.sin(pitch);
                z += MathHelper.sin(yaw) * cos;
                pitch = pitch * (steep ? 0.92F : 0.7F);
                pitch = pitch + pitchTurn * 0.1F;
                yaw += yawTurn * 0.1F;
                pitchTurn = pitchTurn * 0.9F;
                yawTurn = yawTurn * 0.75F;
                pitchTurn = pitchTurn + (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
                yawTurn = yawTurn + (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
                if (step == branchAt && radius > 1.0F) {
                    tunnel(target, random.nextLong(), x, y, z, random.nextFloat() * 0.5F + 0.5F,
                            yaw - (float) Math.PI / 2F, pitch / 3.0F, step, steps, 1.0D);
                    tunnel(target, random.nextLong(), x, y, z, random.nextFloat() * 0.5F + 0.5F,
                            yaw + (float) Math.PI / 2F, pitch / 3.0F, step, steps, 1.0D);
                    return;
                }
                if (random.nextInt(4) != 0) {
                    if (!inRange(target, x, z, step, steps, radius)) {
                        return;
                    }
                    carveRegion(target, seed, x, y, z, h, v);
                }
            }
        }
    }

    /** 1.14.4's CanyonWorldCarver shapes, carving as the underwater canyon does. */
    static final class Canyon extends Carver {

        private final float[] widths = new float[1024];

        @Override
        boolean carvable(IBlockState state) {
            return carvableBase(state.getBlock());
        }

        @Override
        boolean skip(double dx, double dy, double dz, int y) {
            return (dx * dx + dz * dz) * this.widths[y - 1] + dy * dy / 6.0D >= 1.0D;
        }

        void carve(Target target, Random rand, int chunkX, int chunkZ) {
            int length = (4 * 2 - 1) * 16;
            double x = chunkX * 16 + rand.nextInt(16);
            double y = rand.nextInt(rand.nextInt(40) + 8) + 20;
            double z = chunkZ * 16 + rand.nextInt(16);
            float yaw = rand.nextFloat() * ((float) Math.PI * 2F);
            float pitch = (rand.nextFloat() - 0.5F) * 2.0F / 8.0F;
            float radius = (rand.nextFloat() * 2.0F + rand.nextFloat()) * 2.0F;
            int steps = length - rand.nextInt(length / 4);
            canyon(target, rand.nextLong(), x, y, z, radius, yaw, pitch, 0, steps, 3.0D);
        }

        private void canyon(Target target, long seed, double x, double y, double z, float radius, float yaw, float pitch,
                            int start, int steps, double squash) {
            Random random = new Random(seed);
            float f = 1.0F;
            for (int i = 0; i < 256; ++i) {
                if (i == 0 || random.nextInt(3) == 0) {
                    f = 1.0F + random.nextFloat() * random.nextFloat();
                }
                this.widths[i] = f * f;
            }
            float yawTurn = 0.0F;
            float pitchTurn = 0.0F;
            for (int step = start; step < steps; ++step) {
                double h = 1.5D + MathHelper.sin(step * (float) Math.PI / steps) * radius;
                double v = h * squash;
                h = h * (random.nextFloat() * 0.25D + 0.75D);
                v = v * (random.nextFloat() * 0.25D + 0.75D);
                float cos = MathHelper.cos(pitch);
                float sin = MathHelper.sin(pitch);
                x += MathHelper.cos(yaw) * cos;
                y += sin;
                z += MathHelper.sin(yaw) * cos;
                pitch = pitch * 0.7F;
                pitch = pitch + pitchTurn * 0.05F;
                yaw += yawTurn * 0.05F;
                pitchTurn = pitchTurn * 0.8F;
                yawTurn = yawTurn * 0.5F;
                pitchTurn = pitchTurn + (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 2.0F;
                yawTurn = yawTurn + (random.nextFloat() - random.nextFloat()) * random.nextFloat() * 4.0F;
                if (random.nextInt(4) != 0) {
                    if (!inRange(target, x, z, step, steps, radius)) {
                        return;
                    }
                    carveRegion(target, seed, x, y, z, h, v);
                }
            }
        }
    }
}
