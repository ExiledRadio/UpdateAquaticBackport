package com.exiledradio.updateaquaticbackport.ocean;

import net.minecraft.block.state.IBlockState;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * The world generation hooks, all Forge events, registered only when the stream is on.
 *
 * <ul>
 *   <li>{@link Generation} on the terrain bus: the biome map ({@link OceanLayers}), the underwater carvers
 *       ({@link UnderwaterCarvers}, wrapping the ravine generator), the monument rule
 *       ({@link OceanMonuments}), and the freezing pass for the frozen oceans.</li>
 *   <li>{@link Population} on the main bus: makes carved water next to air flow once its chunk is
 *       populated, and forgets a world's notes when it unloads.</li>
 * </ul>
 */
public final class OceanTerrain {

    /** Chunks remembered at most per world; chunks generated but never populated fall off the end. */
    private static final int MAX_PENDING = 4096;

    /** world -> chunk -> packed positions (x | z << 4 | y << 8) of carved water next to air */
    private static final Map<World, Map<Long, int[]>> PENDING = new WeakHashMap<World, Map<Long, int[]>>();

    private OceanTerrain() {
    }

    public static synchronized void noteFlowing(World world, int chunkX, int chunkZ, int[] positions, int count) {
        if (count == 0) {
            return;
        }
        Map<Long, int[]> chunks = PENDING.get(world);
        if (chunks == null) {
            chunks = new LinkedHashMap<Long, int[]>(64, 0.75F, false) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Long, int[]> eldest) {
                    return size() > MAX_PENDING;
                }
            };
            PENDING.put(world, chunks);
        }
        int[] copy = new int[count];
        System.arraycopy(positions, 0, copy, 0, count);
        chunks.put(ChunkPos.asLong(chunkX, chunkZ), copy);
    }

    private static synchronized int[] takeFlowing(World world, int chunkX, int chunkZ) {
        Map<Long, int[]> chunks = PENDING.get(world);
        return chunks == null ? null : chunks.remove(ChunkPos.asLong(chunkX, chunkZ));
    }

    /** @return how many chunks of this world still wait for their water to be set flowing (for the self-test) */
    public static synchronized int pendingChunks(World world) {
        Map<Long, int[]> chunks = PENDING.get(world);
        return chunks == null ? 0 : chunks.size();
    }

    /**
     * Turns noted water that now has air beside or below it into flowing water with a tick scheduled, as
     * 1.13 schedules a fluid tick when its carver puts water there.
     *
     * @return how many blocks were set flowing
     */
    public static int flow(World world, int chunkX, int chunkZ) {
        int[] positions = takeFlowing(world, chunkX, chunkZ);
        if (positions == null) {
            return 0;
        }
        int flowed = 0;
        BlockPos.MutableBlockPos side = new BlockPos.MutableBlockPos();
        for (int packed : positions) {
            BlockPos pos = new BlockPos(chunkX * 16 + (packed & 15), packed >> 8, chunkZ * 16 + (packed >> 4 & 15));
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() != Blocks.WATER) {
                continue;
            }
            boolean open = false;
            for (EnumFacing facing : EnumFacing.values()) {
                if (facing == EnumFacing.UP) {
                    continue;
                }
                side.setPos(pos).move(facing);
                if (world.isBlockLoaded(side) && world.getBlockState(side).getMaterial() == Material.AIR) {
                    open = true;
                    break;
                }
            }
            if (open) {
                // 2 | 16: no observer pass, which would read (and load) the next chunk for an edge block.
                world.setBlockState(pos, Blocks.FLOWING_WATER.getDefaultState(), 2 | 16);
                world.scheduleUpdate(pos, Blocks.FLOWING_WATER, Blocks.FLOWING_WATER.tickRate(world));
                flowed++;
            }
        }
        return flowed;
    }

    /**
     * Vanilla's population freezing pass (the 16 x 16 columns offset by 8) with 1.13's patchy temperature in
     * the frozen oceans, run instead of vanilla's when any of those columns is a frozen ocean.
     *
     * @return true if this pass ran in place of vanilla's
     */
    static boolean freeze(World world, int chunkX, int chunkZ) {
        int x0 = chunkX * 16 + 8;
        int z0 = chunkZ * 16 + 8;
        boolean frozen = false;
        BlockPos.MutableBlockPos column = new BlockPos.MutableBlockPos();
        for (int dx = 0; dx < 16 && !frozen; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                if (world.getBiome(column.setPos(x0 + dx, 0, z0 + dz)) instanceof BiomeFrozenOcean) {
                    frozen = true;
                    break;
                }
            }
        }
        if (!frozen) {
            return false;
        }
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos top = world.getPrecipitationHeight(new BlockPos(x0 + dx, 0, z0 + dz));
                BlockPos below = top.down();
                Biome biome = world.getBiome(below);
                if (biome instanceof BiomeFrozenOcean) {
                    BiomeFrozenOcean ocean = (BiomeFrozenOcean) biome;
                    if (BiomeAquaticOcean.freezes(world, below, ocean.temperatureAt(below))) {
                        world.setBlockState(below, Blocks.ICE.getDefaultState(), 2);
                    }
                    if (BiomeAquaticOcean.snows(world, top, ocean.temperatureAt(top))) {
                        world.setBlockState(top, Blocks.SNOW_LAYER.getDefaultState(), 2);
                    }
                } else {
                    if (world.canBlockFreezeWater(below)) {
                        world.setBlockState(below, Blocks.ICE.getDefaultState(), 2);
                    }
                    if (world.canSnowAt(top, true)) {
                        world.setBlockState(top, Blocks.SNOW_LAYER.getDefaultState(), 2);
                    }
                }
            }
        }
        return true;
    }

    /** Terrain bus. */
    public static final class Generation {
        private Generation() {
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
            OceanLayers.onInitBiomeGens(event);
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onInitMapGen(InitMapGenEvent event) {
            if (event.getType() == InitMapGenEvent.EventType.RAVINE) {
                event.setNewGen(new UnderwaterCarvers(event.getNewGen()));
            } else if (event.getType() == InitMapGenEvent.EventType.OCEAN_MONUMENT
                    && event.getNewGen().getClass() == StructureOceanMonument.class) {
                event.setNewGen(new OceanMonuments());
            }
        }

        @SubscribeEvent(priority = EventPriority.LOW)
        public static void onPopulate(PopulateChunkEvent.Populate event) {
            if (event.getType() == PopulateChunkEvent.Populate.EventType.ICE && event.getResult() != Event.Result.DENY
                    && freeze(event.getWorld(), event.getChunkX(), event.getChunkZ())) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    /** Main bus. */
    public static final class Population {
        private Population() {
        }

        @SubscribeEvent
        public static void onPopulated(PopulateChunkEvent.Post event) {
            flow(event.getWorld(), event.getChunkX(), event.getChunkZ());
        }

        @SubscribeEvent
        public static void onUnload(WorldEvent.Unload event) {
            synchronized (OceanTerrain.class) {
                PENDING.remove(event.getWorld());
            }
        }
    }
}
