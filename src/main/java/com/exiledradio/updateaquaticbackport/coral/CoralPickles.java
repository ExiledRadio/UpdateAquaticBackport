package com.exiledradio.updateaquaticbackport.coral;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.pickle.SeaPickleContent;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Random;

/**
 * Bone meal on a lit sea pickle sitting on a living coral block, from 1.14.4's {@code SeaPickleBlock.grow}:
 * the pickle becomes four, and more pickles (one to four each) appear on the coral blocks around it, in a
 * diamond five blocks across, one in six of the places with water over coral.
 *
 * <p>Works on this mod's sea pickle by its property names. Runs early, so the pickle's own bone meal
 * handling ({@code BlockSeaPickle.grow}, which looks for coral by name) never sees these.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class CoralPickles {

    private CoralPickles() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBonemeal(BonemealEvent event) {
        if (!CoralContent.enabled() || event.getResult() != Event.Result.DEFAULT) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState state = event.getBlock();
        Block pickle = SeaPickleContent.seaPickle();
        if (pickle == null || state.getBlock() != pickle || !isLit(world, pos, state) || !isLivingCoralBlock(world.getBlockState(pos.down()))) {
            return;
        }
        if (!world.isRemote) {
            spread(world, pos, state, world.rand);
        }
        event.setResult(Event.Result.ALLOW);
    }

    static boolean isLivingCoralBlock(IBlockState state) {
        return CoralContent.BLOCKS.containsValue(state.getBlock());
    }

    /**
     * @return true if the sea pickle holds water: real water through Backport Core, as both pickles now keep it,
     * or the stored flag older pickles still carry until their chunk converts them
     */
    static boolean isLit(World world, BlockPos pos, IBlockState state) {
        if (com.exiledradio.backportcore.waterlog.Waterlogging.hasWater(world, pos)) {
            return true;
        }
        IProperty<?> wet = property(state, "waterlogged");
        return wet != null && Boolean.TRUE.equals(state.getValue(wet));
    }

    static IProperty<?> property(IBlockState state, String name) {
        for (IProperty<?> p : state.getPropertyKeys()) {
            if (name.equals(p.getName())) {
                return p;
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void spread(World world, BlockPos pos, IBlockState state, Random rand) {
        int width = 1;
        int offset = 0;
        int x0 = pos.getX() - 2;
        int step = 0;
        for (int dx = 0; dx < 5; dx++) {
            for (int dz = 0; dz < width; dz++) {
                int top = 2 + pos.getY() - 1;
                for (int y = top - 2; y < top; y++) {
                    BlockPos at = new BlockPos(x0 + dx, y, pos.getZ() - offset + dz);
                    if (!at.equals(pos) && rand.nextInt(6) == 0 && BlockCoral.isVanillaWaterSource(world, at)
                            && isLivingCoralBlock(world.getBlockState(at.down()))) {
                        IBlockState more = CoralGenerator.seaPickle(rand.nextInt(4) + 1);
                        if (more != null) {
                            // Real water, as the pickle keeps it now; its stored flag is no longer set.
                            com.exiledradio.backportcore.waterlog.Waterlogging.placeWithWater(world, at, more, 3);
                        }
                    }
                }
            }
            if (step < 2) {
                width += 2;
                offset++;
            } else {
                width -= 2;
                offset--;
            }
            step++;
        }
        IProperty pickles = property(state, "pickles");
        if (pickles != null) {
            world.setBlockState(pos, state.withProperty(pickles, Integer.valueOf(4)), 2);
        }
    }
}
