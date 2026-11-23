package com.exiledradio.updateaquaticbackport.compat;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.common.Loader;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Random;

/**
 * Pillar (Vazkii's, and the Pillar Patched fork; modid {@code pillar}): structures from JSON files, placed by an
 * {@code IWorldGenerator}.
 *
 * <h2>The problem</h2>
 * Pillar tells water from ground by {@code instanceof BlockLiquid}. Its surface rule takes
 * {@code getTopSolidOrLiquidBlock}, which passes through water to the sea floor, and refuses the spot only if the
 * block there is a liquid block. In 1.12.2 that block is water; with this mod's sea floors it is as often seagrass,
 * kelp, coral or a sea pickle, which hold water but are not liquid blocks, so a "surface" structure is built on the
 * sea floor. Its underwater rule has the mirror problem (it wants a liquid block above the floor and finds
 * seagrass), and its above-water rule takes the first liquid block from the top, which under a kelp column that
 * reaches the surface is metres down.
 *
 * <h2>The fix</h2>
 * Each of those three rules is a provider in Pillar's {@code GeneratorType} enum. After every mod has loaded, they
 * are wrapped (a {@link Proxy} of Pillar's interface, so nothing links against Pillar) with the same rule asking
 * "does this hold water?" ({@link Waterlogging#hasWater}, a water-material block or one with water kept in it)
 * where Pillar asks "is this a liquid block?". Structure files, rarity, biome and dimension rules are Pillar's own.
 */
public final class PillarCompat {

    public static final String MODID = "pillar";
    private static final String GENERATOR_TYPE = "vazkii.pillar.schema.GeneratorType";

    private PillarCompat() {
    }

    /** Load complete: Pillar's enum exists from its class load, but nothing places structures before a world. */
    public static void loadComplete() {
        if (!Loader.isModLoaded(MODID)) {
            return;
        }
        try {
            Class<?> type = Class.forName(GENERATOR_TYPE);
            Field provider = type.getDeclaredField("provider");
            provider.setAccessible(true);
            Method inBounds = type.getDeclaredMethod("isInYBounds", Class.forName("vazkii.pillar.schema.StructureSchema"), int.class);
            inBounds.setAccessible(true);
            int wrapped = 0;
            for (Object constant : type.getEnumConstants()) {
                Rule rule = Rule.of(((Enum<?>) constant).name());
                if (rule == null) {
                    continue;
                }
                Object original = provider.get(constant);
                provider.set(constant, Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {provider.getType()},
                        new Handler(rule, original, inBounds)));
                wrapped++;
            }
            UpdateAquaticBackport.LOGGER.info("Pillar's {} water rules now count water-holding blocks as water.", wrapped);
        } catch (Exception | LinkageError e) {
            UpdateAquaticBackport.LOGGER.warn("Could not adjust Pillar's water rules; its surface structures can be "
                    + "built on sea floors covered with seagrass or kelp.", e);
        }
    }

    /** @return true if Pillar should treat this block as water */
    static boolean water(World world, BlockPos pos) {
        IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockLiquid || state.getMaterial() == Material.WATER
                || Waterlogging.hasWater(world, pos);
    }

    private enum Rule {
        SURFACE, UNDERWATER, ABOVE_WATER;

        static Rule of(String name) {
            for (Rule rule : values()) {
                if (rule.name().equals(name)) {
                    return rule;
                }
            }
            return null;
        }
    }

    private static final class Handler implements InvocationHandler {
        private final Rule rule;
        private final Object original;
        private final Method inBounds;

        Handler(Rule rule, Object original, Method inBounds) {
            this.rule = rule;
            this.original = original;
            this.inBounds = inBounds;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (!"getGenerationPosition".equals(method.getName()) || args == null || args.length != 4) {
                if ("equals".equals(method.getName())) {
                    return proxy == args[0];
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                if ("toString".equals(method.getName())) {
                    return "UpdateAquatic water rule " + this.rule + " over " + this.original;
                }
                return call(method, args);
            }
            Object schema = args[0];
            World world = (World) args[2];
            BlockPos pos = (BlockPos) args[3];
            switch (this.rule) {
                case SURFACE: {
                    // Pillar's rule, then refuse the spot if it holds water.
                    BlockPos found = (BlockPos) call(method, args);
                    return found != null && water(world, found) ? null : found;
                }
                case UNDERWATER: {
                    // Pillar's rule (its x == 0 refusal included) with "holds water" above the floor.
                    BlockPos floor = topSolid(world, pos);
                    return floor.getX() != 0 && water(world, floor.up()) ? floor : null;
                }
                default: {
                    // ABOVE_WATER: above the topmost block holding water, then Pillar's y bounds.
                    BlockPos top = topWater(world, pos);
                    return top.getX() != 0 && (Boolean) this.inBounds.invoke(null, schema, top.getY()) ? top : null;
                }
            }
        }

        private Object call(Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(this.original, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    /**
     * Pillar's getTopSolidBlock: down from above the top segment to the first opaque cube that is no leaf or plant;
     * the position above it.
     */
    static BlockPos topSolid(World world, BlockPos pos) {
        Chunk chunk = world.getChunk(pos);
        BlockPos at = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ());
        while (at.getY() >= 0) {
            BlockPos below = at.down();
            IBlockState state = chunk.getBlockState(below);
            if (state.isOpaqueCube() && !state.getBlock().isLeaves(state, world, below)
                    && !state.getBlock().isFoliage(world, below)) {
                break;
            }
            at = below;
        }
        return at;
    }

    /**
     * Pillar's getTopLiquidBlock, asking "holds water" (or is lava) where it asks "is a liquid block": the position
     * above the topmost one.
     */
    static BlockPos topWater(World world, BlockPos pos) {
        Chunk chunk = world.getChunk(pos);
        BlockPos at = new BlockPos(pos.getX(), chunk.getTopFilledSegment() + 16, pos.getZ());
        while (at.getY() >= 0) {
            BlockPos below = at.down();
            if (chunk.getBlockState(below).getBlock() instanceof BlockLiquid || water(world, below)) {
                return at;
            }
            at = below;
        }
        return at;
    }
}
