package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The carved pumpkin's item: placed as a block, and worn on the head (1.13 moved wearing from the pumpkin
 * to the carved pumpkin), with the pumpkin blur over the screen while worn.
 */
public final class ItemCarvedPumpkin extends ItemBlock {

    ItemCarvedPumpkin(Block block) {
        super(block);
    }

    @Nullable
    @Override
    public EntityEquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EntityEquipmentSlot.HEAD;
    }

    /**
     * Forge draws vanilla's pumpkin blur only for vanilla's pumpkin and asks the item for anything else.
     * Stripped from the dedicated server with the annotation; the drawing is in a client class.
     */
    @Override
    @SideOnly(Side.CLIENT)
    public void renderHelmetOverlay(ItemStack stack, EntityPlayer player, net.minecraft.client.gui.ScaledResolution resolution,
                                    float partialTicks) {
        com.exiledradio.updateaquaticbackport.blocks.client.PumpkinOverlay.draw(resolution);
    }
}
