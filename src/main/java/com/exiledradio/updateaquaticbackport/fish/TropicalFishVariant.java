package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

/**
 * A tropical fish's look, packed into one int as 1.13 does (and as its {@code Variant} and
 * {@code BucketVariantTag} NBT store it): shape in bits 0-7, pattern in 8-15, body color in 16-23,
 * pattern color in 24-31.
 *
 * <p>2 shapes x 6 patterns make the 12 named types (Kob ... Clayfish); colors are dye metadata, which
 * 1.12.2 numbers the same way as 1.13. 22 combinations are the "predefined" common fish with names of
 * their own; 9 in 10 wild tropical fish are one of them.
 *
 * <p>Server-safe: only numbers and translation keys, no client types.
 */
public final class TropicalFishVariant {

    /** The 12 types, index {@code pattern + 6 * shape}; their names are 1.13's translation key ends. */
    public static final String[] TYPES = {"kob", "sunstreak", "snooper", "dasher", "brinely", "spotty",
            "flopper", "stripey", "glitter", "blockfish", "betty", "clayfish"};

    /** Dye colors by metadata, under their 1.13 names (1.12.2 calls light gray "silver"). */
    public static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
            "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};

    private static final int KOB = 0, SUNSTREAK = 1, SNOOPER = 2, DASHER = 3, BRINELY = 4, SPOTTY = 5,
            FLOPPER = 6, STRIPEY = 7, GLITTER = 8, BLOCKFISH = 9, BETTY = 10, CLAYFISH = 11;
    private static final int WHITE = 0, ORANGE = 1, LIGHT_BLUE = 3, YELLOW = 4, LIME = 5, PINK = 6, GRAY = 7,
            CYAN = 9, PURPLE = 10, BLUE = 11, RED = 14;

    /**
     * The 22 predefined fish, in 1.13's order (their names are {@code ...predefined.<index>}): type, body
     * color, pattern color.
     */
    public static final int[] PREDEFINED = {
            of(STRIPEY, ORANGE, GRAY), of(FLOPPER, GRAY, GRAY), of(FLOPPER, GRAY, BLUE), of(CLAYFISH, WHITE, GRAY),
            of(SUNSTREAK, BLUE, GRAY), of(KOB, ORANGE, WHITE), of(SPOTTY, PINK, LIGHT_BLUE), of(BLOCKFISH, PURPLE, YELLOW),
            of(CLAYFISH, WHITE, RED), of(SPOTTY, WHITE, YELLOW), of(GLITTER, WHITE, GRAY), of(CLAYFISH, WHITE, ORANGE),
            of(DASHER, CYAN, PINK), of(BRINELY, LIME, LIGHT_BLUE), of(BETTY, RED, WHITE), of(SNOOPER, GRAY, RED),
            of(BLOCKFISH, RED, WHITE), of(FLOPPER, WHITE, YELLOW), of(KOB, RED, WHITE), of(SUNSTREAK, GRAY, WHITE),
            of(DASHER, CYAN, YELLOW), of(FLOPPER, YELLOW, YELLOW)};

    private TropicalFishVariant() {
    }

    private static int of(int type, int body, int pattern) {
        return pack(type / 6, type % 6, body, pattern);
    }

    public static int pack(int shape, int pattern, int bodyColor, int patternColor) {
        return shape & 255 | (pattern & 255) << 8 | (bodyColor & 255) << 16 | (patternColor & 255) << 24;
    }

    /** @return 0 (the small, "A" body) or 1 (the tall, "B" body) */
    public static int shape(int variant) {
        return Math.min(variant & 255, 1);
    }

    /** @return 0-5 */
    public static int pattern(int variant) {
        return Math.min((variant >> 8) & 255, 5);
    }

    /** @return dye metadata 0-15 */
    public static int bodyColor(int variant) {
        return clampColor((variant >> 16) & 255);
    }

    /** @return dye metadata 0-15 */
    public static int patternColor(int variant) {
        return clampColor((variant >>> 24) & 255);
    }

    private static int clampColor(int c) {
        // 1.13's DyeColor.byId: anything out of range is white.
        return c < 0 || c >= COLORS.length ? 0 : c;
    }

    /** @return the index into {@link #PREDEFINED}, or -1 for a fish of no common kind */
    public static int predefinedIndex(int variant) {
        for (int i = 0; i < PREDEFINED.length; i++) {
            if (PREDEFINED[i] == variant) {
                return i;
            }
        }
        return -1;
    }

    public static String predefinedKey(int index) {
        return "entity." + UpdateAquaticBackport.MOD_ID + ".tropical_fish.predefined." + index;
    }

    public static String typeKey(int variant) {
        return "entity." + UpdateAquaticBackport.MOD_ID + ".tropical_fish.type." + TYPES[pattern(variant) + 6 * shape(variant)];
    }

    public static String colorKey(int color) {
        return "color." + UpdateAquaticBackport.MOD_ID + "." + COLORS[clampColor(color)];
    }
}
