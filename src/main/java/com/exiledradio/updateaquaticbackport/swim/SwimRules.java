package com.exiledradio.updateaquaticbackport.swim;

import com.exiledradio.backportcore.pose.PoseRules;
import com.exiledradio.updateaquaticbackport.trident.Riptide;
import com.exiledradio.updateaquaticbackport.trident.TridentContent;
import com.google.common.collect.MapMaker;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;

import java.util.Collections;
import java.util.Set;

/**
 * When a player swims, for Backport Core's pose engine: 1.13's swimming state, and 1.14's crawling.
 *
 * <p>Swimming (1.14.4's {@code Entity.updateSwimming} and {@code PlayerEntity.isSwimming}): a player who is
 * sprinting with their eyes under water starts swimming, and keeps swimming while sprinting anywhere in
 * water. Never while flying, riding or in spectator mode. Sprinting itself starts and stops by 1.13's rules
 * on the player's client ({@code client/SwimClient}); the server follows the sprint flag the client sends,
 * as vanilla does.
 *
 * <p>The state is kept per player object on each side and updated at the end of each tick, before the
 * engine chooses the pose; 1.14 updates it at the start of the next tick, from the same position.
 */
public final class SwimRules implements PoseRules {

    /** Players swimming right now, by identity (a world's client and server copies share an entity id). */
    private static final Set<EntityPlayer> SWIMMING = Collections.newSetFromMap(new MapMaker().weakKeys().<EntityPlayer, Boolean>makeMap());

    @Override
    public boolean enabled() {
        return SwimContent.swimmingOn();
    }

    @Override
    public void tick(EntityPlayer player) {
        boolean can = player.isSprinting() && player.isInWater() && !player.isRiding()
                && !player.capabilities.isFlying && !player.isSpectator();
        boolean now = isSwimming(player) ? can : can && eyesInWater(player);
        if (now) {
            SWIMMING.add(player);
        } else {
            SWIMMING.remove(player);
        }
    }

    @Override
    public boolean isSwimming(EntityPlayer player) {
        return SWIMMING.contains(player);
    }

    @Override
    public boolean isSpinAttacking(EntityPlayer player) {
        return TridentContent.trident() != null && Riptide.isSpinning(player);
    }

    @Override
    public boolean crawls() {
        return SwimContent.crawlingOn();
    }

    /** @return 1.13's swimming state for this player on this side */
    public static boolean swimming(EntityPlayer player) {
        return SWIMMING.contains(player);
    }

    /**
     * @return true if the player's eyes are under water: 1.14's {@code areEyesInFluid(WATER)}, through
     * 1.12.2's own test, which Fluidlogged API extends to water held in other blocks
     */
    public static boolean eyesInWater(EntityPlayer player) {
        return player.isInsideOfMaterial(Material.WATER);
    }

    /** @return 1.14's {@code canSwim}: in water with the eyes under too */
    public static boolean canSwim(EntityPlayer player) {
        return player.isInWater() && eyesInWater(player);
    }
}
