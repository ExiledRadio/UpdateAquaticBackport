package com.exiledradio.updateaquaticbackport.blocks;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemShears;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Shears carve a pumpkin, and mobs that wear a pumpkin wear a carved one.
 *
 * <h2>Carving, from 1.14.4's {@code PumpkinBlock.onBlockActivated}</h2>
 * Right-clicking a pumpkin with shears turns it into a carved pumpkin facing the clicked side (or, clicked
 * on the top or bottom, facing the player), drops 4 pumpkin seeds out of the new face, plays
 * {@code block.pumpkin.carve} and wears the shears by 1. Vanilla's shears and any other {@link ItemShears}.
 * As in 1.14 the pumpkin takes the click before anything else a sneaking player would do, and the client
 * only swings the arm.
 *
 * <h2>Pumpkin heads</h2>
 * 1.12.2's zombies and skeletons put on a pumpkin at Halloween; 1.13's put on a carved one. Vanilla's
 * pumpkin has no face now, so a mob joining the world with one on its head gets the carved pumpkin in
 * its place (its drop chance for the slot is kept). A player's own head is left alone.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class PumpkinCarving {

    private PumpkinCarving() {
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Block carved = BlocksContent.carvedPumpkin();
        if (carved == null || event.getUseItem() == Event.Result.DENY) {
            return;
        }
        ItemStack held = event.getItemStack();
        EntityPlayer player = event.getEntityPlayer();
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (!(held.getItem() instanceof ItemShears) || player.isSpectator() || world.getBlockState(pos).getBlock() != Blocks.PUMPKIN
                || !player.canPlayerEdit(pos, event.getFace(), held)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(EnumActionResult.SUCCESS);
        if (world.isRemote) {
            return;
        }
        EnumFacing face = event.getFace() == null || event.getFace().getAxis() == EnumFacing.Axis.Y
                ? player.getHorizontalFacing().getOpposite() : event.getFace();
        carve(world, pos, face, player, held, event.getHand());
    }

    /** The carving itself, server side. */
    static void carve(World world, BlockPos pos, EnumFacing face, EntityPlayer player, ItemStack shears,
                      net.minecraft.util.EnumHand hand) {
        Block carved = BlocksContent.carvedPumpkin();
        world.playSound(null, pos, BlocksContent.carveSound(), SoundCategory.BLOCKS, 1.0F, 1.0F);
        world.setBlockState(pos, carved.getDefaultState().withProperty(BlockHorizontal.FACING, face), 11);
        EntityItem seeds = new EntityItem(world, pos.getX() + 0.5D + face.getXOffset() * 0.65D, pos.getY() + 0.1D,
                pos.getZ() + 0.5D + face.getZOffset() * 0.65D, new ItemStack(Items.PUMPKIN_SEEDS, 4));
        seeds.motionX = 0.05D * face.getXOffset() + world.rand.nextDouble() * 0.02D;
        seeds.motionY = 0.05D;
        seeds.motionZ = 0.05D * face.getZOffset() + world.rand.nextDouble() * 0.02D;
        world.spawnEntity(seeds);
        shears.damageItem(1, player);
    }

    @SubscribeEvent
    public static void joinWorld(EntityJoinWorldEvent event) {
        Block carved = BlocksContent.carvedPumpkin();
        if (carved == null || event.getWorld().isRemote || !(event.getEntity() instanceof EntityLiving)) {
            return;
        }
        EntityLiving mob = (EntityLiving) event.getEntity();
        ItemStack head = mob.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
        if (!head.isEmpty() && head.getItem() == Item.getItemFromBlock(Blocks.PUMPKIN)) {
            ItemStack replacement = new ItemStack(carved, head.getCount());
            if (head.hasTagCompound()) {
                replacement.setTagCompound(head.getTagCompound().copy());
            }
            mob.setItemStackToSlot(EntityEquipmentSlot.HEAD, replacement);
        }
    }
}
