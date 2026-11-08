package com.exiledradio.updateaquaticbackport.blocks;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/**
 * One of 1.13's prismarine stairs.
 *
 * <p>Vanilla's {@link BlockStairs} already does everything - corners, upside-down placement, copying its
 * strength, sound and map color from the block it is made of - and only hides its constructor. This
 * exists to open it.
 *
 * <p>Waterloggable, as every 1.13 stair: a water source only (Backport Core's rules, before Fluidlogged API's
 * built-in list, which would also let in lava and flowing water).
 */
public final class BlockPrismarineStairs extends BlockStairs implements Waterloggable {

    BlockPrismarineStairs(IBlockState base) {
        super(base);
        this.useNeighborBrightness = true;
    }
}
