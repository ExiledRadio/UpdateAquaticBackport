package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 1.13's ocean biomes: warm, lukewarm, cold and frozen oceans and the deep lukewarm, deep cold and deep
 * frozen oceans (no deep warm ocean: 1.13 never generated it and today's game has removed it).
 *
 * <p>Registers the seven biomes, declares each to {@link OceanKind} so every stream's generation follows
 * it, and turns on the generation hooks in {@link OceanTerrain}. Vanilla's ocean and deep ocean become
 * 1.13's "normal" temperature in chunks these biomes' map made: that is exactly where they still generate
 * ({@link OceanChunks}).
 * Off ({@code oceans.enabled}), nothing is registered and no hook runs.
 *
 * <p>Biome names: 1.12.2 has no translation for biome names (vanilla's are English strings too, shown only
 * in the debug screen), so each is 1.18's English name, checked against 1.18's language file by
 * {@code tools/ocean/generate.py}.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class OceanContent {

    private static BiomeAquaticOcean warm;
    private static BiomeAquaticOcean lukewarm;
    private static BiomeAquaticOcean deepLukewarm;
    private static BiomeAquaticOcean cold;
    private static BiomeAquaticOcean deepCold;
    private static BiomeFrozenOcean frozen;
    private static BiomeFrozenOcean deepFrozen;
    private static final List<Biome> BIOMES = new ArrayList<Biome>();

    private OceanContent() {
    }

    public static boolean enabled() {
        return ModConfig.oceans.enabled;
    }

    @Nullable
    public static BiomeAquaticOcean warm() {
        return warm;
    }

    @Nullable
    public static BiomeAquaticOcean lukewarm() {
        return lukewarm;
    }

    @Nullable
    public static BiomeAquaticOcean deepLukewarm() {
        return deepLukewarm;
    }

    @Nullable
    public static BiomeAquaticOcean cold() {
        return cold;
    }

    @Nullable
    public static BiomeAquaticOcean deepCold() {
        return deepCold;
    }

    @Nullable
    public static BiomeFrozenOcean frozen() {
        return frozen;
    }

    @Nullable
    public static BiomeFrozenOcean deepFrozen() {
        return deepFrozen;
    }

    /** @return the seven biomes, in registration order; empty when the stream is off */
    public static List<Biome> biomes() {
        return Collections.unmodifiableList(BIOMES);
    }

    private static Biome.BiomeProperties properties(String name, boolean deep, float temperature) {
        return new Biome.BiomeProperties(name).setBaseHeight(deep ? -1.8F : -1.0F).setHeightVariation(0.1F)
                .setTemperature(temperature).setRainfall(0.5F);
    }

    @SubscribeEvent
    public static void registerBiomes(RegistryEvent.Register<Biome> event) {
        if (!enabled()) {
            return;
        }
        IBlockState sand = Blocks.SAND.getDefaultState();
        IBlockState grass = Blocks.GRASS.getDefaultState();
        IBlockState dirt = Blocks.DIRT.getDefaultState();
        IBlockState gravel = Blocks.GRAVEL.getDefaultState();

        // 1.14.4's numbers: depth, scale, temperature, downfall, floors and squid spawns.
        warm = new BiomeAquaticOcean(properties("Warm Ocean", false, 0.5F), OceanKind.WARM, false,
                sand, sand, sand, 10, 4, 4);
        lukewarm = new BiomeAquaticOcean(properties("Lukewarm Ocean", false, 0.5F), OceanKind.LUKEWARM, false,
                grass, dirt, sand, 10, 1, 2);
        deepLukewarm = new BiomeAquaticOcean(properties("Deep Lukewarm Ocean", true, 0.5F), OceanKind.LUKEWARM, true,
                grass, dirt, sand, 8, 1, 4);
        cold = new BiomeAquaticOcean(properties("Cold Ocean", false, 0.5F), OceanKind.COLD, false,
                grass, dirt, gravel, 3, 1, 4);
        deepCold = new BiomeAquaticOcean(properties("Deep Cold Ocean", true, 0.5F), OceanKind.COLD, true,
                grass, dirt, gravel, 3, 1, 4);
        frozen = new BiomeFrozenOcean(properties("Frozen Ocean", false, 0.0F).setSnowEnabled(), false, 1, 1, 4);
        deepFrozen = new BiomeFrozenOcean(properties("Deep Frozen Ocean", true, 0.5F), true, 1, 1, 4);

        register(event, "warm_ocean", warm, BiomeDictionary.Type.HOT);
        register(event, "lukewarm_ocean", lukewarm);
        register(event, "deep_lukewarm_ocean", deepLukewarm);
        register(event, "cold_ocean", cold, BiomeDictionary.Type.COLD);
        register(event, "deep_cold_ocean", deepCold, BiomeDictionary.Type.COLD);
        register(event, "frozen_ocean", frozen, BiomeDictionary.Type.COLD, BiomeDictionary.Type.SNOWY);
        register(event, "deep_frozen_ocean", deepFrozen, BiomeDictionary.Type.COLD);

        // Where these biomes are in the map, vanilla's oceans are exactly 1.13's "normal" ones; chunks made
        // before (or by a map these layers do not reach) keep the noise.
        OceanChunks.install();

        // Worldgen hooks that are not registry events: this event comes before any world exists.
        MinecraftForge.TERRAIN_GEN_BUS.register(OceanTerrain.Generation.class);
        MinecraftForge.EVENT_BUS.register(OceanTerrain.Population.class);
    }

    private static void register(RegistryEvent.Register<Biome> event, String name, BiomeAquaticOcean biome,
                                 BiomeDictionary.Type... types) {
        biome.setRegistryName(Content.id(name));
        event.getRegistry().register(biome);
        BiomeDictionary.addTypes(biome, BiomeDictionary.Type.OCEAN);
        if (types.length > 0) {
            BiomeDictionary.addTypes(biome, types);
        }
        OceanKind.register(biome, biome.kind());
        BiomeManager.oceanBiomes.add(biome);
        BIOMES.add(biome);
    }
}
