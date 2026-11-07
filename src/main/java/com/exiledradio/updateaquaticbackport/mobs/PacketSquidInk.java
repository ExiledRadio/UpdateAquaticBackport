package com.exiledradio.updateaquaticbackport.mobs;

import io.netty.buffer.ByteBuf;

import net.minecraft.entity.passive.EntitySquid;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * Server to client (packet id 65): a squid squirted ink. Carries what current Java's {@code spawnInk} uses
 * to aim its 30 particles - the squid's position and its body's pitch and yaw - so the client makes them
 * (1.12.2 has no squid ink particle to send).
 */
public final class PacketSquidInk implements IMessage {

    double x;
    double y;
    double z;
    float pitch;
    float yaw;

    public PacketSquidInk() {
    }

    public PacketSquidInk(EntitySquid squid) {
        this.x = squid.posX;
        this.y = squid.posY;
        this.z = squid.posZ;
        this.pitch = squid.prevSquidPitch;
        this.yaw = squid.prevRenderYawOffset;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readDouble();
        this.y = buf.readDouble();
        this.z = buf.readDouble();
        this.pitch = buf.readFloat();
        this.yaw = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(this.x);
        buf.writeDouble(this.y);
        buf.writeDouble(this.z);
        buf.writeFloat(this.pitch);
        buf.writeFloat(this.yaw);
    }

    public static final class Handler implements IMessageHandler<PacketSquidInk, IMessage> {
        @Override
        public IMessage onMessage(PacketSquidInk message, MessageContext ctx) {
            com.exiledradio.updateaquaticbackport.mobs.client.MobsClient.squidInk(message.x, message.y, message.z,
                    message.pitch, message.yaw);
            return null;
        }
    }
}
