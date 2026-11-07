package com.exiledradio.updateaquaticbackport.trident.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import javax.vecmath.Matrix4f;
import java.util.Collections;
import java.util.List;

/**
 * The trident item model: the flat icon in inventories, on the ground and in item frames, the 3D model
 * everywhere else, as 1.14.4's item renderer chooses between {@code trident} and {@code trident_in_hand}.
 * While being thrown it switches to {@code trident_throwing}, which points the spear up for a raised arm.
 */
final class TridentBakedModel implements IBakedModel {

    private final IBakedModel flat;
    private final IBakedModel held;
    private final ItemOverrideList overrides;

    TridentBakedModel(IBakedModel flat, IBakedModel inHand, @Nullable final IBakedModel throwing) {
        this.flat = flat;
        this.held = inHand;
        final TridentBakedModel throwingModel = throwing == null ? null : new TridentBakedModel(flat, throwing);
        this.overrides = new ItemOverrideList(Collections.emptyList()) {
            @Override
            public IBakedModel handleItemState(IBakedModel original, ItemStack stack, @Nullable World world, @Nullable EntityLivingBase entity) {
                boolean thrown = entity != null && entity.isHandActive() && entity.getActiveItemStack() == stack;
                // A player's arm is not raised by 1.12.2's player model, so the spear stays as held.
                if (thrown && throwingModel != null && (!(entity instanceof EntityPlayer) || TridentPoses.playersRaiseArm)) {
                    return throwingModel;
                }
                return original;
            }
        };
    }

    private TridentBakedModel(IBakedModel flat, IBakedModel held) {
        this.flat = flat;
        this.held = held;
        this.overrides = ItemOverrideList.NONE;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        return this.flat.getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return this.flat.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.flat.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.flat.getParticleTexture();
    }

    @Override
    @SuppressWarnings("deprecation")
    public ItemCameraTransforms getItemCameraTransforms() {
        return this.flat.getItemCameraTransforms();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return this.overrides;
    }

    @Override
    public Pair<? extends IBakedModel, Matrix4f> handlePerspective(ItemCameraTransforms.TransformType type) {
        if (type == ItemCameraTransforms.TransformType.GUI || type == ItemCameraTransforms.TransformType.GROUND
                || type == ItemCameraTransforms.TransformType.FIXED) {
            return this.flat.handlePerspective(type);
        }
        return this.held.handlePerspective(type);
    }
}
