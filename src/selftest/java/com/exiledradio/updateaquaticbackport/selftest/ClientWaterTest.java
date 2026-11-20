package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.presentation.option.SliderOption;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.lookfeel.LookAndFeel;
import com.exiledradio.updateaquaticbackport.water.BiomeBlend;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.IBlockAccess;
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
 * Biome Blend (1.13's, {@code water/BiomeBlend}): the average over a made-up border at each radius, the option's
 * range, default and label, and a last world whose client chunks get a jagged swamp and warm ocean border, shot from
 * above at 3x3 (1.12.2's) and 5x5 (1.13's default), then a mosaic of quick changes between five water biomes
 * ({@code ua_blend_border_*.png}, {@code ua_blend_mosaic_*.png}).
 * Logs {@code WATER ...} lines and {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes.
 */
@SideOnly(Side.CLIENT)
public final class ClientWaterTest {

    private static final String[][] WORLDS = {
            {"plains", "minecraft:plains", Integer.toHexString(WaterColors.DEFAULT_WATER)},
            {"swamp", "minecraft:swampland", "617b64"},
            {"warm_ocean", UpdateAquaticBackport.MOD_ID + ":warm_ocean", "43d5ee"},
            // Plains (no ocean decoration); its client chunks get a swamp and warm ocean border (borderWorld).
            {"border", "minecraft:plains", null},
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
            boolean border = WORLDS[this.world][2] == null;
            if (this.inWorld == 20) {
                MinecraftServer server = mc.getIntegratedServer();
                for (String c : new String[] {"time set 6000", "weather clear 100000", "gamerule doDaylightCycle false",
                        "gamerule doWeatherCycle false", "gamemode spectator @p", border ? "tp @p 8 24 8 0 90" : "tp @p 8 11 8 -45 35"}) {
                    server.getCommandManager().executeCommand(server, c);
                }
            }
            if (border) {
                borderWorld(mc);
                return;
            }
            if (this.inWorld == 200) {
                worldChecks(mc);
            }
            if (this.inWorld == 220) {
                screenshot(mc, "ua_water_" + WORLDS[this.world][0]);
                nextWorld(mc);
            }
        } catch (Throwable t) {
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private static void screenshot(Minecraft mc, String base) {
        mc.ingameGUI.getChatGUI().clearChatMessages(true);
        mc.gameSettings.hideGUI = true;
        mc.getToastGui().clear();
        String name = base + (FLUIDLOGGED ? "" : "_no_fluidlogged") + ".png";
        ScreenShotHelper.saveScreenshot(mc.gameDir, name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        UpdateAquaticBackport.LOGGER.info("WATER screenshot {}", name);
        mc.gameSettings.hideGUI = false;
    }

    private void nextWorld(Minecraft mc) throws IOException {
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
        if (model instanceof com.exiledradio.updateaquaticbackport.water.client.WaterClient.FlatFluid) {
            // Lit flat as vanilla's fluid renderer lights it: no shore-darkening gradient from ambient occlusion.
            ok(!model.isAmbientOcclusion() && !model.isAmbientOcclusion(Blocks.WATER.getDefaultState()),
                    "Fluidlogged API's water model is lit flat (no ambient occlusion)");
            model = ((com.exiledradio.updateaquaticbackport.water.client.WaterClient.FlatFluid) model).wrapped();
        } else {
            ok(!model.getClass().getName().endsWith("BakedModelFluid"), "Fluidlogged API's water model is wrapped to be lit flat");
        }
        if (model != null && model.getClass().getName().endsWith("BakedModelFluid")) {
            float[] argb = ReflectionHelper.getPrivateValue((Class<? super IBakedModel>) model.getClass(), model, "argb");
            ok(argb[0] == 1.0F && argb[1] == 1.0F && argb[2] == 1.0F && argb[3] == 1.0F,
                    "Fluidlogged API's water model is baked untinted: " + java.util.Arrays.toString(argb));
        }
        int fluid = net.minecraftforge.fluids.FluidRegistry.WATER.getColor();
        ok(fluid == (0xFF000000 | WaterColors.DEFAULT_WATER), "Forge's water fluid keeps 1.13's blue for tanks: " + Integer.toHexString(fluid));

        ok(Biomes.PLAINS.getWaterColor() == WaterColors.DEFAULT_WATER, "plains water color " + hex(Biomes.PLAINS.getWaterColor()));
        ok(Biomes.SWAMPLAND.getWaterColor() == 0x617B64, "swamp water color " + hex(Biomes.SWAMPLAND.getWaterColor()));
        // The cauldron's water: tinted (1.12.2's model has no tint, so 1.13's gray texture showed gray).
        IBakedModel cauldron = mc.getBlockRendererDispatcher().getModelForState(
                Blocks.CAULDRON.getDefaultState().withProperty(net.minecraft.block.BlockCauldron.LEVEL, 3));
        int tinted = 0;
        for (net.minecraft.client.renderer.block.model.BakedQuad q : cauldron.getQuads(null, null, 0L)) {
            if ("minecraft:blocks/water_still".equals(q.getSprite().getIconName()) && q.getTintIndex() == 0) {
                tinted++;
            }
        }
        ok(tinted == 1, "the full cauldron's water quad carries tint 0 (" + tinted + " found)");
        blendChecks();
    }

    // ---------------------------------------------------------------------------------- biome blend

    /** Swamp west of x = 0, plains from x = 0: the blend at (0, 64, 0) at each radius against the plain average. */
    private void blendChecks() {
        SliderOption option = LookAndFeel.BIOME_BLEND;
        ok(option.defaultValue() == 2.0 && option.min() == 0.0 && option.max() == 7.0,
                "Biome Blend is 1.13.2's: 0 to 7, default 2 (" + option.min() + ".." + option.max() + ", " + option.defaultValue() + ")");
        ok(BiomeBlend.radius == LookAndFeel.biomeBlendRadius(), "the colors' radius follows the option: " + BiomeBlend.radius);
        ok(option.get() != 2.0 || "5x5".equals(option.valueText()), "Biome Blend at 2 reads 5x5: " + option.valueText());
        IBlockAccess half = new IBlockAccess() {
            @Override public TileEntity getTileEntity(BlockPos pos) { return null; }
            @Override public int getCombinedLight(BlockPos pos, int light) { return 0; }
            @Override public IBlockState getBlockState(BlockPos pos) { return Blocks.AIR.getDefaultState(); }
            @Override public boolean isAirBlock(BlockPos pos) { return true; }
            @Override public Biome getBiome(BlockPos pos) { return pos.getX() < 0 ? Biomes.SWAMPLAND : Biomes.PLAINS; }
            @Override public int getStrongPower(BlockPos pos, EnumFacing side) { return 0; }
            @Override public WorldType getWorldType() { return WorldType.DEFAULT; }
            @Override public boolean isSideSolid(BlockPos pos, EnumFacing side, boolean fallback) { return fallback; }
        };
        int saved = BiomeBlend.radius;
        try {
            for (int r = 0; r <= 7; r++) {
                BiomeBlend.radius = r;
                int got = BiomeColorHelper.getWaterColorAtPos(half, new BlockPos(0, 64, 0));
                int want = average(r, r, r + 1);
                ok(got == want, "Biome Blend " + (2 * r + 1) + "x" + (2 * r + 1) + " at a swamp border: " + hex(got) + ", want " + hex(want));
            }
        } finally {
            BiomeBlend.radius = saved;
        }
    }

    /** @return the per-channel average of {@code swamp} swamp columns and {@code plains} plains columns, each 2r+1 long */
    private static int average(int r, int swamp, int plains) {
        int side = 2 * r + 1;
        int s = Biomes.SWAMPLAND.getWaterColor(), p = Biomes.PLAINS.getWaterColor();
        int out = 0;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int sum = side * (swamp * (s >> shift & 255) + plains * (p >> shift & 255));
            out |= (sum / (side * side) & 255) << shift;
        }
        return out;
    }

    /** Swamp west of a jagged line, warm ocean east of it: the line jumps 4 blocks every 4 blocks, as 1.12.2's borders do. */
    private static int jagged(int x, int z, Biome[] palette) {
        return x < ((Math.floorDiv(z, 4) & 1) == 0 ? 6 : 10) ? 0 : 1;
    }

    /**
     * Quick changes of biome: 4-block cells of swamp, ocean, warm, lukewarm and cold ocean, with a one-block fuzz at
     * their edges, as 1.12.2's biome map comes out of its zoom layers.
     */
    private static int mosaic(int x, int z, Biome[] palette) {
        int fx = x + Math.floorMod(hash(x, z, 1), 3) - 1, fz = z + Math.floorMod(hash(x, z, 2), 3) - 1;
        return Math.floorMod(hash(Math.floorDiv(fx, 4), Math.floorDiv(fz, 4), 3), palette.length);
    }

    private static int hash(int x, int z, int salt) {
        int h = x * 73856093 ^ z * 19349663 ^ salt * 83492791;
        h ^= h >>> 13;
        h *= 0x5bd1e995;
        return h ^ h >>> 15;
    }

    private interface Pattern {
        int at(int x, int z, Biome[] palette);
    }

    /** Gives the client's chunks around the player a biome pattern and rebuilds them at 3x3. */
    private void paint(Minecraft mc, Pattern pattern, Biome[] palette, String what) {
        for (int cx = -6; cx <= 6; cx++) {
            for (int cz = -6; cz <= 6; cz++) {
                byte[] biomes = mc.world.getChunk(cx, cz).getBiomeArray();
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        biomes[z << 4 | x] = (byte) Biome.getIdForBiome(palette[pattern.at(cx * 16 + x, cz * 16 + z, palette)]);
                    }
                }
            }
        }
        UpdateAquaticBackport.LOGGER.info("WATER border: {} on the client's chunks", what);
        LookAndFeel.BIOME_BLEND.set(1.0);
        ok(BiomeBlend.radius == 1, what + ": setting Biome Blend to 3x3 sets the colors' radius: " + BiomeBlend.radius);
        mc.renderGlobal.loadRenderers();
    }

    private static Biome ours(String path) {
        return ForgeRegistries.BIOMES.getValue(new ResourceLocation(UpdateAquaticBackport.MOD_ID, path));
    }

    /** The tint the client gives the water at a block is the plain average of the 5x5 columns around it. */
    private void checkAverage(Minecraft mc, BlockPos pos, String what) {
        long r = 0, g = 0, b = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int c = mc.world.getBiome(pos.add(dx, 0, dz)).getWaterColor();
                r += c >> 16 & 255;
                g += c >> 8 & 255;
                b += c & 255;
            }
        }
        int want = (int) (r / 25 << 16 | g / 25 << 8 | b / 25);
        int got = mc.getBlockColors().colorMultiplier(mc.world.getBlockState(pos), mc.world, pos, 0);
        ok(mc.world.getBlockState(pos).getBlock() == Blocks.WATER && (got & 0xFFFFFF) == want,
                what + ": the water's tint at " + pos + " is the 5x5 average: " + hex(got) + ", want " + hex(want));
    }

    /** Two scenes, each shot from above at 3x3 and then 5x5: a jagged swamp and warm ocean border, then a mosaic. */
    private void borderWorld(Minecraft mc) throws IOException {
        Biome[] two = {Biomes.SWAMPLAND, ours("warm_ocean")};
        Biome[] five = {Biomes.SWAMPLAND, Biomes.OCEAN, ours("warm_ocean"), ours("lukewarm_ocean"), ours("cold_ocean")};
        if (this.inWorld == 40) {
            paint(mc, ClientWaterTest::jagged, two, "jagged swamp and warm ocean border");
        }
        if (this.inWorld == 200 || this.inWorld == 520) {
            screenshot(mc, this.inWorld == 200 ? "ua_blend_border_3x3" : "ua_blend_mosaic_3x3");
            LookAndFeel.BIOME_BLEND.set(2.0);
            ok(BiomeBlend.radius == 2, "setting Biome Blend to 5x5 sets the colors' radius: " + BiomeBlend.radius);
        }
        if (this.inWorld == 360) {
            screenshot(mc, "ua_blend_border_5x5");
            checkAverage(mc, new BlockPos(7, 7, 0), "border");
            paint(mc, ClientWaterTest::mosaic, five, "mosaic of swamp, ocean, warm, lukewarm and cold ocean");
        }
        if (this.inWorld == 680) {
            screenshot(mc, "ua_blend_mosaic_5x5");
            checkAverage(mc, new BlockPos(3, 7, 5), "mosaic");
            nextWorld(mc);
        }
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
        int cauldronTint = mc.getBlockColors().colorMultiplier(Blocks.CAULDRON.getDefaultState(), mc.world, pos, 0);
        ok(w[2].equals(hex(cauldronTint)), w[0] + ": cauldron water tint " + hex(cauldronTint) + ", want " + w[2]);
    }
}
