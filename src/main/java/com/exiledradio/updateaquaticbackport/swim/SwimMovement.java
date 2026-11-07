package com.exiledradio.updateaquaticbackport.swim;

import com.exiledradio.backportcore.pose.PlayerMovement;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.dolphin.DolphinContent;
import com.exiledradio.updateaquaticbackport.phantom.PhantomContent;
import com.google.common.collect.MapMaker;

import git.jbredwards.fluidlogged_api.api.util.FluidloggedUtils;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * How a player moves in water, 1.13's way, through Backport Core's movement hooks (its patch P1 calls these
 * from inside {@code EntityLivingBase.travel}; the defaults there are vanilla 1.12.2's):
 * <ul>
 *   <li>before moving, a swimming player's vertical motion turns toward where they look (1.14.4's
 *       {@code PlayerEntity.travel}): {@code motionY += (lookY - motionY) * k}, with k 0.085 looking more
 *       than a little down and 0.06 otherwise, unless they look up at the surface without jumping;</li>
 *   <li>a sprinting player is slowed by 0.9 a tick instead of 0.8, so swimming is fast (Depth Strider then
 *       works on that as on vanilla's); with Dolphin's Grace the dolphin stream's 0.96 wins, as in 1.14;</li>
 *   <li>water pulls down by 0.005 a tick instead of 0.02 (1.14's gravity / 16), and not at all while
 *       sprinting; Slow Falling makes it 0.01 / 16 while sinking.</li>
 * </ul>
 * Movement runs where the player is simulated, on their own client (and harmlessly on the server, whose
 * copy is put back where the client says each tick).
 */
public final class SwimMovement implements PlayerMovement {

    /** 1.14.4's water slowdown while sprinting. */
    public static final float SPRINT_SLOWDOWN = 0.9F;
    /** 1.14.4's gravity in the air, and with Slow Falling, of which water applies a sixteenth. */
    public static final double GRAVITY = 0.08D;
    public static final double SLOW_GRAVITY = 0.01D;

    /** Whether each player's motion was at or below zero when this tick's travel began (1.14's "falling"). */
    private static final Map<EntityPlayer, Boolean> FALLING = new MapMaker().weakKeys().makeMap();

    private static Field isJumping;

    @Override
    public void beforeTravel(EntityPlayer player) {
        if (SwimRules.swimming(player) && !player.isRiding()) {
            double lookY = player.getLookVec().y;
            double k = lookY < -0.2D ? 0.085D : 0.06D;
            if (lookY <= 0.0D || jumping(player) || fluidAt(player, new BlockPos(player.posX, player.posY + 1.0D - 0.1D, player.posZ))) {
                player.motionY += (lookY - player.motionY) * k;
            }
        }
        FALLING.put(player, player.motionY <= 0.0D);
    }

    @Override
    public float waterSlowDown(EntityPlayer player, float slowDown) {
        if (!player.isSprinting() || graceHandled(player)) {
            return slowDown;
        }
        return SPRINT_SLOWDOWN;
    }

    @Override
    public double waterGravity(EntityPlayer player, double gravity) {
        if (player.isSprinting()) {
            return 0.0D;
        }
        boolean falling = Boolean.TRUE.equals(FALLING.get(player));
        double g = falling && slowFalling(player) ? SLOW_GRAVITY : GRAVITY;
        // 1.14.4: a faller drifting at the slow pull's speed settles at -0.003 (only ever with Slow Falling).
        if (falling && Math.abs(player.motionY - 0.005D) >= 0.003D && Math.abs(player.motionY - g / 16.0D) < 0.003D) {
            return player.motionY + 0.003D;
        }
        return g / 16.0D;
    }

    @Override
    public void beforeClientMove(EntityPlayer player) {
        com.exiledradio.updateaquaticbackport.swim.client.SwimClient.beforeMove(player);
    }

    /** @return true if Dolphin's Grace is on the player: the dolphin stream then sets the slowdown itself */
    static boolean graceHandled(EntityLivingBase entity) {
        Potion grace = DolphinContent.dolphinsGrace();
        return grace != null && entity.isPotionActive(grace);
    }

    static boolean slowFalling(EntityLivingBase entity) {
        Potion potion = PhantomContent.slowFalling();
        return potion != null && entity.isPotionActive(potion);
    }

    /** @return true if any fluid is at the position, held there by Fluidlogged API or as the block itself */
    static boolean fluidAt(EntityPlayer player, BlockPos pos) {
        return FluidloggedUtils.getFluidOrReal(player.world, pos).getMaterial().isLiquid();
    }

    /** @return the player's jump key state as their movement sees it (1.12.2's {@code isJumping}) */
    static boolean jumping(EntityLivingBase entity) {
        try {
            if (isJumping == null) {
                isJumping = ObfuscationReflectionHelper.findField(EntityLivingBase.class, "field_70703_bu");
            }
            return isJumping.getBoolean(entity);
        } catch (ReflectiveOperationException | RuntimeException e) {
            UpdateAquaticBackport.LOGGER.warn("Swimming: cannot read the jump state.", e);
            return false;
        }
    }
}
