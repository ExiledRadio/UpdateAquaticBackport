package com.exiledradio.updateaquaticbackport.water;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.List;

/**
 * Items and experience orbs float up in water (1.13), by 1.14.4's {@code ItemEntity.applyFloatMotion} and
 * {@code ExperienceOrbEntity.applyFloatMotion}.
 *
 * <h2>How, without a patch</h2>
 * Neither entity has an update event in 1.12.2, so this runs once a tick after every entity has moved: at
 * the end of each server world's tick, and (from {@code client/WaterClient}) at the end of the client's
 * tick, because the client moves dropped items and orbs between the server's updates too.
 *
 * <p>1.14.4, with the eyes in water, replaces the tick's gravity with a small lift. 1.12.2 starts each tick
 * by taking gravity off the motion and then moves. So, for one whose eyes are in water after this tick's
 * move, this adds the lift and gives back the gravity the next tick takes, which leaves next tick's move
 * exactly 1.14.4's:
 * <ul>
 *   <li>item: {@code y + (y < 0.06 ? 0.0005 : 0)}, horizontal x 0.99; gravity 0.04;</li>
 *   <li>orb: {@code min(y + 0.0005, 0.06)}, horizontal x 0.99; gravity 0.03.</li>
 * </ul>
 * The eyes are checked the way 1.12.2 does ({@link Entity#isInsideOfMaterial}), at the same position the
 * next tick starts from.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class FloatingItems {

    static final double ITEM_GRAVITY = 0.03999999910593033D, ORB_GRAVITY = 0.029999999329447746D;
    static final double LIFT = (double) 5.0E-4F, LIFT_LIMIT = (double) 0.06F, DRAG = (double) 0.99F;

    private FloatingItems() {
    }

    public static boolean on() {
        return WaterContent.enabled() && WaterContent.config().floatingItems;
    }

    @SubscribeEvent
    public static void worldTick(TickEvent.WorldTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !event.world.isRemote && on()) {
            applyAll(event.world);
        }
    }

    /** Applies the float to every loaded item and orb of a world. */
    public static void applyAll(World world) {
        List<Entity> entities = world.loadedEntityList;
        for (int i = 0, n = entities.size(); i < n; i++) {
            Entity e = entities.get(i);
            if (e instanceof EntityItem || e instanceof EntityXPOrb) {
                apply(e);
            }
        }
    }

    /** @return true if the entity's motion was changed */
    public static boolean apply(Entity e) {
        // An item keeps its in-water flag up to date after it moves, which makes a cheap first test; an orb
        // never sets the flag in 1.12.2, so it is asked about its eyes straight away.
        if (e.isDead || (e instanceof EntityItem && !e.isInWater()) || !e.isInsideOfMaterial(Material.WATER)) {
            return false;
        }
        boolean gravity = !e.hasNoGravity();
        if (e instanceof EntityItem) {
            e.motionY += (e.motionY < LIFT_LIMIT ? LIFT : 0.0D) + (gravity ? ITEM_GRAVITY : 0.0D);
        } else if (e instanceof EntityXPOrb) {
            e.motionY = Math.min(e.motionY + LIFT, LIFT_LIMIT) + (gravity ? ORB_GRAVITY : 0.0D);
        } else {
            return false;
        }
        e.motionX *= DRAG;
        e.motionZ *= DRAG;
        return true;
    }
}
