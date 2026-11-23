package com.exiledradio.updateaquaticbackport.compat;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.ocean.OceanContent;

import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.Loader;

import javax.annotation.Nullable;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Realistic Terrain Generation (the original 6.x and the Unofficial fork, both modid {@code rtg}).
 *
 * <h2>Biomes</h2>
 * RTG builds every column from a "realistic biome" registered for the column's biome; a biome nobody registered
 * becomes RTG's patch biome (plains by default), terrain, surface and biome id alike. So without a registration
 * this mod's oceans are put in the biome map and then turned into plains. {@link #postInit} registers one for each
 * of them ({@link RtgOceans}, the only class that links against RTG), before RTG locks its list at load complete.
 *
 * <h2>Where the biome is read</h2>
 * RTG then "repairs" the map per chunk: oceans along coasts become beach or land to match its terrain, and the
 * chunk keeps the repaired biomes. The biome provider still answers with the unrepaired map, so a structure that
 * asks it puts a shipwreck where RTG made a beach. {@link #biomeAt} reads the repaired biome from RTG's landscape
 * instead (cached by RTG, and reused when the chunk is made). Everything here but the registration is reflection.
 */
public final class RtgCompat {

    public static final String MODID = "rtg";
    private static final String GENERATOR = "rtg.world.gen.ChunkGeneratorRTG";

    private static boolean failed;
    private static Method landscape;
    private static Field biomes;
    private static Method baseBiome;

    private RtgCompat() {
    }

    /** Post-init: RTG registers its own biomes in its post-init and locks the list at load complete. */
    public static void postInit() {
        if (!Loader.isModLoaded(MODID) || !OceanContent.enabled()) {
            return;
        }
        try {
            int count = RtgOceans.register(OceanContent.biomes());
            UpdateAquaticBackport.LOGGER.info("Registered {} oceans with Realistic Terrain Generation.", count);
        } catch (Exception | LinkageError e) {
            UpdateAquaticBackport.LOGGER.warn("Could not register the oceans with Realistic Terrain Generation; "
                    + "in its worlds they generate as its patch biome.", e);
        }
    }

    @Nullable
    private static IChunkGenerator generator(World world) {
        if (!(world instanceof WorldServer)) {
            return null;
        }
        ChunkProviderServer provider = ((WorldServer) world).getChunkProvider();
        IChunkGenerator generator = provider == null ? null : provider.chunkGenerator;
        return generator != null && GENERATOR.equals(generator.getClass().getName()) ? generator : null;
    }

    /** @return true if this world's chunks come from RTG */
    public static boolean handles(World world) {
        return generator(world) != null;
    }

    /**
     * @return the biome RTG gives this column once its chunk is made (after its coast repair), without making the
     * chunk; null outside RTG worlds or if its landscape cannot be read
     */
    @Nullable
    public static Biome biomeAt(World world, int x, int z) {
        IChunkGenerator generator = failed ? null : generator(world);
        if (generator == null) {
            return null;
        }
        try {
            if (landscape == null) {
                landscape = generator.getClass().getMethod("getLandscape", BiomeProvider.class, ChunkPos.class);
                biomes = landscape.getReturnType().getField("biome");
                baseBiome = Class.forName("rtg.api.world.biome.IRealisticBiome").getMethod("baseBiome");
            }
            Object chunk = landscape.invoke(generator, world.getBiomeProvider(), new ChunkPos(x >> 4, z >> 4));
            // Indexed like the landscape's heights, x * 16 + z: an array in RTG 6, a list in the Unofficial fork.
            Object all = biomes.get(chunk);
            int index = (x & 15) * 16 + (z & 15);
            Object realistic = all instanceof List ? ((List<?>) all).get(index) : Array.get(all, index);
            return realistic == null ? null : (Biome) baseBiome.invoke(realistic);
        } catch (Exception | LinkageError e) {
            failed = true;
            UpdateAquaticBackport.LOGGER.warn("Could not read Realistic Terrain Generation's biomes; ocean structures "
                    + "in its worlds read the unrepaired biome map.", e);
            return null;
        }
    }
}
