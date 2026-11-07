package com.exiledradio.updateaquaticbackport.dolphin.client;

import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.dolphin.EntityDolphin;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/** 1.14.4's dolphin renderer: its model, 0.7 shadow, and the item it carries in its mouth. */
final class RenderDolphin extends RenderLiving<EntityDolphin> {

    private static final ResourceLocation TEXTURE = Content.id("textures/entity/dolphin.png");

    RenderDolphin(RenderManager manager) {
        super(manager, new ModelDolphin(), 0.7F);
        this.addLayer(new CarriedItem());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDolphin entity) {
        return TEXTURE;
    }

    /** 1.14.4's {@code DolphinCarriedItemLayer}: the held item, at the snout, shifted by the dolphin's pitch. */
    private static final class CarriedItem implements LayerRenderer<EntityDolphin> {

        @Override
        public void doRenderLayer(EntityDolphin dolphin, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            boolean right = dolphin.getPrimaryHand() == EnumHandSide.RIGHT;
            ItemStack off = right ? dolphin.getHeldItemOffhand() : dolphin.getHeldItemMainhand();
            ItemStack main = right ? dolphin.getHeldItemMainhand() : dolphin.getHeldItemOffhand();
            if (off.isEmpty() && main.isEmpty()) {
                return;
            }
            this.render(dolphin, main);
        }

        private void render(EntityDolphin dolphin, ItemStack stack) {
            if (stack.isEmpty()) {
                return;
            }
            RenderItem items = Minecraft.getMinecraft().getRenderItem();
            Block block = Block.getBlockFromItem(stack.getItem());
            GlStateManager.pushMatrix();
            boolean translucent = items.shouldRenderItemIn3D(stack) && block.getRenderLayer() == BlockRenderLayer.TRANSLUCENT;
            if (translucent) {
                GlStateManager.depthMask(false);
            }
            float tilt = MathHelper.abs(dolphin.rotationPitch) / 60.0F;
            if (dolphin.rotationPitch < 0.0F) {
                GlStateManager.translate(0.0F, 1.0F - tilt * 0.5F, -1.0F + tilt * 0.5F);
            } else {
                GlStateManager.translate(0.0F, 1.0F + tilt * 0.8F, -1.0F + tilt * 0.2F);
            }
            items.renderItem(stack, dolphin, ItemCameraTransforms.TransformType.GROUND, false);
            if (translucent) {
                GlStateManager.depthMask(true);
            }
            GlStateManager.popMatrix();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
