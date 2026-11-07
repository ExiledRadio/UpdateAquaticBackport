package com.exiledradio.updateaquaticbackport.blocks;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * 1.13's smaller placement rules, and the plain pumpkin no longer building golems.
 *
 * <ul>
 *   <li><b>Pumpkins, jack o'lanterns and fence gates need no support</b> (1.13 dropped the rule): placed
 *       over air or anything else, where 1.12.2 asks for a solid block below.</li>
 *   <li><b>Doors place on a side face</b>: 1.12.2's door item works only on a block's top; 1.13's door is
 *       placed like any block, against any face, and still stands only on a solid top. A click on a side is
 *       handed to the door item as the click on the top of the block under the door's place.</li>
 *   <li><b>A plain pumpkin builds no golem</b>: 1.13 moved that to the carved pumpkin. Vanilla's pumpkin
 *       builds one in {@code onBlockAdded}, so a player's pumpkin is placed here without it (it is the only
 *       thing that method does for a pumpkin).</li>
 * </ul>
 * The first two follow {@code blocks.placementRules}; the third follows the stream switch. Everything is
 * done at {@link EventPriority#LOW}, so another mod's handler that took the click first wins, and only
 * where vanilla would refuse (or, for the plain pumpkin, build a golem); every other click is vanilla's.
 * Both sides run the same checks: the client cancels the same clicks, so it draws no block the server
 * did not place.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class PlacementRules {

    private PlacementRules() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!BlocksContent.enabled() || event.getUseItem() == Event.Result.DENY || event.getFace() == null) {
            return;
        }
        EntityPlayer player = event.getEntityPlayer();
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || player.isSpectator() || player.getCooldownTracker().hasCooldown(stack.getItem())) {
            return;
        }
        Item item = stack.getItem();
        if (item instanceof ItemDoor) {
            if (ModConfig.blocks.placementRules) {
                door(event, stack);
            }
        } else if (item instanceof ItemBlock) {
            block(event, ((ItemBlock) item).getBlock());
        }
    }

    private static void block(PlayerInteractEvent.RightClickBlock event, Block block) {
        boolean plainPumpkin = block == Blocks.PUMPKIN;
        boolean supportless = block == Blocks.PUMPKIN || block == Blocks.LIT_PUMPKIN || block instanceof BlockFenceGate;
        if (!supportless) {
            return;
        }
        World world = event.getWorld();
        EntityPlayer player = event.getEntityPlayer();
        EnumFacing face = event.getFace();
        BlockPos pos = Placement.target(world, event.getPos(), face);
        if (!Placement.roomFor(world, pos, block, face, player, event.getItemStack())) {
            return;
        }
        boolean supported = block.canPlaceBlockOnSide(world, pos, face);
        if (supported ? !plainPumpkin : !ModConfig.blocks.placementRules) {
            return;
        }
        float[] hit = Placement.hit(event);
        // Ask the block before cancelling: cancelling sets the event's useBlock to DENY.
        boolean taken = Placement.blockTakesClick(event, hit);
        event.setCanceled(true);
        if (taken) {
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }
        event.setCancellationResult(world.isRemote ? EnumActionResult.SUCCESS
                : Placement.place(player, world, pos, face, event.getHand(), hit, !plainPumpkin));
    }

    private static void door(PlayerInteractEvent.RightClickBlock event, ItemStack stack) {
        EnumFacing face = event.getFace();
        if (face == EnumFacing.UP) {
            return;
        }
        World world = event.getWorld();
        EntityPlayer player = event.getEntityPlayer();
        BlockPos pos = Placement.target(world, event.getPos(), face);
        BlockPos below = pos.down();
        // With a replaceable block below, vanilla's door item would sink into it: not the clicked place.
        if (world.getBlockState(below).getBlock().isReplaceable(world, below)) {
            return;
        }
        float[] hit = Placement.hit(event);
        // Ask the block before cancelling: cancelling sets the event's useBlock to DENY.
        boolean taken = Placement.blockTakesClick(event, hit);
        event.setCanceled(true);
        if (taken) {
            event.setCancellationResult(EnumActionResult.SUCCESS);
            return;
        }
        int count = stack.getCount();
        EnumActionResult result = stack.onItemUse(player, world, below, event.getHand(), EnumFacing.UP, hit[0], 1.0F, hit[2]);
        if (player.capabilities.isCreativeMode) {
            // Vanilla gives a creative player the item back after a use; a cancelled click skips that.
            stack.setCount(count);
        }
        event.setCancellationResult(result);
    }
}
