package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * The trident's and the drowned's sounds, under 1.13's event names in this mod's namespace.
 *
 * <p>Built eagerly so the entity and item classes can hold them in static fields; registered in the
 * sound event only when the stream is on (a sound nobody plays costs nothing, but an unregistered event
 * would be played as silence, so the classes never play them with the stream off).
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class TridentSounds {

    public static final SoundEvent TRIDENT_HIT = event("item.trident.hit");
    public static final SoundEvent TRIDENT_HIT_GROUND = event("item.trident.hit_ground");
    public static final SoundEvent TRIDENT_RETURN = event("item.trident.return");
    public static final SoundEvent TRIDENT_RIPTIDE_1 = event("item.trident.riptide_1");
    public static final SoundEvent TRIDENT_RIPTIDE_2 = event("item.trident.riptide_2");
    public static final SoundEvent TRIDENT_RIPTIDE_3 = event("item.trident.riptide_3");
    public static final SoundEvent TRIDENT_THROW = event("item.trident.throw");
    public static final SoundEvent TRIDENT_THUNDER = event("item.trident.thunder");

    public static final SoundEvent DROWNED_AMBIENT = event("entity.drowned.ambient");
    public static final SoundEvent DROWNED_AMBIENT_WATER = event("entity.drowned.ambient_water");
    public static final SoundEvent DROWNED_DEATH = event("entity.drowned.death");
    public static final SoundEvent DROWNED_DEATH_WATER = event("entity.drowned.death_water");
    public static final SoundEvent DROWNED_HURT = event("entity.drowned.hurt");
    public static final SoundEvent DROWNED_HURT_WATER = event("entity.drowned.hurt_water");
    public static final SoundEvent DROWNED_SHOOT = event("entity.drowned.shoot");
    public static final SoundEvent DROWNED_STEP = event("entity.drowned.step");
    public static final SoundEvent DROWNED_SWIM = event("entity.drowned.swim");

    public static final SoundEvent ZOMBIE_CONVERTED_TO_DROWNED = event("entity.zombie.converted_to_drowned");
    public static final SoundEvent HUSK_CONVERTED_TO_ZOMBIE = event("entity.husk.converted_to_zombie");

    private static final SoundEvent[] ALL = {
            TRIDENT_HIT, TRIDENT_HIT_GROUND, TRIDENT_RETURN, TRIDENT_RIPTIDE_1, TRIDENT_RIPTIDE_2, TRIDENT_RIPTIDE_3,
            TRIDENT_THROW, TRIDENT_THUNDER,
            DROWNED_AMBIENT, DROWNED_AMBIENT_WATER, DROWNED_DEATH, DROWNED_DEATH_WATER, DROWNED_HURT, DROWNED_HURT_WATER,
            DROWNED_SHOOT, DROWNED_STEP, DROWNED_SWIM,
            ZOMBIE_CONVERTED_TO_DROWNED, HUSK_CONVERTED_TO_ZOMBIE,
    };

    private TridentSounds() {
    }

    private static SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        if (TridentContent.enabled()) {
            event.getRegistry().registerAll(ALL);
        }
    }

    /** @return every sound of this stream, for the self-test */
    public static SoundEvent[] all() {
        return ALL.clone();
    }
}
