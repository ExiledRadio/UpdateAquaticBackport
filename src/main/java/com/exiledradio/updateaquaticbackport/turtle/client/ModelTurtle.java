package com.exiledradio.updateaquaticbackport.turtle.client;

import com.exiledradio.updateaquaticbackport.turtle.EntityTurtle;

import net.minecraft.client.model.ModelQuadruped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14.4's {@code TurtleModel}: shell, plastron, head, four flippers, and the egg bulge under a turtle
 * carrying eggs. A baby is drawn at a sixth of the size. On land the front flippers paddle sideways
 * (faster while digging).
 */
@SideOnly(Side.CLIENT)
public class ModelTurtle extends ModelQuadruped {

    private final ModelRenderer eggBelly;

    public ModelTurtle(float scale) {
        super(12, scale);
        this.textureWidth = 128;
        this.textureHeight = 64;
        this.head = new ModelRenderer(this, 3, 0);
        this.head.addBox(-3.0F, -1.0F, -3.0F, 6, 5, 6, 0.0F);
        this.head.setRotationPoint(0.0F, 19.0F, -10.0F);
        this.body = new ModelRenderer(this);
        this.body.setTextureOffset(7, 37).addBox(-9.5F, 3.0F, -10.0F, 19, 20, 6, 0.0F);
        this.body.setTextureOffset(31, 1).addBox(-5.5F, 3.0F, -13.0F, 11, 18, 3, 0.0F);
        this.body.setRotationPoint(0.0F, 11.0F, -10.0F);
        this.eggBelly = new ModelRenderer(this);
        this.eggBelly.setTextureOffset(70, 33).addBox(-4.5F, 3.0F, -14.0F, 9, 18, 1, 0.0F);
        this.eggBelly.setRotationPoint(0.0F, 11.0F, -10.0F);
        this.leg1 = new ModelRenderer(this, 1, 23);
        this.leg1.addBox(-2.0F, 0.0F, 0.0F, 4, 1, 10, 0.0F);
        this.leg1.setRotationPoint(-3.5F, 22.0F, 11.0F);
        this.leg2 = new ModelRenderer(this, 1, 12);
        this.leg2.addBox(-2.0F, 0.0F, 0.0F, 4, 1, 10, 0.0F);
        this.leg2.setRotationPoint(3.5F, 22.0F, 11.0F);
        this.leg3 = new ModelRenderer(this, 27, 30);
        this.leg3.addBox(-13.0F, 0.0F, -2.0F, 13, 1, 5, 0.0F);
        this.leg3.setRotationPoint(-5.0F, 21.0F, -4.0F);
        this.leg4 = new ModelRenderer(this, 27, 24);
        this.leg4.addBox(0.0F, 0.0F, -2.0F, 13, 1, 5, 0.0F);
        this.leg4.setRotationPoint(5.0F, 21.0F, -4.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        boolean egg = entity instanceof EntityTurtle && ((EntityTurtle) entity).hasEgg();
        if (this.isChild) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(1.0F / 6.0F, 1.0F / 6.0F, 1.0F / 6.0F);
            GlStateManager.translate(0.0F, 120.0F * scale, 0.0F);
            this.head.render(scale);
            this.body.render(scale);
            this.leg1.render(scale);
            this.leg2.render(scale);
            this.leg3.render(scale);
            this.leg4.render(scale);
            GlStateManager.popMatrix();
        } else {
            GlStateManager.pushMatrix();
            if (egg) {
                GlStateManager.translate(0.0F, -0.08F, 0.0F);
            }
            this.head.render(scale);
            this.body.render(scale);
            this.leg1.render(scale);
            this.leg2.render(scale);
            this.leg3.render(scale);
            this.leg4.render(scale);
            if (egg) {
                this.eggBelly.render(scale);
            }
            GlStateManager.popMatrix();
        }
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
                                  float scaleFactor, Entity entity) {
        super.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scaleFactor, entity);
        this.leg1.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F * 0.6F) * 0.5F * limbSwingAmount;
        this.leg2.rotateAngleX = MathHelper.cos(limbSwing * 0.6662F * 0.6F + (float) Math.PI) * 0.5F * limbSwingAmount;
        this.leg3.rotateAngleZ = MathHelper.cos(limbSwing * 0.6662F * 0.6F + (float) Math.PI) * 0.5F * limbSwingAmount;
        this.leg4.rotateAngleZ = MathHelper.cos(limbSwing * 0.6662F * 0.6F) * 0.5F * limbSwingAmount;
        this.leg3.rotateAngleX = 0.0F;
        this.leg4.rotateAngleX = 0.0F;
        this.leg3.rotateAngleY = 0.0F;
        this.leg4.rotateAngleY = 0.0F;
        this.leg1.rotateAngleY = 0.0F;
        this.leg2.rotateAngleY = 0.0F;
        this.eggBelly.rotateAngleX = (float) Math.PI / 2F;
        if (entity instanceof EntityTurtle && !entity.isInWater() && entity.onGround) {
            boolean digging = ((EntityTurtle) entity).isDigging();
            float speed = digging ? 4.0F : 1.0F;
            float reach = digging ? 2.0F : 1.0F;
            this.leg3.rotateAngleY = MathHelper.cos(speed * limbSwing * 5.0F + (float) Math.PI) * 8.0F * limbSwingAmount * reach;
            this.leg3.rotateAngleZ = 0.0F;
            this.leg4.rotateAngleY = MathHelper.cos(speed * limbSwing * 5.0F) * 8.0F * limbSwingAmount * reach;
            this.leg4.rotateAngleZ = 0.0F;
            this.leg1.rotateAngleY = MathHelper.cos(limbSwing * 5.0F + (float) Math.PI) * 3.0F * limbSwingAmount;
            this.leg1.rotateAngleX = 0.0F;
            this.leg2.rotateAngleY = MathHelper.cos(limbSwing * 5.0F) * 3.0F * limbSwingAmount;
            this.leg2.rotateAngleX = 0.0F;
        }
    }
}
