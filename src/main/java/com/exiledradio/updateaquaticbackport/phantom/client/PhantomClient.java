package com.exiledradio.updateaquaticbackport.phantom.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.phantom.ElytraRepair;
import com.exiledradio.updateaquaticbackport.phantom.EntityPhantom;
import com.exiledradio.updateaquaticbackport.phantom.PhantomContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.IRenderFactory;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The phantom stream's client side: the phantom's renderer and Slow Falling's icon. A dedicated server never
 * loads this class (the subscriber is client-only, and the potion reaches {@link #drawEffectIcon} only from
 * methods stripped on a server).
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class PhantomClient {

    private static final ResourceLocation SLOW_FALLING_ICON = Content.id("textures/mob_effect/slow_falling.png");

    private PhantomClient() {
    }

    /** Before the render manager exists, so the factory is picked up when it is made. */
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (!PhantomContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityPhantom.class, new IRenderFactory<EntityPhantom>() {
            @Override
            public Render<? super EntityPhantom> createRenderFor(RenderManager manager) {
                return new RenderPhantom(manager);
            }
        });
    }

    /** The client's half of {@code ElytraRepair.refuse}: the anvil this player has open. */
    public static void clearAnvilOutput(ItemStack left, ItemStack right) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        if (player != null) {
            ElytraRepair.clearOutput(player.openContainer, left, right);
        }
    }

    /** Draws Slow Falling's 18x18 icon (1.18's texture) where vanilla would draw one from its sheet. */
    public static void drawEffectIcon(int x, int y, float alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(SLOW_FALLING_ICON);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        GlStateManager.enableBlend();
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, 0.0F, 18, 18, 18.0F, 18.0F);
    }
}
