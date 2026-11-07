package com.exiledradio.updateaquaticbackport.trident.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/** The enchantment glint over a trident drawn from its model, the way vanilla draws it over item models. */
final class TridentGlint {

    private static final ResourceLocation GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");

    private TridentGlint() {
    }

    /** Draws the glint over what {@code draw} draws; call after drawing it once normally. */
    static void render(Runnable draw, ResourceLocation restore) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(514);
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_COLOR, GlStateManager.DestFactor.ONE);
        mc.getTextureManager().bindTexture(GLINT);
        GlStateManager.color(0.5F, 0.25F, 0.8F, 1.0F);
        for (int pass = 0; pass < 2; pass++) {
            GlStateManager.matrixMode(5890);
            GlStateManager.pushMatrix();
            GlStateManager.scale(8.0F, 8.0F, 8.0F);
            float shift = pass == 0
                    ? (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F
                    : -(float) (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
            GlStateManager.translate(shift, 0.0F, 0.0F);
            GlStateManager.rotate(pass == 0 ? -50.0F : 10.0F, 0.0F, 0.0F, 1.0F);
            GlStateManager.matrixMode(5888);
            draw.run();
            GlStateManager.matrixMode(5890);
            GlStateManager.popMatrix();
            GlStateManager.matrixMode(5888);
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableBlend();
        GlStateManager.enableLighting();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        mc.getTextureManager().bindTexture(restore);
    }
}
