package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;

/**
 * One of 1.13's prismarine stairs.
 *
 * <p>Vanilla's {@link BlockStairs} already does everything - corners, upside-down placement, copying its
 * strength, sound and map color from the block it is made of - and only hides its constructor. This
 * exists to open it.
 */
public final class BlockPrismarineStairs extends BlockStairs {

    BlockPrismarineStairs(IBlockState base) {
        super(base);
        this.useNeighborBrightness = true;
    }
}
