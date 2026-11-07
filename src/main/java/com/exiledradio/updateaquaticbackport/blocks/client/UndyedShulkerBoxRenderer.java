package com.exiledradio.updateaquaticbackport.blocks.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.client.model.ModelShulker;
import net.minecraft.client.renderer.tileentity.TileEntityShulkerBoxRenderer;
import net.minecraft.util.ResourceLocation;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Vanilla's shulker box renderer, with 1.13's undyed texture ({@code entity/shulker/shulker.png}) in place
 * of the purple one. Vanilla's {@code render} picks the texture from a color array and hands it to
 * {@link #bindTexture}; this swaps it there, so the drawing, the lid animation and the breaking
 * overlay all stay vanilla's.
 */
public final class UndyedShulkerBoxRenderer extends TileEntityShulkerBoxRenderer {

    static final ResourceLocation TEXTURE = new ResourceLocation(UpdateAquaticBackport.MOD_ID, "textures/entity/shulker/shulker.png");

    private static final Set<ResourceLocation> DESTROY = new HashSet<ResourceLocation>(Arrays.asList(DESTROY_STAGES));

    public UndyedShulkerBoxRenderer() {
        super(new ModelShulker());
    }

    @Override
    protected void bindTexture(ResourceLocation location) {
        super.bindTexture(DESTROY.contains(location) ? location : TEXTURE);
    }
}
