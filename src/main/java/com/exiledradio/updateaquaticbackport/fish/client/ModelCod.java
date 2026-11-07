package com.exiledradio.updateaquaticbackport.fish.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** 1.14.4's {@code CodModel}. */
@SideOnly(Side.CLIENT)
public class ModelCod extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer finTop;
    private final ModelRenderer head;
    private final ModelRenderer headFront;
    private final ModelRenderer finRight;
    private final ModelRenderer finLeft;
    private final ModelRenderer tail;

    public ModelCod() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.body = new ModelRenderer(this, 0, 0);
        this.body.addBox(-1.0F, -2.0F, 0.0F, 2, 4, 7);
        this.body.setRotationPoint(0.0F, 22.0F, 0.0F);
        this.head = new ModelRenderer(this, 11, 0);
        this.head.addBox(-1.0F, -2.0F, -3.0F, 2, 4, 3);
        this.head.setRotationPoint(0.0F, 22.0F, 0.0F);
        this.headFront = new ModelRenderer(this, 0, 0);
        this.headFront.addBox(-1.0F, -2.0F, -1.0F, 2, 3, 1);
        this.headFront.setRotationPoint(0.0F, 22.0F, -3.0F);
        this.finRight = new ModelRenderer(this, 22, 1);
        this.finRight.addBox(-2.0F, 0.0F, -1.0F, 2, 0, 2);
        this.finRight.setRotationPoint(-1.0F, 23.0F, 0.0F);
        this.finRight.rotateAngleZ = -(float) Math.PI / 4F;
        this.finLeft = new ModelRenderer(this, 22, 4);
        this.finLeft.addBox(0.0F, 0.0F, -1.0F, 2, 0, 2);
        this.finLeft.setRotationPoint(1.0F, 23.0F, 0.0F);
        this.finLeft.rotateAngleZ = (float) Math.PI / 4F;
        this.tail = new ModelRenderer(this, 22, 3);
        this.tail.addBox(0.0F, -2.0F, 0.0F, 0, 4, 4);
        this.tail.setRotationPoint(0.0F, 22.0F, 7.0F);
        this.finTop = new ModelRenderer(this, 20, -6);
        this.finTop.addBox(0.0F, -1.0F, -1.0F, 0, 1, 6);
        this.finTop.setRotationPoint(0.0F, 20.0F, 0.0F);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
        this.head.render(scale);
        this.headFront.render(scale);
        this.finRight.render(scale);
        this.finLeft.render(scale);
        this.tail.render(scale);
        this.finTop.render(scale);
    }

    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, Entity entity) {
        float f = entity.isInWater() ? 1.0F : 1.5F;
        this.tail.rotateAngleY = -f * 0.45F * MathHelper.sin(0.6F * ageInTicks);
    }
}
