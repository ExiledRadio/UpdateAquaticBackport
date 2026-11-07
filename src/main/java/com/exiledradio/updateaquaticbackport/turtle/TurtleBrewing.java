package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * One brewing step from one potion type to another, for drinkable, splash and lingering potions alike, as
 * vanilla's own potion mixes work.
 *
 * <p>Forge's ready-made {@code BrewingRecipe} compares stacks without their NBT, so it would take any potion
 * as the input; this checks the potion type.
 */
public final class TurtleBrewing implements IBrewingRecipe {

    private final PotionType from;
    private final Item ingredient;
    private final PotionType to;

    public TurtleBrewing(PotionType from, Item ingredient, PotionType to) {
        this.from = from;
        this.ingredient = ingredient;
        this.to = to;
    }

    @Override
    public boolean isInput(ItemStack input) {
        Item item = input.getItem();
        return (item == Items.POTIONITEM || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION)
                && PotionUtils.getPotionFromItem(input) == this.from;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return !ingredient.isEmpty() && ingredient.getItem() == this.ingredient;
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        return PotionUtils.addPotionToItemStack(new ItemStack(input.getItem()), this.to);
    }
}
