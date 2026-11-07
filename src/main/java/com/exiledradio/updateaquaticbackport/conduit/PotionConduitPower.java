package com.exiledradio.updateaquaticbackport.conduit;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Conduit Power: 1.14.4's {@code Effects.CONDUIT_POWER}, beneficial, color {@code #1DC2D1}, no attribute
 * modifiers. What it does is in {@link ConduitPowerEvents} (breath and mining speed) and the client's
 * {@code ConduitClient} (seeing underwater as with Night Vision).
 *
 * <p>Its icon is 1.18's {@code mob_effect/conduit_power} texture, drawn by the client class: vanilla's
 * icon sheet has no slot for it. The two drawing methods are client-only and stripped from a dedicated
 * server's copy of this class.
 */
public class PotionConduitPower extends Potion {

    public PotionConduitPower() {
        super(false, 0x1DC2D1);
        this.setPotionName("effect." + UpdateAquaticBackport.MOD_ID + ".conduit_power");
        this.setBeneficial();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderInventoryEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z) {
        com.exiledradio.updateaquaticbackport.conduit.client.ConduitClient.drawEffectIcon(x + 6, y + 7, 1.0F);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderHUDEffect(PotionEffect effect, net.minecraft.client.gui.Gui gui, int x, int y, float z, float alpha) {
        com.exiledradio.updateaquaticbackport.conduit.client.ConduitClient.drawEffectIcon(x + 3, y + 3, alpha);
    }
}
