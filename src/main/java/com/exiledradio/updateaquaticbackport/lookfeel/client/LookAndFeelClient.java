package com.exiledradio.updateaquaticbackport.lookfeel.client;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Stream Lf's client side: the underwater fog step ({@link UnderwaterFogStep}) and the underwater music
 * ({@link LookAndFeel113}). F3 + C, F3 + I, 1.13's F3 text and Biome Blend are Backport Core's since Core 1.4.7.
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    private LookAndFeelClient() {
    }

    public static void init() {
        UnderwaterFogStep.register();
        LookAndFeel113.register();
    }
}
