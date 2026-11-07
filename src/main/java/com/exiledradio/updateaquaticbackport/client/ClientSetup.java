package com.exiledradio.updateaquaticbackport.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;

/**
 * Client-only setup. A dedicated server never loads this class: the mod class reaches it through a
 * no-argument call, which names no client type.
 *
 * <ul>
 *   <li>A model for every item model request in {@link Content#itemModels()} - by exact name, never by
 *       walking the model registry's keys, which some performance mods bake lazily.</li>
 *   <li>One model name per multipart block ({@link Content#multipart}).</li>
 * </ul>
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientSetup());
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        for (Map.Entry<Item, Map<Integer, String>> entry : Content.itemModels().entrySet()) {
            for (Map.Entry<Integer, String> meta : entry.getValue().entrySet()) {
                ModelLoader.setCustomModelResourceLocation(entry.getKey(), meta.getKey(),
                        new ModelResourceLocation(UpdateAquaticBackport.MOD_ID + ":" + meta.getValue(), "inventory"));
            }
        }
        for (final Block block : Content.multipartBlocks()) {
            final ModelResourceLocation one = new ModelResourceLocation(block.getRegistryName(), "multipart");
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return one;
                }
            });
        }
    }
}
