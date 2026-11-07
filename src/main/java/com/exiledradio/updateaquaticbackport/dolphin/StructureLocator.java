package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Where the nearest structure of a kind is, for whatever needs to point at one: dolphins fed a fish, and
 * later treasure maps or explorer maps.
 *
 * <h2>Temporary home</h2>
 * This is the {@code common/Structures} locator the plan asks for, kept in the dolphin package until the
 * integrator moves it into {@code common} (a stream does not change {@code common}). Moved, it keeps this
 * exact API; the dolphin package's one use of it is {@link AISwimToTreasure}. The structure stream
 * registers its locators against the {@code common} class, never against this one.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>A stream that generates a structure calls {@link #register} once (in {@code init}) for each kind
 *       it places, with the kind's 1.13 structure name in lower case: {@link #SHIPWRECK},
 *       {@link #OCEAN_RUIN}, {@link #BURIED_TREASURE}. Registering a kind again replaces its locator.</li>
 *   <li>A locator runs on the server thread, may generate nothing and load no chunks it does not have
 *       to, and returns the structure's position (any block of it, its start is fine) or null when there
 *       is none within {@code radiusChunks} chunks.</li>
 *   <li>With nothing registered every lookup returns null: dolphins fed a fish then just swim about.</li>
 * </ul>
 */
public final class StructureLocator {

    public static final String SHIPWRECK = "shipwreck";
    public static final String OCEAN_RUIN = "ocean_ruin";
    public static final String BURIED_TREASURE = "buried_treasure";

    /** Finds the nearest structure of one kind. */
    public interface Locator {

        /**
         * @param from where to search from
         * @param radiusChunks how far to search, in chunks
         * @return a position in the nearest structure of this kind, or null when there is none in range
         */
        @Nullable
        BlockPos nearest(WorldServer world, BlockPos from, int radiusChunks);
    }

    private static final Map<String, Locator> LOCATORS = new LinkedHashMap<String, Locator>();

    private StructureLocator() {
    }

    /** Registers (or replaces) the locator for a kind of structure. */
    public static synchronized void register(String kind, Locator locator) {
        LOCATORS.put(kind, locator);
    }

    /** Removes a kind's locator; for tests. */
    public static synchronized void unregister(String kind) {
        LOCATORS.remove(kind);
    }

    /** @return true when some stream can find this kind of structure */
    public static synchronized boolean has(String kind) {
        return LOCATORS.containsKey(kind);
    }

    /** @return the nearest structure of this kind, or null (none in range, or nothing locates the kind) */
    @Nullable
    public static BlockPos nearest(WorldServer world, String kind, BlockPos from, int radiusChunks) {
        Locator locator;
        synchronized (StructureLocator.class) {
            locator = LOCATORS.get(kind);
        }
        return locator == null ? null : locator.nearest(world, from, radiusChunks);
    }

    /** @return the nearest structure of any of these kinds, or null */
    @Nullable
    public static BlockPos nearestOf(WorldServer world, BlockPos from, int radiusChunks, String... kinds) {
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (String kind : kinds) {
            BlockPos pos = nearest(world, kind, from, radiusChunks);
            if (pos != null) {
                double d = (pos.getX() - from.getX()) * (double) (pos.getX() - from.getX())
                        + (pos.getZ() - from.getZ()) * (double) (pos.getZ() - from.getZ());
                if (d < bestDistance) {
                    bestDistance = d;
                    best = pos;
                }
            }
        }
        return best;
    }
}
