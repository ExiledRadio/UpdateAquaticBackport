package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.PresentationOption;
import com.exiledradio.backportcore.recipe.RecipeUnlocks;
import com.exiledradio.backportcore.recipe.book.SmeltingBook;
import com.exiledradio.backportcore.recipe.book.SmeltingBooks;
import com.exiledradio.backportcore.recipe.book.SmeltingPlacer;
import com.exiledradio.backportcore.recipe.book.SmeltingRecipe;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.lookfeel.LookAndFeel;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.ContainerFurnace;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.InputStream;
import java.util.Collections;

/**
 * Command suggestions and the furnace book as this mod turns them on in Backport Core: 1.13's option on Chat
 * Settings, the furnace book with this mod's own smelting recipes in it (dried kelp, lime dye from sea pickles,
 * charcoal from stripped logs), unlocked by holding them and placed into a real furnace, and the two 1.13.2 textures.
 */
public final class CommandsFurnaceSelfTest implements SelfTest {

    private static final String ID = UpdateAquaticBackport.MOD_ID;

    @Override
    public String name() {
        return "cmdfurnace";
    }

    @Override
    public void run(Check c) throws Exception {
        c.section("command suggestions");
        c.ok(Presentation.commandSuggestions() == LookAndFeel.AUTO_SUGGEST, "1.13 turns command suggestions on with its option");
        c.ok(LookAndFeel.AUTO_SUGGEST.defaultValue(), "Command Suggestions is on by default");
        c.eq("options.autoSuggestCommands", LookAndFeel.AUTO_SUGGEST.labelKey(), "the option's label is vanilla's");
        boolean onChat = false;
        for (PresentationOption o : Presentation.optionsOn(OptionScreen.CHAT)) {
            onChat |= o == LookAndFeel.AUTO_SUGGEST;
        }
        c.ok(onChat, "Command Suggestions shows on Chat Settings");

        c.section("furnace book");
        SmeltingBook book = SmeltingBooks.get(SmeltingBook.FURNACE);
        c.ok(book != null, "the furnace book is registered");
        if (book == null) {
            return;
        }
        Item kelp = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ID, "kelp"));
        Item driedKelp = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ID, "dried_kelp"));
        Item pickle = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ID, "sea_pickle"));
        c.ok(kelp != null && driedKelp != null && pickle != null, "kelp, dried kelp and sea pickles exist");
        if (kelp == null || driedKelp == null || pickle == null) {
            return;
        }
        SmeltingRecipe dried = SmeltingBooks.recipe(book, SmeltingRecipe.idOf(new ItemStack(driedKelp)));
        SmeltingRecipe lime = SmeltingBooks.recipe(book, "minecraft:dye@10");
        SmeltingRecipe charcoal = SmeltingBooks.recipe(book, "minecraft:coal@1");
        c.ok(dried != null && dried.input.apply(new ItemStack(kelp)), "dried kelp is in the book, from kelp");
        c.ok(dried != null && dried.tab == 0, "dried kelp is on the Food tab (1.13: it is food)");
        c.ok(lime != null && lime.input.apply(new ItemStack(pickle)), "lime dye from sea pickles is in the book");
        Item stripped = ForgeRegistries.ITEMS.getValue(new ResourceLocation(ID, "stripped_oak_log"));
        c.ok(charcoal != null && stripped != null && charcoal.input.apply(new ItemStack(stripped)) && charcoal.input.apply(new ItemStack(Blocks.LOG)),
                "one charcoal recipe, from logs and this mod's stripped logs");

        EntityPlayerMP player = Players.real(c.world, "cmdfurnace");
        player.inventory.addItemStackToInventory(new ItemStack(kelp, 5));
        RecipeUnlocks.check(player);
        c.ok(dried != null && SmeltingBooks.data(player, book).isUnlocked(dried.id), "holding kelp unlocks dried kelp");
        c.ok(lime != null && !SmeltingBooks.data(player, book).isUnlocked(lime.id), "and not lime dye");

        BlockPos pos = c.area().up(2);
        c.world.setBlockState(pos, Blocks.FURNACE.getDefaultState());
        TileEntityFurnace furnace = (TileEntityFurnace) c.world.getTileEntity(pos);
        ContainerFurnace container = new ContainerFurnace(player.inventory, furnace);
        player.openContainer = container;
        SmeltingPlacer.Result r = SmeltingBooks.place(player, book, dried, true);
        c.ok(r == SmeltingPlacer.Result.PLACED && furnace.getStackInSlot(0).getItem() == kelp && furnace.getStackInSlot(0).getCount() == 5,
                "shift on dried kelp puts all 5 kelp in the furnace: " + r + " " + furnace.getStackInSlot(0));
        SmeltingBooks.unlock(player, book, Collections.singleton(lime.id));
        r = SmeltingBooks.place(player, book, lime, false);
        c.ok(r == SmeltingPlacer.Result.GHOST && furnace.getStackInSlot(0).isEmpty() && countOf(player, kelp) == 5,
                "lime dye without sea pickles: the kelp goes back and the ghost shows: " + r);
        player.openContainer = player.inventoryContainer;
        c.world.setBlockToAir(pos);

        c.section("1.13.2 textures");
        for (String path : new String[] {"recipe_book.png", "recipe_button.png"}) {
            c.ok(Presentation.newest() != null && hasAsset(path), path + " is shipped");
        }
    }

    private static int countOf(EntityPlayerMP player, Item item) {
        int n = 0;
        for (ItemStack s : player.inventory.mainInventory) {
            if (s.getItem() == item) {
                n += s.getCount();
            }
        }
        return n;
    }

    private static boolean hasAsset(String file) throws java.io.IOException {
        try (InputStream in = CommandsFurnaceSelfTest.class.getResourceAsStream("/assets/" + ID + "/textures/gui/" + file)) {
            return in != null;
        }
    }
}
