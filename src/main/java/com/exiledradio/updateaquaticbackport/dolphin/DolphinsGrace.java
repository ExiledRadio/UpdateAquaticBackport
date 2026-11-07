package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;

/**
 * What Dolphin's Grace does: in water, horizontal speed is kept at 0.96 a tick instead of 0.8.
 *
 * <h2>How, without a patch</h2>
 * 1.14.4's {@code LivingEntity.travel} replaces the water slowdown with 0.96 while the effect is on.
 * 1.12.2's {@code travel} multiplies horizontal motion by its slowdown (0.8, raised by Depth Strider)
 * after moving. {@link LivingEvent.LivingUpdateEvent} fires at the start of the next tick, before the
 * next move, and the entity's in-water flag is still the one that travel used; scaling horizontal
 * motion there by {@code 0.96 / slowdown} gives exactly 1.14's result. Vertical motion keeps vanilla's
 * 0.8 in both versions.
 *
 * <p>Movement runs where the entity is simulated: a player's on its own client, everything else on the
 * server, and only while vanilla's own check lets travel run. The effect is synced to its holder's
 * client by vanilla, so the same handler serves both.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class DolphinsGrace {

    /** 1.14.4's slowdown in water with Dolphin's Grace. */
    public static final float SLOWDOWN = 0.96F;

    private static Method waterSlowDown;
    private static boolean lookedUp;

    private DolphinsGrace() {
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (DolphinContent.dolphinsGrace() == null || !entity.isInWater()) {
            return;
        }
        // Exactly when travel moves the entity (a NoAI mob never travels), but a player only on its own
        // client: the server's copy of a player is moved by the client's packets.
        boolean simulatedHere = (entity.isServerWorld() || entity.canPassengerSteer())
                && !(entity instanceof EntityPlayer && !entity.world.isRemote);
        if (!simulatedHere || !entity.isPotionActive(DolphinContent.dolphinsGrace())) {
            return;
        }
        if (entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.isFlying) {
            return;
        }
        float vanilla = vanillaSlowdown(entity);
        if (vanilla <= 0.0F || vanilla >= SLOWDOWN) {
            return;
        }
        double scale = SLOWDOWN / vanilla;
        entity.motionX *= scale;
        entity.motionZ *= scale;
    }

    /** @return the horizontal slowdown 1.12.2's travel applied in water: the entity's own, then Depth Strider */
    public static float vanillaSlowdown(EntityLivingBase entity) {
        float f1 = waterSlowDown(entity);
        float f3 = (float) EnchantmentHelper.getDepthStriderModifier(entity);
        if (f3 > 3.0F) {
            f3 = 3.0F;
        }
        if (!entity.onGround) {
            f3 *= 0.5F;
        }
        if (f3 > 0.0F) {
            f1 += (0.54600006F - f1) * f3 / 3.0F;
        }
        return f1;
    }

    private static float waterSlowDown(EntityLivingBase entity) {
        if (!lookedUp) {
            lookedUp = true;
            try {
                waterSlowDown = ObfuscationReflectionHelper.findMethod(EntityLivingBase.class, "func_189749_co", float.class);
            } catch (RuntimeException e) {
                UpdateAquaticBackport.LOGGER.warn("Dolphin's Grace: no water slowdown getter; assuming vanilla's 0.8.", e);
            }
        }
        if (waterSlowDown != null) {
            try {
                return (Float) waterSlowDown.invoke(entity);
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // fall through
            }
        }
        return 0.8F;
    }
}
