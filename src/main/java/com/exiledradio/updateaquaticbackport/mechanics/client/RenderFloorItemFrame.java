package com.exiledradio.updateaquaticbackport.mechanics.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockRendererDispatcher;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.entity.RenderItemFrame;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.storage.MapData;
import net.minecraftforge.client.event.RenderItemInFrameEvent;
import net.minecraftforge.common.MinecraftForge;

/**
 * 1.12.2's {@code RenderItemFrame}, turned by the frame's pitch as well as its yaw, as 1.14.4's
 * {@code ItemFrameRenderer} is ({@code rotate X by pitch, then Y by 180 - yaw}, about the middle of the frame's
 * block), so a frame facing up lies on the floor and one facing down hangs under a ceiling. The name tag sits just in
 * front of the frame.
 */
public class RenderFloorItemFrame extends RenderItemFrame {

    private static final ResourceLocation MAP_BACKGROUND = new ResourceLocation("textures/map/map_background.png");
    private static final ModelResourceLocation FRAME = new ModelResourceLocation("item_frame", "normal");
    private static final ModelResourceLocation MAP_FRAME = new ModelResourceLocation("item_frame", "map");

    private final Minecraft mc = Minecraft.getMinecraft();
    private final RenderItem items;

    public RenderFloorItemFrame(RenderManager manager, RenderItem items) {
        super(manager, items);
        this.items = items;
    }

    @Override
    public void doRender(EntityItemFrame entity, double x, double y, double z, float entityYaw, float partialTicks) {
        GlStateManager.pushMatrix();
        BlockPos pos = entity.getHangingPosition();
        GlStateManager.translate(pos.getX() - entity.posX + x + 0.5D, pos.getY() - entity.posY + y + 0.5D, pos.getZ() - entity.posZ + z + 0.5D);
        GlStateManager.rotate(entity.rotationPitch, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(180.0F - entity.rotationYaw, 0.0F, 1.0F, 0.0F);
        this.renderManager.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        BlockRendererDispatcher blocks = this.mc.getBlockRendererDispatcher();
        IBakedModel model = blocks.getBlockModelShapes().getModelManager()
                .getModel(entity.getDisplayedItem().getItem() instanceof ItemMap ? MAP_FRAME : FRAME);
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        if (this.renderOutlines) {
            GlStateManager.enableColorMaterial();
            GlStateManager.enableOutlineMode(this.getTeamColor(entity));
        }
        blocks.getBlockModelRenderer().renderModelBrightnessColor(model, 1.0F, 1.0F, 1.0F, 1.0F);
        if (this.renderOutlines) {
            GlStateManager.disableOutlineMode();
            GlStateManager.disableColorMaterial();
        }
        GlStateManager.popMatrix();
        GlStateManager.translate(0.0F, 0.0F, 0.4375F);
        this.renderContents(entity);
        GlStateManager.popMatrix();
        this.renderName(entity, x + entity.facingDirection.getXOffset() * 0.3F,
                y - 0.25D + entity.facingDirection.getYOffset() * 0.3F, z + entity.facingDirection.getZOffset() * 0.3F);
    }

    /** 1.12.2's {@code RenderItemFrame.renderItem} (private there). */
    private void renderContents(EntityItemFrame frame) {
        ItemStack stack = frame.getDisplayedItem();
        if (stack.isEmpty()) {
            return;
        }
        GlStateManager.pushMatrix();
        GlStateManager.disableLighting();
        boolean map = stack.getItem() instanceof ItemMap;
        int turns = map ? frame.getRotation() % 4 * 2 : frame.getRotation();
        GlStateManager.rotate(turns * 360.0F / 8.0F, 0.0F, 0.0F, 1.0F);
        if (!MinecraftForge.EVENT_BUS.post(new RenderItemInFrameEvent(frame, this))) {
            if (map) {
                this.renderManager.renderEngine.bindTexture(MAP_BACKGROUND);
                GlStateManager.rotate(180.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.scale(0.0078125F, 0.0078125F, 0.0078125F);
                GlStateManager.translate(-64.0F, -64.0F, 0.0F);
                MapData data = ((ItemMap) stack.getItem()).getMapData(stack, frame.world);
                GlStateManager.translate(0.0F, 0.0F, -1.0F);
                if (data != null) {
                    this.mc.entityRenderer.getMapItemRenderer().renderMap(data, true);
                }
            } else {
                GlStateManager.scale(0.5F, 0.5F, 0.5F);
                GlStateManager.pushAttrib();
                RenderHelper.enableStandardItemLighting();
                this.items.renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
                RenderHelper.disableStandardItemLighting();
                GlStateManager.popAttrib();
            }
        }
        GlStateManager.enableLighting();
        GlStateManager.popMatrix();
    }
}
