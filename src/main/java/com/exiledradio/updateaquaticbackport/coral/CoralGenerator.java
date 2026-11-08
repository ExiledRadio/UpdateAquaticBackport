package com.exiledradio.updateaquaticbackport.coral;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.fml.common.IWorldGenerator;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Coral reefs in warm oceans, from 1.14.4's {@code CoralTreeFeature}, {@code CoralClawFeature},
 * {@code CoralMushroomFeature} and {@code SeaPickleFeature}, as the warm ocean places them:
 *
 * <ul>
 *   <li>{@code ceil(noise(x/400, z/400) * 20)} reef pieces per chunk, each a tree, claw or mushroom of one
 *       random living coral block, rising from the ocean floor;</li>
 *   <li>on each coral block placed, a quarter of the time a coral or fan on top, else 5% a sea pickle, and
 *       on each open side 20% a wall fan;</li>
 *   <li>one chunk in 16 gets up to 20 sea pickles scattered on the floor.</li>
 * </ul>
 * Sea pickles are the owner's when it is installed, else this mod's (through {@code Owners}); with neither
 * there are no pickles.
 */
public final class CoralGenerator implements IWorldGenerator {

    private static final NoiseGeneratorPerlin INFO_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!CoralContent.enabled() || !ModConfig.coral.generate || !world.provider.isSurfaceWorld()) {
            return;
        }
        BlockPos center = new BlockPos(chunkX * 16 + 8, 0, chunkZ * 16 + 8);
        if (OceanKind.at(world, center.add(8, 0, 8)) != OceanKind.WARM) {
            return;
        }
        int pieces = (int) Math.ceil(INFO_NOISE.getValue(center.getX() / 400.0, center.getZ() / 400.0) * 20);
        for (int i = 0; i < pieces; i++) {
            BlockPos floor = floor(world, center.getX() + random.nextInt(16), center.getZ() + random.nextInt(16));
            if (OceanKind.at(world, floor) != OceanKind.WARM) {
                continue;
            }
            IBlockState coral = randomOf(new ArrayList<Block>(CoralContent.BLOCKS.values()), random).getDefaultState();
            switch (random.nextInt(3)) {
                case 0:
                    tree(world, random, floor, coral);
                    break;
                case 1:
                    claw(world, random, floor, coral);
                    break;
                default:
                    mushroom(world, random, floor, coral);
                    break;
            }
        }
        if (random.nextInt(16) == 0) {
            for (int i = 0; i < 20; i++) {
                int x = center.getX() + 8 + random.nextInt(8) - random.nextInt(8);
                int z = center.getZ() + 8 + random.nextInt(8) - random.nextInt(8);
                BlockPos at = floor(world, x, z);
                if (BlockCoral.isVanillaWaterSource(world, at)) {
                    IBlockState pickle = seaPickle(random.nextInt(4) + 1);
                    if (pickle != null && pickle.getBlock().canPlaceBlockAt(world, at)) {
                        Waterlogging.placeKeepingWater(world, at, pickle, 2);
                    }
                }
            }
        }
    }

    /** @return the first position above the highest block that blocks movement other than coral */
    static BlockPos floor(World world, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, world.getHeight(x, z), z);
        while (pos.getY() > 0) {
            pos.setY(pos.getY() - 1);
            IBlockState state = world.getBlockState(pos);
            if (state.getMaterial().blocksMovement() && !(state.getBlock() instanceof BlockCoral)) {
                return pos.up();
            }
        }
        return new BlockPos(x, 0, z);
    }

    /**
     * A sea pickle state with this many pickles, waterlogged: the owner's or ours, matched by property
     * name so the owner's class is never linked.
     */
    @Nullable
    @SuppressWarnings({"rawtypes", "unchecked"})
    static IBlockState seaPickle(int count) {
        Block block = Owners.block(Owners.VILLAGE_AND_PILLAGE, "sea_pickle");
        if (block == null) {
            return null;
        }
        IBlockState state = block.getDefaultState();
        for (IProperty p : state.getPropertyKeys()) {
            if ("pickles".equals(p.getName())) {
                state = state.withProperty(p, Integer.valueOf(count));
            } else if ("waterlogged".equals(p.getName())) {
                // Placed already marked wet, which the pickle turns into real water on its first update.
                // Placing it dry and adding the water as a second change made a light update reach into
                // the next chunks mid-generation, which stopped kelp, seagrass and ruins generating there.
                state = state.withProperty(p, Boolean.TRUE);
            }
        }
        return state;
    }

    private static <T> T randomOf(List<T> list, Random random) {
        return list.get(random.nextInt(list.size()));
    }

    // ------------------------------------------------------------------------------------------
    // 1.14.4's CoralFeature pieces
    // ------------------------------------------------------------------------------------------

    /** 1.14.4's func_204624_b: one coral block here, with what grows on and around it. */
    public static boolean coralAt(World world, Random random, BlockPos pos, IBlockState coral) {
        IBlockState here = world.getBlockState(pos);
        boolean open = BlockCoral.isVanillaWaterSource(world, pos)
                || CoralContent.CORALS.containsValue(here.getBlock()) || CoralContent.FANS.containsValue(here.getBlock());
        if (!open || !BlockCoral.isVanillaWaterSource(world, pos.up())) {
            return false;
        }
        world.setBlockState(pos, coral, 3);
        BlockPos up = pos.up();
        if (random.nextFloat() < 0.25F) {
            List<Block> tops = new ArrayList<Block>(CoralContent.CORALS.values());
            tops.addAll(CoralContent.FANS.values());
            Waterlogging.placeKeepingWater(world, up, randomOf(tops, random).getDefaultState(), 2);
        } else if (random.nextFloat() < 0.05F) {
            IBlockState pickle = seaPickle(random.nextInt(4) + 1);
            if (pickle != null) {
                Waterlogging.placeKeepingWater(world, up, pickle, 2);
            }
        }
        for (EnumFacing side : EnumFacing.Plane.HORIZONTAL) {
            if (random.nextFloat() < 0.2F) {
                BlockPos beside = pos.offset(side);
                if (BlockCoral.isVanillaWaterSource(world, beside)) {
                    Waterlogging.placeKeepingWater(world, beside, randomOf(new ArrayList<Block>(CoralContent.WALL_FANS.values()), random)
                            .getDefaultState().withProperty(BlockCoralWallFan.FACING, side), 2);
                }
            }
        }
        return true;
    }

    public static void tree(World world, Random random, BlockPos start, IBlockState coral) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start);
        int trunk = random.nextInt(3) + 1;
        for (int j = 0; j < trunk; j++) {
            if (!coralAt(world, random, pos, coral)) {
                return;
            }
            pos.move(EnumFacing.UP);
        }
        BlockPos top = pos.toImmutable();
        int branches = random.nextInt(3) + 2;
        List<EnumFacing> sides = new ArrayList<EnumFacing>();
        Collections.addAll(sides, EnumFacing.HORIZONTALS);
        Collections.shuffle(sides, random);
        for (EnumFacing side : sides.subList(0, branches)) {
            pos.setPos(top);
            pos.move(side);
            int length = random.nextInt(5) + 2;
            int sinceStep = 0;
            for (int j = 0; j < length && coralAt(world, random, pos, coral); j++) {
                sinceStep++;
                pos.move(EnumFacing.UP);
                if (j == 0 || sinceStep >= 2 && random.nextFloat() < 0.25F) {
                    pos.move(side);
                    sinceStep = 0;
                }
            }
        }
    }

    public static void claw(World world, Random random, BlockPos start, IBlockState coral) {
        if (!coralAt(world, random, start, coral)) {
            return;
        }
        EnumFacing main = EnumFacing.HORIZONTALS[random.nextInt(4)];
        int arms = random.nextInt(2) + 2;
        List<EnumFacing> list = new ArrayList<EnumFacing>();
        Collections.addAll(list, main, main.rotateY(), main.rotateYCCW());
        Collections.shuffle(list, random);
        for (EnumFacing arm : list.subList(0, arms)) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start);
            int reach = random.nextInt(2) + 1;
            pos.move(arm);
            int length;
            EnumFacing grow;
            if (arm == main) {
                grow = main;
                length = random.nextInt(3) + 2;
            } else {
                pos.move(EnumFacing.UP);
                grow = random.nextBoolean() ? arm : EnumFacing.UP;
                length = random.nextInt(3) + 3;
            }
            for (int l = 0; l < reach && coralAt(world, random, pos, coral); l++) {
                pos.move(grow);
            }
            pos.move(grow.getOpposite());
            pos.move(EnumFacing.UP);
            for (int i = 0; i < length; i++) {
                pos.move(main);
                if (!coralAt(world, random, pos, coral)) {
                    break;
                }
                if (random.nextFloat() < 0.25F) {
                    pos.move(EnumFacing.UP);
                }
            }
        }
    }

    public static void mushroom(World world, Random random, BlockPos start, IBlockState coral) {
        int height = random.nextInt(3) + 3;
        int width = random.nextInt(3) + 3;
        int depth = random.nextInt(3) + 3;
        int sink = random.nextInt(3) + 1;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = 0; x <= width; x++) {
            for (int y = 0; y <= height; y++) {
                for (int z = 0; z <= depth; z++) {
                    pos.setPos(x + start.getX(), y + start.getY() - sink, z + start.getZ());
                    boolean notEdgeXY = x != 0 && x != width || y != 0 && y != height;
                    boolean notEdgeZY = z != 0 && z != depth || y != 0 && y != height;
                    boolean notEdgeXZ = x != 0 && x != width || z != 0 && z != depth;
                    boolean onShell = x == 0 || x == width || y == 0 || y == height || z == 0 || z == depth;
                    if (notEdgeXY && notEdgeZY && notEdgeXZ && onShell && !(random.nextFloat() < 0.1F)) {
                        coralAt(world, random, pos, coral);
                    }
                }
            }
        }
    }
}
