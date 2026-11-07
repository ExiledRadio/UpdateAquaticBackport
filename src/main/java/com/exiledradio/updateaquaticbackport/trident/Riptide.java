package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.network.NetworkHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Riptide's spin attack: 1.14.4's {@code LivingEntity} spin, which 1.12.2's player does not have.
 *
 * <p>For 20 ticks after a Riptide launch the player spins. On the server, the first living entity their
 * path (last tick's box joined with this tick's) touches takes a normal attack with the trident, the
 * spin ends and the player bounces back; running into a wall ends it too. Clients are told when a
 * spin starts and stops, and count it down themselves, so others see the player spin. The spinning
 * pose and the riptide swirl are drawn by the client hook; the player's hitbox does not shrink (that is
 * the swimming stream's pose engine).
 *
 * <p>The state is kept per entity object, so the integrated server's and the client's copies of the
 * same player never share it.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class Riptide {

    /** 1.14.4: a launch spins for 20 ticks. */
    public static final int SPIN_TICKS = 20;

    private static final Map<Entity, Integer> SPIN = Collections.synchronizedMap(new WeakHashMap<Entity, Integer>());
    private static final Map<Entity, AxisAlignedBB> LAST_BOX = Collections.synchronizedMap(new WeakHashMap<Entity, AxisAlignedBB>());

    private Riptide() {
    }

    /** Starts a spin on this side; a server also tells the player and everyone who can see them. */
    public static void start(EntityLivingBase entity, int ticks) {
        SPIN.put(entity, ticks);
        LAST_BOX.put(entity, entity.getEntityBoundingBox());
        if (!entity.world.isRemote) {
            send(entity, ticks);
        }
    }

    /** Ends a spin; a server tells the clients. */
    public static void stop(Entity entity) {
        if (SPIN.remove(entity) != null && !entity.world.isRemote) {
            send(entity, 0);
        }
        LAST_BOX.remove(entity);
    }

    public static boolean isSpinning(Entity entity) {
        return SPIN.containsKey(entity);
    }

    /** @return the ticks left in this entity's spin, 0 if it is not spinning */
    public static int ticksLeft(Entity entity) {
        Integer left = SPIN.get(entity);
        return left == null ? 0 : left;
    }

    /** Client side: a spin the server told us about (0 stops it). */
    public static void clientSet(Entity entity, int ticks) {
        if (ticks > 0) {
            SPIN.put(entity, ticks);
        } else {
            SPIN.remove(entity);
        }
    }

    /** Client side: counts every known spin down by one tick. */
    public static void clientTick() {
        synchronized (SPIN) {
            for (Map.Entry<Entity, Integer> e : SPIN.entrySet()) {
                if (e.getKey().world.isRemote) {
                    e.setValue(e.getValue() - 1);
                }
            }
            SPIN.values().removeIf(left -> left <= 0);
        }
    }

    private static void send(Entity entity, int ticks) {
        PacketSpin packet = new PacketSpin(entity.getEntityId(), ticks);
        NetworkHandler.channel.sendToAllTracking(packet, entity);
        if (entity instanceof EntityPlayerMP && TridentContent.connected((EntityPlayerMP) entity)) {
            NetworkHandler.channel.sendTo(packet, (EntityPlayerMP) entity);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.player.world.isRemote && isSpinning(event.player)) {
            tick(event.player);
        }
    }

    /** One server tick of a spinning player: 1.14.4's {@code updateSpinAttack}. */
    public static void tick(EntityPlayer player) {
        int left = ticksLeft(player) - 1;
        AxisAlignedBB now = player.getEntityBoundingBox();
        AxisAlignedBB before = LAST_BOX.get(player);
        AxisAlignedBB path = before == null ? now : before.union(now);
        LAST_BOX.put(player, now);
        List<Entity> touched = player.world.getEntitiesWithinAABBExcludingEntity(player, path);
        boolean hit = false;
        for (Entity entity : touched) {
            if (entity instanceof EntityLivingBase && entity.isEntityAlive()) {
                player.attackTargetEntityWithCurrentItem(entity);
                player.motionX *= -0.2D;
                player.motionY *= -0.2D;
                player.motionZ *= -0.2D;
                player.velocityChanged = true;
                hit = true;
                break;
            }
        }
        if (hit || player.collidedHorizontally || left <= 0) {
            stop(player);
        } else {
            SPIN.put(player, left);
        }
    }

    /** A player who starts seeing a spinning player is told about the spin. */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        Entity target = event.getTarget();
        if (isSpinning(target) && event.getEntityPlayer() instanceof EntityPlayerMP
                && TridentContent.connected((EntityPlayerMP) event.getEntityPlayer())) {
            NetworkHandler.channel.sendTo(new PacketSpin(target.getEntityId(), ticksLeft(target)), (EntityPlayerMP) event.getEntityPlayer());
        }
    }
}
