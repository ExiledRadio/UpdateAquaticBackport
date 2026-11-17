package com.exiledradio.updateaquaticbackport.mechanics.client;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.mechanics.BlockShape;
import com.exiledradio.updateaquaticbackport.mechanics.EntityFloorItemFrame;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.lwjgl.opengl.GL11;

import java.util.List;

/**
 * The mechanics stream's client side: the floor and ceiling item frame's renderer, and 1.13's shaped outline around
 * the block looked at ({@link BlockShape}). The outline is drawn as vanilla's box is (black at 40%, 2 pixels wide, no
 * depth writes, pulled toward the camera as 1.14.4's is); a block {@link BlockShape} has no shape for keeps
 * vanilla's box.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class MechanicsClient {

    private MechanicsClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(ModelRegistryEvent event) {
        RenderingRegistry.registerEntityRenderingHandler(EntityFloorItemFrame.class,
                manager -> new RenderFloorItemFrame(manager, Minecraft.getMinecraft().getRenderItem()));
    }

    @SubscribeEvent
    public static void outline(DrawBlockHighlightEvent event) {
        RayTraceResult hit = event.getTarget();
        if (!ModConfig.mechanics.shapedOutlines || event.getSubID() != 0 || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) {
            return;
        }
        EntityPlayer player = event.getPlayer();
        World world = player.world;
        BlockPos pos = hit.getBlockPos();
        IBlockState state = world.getBlockState(pos);
        if (state.getMaterial() == Material.AIR || !world.getWorldBorder().contains(pos)) {
            return;
        }
        List<double[]> edges;
        try {
            edges = BlockShape.edges(world, pos, state.getActualState(world, pos), player);
        } catch (RuntimeException e) {
            // A modded block whose collision needs more than the world gives here: keep vanilla's box.
            return;
        }
        if (edges == null || edges.isEmpty()) {
            return;
        }
        event.setCanceled(true);
        float partialTicks = event.getPartialTicks();
        double dx = pos.getX() - (player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks);
        double dy = pos.getY() - (player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks);
        double dz = pos.getZ() - (player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        GlStateManager.glLineWidth(2.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.depthMask(false);
        // 1.14.4's drawSelectionBox: the projection scaled to 0.999 in depth, so lines on a block's faces are drawn
        // in front of them (1.12.2 grows its box by 0.002 instead, which would move inner edges off the shape).
        GlStateManager.matrixMode(GL11.GL_PROJECTION);
        GlStateManager.pushMatrix();
        GlStateManager.scale(1.0F, 1.0F, 0.999F);
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        for (double[] e : edges) {
            buffer.pos(dx + e[0], dy + e[1], dz + e[2]).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
            buffer.pos(dx + e[3], dy + e[4], dz + e[5]).color(0.0F, 0.0F, 0.0F, 0.4F).endVertex();
        }
        tessellator.draw();
        GlStateManager.popMatrix();
        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.depthMask(true);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
