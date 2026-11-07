package com.exiledradio.updateaquaticbackport.swim.client;

import com.exiledradio.backportcore.pose.Pose;
import com.exiledradio.backportcore.pose.Poses;
import com.exiledradio.backportcore.pose.client.PoseClient;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.swim.SwimContent;
import com.exiledradio.updateaquaticbackport.swim.SwimRules;
import com.exiledradio.updateaquaticbackport.trident.TridentContent;
import com.exiledradio.updateaquaticbackport.trident.client.TridentPoses;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.MovementInput;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.lang.reflect.Field;

/**
 * Swimming and crawling on the player's own client, where 1.12.2 moves the player.
 *
 * <ul>
 *   <li><b>Input</b> ({@code InputUpdateEvent}, right after vanilla reads the keys): crawling moves at
 *       sneaking speed (x 0.3) and swimming does not, whether or not sneak is held; 1.14 slows by pose,
 *       1.12.2 by the sneak key.</li>
 *   <li><b>Sprinting</b> (Backport Core's patch P2, after vanilla's own sprint checks and before the player
 *       moves): decided again by 1.14.4's {@code ClientPlayerEntity.livingTick}, from the state the tick
 *       started with. A sprint can start in water with the eyes under, by double tap or the sprint key; any
 *       forward input keeps a swim going under water; bumping a wall does not end a swim; a swim ends when
 *       sprinting stops, on leaving the water, or when hunger runs out away from the ground. Double-tapping
 *       jump does not start or stop creative flight mid-swim. Sneaking in water sinks a little faster.</li>
 *   <li><b>Trident</b>: players raise a trident to throw it, through the library's model hook.</li>
 * </ul>
 */
public final class SwimClient {

    private static final float SNEAK_SPEED = 0.3F;
    private static final float USE_SPEED = 0.2F;

    private static Field sprintToggleTimer;
    private static Field flyToggleTimer;
    private static boolean warned;

    /** The tick's state when vanilla's sprint checks began (captured at the input event). */
    private static boolean captured;
    private static boolean sprintingBefore;
    private static int sprintTimerBefore;
    private static boolean flyingBefore;
    private static int flyTimerBefore;
    private static float forwardInput;

    /** The input as the last tick left it: what 1.14's sprint checks read as "before" (flag1, flag2). */
    private static float lastForward;
    private static boolean lastSneak;

    private SwimClient() {
    }

    public static void init() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new SwimClient());
        if (TridentContent.enabled()) {
            PoseClient.addModelHook((model, player) -> TridentPoses.apply(model, player));
        }
    }

    /** Players raise a trident to throw it once the library's model patch is known to be in. */
    @SubscribeEvent
    public void preRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!TridentPoses.playersRaiseArm && TridentContent.enabled() && PoseClient.modelHooksWork()) {
            TridentPoses.playersRaiseArm = true;
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInput(InputUpdateEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != Minecraft.getMinecraft().player || !Poses.active() || !SwimContent.swimmingOn()) {
            captured = false;
            return;
        }
        MovementInput input = event.getMovementInput();
        if (Poses.get(player) == Pose.SWIMMING) {
            if (!player.isInWater()) {
                if (!input.sneak) {
                    input.moveStrafe = (float) ((double) input.moveStrafe * SNEAK_SPEED);
                    input.moveForward = (float) ((double) input.moveForward * SNEAK_SPEED);
                }
            } else if (input.sneak) {
                input.moveStrafe = (float) ((double) input.moveStrafe / SNEAK_SPEED);
                input.moveForward = (float) ((double) input.moveForward / SNEAK_SPEED);
            }
        }
        captured = true;
        sprintingBefore = player.isSprinting();
        sprintTimerBefore = getInt(sprintToggleTimer(), player);
        flyingBefore = player.capabilities.isFlying;
        flyTimerBefore = getInt(flyToggleTimer(), player);
        forwardInput = input.moveForward;
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        SwimContent.clearServerRules();
    }

    /** Patch P2 (through {@code SwimMovement}): the sprint decided again, the 1.14 way. */
    public static void beforeMove(EntityPlayer p) {
        if (!(p instanceof EntityPlayerSP) || !captured || !SwimContent.swimmingOn()) {
            return;
        }
        captured = false;
        EntityPlayerSP player = (EntityPlayerSP) p;
        Minecraft mc = Minecraft.getMinecraft();
        MovementInput input = player.movementInput;

        boolean handSlowed = player.isHandActive() && !player.isRiding();
        float forward = handSlowed ? forwardInput * USE_SPEED : forwardInput;
        int timer = handSlowed ? 0 : sprintTimerBefore;
        boolean canSwim = SwimRules.canSwim(player);
        boolean inWater = player.isInWater();
        boolean hasFood = (float) player.getFoodStats().getFoodLevel() > 6.0F || player.capabilities.allowFlying;
        boolean blind = player.isPotionActive(MobEffects.BLINDNESS);
        boolean sprintKey = mc.gameSettings.keyBindSprint.isKeyDown();
        boolean wasStrong = canSwim ? lastForward > 1.0E-5F : lastForward >= 0.8F;
        boolean strong = canSwim ? forward > 1.0E-5F : forward >= 0.8F;

        boolean sprint = sprintingBefore;
        if ((player.onGround || canSwim) && !lastSneak && !wasStrong && strong && !sprint && hasFood && !player.isHandActive() && !blind) {
            if (timer <= 0 && !sprintKey) {
                timer = 7;
            } else {
                sprint = true;
            }
        }
        if (!sprint && (!inWater || canSwim) && strong && hasFood && !player.isHandActive() && !blind && sprintKey) {
            sprint = true;
        }
        if (sprint) {
            boolean noForward = !(forward > 1.0E-5F) || !hasFood;
            if (SwimRules.swimming(player)) {
                if (!player.onGround && !input.sneak && noForward || !inWater) {
                    sprint = false;
                }
            } else if (noForward || player.collidedHorizontally || inWater && !canSwim) {
                sprint = false;
            }
        }
        if (player.isSprinting() != sprint) {
            player.setSprinting(sprint);
        }
        setInt(sprintToggleTimer(), player, timer);

        // 1.14 never toggles creative flight while swimming; the second tap leaves the timer as it was.
        if (SwimRules.swimming(player) && player.capabilities.isFlying != flyingBefore && !mc.playerController.isSpectatorMode()) {
            player.capabilities.isFlying = flyingBefore;
            player.sendPlayerAbilities();
            setInt(flyToggleTimer(), player, flyTimerBefore);
        }

        // 1.14.4's handleFluidSneak: sneaking in water sinks.
        if (inWater && input.sneak) {
            player.motionY += -0.04F * player.getEntityAttribute(EntityLivingBase.SWIM_SPEED).getAttributeValue();
        }

        lastForward = input.moveForward;
        lastSneak = input.sneak;
    }

    private static Field sprintToggleTimer() {
        if (sprintToggleTimer == null) {
            sprintToggleTimer = find(EntityPlayerSP.class, "field_71156_d");
        }
        return sprintToggleTimer;
    }

    private static Field flyToggleTimer() {
        if (flyToggleTimer == null) {
            flyToggleTimer = find(EntityPlayer.class, "field_71101_bC");
        }
        return flyToggleTimer;
    }

    private static Field find(Class<?> owner, String srg) {
        try {
            return ObfuscationReflectionHelper.findField(owner, srg);
        } catch (RuntimeException e) {
            warn(e);
            return null;
        }
    }

    private static int getInt(Field field, Object target) {
        try {
            return field == null ? 0 : field.getInt(target);
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
            return 0;
        }
    }

    private static void setInt(Field field, Object target, int value) {
        try {
            if (field != null) {
                field.setInt(target, value);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            warn(e);
        }
    }

    private static void warn(Exception e) {
        if (!warned) {
            warned = true;
            UpdateAquaticBackport.LOGGER.warn("Swimming: a sprint timer could not be reached; sprint starts may differ.", e);
        }
    }
}
