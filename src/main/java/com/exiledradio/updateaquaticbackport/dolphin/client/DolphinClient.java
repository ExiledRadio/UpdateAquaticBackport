package com.exiledradio.updateaquaticbackport.dolphin.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.dolphin.DolphinContent;
import com.exiledradio.updateaquaticbackport.dolphin.EntityDolphin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The dolphin stream's client side: the renderer, the swimming trail and the Dolphin's Grace icon.
 *
 * <p>Subscribed for the client only, so a dedicated server never loads it; the two static methods are
 * reached from common code that runs only on the client, through calls that name no client type.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class DolphinClient {

    private static final ResourceLocation GRACE_ICON = Content.id("textures/mob_effect/dolphins_grace.png");

    private DolphinClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(ModelRegistryEvent event) {
        if (DolphinContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityDolphin.class, RenderDolphin::new);
        }
    }

    /** 1.14.4's two lines of dolphin particles behind a swimming dolphin. */
    public static void trail(EntityDolphin dolphin) {
        Vec3d look = dolphin.getLook(0.0F);
        float cos = MathHelper.cos(dolphin.rotationYaw * 0.017453292F) * 0.3F;
        float sin = MathHelper.sin(dolphin.rotationYaw * 0.017453292F) * 0.3F;
        float back = 1.2F - dolphin.getRNG().nextFloat() * 0.7F;
        Minecraft mc = Minecraft.getMinecraft();
        for (int i = 0; i < 2; ++i) {
            mc.effectRenderer.addEffect(new ParticleDolphin(dolphin.world,
                    dolphin.posX - look.x * back + cos, dolphin.posY - look.y, dolphin.posZ - look.z * back + sin));
            mc.effectRenderer.addEffect(new ParticleDolphin(dolphin.world,
                    dolphin.posX - look.x * back - cos, dolphin.posY - look.y, dolphin.posZ - look.z * back - sin));
        }
    }

    /** Draws 1.13's Dolphin's Grace icon, 18x18, at a screen position. */
    public static void drawEffectIcon(int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(GRACE_ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
