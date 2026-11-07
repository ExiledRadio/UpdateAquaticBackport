package com.exiledradio.updateaquaticbackport.water.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.water.FloatingItems;
import com.exiledradio.updateaquaticbackport.water.GradualAir;
import com.exiledradio.updateaquaticbackport.water.ShallowWater;
import com.exiledradio.updateaquaticbackport.water.WaterColors;
import com.exiledradio.updateaquaticbackport.water.WaterContent;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Biomes;
import net.minecraft.init.MobEffects;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.client.GuiIngameForge;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.InputUpdateEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/**
 * The water stream's client side. FML registers this class on the client only; nothing a server runs
 * names it.
 *
 * <ul>
 *   <li><b>Texture</b>: 1.13's gray water texture in place of 1.12.2's blue one ({@link WaterTexturePack}),
 *       put in before the block atlas is first stitched, while biome colors are on. Forge's water fluid
 *       gets 1.13's default blue as its color, so tanks and pipes that draw it with the fluid's color are
 *       not gray.</li>
 *   <li><b>Fog</b>, 1.14.4's {@code FogRenderer}: under water the fog is exponential-squared (density 0.05,
 *       clearing to 0.02 over 30 seconds under water, 0.005 thicker in swamps), colored by the biome's water
 *       fog color (blending over 5 seconds as the biome changes), darker below y=32 as all fog is, and
 *       brightened the longer the head stays under. Respiration and Water Breathing no longer clear it.</li>
 *   <li><b>Ambience</b>, 1.14.4's {@code UnderwaterAmbientSoundHandler}: sounds on going under and coming
 *       up, the loop, and a 1% chance a tick of an addition (1 in 10 of those rare, 1 in 100 ultra rare).</li>
 *   <li><b>Air bar</b> while air refills above water ({@link GradualAir}): 1.13 shows the bubbles whenever
 *       air is not full, where 1.12.2 shows them only under water.</li>
 *   <li><b>Floating items</b> on the client's own copies ({@link FloatingItems}).</li>
 *   <li><b>Shallow-water jump</b> ({@link ShallowWater}), where 1.12.2 moves the player.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class WaterClient {

    /** 1.14.4's {@code counterInWater}: ticks under water, 0 to 600. */
    private static int counterInWater;
    private static boolean wasEyesInWater;
    private static EntityPlayerSP tracked;
    private static int jumpCooldown;

    private static int lastWaterFogColor = -1;
    private static int waterFogColor = -1;
    private static long waterFogUpdateTime = -1L;

    private WaterClient() {
    }

    // ---------------------------------------------------------------------------------- switches

    private static boolean fogOn() {
        return WaterContent.enabled() && WaterContent.config().underwaterFog;
    }

    static boolean ambienceOn() {
        return WaterContent.enabled() && WaterContent.config().ambience;
    }

    private static boolean airOn() {
        return WaterContent.enabled() && WaterContent.config().gradualAir;
    }

    static boolean eyesInWater(Entity entity) {
        return entity.isInsideOfMaterial(Material.WATER);
    }

    // ---------------------------------------------------------------------------------- texture

    /** Fired during pre-initialization, after resources are first loaded and before the block atlas is stitched. */
    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        if (!WaterColors.active()) {
            return;
        }
        WaterTexturePack pack = new WaterTexturePack();
        try {
            if (!pack.complete()) {
                throw new IllegalStateException("water textures missing from the jar");
            }
            Minecraft mc = Minecraft.getMinecraft();
            // Kept for every later reload (F3+T, a resource pack change), which rebuilds from this list...
            List<IResourcePack> defaults = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "field_110449_ao", "defaultResourcePacks");
            defaults.add(pack);
            // ...and added to the live manager now, since the first reload has already happened.
            IReloadableResourceManager manager = (IReloadableResourceManager) mc.getResourceManager();
            if (manager instanceof SimpleReloadableResourceManager) {
                ((SimpleReloadableResourceManager) manager).reloadResourcePack(pack);
            }
            FluidRegistry.WATER.setColor(0xFF000000 | WaterColors.DEFAULT_WATER);
        } catch (RuntimeException e) {
            UpdateAquaticBackport.LOGGER.error("Could not put 1.13's water texture in place; keeping 1.12.2's water and colors", e);
            WaterColors.textureMissing();
        }
    }

    // ---------------------------------------------------------------------------------- ticks

    @SubscribeEvent
    public static void playerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof EntityPlayerSP)) {
            return;
        }
        EntityPlayerSP player = (EntityPlayerSP) event.player;
        if (player != Minecraft.getMinecraft().player) {
            return;
        }
        if (player != tracked) {
            tracked = player;
            counterInWater = 0;
            wasEyesInWater = false;
            jumpCooldown = 0;
        }
        boolean eyes = eyesInWater(player);
        if (eyes) {
            counterInWater = MathHelper.clamp(counterInWater + (player.isSpectator() ? 10 : 1), 0, 600);
        } else if (counterInWater > 0) {
            counterInWater = MathHelper.clamp(counterInWater - 10, 0, 600);
        }

        boolean heard = eyes && !player.isSpectator();
        if (ambienceOn()) {
            Minecraft mc = Minecraft.getMinecraft();
            if (heard && !wasEyesInWater) {
                player.world.playSound(player.posX, player.posY, player.posZ, WaterContent.UNDERWATER_ENTER, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
                mc.getSoundHandler().playSound(new UnderwaterSound.Loop(player));
            } else if (!heard && wasEyesInWater) {
                player.world.playSound(player.posX, player.posY, player.posZ, WaterContent.UNDERWATER_EXIT, SoundCategory.AMBIENT, 1.0F, 1.0F, false);
            }
            if (heard) {
                float f = player.world.rand.nextFloat();
                SoundEvent addition = f < 1.0E-4F ? WaterContent.UNDERWATER_ADDITIONS_ULTRA_RARE
                        : f < 0.001F ? WaterContent.UNDERWATER_ADDITIONS_RARE
                        : f < 0.01F ? WaterContent.UNDERWATER_ADDITIONS : null;
                if (addition != null) {
                    mc.getSoundHandler().playSound(new UnderwaterSound.Addition(player, addition));
                }
            }
        }
        wasEyesInWater = heard;
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase == TickEvent.Phase.END && mc.world != null && !mc.isGamePaused() && FloatingItems.on()) {
            FloatingItems.applyAll(mc.world);
        }
    }

    /**
     * After the movement keys are read and before the player's jump is handled: a normal jump where 1.13
     * makes one. Vanilla then adds its swim-up boost for the same tick, so the jump gives that back.
     */
    @SubscribeEvent
    public static void input(InputUpdateEvent event) {
        EntityPlayer player = event.getEntityPlayer();
        if (player != Minecraft.getMinecraft().player || !ShallowWater.on()) {
            return;
        }
        if (jumpCooldown > 0) {
            jumpCooldown--;
        }
        if (!event.getMovementInput().jump) {
            jumpCooldown = 0;
            return;
        }
        if (jumpCooldown == 0 && ShallowWater.canJumpNormally(player)) {
            player.jump();
            player.motionY -= ShallowWater.SWIM_UP;
            jumpCooldown = ShallowWater.JUMP_TICKS;
        }
    }

    // ---------------------------------------------------------------------------------- fog

    /** 1.14.4's {@code ClientPlayerEntity.getWaterBrightness}: 0 to 1 over 30 seconds under water. */
    private static float waterBrightness(Entity entity) {
        if (entity != tracked || !eyesInWater(entity)) {
            return 0.0F;
        }
        if (counterInWater >= 600) {
            return 1.0F;
        }
        float a = MathHelper.clamp(counterInWater / 100.0F, 0.0F, 1.0F);
        float b = counterInWater < 100 ? 0.0F : MathHelper.clamp((counterInWater - 100.0F) / 500.0F, 0.0F, 1.0F);
        return a * 0.6F + b * 0.39999998F;
    }

    private static boolean blind(Entity entity) {
        return entity instanceof EntityLivingBase && ((EntityLivingBase) entity).isPotionActive(MobEffects.BLINDNESS);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fogColor(EntityViewRenderEvent.FogColors event) {
        if (!fogOn()) {
            return;
        }
        if (event.getState().getMaterial() != Material.WATER) {
            waterFogUpdateTime = -1L;
            return;
        }
        Entity entity = event.getEntity();
        World world = entity.world;
        float partialTicks = (float) event.getRenderPartialTicks();
        Vec3d eye = ActiveRenderInfo.projectViewFromEntity(entity, partialTicks);

        // The biome's water fog color, blending from the last one over 5 seconds.
        long now = Minecraft.getSystemTime();
        int target = WaterColors.fog(world.getBiome(new BlockPos(eye)));
        if (waterFogUpdateTime < 0L) {
            lastWaterFogColor = target;
            waterFogColor = target;
            waterFogUpdateTime = now;
        }
        float t = MathHelper.clamp((now - waterFogUpdateTime) / 5000.0F, 0.0F, 1.0F);
        float r = lerp(t, waterFogColor >> 16 & 255, lastWaterFogColor >> 16 & 255);
        float g = lerp(t, waterFogColor >> 8 & 255, lastWaterFogColor >> 8 & 255);
        float b = lerp(t, waterFogColor & 255, lastWaterFogColor & 255);
        if (lastWaterFogColor != target) {
            lastWaterFogColor = target;
            waterFogColor = MathHelper.floor(r) << 16 | MathHelper.floor(g) << 8 | MathHelper.floor(b);
            waterFogUpdateTime = now;
        }
        r /= 255.0F;
        g /= 255.0F;
        b /= 255.0F;

        // Darker deeper down: below y=32 in the overworld, as all of vanilla's fog.
        double dark = eye.y * world.provider.getVoidFogYFactor();
        if (blind(entity)) {
            int duration = ((EntityLivingBase) entity).getActivePotionEffect(MobEffects.BLINDNESS).getDuration();
            dark = duration < 20 ? dark * (1.0F - duration / 20.0F) : 0.0D;
        }
        if (dark < 1.0D) {
            dark = Math.max(dark, 0.0D);
            dark *= dark;
            r *= dark;
            g *= dark;
            b *= dark;
        }

        // Clearer the longer the head is under.
        float bright = waterBrightness(entity);
        if (bright > 0.0F) {
            float scale = Math.min(1.0F / r, Math.min(1.0F / g, 1.0F / b));
            if (Float.isInfinite(scale)) {
                scale = Math.nextAfter(scale, 0.0D);
            }
            r = r * (1.0F - bright) + r * scale * bright;
            g = g * (1.0F - bright) + g * scale * bright;
            b = b * (1.0F - bright) + b * scale * bright;
        }

        if (Minecraft.getMinecraft().gameSettings.anaglyph) {
            float ar = (r * 30.0F + g * 59.0F + b * 11.0F) / 100.0F;
            float ag = (r * 30.0F + g * 70.0F) / 100.0F;
            float ab = (r * 30.0F + b * 70.0F) / 100.0F;
            r = ar;
            g = ag;
            b = ab;
        }
        event.setRed(r);
        event.setGreen(g);
        event.setBlue(b);
    }

    private static float lerp(float t, float from, float to) {
        return from + t * (to - from);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void fogDensity(EntityViewRenderEvent.FogDensity event) {
        if (!fogOn() || event.getState().getMaterial() != Material.WATER) {
            return;
        }
        Entity entity = event.getEntity();
        if (blind(entity)) {
            return; // vanilla's blindness fog wins, as in 1.14.4
        }
        // A cancelled event skips vanilla's fog setup, mode included.
        GlStateManager.setFog(GlStateManager.FogMode.EXP2);
        float density;
        if (entity instanceof EntityPlayerSP) {
            float bright = waterBrightness(entity);
            density = 0.05F - bright * bright * 0.03F;
            Biome biome = entity.world.getBiome(new BlockPos(entity));
            if (biome == Biomes.SWAMPLAND || biome == Biomes.MUTATED_SWAMPLAND) {
                density += 0.005F;
            }
        } else {
            density = entity instanceof EntityLivingBase ? 0.05F : 0.1F;
        }
        event.setDensity(density);
        event.setCanceled(true);
    }

    // ---------------------------------------------------------------------------------- air bar

    @SubscribeEvent
    public static void airBar(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.AIR || !airOn()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!(mc.getRenderViewEntity() instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) mc.getRenderViewEntity();
        int air = player.getAir();
        if (player.isInsideOfMaterial(Material.WATER) || air >= GradualAir.MAX_AIR) {
            return; // vanilla drew it, or there is nothing to draw
        }
        mc.getTextureManager().bindTexture(Gui.ICONS);
        GlStateManager.enableBlend();
        int left = event.getResolution().getScaledWidth() / 2 + 91;
        int top = event.getResolution().getScaledHeight() - GuiIngameForge.right_height;
        int full = MathHelper.ceil((air - 2) * 10.0D / 300.0D);
        int partial = MathHelper.ceil(air * 10.0D / 300.0D) - full;
        for (int i = 0; i < full + partial; ++i) {
            mc.ingameGUI.drawTexturedModalRect(left - i * 8 - 9, top, i < full ? 16 : 25, 18, 9, 9);
        }
        GuiIngameForge.right_height += 10;
        GlStateManager.disableBlend();
    }
}
