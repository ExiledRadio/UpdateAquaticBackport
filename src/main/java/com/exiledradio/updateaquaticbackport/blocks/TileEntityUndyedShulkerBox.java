package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntityShulkerBox;

/**
 * The undyed shulker box's tile entity: vanilla's, under its own class so the client can bind a renderer
 * that draws 1.13's undyed texture to it (renderers are chosen by tile entity class). Saved under this
 * mod's id.
 */
public final class TileEntityUndyedShulkerBox extends TileEntityShulkerBox {

    public TileEntityUndyedShulkerBox() {
        super(EnumDyeColor.PURPLE);
    }
}
