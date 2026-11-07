package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.SoundType;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Sounds more than one stream uses, registered once.
 *
 * <p>1.13's {@code wet_grass} sound type is kelp's, seagrass's and every coral plant and fan's. Its
 * {@link SoundType} is built here before any block registers (sounds register first at
 * {@link EventPriority#HIGHEST}; blocks take the type in their constructors, which run in the block
 * event, and the sound registry event comes after it in 1.12 - so the events are created here, eagerly,
 * and only <em>registered</em> in the event).
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class SharedSounds {

    public static final SoundEvent WET_GRASS_BREAK = event("block.wet_grass.break");
    public static final SoundEvent WET_GRASS_STEP = event("block.wet_grass.step");
    public static final SoundEvent WET_GRASS_PLACE = event("block.wet_grass.place");
    public static final SoundEvent WET_GRASS_HIT = event("block.wet_grass.hit");
    public static final SoundEvent WET_GRASS_FALL = event("block.wet_grass.fall");

    /** 1.13's {@code SoundType.WET_GRASS}: volume 1, pitch 1. */
    public static final SoundType WET_GRASS = new SoundType(1.0F, 1.0F,
            WET_GRASS_BREAK, WET_GRASS_STEP, WET_GRASS_PLACE, WET_GRASS_HIT, WET_GRASS_FALL);

    private SharedSounds() {
    }

    private static SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        event.getRegistry().registerAll(WET_GRASS_BREAK, WET_GRASS_STEP, WET_GRASS_PLACE, WET_GRASS_HIT, WET_GRASS_FALL);
    }
}
