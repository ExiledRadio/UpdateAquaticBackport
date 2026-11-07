package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/** The mod's creative tab: {@code itemGroup.updateaquaticbackport}. */
public final class ModCreativeTab extends CreativeTabs {

    public static final ModCreativeTab TAB = new ModCreativeTab();

    /** Items that can stand for the tab, first one registered wins; switched-off streams have none. */
    private static final String[] ICONS = {"heart_of_the_sea", "trident", "tube_coral_fan", "kelp", "sea_pickle"};

    private ModCreativeTab() {
        super(UpdateAquaticBackport.MOD_ID);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack createIcon() {
        for (String name : ICONS) {
            Item item = ForgeRegistries.ITEMS.getValue(Content.id(name));
            if (item != null) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.PRISMARINE_SHARD);
    }
}
