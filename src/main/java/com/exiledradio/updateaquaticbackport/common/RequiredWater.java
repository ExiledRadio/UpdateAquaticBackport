package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Gives a water source to every {@link Waterloggable.Need#REQUIRED} block placed without one.
 *
 * <p>Today's kelp, seagrass and bubble columns are always water: their water is part of the block, so a
 * command that sets kelp in the air makes a water source there, and kelp put into falling water turns it
 * into a source. Here the water is Fluidlogged API's and is kept only where there was some, so a block placed
 * in the air, or in water that is not a source, is given a source once the tick that placed it is over.
 * Waiting until then matters: while the block is being set, the water it replaces has not been moved into
 * it yet. Placed into a source, as every placement by hand and by generation is, there is nothing to do.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class RequiredWater {

    private static final Map<World, Set<BlockPos>> PENDING = new WeakHashMap<World, Set<BlockPos>>();

    private RequiredWater() {
    }

    /** Checks the block at a position at the end of this world tick. Server thread only. */
    public static void check(World world, BlockPos pos) {
        Set<BlockPos> set = PENDING.get(world);
        if (set == null) {
            set = new LinkedHashSet<BlockPos>();
            PENDING.put(world, set);
        }
        set.add(pos.toImmutable());
    }

    /**
     * Does the waiting checks of a world now.
     *
     * @return how many blocks were given water
     */
    public static int flush(World world) {
        Set<BlockPos> set = PENDING.remove(world);
        if (set == null) {
            return 0;
        }
        int given = 0;
        List<BlockPos> positions = new ArrayList<BlockPos>(set);
        for (BlockPos pos : positions) {
            if (!world.isBlockLoaded(pos)) {
                continue;
            }
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof Waterloggable
                    && ((Waterloggable) state.getBlock()).waterNeed(state) == Waterloggable.Need.REQUIRED
                    && !Waterlogging.hasWaterSource(world, pos) && Waterlogging.setWater(world, pos, true, 3)) {
                given++;
            }
        }
        return given;
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote) {
            flush(event.world);
        }
    }
}
