package com.exiledradio.updateaquaticbackport.trident.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.trident.DrownedConversion;
import com.exiledradio.updateaquaticbackport.trident.EntityDrowned;
import com.exiledradio.updateaquaticbackport.trident.EntityTrident;
import com.exiledradio.updateaquaticbackport.trident.ItemTrident;
import com.exiledradio.updateaquaticbackport.trident.Riptide;
import com.exiledradio.updateaquaticbackport.trident.TridentContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumHandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * The stream's client hooks. FML loads this class only on a client.
 *
 * <ul>
 *   <li>Renderers for the thrown trident and the drowned; the trident item drawn flat in inventories and
 *       3D in hand ({@link TridentBakedModel}, {@link TridentItemRenderer}).</li>
 *   <li>First person: 1.14.4's raised-spear view while a trident is being thrown, and the Riptide spin view.</li>
 *   <li>Third person, through the living render event (no patches): a spinning player turned along their
 *       look and rotating, with the riptide rings added as a layer of the player renderers; a converting
 *       zombie or husk shaking.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class TridentClient {

    private static final Set<RenderLivingBase<?>> LAYERED = Collections.newSetFromMap(new WeakHashMap<RenderLivingBase<?>, Boolean>());
    /** Whether each living render's Pre pushed a matrix its Post must pop. */
    private static final Deque<Boolean> PUSHED = new ArrayDeque<Boolean>();

    private TridentClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        ItemTrident trident = TridentContent.trident();
        if (trident == null) {
            return;
        }
        RenderingRegistry.registerEntityRenderingHandler(EntityTrident.class, RenderTrident::new);
        RenderingRegistry.registerEntityRenderingHandler(EntityDrowned.class, RenderDrowned::new);
        ModelLoader.registerItemVariants(trident, Content.id("trident"), Content.id("trident_in_hand"), Content.id("trident_throwing"));
        trident.setTileEntityItemStackRenderer(new TridentItemRenderer());
    }

    @SubscribeEvent
    public static void bakeModels(ModelBakeEvent event) {
        if (TridentContent.trident() == null) {
            return;
        }
        IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();
        ModelResourceLocation key = new ModelResourceLocation(Content.id("trident"), "inventory");
        IBakedModel flat = registry.getObject(key);
        IBakedModel inHand = registry.getObject(new ModelResourceLocation(Content.id("trident_in_hand"), "inventory"));
        IBakedModel throwing = registry.getObject(new ModelResourceLocation(Content.id("trident_throwing"), "inventory"));
        if (flat != null && inHand != null) {
            registry.putObject(key, new TridentBakedModel(flat, inHand, throwing));
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.getMinecraft().world != null) {
            Riptide.clientTick();
        }
    }

    @SubscribeEvent
    public static void worldUnload(WorldEvent.Unload event) {
        if (event.getWorld().isRemote) {
            TridentClientPackets.DROWNING.clear();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void preRenderLiving(RenderLivingEvent.Pre<?> event) {
        EntityLivingBase entity = event.getEntity();
        if (event.getRenderer() instanceof RenderPlayer && LAYERED.add(event.getRenderer())) {
            ((RenderPlayer) event.getRenderer()).addLayer(new LayerRiptide());
        }
        boolean shake = DrownedConversion.converts(entity) && TridentClientPackets.DROWNING.contains(entity.getEntityId());
        boolean spin = Riptide.isSpinning(entity);
        PUSHED.push(shake || spin);
        if (!shake && !spin) {
            return;
        }
        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();
        float partial = event.getPartialRenderTick();
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        if (shake) {
            // 1.14.4's LivingRenderer: the body's yaw jitters while it converts.
            GlStateManager.rotate((float) (Math.cos(entity.ticksExisted * 3.25D) * Math.PI * 0.4D), 0.0F, 1.0F, 0.0F);
        }
        if (spin) {
            // 1.14.4: after the body's yaw, turn along the look and spin. Done here before the renderer's
            // own yaw turn, so it is wrapped in that turn and its inverse.
            float body = 180.0F - interpolateRotation(entity.prevRenderYawOffset, entity.renderYawOffset, partial);
            GlStateManager.rotate(body, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-90.0F - entity.rotationPitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate((entity.ticksExisted + partial) * -75.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(-body, 0.0F, 1.0F, 0.0F);
        }
        GlStateManager.translate(-x, -y, -z);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void postRenderLiving(RenderLivingEvent.Post<?> event) {
        Boolean pushed = PUSHED.poll();
        if (pushed != null && pushed) {
            GlStateManager.popMatrix();
        }
    }

    private static float interpolateRotation(float prev, float now, float partial) {
        return prev + MathHelper.wrapDegrees(now - prev) * partial;
    }

    /** 1.14.4's first-person trident: raised and drawn back while being thrown; held out during a spin. */
    @SubscribeEvent
    public static void renderHand(RenderSpecificHandEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        ItemStack stack = event.getItemStack();
        if (player == null || stack.isEmpty()) {
            return;
        }
        boolean throwing = stack.getItem() instanceof ItemTrident && player.isHandActive()
                && player.getActiveHand() == event.getHand() && player.getItemInUseCount() > 0;
        boolean spin = !throwing && event.getHand() == EnumHand.MAIN_HAND && Riptide.isSpinning(player);
        if (!throwing && !spin) {
            return;
        }
        event.setCanceled(true);
        EnumHandSide side = event.getHand() == EnumHand.MAIN_HAND ? player.getPrimaryHand() : player.getPrimaryHand().opposite();
        boolean right = side == EnumHandSide.RIGHT;
        int k = right ? 1 : -1;
        float partial = event.getPartialTicks();
        GlStateManager.pushMatrix();
        GlStateManager.translate(k * 0.56F, -0.52F + event.getEquipProgress() * -0.6F, -0.72F);
        if (throwing) {
            GlStateManager.translate(k * -0.5F, 0.7F, 0.1F);
            GlStateManager.rotate(-55.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(k * 35.3F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(k * -9.785F, 0.0F, 0.0F, 1.0F);
            float used = stack.getMaxItemUseDuration() - (player.getItemInUseCount() - partial + 1.0F);
            float pull = Math.min(1.0F, used / 10.0F);
            if (pull > 0.1F) {
                float wobble = MathHelper.sin((used - 0.1F) * 1.3F) * (pull - 0.1F);
                GlStateManager.translate(0.0F, wobble * 0.004F, 0.0F);
            }
            GlStateManager.translate(0.0F, 0.0F, pull * 0.2F);
            GlStateManager.scale(1.0F, 1.0F, 1.0F + pull * 0.2F);
            GlStateManager.rotate(k * 45.0F, 0.0F, -1.0F, 0.0F);
        } else {
            GlStateManager.translate(k * -0.4F, 0.8F, 0.3F);
            GlStateManager.rotate(k * 65.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(k * -85.0F, 0.0F, 0.0F, 1.0F);
        }
        mc.getItemRenderer().renderItemSide(player, stack, right
                ? ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND
                : ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND, !right);
        GlStateManager.popMatrix();
    }
}
