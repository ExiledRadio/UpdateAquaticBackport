package com.exiledradio.updateaquaticbackport.turtle.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.turtle.EntityTurtle;
import com.exiledradio.updateaquaticbackport.turtle.TurtleContent;

import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

/**
 * The turtle's renderer. Subscribed for the client only, so a dedicated server never loads this class or
 * anything it names; item and block models go through {@code common/Content} as every stream's do.
 * {@link ModelRegistryEvent} comes before the render manager is built, as entity renderers need.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class TurtleClient {

    private TurtleClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (TurtleContent.enabled()) {
            RenderingRegistry.registerEntityRenderingHandler(EntityTurtle.class, RenderTurtle::new);
        }
    }
}
