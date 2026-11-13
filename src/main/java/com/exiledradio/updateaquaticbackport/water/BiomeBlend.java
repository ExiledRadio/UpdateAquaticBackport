package com.exiledradio.updateaquaticbackport.water;

/**
 * 1.13's Biome Blend (Video Settings): how far the grass, foliage and water colors are averaged over the biomes
 * around a block. 1.13.2 averages a square of {@code (2r+1)x(2r+1)} columns at the block's height, one color per
 * block, with r from 0 (OFF, the block's own biome) to 7 (15x15) and 2 (5x5) by default; 1.12.2 always averages
 * 3x3 (r = 1). With 1.13's water colors, which differ far more between biomes than 1.12.2's, 3x3 gives the
 * blocky borders between, say, an ocean and a swamp.
 *
 * <p>The patch {@code asm/BiomeBlendPatch} (docs/PATCHES.md, P6) makes 1.12.2's {@code BiomeColorHelper} ask
 * these methods for its square and its divisor, so everything that tints by biome (vanilla's water, grass,
 * leaves and vines, Fluidlogged API's water, other mods' blocks) blends as 1.13 does. The client sets
 * {@link #radius} from the option ({@code lookfeel/LookAndFeel.BIOME_BLEND}). This class names no client type.
 */
public final class BiomeBlend {

    /** 1.13's default, 5x5. Read by the chunk builder threads, set by the client thread. */
    public static volatile int radius = 2;

    private BiomeBlend() {
    }

    /** @return the square's low corner offset, for x and for z ({@code BiomeColorHelper}'s -1) */
    public static int low() {
        return -radius;
    }

    /** @return the square's high corner offset, for x and for z ({@code BiomeColorHelper}'s 1) */
    public static int high() {
        return radius;
    }

    /**
     * @return the number of columns averaged ({@code BiomeColorHelper}'s 9). Read once per channel, after the square;
     * a change between the two reads affects only chunks being built at that moment, which the option's change
     * rebuilds anyway.
     */
    public static int count() {
        int side = 2 * radius + 1;
        return side * side;
    }
}
