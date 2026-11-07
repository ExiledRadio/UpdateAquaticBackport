package com.exiledradio.updateaquaticbackport.network;

import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

/**
 * The mod's one network channel.
 *
 * <h2>Packet ids</h2>
 * Each stream owns a block of five ids (see {@code docs/PLAN.md}) and registers its packets here, inside
 * its own block, so two streams can never pick the same number and merges stay mechanical:
 *
 * <pre>
 *   0-4   common          25-29 dolphins      50-54 oceans
 *   5-9   kelp            30-34 phantoms      55-59 bubbles
 *   10-14 coral           35-39 trident       60-64 blocks
 *   15-19 fish            40-44 conduit       65-69 mobs
 *   20-24 turtles         45-49 structures    70-74 water
 *   75-79 swimming        80-84 waterlogging  85-89 woodset
 * </pre>
 *
 * <p>The channel name is short because Forge caps channel names at 20 characters, one fewer than the
 * mod id.
 */
public final class NetworkHandler {

    public static final String CHANNEL = "uabackport";

    public static SimpleNetworkWrapper channel;

    private NetworkHandler() {
    }

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL);
        // Streams register their packets below, in id order.
        channel.registerMessage(com.exiledradio.updateaquaticbackport.mobs.PacketSquidInk.Handler.class,
                com.exiledradio.updateaquaticbackport.mobs.PacketSquidInk.class,
                65, net.minecraftforge.fml.relauncher.Side.CLIENT);
    }
}
