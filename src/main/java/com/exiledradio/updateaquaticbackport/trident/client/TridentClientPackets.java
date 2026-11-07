package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.trident.Riptide;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Client side of the stream's two packets. Reached from the packet handlers by calls that name no client
 * type, so a dedicated server never loads this class.
 */
public final class TridentClientPackets {

    /** Ids of zombies and husks the server says are converting underwater: they shake. */
    static final Set<Integer> DROWNING = Collections.synchronizedSet(new HashSet<Integer>());

    private TridentClientPackets() {
    }

    public static void spin(final int entityId, final int ticks) {
        final Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            Entity entity = mc.world == null ? null : mc.world.getEntityByID(entityId);
            if (entity != null) {
                Riptide.clientSet(entity, ticks);
            }
        });
    }

    public static void drowning(int entityId, boolean converting) {
        if (converting) {
            DROWNING.add(entityId);
        } else {
            DROWNING.remove(entityId);
        }
    }
}
