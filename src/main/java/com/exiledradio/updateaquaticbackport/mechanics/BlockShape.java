package com.exiledradio.updateaquaticbackport.mechanics;

import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

/**
 * A block's 1.13 outline shape as edges, block-relative (0 to 1), for the outline drawn around the block looked at
 * (1.13, 17w47a: "blocks with a collision box now have matching bounding boxes").
 *
 * <p>1.13 outlines a block by its shape ({@code VoxelShape}), and draws the shape's edges: the lines where its
 * surface turns, not the boxes it was built from. 1.12.2 outlines one box. Here:
 * <ul>
 *   <li>a hopper, anvil and cauldron get 1.14.4's shapes (their 1.12.2 collision is a plain box or misses the
 *       spout);</li>
 *   <li>any other block whose collision is two or more boxes (stairs, fences, panes, iron bars, brewing stands, end
 *       portal frames with an eye, piston heads, chorus plants, other mods' blocks) is outlined by those boxes, cut at
 *       the top of the block (fences and walls collide 1.5 high; their 1.13 outline is 1 high);</li>
 *   <li>everything else (one box: full blocks, slabs, soul sand, snow) keeps 1.12.2's outline: {@link #edges} gives
 *       null.</li>
 * </ul>
 * Edges come from the shape on the grid of its own box coordinates, as {@code VoxelShape.forEachEdge}: a grid line is
 * an edge where the four cells around it are not all filled or all empty, and not two side by side.
 */
public final class BlockShape {

    private final List<AxisAlignedBB> add = new ArrayList<AxisAlignedBB>();
    private final List<AxisAlignedBB> sub = new ArrayList<AxisAlignedBB>();
    private final List<AxisAlignedBB> addAfter = new ArrayList<AxisAlignedBB>();

    private BlockShape() {
    }

    private static AxisAlignedBB px(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AxisAlignedBB(x1 / 16, y1 / 16, z1 / 16, x2 / 16, y2 / 16, z2 / 16);
    }

    /**
     * @return the outline's edges as {x1, y1, z1, x2, y2, z2} each, block-relative, or null to keep 1.12.2's box
     */
    @Nullable
    public static List<double[]> edges(World world, BlockPos pos, IBlockState state, @Nullable Entity looker) {
        BlockShape shape = shape(world, pos, state, looker);
        return shape == null ? null : shape.edges();
    }

    @Nullable
    static BlockShape shape(World world, BlockPos pos, IBlockState state, @Nullable Entity looker) {
        BlockShape s = new BlockShape();
        if (state.getBlock() instanceof BlockHopper) {
            // 1.14.4's HopperBlock: the input and middle, less the bowl, and the spout.
            s.add.add(px(0, 10, 0, 16, 16, 16));
            s.add.add(px(4, 4, 4, 12, 10, 12));
            s.sub.add(px(2, 11, 2, 14, 16, 14));
            EnumFacing facing = state.getValue(BlockHopper.FACING);
            switch (facing) {
                case DOWN:
                    s.addAfter.add(px(6, 0, 6, 10, 4, 10));
                    break;
                case EAST:
                    s.addAfter.add(px(12, 4, 6, 16, 8, 10));
                    break;
                case NORTH:
                    s.addAfter.add(px(6, 4, 0, 10, 8, 4));
                    break;
                case SOUTH:
                    s.addAfter.add(px(6, 4, 12, 10, 8, 16));
                    break;
                case WEST:
                    s.addAfter.add(px(0, 4, 6, 4, 8, 10));
                    break;
                default:
                    break;
            }
            return s;
        }
        if (state.getBlock() instanceof BlockAnvil) {
            s.add.add(px(2, 0, 2, 14, 4, 14));
            if (state.getValue(BlockAnvil.FACING).getAxis() == EnumFacing.Axis.X) {
                s.add.addAll(Arrays.asList(px(3, 4, 4, 13, 5, 12), px(4, 5, 6, 12, 10, 10), px(0, 10, 3, 16, 16, 13)));
            } else {
                s.add.addAll(Arrays.asList(px(4, 4, 3, 12, 5, 13), px(6, 5, 4, 10, 10, 12), px(3, 10, 0, 13, 16, 16)));
            }
            return s;
        }
        if (state.getBlock() instanceof BlockCauldron) {
            s.add.add(new AxisAlignedBB(0, 0, 0, 1, 1, 1));
            s.sub.addAll(Arrays.asList(px(0, 0, 4, 16, 3, 12), px(4, 0, 0, 12, 3, 16), px(2, 0, 2, 14, 3, 14), px(2, 4, 2, 14, 16, 14)));
            return s;
        }
        List<AxisAlignedBB> boxes = new ArrayList<AxisAlignedBB>();
        AxisAlignedBB around = new AxisAlignedBB(pos).grow(1.0D);
        state.addCollisionBoxToList(world, pos, around, boxes, looker, false);
        int kept = 0;
        for (AxisAlignedBB box : boxes) {
            AxisAlignedBB b = box.offset(-pos.getX(), -pos.getY(), -pos.getZ());
            double maxY = Math.min(b.maxY, 1.0D);
            if (maxY - b.minY > 1.0E-7D) {
                s.add.add(new AxisAlignedBB(b.minX, b.minY, b.minZ, b.maxX, maxY, b.maxZ));
                kept++;
            }
        }
        return kept >= 2 ? s : null;
    }

    private boolean filled(double x, double y, double z) {
        for (AxisAlignedBB b : this.addAfter) {
            if (inside(b, x, y, z)) {
                return true;
            }
        }
        boolean in = false;
        for (AxisAlignedBB b : this.add) {
            if (inside(b, x, y, z)) {
                in = true;
                break;
            }
        }
        if (!in) {
            return false;
        }
        for (AxisAlignedBB b : this.sub) {
            if (inside(b, x, y, z)) {
                return false;
            }
        }
        return true;
    }

    private static boolean inside(AxisAlignedBB b, double x, double y, double z) {
        return x > b.minX && x < b.maxX && y > b.minY && y < b.maxY && z > b.minZ && z < b.maxZ;
    }

    List<double[]> edges() {
        TreeSet<Double> xs = new TreeSet<Double>();
        TreeSet<Double> ys = new TreeSet<Double>();
        TreeSet<Double> zs = new TreeSet<Double>();
        for (List<AxisAlignedBB> list : Arrays.asList(this.add, this.sub, this.addAfter)) {
            for (AxisAlignedBB b : list) {
                xs.add(b.minX);
                xs.add(b.maxX);
                ys.add(b.minY);
                ys.add(b.maxY);
                zs.add(b.minZ);
                zs.add(b.maxZ);
            }
        }
        double[] x = unbox(xs);
        double[] y = unbox(ys);
        double[] z = unbox(zs);
        boolean[][][] cell = new boolean[x.length - 1][y.length - 1][z.length - 1];
        for (int i = 0; i < x.length - 1; i++) {
            for (int j = 0; j < y.length - 1; j++) {
                for (int k = 0; k < z.length - 1; k++) {
                    cell[i][j][k] = filled((x[i] + x[i + 1]) / 2, (y[j] + y[j + 1]) / 2, (z[k] + z[k + 1]) / 2);
                }
            }
        }
        List<double[]> out = new ArrayList<double[]>();
        // Edges along x: grid lines (j, k), segments i.
        for (int j = 0; j < y.length; j++) {
            for (int k = 0; k < z.length; k++) {
                int start = -1;
                for (int i = 0; i <= x.length - 1; i++) {
                    boolean edge = i < x.length - 1 && edge(get(cell, i, j - 1, k - 1), get(cell, i, j, k - 1), get(cell, i, j - 1, k), get(cell, i, j, k));
                    if (edge && start < 0) {
                        start = i;
                    } else if (!edge && start >= 0) {
                        out.add(new double[] {x[start], y[j], z[k], x[i], y[j], z[k]});
                        start = -1;
                    }
                }
            }
        }
        // Edges along y: grid lines (i, k), segments j.
        for (int i = 0; i < x.length; i++) {
            for (int k = 0; k < z.length; k++) {
                int start = -1;
                for (int j = 0; j <= y.length - 1; j++) {
                    boolean edge = j < y.length - 1 && edge(get(cell, i - 1, j, k - 1), get(cell, i, j, k - 1), get(cell, i - 1, j, k), get(cell, i, j, k));
                    if (edge && start < 0) {
                        start = j;
                    } else if (!edge && start >= 0) {
                        out.add(new double[] {x[i], y[start], z[k], x[i], y[j], z[k]});
                        start = -1;
                    }
                }
            }
        }
        // Edges along z: grid lines (i, j), segments k.
        for (int i = 0; i < x.length; i++) {
            for (int j = 0; j < y.length; j++) {
                int start = -1;
                for (int k = 0; k <= z.length - 1; k++) {
                    boolean edge = k < z.length - 1 && edge(get(cell, i - 1, j - 1, k), get(cell, i, j - 1, k), get(cell, i - 1, j, k), get(cell, i, j, k));
                    if (edge && start < 0) {
                        start = k;
                    } else if (!edge && start >= 0) {
                        out.add(new double[] {x[i], y[j], z[start], x[i], y[j], z[k]});
                        start = -1;
                    }
                }
            }
        }
        return out;
    }

    /**
     * The four cells around a grid line, as a, b (one row) and c, d (the next): an edge unless all four are the same or
     * they are two filled side by side (a flat face goes on through the line).
     */
    private static boolean edge(boolean a, boolean b, boolean c, boolean d) {
        int n = (a ? 1 : 0) + (b ? 1 : 0) + (c ? 1 : 0) + (d ? 1 : 0);
        if (n == 0 || n == 4) {
            return false;
        }
        if (n == 2) {
            // Diagonal pairs (a with d, b with c) meet only along the line: an edge. Side-by-side pairs: a flat face.
            return a == d;
        }
        return true;
    }

    private static boolean get(boolean[][][] cell, int i, int j, int k) {
        return i >= 0 && j >= 0 && k >= 0 && i < cell.length && j < cell[0].length && k < cell[0][0].length && cell[i][j][k];
    }

    private static double[] unbox(TreeSet<Double> set) {
        double[] out = new double[set.size()];
        int n = 0;
        for (double d : set) {
            out[n++] = d;
        }
        return out;
    }
}
