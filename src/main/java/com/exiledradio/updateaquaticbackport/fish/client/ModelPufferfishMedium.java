package com.exiledradio.updateaquaticbackport.fish.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code PufferFishMediumModel}: half puffed. */
@SideOnly(Side.CLIENT)
public class ModelPufferfishMedium extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer finRight;
    private final ModelRenderer finLeft;
    private final ModelRenderer spikeFront;
    private final ModelRenderer spikeBack;
    private final ModelRenderer spikeBackRight;
    private final ModelRenderer spikeBackLeft;
    private final ModelRenderer spikeFrontLeft;
    private final ModelRenderer spikeFrontRight;
    private final ModelRenderer spikeBottomBack;
    private final ModelRenderer spikeBottomFront;

    public ModelPufferfishMedium() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        float q = (float) Math.PI / 4F;
        this.body = new ModelRenderer(this, 12, 22);
        this.body.addBox(-2.5F, -5.0F, -2.5F, 5, 5, 5);
        this.body.setRotationPoint(0.0F, 22.0F, 0.0F);
        this.finRight = new ModelRenderer(this, 24, 0);
        this.finRight.addBox(-2.0F, 0.0F, 0.0F, 2, 0, 2);
        this.finRight.setRotationPoint(-2.5F, 17.0F, -1.5F);
        this.finLeft = new ModelRenderer(this, 24, 3);
        this.finLeft.addBox(0.0F, 0.0F, 0.0F, 2, 0, 2);
        this.finLeft.setRotationPoint(2.5F, 17.0F, -1.5F);
        this.spikeFront = new ModelRenderer(this, 15, 16);
        this.spikeFront.addBox(-2.5F, -1.0F, 0.0F, 5, 1, 1);
        this.spikeFront.setRotationPoint(0.0F, 17.0F, -2.5F);
        this.spikeFront.rotateAngleX = q;
        this.spikeBack = new ModelRenderer(this, 10, 16);
        this.spikeBack.addBox(-2.5F, -1.0F, -1.0F, 5, 1, 1);
        this.spikeBack.setRotationPoint(0.0F, 17.0F, 2.5F);
        this.spikeBack.rotateAngleX = -q;
        this.spikeBackRight = new ModelRenderer(this, 8, 16);
        this.spikeBackRight.addBox(-1.0F, -5.0F, 0.0F, 1, 5, 1);
        this.spikeBackRight.setRotationPoint(-2.5F, 22.0F, -2.5F);
        this.spikeBackRight.rotateAngleY = -q;
        this.spikeBackLeft = new ModelRenderer(this, 8, 16);
        this.spikeBackLeft.addBox(-1.0F, -5.0F, 0.0F, 1, 5, 1);
        this.spikeBackLeft.setRotationPoint(-2.5F, 22.0F, 2.5F);
        this.spikeBackLeft.rotateAngleY = q;
        this.spikeFrontLeft = new ModelRenderer(this, 4, 16);
        this.spikeFrontLeft.addBox(0.0F, -5.0F, 0.0F, 1, 5, 1);
        this.spikeFrontLeft.setRotationPoint(2.5F, 22.0F, 2.5F);
        this.spikeFrontLeft.rotateAngleY = -q;
        this.spikeFrontRight = new ModelRenderer(this, 0, 16);
        this.spikeFrontRight.addBox(0.0F, -5.0F, 0.0F, 1, 5, 1);
        this.spikeFrontRight.setRotationPoint(2.5F, 22.0F, -2.5F);
        this.spikeFrontRight.rotateAngleY = q;
        this.spikeBottomBack = new ModelRenderer(this, 8, 22);
        this.spikeBottomBack.addBox(0.0F, 0.0F, 0.0F, 1, 1, 1);
        this.spikeBottomBack.setRotationPoint(0.5F, 22.0F, 2.5F);
        this.spikeBottomBack.rotateAngleX = q;
        this.spikeBottomFront = new ModelRenderer(this, 17, 21);
        this.spikeBottomFront.addBox(-2.5F, 0.0F, 0.0F, 5, 1, 1);
        this.spikeBottomFront.setRotationPoint(0.0F, 22.0F, -2.5F);
        this.spikeBottomFront.rotateAngleX = -q;
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.finRight.render(scale);
        this.finLeft.render(scale);
        this.spikeFront.render(scale);
        this.spikeBack.render(scale);
        this.spikeBackRight.render(scale);
        this.spikeBackLeft.render(scale);
        this.spikeFrontLeft.render(scale);
        this.spikeFrontRight.render(scale);
        this.spikeBottomBack.render(scale);
        this.spikeBottomFront.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.finRight.rotateAngleZ = -0.2F + 0.4F * MathHelper.sin(ageInTicks * 0.2F);
        this.finLeft.rotateAngleZ = 0.2F - 0.4F * MathHelper.sin(ageInTicks * 0.2F);
    }
}
