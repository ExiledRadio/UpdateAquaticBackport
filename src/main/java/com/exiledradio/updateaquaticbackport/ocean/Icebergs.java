package com.exiledradio.updateaquaticbackport.ocean;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14.4's {@code IcebergFeature} and {@code BlueIceFeature}, line for line, for 1.12.2's world.
 *
 * <p>An iceberg is centered at sea level on the given column and reaches at most 11 blocks out, so a
 * caller that centers it 12-19 blocks into the area it decorates keeps it inside the four loaded chunks.
 * Blocks are set without neighbor or observer updates: 1.12.2's flag 2 alone still reads all six
 * neighbors for observers, which would load the next chunk for a block on the area's edge.
 *
 * <p>The cave carved into an iceberg can reach up to 16 blocks from its center, past the four loaded
 * chunks; only blocks in loaded chunks are carved, so it never loads a chunk (1.13 decorates a 3 x 3 chunk
 * region and has the room).
 */
public final class Icebergs {

    /** Send to clients, no neighbor updates, no observer updates. */
    private static final int FLAGS = 2 | 16;

    private Icebergs() {
    }

    /**
     * Places one iceberg.
     *
     * @param ice     the iceberg's block: packed ice, or blue ice for the rare blue ones
     * @param blueIce blue ice, counted as iceberg material when carving; null when there is none
     */
    public static void iceberg(World world, Random rand, BlockPos column, IBlockState ice, @Nullable Block blueIce) {
        BlockPos pos = new BlockPos(column.getX(), world.getSeaLevel(), column.getZ());
        boolean snowy = rand.nextDouble() > 0.7D;
        double angle = rand.nextDouble() * 2.0D * Math.PI;
        int ellipseRadius = 11 - rand.nextInt(5);
        int tipRadius = 3 + rand.nextInt(3);
        boolean ellipse = rand.nextDouble() > 0.7D;
        int height = ellipse ? rand.nextInt(6) + 6 : rand.nextInt(15) + 3;
        if (!ellipse && rand.nextDouble() > 0.9D) {
            height += rand.nextInt(19) + 7;
        }
        int depth = Math.min(height + rand.nextInt(11), 18);
        int width = Math.min(height + rand.nextInt(7) - rand.nextInt(5), 11);
        int radius = ellipse ? ellipseRadius : 11;

        for (int x = -radius; x < radius; ++x) {
            for (int z = -radius; z < radius; ++z) {
                for (int y = 0; y < height; ++y) {
                    int r = ellipse ? ellipseHeight(y, height, width) : height(rand, y, height, width);
                    if (ellipse || x < r) {
                        place(world, rand, pos, height, x, y, z, r, radius, ellipse, tipRadius, angle, snowy, ice);
                    }
                }
            }
        }

        smooth(world, pos, width, height, ellipse, ellipseRadius, blueIce);

        for (int x = -radius; x < radius; ++x) {
            for (int z = -radius; z < radius; ++z) {
                for (int y = -1; y > -depth; --y) {
                    int r = ellipse ? MathHelper.ceil(radius * (1.0F - (float) Math.pow(y, 2.0D) / (depth * 8.0F))) : radius;
                    int below = belowHeight(rand, -y, depth, width);
                    if (x < below) {
                        place(world, rand, pos, depth, x, y, z, below, r, ellipse, tipRadius, angle, snowy, ice);
                    }
                }
            }
        }

        boolean cave = ellipse ? rand.nextDouble() > 0.1D : rand.nextDouble() > 0.7D;
        if (cave) {
            carve(rand, world, width, height, pos, ellipse, ellipseRadius, angle, tipRadius, blueIce);
        }
    }

    private static void carve(Random rand, World world, int width, int height, BlockPos pos, boolean ellipse,
                              int ellipseRadius, double angle, int tipRadius, @Nullable Block blueIce) {
        int sx = rand.nextBoolean() ? -1 : 1;
        int sz = rand.nextBoolean() ? -1 : 1;
        int dx = rand.nextInt(Math.max(width / 2 - 2, 1));
        if (rand.nextBoolean()) {
            dx = width / 2 + 1 - rand.nextInt(Math.max(width - width / 2 - 1, 1));
        }
        int dz = rand.nextInt(Math.max(width / 2 - 2, 1));
        if (rand.nextBoolean()) {
            dz = width / 2 + 1 - rand.nextInt(Math.max(width - width / 2 - 1, 1));
        }
        if (ellipse) {
            dx = dz = rand.nextInt(Math.max(ellipseRadius - 5, 1));
        }
        BlockPos offset = new BlockPos(sx * dx, 0, sz * dz);
        double caveAngle = ellipse ? angle + Math.PI / 2.0D : rand.nextDouble() * 2.0D * Math.PI;

        for (int y = 0; y < height - 3; ++y) {
            int r = height(rand, y, height, width);
            carveLayer(r, y, pos, world, false, caveAngle, offset, ellipseRadius, tipRadius, blueIce);
        }
        for (int y = -1; y > -height + rand.nextInt(5); --y) {
            int r = belowHeight(rand, -y, height, width);
            carveLayer(r, y, pos, world, true, caveAngle, offset, ellipseRadius, tipRadius, blueIce);
        }
    }

    private static void carveLayer(int r, int y, BlockPos pos, World world, boolean underwater, double angle,
                                   BlockPos offset, int ellipseRadius, int tipRadius, @Nullable Block blueIce) {
        int a = r + 1 + ellipseRadius / 3;
        int b = Math.min(r - 3, 3) + tipRadius / 2 - 1;
        for (int x = -a; x < a; ++x) {
            for (int z = -a; z < a; ++z) {
                if (ellipse(x, z, offset, a, b, angle) < 0.0D) {
                    BlockPos at = pos.add(x, y, z);
                    if (!world.isBlockLoaded(at) || !world.isBlockLoaded(at.up())) {
                        continue;
                    }
                    Block block = world.getBlockState(at).getBlock();
                    if (isIce(block, blueIce) || block == Blocks.SNOW) {
                        if (underwater) {
                            world.setBlockState(at, Blocks.WATER.getDefaultState(), FLAGS);
                        } else {
                            world.setBlockState(at, Blocks.AIR.getDefaultState(), FLAGS);
                            removeSnowLayer(world, at);
                        }
                    }
                }
            }
        }
    }

    private static void removeSnowLayer(World world, BlockPos pos) {
        if (world.getBlockState(pos.up()).getBlock() == Blocks.SNOW_LAYER) {
            world.setBlockState(pos.up(), Blocks.AIR.getDefaultState(), FLAGS);
        }
    }

    private static void place(World world, Random rand, BlockPos pos, int height, int x, int y, int z, int r,
                              int radius, boolean ellipse, int tipRadius, double angle, boolean snowy, IBlockState ice) {
        double d = ellipse ? ellipse(x, z, BlockPos.ORIGIN, radius, tipHeight(y, height, tipRadius), angle)
                : circle(x, z, BlockPos.ORIGIN, r, rand);
        if (d < 0.0D) {
            BlockPos at = pos.add(x, y, z);
            double edge = ellipse ? -0.5D : (double) (-6 - rand.nextInt(3));
            if (d > edge && rand.nextDouble() > 0.9D) {
                return;
            }
            set(at, world, rand, height - y, height, ellipse, snowy, ice);
        }
    }

    private static void set(BlockPos pos, World world, Random rand, int fromTop, int height, boolean ellipse,
                            boolean snowy, IBlockState ice) {
        IBlockState state = world.getBlockState(pos);
        Block block = state.getBlock();
        if (state.getMaterial() == Material.AIR || block == Blocks.SNOW || block == Blocks.ICE || block == Blocks.WATER) {
            boolean notTip = !ellipse || rand.nextDouble() > 0.05D;
            int share = ellipse ? 3 : 2;
            if (snowy && block != Blocks.WATER && fromTop <= rand.nextInt(Math.max(1, height / share)) + height * 0.6D
                    && notTip) {
                world.setBlockState(pos, Blocks.SNOW.getDefaultState(), FLAGS);
            } else {
                world.setBlockState(pos, ice, FLAGS);
            }
        }
    }

    private static int tipHeight(int y, int height, int tipRadius) {
        int r = tipRadius;
        if (y > 0 && height - y <= 3) {
            r = tipRadius - (4 - (height - y));
        }
        return r;
    }

    private static double circle(int x, int z, BlockPos center, int r, Random rand) {
        float f = 10.0F * MathHelper.clamp(rand.nextFloat(), 0.2F, 0.8F) / r;
        return f + Math.pow(x - center.getX(), 2.0D) + Math.pow(z - center.getZ(), 2.0D) - Math.pow(r, 2.0D);
    }

    private static double ellipse(int x, int z, BlockPos center, int a, int b, double angle) {
        double dx = x - center.getX();
        double dz = z - center.getZ();
        return Math.pow((dx * Math.cos(angle) - dz * Math.sin(angle)) / a, 2.0D)
                + Math.pow((dx * Math.sin(angle) + dz * Math.cos(angle)) / b, 2.0D) - 1.0D;
    }

    private static int height(Random rand, int y, int height, int width) {
        float f = 3.5F - rand.nextFloat();
        float f1 = (1.0F - (float) Math.pow(y, 2.0D) / (height * f)) * width;
        if (height > 15 + rand.nextInt(5)) {
            int i = y < 3 + rand.nextInt(6) ? y / 2 : y;
            f1 = (1.0F - i / (height * f * 0.4F)) * width;
        }
        return MathHelper.ceil(f1 / 2.0F);
    }

    private static int ellipseHeight(int y, int height, int width) {
        float f1 = (1.0F - (float) Math.pow(y, 2.0D) / height) * width;
        return MathHelper.ceil(f1 / 2.0F);
    }

    private static int belowHeight(Random rand, int y, int depth, int width) {
        float f = 1.0F + rand.nextFloat() / 2.0F;
        float f1 = (1.0F - y / (depth * f)) * width;
        return MathHelper.ceil(f1 / 2.0F);
    }

    private static boolean isIce(Block block, @Nullable Block blueIce) {
        return block == Blocks.PACKED_ICE || block == Blocks.SNOW || (blueIce != null && block == blueIce);
    }

    /** Removes ice left floating over air or standing alone after the iceberg is built. */
    private static void smooth(World world, BlockPos pos, int width, int height, boolean ellipse, int ellipseRadius,
                               @Nullable Block blueIce) {
        int r = ellipse ? ellipseRadius : width / 2;
        for (int x = -r; x <= r; ++x) {
            for (int z = -r; z <= r; ++z) {
                for (int y = 0; y <= height; ++y) {
                    BlockPos at = pos.add(x, y, z);
                    Block block = world.getBlockState(at).getBlock();
                    if (isIce(block, blueIce) || block == Blocks.SNOW_LAYER) {
                        if (world.getBlockState(at.down()).getMaterial() == Material.AIR) {
                            world.setBlockState(at, Blocks.AIR.getDefaultState(), FLAGS);
                            world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), FLAGS);
                        } else if (isIce(block, blueIce)) {
                            int open = 0;
                            for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
                                if (!isIce(world.getBlockState(at.offset(side)).getBlock(), blueIce)) {
                                    ++open;
                                }
                            }
                            if (open >= 3) {
                                world.setBlockState(at, Blocks.AIR.getDefaultState(), FLAGS);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 1.14.4's {@code BlueIceFeature}: under water and touching packed ice, a blue ice block, then up to 200
     * tries to grow more blue ice touching it within a few blocks.
     *
     * @return true if anything was placed
     */
    public static boolean blueIce(World world, Random rand, BlockPos pos, Block blueIce) {
        if (pos.getY() > world.getSeaLevel() - 1) {
            return false;
        }
        if (world.getBlockState(pos).getBlock() != Blocks.WATER && world.getBlockState(pos.down()).getBlock() != Blocks.WATER) {
            return false;
        }
        boolean touching = false;
        for (EnumFacing side : EnumFacing.values()) {
            if (side != EnumFacing.DOWN && world.getBlockState(pos.offset(side)).getBlock() == Blocks.PACKED_ICE) {
                touching = true;
                break;
            }
        }
        if (!touching) {
            return false;
        }
        world.setBlockState(pos, blueIce.getDefaultState(), FLAGS);
        for (int i = 0; i < 200; ++i) {
            int dy = rand.nextInt(5) - rand.nextInt(6);
            int spread = 3;
            if (dy < 2) {
                spread += dy / 2;
            }
            if (spread >= 1) {
                BlockPos at = pos.add(rand.nextInt(spread) - rand.nextInt(spread), dy, rand.nextInt(spread) - rand.nextInt(spread));
                IBlockState state = world.getBlockState(at);
                Block block = state.getBlock();
                if (state.getMaterial() == Material.AIR || block == Blocks.WATER || block == Blocks.PACKED_ICE || block == Blocks.ICE) {
                    for (EnumFacing side : EnumFacing.values()) {
                        if (world.getBlockState(at.offset(side)).getBlock() == blueIce) {
                            world.setBlockState(at, blueIce.getDefaultState(), FLAGS);
                            break;
                        }
                    }
                }
            }
        }
        return true;
    }
}
