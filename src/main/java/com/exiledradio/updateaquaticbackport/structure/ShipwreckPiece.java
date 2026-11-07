package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.TemplatePiece;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.terrain.TerrainHeights;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * One shipwreck: today's {@code ShipwreckPieces.ShipwreckPiece}. Turned around (4, 0, 15), air and
 * structure blocks left out; each of the three chest markers gives the chest under it its loot table.
 */
public class ShipwreckPiece extends TemplatePiece {

    static final BlockPos PIVOT = new BlockPos(4, 0, 15);

    private boolean beached;

    /** For loading. */
    public ShipwreckPiece() {
    }

    public ShipwreckPiece(ResourceLocation file, BlockPos origin, Rotation rotation, boolean beached, Random random) {
        super(file, origin, rotation, Mirror.NONE, PIVOT, random);
        this.beached = beached;
    }

    public boolean beached() {
        return this.beached;
    }

    /**
     * Today's {@code postProcess} height, worked out at layout from the predicted terrain (the chunks the
     * ship covers are not built yet). Over the file's unturned footprint from the corner, as today: a sunken
     * ship sits on the average sea floor ({@code OCEAN_FLOOR_WG}); a beached one is sunk half its height,
     * plus 0 to 2, below the lowest surface ({@code WORLD_SURFACE_WG}).
     */
    void settle(TerrainHeights heights, Random random) {
        BlockPos size = template().size();
        int x0 = this.origin.getX();
        int z0 = this.origin.getZ();
        int lowest = 256;
        int sum = 0;
        int area = size.getX() * size.getZ();
        if (area == 0) {
            sum = column(heights, x0, z0);
        } else {
            for (int dx = 0; dx < size.getX(); dx++) {
                for (int dz = 0; dz < size.getZ(); dz++) {
                    int h = column(heights, x0 + dx, z0 + dz);
                    sum += h;
                    lowest = Math.min(lowest, h);
                }
            }
            sum /= area;
        }
        int y = this.beached ? lowest - size.getY() / 2 - random.nextInt(3) : sum;
        setOrigin(new BlockPos(x0, y, z0));
    }

    private int column(TerrainHeights heights, int x, int z) {
        return this.beached ? heights.height(x, z) : heights.floor(x, z);
    }

    @Override
    protected TemplatePlacement placement() {
        return super.placement().ignoreAir(true);
    }

    @Nullable
    static ResourceLocation lootFor(String marker) {
        switch (marker) {
            case "map_chest":
                return StructureContent.SHIPWRECK_MAP;
            case "treasure_chest":
                return StructureContent.SHIPWRECK_TREASURE;
            case "supply_chest":
                return StructureContent.SHIPWRECK_SUPPLY;
            default:
                return null;
        }
    }

    @Override
    protected void handleDataMarker(String metadata, BlockPos pos, World world, Random random, StructureBoundingBox chunk) {
        ResourceLocation table = lootFor(metadata);
        if (table == null) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos.down());
        if (tile instanceof TileEntityLockableLoot) {
            ((TileEntityLockableLoot) tile).setLootTable(table, random.nextLong());
        }
    }

    @Override
    protected void writeExtra(NBTTagCompound tag) {
        tag.setBoolean("isBeached", this.beached);
    }

    @Override
    protected void readExtra(NBTTagCompound tag) {
        this.beached = tag.getBoolean("isBeached");
    }
}
