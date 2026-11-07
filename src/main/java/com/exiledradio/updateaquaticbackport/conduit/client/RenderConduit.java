package com.exiledradio.updateaquaticbackport.conduit.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.conduit.TileEntityConduit;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;

/**
 * The conduit's look: 1.14.4's {@code ConduitTileEntityRenderer}, with 1.18's textures.
 *
 * <ul>
 *   <li>Inactive: the closed shell, a 6x6x6 cube.</li>
 *   <li>Active: the cage (8x8x8) bobbing and turning, the wind round it - one of 22 frames, every 3 ticks,
 *       in one of three orientations, a new one every 66 ticks - and the eye facing the camera, open with
 *       a full frame.</li>
 * </ul>
 * 1.18 keeps the wind's frames in one 64x704 strip, the same layout 1.14 cut from a 64x1024 sheet.
 */
public class RenderConduit extends TileEntitySpecialRenderer<TileEntityConduit> {

    private static final ResourceLocation BASE = tex("base");
    private static final ResourceLocation CAGE = tex("cage");
    private static final ResourceLocation WIND = tex("wind");
    private static final ResourceLocation WIND_VERTICAL = tex("wind_vertical");
    private static final ResourceLocation OPEN_EYE = tex("open_eye");
    private static final ResourceLocation CLOSED_EYE = tex("closed_eye");

    private static final int WIND_FRAMES = 22;

    private final ModelRenderer shell;
    private final ModelRenderer cage;
    private final ModelRenderer[] wind = new ModelRenderer[WIND_FRAMES];
    private final ModelRenderer eye;

    public RenderConduit() {
        this.shell = new ModelRenderer(model(32, 16), 0, 0);
        this.shell.addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6);
        this.cage = new ModelRenderer(model(32, 16), 0, 0);
        this.cage.addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8);
        ModelBase windModel = model(64, 32 * WIND_FRAMES);
        for (int i = 0; i < WIND_FRAMES; i++) {
            this.wind[i] = new ModelRenderer(windModel, 0, 32 * i);
            this.wind[i].addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16);
        }
        // 1.18's eye textures are 16x16 with the eye in the top-left 8x8.
        this.eye = new ModelRenderer(model(16, 16), 0, 0);
        this.eye.addBox(-4.0F, -4.0F, 0.0F, 8, 8, 0, 0.01F);
    }

    private static ResourceLocation tex(String name) {
        return new ResourceLocation(UpdateAquaticBackport.MOD_ID, "textures/entity/conduit/" + name + ".png");
    }

    private static ModelBase model(int width, int height) {
        ModelBase m = new ModelBase() {
        };
        m.textureWidth = width;
        m.textureHeight = height;
        return m;
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    @Override
    public void render(TileEntityConduit te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        float time = te.ticksExisted + partialTicks;
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        if (!te.isActive()) {
            // 1.14.4 passes the radians-scaled rotation here unconverted, so the shell barely turns.
            float rotation = te.getActiveRotation(0.0F);
            bind(BASE);
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            GlStateManager.rotate(rotation, 0.0F, 1.0F, 0.0F);
            this.shell.render(0.0625F);
            GlStateManager.popMatrix();
            return;
        }

        float rotation = te.getActiveRotation(partialTicks) * (180F / (float) Math.PI);
        float bob = MathHelper.sin(time * 0.1F) / 2.0F + 0.5F;
        bob = bob * bob + bob;
        bind(CAGE);
        GlStateManager.disableCull();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.3 + bob * 0.2F, z + 0.5);
        GlStateManager.rotate(rotation, 0.5F, 1.0F, 0.5F);
        this.cage.render(0.0625F);
        GlStateManager.popMatrix();

        ModelRenderer frame = this.wind[te.ticksExisted / 3 % WIND_FRAMES];
        int orientation = te.ticksExisted / 66 % 3;
        bind(orientation == 1 ? WIND_VERTICAL : WIND);
        for (int inner = 0; inner < 2; inner++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);
            if (inner == 1) {
                GlStateManager.scale(0.875F, 0.875F, 0.875F);
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
            } else if (orientation == 1) {
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            } else if (orientation == 2) {
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }
            frame.render(0.0625F);
            GlStateManager.popMatrix();
        }

        RenderManager camera = Minecraft.getMinecraft().getRenderManager();
        boolean frontView = Minecraft.getMinecraft().gameSettings.thirdPersonView == 2;
        bind(te.isEyeOpen() ? OPEN_EYE : CLOSED_EYE);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 0.5, y + 0.3 + bob * 0.2F, z + 0.5);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.rotate(-camera.playerViewY, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate((frontView ? -1 : 1) * camera.playerViewX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
        this.eye.render(1.0F / 12.0F);
        GlStateManager.popMatrix();
        GlStateManager.enableCull();
    }
}
