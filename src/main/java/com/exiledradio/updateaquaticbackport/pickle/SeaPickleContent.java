package com.exiledradio.updateaquaticbackport.pickle;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Compat;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;

/**
 * The sea pickle, this mod's own block.
 *
 * <p>Village &amp; Pillage Backport shipped sea pickles first, from 2.3.0, and this mod used to give way
 * to it. They are an ocean block, though, and Backport Core's ownership table now gives them to this
 * mod: when two backports need a block, it belongs to the one it fits best. So the pickle is always
 * registered, whatever else is installed, and Village &amp; Pillage 2.5.0 and later use this one (its
 * wandering trader, one desert house).
 *
 * <h2>Older worlds</h2>
 * A world saved by Village &amp; Pillage 2.3.0 to 2.5.0 holds {@code villagerbackport:sea_pickle} blocks
 * and items. When nothing registers that name any more, {@link #remapOld} points it at this pickle;
 * the states and metadata are the same, so each keeps its count, and the water Fluidlogged API keeps
 * beside it stays. A pickle saved with the old stored {@code waterlogged} flag gets real water once, as
 * its chunk loads (Backport Core's migration, asked for here). Village &amp; Pillage makes the same remap
 * when it is installed; both point the name at this block.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class SeaPickleContent {

    /** What older worlds call it. */
    static final ResourceLocation OLD = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "sea_pickle");

    private static BlockSeaPickle seaPickle;

    private SeaPickleContent() {
    }

    /** @return the sea pickle; registered in every game, so null only before the block registry event */
    @Nullable
    public static Block seaPickle() {
        return seaPickle;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        seaPickle = Content.block(event.getRegistry(), "sea_pickle", new BlockSeaPickle(), null);
        // Worlds saved by Village & Pillage 2.4.0 and earlier stored a pickle's water as a property; each
        // saved chunk moves it into real water once, as it loads.
        Waterlogging.migrateStoredProperty(seaPickle, BlockSeaPickle.WATERLOGGED);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        Item item = Content.registerBlockItem(event.getRegistry(), seaPickle);
        // 1.14's composter takes sea pickles at 65%, the same as a flower.
        Compat.compost(0.65F, item);
    }

    /** Lime dye from sea pickles, as 1.14. 1.12.2 already has lime dye: dye metadata 10. */
    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        FurnaceRecipes.instance().addSmeltingRecipeForBlock(seaPickle, new ItemStack(Items.DYE, 1, 10), 0.1F);
    }

    /** {@code villagerbackport:sea_pickle}, unregistered: this pickle. See the class notes. */
    @SubscribeEvent
    public static void remapOld(RegistryEvent.MissingMappings<Block> event) {
        for (RegistryEvent.MissingMappings.Mapping<Block> mapping : event.getAllMappings()) {
            if (OLD.equals(mapping.key) && seaPickle != null) {
                mapping.remap(seaPickle);
            }
        }
    }

    /** As {@link #remapOld}, for the items. */
    @SubscribeEvent
    public static void remapOldItems(RegistryEvent.MissingMappings<Item> event) {
        Item item = seaPickle == null ? null : Item.getItemFromBlock(seaPickle);
        for (RegistryEvent.MissingMappings.Mapping<Item> mapping : event.getAllMappings()) {
            if (OLD.equals(mapping.key) && item != null && item != Items.AIR) {
                mapping.remap(item);
            }
        }
    }
}
