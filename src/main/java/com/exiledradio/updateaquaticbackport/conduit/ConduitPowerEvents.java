package com.exiledradio.updateaquaticbackport.conduit;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * What Conduit Power does, on both sides (the client predicts breath and digging for its own player):
 *
 * <ul>
 *   <li><b>Breath</b>: as Water Breathing, the air supply does not drop underwater - and, as today's Java
 *       (1.21.4), it refills there, 4 a tick as on land since 1.13. 1.12.2 lowers air after the living
 *       update event, so the event adds back the one it is about to take.</li>
 *   <li><b>Mining speed</b>: as Haste of the same level, 20% per level, the stronger of the two used and
 *       never both (1.14.4's {@code EffectUtils.getMiningSpeedup}).</li>
 * </ul>
 * Seeing underwater is the client's ({@code ConduitClient}). 1.14's faster arm swing with Haste has no
 * hook in 1.12.2 and is left out.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class ConduitPowerEvents {

    /** 1.14.4's {@code LivingEntity.determineNextAir}: air refills 4 a tick. */
    static final int REFILL = 4;

    private ConduitPowerEvents() {
    }

    @SubscribeEvent
    public static void breathe(LivingEvent.LivingUpdateEvent event) {
        if (!ConduitContent.enabled()) {
            return;
        }
        EntityLivingBase entity = event.getEntityLiving();
        if (!entity.isPotionActive(ConduitContent.CONDUIT_POWER) || !entity.isEntityAlive()
                || !entity.isInsideOfMaterial(Material.WATER)) {
            return;
        }
        int air = entity.getAir();
        int max = 300;
        int next = Math.min(max, air + REFILL);
        // The same test 1.12.2's EntityLivingBase.onEntityUpdate makes before taking one away.
        boolean vanillaTakesOne = !entity.canBreatheUnderwater() && !entity.isPotionActive(MobEffects.WATER_BREATHING)
                && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage);
        if (vanillaTakesOne && next < max) {
            next++;
        }
        if (next != air) {
            entity.setAir(next);
        }
    }

    @SubscribeEvent
    public static void dig(PlayerEvent.BreakSpeed event) {
        if (!ConduitContent.enabled()) {
            return;
        }
        PotionEffect conduit = event.getEntityPlayer().getActivePotionEffect(ConduitContent.CONDUIT_POWER);
        if (conduit == null) {
            return;
        }
        PotionEffect haste = event.getEntityPlayer().getActivePotionEffect(MobEffects.HASTE);
        int hasteLevel = haste == null ? -1 : haste.getAmplifier();
        if (conduit.getAmplifier() > hasteLevel) {
            // Vanilla has already applied Haste (if any); swap it for the stronger level.
            float without = event.getNewSpeed() / (1.0F + (hasteLevel + 1) * 0.2F);
            event.setNewSpeed(without * (1.0F + (conduit.getAmplifier() + 1) * 0.2F));
        }
    }
}
