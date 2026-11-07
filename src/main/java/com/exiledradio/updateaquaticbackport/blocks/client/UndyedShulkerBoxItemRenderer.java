package com.exiledradio.updateaquaticbackport.blocks.client;

import com.exiledradio.updateaquaticbackport.blocks.TileEntityUndyedShulkerBox;

import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;

/**
 * Draws the undyed shulker box item the way vanilla draws the dyed ones: its tile entity, through the
 * dispatcher, which picks {@link UndyedShulkerBoxRenderer}. Vanilla's item renderer would draw it purple.
 */
public final class UndyedShulkerBoxItemRenderer extends TileEntityItemStackRenderer {

    private final TileEntityUndyedShulkerBox box = new TileEntityUndyedShulkerBox();

    @Override
    public void renderByItem(ItemStack stack) {
        renderByItem(stack, 1.0F);
    }

    @Override
    public void renderByItem(ItemStack stack, float partialTicks) {
        TileEntityRendererDispatcher.instance.render(this.box, 0.0D, 0.0D, 0.0D, 0.0F, partialTicks);
    }
}
