package com.exiledradio.updateaquaticbackport.phantom;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Slow Falling: while falling, gravity is 0.01 instead of 0.08 and no fall damage builds up.
 *
 * <h2>Without patching travel</h2>
 * 1.14 swaps the gravity constant inside {@code travel}. Here the difference is made up around vanilla's own
 * travel, from {@link LivingEvent.LivingUpdateEvent}, which runs at the start of every living tick on the side
 * that moves the entity (the server for mobs, the client for its own player):
 * <ul>
 *   <li>In the air vanilla moves first and pulls down after, so the pull is corrected at the start of the next
 *       tick: it was a fall (1.14's "motion at or below zero") exactly when the motion left is at or below
 *       vanilla's {@code -0.08 * 0.98}, and {@code 0.07 * 0.98} is given back.</li>
 *   <li>Gliding with an elytra pulls down before moving, so the difference ({@code 0.07 - 0.0525 * lift}) is
 *       added before vanilla's travel runs, and the rest of the glide follows vanilla exactly.</li>
 * </ul>
 * Water, lava, creative flight, levitation and no-gravity entities are left alone, as in 1.12.2 they never used
 * the constant 1.14 changes. Fall distance is cleared every tick the effect is on.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public class PotionSlowFalling extends Potion {

    /** 1.14.4's color. */
    public static final int COLOR = 0xFFEFD1;

    /** Vanilla 1.12.2's pull in the air and its drag, as doubles exactly as travel uses them. */
    static final double GRAVITY = 0.08D;
    static final double DRAG = 0.9800000190734863D;
    /** 1.14's pull with Slow Falling. */
    static final double SLOW_GRAVITY = 0.01D;

    public PotionSlowFalling() {
        super(false, COLOR);
        this.setPotionName("effect." + UpdateAquaticBackport.MOD_ID + ".slow_falling");
        this.setBeneficial();
    }

    // No icon index on vanilla's sheet (so hasStatusIcon is false); the icon is drawn from its own texture.

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z) {
        com.exiledradio.updateaquaticbackport.phantom.client.PhantomClient.drawEffectIcon(x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z, float alpha) {
        com.exiledradio.updateaquaticbackport.phantom.client.PhantomClient.drawEffectIcon(x + 3, y + 3, alpha);
    }

    static boolean has(EntityLivingBase entity) {
        Potion potion = PhantomContent.slowFalling();
        return potion != null && entity.isPotionActive(potion);
    }

    /** Whether vanilla's travel would take its ordinary air or elytra branch this tick. */
    private static boolean usesAirGravity(EntityLivingBase entity) {
        if (entity.hasNoGravity() || entity.isInWater() || entity.isInLava() || entity.isPotionActive(MobEffects.LEVITATION)) {
            return false;
        }
        return !(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying);
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!has(entity)) {
            return;
        }
        entity.fallDistance = 0.0F;
        if (!(entity.isServerWorld() || entity.canPassengerSteer()) || !usesAirGravity(entity)) {
            return;
        }
        if (entity.isElytraFlying()) {
            if (entity.motionY <= 0.0D) {
                entity.motionY += (GRAVITY - SLOW_GRAVITY) - 0.0525D * elytraLift(entity);
            }
        } else if (entity.motionY <= -GRAVITY * DRAG + 1.0E-7D) {
            entity.motionY += (GRAVITY - SLOW_GRAVITY) * DRAG;
        }
    }

    /** 1.12.2's elytra lift factor: cos^2 of the pitch, less when looking nearly straight up or down. */
    static double elytraLift(EntityLivingBase entity) {
        Vec3d look = entity.getLookVec();
        float cos = MathHelper.cos(entity.rotationPitch * 0.017453292F);
        return (float) ((double) cos * (double) cos * Math.min(1.0D, look.length() / 0.4D));
    }

    /** A landing with the effect on does no damage (the distance is cleared every tick; this covers the last one). */
    @SubscribeEvent
    public static void onFall(LivingFallEvent event) {
        if (has(event.getEntityLiving())) {
            event.setDistance(0.0F);
        }
    }
}
