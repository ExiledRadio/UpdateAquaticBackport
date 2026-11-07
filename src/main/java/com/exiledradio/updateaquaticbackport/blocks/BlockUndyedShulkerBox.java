package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

/**
 * 1.13's undyed shulker box ({@code shulker_box}): what two shells and a chest make now, and what a
 * cauldron washes a dyed one back into.
 *
 * <h2>A shulker box in every way but the color</h2>
 * It is vanilla's {@link BlockShulkerBox}, so everything that box does - the inventory, opening, keeping
 * its contents when broken, the tooltip, comparators, pistons, dyeing with any dye - comes from vanilla
 * code. 1.12.2's box must have a dye color, and vanilla's code looks colors up in arrays, so this one
 * reports purple, the color 1.12.2's recipe gave; its own tile entity class
 * ({@link TileEntityUndyedShulkerBox}) is what lets the client draw it with 1.13's undyed texture.
 *
 * <p>Strength from 1.12.2's boxes (hardness 2, stone sounds); purple on maps, as 1.14's
 * {@code Material.SHULKER}.
 */
public final class BlockUndyedShulkerBox extends BlockShulkerBox {

    BlockUndyedShulkerBox() {
        super(EnumDyeColor.PURPLE);
        setHardness(2.0F);
        setSoundType(SoundType.STONE);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityUndyedShulkerBox();
    }

    @Override
    @SuppressWarnings("deprecation")
    public MapColor getMapColor(IBlockState state, IBlockAccess world, BlockPos pos) {
        return MapColor.PURPLE;
    }
}
