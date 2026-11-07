package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.TemplatePiece;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.backportcore.terrain.TerrainHeights;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityLockableLoot;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;

import java.util.Random;

/**
 * One ocean ruin file: today's {@code OceanRuinPieces.OceanRuinPiece}. No pivot; air and structure blocks
 * left out; each block kept with the piece's integrity (today's {@code BlockRotProcessor}). Markers: a
 * {@code chest} becomes a chest (in water if there is water there) with the big or small ruin table, a
 * {@code drowned} spawns a persistent drowned and leaves water below sea level, air above.
 */
public class OceanRuinPiece extends TemplatePiece {

    private boolean warm;
    private boolean large;

    /** For loading. */
    public OceanRuinPiece() {
    }

    public OceanRuinPiece(ResourceLocation file, BlockPos origin, Rotation rotation, float integrity, boolean warm,
                   boolean large, Random random) {
        super(file, origin, rotation, Mirror.NONE, BlockPos.ORIGIN, random);
        this.integrity = integrity;
        this.warm = warm;
        this.large = large;
    }

    public boolean warm() {
        return this.warm;
    }

    public boolean large() {
        return this.large;
    }

    public float integrity() {
        return this.integrity;
    }

    /**
     * Today's {@code postProcess} height, at layout from the predicted terrain: the sea floor at the
     * corner, then {@code getHeight} over the turned footprint, which drops the ruin to the lowest column
     * when most of the footprint is more than two blocks below it. Today's column scan (down through air,
     * water and ice from one below the corner's height) lands on the floor's top block or one below the
     * corner's height, whichever is lower.
     */
    void settle(TerrainHeights heights) {
        int x0 = this.origin.getX();
        int z0 = this.origin.getZ();
        BlockPos from = new BlockPos(x0, heights.floor(x0, z0), z0);
        BlockPos size = template().size();
        BlockPos far = ModernTemplate.transform(new BlockPos(size.getX() - 1, 0, size.getZ() - 1), Mirror.NONE,
                this.rotation, BlockPos.ORIGIN).add(from);

        int y = from.getY();
        int lowest = 512;
        int top = y - 1;
        int deeper = 0;
        for (int x = Math.min(from.getX(), far.getX()); x <= Math.max(from.getX(), far.getX()); x++) {
            for (int z = Math.min(from.getZ(), far.getZ()); z <= Math.max(from.getZ(), far.getZ()); z++) {
                int column = Math.max(1, Math.min(y - 1, heights.floor(x, z) - 1));
                lowest = Math.min(lowest, column);
                if (column < top - 2) {
                    deeper++;
                }
            }
        }
        int span = Math.abs(from.getX() - far.getX());
        if (top - lowest > 2 && deeper > span - 2) {
            y = lowest + 1;
        }
        setOrigin(new BlockPos(x0, y, z0));
    }

    @Override
    protected TemplatePlacement placement() {
        return super.placement().ignoreAir(true);
    }

    @Override
    protected void handleDataMarker(String metadata, BlockPos pos, World world, Random random, StructureBoundingBox chunk) {
        if ("chest".equals(metadata)) {
            IBlockState chest = Blocks.CHEST.getDefaultState();
            if (Waterlogging.hasWater(world, pos)) {
                Waterlogging.placeWithWater(world, pos, chest, 2);
            } else {
                Waterlogging.placeDry(world, pos, chest, 2);
            }
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityLockableLoot) {
                ((TileEntityLockableLoot) tile).setLootTable(
                        this.large ? StructureContent.UNDERWATER_RUIN_BIG : StructureContent.UNDERWATER_RUIN_SMALL,
                        random.nextLong());
            }
        } else if ("drowned".equals(metadata)) {
            // The trident stream's drowned, by name; none when that stream is off.
            Entity entity = EntityList.createEntityByIDFromName(Content.id("drowned"), world);
            if (entity instanceof EntityLiving) {
                EntityLiving drowned = (EntityLiving) entity;
                drowned.enablePersistence();
                drowned.setLocationAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
                drowned.onInitialSpawn(world.getDifficultyForLocation(pos), null);
                world.spawnEntity(drowned);
            }
            IBlockState fill = pos.getY() > world.getSeaLevel() ? Blocks.AIR.getDefaultState() : Blocks.WATER.getDefaultState();
            world.setBlockState(pos, fill, 2);
        }
    }

    @Override
    protected void writeExtra(NBTTagCompound tag) {
        tag.setString("BiomeType", this.warm ? "WARM" : "COLD");
        tag.setBoolean("IsLarge", this.large);
    }

    @Override
    protected void readExtra(NBTTagCompound tag) {
        this.warm = "WARM".equals(tag.getString("BiomeType"));
        this.large = tag.getBoolean("IsLarge");
    }
}
