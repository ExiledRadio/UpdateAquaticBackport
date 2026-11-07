package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.structure.ModernTemplate;
import com.exiledradio.backportcore.structure.ScatteredPlacement;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.structure.TemplateBlock;
import com.exiledradio.backportcore.structure.TemplatePiece;
import com.exiledradio.backportcore.structure.TemplatePlacement;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.structure.MapGenBuriedTreasure;
import com.exiledradio.updateaquaticbackport.structure.MapGenOceanRuin;
import com.exiledradio.updateaquaticbackport.structure.MapGenShipwreck;
import com.exiledradio.updateaquaticbackport.structure.OceanRuinPiece;
import com.exiledradio.updateaquaticbackport.structure.ShipwreckPiece;
import com.exiledradio.updateaquaticbackport.structure.StructureBiomes;
import com.exiledradio.updateaquaticbackport.structure.StructureContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockStone;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.Mirror;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.storage.MapData;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

/**
 * Stream S: shipwrecks, ocean ruins and buried treasure.
 *
 * <ul>
 *   <li>registration: kinds, placement numbers, the frequency roll, the red X and its icon;</li>
 *   <li>biome rules on vanilla's and the ocean stream's biomes;</li>
 *   <li>every structure file loads with every block resolved, and places, with its markers filled;</li>
 *   <li>every loot table loads and rolls today's items, buried treasure maps included;</li>
 *   <li>real generation: the nearest sunken and beached shipwreck, warm and cold ruin and buried treasure
 *       from spawn are generated in fresh chunks, then checked block by block against their files, their
 *       chests, drowned and the locator;</li>
 *   <li>saved pieces read back the same.</li>
 * </ul>
 */
public final class StructureSelfTest implements SelfTest {

    private Check c;
    private int largeRuins;
    private int clusters;
    private WorldServer world;

    @Override
    public String name() {
        return "structure";
    }

    @Override
    public void run(Check check) {
        this.c = check;
        this.world = check.world;
        if (!c.ok(StructureContent.enabled(), "structures.enabled is on for the test")) {
            return;
        }
        registration();
        biomes();
        templates();
        loot();
        generation();
    }

    // ---- registration

    private void registration() {
        for (String kind : new String[]{Structures.SHIPWRECK, Structures.OCEAN_RUIN, Structures.BURIED_TREASURE}) {
            c.ok(Structures.has(kind), kind + " has a locator");
            c.ok(ScatteredStructure.of(world, kind) != null, kind + " generates in the overworld");
        }
        placement(Structures.SHIPWRECK, 24, 4, 165745295);
        placement(Structures.OCEAN_RUIN, 20, 8, 14357621);
        placement(Structures.BURIED_TREASURE, 1, 0, 0);

        // legacy_type_2 at 0.01 over 200 000 chunks.
        int kept = 0;
        for (int x = 0; x < 400; x++) {
            for (int z = 0; z < 500; z++) {
                kept += MapGenBuriedTreasure.frequencyRoll(world.getSeed(), x - 200, z - 250) ? 1 : 0;
            }
        }
        c.ok(kept > 1800 && kept < 2200, "buried treasure kept in about 1% of chunks: " + kept + " of 200000");
        // The exact formula: setLargeFeatureWithSalt(seed, x, z, 10387320).nextFloat() < 0.01.
        Random r = new Random(5 * 341873128712L + -3 * 132897987541L + 1234L + 10387320);
        c.eq(r.nextFloat() < 0.01F, MapGenBuriedTreasure.frequencyRoll(1234L, 5, -3), "frequency roll formula");

        c.eq("RED_X", StructureContent.redX().name(), "the red X decoration type");
        c.eq((byte) 10, StructureContent.redX().getIcon(), "red X icon number (slot 10 of map_icons.png)");
        c.ok(!StructureContent.redX().hasMapColor(), "a buried treasure map has no tint");
        // Ours, not 1.12.2's own (whose slots 10-15 are a plain blue filler): the copy in this mod's jar.
        java.net.URL ours = null;
        try {
            java.util.Enumeration<java.net.URL> urls = UpdateAquaticBackport.class.getClassLoader()
                    .getResources("assets/minecraft/textures/map/map_icons.png");
            while (urls.hasMoreElements()) {
                java.net.URL u = urls.nextElement();
                if (u.toString().toLowerCase(java.util.Locale.ROOT).contains("updateaquaticbackport")) {
                    ours = u;
                }
            }
        } catch (java.io.IOException e) {
            c.fail("map_icons.png lookup: " + e);
        }
        if (!c.ok(ours != null, "this mod ships its own map_icons.png")) {
            return;
        }
        try (InputStream in = ours.openStream()) {
            BufferedImage icons = ImageIO.read(in);
            int cell = icons.getWidth() / 4;
            int opaque = 0;
            int red = 0;
            for (int x = 0; x < cell; x++) {
                for (int y = 0; y < cell; y++) {
                    int argb = icons.getRGB(2 * cell + x, 2 * cell + y);
                    if ((argb >>> 24) > 0) {
                        opaque++;
                        if (((argb >> 16) & 255) > 2 * ((argb >> 8) & 255)) {
                            red++;
                        }
                    }
                }
            }
            c.eq(32, icons.getWidth(), "map_icons.png is 1.12.2's size");
            c.ok(opaque > 8 && opaque < cell * cell && red > 4, "slot 10 of map_icons.png holds a red X: " + opaque + " drawn, " + red + " red");
        } catch (Exception e) {
            c.fail("map_icons.png: " + e);
        }
    }

    private void placement(String kind, int spacing, int separation, int salt) {
        ScatteredStructure s = ScatteredStructure.of(world, kind);
        if (s == null) {
            return;
        }
        ScatteredPlacement p = s.placement();
        c.eq(spacing + "/" + separation + "/" + salt, p.spacing() + "/" + p.separation() + "/" + p.salt(),
                kind + " spacing/separation/salt");
    }

    // ---- biomes

    private void biomes() {
        c.ok(StructureBiomes.isBeach(Biomes.BEACH), "beach is #is_beach");
        c.ok(StructureBiomes.isBeach(Biomes.COLD_BEACH), "snowy beach is #is_beach");
        c.ok(!StructureBiomes.isBeach(Biomes.STONE_BEACH), "stone beach is not #is_beach");
        c.ok(!StructureBiomes.isBeach(Biomes.MUSHROOM_ISLAND_SHORE), "mushroom shore is not #is_beach");
        c.ok(!StructureBiomes.isBeach(Biomes.PLAINS), "plains is not a beach");
        c.ok(!StructureBiomes.isBeach(Biomes.OCEAN), "ocean is not a beach");
        Map<String, Boolean> warm = new LinkedHashMap<String, Boolean>();
        warm.put("updateaquaticbackport:warm_ocean", true);
        warm.put("updateaquaticbackport:lukewarm_ocean", true);
        warm.put("updateaquaticbackport:deep_lukewarm_ocean", true);
        warm.put("updateaquaticbackport:cold_ocean", false);
        warm.put("updateaquaticbackport:deep_cold_ocean", false);
        warm.put("updateaquaticbackport:frozen_ocean", false);
        warm.put("updateaquaticbackport:deep_frozen_ocean", false);
        warm.put("minecraft:frozen_ocean", false);
        int found = 0;
        for (Map.Entry<String, Boolean> e : warm.entrySet()) {
            Biome b = ForgeRegistries.BIOMES.getValue(new ResourceLocation(e.getKey()));
            if (b == null || !new ResourceLocation(e.getKey()).equals(b.getRegistryName())) {
                continue;
            }
            found++;
            OceanKind kind = OceanKind.of(b, world.getSeed(), 0, 0);
            c.eq(e.getValue(), StructureBiomes.warmRuin(kind), e.getKey() + ": warm ruin (else cold)");
        }
        c.ok(found >= 7, "the ocean stream's seven biomes are there: " + found);
        // With the ocean stream, vanilla's ocean and deep ocean are ordinary (cold-ruin) oceans.
        c.eq(Boolean.FALSE, StructureBiomes.warmRuin(OceanKind.of(Biomes.DEEP_OCEAN, world.getSeed(), 0, 0)), "deep ocean: cold ruin");
        c.eq(null, StructureBiomes.warmRuin(OceanKind.of(Biomes.RIVER, world.getSeed(), 0, 0)), "river: no ruin");
        c.eq(null, StructureBiomes.warmRuin(OceanKind.of(Biomes.BEACH, world.getSeed(), 0, 0)), "beach: no ruin");
    }

    // ---- structure files

    private List<ResourceLocation> allFiles() {
        List<ResourceLocation> all = new ArrayList<ResourceLocation>();
        Set<ResourceLocation> seen = new HashSet<ResourceLocation>();
        for (ResourceLocation[] group : new ResourceLocation[][]{MapGenShipwreck.OCEAN, MapGenShipwreck.BEACHED,
                MapGenOceanRuin.WARM, MapGenOceanRuin.BRICK, MapGenOceanRuin.CRACKED, MapGenOceanRuin.MOSSY,
                MapGenOceanRuin.BIG_BRICK, MapGenOceanRuin.BIG_CRACKED, MapGenOceanRuin.BIG_MOSSY, MapGenOceanRuin.BIG_WARM}) {
            for (ResourceLocation id : group) {
                if (seen.add(id)) {
                    all.add(id);
                }
            }
        }
        return all;
    }

    private void templates() {
        List<ResourceLocation> all = allFiles();
        c.eq(68, all.size(), "files named by the structures (20 shipwrecks, 48 ruins)");
        Map<String, Integer> markers = new TreeMap<String, Integer>();
        Random random = new Random(42);
        int placedFiles = 0;
        int drownedSpawned = 0;
        Item heart = Owners.ours("heart_of_the_sea");
        boolean drownedExists = EntityList.isRegistered(Content.id("drowned"));
        for (ResourceLocation id : all) {
            ModernTemplate t = ModernTemplate.load(id);
            if (!c.ok(!t.isEmpty(), id + " loads")) {
                continue;
            }
            c.ok(t.unresolvedBlocks().isEmpty(), id + " resolves every block: " + t.unresolvedBlocks());
            for (ModernTemplate.DataMarker m : t.dataMarkers()) {
                markers.merge(m.metadata, 1, Integer::sum);
            }
            BlockPos size = t.size();
            if (!c.ok(size.getX() <= 32 && size.getZ() <= 32 && size.getY() <= 50, id + " fits a test area: " + size)) {
                continue;
            }
            BlockPos corner = c.area();
            boolean wreck = id.getPath().startsWith("shipwreck/");
            TemplatePiece piece = wreck
                    ? new ShipwreckPiece(id, corner, Rotation.NONE, false, random)
                    : new OceanRuinPiece(id, corner, Rotation.NONE, 1.0F, id.getPath().contains("warm"),
                    id.getPath().contains("big_"), random);
            StructureBoundingBox box = piece.getBoundingBox();
            piece.addComponentParts(world, random, box);
            int palette = paletteOf(piece);
            int[] match = match(t, corner, Rotation.NONE, ShipwreckPieceInfo.pivot(wreck), palette);
            if (c.ok(match[1] > 0 && match[0] * 100 >= match[1] * 99,
                    id + " places its blocks: " + match[0] + " of " + match[1])) {
                placedFiles++;
            }
            // Markers filled.
            for (ModernTemplate.DataMarker m : t.dataMarkers()) {
                BlockPos at = corner.add(m.pos);
                if (wreck) {
                    ResourceLocation table = "map_chest".equals(m.metadata) ? StructureContent.SHIPWRECK_MAP
                            : "treasure_chest".equals(m.metadata) ? StructureContent.SHIPWRECK_TREASURE
                            : "supply_chest".equals(m.metadata) ? StructureContent.SHIPWRECK_SUPPLY : null;
                    if (table != null) {
                        c.eq(table, lootOf(at.down()), id + " " + m.metadata + " chest below the marker");
                    }
                } else if ("chest".equals(m.metadata)) {
                    c.eq(id.getPath().contains("big_") ? StructureContent.UNDERWATER_RUIN_BIG
                            : StructureContent.UNDERWATER_RUIN_SMALL, lootOf(at), id + " chest marker becomes a ruin chest");
                } else if ("drowned".equals(m.metadata)) {
                    c.eq(Blocks.AIR, world.getBlockState(at).getBlock(), id + " drowned marker above sea level becomes air");
                    List<EntityLiving> near = world.getEntitiesWithinAABB(EntityLiving.class, new AxisAlignedBB(at).grow(1));
                    boolean found = false;
                    for (EntityLiving e : near) {
                        if (Content.id("drowned").equals(EntityList.getKey(e))) {
                            found = true;
                            c.ok(e.isNoDespawnRequired(), id + " drowned is persistent");
                            e.setDead();
                        }
                    }
                    if (drownedExists) {
                        c.ok(found, id + " drowned marker spawns a drowned");
                        drownedSpawned += found ? 1 : 0;
                    }
                }
            }
            // No structure or jigsaw block is placed.
            int stray = 0;
            for (BlockPos p : BlockPos.getAllInBox(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
                Block b = world.getBlockState(p).getBlock();
                if (b == Blocks.STRUCTURE_BLOCK || b == Blocks.STRUCTURE_VOID) {
                    stray++;
                }
            }
            c.eq(0, stray, id + " leaves no structure blocks");
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST structure: {} files placed, markers {}, drowned spawned {}", placedFiles, markers, drownedSpawned);
        c.eq(68, placedFiles, "every file places");
        c.ok(markers.containsKey("supply_chest") && markers.containsKey("map_chest") && markers.containsKey("treasure_chest"),
                "shipwreck files carry the three chest markers: " + markers);
        c.ok(markers.containsKey("chest") && markers.containsKey("drowned"), "ruin files carry chest and drowned markers: " + markers);
        c.ok(heart != null, "the heart of the sea exists (conduit stream on)");
    }

    /** Pivots by kind: shipwrecks turn around (4, 0, 15), ruins around their corner. */
    private static final class ShipwreckPieceInfo {
        static BlockPos pivot(boolean wreck) {
            return wreck ? new BlockPos(4, 0, 15) : BlockPos.ORIGIN;
        }
    }

    private static int paletteOf(StructureComponent piece) {
        NBTTagCompound tag = piece.createStructureBaseNBT();
        return tag.getInteger("Palette");
    }

    /**
     * @return {matching, total}: the file's blocks other than air, structure blocks and voids, and how many of
     * them are in the world as the same block
     */
    private int[] match(ModernTemplate t, BlockPos origin, Rotation rotation, BlockPos pivot, int palette) {
        TemplatePlacement placement = TemplatePlacement.of(rotation, Mirror.NONE).pivot(pivot);
        BlockPos size = t.size();
        int total = 0;
        int same = 0;
        for (int x = 0; x < size.getX(); x++) {
            for (int y = 0; y < size.getY(); y++) {
                for (int z = 0; z < size.getZ(); z++) {
                    BlockPos local = new BlockPos(x, y, z);
                    TemplateBlock b = t.blockAt(local, palette);
                    if (b == null || b.state() == null || b.name().equals("minecraft:air")
                            || b.name().equals("minecraft:structure_block") || b.name().equals("minecraft:structure_void")
                            || b.name().equals("minecraft:jigsaw")) {
                        continue;
                    }
                    total++;
                    if (world.getBlockState(ModernTemplate.worldPos(origin, local, placement)).getBlock() == b.state().getBlock()) {
                        same++;
                    }
                }
            }
        }
        return new int[]{same, total};
    }

    private ResourceLocation lootOf(BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof TileEntityChest ? ((TileEntityChest) tile).getLootTable() : null;
    }

    // ---- loot

    private void loot() {
        FakePlayer player = c.player("structure-loot", world.getSpawnPoint());
        LootContext context = new LootContext.Builder(world).withPlayer(player).withLuck(0).build();
        Item heart = Owners.ours("heart_of_the_sea");
        boolean treasureNear = Structures.nearest(world, Structures.BURIED_TREASURE, player.getPosition(), 50) != null;

        Map<ResourceLocation, Set<String>> expected = new LinkedHashMap<ResourceLocation, Set<String>>();
        expected.put(StructureContent.BURIED_TREASURE, names("iron_ingot", "gold_ingot", "tnt", "emerald", "diamond",
                "prismarine_crystals", "leather_chestplate", "iron_sword", "cooked_fish", "potion",
                "updateaquaticbackport:heart_of_the_sea"));
        expected.put(StructureContent.SHIPWRECK_SUPPLY, names("paper", "potato", "poisonous_potato", "carrot", "wheat",
                "coal", "rotten_flesh", "pumpkin", "gunpowder", "tnt", "leather_helmet", "leather_chestplate",
                "leather_leggings", "leather_boots"));
        expected.put(StructureContent.SHIPWRECK_TREASURE, names("iron_ingot", "gold_ingot", "emerald", "diamond",
                "experience_bottle", "iron_nugget", "gold_nugget", "dye"));
        expected.put(StructureContent.UNDERWATER_RUIN_BIG, names("coal", "gold_nugget", "emerald", "wheat",
                "golden_apple", "book", "enchanted_book", "leather_chestplate", "golden_helmet", "fishing_rod", "filled_map"));
        expected.put(StructureContent.UNDERWATER_RUIN_SMALL, names("coal", "stone_axe", "rotten_flesh", "emerald",
                "wheat", "leather_chestplate", "golden_helmet", "fishing_rod", "filled_map"));
        expected.put(StructureContent.SHIPWRECK_MAP, names("filled_map", "compass", "map", "clock", "paper", "feather", "book"));

        for (Map.Entry<ResourceLocation, Set<String>> e : expected.entrySet()) {
            LootTable table = world.getLootTableManager().getLootTableFromLocation(e.getKey());
            if (!c.ok(table != LootTable.EMPTY_LOOT_TABLE, e.getKey() + " loads")) {
                continue;
            }
            boolean maps = e.getKey().equals(StructureContent.SHIPWRECK_MAP) || e.getKey().getPath().startsWith("chests/underwater_ruin");
            int rolls = maps ? 12 : 300;
            Map<String, Integer> seen = new TreeMap<String, Integer>();
            int unexpected = 0;
            int hearts = 0;
            int min = Integer.MAX_VALUE;
            int max = 0;
            for (int i = 0; i < rolls; i++) {
                List<ItemStack> stacks = table.generateLootForPools(new Random(i * 31L + 7), context);
                min = Math.min(min, stacks.size());
                max = Math.max(max, stacks.size());
                for (ItemStack s : stacks) {
                    String n = String.valueOf(s.getItem().getRegistryName());
                    String shortName = n.startsWith("minecraft:") ? n.substring(10) : n;
                    seen.merge(shortName, 1, Integer::sum);
                    if (!e.getValue().contains(shortName)) {
                        unexpected++;
                        c.fail(e.getKey() + " gave " + n);
                    }
                    if (heart != null && s.getItem() == heart) {
                        hearts++;
                    }
                    if (s.getItem() == Items.POTIONITEM) {
                        c.eq("minecraft:water_breathing", s.getTagCompound() == null ? null : s.getTagCompound().getString("Potion"),
                                "buried treasure potions are water breathing");
                    }
                    if (s.getItem() == Items.FILLED_MAP) {
                        treasureMap(s, player.getPosition());
                    }
                    if (s.getItem() == Items.FISH) {
                        c.ok(false, "raw fish never appears");
                    }
                }
            }
            UpdateAquaticBackport.LOGGER.info("SELFTEST structure loot {}: {} rolls, {}-{} stacks, {}", e.getKey(), rolls, min, max, seen);
            c.eq(0, unexpected, e.getKey() + " gives only today's items 1.12.2 has");
            c.ok(seen.size() >= Math.min(e.getValue().size() - 2, 5), e.getKey() + " gives a spread of items: " + seen.keySet());
            if (e.getKey().equals(StructureContent.BURIED_TREASURE)) {
                if (heart != null) {
                    c.eq(rolls, hearts, "every buried treasure has exactly one heart of the sea");
                }
                c.ok(seen.containsKey("potion") && seen.containsKey("cooked_fish"), "buried treasure has potions and cooked fish");
            }
            if (e.getKey().equals(StructureContent.SHIPWRECK_MAP)) {
                c.eq(treasureNear ? rolls : 0, seen.getOrDefault("filled_map", 0),
                        "every map chest has one buried treasure map when one is within 50 chunks (" + treasureNear + ")");
            }
            if (e.getKey().equals(StructureContent.SHIPWRECK_SUPPLY)) {
                // 3-10 rolls; today's moss, suspicious stew and bamboo (19 of 86 weight) roll nothing here.
                c.ok(max <= 10, "supply chests roll at most 10 stacks: " + min + "-" + max);
            }
        }
    }

    private static Set<String> names(String... names) {
        Set<String> out = new HashSet<String>();
        for (String n : names) {
            out.add(n);
        }
        return out;
    }

    /** A buried treasure map: centered on a real buried treasure, zoom 1, the red X on it, today's name. */
    private void treasureMap(ItemStack map, BlockPos from) {
        NBTTagCompound tag = map.getTagCompound();
        if (!c.ok(tag != null && tag.hasKey("Decorations"), "treasure map has a decoration")) {
            return;
        }
        NBTTagList decorations = tag.getTagList("Decorations", 10);
        c.eq(1, decorations.tagCount(), "one decoration");
        NBTTagCompound d = decorations.getCompoundTagAt(0);
        c.eq(StructureContent.redX().getIcon(), d.getByte("type"), "the decoration is the red X");
        c.eq("filled_map.buried_treasure", tag.getCompoundTag("display").getString("LocName"), "named Buried Treasure Map by key");
        int x = (int) d.getDouble("x");
        int z = (int) d.getDouble("z");
        c.eq(9, x & 15, "the X is on block 9 of its chunk (x)");
        c.eq(9, z & 15, "the X is on block 9 of its chunk (z)");
        ScatteredStructure treasure = ScatteredStructure.of(world, Structures.BURIED_TREASURE);
        c.ok(treasure != null && treasure.startsAt(world, x >> 4, z >> 4), "the X marks a buried treasure chunk");
        MapData data = Items.FILLED_MAP.getMapData(map, world);
        if (c.ok(data != null, "the map has its data")) {
            c.eq((byte) 1, data.scale, "zoom 1");
            int half = 64 << data.scale;
            c.ok(Math.abs(data.xCenter - x) <= half && Math.abs(data.zCenter - z) <= half, "the X is on the map");
            c.ok(data.trackingPosition && data.unlimitedTracking, "tracks position without limit, as today's");
        }
        c.ok(Math.abs(x - from.getX()) <= 50 * 16 + 16 && Math.abs(z - from.getZ()) <= 50 * 16 + 16, "within 50 chunks");
    }

    // ---- real generation

    private static final class Found {
        final String kind;
        final String label;
        final int chunkX;
        final int chunkZ;

        Found(String kind, String label, int chunkX, int chunkZ) {
            this.kind = kind;
            this.label = label;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }
    }

    private void generation() {
        BlockPos spawn = world.getSpawnPoint();
        Map<String, Found> want = new LinkedHashMap<String, Found>();
        scan(Structures.SHIPWRECK, spawn, 40, 0, want,
                cx -> StructureBiomes.isBeach(world, cx[0], cx[1]) ? "beached shipwreck" : "sunken shipwreck");
        // Ten ruins: enough for large ones (0.3) with their clusters.
        scan(Structures.OCEAN_RUIN, spawn, 40, 6, want, cx -> {
            Boolean w = StructureBiomes.warmRuin(StructureBiomes.ocean(world, cx[0], cx[1]));
            Biome b = StructureBiomes.middle(world, cx[0], cx[1]);
            boolean ours = b.getRegistryName() != null && "updateaquaticbackport".equals(b.getRegistryName().getNamespace());
            return (Boolean.TRUE.equals(w) ? "warm ruin" : "cold ruin") + (ours ? " (1.13 biome)" : "");
        });
        // Buried treasure: every chunk is a candidate; look outward ring by ring for two.
        ScatteredStructure treasure = ScatteredStructure.of(world, Structures.BURIED_TREASURE);
        int sx = spawn.getX() >> 4;
        int sz = spawn.getZ() >> 4;
        int treasures = 0;
        for (int ring = 0; ring < 400 && treasures < 2; ring++) {
            for (int dx = -ring; dx <= ring && treasures < 2; dx++) {
                for (int dz = -ring; dz <= ring && treasures < 2; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    if (treasure.startsAt(world, sx + dx, sz + dz)) {
                        want.put("buried treasure " + treasures, new Found(Structures.BURIED_TREASURE, "buried treasure", sx + dx, sz + dz));
                        treasures++;
                    }
                }
            }
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST structure: starts found {}", want.keySet());
        c.ok(want.containsKey("sunken shipwreck"), "a sunken shipwreck within 40 regions");
        c.ok(want.containsKey("beached shipwreck"), "a beached shipwreck within 40 regions");
        c.ok(want.containsKey("warm ruin") || want.containsKey("warm ruin (1.13 biome)"), "a warm ruin within 40 regions");
        c.ok(want.containsKey("cold ruin") || want.containsKey("cold ruin (1.13 biome)"), "a cold ruin within 40 regions");
        c.ok(want.containsKey("warm ruin (1.13 biome)") || want.containsKey("cold ruin (1.13 biome)"), "a ruin in the ocean stream's biomes");
        c.eq(2, treasures, "two buried treasures found");

        int checked = 0;
        for (Map.Entry<String, Found> e : want.entrySet()) {
            checked += built(e.getKey(), e.getValue()) ? 1 : 0;
        }
        c.eq(want.size(), checked, "every structure found was built and checked");
        UpdateAquaticBackport.LOGGER.info("SELFTEST structure ruins: {} large, {} clustered", this.largeRuins, this.clusters);
        c.ok(this.largeRuins > 0, "a large ruin among those built: " + this.largeRuins);
        c.ok(this.clusters > 0, "a large ruin with a cluster of small ones: " + this.clusters);
    }

    private interface Classifier {
        String label(int[] chunk);
    }

    /** The candidates of a kind's regions around spawn, nearest first; keeps the first of each label. */
    private void scan(String kind, BlockPos from, int regions, int extra, Map<String, Found> out, Classifier classify) {
        int extras = 0;
        ScatteredStructure s = ScatteredStructure.of(world, kind);
        if (s == null) {
            return;
        }
        ScatteredPlacement p = s.placement();
        int rx = Math.floorDiv(from.getX() >> 4, p.spacing());
        int rz = Math.floorDiv(from.getZ() >> 4, p.spacing());
        for (int ring = 0; ring <= regions; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                        continue;
                    }
                    ChunkPos cand = p.candidate(world.getSeed(), (rx + dx) * p.spacing(), (rz + dz) * p.spacing());
                    if (!s.startsAt(world, cand.x, cand.z)) {
                        continue;
                    }
                    String label = classify.label(new int[]{cand.x, cand.z});
                    if (!out.containsKey(label)) {
                        out.put(label, new Found(kind, label, cand.x, cand.z));
                    } else if (extras < extra) {
                        extras++;
                        out.put(label + " #" + extras, new Found(kind, label, cand.x, cand.z));
                    }
                }
            }
        }
    }

    /** Generates the chunks around a start, then checks the built structure. */
    private boolean built(String name, Found f) {
        int r = Structures.BURIED_TREASURE.equals(f.kind) ? 2 : 5;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                world.getChunk(f.chunkX + dx, f.chunkZ + dz);
            }
        }
        String save = Structures.SHIPWRECK.equals(f.kind) ? "UAB_Shipwreck"
                : Structures.OCEAN_RUIN.equals(f.kind) ? "UAB_OceanRuin" : "UAB_BuriedTreasure";
        MapGenStructureData data = (MapGenStructureData) world.getPerWorldStorage().getOrLoadData(MapGenStructureData.class, save);
        if (!c.ok(data != null, name + ": " + save + ".dat exists")) {
            return false;
        }
        NBTTagCompound start = data.getTagCompound().getCompoundTag(MapGenStructureData.formatChunkCoords(f.chunkX, f.chunkZ));
        if (!c.ok(!start.isEmpty(), name + " at chunk " + f.chunkX + ", " + f.chunkZ + " is laid out and saved")) {
            return false;
        }
        NBTTagList children = start.getTagList("Children", 10);
        c.ok(children.tagCount() > 0, name + " has pieces: " + children.tagCount());
        int[] bb = start.getIntArray("BB");
        StructureBoundingBox box = new StructureBoundingBox(bb);
        // Every chunk it covers was populated, so all of it is built.
        boolean populated = true;
        for (int cx = box.minX >> 4; cx <= box.maxX >> 4; cx++) {
            for (int cz = box.minZ >> 4; cz <= box.maxZ >> 4; cz++) {
                Chunk chunk = world.getChunk(cx, cz);
                populated &= chunk.isTerrainPopulated();
            }
        }
        c.ok(populated, name + ": every chunk it covers is populated");
        c.ok(Math.abs(box.minX - (f.chunkX << 4)) <= 8 * 16 && Math.abs(box.minZ - (f.chunkZ << 4)) <= 8 * 16,
                name + " stays within 8 chunks of its start");

        // Saved pieces read back to the same tag.
        for (int i = 0; i < children.tagCount(); i++) {
            NBTTagCompound tag = children.getCompoundTagAt(i);
            StructureComponent piece = MapGenStructureIO.getStructureComponent(tag, world);
            if (c.ok(piece != null, name + " piece " + tag.getString("id") + " loads from its save")) {
                c.eq(tag, piece.createStructureBaseNBT(), name + " piece saves the same after loading");
            }
        }

        // The locator finds it.
        BlockPos center = new BlockPos((box.minX + box.maxX) / 2, 64, (box.minZ + box.maxZ) / 2);
        BlockPos located = Structures.nearest(world, f.kind, center, 4);
        if (c.ok(located != null, name + ": the locator finds it")) {
            c.ok(located.getX() >> 4 == f.chunkX || box.isVecInside(new BlockPos(located.getX(), box.minY, located.getZ())),
                    name + ": located at " + located + " in its box " + box);
        }

        if (Structures.BURIED_TREASURE.equals(f.kind)) {
            return treasureBuilt(name, f);
        }
        predictedGround(name, box, Structures.SHIPWRECK.equals(f.kind) && children.getCompoundTagAt(0).getBoolean("isBeached"));
        Map<ResourceLocation, Integer> chests = new HashMap<ResourceLocation, Integer>();
        int otherChests = 0;
        for (BlockPos p : BlockPos.getAllInBox(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
            TileEntity tile = world.getTileEntity(p);
            if (tile instanceof TileEntityChest) {
                ResourceLocation table = ((TileEntityChest) tile).getLootTable();
                if (table == null) {
                    otherChests++;
                } else {
                    chests.merge(table, 1, Integer::sum);
                }
            }
        }
        // Each piece against its file, as placed (turned, with its palette and integrity).
        int pieces = 0;
        int sameTotal = 0;
        int allTotal = 0;
        boolean wreck = Structures.SHIPWRECK.equals(f.kind);
        for (int i = 0; i < children.tagCount(); i++) {
            NBTTagCompound tag = children.getCompoundTagAt(i);
            ModernTemplate t = ModernTemplate.load(new ResourceLocation(tag.getString("Template")));
            BlockPos origin = new BlockPos(tag.getInteger("TPX"), tag.getInteger("TPY"), tag.getInteger("TPZ"));
            Rotation rot = Rotation.valueOf(tag.getString("Rot"));
            BlockPos pivot = new BlockPos(tag.getInteger("PvX"), tag.getInteger("PvY"), tag.getInteger("PvZ"));
            int[] m = match(t, origin, rot, pivot, tag.getInteger("Palette"));
            float integrity = tag.getFloat("Integrity");
            UpdateAquaticBackport.LOGGER.info("SELFTEST structure {}: {} at {} {} integrity {}: {} of {} blocks",
                    name, tag.getString("Template"), origin, rot, integrity, m[0], m[1]);
            double need = wreck ? 0.95 : 0.4;
            c.ok(m[1] > 0 && m[0] >= m[1] * need, name + " " + tag.getString("Template") + " built: " + m[0] + " of " + m[1]);
            pieces++;
            sameTotal += m[0];
            allTotal += m[1];
        }
        Biome biome = StructureBiomes.middle(world, f.chunkX, f.chunkZ);
        UpdateAquaticBackport.LOGGER.info("SELFTEST structure {}: chunk {}, {} in {}, box {}, {} pieces, {} of {} blocks, chests {}, {} without a table",
                name, f.chunkX + "," + f.chunkZ, f.kind, biome.getRegistryName(), box, pieces, sameTotal, allTotal, chests, otherChests);
        if (wreck) {
            int wrecks = chests.getOrDefault(StructureContent.SHIPWRECK_MAP, 0) + chests.getOrDefault(StructureContent.SHIPWRECK_SUPPLY, 0)
                    + chests.getOrDefault(StructureContent.SHIPWRECK_TREASURE, 0);
            c.ok(wrecks > 0, name + " has shipwreck chests: " + chests);
            c.eq(0, otherChests, name + ": every chest has its table");
            NBTTagCompound piece = children.getCompoundTagAt(0);
            c.eq(name.startsWith("beached"), piece.getBoolean("isBeached"), name + " beached flag");
            if (!name.startsWith("beached")) {
                c.ok(box.minY < world.getSeaLevel(), name + " sits under the sea: " + box.minY);
            }
        } else {
            boolean large = children.getCompoundTagAt(0).getBoolean("IsLarge");
            int ruinChests = chests.getOrDefault(StructureContent.UNDERWATER_RUIN_BIG, 0) + chests.getOrDefault(StructureContent.UNDERWATER_RUIN_SMALL, 0);
            int drowned = 0;
            for (Entity e : world.getEntitiesWithinAABB(EntityLiving.class,
                    new AxisAlignedBB(box.minX, box.minY, box.minZ, box.maxX + 1, box.maxY + 1, box.maxZ + 1))) {
                if (Content.id("drowned").equals(EntityList.getKey(e))) {
                    drowned++;
                    c.ok(((EntityLiving) e).isNoDespawnRequired(), name + " drowned is persistent");
                }
            }
            UpdateAquaticBackport.LOGGER.info("SELFTEST structure {}: large {}, {} ruin chests, {} drowned", name, large, ruinChests, drowned);
            if (large) {
                this.largeRuins++;
                int own = name.startsWith("warm") ? 1 : 3;
                if (children.tagCount() > own) {
                    this.clusters++;
                    int small = 0;
                    for (int i = own; i < children.tagCount(); i++) {
                        small += children.getCompoundTagAt(i).getBoolean("IsLarge") ? 0 : 1;
                    }
                    c.eq(children.tagCount() - own, small, name + ": the cluster is small ruins");
                    c.ok((children.tagCount() - own) / own <= 8, name + ": at most eight in the cluster");
                }
            }
            c.eq(Boolean.valueOf(name.startsWith("warm")), Boolean.valueOf("WARM".equals(children.getCompoundTagAt(0).getString("BiomeType"))),
                    name + " biome type saved");
            c.ok(box.minY < world.getSeaLevel(), name + " sits under the sea: " + box.minY);
        }
        return true;
    }

    /**
     * The layout's heights came from Core's prediction; around the box (where the structure did not build)
     * the real ground must agree: OCEAN_FLOOR_WG for sunken ones, WORLD_SURFACE_WG for beached ones.
     */
    private void predictedGround(String name, StructureBoundingBox box, boolean surface) {
        com.exiledradio.backportcore.terrain.TerrainHeights h = com.exiledradio.backportcore.terrain.TerrainHeights.forWorld(world);
        if (!c.ok(h != null, "terrain heights for this world")) {
            return;
        }
        int columns = 0;
        int agree = 0;
        for (int x = box.minX - 3; x <= box.maxX + 3; x++) {
            for (int z = box.minZ - 3; z <= box.maxZ + 3; z++) {
                boolean ring = x == box.minX - 3 || x == box.maxX + 3 || z == box.minZ - 3 || z == box.maxZ + 3;
                if (!ring) {
                    continue;
                }
                int predicted = surface ? h.height(x, z) : h.floor(x, z);
                BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, 255, z);
                while (p.getY() > 0) {
                    IBlockState st = world.getBlockState(p);
                    boolean stops = surface ? st.getMaterial() != net.minecraft.block.material.Material.AIR
                            : st.getMaterial().blocksMovement();
                    if (stops && !(st.getBlock() instanceof net.minecraft.block.BlockLeaves) && !(st.getBlock() instanceof net.minecraft.block.BlockLog)) {
                        break;
                    }
                    p.setY(p.getY() - 1);
                }
                columns++;
                agree += Math.abs(p.getY() + 1 - predicted) <= 1 ? 1 : 0;
            }
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST structure {}: predicted ground agrees in {} of {} columns around it", name, agree, columns);
        c.ok(agree * 10 >= columns * 7, name + ": predicted ground matches the built world around it: " + agree + " of " + columns);
    }

    private boolean treasureBuilt(String name, Found f) {
        int x = (f.chunkX << 4) + 9;
        int z = (f.chunkZ << 4) + 9;
        BlockPos chest = null;
        for (int y = 1; y < 128; y++) {
            BlockPos p = new BlockPos(x, y, z);
            if (world.getBlockState(p).getBlock() == Blocks.CHEST) {
                chest = p;
                break;
            }
        }
        if (!c.ok(chest != null, name + ": a chest in column " + x + ", " + z)) {
            return false;
        }
        c.eq(StructureContent.BURIED_TREASURE, lootOf(chest), name + " chest has the buried treasure table");
        IBlockState below = world.getBlockState(chest.down());
        boolean host = below == Blocks.SANDSTONE.getDefaultState() || below.getBlock() == Blocks.STONE;
        c.ok(host, name + " chest stands on sandstone or stone: " + below);
        // Today's rule: no air, still water or still lava left beside it.
        int open = 0;
        for (net.minecraft.util.EnumFacing side : net.minecraft.util.EnumFacing.values()) {
            IBlockState s = world.getBlockState(chest.offset(side));
            if (s.getMaterial() == net.minecraft.block.material.Material.AIR || s == Blocks.WATER.getDefaultState()
                    || s == Blocks.LAVA.getDefaultState()) {
                open++;
            }
        }
        c.eq(0, open, name + " chest is walled in");
        c.ok(chest.getY() < world.getHeight(x, z), name + " chest is buried: y " + chest.getY());
        UpdateAquaticBackport.LOGGER.info("SELFTEST structure {}: chest at {} in {}, below {}", name, chest,
                world.getBiome(chest).getRegistryName(), below);
        return true;
    }
}
