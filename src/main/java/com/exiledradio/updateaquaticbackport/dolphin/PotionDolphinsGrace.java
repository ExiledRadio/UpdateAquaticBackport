package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.client.Minecraft;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Dolphin's Grace: water slows its holder far less (see {@link DolphinsGrace}).
 *
 * <p>1.14.4's numbers: beneficial, color 8954814. Its icon is 1.13's own texture, drawn by the client
 * class; the two drawing methods are client-only and stripped from a dedicated server's copy of this
 * class, so their {@link Minecraft} parameter never reaches a server.
 */
public final class PotionDolphinsGrace extends Potion {

    public static final int COLOR = 8954814;

    PotionDolphinsGrace() {
        super(false, COLOR);
        // effect.<modid>.<name>: the family's language tool matches it to effect.minecraft.dolphins_grace.
        this.setPotionName("effect." + UpdateAquaticBackport.MOD_ID + ".dolphins_grace");
        this.setBeneficial();
    }

    /** No index into vanilla's icon sheet: the icon is drawn from its own texture below. */
    @Override
    public boolean hasStatusIcon() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(int x, int y, PotionEffect effect, Minecraft mc) {
        com.exiledradio.updateaquaticbackport.dolphin.client.DolphinClient.drawEffectIcon(x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(int x, int y, PotionEffect effect, Minecraft mc, float alpha) {
        com.exiledradio.updateaquaticbackport.dolphin.client.DolphinClient.drawEffectIcon(x + 3, y + 3, alpha);
    }
}
