package com.exiledradio.updateaquaticbackport.fish.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code PufferFishSmallModel}: the deflated pufferfish. */
@SideOnly(Side.CLIENT)
public class ModelPufferfishSmall extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer eyeRight;
    private final ModelRenderer eyeLeft;
    private final ModelRenderer finRight;
    private final ModelRenderer finLeft;
    private final ModelRenderer tail;

    public ModelPufferfishSmall() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.body = new ModelRenderer(this, 0, 27);
        this.body.addBox(-1.5F, -2.0F, -1.5F, 3, 2, 3);
        this.body.setRotationPoint(0.0F, 23.0F, 0.0F);
        this.eyeRight = new ModelRenderer(this, 24, 6);
        this.eyeRight.addBox(-1.5F, 0.0F, -1.5F, 1, 1, 1);
        this.eyeRight.setRotationPoint(0.0F, 20.0F, 0.0F);
        this.eyeLeft = new ModelRenderer(this, 28, 6);
        this.eyeLeft.addBox(0.5F, 0.0F, -1.5F, 1, 1, 1);
        this.eyeLeft.setRotationPoint(0.0F, 20.0F, 0.0F);
        this.tail = new ModelRenderer(this, -3, 0);
        this.tail.addBox(-1.5F, 0.0F, 0.0F, 3, 0, 3);
        this.tail.setRotationPoint(0.0F, 22.0F, 1.5F);
        this.finRight = new ModelRenderer(this, 25, 0);
        this.finRight.addBox(-1.0F, 0.0F, 0.0F, 1, 0, 2);
        this.finRight.setRotationPoint(-1.5F, 22.0F, -1.5F);
        this.finLeft = new ModelRenderer(this, 25, 0);
        this.finLeft.addBox(0.0F, 0.0F, 0.0F, 1, 0, 2);
        this.finLeft.setRotationPoint(1.5F, 22.0F, -1.5F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.eyeRight.render(scale);
        this.eyeLeft.render(scale);
        this.tail.render(scale);
        this.finRight.render(scale);
        this.finLeft.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        this.finRight.rotateAngleZ = -0.2F + 0.4F * MathHelper.sin(ageInTicks * 0.2F);
        this.finLeft.rotateAngleZ = 0.2F - 0.4F * MathHelper.sin(ageInTicks * 0.2F);
    }
}
