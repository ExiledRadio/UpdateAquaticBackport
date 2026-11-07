package com.exiledradio.updateaquaticbackport.swim;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client (packet id 75), at login: the server's {@code swimming.enabled} and
 * {@code swimming.crawling}, which its clients follow while connected. The server checks every move against
 * the box these rules give, so a client that swam or crawled when the server did not would be pulled back.
 */
public final class PacketSwimRules implements IMessage {

    boolean enabled;
    boolean crawling;

    public PacketSwimRules() {
    }

    public PacketSwimRules(boolean enabled, boolean crawling) {
        this.enabled = enabled;
        this.crawling = crawling;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int bits = buf.readByte();
        this.enabled = (bits & 1) != 0;
        this.crawling = (bits & 2) != 0;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeByte((this.enabled ? 1 : 0) | (this.crawling ? 2 : 0));
    }

    public boolean enabled() {
        return this.enabled;
    }

    public boolean crawling() {
        return this.crawling;
    }

    public static final class Handler implements IMessageHandler<PacketSwimRules, IMessage> {
        @Override
        public IMessage onMessage(PacketSwimRules message, MessageContext ctx) {
            SwimContent.receivedServerRules(message.enabled, message.crawling);
            return null;
        }
    }
}
