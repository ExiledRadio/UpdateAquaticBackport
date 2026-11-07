package com.exiledradio.updateaquaticbackport.fish.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code PufferFishBigModel}: fully puffed. */
@SideOnly(Side.CLIENT)
public class ModelPufferfishBig extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer finRight;
    private final ModelRenderer finLeft;
    private final ModelRenderer spikeTopFront;
    private final ModelRenderer spikeTopMiddle;
    private final ModelRenderer spikeTopBack;
    private final ModelRenderer spikeFrontRight;
    private final ModelRenderer spikeFrontLeft;
    private final ModelRenderer spikeBottomFront;
    private final ModelRenderer spikeBottomMiddle;
    private final ModelRenderer spikeBottomBack;
    private final ModelRenderer spikeBackRight;
    private final ModelRenderer spikeBackLeft;

    public ModelPufferfishBig() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        float q = (float) Math.PI / 4F;
        this.body = new ModelRenderer(this, 0, 0);
        this.body.addBox(-4.0F, -8.0F, -4.0F, 8, 8, 8);
        this.body.setRotationPoint(0.0F, 22.0F, 0.0F);
        this.finRight = new ModelRenderer(this, 24, 0);
        this.finRight.addBox(-2.0F, 0.0F, -1.0F, 2, 1, 2);
        this.finRight.setRotationPoint(-4.0F, 15.0F, -2.0F);
        this.finLeft = new ModelRenderer(this, 24, 3);
        this.finLeft.addBox(0.0F, 0.0F, -1.0F, 2, 1, 2);
        this.finLeft.setRotationPoint(4.0F, 15.0F, -2.0F);
        this.spikeTopFront = new ModelRenderer(this, 15, 17);
        this.spikeTopFront.addBox(-4.0F, -1.0F, 0.0F, 8, 1, 0);
        this.spikeTopFront.setRotationPoint(0.0F, 14.0F, -4.0F);
        this.spikeTopFront.rotateAngleX = q;
        this.spikeTopMiddle = new ModelRenderer(this, 14, 16);
        this.spikeTopMiddle.addBox(-4.0F, -1.0F, 0.0F, 8, 1, 1);
        this.spikeTopMiddle.setRotationPoint(0.0F, 14.0F, 0.0F);
        this.spikeTopBack = new ModelRenderer(this, 23, 18);
        this.spikeTopBack.addBox(-4.0F, -1.0F, 0.0F, 8, 1, 0);
        this.spikeTopBack.setRotationPoint(0.0F, 14.0F, 4.0F);
        this.spikeTopBack.rotateAngleX = -q;
        this.spikeFrontRight = new ModelRenderer(this, 5, 17);
        this.spikeFrontRight.addBox(-1.0F, -8.0F, 0.0F, 1, 8, 0);
        this.spikeFrontRight.setRotationPoint(-4.0F, 22.0F, -4.0F);
        this.spikeFrontRight.rotateAngleY = -q;
        this.spikeFrontLeft = new ModelRenderer(this, 1, 17);
        this.spikeFrontLeft.addBox(0.0F, -8.0F, 0.0F, 1, 8, 0);
        this.spikeFrontLeft.setRotationPoint(4.0F, 22.0F, -4.0F);
        this.spikeFrontLeft.rotateAngleY = q;
        this.spikeBottomFront = new ModelRenderer(this, 15, 20);
        this.spikeBottomFront.addBox(-4.0F, 0.0F, 0.0F, 8, 1, 0);
        this.spikeBottomFront.setRotationPoint(0.0F, 22.0F, -4.0F);
        this.spikeBottomFront.rotateAngleX = -q;
        this.spikeBottomMiddle = new ModelRenderer(this, 15, 20);
        this.spikeBottomMiddle.addBox(-4.0F, 0.0F, 0.0F, 8, 1, 0);
        this.spikeBottomMiddle.setRotationPoint(0.0F, 22.0F, 0.0F);
        this.spikeBottomBack = new ModelRenderer(this, 15, 20);
        this.spikeBottomBack.addBox(-4.0F, 0.0F, 0.0F, 8, 1, 0);
        this.spikeBottomBack.setRotationPoint(0.0F, 22.0F, 4.0F);
        this.spikeBottomBack.rotateAngleX = q;
        this.spikeBackRight = new ModelRenderer(this, 9, 17);
        this.spikeBackRight.addBox(-1.0F, -8.0F, 0.0F, 1, 8, 0);
        this.spikeBackRight.setRotationPoint(-4.0F, 22.0F, 4.0F);
        this.spikeBackRight.rotateAngleY = q;
        this.spikeBackLeft = new ModelRenderer(this, 9, 17);
        this.spikeBackLeft.addBox(0.0F, -8.0F, 0.0F, 1, 8, 0);
        this.spikeBackLeft.setRotationPoint(4.0F, 22.0F, 4.0F);
        this.spikeBackLeft.rotateAngleY = -q;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.finRight.render(scale);
        this.finLeft.render(scale);
        this.spikeTopFront.render(scale);
        this.spikeTopMiddle.render(scale);
        this.spikeTopBack.render(scale);
        this.spikeFrontRight.render(scale);
        this.spikeFrontLeft.render(scale);
        this.spikeBottomFront.render(scale);
        this.spikeBottomMiddle.render(scale);
        this.spikeBottomBack.render(scale);
        this.spikeBackRight.render(scale);
        this.spikeBackLeft.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.finRight.rotateAngleZ = -0.2F + 0.4F * MathHelper.sin(ageInTicks * 0.2F);
        this.finLeft.rotateAngleZ = 0.2F - 0.4F * MathHelper.sin(ageInTicks * 0.2F);
    }
}
