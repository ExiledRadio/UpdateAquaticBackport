package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.trident.ItemTrident;

import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;

/**
 * 1.14.4's {@code THROW_SPEAR} arm pose: the arm holding a trident being thrown is raised overhead.
 *
 * <p>The drowned's model calls {@link #apply}. 1.12.2's player model cannot be changed without replacing
 * it, which the swimming stream's own player model will do; that model should call {@link #apply} after
 * its own angles and set {@link #playersRaiseArm}, which also makes players hold the spear point up.
 */
public final class TridentPoses {

    /** True once a player model raises the throwing arm; until then players hold the spear as usual. */
    public static boolean playersRaiseArm;

    private TridentPoses() {
    }

    /** @return the side of the arm raising a trident to throw it, or null */
    public static EnumHandSide throwingSide(EntityLivingBase entity) {
        if (!entity.isHandActive() || !(entity.getActiveItemStack().getItem() instanceof ItemTrident)) {
            return null;
        }
        EnumHandSide main = entity.getPrimaryHand();
        return entity.getActiveHand() == EnumHand.MAIN_HAND ? main : main.opposite();
    }

    /** Raises the throwing arm of a biped model, after its own angles are set. */
    public static void apply(ModelBiped model, EntityLivingBase entity) {
        EnumHandSide side = throwingSide(entity);
        if (side != null) {
            raise(side == EnumHandSide.RIGHT ? model.bipedRightArm : model.bipedLeftArm);
        }
    }

    static void raise(ModelRenderer arm) {
        arm.rotateAngleX = arm.rotateAngleX * 0.5F - (float) Math.PI;
        arm.rotateAngleY = 0.0F;
    }
}
