package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.13's changes to mobs 1.12.2 already has. Nothing new is registered but seven sound events; everything
 * else is events, and AI tasks added to (or removed from) a mob when it joins a world, never from inside a
 * goal.
 *
 * <ul>
 *   <li>{@link UndeadInWater}: zombies, skeletons and undead horses sink, no undead mob drowns, baby
 *       zombies burn in daylight.</li>
 *   <li>{@link SquidInk}: a hurt squid squirts ink and flees.</li>
 *   <li>{@link PolarBearSpawning}: polar bears spawn on the ice of frozen oceans.</li>
 *   <li>{@link ParrotImitation}: parrots imitate phantoms and drowned.</li>
 *   <li>{@link SkeletonHorses}: skeleton horses keep their rider under water and are fast there; their
 *       underwater sounds are swapped in on the client.</li>
 * </ul>
 *
 * Off ({@code mobs.enabled}), every part does nothing and no sound is registered.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class MobsContent {

    /** The id of the one packet this stream sends (its block is 65-69). */
    public static final int PACKET_SQUID_INK = 65;

    public static final SoundEvent SQUID_SQUIRT = event("entity.squid.squirt");
    public static final SoundEvent PARROT_IMITATE_PHANTOM = event("entity.parrot.imitate.phantom");
    public static final SoundEvent PARROT_IMITATE_DROWNED = event("entity.parrot.imitate.drowned");
    public static final SoundEvent SKELETON_HORSE_AMBIENT_WATER = event("entity.skeleton_horse.ambient_water");
    public static final SoundEvent SKELETON_HORSE_GALLOP_WATER = event("entity.skeleton_horse.gallop_water");
    public static final SoundEvent SKELETON_HORSE_STEP_WATER = event("entity.skeleton_horse.step_water");
    public static final SoundEvent SKELETON_HORSE_SWIM = event("entity.skeleton_horse.swim");

    private MobsContent() {
    }

    public static boolean enabled() {
        return ModConfig.mobs.enabled;
    }

    private static SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().registerAll(SQUID_SQUIRT, PARROT_IMITATE_PHANTOM, PARROT_IMITATE_DROWNED,
                SKELETON_HORSE_AMBIENT_WATER, SKELETON_HORSE_GALLOP_WATER, SKELETON_HORSE_STEP_WATER, SKELETON_HORSE_SWIM);
    }

    /**
     * From the mod's post-init, once every mod has registered its entities and biomes and typed them: the
     * parrots' new imitations and the polar bears' spawn entries.
     */
    public static void postInit() {
        if (!enabled()) {
            return;
        }
        ParrotImitation.register();
        PolarBearSpawning.register();
    }
}
