package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.DebugKeys;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.FogPipeline;
import com.exiledradio.backportcore.presentation.client.PanoramaRenderer;
import com.exiledradio.backportcore.presentation.client.TitleSplashes;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.lookfeel.client.LookAndFeelClient;
import com.exiledradio.updateaquaticbackport.lookfeel.client.UnderwaterFogStep;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.EntityList;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.List;
import java.util.Map;

/**
 * Stream Lf's client check, for {@code gradlew runClient -Pselftest=client}: at the title screen it checks what
 * Backport Core shows for 1.13 (panorama, overlay, splash pool, loading revision, F3 keys, the underwater fog
 * step, the Allow Snooper default), the F3+C and F3+I commands and the lang overrides, all in code; nothing
 * reaches the window. Logs {@code LOOKFEEL ...} lines and {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes.
 */
@SideOnly(Side.CLIENT)
public final class ClientLookFeelTest {

    private int ticks;
    private int pass;
    private int fail;
    private boolean done;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientLookFeelTest());
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
        } else {
            this.fail++;
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (event.phase != TickEvent.Phase.END || this.done || !(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 400) {
            return;
        }
        this.done = true;
        try {
            run(mc);
        } catch (Throwable t) {
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
        }
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }

    private void run(Minecraft mc) {
        GameVersion newest = Presentation.newest();
        LoadingRevision loading = Presentation.loadingRevision();
        ResourceLocation[] faces = PanoramaRenderer.faces();
        List<String> pool = TitleSplashes.lastPool;
        StringBuilder installed = new StringBuilder();
        for (Contribution c : Presentation.installed()) {
            installed.append(c.version()).append(c.owners()).append(' ');
        }
        UpdateAquaticBackport.LOGGER.info("LOOKFEEL installed: {}", installed.toString().trim());
        UpdateAquaticBackport.LOGGER.info("LOOKFEEL newest={} panorama={} overlay={} loading={} splashes={} panoramaFrames={} fog={}",
                newest, faces == null ? null : faces[0], Presentation.panoramaOverlay(), loading,
                pool == null ? -1 : pool.size(), PanoramaRenderer.frames, FogPipeline.steps().names(newest));
        boolean alone = newest.equals(GameVersion.parse("1.13"));

        ok(PanoramaRenderer.modern(), "the title screen is drawn the 1.13+ way");
        ok(PanoramaRenderer.frames > 20, "the panorama was drawn: " + PanoramaRenderer.frames + " frames");
        if (alone) {
            ok(faces != null && faces[0].getNamespace().equals(UpdateAquaticBackport.MOD_ID), "1.13's ocean panorama is shown: "
                    + (faces == null ? null : faces[0]));
            ok(Presentation.panoramaOverlay() != null && Presentation.panoramaOverlay().getNamespace().equals(UpdateAquaticBackport.MOD_ID),
                    "1.13's overlay is used: " + Presentation.panoramaOverlay());
            ok(loading == LoadingRevision.TEXT_1_13, "the world-loading screen is 1.13's: " + loading);
            ok(!mc.gameSettings.snooperEnabled, "1.13's default turned Allow Snooper off");
        }
        ok(pool != null, "the title screen drew from Core's splash pool");
        if (pool != null) {
            for (String s : new String[] {"Thank you for the fish!", "All rumors are true!", "Truly gone fishing!"}) {
                ok(pool.contains(s), "1.13's splash \"" + s + "\" is in the pool");
            }
        }

        Map<Integer, DebugKeys.Key> keys = DebugKeys.map(Presentation.installed());
        DebugKeys.Key c = keys.get(Keyboard.KEY_C);
        DebugKeys.Key i = keys.get(Keyboard.KEY_I);
        ok(c != null && c.action != null && "debug.copy_location.help".equals(c.helpKey), "F3+C copies the location: " + c);
        ok(i != null && i.action != null && "debug.inspect.help".equals(i.helpKey), "F3+I copies block or entity data: " + i);
        UpdateAquaticBackport.LOGGER.info("LOOKFEEL F3 keys: {}", keys.values());

        List<String> fog = FogPipeline.steps().names(newest);
        ok(fog.toString().contains(UnderwaterFogStep.NAME), "the underwater fog is a step of Core's fog pipeline: " + fog);

        ok("/tp @s 1.50 64.00 -2.25 90.00 -10.00".equals(LookAndFeelClient.locationCommand(1.5, 64, -2.25, 90, -10)),
                "F3+C's command: " + LookAndFeelClient.locationCommand(1.5, 64, -2.25, 90, -10));
        String block = LookAndFeelClient.blockCommand(Blocks.FURNACE.getDefaultState().withProperty(
                net.minecraft.block.BlockFurnace.FACING, EnumFacing.EAST), new BlockPos(1, 2, 3), tag());
        ok("/setblock 1 2 3 minecraft:furnace facing=east replace {CookTime:5}".equals(block), "F3+I's block command: " + block);
        String plain = LookAndFeelClient.blockCommand(Blocks.DIRT.getDefaultState(), new BlockPos(0, 0, 0), null);
        ok(plain.startsWith("/setblock 0 0 0 minecraft:dirt ") && !plain.contains("replace"), "F3+I without data: " + plain);
        String entity = LookAndFeelClient.entityCommand(new ResourceLocation("minecraft:pig"), 1, 2, 3, entityTag());
        ok("/summon minecraft:pig 1.00 2.00 3.00 {Saddle:1b}".equals(entity), "F3+I's entity command drops UUID, Pos and Dimension: " + entity);
        ok(EntityList.getKey(net.minecraft.entity.passive.EntityPig.class) != null, "entity ids resolve");

        String[][] lang = {
                {"debug.copy_location.message", "Copied location to clipboard"},
                {"key.mouse.left", "Left Button"},
                {"multiplayer.downloadingTerrain", "Loading terrain..."},
                {"options.narrator.off", "OFF"},
        };
        for (String[] kv : lang) {
            ok(kv[1].equals(I18n.format(kv[0])), "I18n " + kv[0] + " = \"" + kv[1] + "\", got \"" + I18n.format(kv[0]) + "\"");
        }
    }

    private static NBTTagCompound tag() {
        NBTTagCompound t = new NBTTagCompound();
        t.setInteger("x", 1);
        t.setInteger("y", 2);
        t.setInteger("z", 3);
        t.setString("id", "minecraft:furnace");
        t.setInteger("CookTime", 5);
        return t;
    }

    private static NBTTagCompound entityTag() {
        NBTTagCompound t = new NBTTagCompound();
        t.setLong("UUIDMost", 1L);
        t.setLong("UUIDLeast", 2L);
        t.setInteger("Dimension", 0);
        t.setTag("Pos", new net.minecraft.nbt.NBTTagList());
        t.setBoolean("Saddle", true);
        return t;
    }
}
