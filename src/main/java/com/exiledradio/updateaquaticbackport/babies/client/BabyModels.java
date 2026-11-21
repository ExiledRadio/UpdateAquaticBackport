package com.exiledradio.updateaquaticbackport.babies.client;

import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

/**
 * Whether babies draw 26.1's baby models (the redrawn turtle) or this mod's 1.13 one.
 *
 * <p>26.1 belongs to Game Drops Backport, so the new babies come with it: while it is installed they follow its own
 * {@code babies.enabled} and {@code babies.babyModels} settings, read every frame so its config screen applies at once.
 * Without it they show only when Core's look reaches 26.1 (a later 26.x backport).
 */
@SideOnly(Side.CLIENT)
public final class BabyModels {

    private static final String GAME_DROPS = "gamedropsbackport";

    private static Boolean gameDrops;
    private static Object babiesConfig;
    private static Field enabled;
    private static Field babyModels;
    private static boolean configFailed;

    private BabyModels() {
    }

    /** @return true when babies draw 26.1's models */
    public static boolean active() {
        if (gameDrops == null) {
            gameDrops = Loader.isModLoaded(GAME_DROPS);
        }
        if (gameDrops) {
            return gameDropsSays();
        }
        return Presentation.isAtLeast("26.1");
    }

    private static boolean gameDropsSays() {
        if (configFailed) {
            return true;
        }
        try {
            if (babiesConfig == null) {
                Class<?> config = Class.forName("com.exiledradio.gamedropsbackport.ModConfig");
                babiesConfig = config.getField("babies").get(null);
                enabled = babiesConfig.getClass().getField("enabled");
                babyModels = babiesConfig.getClass().getField("babyModels");
            }
            return enabled.getBoolean(babiesConfig) && babyModels.getBoolean(babiesConfig);
        } catch (ReflectiveOperationException | RuntimeException e) {
            // A Game Drops without these settings: its babies are always on, so these are too.
            configFailed = true;
            UpdateAquaticBackport.LOGGER.info("Game Drops Backport has no babies.babyModels setting; 26.1 babies always shown");
            return true;
        }
    }
}
