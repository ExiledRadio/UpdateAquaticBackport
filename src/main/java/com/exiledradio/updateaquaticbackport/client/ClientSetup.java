package com.exiledradio.updateaquaticbackport.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.PseudoWater;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.color.IBlockColor;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Client-only setup. A dedicated server never loads this class: the mod class reaches it through a
 * no-argument call, which names no client type.
 *
 * <ul>
 *   <li>A model for every item model request in {@link Content#itemModels()} - by exact name, never by
 *       walking the model registry's keys, which some performance mods bake lazily.</li>
 *   <li>One model name per multipart block ({@link Content#multipart}). A block that is the water in its space
 *       ({@link PseudoWater}, without Fluidlogged API) reads its blockstate from {@code blockstates/pseudo_water/},
 *       the same parts plus a water surface.</li>
 *   <li>Without Fluidlogged API, biome water color on tint index 1 of those blocks: their water surface.</li>
 * </ul>
 */
public final class ClientSetup {

    private ClientSetup() {
    }

    public static void init() {
        MinecraftForge.EVENT_BUS.register(new ClientSetup());
        com.exiledradio.updateaquaticbackport.lookfeel.client.LookAndFeelClient.init();
        // `runClient -Pselftest=client`: stream Lf's title-screen check, reached by name (a release jar has no test classes).
        String selftest = System.getProperty(UpdateAquaticBackport.MOD_ID + ".selftest");
        if (selftest != null && selftest.contains("client")) {
            try {
                Class.forName("com.exiledradio.updateaquaticbackport.selftest.ClientLookFeelTest").getMethod("register").invoke(null);
            } catch (ReflectiveOperationException e) {
                UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST DONE pass=0 fail=1: the test classes are not in this build", e);
            }
        }
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
            ResourceLocation name = block.getRegistryName();
            if (PseudoWater.isPseudoWater(block)) {
                name = new ResourceLocation(name.getNamespace(), "pseudo_water/" + name.getPath());
            }
            final ModelResourceLocation one = new ModelResourceLocation(name, "multipart");
            ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
                @Override
                protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                    return one;
                }
            });
        }
    }

    @SubscribeEvent
    public void blockColors(ColorHandlerEvent.Block event) {
        List<Block> water = new ArrayList<Block>();
        for (Block block : Content.blocks()) {
            if (PseudoWater.isPseudoWater(block)) {
                water.add(block);
            }
        }
        if (water.isEmpty()) {
            return;
        }
        event.getBlockColors().registerBlockColorHandler(new IBlockColor() {
            @Override
            public int colorMultiplier(IBlockState state, @Nullable IBlockAccess world, @Nullable BlockPos pos, int tintIndex) {
                // Tint 0 is the plant's own (1.13's tinted_cross and seagrass carry it, untinted); 1 is the
                // water surface the blockstate adds.
                if (tintIndex != 1) {
                    return -1;
                }
                return world != null && pos != null ? BiomeColorHelper.getWaterColorAtPos(world, pos) : -1;
            }
        }, water.toArray(new Block[0]));
    }
}
