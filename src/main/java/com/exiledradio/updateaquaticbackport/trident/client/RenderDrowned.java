package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.trident.EntityDrowned;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderBiped;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerBipedArmor;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.ResourceLocation;

/** 1.14.4's {@code DrownedRenderer}: drowned body, its translucent outer layer, armor, and the swimming tilt. */
final class RenderDrowned extends RenderBiped<EntityDrowned> {

    private static final ResourceLocation TEXTURE = new ResourceLocation("updateaquaticbackport", "textures/entity/zombie/drowned.png");
    private static final ResourceLocation OUTER = new ResourceLocation("updateaquaticbackport", "textures/entity/zombie/drowned_outer_layer.png");

    RenderDrowned(RenderManager manager) {
        super(manager, new ModelDrowned(0.0F, 0.0F), 0.5F);
        this.addLayer(new LayerBipedArmor(this) {
            @Override
            protected void initArmor() {
                this.modelLeggings = new ModelDrowned(0.5F, true);
                this.modelArmor = new ModelDrowned(1.0F, true);
            }
        });
        this.addLayer(new Outer());
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityDrowned entity) {
        return TEXTURE;
    }

    @Override
    protected void applyRotations(EntityDrowned entity, float ageInTicks, float rotationYaw, float partialTicks) {
        float swim = entity.getSwimAnimation(partialTicks);
        super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
        if (swim > 0.0F) {
            GlStateManager.rotate(entity.rotationPitch + swim * (-10.0F - entity.rotationPitch - entity.rotationPitch), 1.0F, 0.0F, 0.0F);
        }
    }

    /** 1.14.4's {@code DrownedOuterLayer}: the same model, 0.25 larger, with the seaweed texture. */
    private final class Outer implements LayerRenderer<EntityDrowned> {
        private final ModelDrowned model = new ModelDrowned(0.25F, 0.0F);

        @Override
        public void doRenderLayer(EntityDrowned entity, float limbSwing, float limbSwingAmount, float partialTicks,
                float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (entity.isInvisible()) {
                return;
            }
            this.model.setModelAttributes(RenderDrowned.this.getMainModel());
            this.model.setLivingAnimations((EntityLivingBase) entity, limbSwing, limbSwingAmount, partialTicks);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            RenderDrowned.this.bindTexture(OUTER);
            this.model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
