package com.exiledradio.updateaquaticbackport.turtle;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.13's turtle sounds, under this mod's names ({@code updateaquaticbackport:entity.turtle.hurt} and so on).
 *
 * <p>Created eagerly: the turtle shell's armor material takes its equip sound in the item event, which in
 * 1.12 comes before the sound event. Registered only when the stream is on.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class TurtleSounds {

    public static final SoundEvent AMBIENT_LAND = event("entity.turtle.ambient_land");
    public static final SoundEvent DEATH = event("entity.turtle.death");
    public static final SoundEvent DEATH_BABY = event("entity.turtle.death_baby");
    public static final SoundEvent EGG_BREAK = event("entity.turtle.egg_break");
    public static final SoundEvent EGG_CRACK = event("entity.turtle.egg_crack");
    public static final SoundEvent EGG_HATCH = event("entity.turtle.egg_hatch");
    public static final SoundEvent HURT = event("entity.turtle.hurt");
    public static final SoundEvent HURT_BABY = event("entity.turtle.hurt_baby");
    public static final SoundEvent LAY_EGG = event("entity.turtle.lay_egg");
    public static final SoundEvent SHAMBLE = event("entity.turtle.shamble");
    public static final SoundEvent SHAMBLE_BABY = event("entity.turtle.shamble_baby");
    public static final SoundEvent SWIM = event("entity.turtle.swim");
    public static final SoundEvent ZOMBIE_DESTROY_EGG = event("entity.zombie.destroy_egg");
    public static final SoundEvent EQUIP_TURTLE = event("item.armor.equip_turtle");

    private TurtleSounds() {
    }

    private static SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent
    public static void register(RegistryEvent.Register<SoundEvent> event) {
        if (!TurtleContent.enabled()) {
            return;
        }
        event.getRegistry().registerAll(AMBIENT_LAND, DEATH, DEATH_BABY, EGG_BREAK, EGG_CRACK, EGG_HATCH, HURT, HURT_BABY,
                LAY_EGG, SHAMBLE, SHAMBLE_BABY, SWIM, ZOMBIE_DESTROY_EGG, EQUIP_TURTLE);
    }
}
