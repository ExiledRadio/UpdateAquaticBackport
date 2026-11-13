package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.terrain.ModernGeneration;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.IChunkGenerator;

import javax.annotation.Nullable;

/**
 * Worlds whose chunk generator runs a newer version's world generation in full (Backport Core's
 * {@code ModernGeneration}: Caves &amp; Cliffs Backport's 1.18 terrain, world type {@code cavesandcliffs}). That
 * generator already places 1.18.2's coral reefs, kelp, seagrass, sea pickles and icebergs in 1.18's biomes, so this
 * mod's generators of them step aside there, and the ocean rules read its biome in three dimensions.
 *
 * <p>Default worlds, and Caves &amp; Cliffs worlds that kept 1.12's terrain, have no such generator: everything
 * there is as before.
 */
public final class ModernWorlds {

    /** False once a Backport Core without {@code ModernGeneration} turned out to be installed. */
    private static boolean available = true;

    private ModernWorlds() {
    }

    /** @return true if this world's generator places today's features itself (its {@code IWorldGenerator} hand-off) */
    public static boolean placesFeatures(World world, @Nullable IChunkGenerator generator) {
        if (!available || world.isRemote) {
            return false;
        }
        try {
            return ModernGeneration.of(generator) != null || ModernGeneration.of(world) != null;
        } catch (LinkageError e) {
            missing(e);
            return false;
        }
    }

    /** @return true if this world's generator places today's features itself */
    public static boolean placesFeatures(World world) {
        return placesFeatures(world, null);
    }

    /**
     * @return today's biome at a block in three dimensions, without its namespace ({@code "warm_ocean"}), in a world
     * whose generator knows it; null in any other world
     */
    @Nullable
    public static String biomeAt(World world, BlockPos pos) {
        if (!available || world.isRemote) {
            return null;
        }
        try {
            String id = ModernGeneration.biomeId(world, pos);
            if (id == null) {
                return null;
            }
            int colon = id.indexOf(':');
            return colon < 0 ? id : id.substring(colon + 1);
        } catch (LinkageError e) {
            missing(e);
            return null;
        }
    }

    private static void missing(LinkageError e) {
        available = false;
        UpdateAquaticBackport.LOGGER.warn("This Backport Core has no ModernGeneration; update it, or Update Aquatic's "
                + "reefs, kelp and seagrass also generate in Caves & Cliffs 1.18 worlds. ({})", e.toString());
    }
}
