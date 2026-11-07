package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.trident.EntityDrowned;

import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.model.ModelZombie;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.math.MathHelper;

/**
 * 1.14.4's {@code DrownedModel}: a zombie with its own right arm and leg texture areas, the trident thrown
 * from a raised arm, and the swimming stroke.
 */
final class ModelDrowned extends ModelZombie {

    private float swimAnimation;

    /** The body and its outer layer: 64x64 texture with the drowned's own right limbs. */
    ModelDrowned(float scale, float yOffset) {
        super(scale, false);
        this.bipedRightArm = new ModelRenderer(this, 32, 48);
        this.bipedRightArm.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4, scale);
        this.bipedRightArm.setRotationPoint(-5.0F, 2.0F + yOffset, 0.0F);
        this.bipedRightLeg = new ModelRenderer(this, 16, 48);
        this.bipedRightLeg.addBox(-2.0F, 0.0F, -2.0F, 4, 12, 4, scale);
        this.bipedRightLeg.setRotationPoint(-1.9F, 12.0F + yOffset, 0.0F);
    }

    /** Armor: the zombie's armor model, posed as the drowned. */
    ModelDrowned(float scale, boolean armor) {
        super(scale, armor);
    }

    @Override
    public void setLivingAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount, float partialTicks) {
        this.swimAnimation = entity instanceof EntityDrowned ? ((EntityDrowned) entity).getSwimAnimation(partialTicks) : 0.0F;
        super.setLivingAnimations(entity, limbSwing, limbSwingAmount, partialTicks);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            float scale, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        if (entity instanceof EntityLivingBase) {
            TridentPoses.apply(this, (EntityLivingBase) entity);
        }
        float s = this.swimAnimation;
        if (s > 0.0F) {
            this.bipedRightArm.rotateAngleX = lerpAngle(this.bipedRightArm.rotateAngleX, -2.5132742F, s) + s * 0.35F * MathHelper.sin(0.1F * ageInTicks);
            this.bipedLeftArm.rotateAngleX = lerpAngle(this.bipedLeftArm.rotateAngleX, -2.5132742F, s) - s * 0.35F * MathHelper.sin(0.1F * ageInTicks);
            this.bipedRightArm.rotateAngleZ = lerpAngle(this.bipedRightArm.rotateAngleZ, -0.15F, s);
            this.bipedLeftArm.rotateAngleZ = lerpAngle(this.bipedLeftArm.rotateAngleZ, 0.15F, s);
            this.bipedLeftLeg.rotateAngleX -= s * 0.55F * MathHelper.sin(0.1F * ageInTicks);
            this.bipedRightLeg.rotateAngleX += s * 0.55F * MathHelper.sin(0.1F * ageInTicks);
            this.bipedHead.rotateAngleX = 0.0F;
        }
        copyModelAngles(this.bipedHead, this.bipedHeadwear);
    }

    /** 1.14.4's {@code BipedModel.func_205060_a}: the shortest turn from one angle toward another. */
    private static float lerpAngle(float from, float to, float t) {
        float diff = (to - from) % ((float) Math.PI * 2F);
        if (diff < -(float) Math.PI) {
            diff += (float) Math.PI * 2F;
        }
        if (diff >= (float) Math.PI) {
            diff -= (float) Math.PI * 2F;
        }
        return from + t * diff;
    }

}
