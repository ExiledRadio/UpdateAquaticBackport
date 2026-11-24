package com.exiledradio.updateaquaticbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream Lf: 1.13's look and feel, the changes 1.13 made beyond its content. Backport Core owns every piece only
 * one version can have at a time (panorama, splash list, world-loading screen, options, F3 keys, fog) and shows
 * the newest installed version's; this class declares 1.13 installed and hands Core what stays this mod's:
 *
 * <ul>
 *   <li>1.13-pre1: the ocean panorama (1.13.2 jar's six faces) and its {@code panorama_overlay.png}, the gradient
 *       1.13 draws over the panorama in place of 1.12.2's two;</li>
 *   <li>the underwater fog (stream W), one step of Core's fog pipeline, and the underwater music
 *       ({@code music.under_water}, Core's in-game music owner), client side ({@code lookfeel.client}).</li>
 * </ul>
 * Since Backport Core 1.4.7, Core itself has 1.13's other pieces, whichever backport is installed: Biome Blend,
 * Command Suggestions, F3 + C and F3 + I, 1.13's F3 text, the furnace's recipe book and its textures, 1.13's splashes,
 * Allow Snooper off by default and vanilla's text as 1.13.2 words it. Their option ids are still this mod's
 * ({@code updateaquaticbackport:biome_blend_radius}), so saved values carry over.
 */
public final class LookAndFeel {

    private static final String ID = UpdateAquaticBackport.MOD_ID;

    private static boolean registered;

    private LookAndFeel() {
    }

    /** Pre-init, both sides. */
    public static void preInit() {
        if (registered) {
            return;
        }
        registered = true;
        Contribution v113 = Presentation.version("1.13");
        v113.panorama(ID + ":textures/gui/title/background/panorama_")
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"));
    }
}
