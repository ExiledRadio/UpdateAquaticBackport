package com.exiledradio.updateaquaticbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.updateaquaticbackport.common.SharedSounds;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.13's in-game music, a step of Backport Core's music owner (1.13-pre1): under water in an ocean or river the
 * underwater music (Axolotl, Dragon Fish, Shuniji), 1.14.4's {@code getSituationalMusic}: kept while it plays, never
 * in place of the nether, End or a dimension's own music, and not when the game music is already playing. 1.13's F3
 * text is Backport Core's since Core 1.4.7.
 */
@SideOnly(Side.CLIENT)
final class LookAndFeel113 {

    private LookAndFeel113() {
    }

    static void register() {
        GameMusic.setUnderWater(SharedSounds.MUSIC_UNDER_WATER);
        GameMusic.add("1.13", "under water", LookAndFeel113::music);
    }

    static void music(GameMusic.Situation s) {
        Minecraft mc = s.mc;
        if (mc.player == null || mc.world == null) {
            return;
        }
        MusicTicker.MusicType vanilla = s.vanilla;
        // 1.14.4: the nether, the End and a dimension's own music come first; only game and creative music give way.
        if (vanilla != MusicTicker.MusicType.GAME && vanilla != MusicTicker.MusicType.CREATIVE || mc.world.provider.getMusicType() != null) {
            return;
        }
        boolean playing = s.isPlaying(SharedSounds.MUSIC_UNDER_WATER);
        if (playing || isUnderWater(mc) && !s.isPlaying(MusicTicker.MusicType.GAME.getMusicLocation()) && oceanOrRiver(mc)) {
            s.choose(new GameMusic.Track(SharedSounds.MUSIC_UNDER_WATER, 12000, 24000, true));
        }
    }

    static boolean isUnderWater(Minecraft mc) {
        return mc.player.isInsideOfMaterial(Material.WATER);
    }

    static boolean oceanOrRiver(Minecraft mc) {
        Biome biome = mc.world.getBiome(new BlockPos(mc.player));
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }
}
