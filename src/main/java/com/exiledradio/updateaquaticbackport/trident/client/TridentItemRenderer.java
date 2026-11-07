package com.exiledradio.updateaquaticbackport.trident.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/** Draws the trident held in a hand or worn on the head from its 3D model, as 1.14.4's item renderer does. */
final class TridentItemRenderer extends TileEntityItemStackRenderer {

    static final ResourceLocation TEXTURE = new ResourceLocation("updateaquaticbackport", "textures/entity/trident.png");

    private final ModelTrident model = new ModelTrident();

    @Override
    public void renderByItem(ItemStack stack) {
        this.renderByItem(stack, 1.0F);
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0F, -1.0F, -1.0F);
        this.model.renderTrident();
        if (stack.hasEffect()) {
            TridentGlint.render(this.model::renderTrident, TEXTURE);
        }
        GlStateManager.popMatrix();
    }
}
