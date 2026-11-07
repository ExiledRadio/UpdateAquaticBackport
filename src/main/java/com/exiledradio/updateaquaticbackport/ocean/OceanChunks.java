package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which chunks have 1.13's ocean temperatures in their biomes, so {@link OceanKind} knows where a vanilla
 * ocean is 1.13's normal ocean and where it is an ocean from before, whose temperature still comes from
 * the noise.
 *
 * <h2>As vanilla handles old chunks</h2>
 * Vanilla never rewrites the biomes of a chunk it has already generated: a 1.12 chunk loaded by 1.13 keeps
 * its plain oceans, and vanilla tells old chunks from new ones by what the chunk's own data says (its data
 * version). This does the same with one flag of its own in the chunk's saved data,
 * {@code updateaquaticbackport:OceanTemperatures}:
 * <ul>
 *   <li>a chunk generated now has the temperatures if the world's biome map does - the map has this
 *       stream's layers ({@link OceanLayers}), which a flat or debug world, another dimension, or a world
 *       type that builds its own map does not;</li>
 *   <li>a chunk loaded from disk has them only if its data has the flag, so every chunk generated before
 *       the stream was on keeps the noise, however often it is saved again;</li>
 *   <li>a chunk not loaded is what the map would make there now.</li>
 * </ul>
 * A client knows nothing of a chunk's saved data, so on the client only the map counts.
 */
public final class OceanChunks {

    /** The flag in a chunk's saved data, beside vanilla's {@code DataVersion}. */
    public static final String TAG_NAME = UpdateAquaticBackport.MOD_ID + ":OceanTemperatures";

    /** For each chunk loaded from disk: whether its data had the flag. Generated chunks are not listed. */
    private static final Map<Chunk, Boolean> LOADED = Collections.synchronizedMap(new WeakHashMap<Chunk, Boolean>());

    /** For each biome provider seen: whether its map has this stream's layers. */
    private static final Map<BiomeProvider, Boolean> LAYERED = Collections.synchronizedMap(new WeakHashMap<BiomeProvider, Boolean>());

    private static Field genBiomes;
    private static Field parent;

    private OceanChunks() {
    }

    static void install() {
        OceanKind.useRealTemperatures(new OceanKind.RealTemperatures() {
            @Override
            public boolean at(World world, int x, int z) {
                return realTemperatures(world, x, z);
            }
        });
        MinecraftForge.EVENT_BUS.register(OceanChunks.class);
    }

    /** @return true if the biome at this column came from a map with 1.13's ocean temperatures */
    public static boolean realTemperatures(World world, int x, int z) {
        if (!world.isRemote) {
            Chunk chunk = world.getChunkProvider().getLoadedChunk(x >> 4, z >> 4);
            if (chunk != null) {
                return realTemperatures(chunk);
            }
        }
        return layered(world.getBiomeProvider());
    }

    /** @return true if this loaded chunk's biomes have 1.13's ocean temperatures */
    public static boolean realTemperatures(Chunk chunk) {
        Boolean fromDisk = LOADED.get(chunk);
        return fromDisk != null ? fromDisk : layered(chunk.getWorld().getBiomeProvider());
    }

    /** @return for a chunk loaded from disk, whether its data had the flag; null for a chunk generated now */
    @Nullable
    public static Boolean loadedFlag(Chunk chunk) {
        return LOADED.get(chunk);
    }

    /** @return true if this provider's biome map has {@link OceanLayers}' mixing layer in it */
    public static boolean layered(BiomeProvider provider) {
        Boolean known = LAYERED.get(provider);
        if (known == null) {
            known = findMix(provider);
            LAYERED.put(provider, known);
        }
        return known;
    }

    private static boolean findMix(BiomeProvider provider) {
        try {
            if (genBiomes == null) {
                genBiomes = ReflectionHelper.findField(BiomeProvider.class, "genBiomes", "field_76944_d");
                parent = ReflectionHelper.findField(GenLayer.class, "parent", "field_75909_a");
            }
            // Another mod may have wrapped the map after this stream did: follow the chain down.
            GenLayer layer = (GenLayer) genBiomes.get(provider);
            for (int depth = 0; layer != null && depth < 64; depth++) {
                if (layer instanceof OceanLayers.Mix) {
                    return true;
                }
                layer = (GenLayer) parent.get(layer);
            }
        } catch (Exception e) {
            UpdateAquaticBackport.LOGGER.warn("Could not read a biome map; its vanilla oceans keep the noise temperature.", e);
        }
        return false;
    }

    @SubscribeEvent
    public static void load(ChunkDataEvent.Load event) {
        LOADED.put(event.getChunk(), event.getData().getBoolean(TAG_NAME));
    }

    @SubscribeEvent
    public static void save(ChunkDataEvent.Save event) {
        if (realTemperatures(event.getChunk())) {
            event.getData().setBoolean(TAG_NAME, true);
        }
    }
}
