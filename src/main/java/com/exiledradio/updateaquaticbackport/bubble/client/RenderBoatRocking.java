package com.exiledradio.updateaquaticbackport.bubble.client;

import com.exiledradio.updateaquaticbackport.bubble.BoatRocking;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * Draws a boat rocking over a bubble column, as 1.14.4's {@code BoatRenderer} does, around whatever boat
 * renderer was installed before it.
 *
 * <p>1.14.4 tilts the boat by the rocking angle about the (1, 0, 1) axis after turning it to its yaw.
 * Here the tilt is put in front of the inner renderer, in the boat's own frame (turned to its yaw about the
 * same point the inner renderer turns about), so the inner renderer's own translation, yaw and hit wobble
 * come after it unchanged. A boat not rocking is drawn exactly as before.
 */
@SideOnly(Side.CLIENT)
public final class RenderBoatRocking extends Render<EntityBoat> {

    private final Render<EntityBoat> inner;

    public RenderBoatRocking(RenderManager manager, Render<EntityBoat> inner) {
        super(manager);
        this.inner = inner;
    }

    @Override
    public void doRender(EntityBoat boat, double x, double y, double z, float yaw, float partialTicks) {
        float angle = BoatRocking.angle(boat, partialTicks);
        if (angle == 0.0F) {
            this.inner.doRender(boat, x, y, z, yaw, partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        this.tilt(x, y, z, yaw, angle);
        this.inner.doRender(boat, x, y, z, yaw, partialTicks);
        GlStateManager.popMatrix();
    }

    private void tilt(double x, double y, double z, float yaw, float angle) {
        GlStateManager.translate((float) x, (float) y + 0.375F, (float) z);
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(angle, 1.0F, 0.0F, 1.0F);
        GlStateManager.rotate(yaw - 180.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.translate(-(float) x, -((float) y + 0.375F), -(float) z);
    }

    @Override
    public boolean shouldRender(EntityBoat boat, ICamera camera, double x, double y, double z) {
        return this.inner.shouldRender(boat, camera, x, y, z);
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        this.inner.doRenderShadowAndFire(entity, x, y, z, yaw, partialTicks);
    }

    @Override
    public boolean isMultipass() {
        return this.inner.isMultipass();
    }

    @Override
    public void renderMultipass(EntityBoat boat, double x, double y, double z, float yaw, float partialTicks) {
        float angle = BoatRocking.angle(boat, partialTicks);
        if (angle == 0.0F) {
            this.inner.renderMultipass(boat, x, y, z, yaw, partialTicks);
            return;
        }
        GlStateManager.pushMatrix();
        this.tilt(x, y, z, yaw, angle);
        this.inner.renderMultipass(boat, x, y, z, yaw, partialTicks);
        GlStateManager.popMatrix();
    }

    @Override
    public void setRenderOutlines(boolean outlines) {
        super.setRenderOutlines(outlines);
        this.inner.setRenderOutlines(outlines);
    }

    /** Never used: the inner renderer binds its own texture. */
    @Override
    @Nullable
    protected ResourceLocation getEntityTexture(EntityBoat boat) {
        return null;
    }
}
