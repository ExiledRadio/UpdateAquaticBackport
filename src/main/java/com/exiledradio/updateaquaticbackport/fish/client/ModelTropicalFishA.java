package com.exiledradio.updateaquaticbackport.fish.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code TropicalFishAModel}: the small body (shape 0). {@code grow} inflates it for the pattern layer. */
@SideOnly(Side.CLIENT)
public class ModelTropicalFishA extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer tail;
    private final ModelRenderer finRight;
    private final ModelRenderer finLeft;
    private final ModelRenderer finTop;

    public ModelTropicalFishA(float grow) {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.body = new ModelRenderer(this, 0, 0);
        this.body.addBox(-1.0F, -1.5F, -3.0F, 2, 3, 6, grow);
        this.body.setRotationPoint(0.0F, 22.0F, 0.0F);
        this.tail = new ModelRenderer(this, 22, -6);
        this.tail.addBox(0.0F, -1.5F, 0.0F, 0, 3, 6, grow);
        this.tail.setRotationPoint(0.0F, 22.0F, 3.0F);
        this.finRight = new ModelRenderer(this, 2, 16);
        this.finRight.addBox(-2.0F, -1.0F, 0.0F, 2, 2, 0, grow);
        this.finRight.setRotationPoint(-1.0F, 22.5F, 0.0F);
        this.finRight.rotateAngleY = (float) Math.PI / 4F;
        this.finLeft = new ModelRenderer(this, 2, 12);
        this.finLeft.addBox(0.0F, -1.0F, 0.0F, 2, 2, 0, grow);
        this.finLeft.setRotationPoint(1.0F, 22.5F, 0.0F);
        this.finLeft.rotateAngleY = -(float) Math.PI / 4F;
        this.finTop = new ModelRenderer(this, 10, -5);
        this.finTop.addBox(0.0F, -3.0F, 0.0F, 0, 3, 6, grow);
        this.finTop.setRotationPoint(0.0F, 20.5F, -3.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.tail.render(scale);
        this.finRight.render(scale);
        this.finLeft.render(scale);
        this.finTop.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        float f = entity.isInWater() ? 1.0F : 1.5F;
        this.tail.rotateAngleY = -f * 0.45F * MathHelper.sin(0.6F * ageInTicks);
    }
}
