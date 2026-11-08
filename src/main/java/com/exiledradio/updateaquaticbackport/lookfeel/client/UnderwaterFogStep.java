package com.exiledradio.updateaquaticbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.client.FogPipeline;
import com.exiledradio.updateaquaticbackport.water.client.WaterClient;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.13's underwater visibility as one step of Backport Core's fog pipeline. The fog itself is stream W's
 * ({@link WaterClient#fogColor}, {@link WaterClient#fogDensity}), unchanged; Core runs it on Forge's fog events
 * with every other version's fog steps, oldest version first.
 */
@SideOnly(Side.CLIENT)
public final class UnderwaterFogStep implements FogPipeline.Step {

    public static final String NAME = "underwater visibility";

    private static boolean registered;

    private UnderwaterFogStep() {
    }

    static void register() {
        if (!registered) {
            registered = true;
            FogPipeline.add("1.13", NAME, new UnderwaterFogStep());
        }
    }

    @Override
    public void colors(FogPipeline.Colors colors) {
        WaterClient.fogColor(colors);
    }

    @Override
    public void density(FogPipeline.Density density) {
        WaterClient.fogDensity(density);
    }
}
