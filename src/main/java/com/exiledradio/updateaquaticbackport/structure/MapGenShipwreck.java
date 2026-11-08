package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.StructurePlacements;
import com.exiledradio.backportcore.terrain.TerrainHeights;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureStart;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * Shipwrecks, placed as 1.13 places them ({@code ShipwreckStructure}: spacing 16, separation 8, salt
 * 165745295; 1.16 later widened it to today's 24 and 4, which Backport Core's placement table gives when a 1.16
 * backport is installed), sunken in any ocean and beached on beaches. Their biomes never overlap, so the biome at
 * the version's point ({@link StructureBiomes#middle}) decides which one a candidate chunk gets.
 */
public class MapGenShipwreck extends ScatteredStructure {

    public static final ResourceLocation[] BEACHED = names("with_mast", "sideways_full", "sideways_fronthalf",
            "sideways_backhalf", "rightsideup_full", "rightsideup_fronthalf", "rightsideup_backhalf",
            "with_mast_degraded", "rightsideup_full_degraded", "rightsideup_fronthalf_degraded",
            "rightsideup_backhalf_degraded");
    public static final ResourceLocation[] OCEAN = names("with_mast", "upsidedown_full", "upsidedown_fronthalf",
            "upsidedown_backhalf", "sideways_full", "sideways_fronthalf", "sideways_backhalf", "rightsideup_full",
            "rightsideup_fronthalf", "rightsideup_backhalf", "with_mast_degraded", "upsidedown_full_degraded",
            "upsidedown_fronthalf_degraded", "upsidedown_backhalf_degraded", "sideways_full_degraded",
            "sideways_fronthalf_degraded", "sideways_backhalf_degraded", "rightsideup_full_degraded",
            "rightsideup_fronthalf_degraded", "rightsideup_backhalf_degraded");

    public MapGenShipwreck() {
        // The newest installed version's grid: 1.13's 16/8 (Placement113), 1.16's 24/4 with the Nether backport.
        super(StructureContent.SHIPWRECK_SAVE, StructurePlacements.placement(Structures.SHIPWRECK, new ScatteredPlacement(16, 8, 165745295)));
    }

    private static ResourceLocation[] names(String... names) {
        ResourceLocation[] out = new ResourceLocation[names.length];
        for (int i = 0; i < names.length; i++) {
            out[i] = Content.id("shipwreck/" + names[i]);
        }
        return out;
    }

    /** @return true for a beached shipwreck, false for a sunken one, null where none goes */
    @Nullable
    static Boolean beached(World world, int chunkX, int chunkZ) {
        if (StructureBiomes.ocean(world, chunkX, chunkZ).isOcean()) {
            return Boolean.FALSE;
        }
        return StructureBiomes.isBeach(world, chunkX, chunkZ) ? Boolean.TRUE : null;
    }

    @Override
    protected boolean canStartAt(World world, int chunkX, int chunkZ) {
        return beached(world, chunkX, chunkZ) != null;
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
            Boolean beached = beached(world, chunkX, chunkZ);
            TerrainHeights heights = TerrainHeights.forWorld(world);
            if (beached != null && heights != null) {
                // Today's ShipwreckStructure.generatePieces: the chunk's corner at y 90, a random turn, then
                // a random file of the kind.
                Rotation rotation = Rotation.values()[random.nextInt(Rotation.values().length)];
                ResourceLocation[] files = beached ? BEACHED : OCEAN;
                ResourceLocation file = files[random.nextInt(files.length)];
                ShipwreckPiece piece = new ShipwreckPiece(file, new BlockPos(chunkX << 4, 90, chunkZ << 4), rotation,
                        beached, random);
                piece.settle(heights, random);
                this.components.add(piece);
            }
            updateBoundingBox();
        }

        /** A start on terrain Core cannot predict has no pieces and is not kept. */
        @Override
        public boolean isSizeableStructure() {
            return !this.components.isEmpty();
        }
    }
}
