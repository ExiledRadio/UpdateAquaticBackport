package com.exiledradio.updateaquaticbackport.phantom.client;

import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.phantom.EntityPhantom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.util.ResourceLocation;

/** 1.14.4's {@code PhantomRenderer}: scaled by size, pitched with its flight, glowing eyes. */
public class RenderPhantom extends RenderLiving<EntityPhantom> {

    private static final ResourceLocation TEXTURE = Content.id("textures/entity/phantom.png");
    private static final ResourceLocation EYES = Content.id("textures/entity/phantom_eyes.png");

    public RenderPhantom(RenderManager manager) {
        super(manager, new ModelPhantom(), 0.75F);
        this.addLayer(new Eyes(this));
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityPhantom entity) {
        return TEXTURE;
    }

    @Override
    protected void preRenderCallback(EntityPhantom entity, float partialTicks) {
        float scale = 1.0F + 0.15F * entity.getPhantomSize();
        GlStateManager.scale(scale, scale, scale);
        GlStateManager.translate(0.0F, 1.3125F, 0.1875F);
    }

    @Override
    protected void applyRotations(EntityPhantom entity, float ageInTicks, float rotationYaw, float partialTicks) {
        super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
        float pitch = entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks;
        GlStateManager.rotate(pitch, 1.0F, 0.0F, 0.0F);
    }

    /** 1.14.4's {@code PhantomEyesLayer}, drawn the way 1.12.2 draws a spider's eyes: full bright, additive. */
    static final class Eyes implements LayerRenderer<EntityPhantom> {
        private final RenderPhantom renderer;

        Eyes(RenderPhantom renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityPhantom entity, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks,
                float netHeadYaw, float headPitch, float scale) {
            this.renderer.bindTexture(EYES);
            GlStateManager.enableBlend();
            GlStateManager.disableAlpha();
            GlStateManager.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
            GlStateManager.depthMask(!entity.isInvisible());
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 61680 % 65536, 61680 / 65536);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(true);
            this.renderer.getMainModel().render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
            Minecraft.getMinecraft().entityRenderer.setupFogColor(false);
            int light = entity.getBrightnessForRender();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, light % 65536, light / 65536);
            this.renderer.setLightmap(entity);
            GlStateManager.depthMask(true);
            GlStateManager.disableBlend();
            GlStateManager.enableAlpha();
        }

        @Override
        public boolean shouldCombineTextures() {
            return false;
        }
    }
}
