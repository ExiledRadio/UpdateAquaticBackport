package com.exiledradio.updateaquaticbackport.conduit.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.conduit.BlockConduit;
import com.exiledradio.updateaquaticbackport.conduit.ConduitContent;
import com.exiledradio.updateaquaticbackport.conduit.TileEntityConduit;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * The conduit's client side. FML registers this class on the client only, and nothing a server runs
 * names a client type: the tile entity and the potion reach the static methods here with plain
 * arguments, and only on the client.
 *
 * <ul>
 *   <li>The tile entity renderer ({@link RenderConduit}) and the item's, which draws the closed shell
 *       (1.18's item model is {@code builtin/entity} with its display transforms).</li>
 *   <li>The nautilus particle's sprite, stitched into the block atlas, and the particle itself.</li>
 *   <li>Conduit Power's icon in the inventory and on the HUD.</li>
 *   <li><b>Seeing underwater.</b> 1.14 brightens the light map and the fog with Conduit Power while the
 *       eyes are in water, the same way Night Vision does. 1.12.2 asks only whether Night Vision is
 *       active, in the light map update and the fog color, both inside the world render; so for the
 *       length of the world render only, the player holds a Night Vision entry, put into its effect
 *       map before the frame and taken out when the HUD or a screen starts to draw (or the frame ends). No tick and no packet ever
 *       sees it, and a real Night Vision is never touched.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class ConduitClient {

    private static final ResourceLocation NAUTILUS = new ResourceLocation(UpdateAquaticBackport.MOD_ID, "particle/nautilus");
    private static final ResourceLocation EFFECT_ICON = new ResourceLocation(UpdateAquaticBackport.MOD_ID,
            "textures/mob_effect/conduit_power.png");

    /** The borrowed Night Vision entry while it is in the player's map, else null. */
    @Nullable
    private static PotionEffect borrowed;
    @Nullable
    private static EntityPlayerSP borrower;

    private ConduitClient() {
    }

    @SubscribeEvent
    public static void models(ModelRegistryEvent event) {
        BlockConduit conduit = ConduitContent.conduit();
        if (conduit == null) {
            return;
        }
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityConduit.class, new RenderConduit());
        Item.getItemFromBlock(conduit).setTileEntityItemStackRenderer(new TileEntityItemStackRenderer() {
            private final TileEntityConduit shown = new TileEntityConduit();

            @Override
            public void renderByItem(ItemStack stack) {
                TileEntityRendererDispatcher.instance.render(this.shown, 0.0D, 0.0D, 0.0D, 0.0F);
            }
        });
    }

    @SubscribeEvent
    public static void stitch(TextureStitchEvent.Pre event) {
        if (ConduitContent.enabled()) {
            event.getMap().registerSprite(NAUTILUS);
        }
    }

    /** Adds one nautilus particle, as vanilla's particle settings and 32-block limit allow. */
    public static void nautilus(World world, double x, double y, double z, double dx, double dy, double dz) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null || mc.effectRenderer == null || view.getDistanceSq(x, y, z) > 1024.0D) {
            return;
        }
        int setting = mc.gameSettings.particleSetting;
        if (setting == 1 && world.rand.nextInt(3) == 0) {
            setting = 2;
        }
        if (setting > 1) {
            return;
        }
        TextureAtlasSprite sprite = mc.getTextureMapBlocks().getAtlasSprite(NAUTILUS.toString());
        mc.effectRenderer.addEffect(new ParticleNautilus(world, x, y, z, dx, dy, dz, sprite));
    }

    /** Draws Conduit Power's 18x18 icon at a screen position; vanilla rebinds its own sheet after. */
    public static void drawEffectIcon(int x, int y, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager().bindTexture(EFFECT_ICON);
        GlStateManager.enableBlend();
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Gui.drawModalRectWithCustomSizedTexture(x, y, 0, 0, 18, 18, 18, 18);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        mc.getTextureManager().bindTexture(GuiContainer.INVENTORY_BACKGROUND);
    }

    // ------------------------------------------------------------------------------------------
    // Seeing underwater
    // ------------------------------------------------------------------------------------------

    @SubscribeEvent
    public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            lend();
        } else {
            giveBack();
        }
    }

    /** The world is drawn (both passes, in anaglyph 3D); the HUD is about to be. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void hud(RenderGameOverlayEvent.Pre event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) {
            giveBack();
        }
    }

    /** With the HUD hidden (F1), a screen may still draw its effect list. */
    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void screen(GuiScreenEvent.DrawScreenEvent.Pre event) {
        giveBack();
    }

    /** @return true if this player sees underwater with Conduit Power this frame */
    public static boolean seesUnderwater(EntityPlayerSP player) {
        return player.isPotionActive(ConduitContent.CONDUIT_POWER) && !player.isPotionActive(MobEffects.NIGHT_VISION)
                && player.isInsideOfMaterial(Material.WATER);
    }

    private static void lend() {
        giveBack();
        if (!ConduitContent.enabled()) {
            return;
        }
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || !seesUnderwater(player)) {
            return;
        }
        // Over 200 ticks left, vanilla's Night Vision brightness is a steady 1.
        borrowed = new PotionEffect(MobEffects.NIGHT_VISION, 1000, 0, true, false);
        borrower = player;
        player.getActivePotionMap().put(MobEffects.NIGHT_VISION, borrowed);
    }

    private static void giveBack() {
        if (borrowed == null) {
            return;
        }
        Map<Potion, PotionEffect> map = borrower.getActivePotionMap();
        if (map.get(MobEffects.NIGHT_VISION) == borrowed) {
            map.remove(MobEffects.NIGHT_VISION);
        }
        borrowed = null;
        borrower = null;
    }
}
