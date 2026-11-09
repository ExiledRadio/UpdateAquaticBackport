package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.GameVersion;
import com.exiledradio.backportcore.presentation.LoadingRevision;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.Splashes;
import com.exiledradio.backportcore.presentation.VersionedDefaults;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import net.minecraft.util.ResourceLocation;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stream Lf: 1.13's look and feel registered with Backport Core, its assets, the default and the lang overrides. */
public final class LookFeelSelfTest implements SelfTest {

    private static final String ID = UpdateAquaticBackport.MOD_ID;

    @Override
    public String name() {
        return "lookfeel";
    }

    @Override
    public void run(Check c) throws Exception {
        registration(c);
        panorama(c);
        splashes(c);
        defaults(c);
        lang(c);
    }

    private static void registration(Check c) {
        Contribution v113 = Presentation.contribution("1.13");
        c.ok(v113 != null && v113.owners().contains(ID), "1.13 is registered by " + ID + ": " + v113);
        c.eq(GameVersion.parse("1.13"), Presentation.newest(), "the newest installed version (this mod alone)");
        c.eq(LoadingRevision.TEXT_1_13, Presentation.loadingRevision(), "the world-loading screen revision");
        // Core's finished presentation module: vanilla text, the underwater music, Core's 1.13 table.
        c.ok(Presentation.vanillaTexts().contains(new ResourceLocation(ID, "vanilla_text")), "1.13's vanilla text is registered");
        net.minecraft.util.SoundEvent music = net.minecraft.util.SoundEvent.REGISTRY.getObject(new ResourceLocation(ID, "music.under_water"));
        c.ok(music != null && music == com.exiledradio.updateaquaticbackport.common.SharedSounds.MUSIC_UNDER_WATER, "music.under_water is registered");
        for (String track : new String[] {"axolotl", "dragon_fish", "shuniji"}) {
            c.ok(LookFeelSelfTest.class.getResource("/assets/" + ID + "/sounds/music/game/water/" + track + ".ogg") != null, track + ".ogg is in the jar");
        }
        c.ok(com.exiledradio.backportcore.presentation.VanillaRevisions.anaglyphRemoved() && com.exiledradio.backportcore.presentation.VanillaRevisions.numberedGuiScale()
                && com.exiledradio.backportcore.presentation.VanillaRevisions.titleFadeIn() && !com.exiledradio.backportcore.presentation.VanillaRevisions.vboRemoved(),
                "Core's 1.13 table: no Anaglyph, numbered GUI Scale, the fade-in; VBOs stay until 1.14");
        c.eq(com.exiledradio.backportcore.presentation.VanillaRevisions.Startup.NONE, com.exiledradio.backportcore.presentation.VanillaRevisions.startup(),
                "no startup screen of Core's at 1.13 (1.14 brought it)");
        // CORE_REQUESTS 7: prismarine stairs and slabs are 1.13's waterloggable shapes, water sources only.
        net.minecraft.block.Block stairs = net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS.getValue(new ResourceLocation(ID, "prismarine_stairs"));
        c.ok(stairs instanceof com.exiledradio.backportcore.waterlog.Waterloggable, "prismarine stairs are Waterloggable: " + stairs);
        if (stairs != null) {
            net.minecraft.block.state.IBlockState s = stairs.getDefaultState();
            c.ok(com.exiledradio.backportcore.waterlog.fluidlogged.WaterRules.decide(s, git.jbredwards.fluidlogged_api.api.util.FluidState.of(net.minecraftforge.fluids.FluidRegistry.WATER)) == null
                    && com.exiledradio.backportcore.waterlog.fluidlogged.WaterRules.decide(s, git.jbredwards.fluidlogged_api.api.util.FluidState.of(net.minecraftforge.fluids.FluidRegistry.LAVA)) == Boolean.FALSE,
                    "prismarine stairs take water, not lava");
        }
        for (net.minecraft.block.Block b : net.minecraftforge.fml.common.registry.ForgeRegistries.BLOCKS) {
            if (b instanceof com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineSlab) {
                com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineSlab slab = (com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineSlab) b;
                c.eq(!slab.isDouble(), slab.canHoldWater(slab.getDefaultState(), null, null), b.getRegistryName() + " holds water only as a half slab");
            }
        }
    }

    private static void panorama(Check c) throws IOException {
        List<Presentation.Panorama> all = Presentation.panoramas();
        Presentation.Panorama last = all.isEmpty() ? null : all.get(all.size() - 1);
        if (!c.ok(last != null && last.version.equals(GameVersion.parse("1.13")), "the newest panorama is 1.13's: " + all.size())) {
            return;
        }
        for (int i = 0; i < 6; i++) {
            c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_" + i + ".png"), last.faces[i], "panorama face " + i);
            int[] size = pngSize(last.faces[i]);
            c.ok(size != null && size[0] == 1024 && size[1] == 1024, "face " + i + " is 1.13.2's 1024x1024 image: " + Arrays.toString(size));
        }
        ResourceLocation overlay = Presentation.panoramaOverlay();
        c.eq(new ResourceLocation(ID, "textures/gui/title/background/panorama_overlay.png"), overlay, "the panorama overlay");
        int[] size = overlay == null ? null : pngSize(overlay);
        c.ok(size != null && size[0] == 16 && size[1] == 128, "1.13.2's overlay is the 16x128 gradient: " + Arrays.toString(size));
    }

    private static final String[] ADDED = {"Thank you for the fish!", "All rumors are true!", "Truly gone fishing!"};

    private static void splashes(Check c) throws IOException {
        List<String> base = lines("/assets/minecraft/texts/splashes.txt");
        c.ok(base.size() > 300, "1.12.2's splashes.txt is readable: " + base.size());
        List<String> pool = Splashes.pool(base, Presentation.installed(), f -> lines(f));
        for (String s : ADDED) {
            c.ok(!base.contains(s) && pool.contains(s), "the splash pool has 1.13's \"" + s + "\"");
        }
        Set<String> expected = new LinkedHashSet<String>(base);
        expected.addAll(Arrays.asList(ADDED));
        c.eq(expected.size(), pool.size(), "pool size: 1.12.2's set plus 1.13's three (1.13 removed none)");
    }

    private static void defaults(Check c) {
        // Read against an empty record: a dev client run may already have applied it in this run folder.
        java.io.File real = VersionedDefaults.file();
        java.io.File empty = new java.io.File(real.getParentFile(), "selftest-empty-defaults.txt");
        empty.delete();
        VersionedDefaults.useFile(empty);
        boolean found = false;
        try {
            for (VersionedDefaults.Entry e : VersionedDefaults.pending(Presentation.installed())) {
                if (e.version.equals(GameVersion.parse("1.13")) && "snooperEnabled".equals(e.key)) {
                    found = "false".equals(e.value) && "1.13/game_option/snooperEnabled".equals(e.flag());
                }
            }
        } finally {
            VersionedDefaults.useFile(real);
            empty.delete();
        }
        c.ok(found, "1.13's default \"Allow Snooper: OFF\" (snooperEnabled:false) is registered for a fresh instance");
    }

    private static final String[][] LANG = {
            {"debug.copy_location.help", "F3 + C = Copy location as /tp command, hold F3 + C to crash the game"},
            {"debug.inspect.help", "F3 + I = Copy entity or block data to clipboard"},
            {"debug.inspect.server.block", "Copied server-side block data to clipboard"},
            {"key.mouse.left", "Left Button"},
            {"multiplayer.downloadingTerrain", "Loading terrain..."},
            {"selectWorld.edit.openFolder", "Open World Folder"},
            {"options.narrator.off", "OFF"},
    };

    private static final String[] LANGUAGES = {"de_de", "es_ar", "es_es", "es_mx", "es_uy", "es_ve", "fr_fr", "it_it", "ja_jp",
            "ko_kr", "nl_nl", "pl_pl", "pt_br", "ru_ru", "tr_tr", "uk_ua", "zh_cn", "zh_tw"};

    private static void lang(Check c) throws IOException {
        Map<String, String> en = langFile("en_us");
        for (String[] kv : LANG) {
            c.eq(kv[1], en.get(kv[0]), "en_us " + kv[0]);
        }
        c.ok(!en.containsKey("selectWorld.allowCommands.info"), "1.13's \"/experience\" wording is left out (1.12.2 has /xp)");
        for (String code : LANGUAGES) {
            Map<String, String> l = langFile(code);
            int have = 0;
            for (String[] kv : LANG) {
                if (l.get(kv[0]) != null && !l.get(kv[0]).isEmpty()) {
                    have++;
                }
            }
            c.eq(LANG.length, have, code + " has 1.13's F3 and reworded vanilla keys");
        }
    }

    // ------------------------------------------------------------------------------------------

    /** The mod's own lang file and its vanilla text (Core's, newest version wins) for one language. */
    private static Map<String, String> langFile(String code) throws IOException {
        Map<String, String> out = new HashMap<String, String>();
        for (String folder : new String[] {"lang/", "vanilla_text/"}) {
            for (String line : lines(new ResourceLocation(ID, folder + code + ".lang"))) {
                int eq = line.indexOf('=');
                if (eq > 0 && !line.startsWith("#")) {
                    if (folder.startsWith("vanilla") && out.containsKey(line.substring(0, eq))) {
                        throw new IOException(code + ": " + line.substring(0, eq) + " is in lang/ and vanilla_text/");
                    }
                    out.put(line.substring(0, eq), line.substring(eq + 1));
                }
            }
        }
        return out;
    }

    private static List<String> lines(ResourceLocation rl) throws IOException {
        return lines("/assets/" + rl.getNamespace() + "/" + rl.getPath());
    }

    private static List<String> lines(String path) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream(path);
        if (in == null) {
            throw new IOException("not on the classpath: " + path);
        }
        List<String> out = new ArrayList<String>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        try {
            String line;
            while ((line = r.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    out.add(line);
                }
            }
        } finally {
            r.close();
        }
        return out;
    }

    private static int[] pngSize(ResourceLocation rl) throws IOException {
        InputStream in = LookFeelSelfTest.class.getResourceAsStream("/assets/" + rl.getNamespace() + "/" + rl.getPath());
        if (in == null) {
            return null;
        }
        byte[] b = read(in);
        if (b.length < 24 || b[1] != 'P' || b[2] != 'N' || b[3] != 'G') {
            return null;
        }
        return new int[] {readInt(b, 16), readInt(b, 20)};
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
    }

    private static byte[] read(InputStream in) throws IOException {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }
}
