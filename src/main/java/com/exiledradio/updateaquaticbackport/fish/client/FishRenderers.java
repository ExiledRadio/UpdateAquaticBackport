package com.exiledradio.updateaquaticbackport.fish.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.fish.EntityCod;
import com.exiledradio.updateaquaticbackport.fish.EntityPufferfish;
import com.exiledradio.updateaquaticbackport.fish.EntitySalmon;
import com.exiledradio.updateaquaticbackport.fish.EntityTropicalFish;
import com.exiledradio.updateaquaticbackport.fish.FishContent;
import com.exiledradio.updateaquaticbackport.fish.TropicalFishVariant;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderLiving;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * The four fish renderers, ported from 1.14.4's: the swim wiggle, lying on their side out of water, the
 * pufferfish's model by puff stage, a tropical fish's body and pattern tinted by its two colors.
 *
 * <p>Registered from the model registry event, which runs in pre-init before the render manager exists.
 * A dedicated server never loads this class: the subscriber is client-side only.
 */
@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class FishRenderers {

    private FishRenderers() {
    }

    static ResourceLocation texture(String name) {
        return Content.id("textures/entity/fish/" + name + ".png");
    }

    @SubscribeEvent
    public static void register(ModelRegistryEvent event) {
        if (!FishContent.enabled()) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityCod.class, Cod::new);
        RenderingRegistry.registerEntityRenderingHandler(EntitySalmon.class, Salmon::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityPufferfish.class, Pufferfish::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityTropicalFish.class, TropicalFish::new);
    }

    /** 1.14.4's {@code CodRenderer}. */
    public static final class Cod extends RenderLiving<EntityCod> {
        private static final ResourceLocation TEXTURE = texture("cod");

        public Cod(RenderManager manager) {
            super(manager, new ModelCod(), 0.3F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityCod entity) {
            return TEXTURE;
        }

        @Override
        protected void applyRotations(EntityCod entity, float ageInTicks, float rotationYaw, float partialTicks) {
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
            GlStateManager.rotate(4.3F * MathHelper.sin(0.6F * ageInTicks), 0.0F, 1.0F, 0.0F);
            if (!entity.isInWater()) {
                GlStateManager.translate(0.1F, 0.1F, -0.1F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    /** 1.14.4's {@code SalmonRenderer}. */
    public static final class Salmon extends RenderLiving<EntitySalmon> {
        private static final ResourceLocation TEXTURE = texture("salmon");

        public Salmon(RenderManager manager) {
            super(manager, new ModelSalmon(), 0.4F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntitySalmon entity) {
            return TEXTURE;
        }

        @Override
        protected void applyRotations(EntitySalmon entity, float ageInTicks, float rotationYaw, float partialTicks) {
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
            float f = 1.0F;
            float f1 = 1.0F;
            if (!entity.isInWater()) {
                f = 1.3F;
                f1 = 1.7F;
            }
            GlStateManager.rotate(f * 4.3F * MathHelper.sin(f1 * 0.6F * ageInTicks), 0.0F, 1.0F, 0.0F);
            GlStateManager.translate(0.0F, 0.0F, -0.4F);
            if (!entity.isInWater()) {
                GlStateManager.translate(0.2F, 0.1F, 0.0F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    /** 1.14.4's {@code PufferfishRenderer}: a model and shadow per puff stage, and a slow bob. */
    public static final class Pufferfish extends RenderLiving<EntityPufferfish> {
        private static final ResourceLocation TEXTURE = texture("pufferfish");
        private final ModelBase small = new ModelPufferfishSmall();
        private final ModelBase medium = new ModelPufferfishMedium();
        private final ModelBase big = new ModelPufferfishBig();

        public Pufferfish(RenderManager manager) {
            super(manager, new ModelPufferfishBig(), 0.2F);
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityPufferfish entity) {
            return TEXTURE;
        }

        @Override
        public void doRender(EntityPufferfish entity, double x, double y, double z, float entityYaw, float partialTicks) {
            int state = entity.getPuffState();
            this.mainModel = state == 0 ? this.small : state == 1 ? this.medium : this.big;
            this.shadowSize = 0.1F + 0.1F * state;
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }

        @Override
        protected void applyRotations(EntityPufferfish entity, float ageInTicks, float rotationYaw, float partialTicks) {
            GlStateManager.translate(0.0F, MathHelper.cos(ageInTicks * 0.05F) * 0.08F, 0.0F);
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
        }
    }

    /** 1.14.4's {@code TropicalFishRenderer}: body by shape, tinted with the body color; pattern layer on top. */
    public static final class TropicalFish extends RenderLiving<EntityTropicalFish> {
        private static final ResourceLocation[] BODIES = {texture("tropical_a"), texture("tropical_b")};
        private final ModelBase shapeA = new ModelTropicalFishA(0.0F);
        private final ModelBase shapeB = new ModelTropicalFishB(0.0F);

        public TropicalFish(RenderManager manager) {
            super(manager, new ModelTropicalFishA(0.0F), 0.15F);
            this.addLayer(new Pattern(this));
        }

        @Override
        protected ResourceLocation getEntityTexture(EntityTropicalFish entity) {
            return BODIES[TropicalFishVariant.shape(entity.getVariant())];
        }

        @Override
        public void doRender(EntityTropicalFish entity, double x, double y, double z, float entityYaw, float partialTicks) {
            this.mainModel = TropicalFishVariant.shape(entity.getVariant()) == 0 ? this.shapeA : this.shapeB;
            super.doRender(entity, x, y, z, entityYaw, partialTicks);
        }

        /** The body color goes on right before the body is drawn; the base class resets color afterwards. */
        @Override
        protected void renderModel(EntityTropicalFish entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, float scale) {
            float[] rgb = EnumDyeColor.byMetadata(TropicalFishVariant.bodyColor(entity.getVariant())).getColorComponentValues();
            GlStateManager.color(rgb[0], rgb[1], rgb[2]);
            super.renderModel(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        protected void applyRotations(EntityTropicalFish entity, float ageInTicks, float rotationYaw, float partialTicks) {
            super.applyRotations(entity, ageInTicks, rotationYaw, partialTicks);
            GlStateManager.rotate(4.3F * MathHelper.sin(0.6F * ageInTicks), 0.0F, 1.0F, 0.0F);
            if (!entity.isInWater()) {
                GlStateManager.translate(0.2F, 0.1F, 0.0F);
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
            }
        }
    }

    /** 1.14.4's {@code TropicalFishPatternLayer}: the pattern texture on a slightly larger body, in the pattern color. */
    public static final class Pattern implements LayerRenderer<EntityTropicalFish> {
        private static final ResourceLocation[][] PATTERNS = new ResourceLocation[2][6];

        static {
            for (int p = 0; p < 6; p++) {
                PATTERNS[0][p] = texture("tropical_a_pattern_" + (p + 1));
                PATTERNS[1][p] = texture("tropical_b_pattern_" + (p + 1));
            }
        }

        private final TropicalFish renderer;
        private final ModelBase shapeA = new ModelTropicalFishA(0.008F);
        private final ModelBase shapeB = new ModelTropicalFishB(0.008F);

        Pattern(TropicalFish renderer) {
            this.renderer = renderer;
        }

        @Override
        public void doRenderLayer(EntityTropicalFish entity, float limbSwing, float limbSwingAmount, float partialTicks,
                                  float ageInTicks, float netHeadYaw, float headPitch, float scale) {
            if (entity.isInvisible()) {
                return;
            }
            int variant = entity.getVariant();
            int shape = TropicalFishVariant.shape(variant);
            ModelBase model = shape == 0 ? this.shapeA : this.shapeB;
            this.renderer.bindTexture(PATTERNS[shape][TropicalFishVariant.pattern(variant)]);
            float[] rgb = EnumDyeColor.byMetadata(TropicalFishVariant.patternColor(variant)).getColorComponentValues();
            GlStateManager.color(rgb[0], rgb[1], rgb[2]);
            model.setModelAttributes(this.renderer.getMainModel());
            model.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
            model.render(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale);
        }

        @Override
        public boolean shouldCombineTextures() {
            return true;
        }
    }
}
