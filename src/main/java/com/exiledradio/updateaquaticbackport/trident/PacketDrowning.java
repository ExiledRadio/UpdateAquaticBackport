package com.exiledradio.updateaquaticbackport.trident;

import io.netty.buffer.ByteBuf;

import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client (packet id 35): a zombie or husk started (true) or stopped (false) converting underwater,
 * so the client shakes it as 1.13 does. 1.12.2's zombie has no data parameter for it.
 */
public final class PacketDrowning implements IMessage {

    int entityId;
    boolean converting;

    public PacketDrowning() {
    }

    public PacketDrowning(int entityId, boolean converting) {
        this.entityId = entityId;
        this.converting = converting;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.entityId = buf.readInt();
        this.converting = buf.readBoolean();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.entityId);
        buf.writeBoolean(this.converting);
    }

    public int entityId() {
        return this.entityId;
    }

    public boolean converting() {
        return this.converting;
    }

    public static final class Handler implements IMessageHandler<PacketDrowning, IMessage> {
        @Override
        public IMessage onMessage(PacketDrowning message, MessageContext ctx) {
            com.exiledradio.updateaquaticbackport.trident.client.TridentClientPackets.drowning(message.entityId, message.converting);
            return null;
        }
    }
}
