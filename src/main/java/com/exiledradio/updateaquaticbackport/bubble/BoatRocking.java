package com.exiledradio.updateaquaticbackport.bubble;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 1.13's boats over a bubble column: they rock, and after 60 ticks a whirlpool sinks them and throws
 * their passengers off, an upward column throws them up (2.7 with a player aboard, else 0.6).
 *
 * <h2>From 1.14.4's BoatEntity, kept beside the boat</h2>
 * 1.14.4 keeps a rocking counter in the boat itself. 1.12.2's boat has no room for one, so each side keeps
 * its own table, weakly keyed by the boat: the column's collision {@link #touch}es the boat every tick it
 * sits on top, and the world tick counts down ({@link #tickServer}). A boat that leaves the column before
 * the count ends stops rocking and nothing happens.
 *
 * <p>The client keeps the same count from its own collisions (a client boat runs its block collisions
 * every tick too) only to draw the rocking: 10 degrees, faded in at 0.05 a tick and out at 0.1, as 1.14.4's.
 * Launching and sinking happen on the server alone.
 */
public final class BoatRocking {

    /** 1.14.4: how long a boat rocks before it sinks or is thrown. */
    public static final int ROCKING_TICKS = 60;

    static final class State {
        int ticks;
        boolean downwards;
        boolean touched;
        float intensity;
        float angle;
        float prevAngle;
    }

    private static final Map<EntityBoat, State> SERVER = new WeakHashMap<EntityBoat, State>();
    private static final Map<EntityBoat, State> CLIENT = new WeakHashMap<EntityBoat, State>();

    private BoatRocking() {
    }

    private static Map<EntityBoat, State> table(boolean client) {
        return client ? CLIENT : SERVER;
    }

    /** The column under this boat has air above it this tick. */
    static void touch(EntityBoat boat, boolean downwards) {
        Map<EntityBoat, State> table = table(boat.world.isRemote);
        State state = table.get(boat);
        if (state == null) {
            state = new State();
            table.put(boat, state);
        }
        state.touched = true;
        state.downwards = downwards;
        if (state.ticks == 0) {
            state.ticks = ROCKING_TICKS;
        }
    }

    /** @return the ticks this boat has left to rock (server side), 0 if it is not rocking */
    public static int ticks(EntityBoat boat) {
        State state = table(boat.world.isRemote).get(boat);
        return state == null ? 0 : state.ticks;
    }

    /** One server tick of 1.14.4's {@code updateRocking} for every rocking boat in this world. */
    public static void tickServer(World world) {
        if (SERVER.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<EntityBoat, State>> it = SERVER.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<EntityBoat, State> e = it.next();
            EntityBoat boat = e.getKey();
            if (boat.world != world) {
                continue;
            }
            State state = e.getValue();
            if (boat.isDead || !state.touched) {
                it.remove();
                continue;
            }
            state.touched = false;
            if (--state.ticks <= 0) {
                it.remove();
                if (state.downwards) {
                    boat.motionY -= 0.7;
                    boat.removePassengers();
                } else {
                    boat.motionY = hasPlayer(boat) ? 2.7 : 0.6;
                }
                boat.velocityChanged = true;
            }
        }
    }

    private static boolean hasPlayer(EntityBoat boat) {
        for (Entity passenger : boat.getPassengers()) {
            if (passenger instanceof EntityPlayer) {
                return true;
            }
        }
        return false;
    }

    /** One client tick: the same count, and the angle drawn from it. */
    public static void tickClient(World world) {
        if (CLIENT.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<EntityBoat, State>> it = CLIENT.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<EntityBoat, State> e = it.next();
            EntityBoat boat = e.getKey();
            State state = e.getValue();
            if (boat.world != world || boat.isDead) {
                it.remove();
                continue;
            }
            if (!state.touched) {
                state.ticks = 0;
            } else if (state.ticks > 0) {
                state.ticks--;
            }
            state.touched = false;
            state.intensity = MathHelper.clamp(state.intensity + (state.ticks > 0 ? 0.05F : -0.1F), 0.0F, 1.0F);
            state.prevAngle = state.angle;
            state.angle = 10.0F * MathHelper.sin(0.5F * (float) world.getTotalWorldTime()) * state.intensity;
            if (state.ticks == 0 && state.intensity == 0.0F && state.prevAngle == 0.0F) {
                it.remove();
            }
        }
    }

    /** @return the rocking angle to draw this boat at, in degrees; 0 when it is not rocking */
    public static float angle(EntityBoat boat, float partialTicks) {
        State state = CLIENT.get(boat);
        return state == null ? 0.0F : state.prevAngle + (state.angle - state.prevAngle) * partialTicks;
    }
}
