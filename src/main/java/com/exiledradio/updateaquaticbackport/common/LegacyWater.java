package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Gives water back to blocks saved before this mod's water was waterlogging.
 *
 * <p>Before, kelp, seagrass, tall seagrass and bubble columns <em>were</em> the water in their space, and coral,
 * coral fans and the conduit were when a bit of their metadata said so (bit 1; bit 4 for wall fans). Now that
 * water is Fluidlogged API's, kept beside the block, and an old chunk has none: its kelp would stand dry. No
 * property names were ever saved (1.12.2 saves block ids and metadata), so the blocks themselves load as they
 * are; only their water is missing.
 *
 * <p>So a chunk read from disk without this mod's mark ({@value #TAG}) is searched, in its saved data, for those
 * blocks (by id, and by that metadata bit where there is one); at the end of the world tick each one found and
 * still there gets a water source, still water as generated water is. Every chunk saved afterwards carries the
 * mark, so this happens once per chunk; new chunks are marked the first time they are saved.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class LegacyWater {

    /** The mark in a chunk's {@code Level} data: its blocks have their water. */
    public static final String TAG = UpdateAquaticBackport.MOD_ID + ":waterMoved";

    /** Every metadata value. */
    private static final int ALWAYS = 15;

    /** Block -> the metadata bits that meant "in water" (any of them set), {@link #ALWAYS} for every state. */
    private static final Map<Block, Integer> RULES = Collections.synchronizedMap(new LinkedHashMap<Block, Integer>());

    /** Per world: chunk -> positions waiting for their water. Filled on the chunk loading thread. */
    private static final Map<World, Map<ChunkPos, List<BlockPos>>> PENDING = new WeakHashMap<World, Map<ChunkPos, List<BlockPos>>>();

    private LegacyWater() {
    }

    /** Every saved state of this block was water. */
    public static void always(Block block) {
        RULES.put(block, ALWAYS);
    }

    /** A saved state of this block was water when this metadata bit is set. */
    public static void whenBit(Block block, int bit) {
        RULES.put(block, bit);
    }

    /** @return block id -> rule, for the ids this world uses now */
    private static Map<Integer, Integer> byId() {
        Map<Integer, Integer> out = new HashMap<Integer, Integer>();
        synchronized (RULES) {
            for (Map.Entry<Block, Integer> e : RULES.entrySet()) {
                out.put(Block.getIdFromBlock(e.getKey()), e.getValue());
            }
        }
        return out;
    }

    /**
     * @param level a chunk's saved {@code Level} data
     * @return the world positions of every saved block that was water
     */
    public static List<BlockPos> find(NBTTagCompound level, int chunkX, int chunkZ) {
        List<BlockPos> out = new ArrayList<BlockPos>();
        Map<Integer, Integer> rules = byId();
        if (rules.isEmpty()) {
            return out;
        }
        NBTTagList sections = level.getTagList("Sections", 10);
        for (int s = 0; s < sections.tagCount(); s++) {
            NBTTagCompound section = sections.getCompoundTagAt(s);
            byte[] blocks = section.getByteArray("Blocks");
            byte[] data = section.getByteArray("Data");
            byte[] add = section.hasKey("Add", 7) ? section.getByteArray("Add") : null;
            if (blocks.length != 4096 || data.length != 2048) {
                continue;
            }
            int baseY = (section.getByte("Y") & 255) << 4;
            for (int i = 0; i < 4096; i++) {
                int id = blocks[i] & 255;
                if (add != null) {
                    id |= ((add[i >> 1] >> ((i & 1) << 2)) & 15) << 8;
                }
                Integer rule = rules.get(id);
                if (rule == null) {
                    continue;
                }
                int meta = (data[i >> 1] >> ((i & 1) << 2)) & 15;
                if (rule == ALWAYS || (meta & rule) != 0) {
                    out.add(new BlockPos(chunkX << 4 | (i & 15), baseY | (i >> 8), chunkZ << 4 | ((i >> 4) & 15)));
                }
            }
        }
        return out;
    }

    private static Map<ChunkPos, List<BlockPos>> pending(World world) {
        synchronized (PENDING) {
            Map<ChunkPos, List<BlockPos>> map = PENDING.get(world);
            if (map == null) {
                map = new LinkedHashMap<ChunkPos, List<BlockPos>>();
                PENDING.put(world, map);
            }
            return map;
        }
    }

    /** Queues one position, as a chunk load would; for tests. */
    public static void queue(World world, BlockPos pos) {
        Map<ChunkPos, List<BlockPos>> map = pending(world);
        synchronized (PENDING) {
            ChunkPos chunk = new ChunkPos(pos);
            List<BlockPos> list = map.get(chunk);
            if (list == null) {
                list = new ArrayList<BlockPos>();
                map.put(chunk, list);
            }
            list.add(pos.toImmutable());
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkDataEvent.Load event) {
        World world = event.getWorld();
        if (world == null || world.isRemote || !Waterlogging.isAvailable()) {
            return;
        }
        NBTTagCompound level = event.getData().getCompoundTag("Level");
        if (level.getBoolean(TAG)) {
            return;
        }
        Chunk chunk = event.getChunk();
        List<BlockPos> found = find(level, chunk.x, chunk.z);
        if (found.isEmpty()) {
            return;
        }
        Map<ChunkPos, List<BlockPos>> map = pending(world);
        synchronized (PENDING) {
            map.put(chunk.getPos(), found);
        }
    }

    @SubscribeEvent
    public static void onChunkSave(ChunkDataEvent.Save event) {
        World world = event.getWorld();
        if (world == null || world.isRemote || !Waterlogging.isAvailable()) {
            // Without Fluidlogged API the blocks are the water themselves: saved unmarked, so they get their
            // water once it is installed.
            return;
        }
        Map<ChunkPos, List<BlockPos>> map = pending(world);
        synchronized (PENDING) {
            if (map.containsKey(event.getChunk().getPos())) {
                // Not given its water yet: saved unmarked, so its next load finds them again.
                return;
            }
        }
        event.getData().getCompoundTag("Level").setBoolean(TAG, true);
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.world instanceof WorldServer) {
            run((WorldServer) event.world);
        }
    }

    /**
     * Gives water to every block waiting in a world's loaded chunks.
     *
     * @return how many blocks were given water
     */
    public static int run(WorldServer world) {
        Map<ChunkPos, List<BlockPos>> work;
        Map<ChunkPos, List<BlockPos>> map = pending(world);
        synchronized (PENDING) {
            if (map.isEmpty()) {
                return 0;
            }
            work = new LinkedHashMap<ChunkPos, List<BlockPos>>(map);
            map.clear();
        }
        int given = 0;
        for (Map.Entry<ChunkPos, List<BlockPos>> e : work.entrySet()) {
            Chunk chunk = world.getChunkProvider().getLoadedChunk(e.getKey().x, e.getKey().z);
            if (chunk == null) {
                continue;
            }
            for (BlockPos pos : e.getValue()) {
                if (RULES.containsKey(world.getBlockState(pos).getBlock()) && !Waterlogging.hasWater(world, pos)
                        && Waterlogging.setWater(world, pos, true, 2)) {
                    given++;
                }
            }
            chunk.markDirty();
        }
        if (given > 0) {
            UpdateAquaticBackport.LOGGER.debug("Gave water back to {} blocks saved before waterlogging.", given);
        }
        return given;
    }
}
