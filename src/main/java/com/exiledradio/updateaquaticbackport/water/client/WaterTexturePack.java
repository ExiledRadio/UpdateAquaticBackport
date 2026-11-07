package com.exiledradio.updateaquaticbackport.water.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * Serves 1.13's water textures in place of 1.12.2's, only while biome water colors are on.
 *
 * <p>The files live in this mod's own folder ({@code textures/water/}), not under {@code minecraft}, so
 * switching the colors off brings back 1.12.2's blue texture. The pack sits after the game's and the
 * mods' packs and before any resource pack the player picks, so a player's own water texture still wins.
 */
final class WaterTexturePack implements IResourcePack {

    private static final String FOLDER = "/assets/" + UpdateAquaticBackport.MOD_ID + "/textures/water/";

    /** 1.12.2's paths and our files. */
    private static final Map<String, String> FILES = ImmutableMap.<String, String>builder()
            .put("textures/blocks/water_still.png", "water_still.png")
            .put("textures/blocks/water_still.png.mcmeta", "water_still.png.mcmeta")
            .put("textures/blocks/water_flow.png", "water_flow.png")
            .put("textures/blocks/water_flow.png.mcmeta", "water_flow.png.mcmeta")
            .put("textures/blocks/water_overlay.png", "water_overlay.png")
            .build();

    @Override
    public InputStream getInputStream(ResourceLocation location) throws IOException {
        String file = this.file(location);
        InputStream in = file == null ? null : WaterTexturePack.class.getResourceAsStream(FOLDER + file);
        if (in == null) {
            throw new FileNotFoundException(location.toString());
        }
        return in;
    }

    @Override
    public boolean resourceExists(ResourceLocation location) {
        return this.file(location) != null;
    }

    @Nullable
    private String file(ResourceLocation location) {
        return "minecraft".equals(location.getNamespace()) ? FILES.get(location.getPath()) : null;
    }

    /** @return true if every file is in the jar */
    boolean complete() {
        for (String file : FILES.values()) {
            if (WaterTexturePack.class.getResource(FOLDER + file) == null) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Set<String> getResourceDomains() {
        return ImmutableSet.of("minecraft");
    }

    @Nullable
    @Override
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer serializer, String section) {
        return null;
    }

    @Override
    public BufferedImage getPackImage() throws IOException {
        throw new FileNotFoundException("pack.png");
    }

    @Override
    public String getPackName() {
        return UpdateAquaticBackport.MOD_NAME + " water";
    }
}
