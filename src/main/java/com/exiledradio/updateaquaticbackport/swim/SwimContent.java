package com.exiledradio.updateaquaticbackport.swim;

import com.exiledradio.backportcore.pose.Poses;
import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.network.NetworkHandler;
import com.exiledradio.updateaquaticbackport.trident.TridentContent;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

/**
 * Swimming and crawling (1.13's swimming, 1.14's crawling): the rules, handed to Backport Core's pose engine,
 * which owns the pose, the hitbox, the eyes, sending poses to other players and drawing them.
 *
 * <p>Config {@code swimming}: {@code enabled} and {@code crawling}, both live. A server's own values win on
 * its clients while they are connected (sent at login, packet 75), because the server checks movement
 * against the box these rules give.
 */
public final class SwimContent {

    /** The connected server's switches, on its clients; null when not connected (or the server never said). */
    private static volatile Boolean serverEnabled;
    private static volatile Boolean serverCrawling;

    private SwimContent() {
    }

    /** From the mod's pre-init. */
    public static void preInit(boolean client) {
        Poses.register(new SwimRules());
        Poses.registerMovement(new SwimMovement());
        MinecraftForge.EVENT_BUS.register(new Events());
        if (client) {
            com.exiledradio.updateaquaticbackport.swim.client.SwimClient.init();
        }
    }

    /** @return true while swimming is on (the connected server's switch on a client) */
    public static boolean swimmingOn() {
        Boolean server = serverEnabled;
        return server != null ? server : ModConfig.swimming.enabled;
    }

    /** @return true while players crawl where they cannot stand (needs swimming on) */
    public static boolean crawlingOn() {
        Boolean server = serverCrawling;
        return swimmingOn() && (server != null ? server : ModConfig.swimming.crawling);
    }

    /** Client: what the server said (packet 75). */
    static void receivedServerRules(boolean enabled, boolean crawling) {
        serverEnabled = enabled;
        serverCrawling = crawling;
    }

    /** Client: left the server; our own config counts again. */
    public static void clearServerRules() {
        serverEnabled = null;
        serverCrawling = null;
    }

    static PacketSwimRules ownRules() {
        return new PacketSwimRules(ModConfig.swimming.enabled, ModConfig.swimming.crawling);
    }

    public static final class Events {

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.player instanceof EntityPlayerMP && TridentContent.connected((EntityPlayerMP) event.player)) {
                NetworkHandler.channel.sendTo(ownRules(), (EntityPlayerMP) event.player);
            }
        }

        /** The in-game config screen of a world this game is hosting: its players get the new switches. */
        @SubscribeEvent
        public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
            if (UpdateAquaticBackport.MOD_ID.equals(event.getModID()) && server != null && NetworkHandler.channel != null) {
                server.addScheduledTask(() -> NetworkHandler.channel.sendToAll(ownRules()));
            }
        }
    }
}
