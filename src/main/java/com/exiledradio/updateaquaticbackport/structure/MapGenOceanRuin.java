package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.terrain.TerrainHeights;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureStart;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ocean ruins: today's structure set {@code ocean_ruins} (spacing 20, separation 8, salt 14357621), shared
 * by {@code ocean_ruin_warm} and {@code ocean_ruin_cold}, whose biomes never overlap. Both have a large
 * probability of 0.3 and a cluster probability of 0.9 ({@code OceanRuinPieces}).
 */
public class MapGenOceanRuin extends ScatteredStructure {

    static final float LARGE_PROBABILITY = 0.3F;
    static final float CLUSTER_PROBABILITY = 0.9F;

    public static final ResourceLocation[] WARM = names("warm_1", "warm_2", "warm_3", "warm_4", "warm_5", "warm_6",
            "warm_7", "warm_8");
    public static final ResourceLocation[] BRICK = names("brick_1", "brick_2", "brick_3", "brick_4", "brick_5", "brick_6",
            "brick_7", "brick_8");
    public static final ResourceLocation[] CRACKED = names("cracked_1", "cracked_2", "cracked_3", "cracked_4", "cracked_5",
            "cracked_6", "cracked_7", "cracked_8");
    public static final ResourceLocation[] MOSSY = names("mossy_1", "mossy_2", "mossy_3", "mossy_4", "mossy_5", "mossy_6",
            "mossy_7", "mossy_8");
    public static final ResourceLocation[] BIG_BRICK = names("big_brick_1", "big_brick_2", "big_brick_3", "big_brick_8");
    public static final ResourceLocation[] BIG_MOSSY = names("big_mossy_1", "big_mossy_2", "big_mossy_3", "big_mossy_8");
    public static final ResourceLocation[] BIG_CRACKED = names("big_cracked_1", "big_cracked_2", "big_cracked_3", "big_cracked_8");
    public static final ResourceLocation[] BIG_WARM = names("big_warm_4", "big_warm_5", "big_warm_6", "big_warm_7");

    public MapGenOceanRuin() {
        super(StructureContent.OCEAN_RUIN_SAVE, new ScatteredPlacement(20, 8, 14357621));
    }

    private static ResourceLocation[] names(String... names) {
        ResourceLocation[] out = new ResourceLocation[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = Content.id("underwater_ruin/" + names[i]);
        }
        return out;
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return StructureBiomes.warmRuin(StructureBiomes.ocean(world, chunkX, chunkZ)) != null;
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(this.world, this.rand, chunkX, chunkZ);
    }

    public static class Start extends StructureStart {

        /** For loading. */
        public Start() {
        }

        Start(World world, Random random, int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            Boolean warm = StructureBiomes.warmRuin(StructureBiomes.ocean(world, chunkX, chunkZ));
            TerrainHeights heights = TerrainHeights.forWorld(world);
            if (warm != null && heights != null) {
                BlockPos origin = new BlockPos(chunkX << 4, 90, chunkZ << 4);
                Rotation rotation = rotation(random);
                // Today's OceanRuinPieces.addPieces.
                boolean large = random.nextFloat() <= LARGE_PROBABILITY;
                addPiece(origin, rotation, random, warm, large, large ? 0.9F : 0.8F);
                if (large && random.nextFloat() <= CLUSTER_PROBABILITY) {
                    addCluster(random, rotation, origin, warm);
                }
                for (StructureComponent piece : this.components) {
                    ((OceanRuinPiece) piece).settle(heights);
                }
            }
            updateBoundingBox();
        }

        private static Rotation rotation(Random random) {
            return Rotation.values()[random.nextInt(Rotation.values().length)];
        }

        /** Today's addClusterRuins: four to eight small ruins in the eight spots around a large one. */
        private void addCluster(Random random, Rotation rotation, BlockPos origin, boolean warm) {
            BlockPos base = new BlockPos(origin.getX(), 90, origin.getZ());
            BlockPos far = ModernTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, rotation, BlockPos.ORIGIN).add(base);
            StructureBoundingBox big = StructureBoundingBox.createProper(base.getX(), base.getY(), base.getZ(),
                    far.getX(), far.getY(), far.getZ());
            BlockPos min = new BlockPos(Math.min(base.getX(), far.getX()), base.getY(), Math.min(base.getZ(), far.getZ()));
            List<BlockPos> spots = spots(random, min);
            int count = MathHelper.getInt(random, 4, 8);
            for (int i = 0; i < count; i++) {
                if (spots.isEmpty()) {
                    continue;
                }
                BlockPos at = spots.remove(random.nextInt(spots.size()));
                Rotation turn = rotation(random);
                BlockPos corner = ModernTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, turn, BlockPos.ORIGIN).add(at);
                StructureBoundingBox box = StructureBoundingBox.createProper(at.getX(), at.getY(), at.getZ(),
                        corner.getX(), corner.getY(), corner.getZ());
                if (!box.intersectsWith(big)) {
                    addPiece(at, turn, random, warm, false, 0.8F);
                }
            }
        }

        /** Today's allPositions, in its order. */
        private static List<BlockPos> spots(Random r, BlockPos p) {
            List<BlockPos> out = new ArrayList<BlockPos>();
            out.add(p.add(-16 + MathHelper.getInt(r, 1, 8), 0, 16 + MathHelper.getInt(r, 1, 7)));
            out.add(p.add(-16 + MathHelper.getInt(r, 1, 8), 0, MathHelper.getInt(r, 1, 7)));
            out.add(p.add(-16 + MathHelper.getInt(r, 1, 8), 0, -16 + MathHelper.getInt(r, 4, 8)));
            out.add(p.add(MathHelper.getInt(r, 1, 7), 0, 16 + MathHelper.getInt(r, 1, 7)));
            out.add(p.add(MathHelper.getInt(r, 1, 7), 0, -16 + MathHelper.getInt(r, 4, 6)));
            out.add(p.add(16 + MathHelper.getInt(r, 1, 7), 0, 16 + MathHelper.getInt(r, 3, 8)));
            out.add(p.add(16 + MathHelper.getInt(r, 1, 7), 0, MathHelper.getInt(r, 1, 7)));
            out.add(p.add(16 + MathHelper.getInt(r, 1, 7), 0, -16 + MathHelper.getInt(r, 4, 8)));
            return out;
        }

        /** Today's addPiece: a warm ruin is one file; a cold one is brick, then cracked and mossy over it. */
        private void addPiece(BlockPos at, Rotation rotation, Random random, boolean warm, boolean large, float integrity) {
            if (warm) {
                ResourceLocation[] files = large ? BIG_WARM : WARM;
                this.components.add(new OceanRuinPiece(files[random.nextInt(files.length)], at, rotation, integrity,
                        true, large, random));
                return;
            }
            ResourceLocation[] brick = large ? BIG_BRICK : BRICK;
            ResourceLocation[] cracked = large ? BIG_CRACKED : CRACKED;
            ResourceLocation[] mossy = large ? BIG_MOSSY : MOSSY;
            int i = random.nextInt(brick.length);
            this.components.add(new OceanRuinPiece(brick[i], at, rotation, integrity, false, large, random));
            this.components.add(new OceanRuinPiece(cracked[i], at, rotation, 0.7F, false, large, random));
            this.components.add(new OceanRuinPiece(mossy[i], at, rotation, 0.5F, false, large, random));
        }

        @Override
        public boolean isSizeableStructure() {
            return !this.components.isEmpty();
        }
    }
}
