package com.exiledradio.updateaquaticbackport.blocks;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.List;

/**
 * The stream's smaller event handlers: attacking with the debug stick, washing shulker boxes, wood
 * dropping itself, and wood as fuel.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class BlocksEvents {

    /** A log's furnace time: 1.12.2 gives every wooden item block 300 ticks; wood is a plain item. */
    public static final int WOOD_BURN_TIME = 300;

    private BlocksEvents() {
    }

    /**
     * Attacking a block with the debug stick selects a property: 1.14's
     * {@code canPlayerBreakBlockWhileHolding}, which 1.12.2 lacks. Cancelled on both sides, so the block is
     * never broken (or broken and put back on the client).
     */
    @SubscribeEvent
    public static void leftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        ItemStack held = event.getEntityPlayer().getHeldItemMainhand();
        if (event.getHand() != EnumHand.MAIN_HAND || !(held.getItem() instanceof ItemDebugStick)) {
            return;
        }
        event.setCanceled(true);
        if (!event.getWorld().isRemote) {
            ((ItemDebugStick) held.getItem()).handleClick(event.getEntityPlayer(), event.getWorld().getBlockState(event.getPos()),
                    event.getWorld(), event.getPos(), false, held);
        }
    }

    /**
     * A dyed shulker box used on a cauldron with water comes out undyed, contents and name kept, and the
     * cauldron loses one level - current Java's {@code CauldronInteraction.SHULKER_BOX}, which applies to
     * the sixteen dyed boxes only. Not while sneaking, which uses the item instead (and places the box).
     */
    @SubscribeEvent
    public static void washShulkerBox(PlayerInteractEvent.RightClickBlock event) {
        BlockUndyedShulkerBox undyed = BlocksContent.shulkerBox();
        if (undyed == null || event.getUseBlock() == Event.Result.DENY) {
            return;
        }
        World world = event.getWorld();
        IBlockState state = world.getBlockState(event.getPos());
        EntityPlayer player = event.getEntityPlayer();
        ItemStack held = event.getItemStack();
        if (state.getBlock() != Blocks.CAULDRON || player.isSneaking() || !(held.getItem() instanceof ItemBlock)) {
            return;
        }
        Block box = ((ItemBlock) held.getItem()).getBlock();
        int level = state.getValue(BlockCauldron.LEVEL);
        if (!(box instanceof BlockShulkerBox) || box == undyed || level <= 0) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }
        ItemStack washed = new ItemStack(undyed);
        if (held.hasTagCompound()) {
            washed.setTagCompound(held.getTagCompound().copy());
        }
        player.setHeldItem(event.getHand(), washed);
        Blocks.CAULDRON.setWaterLevel(world, event.getPos(), state, level - 1);
    }

    /** A bark log (vanilla's {@code axis=none}) is wood: it drops the wood item, Silk Touch or not. */
    @SubscribeEvent
    public static void harvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (!BlocksContent.enabled()) {
            return;
        }
        IBlockState state = event.getState();
        Block block = state.getBlock();
        if ((block != Blocks.LOG && block != Blocks.LOG2) || state.getValue(BlockLog.LOG_AXIS) != BlockLog.EnumAxis.NONE) {
            return;
        }
        BlockPlanks.EnumType type = block == Blocks.LOG ? state.getValue(BlockOldLog.VARIANT) : state.getValue(BlockNewLog.VARIANT);
        ItemWood wood = BlocksContent.wood(type);
        if (wood == null) {
            return;
        }
        Item log = Item.getItemFromBlock(block);
        List<ItemStack> drops = event.getDrops();
        for (int i = 0; i < drops.size(); i++) {
            ItemStack drop = drops.get(i);
            if (drop.getItem() == log) {
                drops.set(i, new ItemStack(wood, drop.getCount()));
            }
        }
    }

    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        if (event.getItemStack().getItem() instanceof ItemWood) {
            event.setBurnTime(WOOD_BURN_TIME);
        }
    }
}
