package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.entity.Entity;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.MapDecoration;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Random;

/**
 * Today's {@code exploration_map} loot function, for structures found through Backport Core's
 * {@link Structures}: turns the item into a filled map centered on the nearest structure of a kind, with
 * the unexplored land drawn in, a decoration on the structure and today's name
 * ({@code filled_map.<kind>}). As today's loot table filters it out, nothing is given when there is no
 * such structure within the search radius.
 *
 * <pre>{"function": "updateaquaticbackport:exploration_map", "destination": "buried_treasure",
 *  "decoration": "red_x", "zoom": 1, "search_radius": 50, "skip_existing_chunks": false}</pre>
 *
 * <p>Today's game searches from the chest; 1.12.2's loot context has no position, so the search starts
 * where the player who opened it stands (or the entity the loot comes from). With neither, no map.
 */
public final class LootExplorationMap extends LootFunction {

    private final String destination;
    private final String decoration;
    private final byte zoom;
    private final int searchRadius;
    private final boolean skipExistingChunks;

    LootExplorationMap(LootCondition[] conditions, String destination, String decoration, byte zoom, int searchRadius,
                       boolean skipExistingChunks) {
        super(conditions);
        this.destination = destination;
        this.decoration = decoration;
        this.zoom = zoom;
        this.searchRadius = searchRadius;
        this.skipExistingChunks = skipExistingChunks;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random random, LootContext context) {
        WorldServer world = context.getWorld();
        BlockPos origin = origin(context);
        if (world == null || origin == null) {
            return ItemStack.EMPTY;
        }
        BlockPos target = Structures.nearest(world, this.destination, origin, this.searchRadius);
        if (target == null) {
            return ItemStack.EMPTY;
        }
        return create(world, target, this.destination, decorationType(this.decoration), this.zoom);
    }

    /** Today's {@code MapItem.create} (tracking on, unlimited), biome preview, the target decoration and the name. */
    static ItemStack create(WorldServer world, BlockPos target, String kind, MapDecoration.Type type, byte zoom) {
        ItemStack map = ItemMap.setupNewMap(world, target.getX(), target.getZ(), zoom, true, true);
        ItemMap.renderBiomePreviewMap(world, map);
        MapData.addTargetDecoration(map, target, "+", type);
        map.setTranslatableName("filled_map." + kind);
        return map;
    }

    @Nullable
    private static BlockPos origin(LootContext context) {
        Entity entity = context.getKillerPlayer();
        if (entity == null) {
            entity = context.getLootedEntity();
        }
        return entity == null ? null : new BlockPos(entity);
    }

    static MapDecoration.Type decorationType(String name) {
        if ("red_x".equals(name)) {
            return StructureContent.redX();
        }
        try {
            return MapDecoration.Type.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MapDecoration.Type.TARGET_X;
        }
    }

    public static final class Serializer extends LootFunction.Serializer<LootExplorationMap> {

        public Serializer() {
            super(Content.id("exploration_map"), LootExplorationMap.class);
        }

        @Override
        public void serialize(JsonObject json, LootExplorationMap function, JsonSerializationContext context) {
            json.addProperty("destination", function.destination);
            json.addProperty("decoration", function.decoration);
            json.addProperty("zoom", function.zoom);
            json.addProperty("search_radius", function.searchRadius);
            json.addProperty("skip_existing_chunks", function.skipExistingChunks);
        }

        @Override
        public LootExplorationMap deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
            // Today's defaults: buried treasure, the mansion icon, zoom 2, radius 50, skipping explored chunks.
            return new LootExplorationMap(conditions,
                    JsonUtils.getString(json, "destination", Structures.BURIED_TREASURE),
                    JsonUtils.getString(json, "decoration", "mansion"),
                    (byte) JsonUtils.getInt(json, "zoom", 2),
                    JsonUtils.getInt(json, "search_radius", 50),
                    JsonUtils.getBoolean(json, "skip_existing_chunks", true));
        }
    }
}
