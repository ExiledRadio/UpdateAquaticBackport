package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Buried treasure: every chunk a candidate (spacing 1, separation 0, salt 0), kept with chance 0.01 seeded
 * by salt 10387320 (1.13's {@code BuriedTreasureStructure}; today's {@code legacy_type_2} reducer is the same
 * roll), on beaches by the biome at the version's point ({@link StructureBiomes#middle}); the chest goes at
 * block (9, 9) of the chunk.
 */
public class MapGenBuriedTreasure extends ScatteredStructure {

    static final float FREQUENCY = 0.01F;
    /** {@code StructurePlacement.legacyArbitrarySaltProbabilityReducer}'s fixed salt. */
    private static final int REDUCER_SALT = 10387320;

    public MapGenBuriedTreasure() {
        super(StructureContent.BURIED_TREASURE_SAVE, new ScatteredPlacement(1, 0, 0));
    }

    /** Today's {@code legacy_type_2}: {@code setLargeFeatureWithSalt(seed, x, z, 10387320).nextFloat() < 0.01}. */
    public static boolean frequencyRoll(long seed, int chunkX, int chunkZ) {
        Random random = new Random(chunkX * 341873128712L + chunkZ * 132897987541L + seed + REDUCER_SALT);
        return random.nextFloat() < FREQUENCY;
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return frequencyRoll(world.getSeed(), chunkX, chunkZ) && StructureBiomes.isBeach(world, chunkX, chunkZ);
    }

    @Override
    protected StructureStart getStructureStart(int chunkX, int chunkZ) {
        return new Start(chunkX, chunkZ);
    }

    /** The chest's column ((9, 9) in the chunk), as today's locator reports it, rather than the chunk's middle. */
    @Nullable
    @Override
    public BlockPos nearest(World world, BlockPos from, int radiusChunks) {
        BlockPos found = super.nearest(world, from, radiusChunks);
        if (found == null) {
            return null;
        }
        return new BlockPos((found.getX() & ~15) + 9, found.getY(), (found.getZ() & ~15) + 9);
    }

    public static class Start extends StructureStart {

        /** For loading. */
        public Start() {
        }

        Start(int chunkX, int chunkZ) {
            super(chunkX, chunkZ);
            this.components.add(new BuriedTreasurePiece(new BlockPos((chunkX << 4) + 9, 90, (chunkZ << 4) + 9)));
            updateBoundingBox();
        }
    }
}
