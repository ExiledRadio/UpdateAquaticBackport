package com.exiledradio.updateaquaticbackport.common;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;

/**
 * Who owns a block that more than one mod of this family needs.
 *
 * <h2>The rule</h2>
 * The mod that shipped a block first owns it. When the owner is installed this mod registers nothing
 * for that block and uses the owner's; when it is not, this mod registers its own copy under its own
 * id, with the same states and metadata, so the owner can later rename it in place.
 *
 * <h2>Temporary home</h2>
 * The shared ownership table belongs in the family's core library. Until this mod depends on it, the
 * table is this class: one constant per owner, and lookups by registry name that never link against
 * the other mod's classes.
 */
public final class Owners {

    /**
     * Owns stripped logs and wood, blue ice and the five woods' trapdoors, buttons and plates. Not sea
     * pickles: they shipped there first, but they are this mod's own (Backport Core's ownership table).
     */
    public static final String VILLAGE_AND_PILLAGE = "villagerbackport";

    private Owners() {
    }

    /** @return true if the owning mod is installed, so its block is used and ours is never registered */
    public static boolean present(String owner) {
        return Loader.isModLoaded(owner);
    }

    /**
     * @return the block registered under {@code owner:name} if the owner is installed, else ours under
     * this mod's id; null if neither exists (the stream that would register ours is switched off)
     */
    @Nullable
    public static Block block(String owner, String name) {
        ResourceLocation id = present(owner) ? new ResourceLocation(owner, name) : Content.id(name);
        Block block = ForgeRegistries.BLOCKS.getValue(id);
        return block == null || !id.equals(block.getRegistryName()) ? null : block;
    }

    /** @return the item for {@code name} the same way as {@link #block}, or null */
    @Nullable
    public static Item item(String owner, String name) {
        ResourceLocation id = present(owner) ? new ResourceLocation(owner, name) : Content.id(name);
        Item item = ForgeRegistries.ITEMS.getValue(id);
        return item == null || !id.equals(item.getRegistryName()) ? null : item;
    }

    /**
     * Looks up another stream's item by name, so streams never link against each other's classes.
     *
     * @return this mod's item {@code name}, or null when its stream is switched off
     */
    @Nullable
    public static Item ours(String name) {
        Item item = ForgeRegistries.ITEMS.getValue(Content.id(name));
        return item == null || !Content.id(name).equals(item.getRegistryName()) ? null : item;
    }

    /** As {@link #ours(String)}, for blocks. */
    @Nullable
    public static Block ourBlock(String name) {
        Block block = ForgeRegistries.BLOCKS.getValue(Content.id(name));
        return block == null || !Content.id(name).equals(block.getRegistryName()) ? null : block;
    }
}
