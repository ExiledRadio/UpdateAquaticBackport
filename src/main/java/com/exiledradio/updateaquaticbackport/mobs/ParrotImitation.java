package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Parrots imitate phantoms and drowned (1.13), through Forge's own list of mobs parrots imitate
 * ({@link EntityParrot#registerMimicSound}): a parrot then imitates them as it does the others - now and
 * then when one is within 20 blocks, and once in a while at random.
 *
 * <p>Phantoms and drowned are other streams' mobs, found by name ({@code updateaquaticbackport:phantom},
 * {@code updateaquaticbackport:drowned}); a mob whose stream is off or missing is simply not added. The
 * sounds are 1.13's own events: {@code entity.parrot.imitate.phantom} is the phantom's ambient sound at
 * pitch 1.7 and volume 0.6, {@code entity.parrot.imitate.drowned} the drowned's at pitch 1.8 and volume
 * 0.6 (sounds.json points at those streams' events by name, as vanilla's does), with the parrot's own
 * subtitles.
 */
public final class ParrotImitation {

    public static final String PHANTOM = "phantom";
    public static final String DROWNED = "drowned";

    private ParrotImitation() {
    }

    /** From post-init. @return how many mobs were added */
    static int register() {
        if (!ModConfig.mobs.parrotsImitate) {
            return 0;
        }
        int added = 0;
        added += add(PHANTOM, MobsContent.PARROT_IMITATE_PHANTOM);
        added += add(DROWNED, MobsContent.PARROT_IMITATE_DROWNED);
        UpdateAquaticBackport.LOGGER.debug("Parrots imitate {} more mobs", added);
        return added;
    }

    private static int add(String name, SoundEvent sound) {
        Class<? extends Entity> mob = ourMob(name);
        if (mob == null) {
            return 0;
        }
        EntityParrot.registerMimicSound(mob, sound);
        return 1;
    }

    /** @return the class of this mod's entity {@code name}, or null if its stream is off or absent */
    @Nullable
    public static Class<? extends Entity> ourMob(String name) {
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(Content.id(name));
        return entry == null || !Content.id(name).equals(entry.getRegistryName()) ? null : entry.getEntityClass();
    }
}
