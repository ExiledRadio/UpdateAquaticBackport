package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * A {@code switch} on an enum compiles to a lookup in an {@code int[]} that a synthetic class ({@code Biome$1})
 * fills in when it first runs, one entry per enum value at that moment. A value added afterwards
 * ({@code EnumHelper}) has an ordinal past the end, and the switch throws
 * {@code ArrayIndexOutOfBoundsException} instead of taking its default branch.
 *
 * <p>The mod adds its value before any switch can have run (see {@code FishContent.createWaterAmbient}); this
 * is the second line, for a table something filled even earlier: it is replaced by a longer copy whose new
 * entries are 0, the default branch.
 */
final class SwitchTables {

    private SwitchTables() {
    }

    /**
     * Lengthens every switch table in {@code holder} that is shorter than {@code type} has values.
     *
     * @param holder the synthetic class's name ({@code "net.minecraft.world.biome.Biome$1"})
     */
    static void grow(Class<? extends Enum<?>> type, String holder) {
        int values = type.getEnumConstants().length;
        try {
            Class<?> c = Class.forName(holder, true, SwitchTables.class.getClassLoader());
            for (Field f : c.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int[].class) {
                    continue;
                }
                f.setAccessible(true);
                int[] table = (int[]) f.get(null);
                if (table == null || table.length >= values) {
                    continue;
                }
                Field theUnsafe = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
                theUnsafe.setAccessible(true);
                sun.misc.Unsafe unsafe = (sun.misc.Unsafe) theUnsafe.get(null);
                unsafe.putObjectVolatile(unsafe.staticFieldBase(f), unsafe.staticFieldOffset(f), Arrays.copyOf(table, values));
                UpdateAquaticBackport.LOGGER.info("Lengthened {}'s {} table from {} to {} entries.", holder,
                        type.getSimpleName(), table.length, values);
            }
        } catch (Throwable t) {
            // No such class in this build of the game, or a JVM that will not hand out the field: the early add
            // is what matters, and it has happened.
            UpdateAquaticBackport.LOGGER.debug("Could not check {}: {}", holder, t.toString());
        }
    }
}
