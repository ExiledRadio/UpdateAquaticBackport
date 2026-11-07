package com.exiledradio.updateaquaticbackport.blocks.client;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.item.ItemStack;

/**
 * Vanilla's {@code LayerSnowmanHead} with 1.13's head: a carved pumpkin. Vanilla's layer draws a plain
 * pumpkin, which has no face any more.
 */
final class LayerCarvedSnowmanHead implements LayerRenderer<EntitySnowman> {

    private final RenderSnowMan renderer;
    private final ItemStack head;

    LayerCarvedSnowmanHead(RenderSnowMan renderer, Block carved) {
        this.renderer = renderer;
        this.head = new ItemStack(carved);
    }

    @Override
    public void doRenderLayer(EntitySnowman snowman, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                              float netHeadYaw, float headPitch, float scale) {
        if (!snowman.isInvisible() && snowman.isPumpkinEquipped()) {
            GlStateManager.pushMatrix();
            this.renderer.getMainModel().head.postRender(0.0625F);
            GlStateManager.translate(0.0F, -0.34375F, 0.0F);
            GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.scale(0.625F, -0.625F, -0.625F);
            Minecraft.getMinecraft().getItemRenderer().renderItem(snowman, this.head, ItemCameraTransforms.TransformType.HEAD);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return true;
    }
}
