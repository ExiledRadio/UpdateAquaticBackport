package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.trident.EntityTrident;

import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * 1.14.4's {@code TridentRenderer}: the trident model along its flight, and while a Loyalty trident flies
 * back, the shimmering line to its thrower. Enchanted tridents glint (as current Java).
 */
final class RenderTrident extends Render<EntityTrident> {

    private final ModelTrident model = new ModelTrident();

    RenderTrident(RenderManager manager) {
        super(manager);
    }

    @Override
    public void doRender(final EntityTrident entity, double x, double y, double z, float entityYaw, float partialTicks) {
        this.bindEntityTexture(entity);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        GlStateManager.translate((float) x, (float) y, (float) z);
        GlStateManager.rotate(entity.prevRotationYaw + (entity.rotationYaw - entity.prevRotationYaw) * partialTicks - 90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(entity.prevRotationPitch + (entity.rotationPitch - entity.prevRotationPitch) * partialTicks + 90.0F, 0.0F, 0.0F, 1.0F);
        this.model.renderTrident();
        if (entity.isFoil()) {
            TridentGlint.render(this.model::renderTrident, TridentItemRenderer.TEXTURE);
        }
        GlStateManager.popMatrix();
        this.renderReturnLine(entity, x, y, z, partialTicks);
        super.doRender(entity, x, y, z, entityYaw, partialTicks);
        GlStateManager.enableLighting();
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityTrident entity) {
        return TridentItemRenderer.TEXTURE;
    }

    private static double lerp(double t, double a, double b) {
        return a + t * (b - a);
    }

    /** 1.14.4's {@code func_203085_b}: two twisting strips from the trident to its thrower. */
    private void renderReturnLine(EntityTrident trident, double x, double y, double z, float partialTicks) {
        Entity owner = trident.getOwner();
        if (owner == null || !trident.isReturning()) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        double yaw = lerp(partialTicks * 0.5F, owner.rotationYaw, owner.prevRotationYaw) * (Math.PI / 180D);
        double cos = Math.cos(yaw);
        double sin = Math.sin(yaw);
        double ox = lerp(partialTicks, owner.prevPosX, owner.posX);
        double oy = lerp(partialTicks, owner.prevPosY + owner.getEyeHeight() * 0.8D, owner.posY + owner.getEyeHeight() * 0.8D);
        double oz = lerp(partialTicks, owner.prevPosZ, owner.posZ);
        double a = cos - sin;
        double b = sin + cos;
        double tx = lerp(partialTicks, trident.prevPosX, trident.posX);
        double ty = lerp(partialTicks, trident.prevPosY, trident.posY);
        double tz = lerp(partialTicks, trident.prevPosZ, trident.posZ);
        double dx = (float) (ox - tx);
        double dy = (float) (oy - ty);
        double dz = (float) (oz - tz);
        double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
        int seed = trident.getEntityId() + trident.ticksExisted;
        double phase = (seed + partialTicks) * -0.1D;
        double width = Math.min(0.5D, length / 30.0D);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 255.0F, 255.0F);
        int offset = 7 - seed % 7;
        for (int strip = 0; strip < 2; strip++) {
            buffer.begin(5, DefaultVertexFormats.POSITION_COLOR);
            for (int i = 0; i <= 37; ++i) {
                double t = i / 37.0D;
                float shade = 1.0F - (float) ((i + offset) % 7) / 7.0F;
                double bulge = t * 2.0D - 1.0D;
                bulge = (1.0D - bulge * bulge) * width;
                double px = x + dx * t + Math.sin(t * Math.PI * 8.0D + phase) * a * bulge;
                double py = y + dy * t + Math.cos(t * Math.PI * 8.0D + phase) * (strip == 0 ? 0.02D : 0.01D) + (0.1D + bulge);
                double pz = z + dz * t + Math.sin(t * Math.PI * 8.0D + phase) * b * bulge;
                float r = 0.87F * shade + 0.3F * (1.0F - shade);
                float g = 0.91F * shade + 0.6F * (1.0F - shade);
                float bl = 0.85F * shade + 0.5F * (1.0F - shade);
                buffer.pos(px, py, pz).color(r, g, bl, 1.0F).endVertex();
                if (strip == 0) {
                    buffer.pos(px + 0.1D * bulge, py + 0.1D * bulge, pz).color(r, g, bl, 1.0F).endVertex();
                } else {
                    buffer.pos(px + 0.1D * bulge, py, pz + 0.1D * bulge).color(r, g, bl, 1.0F).endVertex();
                }
                if (i > trident.returningTicks * 2) {
                    break;
                }
            }
            tessellator.draw();
        }
        GlStateManager.enableLighting();
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
    }
}
