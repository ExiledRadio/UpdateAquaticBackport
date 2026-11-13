package com.exiledradio.updateaquaticbackport.lookfeel;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.option.OptionScreen;
import com.exiledradio.backportcore.presentation.option.SliderOption;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import net.minecraft.util.ResourceLocation;

/**
 * Stream Lf: 1.13's look and feel, the changes 1.13 made beyond its content. Backport Core owns every piece only
 * one version can have at a time (panorama, splash list, world-loading screen, options, F3 keys, fog) and shows
 * the newest installed version's; this class hands Core 1.13's pieces.
 *
 * <ul>
 *   <li>1.13-pre1: the ocean panorama (1.13.2 jar's six faces) and its {@code panorama_overlay.png}, the gradient
 *       1.13 draws over the panorama in place of 1.12.2's two;</li>
 *   <li>the three splashes 1.13.2's {@code splashes.txt} adds to 1.12.2's (none removed);</li>
 *   <li>18w21a: Allow Snooper is off by default (applied once per instance);</li>
 *   <li>F3+C copies the location as a {@code /tp} command and F3+I the targeted block or entity as a
 *       {@code /setblock} or {@code /summon} command (client side, {@code lookfeel.client.LookAndFeelClient});</li>
 *   <li>the underwater fog (stream W) is one step of Core's fog pipeline, so later versions' fog changes run
 *       after it in version order;</li>
 *   <li>the world-loading screen: 1.13's "Preparing spawn area: N%" comes from Core's table for 1.13;</li>
 *   <li>1.13's Biome Blend (Video Settings, after Entity Shadows, as 1.13.2's list ends): OFF, 3x3 to 15x15,
 *       5x5 by default ({@link #BIOME_BLEND}, applied by {@code water/BiomeBlend}).</li>
 * </ul>
 * Menu music did not change in 1.13 ({@code music.menu} is menu1 to menu4 in 1.12.2 and 1.13.2). Also: vanilla's own
 * text as 1.13.2 words it ({@code vanilla_text/}: "Left Button", "Loading terrain...", "Open World Folder" ...), the
 * underwater music ({@code music.under_water}, Core's in-game music owner) and 1.13's F3 text (client side). The
 * Video Settings edits (no 3D Anaglyph, numbered GUI Scale), the title fade-in and Edit World's backups are Core's
 * own, by version.
 */
public final class LookAndFeel {

    private static final String ID = UpdateAquaticBackport.MOD_ID;

    /**
     * 1.13's Biome Blend (Video Settings): the radius of the square the biome colors are averaged over, 0 to 7,
     * shown as OFF or "5x5" (1.13.2's {@code biomeBlendRadius}, default 2). Its id's path is the name Core's graphics
     * presets set.
     */
    public static final SliderOption BIOME_BLEND = new SliderOption(ID + ":biome_blend_radius", "options.biomeBlendRadius",
            OptionScreen.VIDEO, 0.0, 7.0, 1.0, 2.0);

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
                .panoramaOverlay(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"))
                .addSplashes("Thank you for the fish!", "All rumors are true!", "Truly gone fishing!")
                .gameOptionDefault("snooperEnabled", "false")
                .vanillaText(new ResourceLocation(ID, "vanilla_text"));
        BIOME_BLEND.zeroKey("options.off").valueText(v -> {
            int side = 2 * (int) Math.round(v) + 1;
            return side + "x" + side;
        });
        v113.option(BIOME_BLEND.after("ENTITY_SHADOWS"));
    }

    /** @return Biome Blend's radius, 0 to 7 */
    public static int biomeBlendRadius() {
        return (int) Math.round(BIOME_BLEND.get());
    }
}
