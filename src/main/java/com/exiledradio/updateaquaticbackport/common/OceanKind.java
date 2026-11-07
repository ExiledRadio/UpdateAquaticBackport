package com.exiledradio.updateaquaticbackport.common;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.common.BiomeDictionary;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Which of 1.13's five ocean temperatures a position belongs to.
 *
 * <p>Kelp grows in every ocean but the warm ones, coral reefs only in warm ones, cod in cold to
 * lukewarm, tropical fish in warm and lukewarm. Every stream that decorates or populates an ocean asks
 * this class, so they agree with each other, and it works with or without 1.13's own ocean biomes:
 *
 * <ul>
 *   <li>A biome registered with {@link #register} - the oceans stream registers its warm, lukewarm,
 *       cold and frozen oceans and their deep forms - is that kind.</li>
 *   <li>1.12.2's frozen ocean is {@link #FROZEN}.</li>
 *   <li>Vanilla's ocean and deep ocean are {@link #NORMAL} where the world's biome map has 1.13's
 *       temperatures in it: there they are exactly the cells the map left at the normal temperature. The
 *       oceans stream says where that is ({@link #useRealTemperatures}): chunks its biome map made, but
 *       not chunks generated before it was switched on, nor worlds and world types its layers do not
 *       reach.</li>
 *   <li>Any other ocean (vanilla's elsewhere, or another mod's, by the biome dictionary) gets its
 *       temperature from a large-scale noise seeded by the world seed, so a world without the new
 *       biomes still has warm seas with reefs and cold seas with kelp, in patches a few hundred blocks
 *       across, the same for every stream and every chunk. Noise never says frozen: frozen water needs
 *       the frozen biome.</li>
 * </ul>
 */
public enum OceanKind {
    WARM, LUKEWARM, NORMAL, COLD, FROZEN,
    /** Not an ocean. Rivers, swamps and beaches are NONE too; ask the biome dictionary for those. */
    NONE;

    /** 1.13's ocean temperature thresholds on its temperature noise. */
    private static final double WARM_ABOVE = 0.4, LUKEWARM_ABOVE = 0.2, COLD_BELOW = -0.2;

    /** Blocks per noise unit: patches a few hundred blocks across, as 1.13's ocean temperature layer. */
    private static final double SCALE = 1.0 / 768.0;

    private static final Map<Biome, OceanKind> REGISTERED = new IdentityHashMap<Biome, OceanKind>();

    private static long noiseSeed;
    private static NoiseGeneratorSimplex noise;

    /** Where vanilla's oceans carry 1.13's temperatures; null without the oceans stream. */
    private static volatile RealTemperatures realTemperatures;

    /** Answers whether the biome at a column of a world came from a map with 1.13's ocean temperatures. */
    public interface RealTemperatures {
        boolean at(World world, int x, int z);
    }

    /** Declares a biome to be an ocean of this kind. Called by whatever registers the biome. */
    public static void register(Biome biome, OceanKind kind) {
        REGISTERED.put(biome, kind);
    }

    /** Called by the oceans stream, which knows where its biome map has been. */
    public static void useRealTemperatures(RealTemperatures rule) {
        realTemperatures = rule;
    }

    /** @return the ocean kind at a position; {@link #NONE} if it is not in an ocean biome */
    public static OceanKind at(World world, BlockPos pos) {
        return of(world, world.getBiome(pos), pos.getX(), pos.getZ());
    }

    /**
     * @return the ocean kind of a biome at a column of a world: as {@link #of(Biome, long, int, int)}, except
     * that vanilla's ocean and deep ocean are {@link #NORMAL} where the world's map has 1.13's temperatures
     */
    public static OceanKind of(World world, Biome biome, int x, int z) {
        RealTemperatures rule = realTemperatures;
        if ((biome == Biomes.OCEAN || biome == Biomes.DEEP_OCEAN) && rule != null && rule.at(world, x, z)) {
            return NORMAL;
        }
        return of(biome, world.getSeed(), x, z);
    }

    /**
     * @return the ocean kind of a biome at a column, for a world with this seed, as in a map without 1.13's
     * temperatures: vanilla's ocean and deep ocean take theirs from the noise
     */
    public static OceanKind of(Biome biome, long seed, int x, int z) {
        OceanKind kind = REGISTERED.get(biome);
        if (kind != null) {
            return kind;
        }
        if (biome == Biomes.FROZEN_OCEAN) {
            return FROZEN;
        }
        if (!BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
            return NONE;
        }
        if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
            return FROZEN;
        }
        return fromNoise(seed, x, z);
    }

    /** @return the temperature an ordinary ocean has at a column */
    public static OceanKind fromNoise(long seed, int x, int z) {
        double t = temperature(seed, x, z);
        if (t > WARM_ABOVE) {
            return WARM;
        }
        if (t > LUKEWARM_ABOVE) {
            return LUKEWARM;
        }
        if (t < COLD_BELOW) {
            return COLD;
        }
        return NORMAL;
    }

    /** @return the raw noise, about -1 to 1 */
    public static synchronized double temperature(long seed, int x, int z) {
        if (noise == null || noiseSeed != seed) {
            // A salt keeps this independent of every other noise seeded from the world seed.
            noise = new NoiseGeneratorSimplex(new Random(seed ^ 0x0CEA7E3A9L));
            noiseSeed = seed;
        }
        return noise.getValue(x * SCALE, z * SCALE);
    }

    /** @return true for warm and lukewarm, where tropical fish and pufferfish live */
    public boolean isWarmish() {
        return this == WARM || this == LUKEWARM;
    }

    /** @return true for any ocean */
    public boolean isOcean() {
        return this != NONE;
    }
}
