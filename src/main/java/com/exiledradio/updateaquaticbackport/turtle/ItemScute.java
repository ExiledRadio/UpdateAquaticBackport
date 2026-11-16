package com.exiledradio.updateaquaticbackport.turtle;

import com.exiledradio.backportcore.presentation.Presentation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * The scute. 1.20.5 renamed it "Turtle Scute" (its key became {@code item.minecraft.turtle_scute}) when the
 * armadillo scute came in, so while a 1.20.5-or-newer backport's presentation is installed the item reads its
 * 1.20.5 name ({@code item.updateaquaticbackport.turtle_scute.name}); otherwise 1.13's "Scute". Both keys are in
 * every language file, with Mojang's text for each. Presentation is registered in pre-init on both sides, so the
 * server and the client agree.
 */
public class ItemScute extends Item {

    /** The release that renamed the scute. */
    public static final String RENAMED_IN = "1.20.5";

    @Override
    public String getTranslationKey(ItemStack stack) {
        return Presentation.isAtLeast(RENAMED_IN) ? "item.updateaquaticbackport.turtle_scute" : super.getTranslationKey(stack);
    }
}
