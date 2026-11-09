package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The water model used when Fluidlogged API is not installed: the block <em>is</em> the water in its space.
 *
 * <p>With Fluidlogged API, kelp, seagrass, coral, coral fans, the conduit and bubble columns are waterlogged
 * ({@link BlockUnderwater}): the water is kept beside the block. Without it, 1.12.2 cannot keep a block and water
 * in one position, so each of these blocks is the water there whenever it is "in water", as this mod did before
 * waterlogging: its material is then {@link Material#WATER}, and it carries vanilla water's
 * {@link BlockLiquid#LEVEL}, always 0 (a still source). That is enough for everything that asks about water to
 * treat it as water: swimming, drowning, underwater fog, squid and fish, water flowing round it and forming
 * sources beside it, a boat floating over it. Vanilla reads {@code LEVEL} from any block whose material is water
 * (boats, buckets, fishing hooks, lily pads, the camera's fog), and would throw on a water block without it.
 *
 * <ul>
 *   <li>{@code LEVEL} is never stored or varied: metadata ignores it and every state has it at 0.</li>
 *   <li>Kelp, seagrass and bubble columns are always water. Coral, coral fans and the conduit have 1.13's
 *       {@link #WATERLOGGED} property, stored in a metadata bit, and are water only when it is true.</li>
 *   <li>{@link #SURFACE} (worked out when drawn, never stored) adds a water surface to the model where nothing
 *       water-like is above, tinted by the biome like water's. The client then reads the blockstate from
 *       {@code blockstates/pseudo_water/}.</li>
 *   <li>Broken, blown up or losing its support, a block that is water leaves still water behind.</li>
 * </ul>
 *
 * <p>Which model a block uses is fixed when it is made: {@link #active()} at construction. Each block then knows
 * from its own state container ({@link #isPseudoWater(Block)}). A world saved without Fluidlogged API keeps the
 * coral's metadata bit, which {@link LegacyWater} reads when Fluidlogged API is installed later.
 */
public final class PseudoWater {

    /** Vanilla water's level, here always 0. */
    public static final PropertyInteger LEVEL = BlockLiquid.LEVEL;

    /** 1.13's waterlogged property, on blocks that can also be dry. */
    public static final PropertyBool WATERLOGGED = PropertyBool.create("waterlogged");

    /** Drawn-only: this block is water and nothing water-like is above it. */
    public static final PropertyBool SURFACE = PropertyBool.create("surface");

    /** Vanilla water's light opacity in 1.12.2. */
    public static final int WATER_OPACITY = 3;

    /** Self-tests: makes blocks constructed on this thread use (or not use) this model. */
    private static final ThreadLocal<Boolean> FORCED = new ThreadLocal<Boolean>();

    private PseudoWater() {
    }

    /** @return true when blocks made now are the water in their space: Fluidlogged API is not installed */
    public static boolean active() {
        Boolean forced = FORCED.get();
        return forced != null ? forced : !Waterlogging.isAvailable();
    }

    /** Self-tests only: blocks constructed on this thread use the model ({@code true}), or not; null to stop. */
    public static void forceForTest(Boolean on) {
        if (on == null) {
            FORCED.remove();
        } else {
            FORCED.set(on);
        }
    }

    /** @return true if this block was made as the water in its space */
    public static boolean isPseudoWater(Block block) {
        return block.getBlockState().getProperties().contains(LEVEL);
    }

    /**
     * Builds a state container: the block's own properties, plus {@link #LEVEL}, {@link #SURFACE} and (for blocks
     * that can be dry) {@link #WATERLOGGED} when {@link #active()}. Called from {@code Block}'s constructor.
     */
    public static BlockStateContainer container(Block block, boolean canBeDry, IProperty<?>... own) {
        if (!active()) {
            return new BlockStateContainer(block, own);
        }
        List<IProperty<?>> all = new ArrayList<IProperty<?>>(Arrays.asList(own));
        all.add(LEVEL);
        all.add(SURFACE);
        if (canBeDry) {
            all.add(WATERLOGGED);
        }
        return new BlockStateContainer(block, all.toArray(new IProperty[0]));
    }

    /** @return the state with LEVEL 0, not at the surface, and waterlogged if it can be dry; unchanged otherwise */
    public static IBlockState base(IBlockState state) {
        if (!state.getPropertyKeys().contains(LEVEL)) {
            return state;
        }
        state = state.withProperty(LEVEL, 0).withProperty(SURFACE, false);
        return state.getPropertyKeys().contains(WATERLOGGED) ? state.withProperty(WATERLOGGED, true) : state;
    }

    /** @return true if this state is water: always, unless it has {@link #WATERLOGGED} false */
    public static boolean isWater(IBlockState state) {
        return !state.getPropertyKeys().contains(WATERLOGGED) || state.getValue(WATERLOGGED);
    }

    /** @return the state in water or dry, if it has {@link #WATERLOGGED}; else unchanged */
    public static IBlockState withWater(IBlockState state, boolean water) {
        return state.getPropertyKeys().contains(WATERLOGGED) ? state.withProperty(WATERLOGGED, water) : state;
    }

    /** @return {@code bit} if the state has {@link #WATERLOGGED} true, else 0 */
    public static int waterloggedBit(IBlockState state, int bit) {
        return state.getPropertyKeys().contains(WATERLOGGED) && state.getValue(WATERLOGGED) ? bit : 0;
    }

    /** @return the state's water from a metadata bit, if it has {@link #WATERLOGGED}; else unchanged */
    public static IBlockState fromBit(IBlockState state, int meta, int bit) {
        return withWater(state, (meta & bit) != 0);
    }

    /** @return water if the state is water, else the block's dry material */
    public static Material material(IBlockState state, Material dry) {
        return isWater(state) ? Material.WATER : dry;
    }

    /** @return water's light opacity if the state is water, else none */
    public static int lightOpacity(IBlockState state) {
        return isWater(state) ? WATER_OPACITY : 0;
    }

    /** @return the state with LEVEL 0 and {@link #SURFACE} worked out from the block above */
    public static IBlockState actualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        boolean surface = isWater(state) && world.getBlockState(pos.up()).getMaterial() != Material.WATER;
        return state.withProperty(LEVEL, 0).withProperty(SURFACE, surface);
    }

    /** @return what is left in this position when the block goes: still water if it was water, else air */
    public static IBlockState leftBehind(IBlockState state) {
        return isWater(state) ? Blocks.WATER.getDefaultState() : Blocks.AIR.getDefaultState();
    }

    /** {@code Block.removedByPlayer}, leaving water behind. */
    public static boolean removedByPlayer(Block block, IBlockState state, World world, BlockPos pos, EntityPlayer player) {
        block.onBlockHarvested(world, pos, state, player);
        return world.setBlockState(pos, leftBehind(state), world.isRemote ? 11 : 3);
    }

    /** {@code Block.onBlockExploded}, leaving water behind. */
    public static void exploded(Block block, World world, BlockPos pos, Explosion explosion) {
        world.setBlockState(pos, leftBehind(world.getBlockState(pos)), 3);
        block.onExplosionDestroy(world, pos, explosion);
    }

    /** Drops the block and leaves water or air, as when its support goes. */
    public static void breakNaturally(World world, BlockPos pos, IBlockState state) {
        state.getBlock().dropBlockAsItem(world, pos, state, 0);
        world.setBlockState(pos, leftBehind(state), 3);
    }
}
