package com.exiledradio.updateaquaticbackport.water;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Air comes back 4 a tick after surfacing (1.14.4's {@code LivingEntity.determineNextAir}), instead of 1.12.2's
 * instant refill to 300.
 *
 * <h2>How, without a patch</h2>
 * 1.12.2's {@code EntityLivingBase.onEntityUpdate} sets air to 300 on every tick the eyes are out of water.
 * That is the only refill it has, so a jump to exactly 300 from below 296 within one tick is vanilla's
 * refill, and it becomes the value before plus 4.
 * <ul>
 *   <li><b>Players</b>: the air at the player pre-tick is remembered and corrected at the post-tick, in the
 *       same tick. Both sides run it, because the client also refills its own copy every tick and the air
 *       bar reads that copy.</li>
 *   <li><b>Other mobs</b> have no post-tick event. At each living update (the start of their tick, before
 *       vanilla's refill) the air is put back to the remembered value plus 4, so the next tick under water
 *       carries on from there. Between ticks a surfaced mob reads 300, which nothing in vanilla minds.</li>
 * </ul>
 * Fish, squid and dolphins ({@link EntityWaterMob}) keep their own instant refill, as in 1.14.4. Entities
 * whose eyes are in a bubble column are left alone: the column refills their air itself.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class GradualAir {

    /** 1.12.2's fixed maximum air. */
    public static final int MAX_AIR = 300;
    /** 1.14.4's refill per tick. */
    public static final int REFILL = 4;

    /** Air remembered per entity, one map per side, so the two game threads never share one. */
    private static final Map<EntityLivingBase, Integer> SERVER = new WeakHashMap<EntityLivingBase, Integer>();
    private static final Map<EntityLivingBase, Integer> CLIENT = new WeakHashMap<EntityLivingBase, Integer>();

    /** The bubble stream's column, looked up by name once; null when that stream is off. */
    private static Block bubbleColumn;
    private static boolean bubbleColumnLooked;

    private GradualAir() {
    }

    private static boolean on() {
        return WaterContent.enabled() && WaterContent.config().gradualAir;
    }

    private static Map<EntityLivingBase, Integer> map(EntityLivingBase entity) {
        return entity.world.isRemote ? CLIENT : SERVER;
    }

    /** @return true for entities this rule leaves alone */
    static boolean exempt(EntityLivingBase entity) {
        if (entity instanceof EntityWaterMob || !entity.isEntityAlive()) {
            return true;
        }
        if (!bubbleColumnLooked) {
            // Registries are complete before any entity ticks.
            bubbleColumn = Owners.ourBlock("bubble_column");
            bubbleColumnLooked = true;
        }
        Block column = bubbleColumn;
        return column != null
                && entity.world.getBlockState(new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ)).getBlock() == column;
    }

    /**
     * @param before the air at the start of the tick
     * @param after the air vanilla left
     * @return the air 1.13 would have: {@code after}, unless vanilla refilled it at once
     */
    public static int corrected(int before, int after) {
        if (after == MAX_AIR && before < MAX_AIR - REFILL) {
            return before + REFILL;
        }
        return after;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        EntityPlayer player = event.player;
        if (!on()) {
            return;
        }
        Map<EntityLivingBase, Integer> map = map(player);
        if (event.phase == TickEvent.Phase.START) {
            if (player.getAir() < MAX_AIR) {
                map.put(player, player.getAir());
            } else {
                map.remove(player);
            }
            return;
        }
        Integer before = map.remove(player);
        if (before != null && !exempt(player)) {
            player.setAir(corrected(before, player.getAir()));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void livingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (entity instanceof EntityPlayer || event.isCanceled() || !on()) {
            return;
        }
        Map<EntityLivingBase, Integer> map = map(entity);
        Integer before = map.get(entity);
        int air = entity.getAir();
        if (before != null && !exempt(entity)) {
            air = corrected(before, air);
            entity.setAir(air);
        }
        if (air < MAX_AIR) {
            map.put(entity, air);
        } else {
            map.remove(entity);
        }
    }
}
