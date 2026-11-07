package com.exiledradio.updateaquaticbackport.water;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.event.terraingen.BiomeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 1.13's water colors and underwater fog colors, by biome.
 *
 * <h2>Why the texture changes too</h2>
 * 1.12.2's water texture is already blue and every biome but the swamp tints it white. 1.13's texture is
 * gray and each biome gives the real color (most of them 1.13's blue, {@code 0x3F76E4}). The client puts
 * 1.13's texture in place ({@code client/WaterClient}); this class gives the colors through Forge's
 * {@link BiomeEvent.GetWaterColor}, which every water tint in 1.12.2 asks: vanilla's water, Forge's fluid
 * renderer and our own water surfaces ({@code BiomeColorHelper.getWaterColorAtPos}).
 *
 * <h2>Which color</h2>
 * <ul>
 *   <li>Vanilla's biomes: 1.14.4's values for the same biome (its {@code Biome.Builder.waterColor} and
 *       {@code waterFogColor}). Only the swamps, the frozen ocean and river, and the snowy taigas and
 *       beach differ from the default.</li>
 *   <li>Our own ocean biomes (the oceans stream), by registry name: warm, lukewarm, cold and frozen, and
 *       the deep forms, with 1.14.4's values.</li>
 *   <li>Any other mod's biome: its 1.12.2 tint was a filter over the old blue water, so it stays a filter
 *       over the new default blue, channel by channel. A biome that left the tint white gets the default.
 *       Its fog is the default fog.</li>
 * </ul>
 *
 * <p>The event runs on both sides, so the colors can be checked on a dedicated server; only the client
 * draws them.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class WaterColors {

    /** 1.13's default water color and underwater fog color. */
    public static final int DEFAULT_WATER = 0x3F76E4, DEFAULT_FOG = 0x050533;

    private static final int SWAMP_WATER = 0x617B64, SWAMP_FOG = 0x232317;
    private static final int FROZEN_WATER = 0x3938C9;
    private static final int COLD_WATER = 0x3D57D6;
    private static final int LUKEWARM_WATER = 0x45ADF2, LUKEWARM_FOG = 0x041633;
    private static final int WARM_WATER = 0x43D5EE, WARM_FOG = 0x041F33;

    /** {water, fog} for vanilla's biomes that differ from the default. */
    private static Map<Biome, int[]> vanilla;

    /** {water, fog} for our ocean biomes, by registry path. */
    private static final Map<String, int[]> OURS = new HashMap<String, int[]>();

    static {
        OURS.put("warm_ocean", new int[] {WARM_WATER, WARM_FOG});
        OURS.put("lukewarm_ocean", new int[] {LUKEWARM_WATER, LUKEWARM_FOG});
        OURS.put("deep_lukewarm_ocean", new int[] {LUKEWARM_WATER, LUKEWARM_FOG});
        OURS.put("cold_ocean", new int[] {COLD_WATER, DEFAULT_FOG});
        OURS.put("deep_cold_ocean", new int[] {COLD_WATER, DEFAULT_FOG});
        OURS.put("frozen_ocean", new int[] {FROZEN_WATER, DEFAULT_FOG});
        OURS.put("deep_frozen_ocean", new int[] {FROZEN_WATER, DEFAULT_FOG});
    }

    /** Off when the client could not put 1.13's texture in place: the colors only suit that texture. */
    private static boolean textureMissing;

    private WaterColors() {
    }

    /** The switch as it was at startup: the texture is chosen once, so the colors follow it until a restart. */
    private static Boolean configured;

    /** @return true when biome water colors are on */
    public static boolean active() {
        if (configured == null) {
            configured = WaterContent.enabled() && WaterContent.config().biomeColors;
        }
        return configured && !textureMissing;
    }

    /** Called by the client when 1.13's water texture could not be put in place. */
    public static void textureMissing() {
        textureMissing = true;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onWaterColor(BiomeEvent.GetWaterColor event) {
        if (active()) {
            event.setNewColor(water(event.getBiome(), event.getNewColor()));
        }
    }

    /**
     * @param tint the biome's own 1.12.2 tint
     * @return 1.13's water color for a biome
     */
    public static int water(Biome biome, int tint) {
        int[] known = known(biome);
        if (known != null) {
            return known[0];
        }
        ResourceLocation id = biome.getRegistryName();
        if (id == null || "minecraft".equals(id.getNamespace()) || (tint & 0xFFFFFF) == 0xFFFFFF) {
            return DEFAULT_WATER;
        }
        return filter(tint, DEFAULT_WATER);
    }

    /** @return 1.13's underwater fog color for a biome */
    public static int fog(Biome biome) {
        int[] known = known(biome);
        return known != null ? known[1] : DEFAULT_FOG;
    }

    /** @return one color multiplied by another, channel by channel, as a tint over a texture */
    static int filter(int tint, int base) {
        int r = ((tint >> 16) & 0xFF) * ((base >> 16) & 0xFF) / 255;
        int g = ((tint >> 8) & 0xFF) * ((base >> 8) & 0xFF) / 255;
        int b = (tint & 0xFF) * (base & 0xFF) / 255;
        return r << 16 | g << 8 | b;
    }

    private static int[] known(Biome biome) {
        int[] v = vanilla().get(biome);
        if (v != null) {
            return v;
        }
        ResourceLocation id = biome.getRegistryName();
        if (id != null && UpdateAquaticBackport.MOD_ID.equals(id.getNamespace())) {
            return OURS.get(id.getPath());
        }
        return null;
    }

    private static synchronized Map<Biome, int[]> vanilla() {
        if (vanilla == null) {
            Map<Biome, int[]> m = new IdentityHashMap<Biome, int[]>();
            for (Biome b : new Biome[] {Biomes.SWAMPLAND, Biomes.MUTATED_SWAMPLAND}) {
                m.put(b, new int[] {SWAMP_WATER, SWAMP_FOG});
            }
            for (Biome b : new Biome[] {Biomes.FROZEN_OCEAN, Biomes.FROZEN_RIVER}) {
                m.put(b, new int[] {FROZEN_WATER, DEFAULT_FOG});
            }
            for (Biome b : new Biome[] {Biomes.COLD_BEACH, Biomes.COLD_TAIGA, Biomes.COLD_TAIGA_HILLS, Biomes.MUTATED_TAIGA_COLD}) {
                m.put(b, new int[] {COLD_WATER, DEFAULT_FOG});
            }
            vanilla = m;
        }
        return vanilla;
    }
}
