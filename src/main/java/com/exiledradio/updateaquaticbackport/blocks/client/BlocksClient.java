package com.exiledradio.updateaquaticbackport.blocks.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.blocks.BlocksContent;
import com.exiledradio.updateaquaticbackport.blocks.TileEntityUndyedShulkerBox;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderLivingBase;
import net.minecraft.client.renderer.entity.RenderSnowMan;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.layers.LayerSnowmanHead;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import java.util.List;

/**
 * The stream's client side. Loaded only on the client ({@code Side.CLIENT} subscriber), so a dedicated
 * server never sees these types.
 *
 * <ul>
 *   <li>Vanilla's pumpkin drawn without a face (1.13): every state and the item mapped to this mod's
 *       {@code pumpkin} models, which use 1.12.2's own pumpkin textures.</li>
 *   <li>The undyed shulker box's renderer, in the world and in hand.</li>
 *   <li>Snow golems wear a carved pumpkin: vanilla's head layer is swapped for {@link LayerCarvedSnowmanHead}
 *       on the first client tick, once the entity renderers exist. If another mod already replaced
 *       vanilla's layer, there is nothing to swap and that mod's stays.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class BlocksClient {

    /** {@code RenderLivingBase.layerRenderers} */
    private static final String LAYERS = "field_177097_h";

    private static boolean layersDone;

    private BlocksClient() {
    }

    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        if (BlocksContent.carvedPumpkin() == null) {
            return;
        }
        final ModelResourceLocation pumpkin = new ModelResourceLocation(UpdateAquaticBackport.MOD_ID + ":pumpkin", "normal");
        ModelLoader.setCustomStateMapper(Blocks.PUMPKIN, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return pumpkin;
            }
        });
        ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(Blocks.PUMPKIN), 0,
                new ModelResourceLocation(UpdateAquaticBackport.MOD_ID + ":pumpkin", "inventory"));

        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityUndyedShulkerBox.class, new UndyedShulkerBoxRenderer());
        if (BlocksContent.shulkerBoxItem() != null) {
            BlocksContent.shulkerBoxItem().setTileEntityItemStackRenderer(new UndyedShulkerBoxItemRenderer());
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (layersDone || event.phase != TickEvent.Phase.END) {
            return;
        }
        layersDone = true;
        Block carved = BlocksContent.carvedPumpkin();
        if (carved == null) {
            return;
        }
        Render<EntitySnowman> render = Minecraft.getMinecraft().getRenderManager().getEntityClassRenderObject(EntitySnowman.class);
        if (!(render instanceof RenderSnowMan)) {
            return;
        }
        try {
            List<LayerRenderer<EntitySnowman>> layers = ObfuscationReflectionHelper.getPrivateValue(RenderLivingBase.class,
                    (RenderSnowMan) render, LAYERS);
            for (int i = 0; i < layers.size(); i++) {
                if (layers.get(i).getClass() == LayerSnowmanHead.class) {
                    layers.set(i, new LayerCarvedSnowmanHead((RenderSnowMan) render, carved));
                }
            }
        } catch (RuntimeException e) {
            UpdateAquaticBackport.LOGGER.warn("Could not give snow golems a carved pumpkin head; they keep vanilla's.", e);
        }
    }
}
