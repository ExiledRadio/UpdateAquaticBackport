package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.trident.Riptide;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/** 1.14.4's {@code SpinAttackEffectLayer}: three turning rings of water around a player in a Riptide spin. */
final class LayerRiptide implements LayerRenderer<EntityLivingBase> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("updateaquaticbackport", "textures/entity/trident_riptide.png");

    private final ModelBase model = new ModelBase() {
    };
    private final ModelRenderer box;

    LayerRiptide() {
        this.model.textureWidth = 64;
        this.model.textureHeight = 64;
        this.box = new ModelRenderer(this.model, 0, 0);
        this.box.addBox(-8.0F, -16.0F, -8.0F, 16, 32, 16);
    }

    @Override
    public void doRenderLayer(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks,
            float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        if (!Riptide.isSpinning(entity)) {
            return;
        }
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TEXTURE);
        for (int i = 0; i < 3; ++i) {
            GlStateManager.pushMatrix();
            GlStateManager.rotate(ageInTicks * -(45 + i * 5), 0.0F, 1.0F, 0.0F);
            float size = 0.75F * i;
            GlStateManager.scale(size, size, size);
            GlStateManager.translate(0.0F, -0.2F + 0.6F * i, 0.0F);
            this.box.render(scale);
            GlStateManager.popMatrix();
        }
    }

    @Override
    public boolean shouldCombineTextures() {
        return false;
    }
}
