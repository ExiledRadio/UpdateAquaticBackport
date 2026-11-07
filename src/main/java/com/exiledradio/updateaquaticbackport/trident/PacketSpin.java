package com.exiledradio.updateaquaticbackport.trident;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client (packet id 36): an entity's Riptide spin started (ticks left) or ended (0).
 *
 * <p>The handler names no client type; the client work is reached through a no-argument-type call.
 */
public final class PacketSpin implements IMessage {

    int entityId;
    int ticks;

    public PacketSpin() {
    }

    public PacketSpin(int entityId, int ticks) {
        this.entityId = entityId;
        this.ticks = ticks;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.ticks = buf.readByte();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeByte(this.ticks);
    }

    public int entityId() {
        return this.entityId;
    }

    public int ticks() {
        return this.ticks;
    }

    public static final class Handler implements IMessageHandler<PacketSpin, IMessage> {
        @Override
        public IMessage onMessage(PacketSpin message, MessageContext ctx) {
            com.exiledradio.updateaquaticbackport.trident.client.TridentClientPackets.spin(message.entityId, message.ticks);
            return null;
        }
    }
}
