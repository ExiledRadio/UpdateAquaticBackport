package com.exiledradio.updateaquaticbackport.compat;

import net.minecraft.init.Blocks;
import net.minecraft.world.biome.Biome;

import rtg.api.RTGAPI;
import rtg.api.world.biome.IRealisticBiome;
import rtg.api.world.biome.RealisticBiomeBase;
import rtg.api.world.deco.collection.DecoCollectionOcean;
import rtg.api.world.surface.SurfaceBase;
import rtg.api.world.terrain.TerrainBase;
import rtg.world.biome.realistic.vanilla.RealisticBiomeVanillaDeepOcean;
import rtg.world.biome.realistic.vanilla.RealisticBiomeVanillaOcean;

import java.util.List;

/**
 * The oceans' realistic biomes. Loaded only from {@link RtgCompat#postInit} with RTG installed: this is the one class
 * that links against RTG. Compiled against the Unofficial fork; every RTG type and member used here is the same in
 * RTG 6.1, except the registration call, which is found by reflection.
 */
final class RtgOceans {

    private RtgOceans() {
    }

    static int register(List<Biome> oceans) throws ReflectiveOperationException {
        knownMod(oceans);
        IRealisticBiome[] made = new IRealisticBiome[oceans.size()];
        for (int i = 0; i < made.length; i++) {
            made[i] = new Ocean(oceans.get(i));
        }
        try {
            // The Unofficial fork.
            RTGAPI.class.getMethod("addRTGBiomes", IRealisticBiome[].class).invoke(null, (Object) made);
        } catch (NoSuchMethodException e) {
            // RTG 6.1: RTGAPI.RTG_BIOMES is a BiomeMap with addBiomes.
            Object map = RTGAPI.class.getField("RTG_BIOMES").get(null);
            map.getClass().getMethod("addBiomes", IRealisticBiome[].class).invoke(map, (Object) made);
        }
        return made.length;
    }

    /**
     * A realistic biome keeps its settings in {@code config/RTG/biomes/<mod>/<biome>.cfg}, the mod looked up by the
     * biome's namespace in RTG's {@code ModCompat.Mods} enum (by constant name), and the constructor throws for a mod
     * the enum does not list. So this mod is added to it, as a constant named after its mod id, with Forge's
     * {@link net.minecraftforge.common.util.EnumHelper}; RTG never switches over the enum, so a new constant is safe.
     * The enum is {@code rtg.compat.ModCompat$Mods} in the Unofficial fork, {@code rtg.util.ModCompat$Mods} in 6.1.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void knownMod(List<Biome> oceans) throws ClassNotFoundException {
        if (oceans.isEmpty() || oceans.get(0).getRegistryName() == null) {
            return;
        }
        String modid = oceans.get(0).getRegistryName().getNamespace();
        Class mods;
        try {
            mods = Class.forName("rtg.compat.ModCompat$Mods");
        } catch (ClassNotFoundException e) {
            mods = Class.forName("rtg.util.ModCompat$Mods");
        }
        for (Object constant : mods.getEnumConstants()) {
            if (((Enum<?>) constant).name().equals(modid)) {
                return;
            }
        }
        net.minecraftforge.common.util.EnumHelper.addEnum(mods, modid, new Class<?>[] {String.class}, "UpdateAquaticBackport");
    }

    /**
     * One of the oceans, built like RTG's own vanilla oceans: its ocean or deep ocean terrain (by the biome's base
     * height, -1.0 or -1.8), its ocean surface laid with the biome's own top and filler blocks, its ocean decorations,
     * no rivers or lakes. RTG still runs the biome's own decoration, so icebergs and the like come as usual.
     */
    static final class Ocean extends RealisticBiomeBase {

        Ocean(Biome biome) {
            super(biome);
        }

        /** Called from the super constructor, where {@link #baseBiome()} is already set. */
        private boolean deep() {
            return baseBiome().getBaseHeight() < -1.4F;
        }

        @Override
        public void initConfig() {
            getConfig().SURFACE_WATER_LAKE_MULT.set(0.0F);
            getConfig().ALLOW_RIVERS.set(false);
            getConfig().ALLOW_SCENIC_LAKES.set(false);
            getConfig().addProperty(getConfig().ALLOW_SPONGE).set(true);
            getConfig().addProperty(getConfig().SURFACE_MIX_BLOCK).set("");
        }

        @Override
        public TerrainBase initTerrain() {
            return deep() ? new RealisticBiomeVanillaDeepOcean.TerrainVanillaDeepOcean()
                    : new RealisticBiomeVanillaOcean.TerrainVanillaOcean();
        }

        @Override
        public SurfaceBase initSurface() {
            Biome biome = baseBiome();
            if (deep()) {
                return new RealisticBiomeVanillaDeepOcean.SurfaceVanillaDeepOcean(getConfig(), biome.topBlock,
                        biome.fillerBlock, Blocks.CLAY.getDefaultState(), 20.0F, 0.1F);
            }
            return new RealisticBiomeVanillaOcean.SurfaceVanillaOcean(getConfig(), biome.topBlock, biome.fillerBlock,
                    Blocks.GRAVEL.getDefaultState(), 20.0F, 0.2F);
        }

        @Override
        public void initDecos() {
            addDecoCollection(new DecoCollectionOcean(getConfig()));
        }
    }
}
