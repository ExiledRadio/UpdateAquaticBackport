package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.event.entity.EntityStruckByLightningEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Which lightning bolts a Channeling trident called down, and by whom: 1.14's bolt "caster", which 1.12.2's
 * lightning has no field for.
 *
 * <p>Used for <i>Very Very Frightening</i>: a villager struck by a player's channeled bolt grants it.
 * 1.12.2 has no {@code channeled_lightning} trigger, so the advancement's one criterion is
 * {@code minecraft:impossible} and is granted here.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class Channeling {

    public static final String VERY_VERY_FRIGHTENING = "adventure/very_very_frightening";
    public static final String CRITERION = "struck_villager";

    private static final Map<EntityLightningBolt, EntityPlayerMP> CASTERS =
            Collections.synchronizedMap(new WeakHashMap<EntityLightningBolt, EntityPlayerMP>());

    private Channeling() {
    }

    static void remember(EntityLightningBolt bolt, @Nullable EntityPlayerMP caster) {
        if (caster != null) {
            CASTERS.put(bolt, caster);
        }
    }

    /** @return the player whose Channeling trident called this bolt, or null */
    @Nullable
    public static EntityPlayerMP caster(Entity bolt) {
        return bolt instanceof EntityLightningBolt ? CASTERS.get(bolt) : null;
    }

    @SubscribeEvent
    public static void onStruck(EntityStruckByLightningEvent event) {
        EntityPlayerMP caster = caster(event.getLightning());
        if (caster != null && event.getEntity() instanceof EntityVillager) {
            grant(caster, VERY_VERY_FRIGHTENING, CRITERION);
        }
    }

    /** Grants one criterion of one of this mod's advancements, if the advancement is loaded. */
    public static void grant(EntityPlayerMP player, String advancement, String criterion) {
        if (player.getServer() == null) {
            return;
        }
        Advancement adv = player.getServer().getAdvancementManager().getAdvancement(Content.id(advancement));
        if (adv != null) {
            player.getAdvancements().grantCriterion(adv, criterion);
        }
    }
}
