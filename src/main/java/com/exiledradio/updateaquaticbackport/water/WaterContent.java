package com.exiledradio.updateaquaticbackport.water;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * How water looks and feels, as 1.13 changed it. Nothing here is a block or an item: it is the water's
 * texture and colors ({@link WaterColors}), the view under water and its sounds ({@code client/WaterClient}),
 * light through water (here), air coming back gradually ({@link GradualAir}), items and orbs floating
 * ({@link FloatingItems}) and the jump out of shallow water ({@link ShallowWater}).
 *
 * <p>Off ({@code water.enabled}), water behaves as 1.12.2's. Each part has its own switch too.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class WaterContent {

    /** 1.13's water light opacity: light drops by 1 per block of water. */
    public static final int WATER_OPACITY = 1;

    public static final SoundEvent UNDERWATER_ENTER = event("ambient.underwater.enter");
    public static final SoundEvent UNDERWATER_EXIT = event("ambient.underwater.exit");
    public static final SoundEvent UNDERWATER_LOOP = event("ambient.underwater.loop");
    public static final SoundEvent UNDERWATER_ADDITIONS = event("ambient.underwater.loop.additions");
    public static final SoundEvent UNDERWATER_ADDITIONS_RARE = event("ambient.underwater.loop.additions.rare");
    public static final SoundEvent UNDERWATER_ADDITIONS_ULTRA_RARE = event("ambient.underwater.loop.additions.ultra_rare");

    private WaterContent() {
    }

    public static boolean enabled() {
        return ModConfig.water.enabled;
    }

    public static ModConfig.Water config() {
        return ModConfig.water;
    }

    private static SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    /** Registered whenever the stream is on, even with the ambience switched off, so the ids never come and go. */
    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(UNDERWATER_ENTER, UNDERWATER_EXIT, UNDERWATER_LOOP, UNDERWATER_ADDITIONS,
                    UNDERWATER_ADDITIONS_RARE, UNDERWATER_ADDITIONS_ULTRA_RARE);
        }
    }

    /**
     * Light through water. The block event runs after vanilla's blocks exist and before any world loads,
     * on both sides, so client and server light water alike. Light already stored in saved chunks stays
     * until something there changes, as 1.13's "newly placed water only".
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void lightThroughWater(RegistryEvent.Register<Block> event) {
        if (enabled() && config().lightThroughWater) {
            Blocks.WATER.setLightOpacity(WATER_OPACITY);
            Blocks.FLOWING_WATER.setLightOpacity(WATER_OPACITY);
        }
    }
}
