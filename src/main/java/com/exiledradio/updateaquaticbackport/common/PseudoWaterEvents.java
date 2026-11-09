package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Buckets and the blocks that are the water in their space ({@link PseudoWater}, without Fluidlogged API), as
 * 1.13's buckets and waterloggable blocks. With Fluidlogged API none of these blocks is, and buckets are its.
 *
 * <ul>
 *   <li>An empty bucket on a waterlogged coral, fan or conduit takes its water and leaves it dry; on a bubble
 *       column it takes the water and leaves air.</li>
 *   <li>An empty bucket on kelp or seagrass does nothing: they are always water, and 1.13's bucket cannot take
 *       water from them either. Vanilla's bucket would otherwise read them as a water source and delete them.</li>
 *   <li>A water bucket on a dry coral, fan or conduit (or on the face of a block next to one) fills it, where
 *       vanilla's bucket would break it and put water there.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class PseudoWaterEvents {

    private PseudoWaterEvents() {
    }

    /** @return the block at a position if it is the water in its space, or null */
    private static PseudoWaterBlock at(World world, BlockPos pos) {
        Object block = world.getBlockState(pos).getBlock();
        return block instanceof PseudoWaterBlock && ((PseudoWaterBlock) block).isPseudoWater() ? (PseudoWaterBlock) block : null;
    }

    @SubscribeEvent
    public static void onBucket(FillBucketEvent event) {
        RayTraceResult hit = event.getTarget();
        if (hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        World world = event.getWorld();
        ItemStack bucket = event.getEmptyBucket();
        BlockPos pos = hit.getBlockPos();

        if (bucket.getItem() == Items.BUCKET) {
            PseudoWaterBlock block = at(world, pos);
            if (block == null) {
                return;
            }
            IBlockState state = world.getBlockState(pos);
            IBlockState drained = block.isWater(state) ? block.drained(state) : null;
            if (drained == null) {
                event.setCanceled(true);
                return;
            }
            if (!world.isRemote) {
                world.setBlockState(pos, drained, 11);
            }
            event.getEntityPlayer().playSound(SoundEvents.ITEM_BUCKET_FILL, 1.0F, 1.0F);
            event.setFilledBucket(new ItemStack(Items.WATER_BUCKET));
            event.setResult(Event.Result.ALLOW);
        } else if (bucket.getItem() == Items.WATER_BUCKET) {
            for (BlockPos target : new BlockPos[] {pos, pos.offset(hit.sideHit)}) {
                PseudoWaterBlock block = at(world, target);
                if (block == null) {
                    continue;
                }
                IBlockState filled = block.filled(world.getBlockState(target));
                if (filled == null) {
                    if (target.equals(pos)) {
                        // Already water: let vanilla place against the face, as it would on water.
                        continue;
                    }
                    event.setCanceled(true);
                    return;
                }
                if (!event.getEntityPlayer().canPlayerEdit(target, hit.sideHit, bucket)) {
                    return;
                }
                if (!world.isRemote) {
                    world.setBlockState(target, filled, 11);
                }
                world.playSound(event.getEntityPlayer(), target, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 1.0F, 1.0F);
                event.setFilledBucket(new ItemStack(Items.BUCKET));
                event.setResult(Event.Result.ALLOW);
                return;
            }
        }
    }
}
