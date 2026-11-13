package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.water.WaterColors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 1.13's water on a real client, for {@code gradlew runClient -Pselftest=clientwater}: at the title screen it checks
 * which pack serves {@code minecraft:textures/blocks/water_still.png}, that the stitched water sprites (the
 * atlas's and the ones the fluid renderer and the water block model use) are 1.13's gray ones, and the biome
 * water colors; then it makes a flat water world for each of plains, swamp and warm ocean, checks the tint
 * the client gives the water there, and saves a screenshot of each ({@code eclipse/screenshots/ua_water_*.png}).
 * Logs {@code WATER ...} lines and {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes.
 */
@SideOnly(Side.CLIENT)
public final class ClientWaterTest {

    private static final String[][] WORLDS = {
            {"plains", "minecraft:plains", Integer.toHexString(WaterColors.DEFAULT_WATER)},
            {"swamp", "minecraft:swampland", "617b64"},
            {"warm_ocean", UpdateAquaticBackport.MOD_ID + ":warm_ocean", "43d5ee"},
    };

    private static final boolean FLUIDLOGGED = net.minecraftforge.fml.common.Loader.isModLoaded("fluidlogged_api");

    private int ticks;
    private int pass;
    private int fail;
    private int world = -1;
    private int inWorld;
    private boolean done;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientWaterTest());
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST water registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            UpdateAquaticBackport.LOGGER.info("WATER ok {}", what);
        } else {
            this.fail++;
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || this.done) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (this.world < 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 100) {
                    return;
                }
                titleChecks(mc);
                // The test window is usually not focused; a paused game would never load the world.
                mc.gameSettings.pauseOnLostFocus = false;
                mc.gameSettings.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                this.world = 0;
                launch(mc);
                return;
            }
            if (mc.player == null || mc.world == null || mc.getIntegratedServer() == null) {
                return;
            }
            if (mc.currentScreen != null) {
                mc.displayGuiScreen(null);
            }
            if (!mc.world.isBlockLoaded(new BlockPos(8, 0, 8))) {
                return;
            }
            this.inWorld++;
            if (this.inWorld == 20) {
                MinecraftServer server = mc.getIntegratedServer();
                for (String c : new String[] {"time set 6000", "weather clear 100000", "gamerule doDaylightCycle false",
                        "gamerule doWeatherCycle false", "gamemode spectator @p", "tp @p 8 11 8 -45 35"}) {
                    server.getCommandManager().executeCommand(server, c);
                }
            }
            if (this.inWorld == 200) {
                worldChecks(mc);
            }
            if (this.inWorld == 220) {
                mc.gameSettings.hideGUI = true;
                mc.getToastGui().clear();
                String name = "ua_water_" + WORLDS[this.world][0] + (FLUIDLOGGED ? "" : "_no_fluidlogged") + ".png";
                ScreenShotHelper.saveScreenshot(mc.gameDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
                UpdateAquaticBackport.LOGGER.info("WATER screenshot {}", name);
                mc.gameSettings.hideGUI = false;
                mc.world.sendQuittingDisconnectingPacket();
                mc.loadWorld(null);
                this.world++;
                this.inWorld = 0;
                if (this.world < WORLDS.length) {
                    launch(mc);
                } else {
                    finish(mc);
                }
            }
        } catch (Throwable t) {
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private void finish(Minecraft mc) {
        this.done = true;
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    // ---------------------------------------------------------------------------------- title screen

    @SuppressWarnings("unchecked")
    private void titleChecks(Minecraft mc) throws IOException {
        List<IResourcePack> defaults = ReflectionHelper.getPrivateValue(Minecraft.class, mc, "defaultResourcePacks", "field_110449_ao");
        List<String> names = new ArrayList<String>();
        for (IResourcePack p : defaults) {
            names.add(p.getPackName());
        }
        UpdateAquaticBackport.LOGGER.info("WATER default packs: {}", names);
        String ours = UpdateAquaticBackport.MOD_NAME + " water";
        ok(names.contains(ours), "the water pack is in the default packs (for every later reload)");

        IResource still = mc.getResourceManager().getResource(new ResourceLocation("minecraft", "textures/blocks/water_still.png"));
        ok(ours.equals(still.getResourcePackName()), "minecraft:textures/blocks/water_still.png comes from " + still.getResourcePackName());
        still.close();

        TextureAtlasSprite atlas = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/water_still");
        TextureAtlasSprite flow = mc.getTextureMapBlocks().getAtlasSprite("minecraft:blocks/water_flow");
        ok(gray(atlas), "the atlas's water_still is 1.13's gray texture: " + describe(atlas));
        ok(gray(flow), "the atlas's water_flow is 1.13's gray texture: " + describe(flow));

        // What the block renderer actually draws with.
        TextureAtlasSprite particle = mc.getBlockRendererDispatcher().getBlockModelShapes().getTexture(Blocks.WATER.getDefaultState());
        ok(gray(particle), "the water block's sprite is gray: " + describe(particle));
        TextureAtlasSprite[] fluidSprites = ReflectionHelper.getPrivateValue(net.minecraft.client.renderer.BlockFluidRenderer.class,
                fluidRenderer(mc), "atlasSpritesWater", "field_178272_a");
        for (TextureAtlasSprite s : fluidSprites) {
            ok(s != null && gray(s), "BlockFluidRenderer draws water with a gray sprite: " + describe(s));
        }
        IBakedModel model = mc.getBlockRendererDispatcher().getModelForState(Blocks.WATER.getDefaultState());
        UpdateAquaticBackport.LOGGER.info("WATER water block model: {}", model == null ? null : model.getClass().getName());
        // Fluidlogged API draws water with its own fluid model, which bakes the fluid's color into the vertices and
        // is tinted by the biome on top: it must be baked white, or every biome's water is tinted twice.
        if (model != null && model.getClass().getName().endsWith("BakedModelFluid")) {
            float[] argb = ReflectionHelper.getPrivateValue((Class<? super IBakedModel>) model.getClass(), model, "argb");
            ok(argb[0] == 1.0F && argb[1] == 1.0F && argb[2] == 1.0F && argb[3] == 1.0F,
                    "Fluidlogged API's water model is baked untinted: " + java.util.Arrays.toString(argb));
        }
        int fluid = net.minecraftforge.fluids.FluidRegistry.WATER.getColor();
        ok(fluid == (0xFF000000 | WaterColors.DEFAULT_WATER), "Forge's water fluid keeps 1.13's blue for tanks: " + Integer.toHexString(fluid));

        ok(Biomes.PLAINS.getWaterColor() == WaterColors.DEFAULT_WATER, "plains water color " + hex(Biomes.PLAINS.getWaterColor()));
        ok(Biomes.SWAMPLAND.getWaterColor() == 0x617B64, "swamp water color " + hex(Biomes.SWAMPLAND.getWaterColor()));
    }

    private static net.minecraft.client.renderer.BlockFluidRenderer fluidRenderer(Minecraft mc) {
        return ReflectionHelper.getPrivateValue(net.minecraft.client.renderer.BlockRendererDispatcher.class,
                mc.getBlockRendererDispatcher(), "fluidRenderer", "field_175025_e");
    }

    /** @return true if nearly every pixel of the sprite's first frame is gray (1.12.2's are clearly blue) */
    private static boolean gray(TextureAtlasSprite sprite) {
        if (sprite == null || sprite.getFrameCount() == 0) {
            return false;
        }
        int[] px = sprite.getFrameTextureData(0)[0];
        int grayish = 0;
        for (int argb : px) {
            int r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
            if (Math.abs(r - b) <= 12 && Math.abs(g - b) <= 12) {
                grayish++;
            }
        }
        return grayish * 10 >= px.length * 9;
    }

    private static String describe(TextureAtlasSprite sprite) {
        if (sprite == null) {
            return "null";
        }
        if (sprite.getFrameCount() == 0) {
            return sprite.getIconName() + " (no frames)";
        }
        int[] px = sprite.getFrameTextureData(0)[0];
        long r = 0, g = 0, b = 0;
        for (int argb : px) {
            r += argb >> 16 & 255;
            g += argb >> 8 & 255;
            b += argb & 255;
        }
        int n = Math.max(1, px.length);
        return sprite.getIconName() + " mean rgb(" + r / n + "," + g / n + "," + b / n + ")";
    }

    private static String hex(int color) {
        return String.format("%06x", color & 0xFFFFFF);
    }

    // ---------------------------------------------------------------------------------- worlds

    private void launch(Minecraft mc) throws IOException {
        String[] w = WORLDS[this.world];
        Biome biome = ForgeRegistries.BIOMES.getValue(new ResourceLocation(w[1]));
        int id = biome == null ? 1 : Biome.getIdForBiome(biome);
        String folder = "uawater_" + w[0];
        FileUtils.deleteDirectory(new File(mc.gameDir, "saves/" + folder));
        WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
        settings.setGeneratorOptions("3;minecraft:bedrock,3*minecraft:sand,minecraft:stone,4*minecraft:water;" + id + ";");
        UpdateAquaticBackport.LOGGER.info("WATER world {} biome {} (id {})", folder, w[1], id);
        mc.launchIntegratedServer(folder, folder, settings);
    }

    private void worldChecks(Minecraft mc) {
        String[] w = WORLDS[this.world];
        BlockPos pos = new BlockPos(8, 7, 8);
        ok(mc.world.getBlockState(pos).getBlock() == Blocks.WATER, w[0] + ": water at " + pos + ": " + mc.world.getBlockState(pos));
        Biome biome = mc.world.getBiome(pos);
        int tint = BiomeColorHelper.getWaterColorAtPos(mc.world, pos);
        int blockTint = mc.getBlockColors().colorMultiplier(mc.world.getBlockState(pos), mc.world, pos, 0);
        ok(w[2].equals(hex(tint)), w[0] + " (" + biome.getRegistryName() + "): BiomeColorHelper water tint " + hex(tint) + ", want " + w[2]);
        ok(w[2].equals(hex(blockTint)), w[0] + ": BlockColors water tint " + hex(blockTint) + ", want " + w[2]);
    }
}
