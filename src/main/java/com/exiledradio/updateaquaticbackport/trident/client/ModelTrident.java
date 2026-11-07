package com.exiledradio.updateaquaticbackport.trident.client;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

/** 1.14.4's {@code TridentModel}: a 31-pixel shaft with a three-pronged head, texture 32x32. */
public final class ModelTrident extends ModelBase {

    private final ModelRenderer shaft;

    public ModelTrident() {
        this.textureWidth = 32;
        this.textureHeight = 32;
        this.shaft = new ModelRenderer(this, 0, 0);
        this.shaft.addBox(-0.5F, -4.0F, -0.5F, 1, 31, 1, 0.0F);
        ModelRenderer base = new ModelRenderer(this, 4, 0);
        base.addBox(-1.5F, 0.0F, -0.5F, 3, 2, 1);
        this.shaft.addChild(base);
        ModelRenderer left = new ModelRenderer(this, 4, 3);
        left.addBox(-2.5F, -3.0F, -0.5F, 1, 4, 1);
        this.shaft.addChild(left);
        ModelRenderer right = new ModelRenderer(this, 4, 3);
        right.mirror = true;
        right.addBox(1.5F, -3.0F, -0.5F, 1, 4, 1);
        this.shaft.addChild(right);
    }

    public void renderTrident() {
        this.shaft.render(0.0625F);
    }
}
