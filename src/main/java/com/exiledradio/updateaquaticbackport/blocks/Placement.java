package com.exiledradio.updateaquaticbackport.blocks;

import com.google.common.collect.Lists;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.StatList;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.ArrayList;
import java.util.List;

/**
 * Placing an item block the way vanilla's item would, for the cases where vanilla's item would refuse or
 * would do too much. Used by {@link PlacementRules}.
 *
 * <h2>Same steps as vanilla's</h2>
 * A right-click this class takes over is first offered to the clicked block, exactly as
 * {@code PlayerInteractionManager.processRightClickBlock} does (so a chest still opens when clicked with a
 * pumpkin), and the placement then follows {@code ItemBlock.onItemUse} inside the same block-snapshot
 * capture as {@code ForgeHooks.onPlaceItemIntoWorld}: the same {@link BlockEvent.PlaceEvent} for
 * protection mods, the same undo when it is cancelled, the same sound, statistic and advancement trigger.
 * One thing differs, the reason for using this class: the block's own support rule is not asked.
 */
final class Placement {

    private Placement() {
    }

    /** @return the hit position inside the clicked block, as the item use methods take it */
    static float[] hit(PlayerInteractEvent.RightClickBlock event) {
        Vec3d vec = event.getHitVec();
        BlockPos pos = event.getPos();
        if (vec == null) {
            return new float[] {0.5F, 0.5F, 0.5F};
        }
        return new float[] {(float) (vec.x - pos.getX()), (float) (vec.y - pos.getY()), (float) (vec.z - pos.getZ())};
    }

    /** @return where an item block used on this face of this block goes: the block itself if replaceable */
    static BlockPos target(World world, BlockPos clicked, EnumFacing face) {
        return world.getBlockState(clicked).getBlock().isReplaceable(world, clicked) ? clicked : clicked.offset(face);
    }

    /**
     * Vanilla's first step of a right-click on a block: the block may take the click (a chest opens, a
     * door swings) unless the player sneaks with something in hand.
     *
     * @return true if the clicked block used the click, so nothing is placed
     */
    static boolean blockTakesClick(PlayerInteractEvent.RightClickBlock event, float[] hit) {
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (event.getUseBlock() == Event.Result.DENY) {
            return false;
        }
        boolean bypass = player.getHeldItemMainhand().doesSneakBypassUse(world, pos, player)
                && player.getHeldItemOffhand().doesSneakBypassUse(world, pos, player);
        if (player.isSneaking() && !bypass && event.getUseBlock() != Event.Result.ALLOW) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().onBlockActivated(world, pos, state, player, event.getHand(), event.getFace(), hit[0], hit[1], hit[2]);
    }

    /**
     * @return true if the block could go at {@code pos} by everything vanilla checks except the block's own
     * support rule: the space is replaceable, the player may edit it, and no entity is in the way
     */
    static boolean roomFor(World world, BlockPos pos, Block block, EnumFacing face, EntityPlayer player, ItemStack stack) {
        if (!world.getBlockState(pos).getBlock().isReplaceable(world, pos) || !player.canPlayerEdit(pos, face, stack)
                || world.isOutsideBuildHeight(pos)) {
            return false;
        }
        AxisAlignedBB box = block.getDefaultState().getCollisionBoundingBox(world, pos);
        return box == Block.NULL_AABB || world.checkNoEntityCollision(box.offset(pos));
    }

    /**
     * Places the held item block at {@code pos} (server side only).
     *
     * @return SUCCESS whether or not a protection mod refused it, as vanilla's item reports
     */
    static EnumActionResult place(EntityPlayer player, World world, BlockPos pos, EnumFacing face, EnumHand hand, float[] hit) {
        ItemStack stack = player.getHeldItem(hand);
        ItemBlock item = (ItemBlock) stack.getItem();
        Block block = item.getBlock();
        IBlockState state = block.getStateForPlacement(world, pos, face, hit[0], hit[1], hit[2],
                item.getMetadata(stack.getMetadata()), player, hand);

        world.captureBlockSnapshots = true;
        boolean placed;
        try {
            placed = item.placeBlockAt(stack, player, world, pos, face, hit[0], hit[1], hit[2], state);
        } finally {
            world.captureBlockSnapshots = false;
        }
        List<BlockSnapshot> snapshots = new ArrayList<BlockSnapshot>(world.capturedBlockSnapshots);
        world.capturedBlockSnapshots.clear();
        if (!placed || snapshots.isEmpty()) {
            return EnumActionResult.FAIL;
        }

        BlockEvent.PlaceEvent event = snapshots.size() == 1
                ? ForgeEventFactory.onPlayerBlockPlace(player, snapshots.get(0), face, hand)
                : ForgeEventFactory.onPlayerMultiBlockPlace(player, snapshots, face, hand);
        if (event.isCanceled()) {
            for (BlockSnapshot snapshot : Lists.reverse(snapshots)) {
                world.restoringBlockSnapshots = true;
                snapshot.restore(true, false);
                world.restoringBlockSnapshots = false;
            }
            return EnumActionResult.FAIL;
        }
        for (BlockSnapshot snapshot : snapshots) {
            IBlockState now = world.getBlockState(snapshot.getPos());
            if (!now.getBlock().hasTileEntity(now)) {
                now.getBlock().onBlockAdded(world, snapshot.getPos(), now);
            }
            world.markAndNotifyBlock(snapshot.getPos(), null, snapshot.getReplacedBlock(), now, snapshot.getFlag());
        }

        IBlockState now = world.getBlockState(pos);
        SoundType sound = now.getBlock().getSoundType(now, world, pos, player);
        world.playSound(null, pos, sound.getPlaceSound(), SoundCategory.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F,
                sound.getPitch() * 0.8F);
        if (!player.capabilities.isCreativeMode) {
            stack.shrink(1);
        }
        player.addStat(StatList.getObjectUseStats(item));
        return EnumActionResult.SUCCESS;
    }
}
