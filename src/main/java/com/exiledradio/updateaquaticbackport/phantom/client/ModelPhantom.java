package com.exiledradio.updateaquaticbackport.phantom.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

/** 1.14.4's {@code PhantomModel}, box for box, on 1.18's 64x64 texture (the same layout). */
public class ModelPhantom extends ModelBase {

    private final ModelRenderer body;
    private final ModelRenderer leftWingBody;
    private final ModelRenderer leftWingTip;
    private final ModelRenderer rightWingBody;
    private final ModelRenderer rightWingTip;
    private final ModelRenderer head;
    private final ModelRenderer tail;
    private final ModelRenderer tailTip;

    public ModelPhantom() {
        this.textureWidth = 64;
        this.textureHeight = 64;
        this.body = new ModelRenderer(this, 0, 8);
        this.body.addBox(-3.0F, -2.0F, -8.0F, 5, 3, 9);
        this.tail = new ModelRenderer(this, 3, 20);
        this.tail.addBox(-2.0F, 0.0F, 0.0F, 3, 2, 6);
        this.tail.setRotationPoint(0.0F, -2.0F, 1.0F);
        this.body.addChild(this.tail);
        this.tailTip = new ModelRenderer(this, 4, 29);
        this.tailTip.addBox(-1.0F, 0.0F, 0.0F, 1, 1, 6);
        this.tailTip.setRotationPoint(0.0F, 0.5F, 6.0F);
        this.tail.addChild(this.tailTip);
        this.leftWingBody = new ModelRenderer(this, 23, 12);
        this.leftWingBody.addBox(0.0F, 0.0F, 0.0F, 6, 2, 9);
        this.leftWingBody.setRotationPoint(2.0F, -2.0F, -8.0F);
        this.leftWingTip = new ModelRenderer(this, 16, 24);
        this.leftWingTip.addBox(0.0F, 0.0F, 0.0F, 13, 1, 9);
        this.leftWingTip.setRotationPoint(6.0F, 0.0F, 0.0F);
        this.leftWingBody.addChild(this.leftWingTip);
        this.rightWingBody = new ModelRenderer(this, 23, 12);
        this.rightWingBody.mirror = true;
        this.rightWingBody.addBox(-6.0F, 0.0F, 0.0F, 6, 2, 9);
        this.rightWingBody.setRotationPoint(-3.0F, -2.0F, -8.0F);
        this.rightWingTip = new ModelRenderer(this, 16, 24);
        this.rightWingTip.mirror = true;
        this.rightWingTip.addBox(-13.0F, 0.0F, 0.0F, 13, 1, 9);
        this.rightWingTip.setRotationPoint(-6.0F, 0.0F, 0.0F);
        this.rightWingBody.addChild(this.rightWingTip);
        this.leftWingBody.rotateAngleZ = 0.1F;
        this.leftWingTip.rotateAngleZ = 0.1F;
        this.rightWingBody.rotateAngleZ = -0.1F;
        this.rightWingTip.rotateAngleZ = -0.1F;
        this.body.rotateAngleX = -0.1F;
        this.head = new ModelRenderer(this, 0, 0);
        this.head.addBox(-4.0F, -2.0F, -5.0F, 7, 3, 5);
        this.head.setRotationPoint(0.0F, 1.0F, -7.0F);
        this.head.rotateAngleX = 0.2F;
        this.body.addChild(this.head);
        this.body.addChild(this.leftWingBody);
        this.body.addChild(this.rightWingBody);
    }

    @Override
    public void render(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale) {
        this.setRotationAngles(limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch, scale, entity);
        this.body.render(scale);
    }

    /** 1.14.4: the wings beat 16 degrees each way, the tail follows at twice the rate. */
    @Override
    public void setRotationAngles(float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch,
            float scale, Entity entity) {
        float t = (entity.getEntityId() * 3 + ageInTicks) * 0.13F;
        this.leftWingBody.rotateAngleZ = MathHelper.cos(t) * 16.0F * 0.017453292F;
        this.leftWingTip.rotateAngleZ = MathHelper.cos(t) * 16.0F * 0.017453292F;
        this.rightWingBody.rotateAngleZ = -this.leftWingBody.rotateAngleZ;
        this.rightWingTip.rotateAngleZ = -this.leftWingTip.rotateAngleZ;
        this.tail.rotateAngleX = -(5.0F + MathHelper.cos(t * 2.0F) * 5.0F) * 0.017453292F;
        this.tailTip.rotateAngleX = -(5.0F + MathHelper.cos(t * 2.0F) * 5.0F) * 0.017453292F;
    }
}
