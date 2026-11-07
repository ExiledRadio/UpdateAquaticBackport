package com.exiledradio.updateaquaticbackport.blocks;

import com.google.common.base.Predicate;

import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockWorldState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockMaterialMatcher;
import net.minecraft.block.state.pattern.BlockPattern;
import net.minecraft.block.state.pattern.BlockStateMatcher;
import net.minecraft.block.state.pattern.FactoryBlockPattern;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.13's carved pumpkin: what shears make of a pumpkin, and what golems and jack o'lanterns now need.
 *
 * <h2>Vanilla's pumpkin, with the face</h2>
 * 1.12.2's {@link BlockPumpkin} already is 1.13's carved pumpkin in all but name: four facings, the face
 * drawn on the front, golem building in {@code onBlockAdded}. It looks its golem patterns up through
 * protected methods, so this class only answers those with patterns whose head is a carved pumpkin or a
 * jack o'lantern (1.14.4's {@code CarvedPumpkinBlock.IS_PUMPKIN}); the spawning itself is vanilla's.
 *
 * <p>Strength from 1.14.4: gourd material, hardness 1, wood sounds, adobe on maps (the defaults of
 * {@code BlockPumpkin}). Unlike 1.12.2's pumpkin it needs nothing under it (1.13), and it does not tick.
 */
public final class BlockCarvedPumpkin extends BlockPumpkin {

    /** The head of a golem: a carved pumpkin (this block) or a jack o'lantern. */
    private final Predicate<IBlockState> head = new Predicate<IBlockState>() {
        @Override
        public boolean apply(@Nullable IBlockState state) {
            return state != null && (state.getBlock() == BlockCarvedPumpkin.this || state.getBlock() == Blocks.LIT_PUMPKIN);
        }
    };

    private BlockPattern snowman;
    private BlockPattern golem;

    BlockCarvedPumpkin() {
        setHardness(1.0F);
        setSoundType(SoundType.WOOD);
        setTickRandomly(false);
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }

    @Override
    protected BlockPattern getSnowmanPattern() {
        if (this.snowman == null) {
            this.snowman = FactoryBlockPattern.start().aisle("^", "#", "#")
                    .where('^', BlockWorldState.hasState(this.head))
                    .where('#', BlockWorldState.hasState(BlockStateMatcher.forBlock(Blocks.SNOW))).build();
        }
        return this.snowman;
    }

    @Override
    protected BlockPattern getGolemPattern() {
        if (this.golem == null) {
            this.golem = FactoryBlockPattern.start().aisle("~^~", "###", "~#~")
                    .where('^', BlockWorldState.hasState(this.head))
                    .where('#', BlockWorldState.hasState(BlockStateMatcher.forBlock(Blocks.IRON_BLOCK)))
                    .where('~', BlockWorldState.hasState(BlockMaterialMatcher.forMaterial(Material.AIR))).build();
        }
        return this.golem;
    }
}
