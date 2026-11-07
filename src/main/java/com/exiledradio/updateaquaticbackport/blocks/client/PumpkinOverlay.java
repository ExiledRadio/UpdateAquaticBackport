package com.exiledradio.updateaquaticbackport.blocks.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;

/** Vanilla's pumpkin blur ({@code GuiIngame.renderPumpkinOverlay}), for a worn carved pumpkin. */
public final class PumpkinOverlay {

    private static final ResourceLocation BLUR = new ResourceLocation("textures/misc/pumpkinblur.png");

    private PumpkinOverlay() {
    }

    public static void draw(ScaledResolution res) {
        GlStateManager.disableDepth();
        GlStateManager.depthMask(false);
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableAlpha();
        Minecraft.getMinecraft().getTextureManager().bindTexture(BLUR);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(7, DefaultVertexFormats.POSITION_TEX);
        buffer.pos(0.0D, res.getScaledHeight(), -90.0D).tex(0.0D, 1.0D).endVertex();
        buffer.pos(res.getScaledWidth(), res.getScaledHeight(), -90.0D).tex(1.0D, 1.0D).endVertex();
        buffer.pos(res.getScaledWidth(), 0.0D, -90.0D).tex(1.0D, 0.0D).endVertex();
        buffer.pos(0.0D, 0.0D, -90.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();
        GlStateManager.depthMask(true);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
