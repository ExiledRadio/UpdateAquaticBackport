package com.exiledradio.updateaquaticbackport.fish.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code TropicalFishBModel}: the tall body (shape 1). {@code grow} inflates it for the pattern layer. */
@SideOnly(Side.CLIENT)
public class ModelTropicalFishB extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer tail;
    private final ModelRenderer finRight;
    private final ModelRenderer finLeft;
    private final ModelRenderer finTop;
    private final ModelRenderer finBottom;

    public ModelTropicalFishB(float grow) {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.body = new ModelRenderer(this, 0, 20);
        this.body.addBox(-1.0F, -3.0F, -3.0F, 2, 6, 6, grow);
        this.body.setRotationPoint(0.0F, 19.0F, 0.0F);
        this.tail = new ModelRenderer(this, 21, 16);
        this.tail.addBox(0.0F, -3.0F, 0.0F, 0, 6, 5, grow);
        this.tail.setRotationPoint(0.0F, 19.0F, 3.0F);
        this.finRight = new ModelRenderer(this, 2, 16);
        this.finRight.addBox(-2.0F, 0.0F, 0.0F, 2, 2, 0, grow);
        this.finRight.setRotationPoint(-1.0F, 20.0F, 0.0F);
        this.finRight.rotateAngleY = (float) Math.PI / 4F;
        this.finLeft = new ModelRenderer(this, 2, 12);
        this.finLeft.addBox(0.0F, 0.0F, 0.0F, 2, 2, 0, grow);
        this.finLeft.setRotationPoint(1.0F, 20.0F, 0.0F);
        this.finLeft.rotateAngleY = -(float) Math.PI / 4F;
        this.finTop = new ModelRenderer(this, 20, 11);
        this.finTop.addBox(0.0F, -4.0F, 0.0F, 0, 4, 6, grow);
        this.finTop.setRotationPoint(0.0F, 16.0F, -3.0F);
        this.finBottom = new ModelRenderer(this, 20, 21);
        this.finBottom.addBox(0.0F, 0.0F, 0.0F, 0, 4, 6, grow);
        this.finBottom.setRotationPoint(0.0F, 22.0F, -3.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.tail.render(scale);
        this.finRight.render(scale);
        this.finLeft.render(scale);
        this.finTop.render(scale);
        this.finBottom.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        float f = entity.isInWater() ? 1.0F : 1.5F;
        this.tail.rotateAngleY = -f * 0.45F * MathHelper.sin(0.6F * ageInTicks);
    }
}
