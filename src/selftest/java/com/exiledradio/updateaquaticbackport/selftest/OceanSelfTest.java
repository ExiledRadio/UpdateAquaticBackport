package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.ocean.BiomeAquaticOcean;
import com.exiledradio.updateaquaticbackport.ocean.BiomeFrozenOcean;
import com.exiledradio.updateaquaticbackport.ocean.Icebergs;
import com.exiledradio.updateaquaticbackport.ocean.OceanChunks;
import com.exiledradio.updateaquaticbackport.ocean.OceanContent;
import com.exiledradio.updateaquaticbackport.ocean.OceanTerrain;
import com.exiledradio.updateaquaticbackport.ocean.UnderwaterCarvers;

import net.minecraft.block.Block;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.biome.BiomeProviderSingle;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraft.world.storage.WorldInfo;
import net.minecraft.world.gen.ChunkGeneratorOverworld;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.structure.StructureOceanMonument;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Stream O: the seven ocean biomes, the biome map, floors, ice, icebergs, underwater carvers, monuments. */
public final class OceanSelfTest implements SelfTest {

    /** Half-width of the biome map sample, in 4-block cells (8192 blocks across). */
    private static final int SAMPLE = 1024;
    private static final int TILE = 256;

    @Override
    public String name() {
        return "ocean";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        List<Biome> biomes = OceanContent.biomes();
        if (!c.eq(7, biomes.size(), "seven ocean biomes registered")) {
            return;
        }
        registration(c);
        oldChunks(c, world);
        Map<Biome, BlockPos> found = biomeMap(c, world);
        hooks(c, world);
        features(c);
        carver(c, world);
        flowing(c);
        int icebergIce = 0;
        for (Biome biome : biomes) {
            BlockPos at = found.get(biome);
            if (c.ok(at != null, "found " + biome.getRegistryName() + " deep inside itself near spawn")) {
                int packed = realChunks(c, world, (BiomeAquaticOcean) biome, at);
                icebergIce += biome instanceof BiomeFrozenOcean ? packed : 0;
            }
        }
        c.ok(icebergIce > 0, "generated frozen oceans have icebergs: " + icebergIce + " packed ice");
        caves(c, world, found);
        monuments(c, world);
    }

    // ------------------------------------------------------------------------------------------

    private static void registration(Check c) {
        Object[][] expect = {
                {OceanContent.warm(), "warm_ocean", OceanKind.WARM, false, 0.5F},
                {OceanContent.lukewarm(), "lukewarm_ocean", OceanKind.LUKEWARM, false, 0.5F},
                {OceanContent.deepLukewarm(), "deep_lukewarm_ocean", OceanKind.LUKEWARM, true, 0.5F},
                {OceanContent.cold(), "cold_ocean", OceanKind.COLD, false, 0.5F},
                {OceanContent.deepCold(), "deep_cold_ocean", OceanKind.COLD, true, 0.5F},
                {OceanContent.frozen(), "frozen_ocean", OceanKind.FROZEN, false, 0.0F},
                {OceanContent.deepFrozen(), "deep_frozen_ocean", OceanKind.FROZEN, true, 0.5F},
        };
        for (Object[] e : expect) {
            BiomeAquaticOcean biome = (BiomeAquaticOcean) e[0];
            String name = (String) e[1];
            c.eq(UpdateAquaticBackport.MOD_ID + ":" + name, String.valueOf(biome.getRegistryName()), name + " registry name");
            c.eq(e[2], OceanKind.of(biome, 0L, 0, 0), name + " ocean kind");
            c.eq(e[3], biome.isDeep(), name + " deep");
            c.eq((Boolean) e[3] ? -1.8F : -1.0F, biome.getBaseHeight(), name + " depth");
            c.eq(0.1F, biome.getHeightVariation(), name + " scale");
            c.eq(e[4], biome.getDefaultTemperature(), name + " temperature");
            c.ok(BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN), name + " is an ocean in the biome dictionary");
            c.ok(BiomeManager.oceanBiomes.contains(biome), name + " in Forge's ocean list");
            c.ok(Biome.getIdForBiome(biome) >= 0 && Biome.getBiome(Biome.getIdForBiome(biome)) == biome, name + " has an id");
            boolean squid = false;
            for (Biome.SpawnListEntry s : biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.WATER_CREATURE)) {
                squid |= s.entityClass == EntitySquid.class;
            }
            c.ok(squid, name + " spawns squid");
            boolean bears = false;
            for (Biome.SpawnListEntry s : biome.getSpawnableList(net.minecraft.entity.EnumCreatureType.CREATURE)) {
                bears |= s.entityClass == EntityPolarBear.class;
            }
            c.eq(e[2] == OceanKind.FROZEN, bears, name + " polar bears only in frozen oceans");
        }
        c.ok(BiomeDictionary.hasType(OceanContent.warm(), BiomeDictionary.Type.HOT), "warm ocean is HOT");
        c.ok(BiomeDictionary.hasType(OceanContent.frozen(), BiomeDictionary.Type.SNOWY), "frozen ocean is SNOWY");
        c.ok(OceanContent.frozen().getEnableSnow(), "frozen ocean snows");
        c.ok(!OceanContent.deepFrozen().getEnableSnow(), "deep frozen ocean rains");
        // Without a world, a vanilla ocean is what a map without 1.13's temperatures has: the noise.
        for (int i = 0; i < 200; i++) {
            int x = i * 977 - 90000, z = i * 613 - 60000;
            c.eq(OceanKind.fromNoise(1L, x, z), OceanKind.of(Biomes.OCEAN, 1L, x, z), "vanilla ocean by seed alone is the noise at " + x + "," + z);
            c.eq(OceanKind.fromNoise(1L, x, z), OceanKind.of(Biomes.DEEP_OCEAN, 1L, x, z), "vanilla deep ocean too at " + x + "," + z);
        }
        c.ok(ModConfig.oceans.underwaterCaves, "underwater caves on by default");

        // 1.13's patchy temperature: a frozen ocean is 0.0 with 0.2 patches, a deep frozen one 0.5 with 0.2.
        int frozenCold = 0, frozenPatch = 0, deepWarm = 0, deepPatch = 0;
        for (int i = 0; i < 4000; i++) {
            BlockPos p = new BlockPos(i * 7 - 14000, 62, i * 13 - 26000);
            float f = OceanContent.frozen().temperatureAt(p);
            float d = OceanContent.deepFrozen().temperatureAt(p);
            frozenCold += f == 0.0F ? 1 : 0;
            frozenPatch += f == 0.2F ? 1 : 0;
            deepWarm += d == 0.5F ? 1 : 0;
            deepPatch += d == 0.2F ? 1 : 0;
        }
        c.eq(4000, frozenCold + frozenPatch, "frozen ocean temperature only 0.0 or 0.2 at sea level");
        c.ok(frozenCold > 400 && frozenPatch > 400, "frozen ocean has both: " + frozenCold + " cold, " + frozenPatch + " patches");
        c.eq(4000, deepWarm + deepPatch, "deep frozen ocean temperature only 0.5 or 0.2");
        c.ok(OceanContent.frozen().temperatureAt(new BlockPos(0, 120, 0)) < OceanContent.frozen().temperatureAt(new BlockPos(0, 62, 0)) + 0.01F,
                "colder with height");
    }

    // ------------------------------------------------------------------------------------------

    /** Samples the 4-block biome map over 8192 x 8192 blocks: every biome present, the coast rule held. */
    /**
     * Vanilla oceans are the normal temperature only where the map had 1.13's temperatures: chunks made by
     * this world's layered map, and columns not loaded (what the map would make). A chunk loaded from disk
     * without the flag (made before the stream was on) keeps the noise; flat worlds and single-biome
     * providers have no layers. The real two-world run is {@link OceanLegacySelfTest}.
     */
    private static void oldChunks(Check c, WorldServer world) {
        c.ok(OceanChunks.layered(world.getBiomeProvider()), "the overworld's biome map has the temperature layers");
        WorldInfo flatInfo = new WorldInfo(new WorldSettings(world.getSeed(), GameType.SURVIVAL, true, false, WorldType.FLAT), "flat");
        c.ok(!OceanChunks.layered(new BiomeProvider(flatInfo)), "a flat world's map has no temperature layers");
        c.ok(!OceanChunks.layered(new BiomeProviderSingle(Biomes.OCEAN)), "nor a single-biome provider");
        WorldInfo defaultInfo = new WorldInfo(new WorldSettings(world.getSeed(), GameType.SURVIVAL, true, false, WorldType.DEFAULT), "default");
        c.ok(OceanChunks.layered(new BiomeProvider(defaultInfo)), "a new default map has them");

        // A vanilla ocean chunk near spawn, generated now.
        Chunk chunk = null;
        search:
        for (int r = 0; r < 40; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    BlockPos p = new BlockPos(dx * 64 + 8, 0, dz * 64 + 8);
                    if (world.getBiomeProvider().getBiome(p) == Biomes.OCEAN) {
                        Chunk here = world.getChunk(p);
                        if (vanillaOceanColumns(here) > 128) {
                            chunk = here;
                            break search;
                        }
                    }
                }
            }
        }
        if (!c.ok(chunk != null, "a chunk of vanilla ocean near spawn")) {
            return;
        }
        int x0 = chunk.x * 16, z0 = chunk.z * 16;
        c.ok(OceanChunks.loadedFlag(chunk) == null && OceanChunks.realTemperatures(chunk), "a chunk generated now has real temperatures");
        c.eq(OceanKind.NORMAL, kindOfVanillaOcean(world, chunk), "its vanilla ocean is the normal temperature");
        NBTTagCompound saved = new NBTTagCompound();
        OceanChunks.save(new ChunkDataEvent.Save(chunk, saved));
        c.ok(saved.getBoolean(OceanChunks.TAG_NAME), "saving it writes the flag");

        // The same chunk as if it had been loaded from a save made before the stream: no flag.
        OceanChunks.load(new ChunkDataEvent.Load(chunk, new NBTTagCompound()));
        c.ok(!OceanChunks.realTemperatures(chunk), "loaded without the flag, it has no real temperatures");
        int noise = 0, columns = 0;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos p = new BlockPos(x0 + dx, 62, z0 + dz);
                Biome b = world.getBiome(p);
                if (b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN) {
                    columns++;
                    noise += OceanKind.at(world, p) == OceanKind.fromNoise(world.getSeed(), p.getX(), p.getZ()) ? 1 : 0;
                }
            }
        }
        c.eq(columns, noise, "its vanilla ocean columns take the noise");
        saved = new NBTTagCompound();
        OceanChunks.save(new ChunkDataEvent.Save(chunk, saved));
        c.ok(!saved.hasKey(OceanChunks.TAG_NAME), "and saving it again does not make it new");

        NBTTagCompound flagged = new NBTTagCompound();
        flagged.setBoolean(OceanChunks.TAG_NAME, true);
        OceanChunks.load(new ChunkDataEvent.Load(chunk, flagged));
        c.ok(OceanChunks.realTemperatures(chunk), "loaded with the flag, it has them again");
        c.eq(OceanKind.NORMAL, kindOfVanillaOcean(world, chunk), "and its vanilla ocean is normal again");

        // A column far away, not loaded: what the layered map would make there.
        c.ok(world.getChunkProvider().getLoadedChunk(400000 >> 4, 400000 >> 4) == null, "a far column is not loaded");
        c.eq(OceanKind.NORMAL, OceanKind.of(world, Biomes.OCEAN, 400000, 400000), "a vanilla ocean not loaded is normal in a layered world");
    }

    static int vanillaOceanColumns(Chunk chunk) {
        int n = 0;
        for (byte id : chunk.getBiomeArray()) {
            Biome b = Biome.getBiome(id & 255);
            n += b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN ? 1 : 0;
        }
        return n;
    }

    /** @return the one kind every vanilla ocean column of the chunk has, or null if they differ */
    static OceanKind kindOfVanillaOcean(WorldServer world, Chunk chunk) {
        OceanKind kind = null;
        for (int dx = 0; dx < 16; dx++) {
            for (int dz = 0; dz < 16; dz++) {
                BlockPos p = new BlockPos(chunk.x * 16 + dx, 62, chunk.z * 16 + dz);
                Biome b = world.getBiome(p);
                if (b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN) {
                    OceanKind k = OceanKind.at(world, p);
                    if (kind != null && k != kind) {
                        return null;
                    }
                    kind = k;
                }
            }
        }
        return kind;
    }

    private static Map<Biome, BlockPos> biomeMap(Check c, WorldServer world) {
        Map<Integer, Integer> counts = new HashMap<Integer, Integer>();
        Map<Biome, BlockPos> found = new HashMap<Biome, BlockPos>();
        int warmId = Biome.getIdForBiome(OceanContent.warm());
        int frozenId = Biome.getIdForBiome(OceanContent.frozen());
        int coastBreaks = 0;
        int checked = 0;
        int margin = 8;
        for (int tx = -SAMPLE; tx < SAMPLE; tx += TILE) {
            for (int tz = -SAMPLE; tz < SAMPLE; tz += TILE) {
                int w = TILE + 2 * margin;
                Biome[] map = world.getBiomeProvider().getBiomesForGeneration(null, tx - margin, tz - margin, w, w);
                for (int z = margin; z < TILE + margin; z++) {
                    for (int x = margin; x < TILE + margin; x++) {
                        Biome b = map[x + z * w];
                        int id = Biome.getIdForBiome(b);
                        Integer n = counts.get(id);
                        counts.put(id, n == null ? 1 : n + 1);
                        if (id == warmId || id == frozenId) {
                            checked++;
                            for (int dx = -8; dx <= 8; dx += 4) {
                                for (int dz = -8; dz <= 8; dz += 4) {
                                    Biome near = map[(x + dx) + (z + dz) * w];
                                    if (!BiomeDictionary.hasType(near, BiomeDictionary.Type.OCEAN)) {
                                        coastBreaks++;
                                    }
                                }
                            }
                        }
                        if (b instanceof BiomeAquaticOcean && !found.containsKey(b) && x >= 2 * margin && z >= 2 * margin
                                && x < TILE && z < TILE && uniform(map, w, x, z, b, 6)) {
                            found.put(b, new BlockPos((tx - margin + x) * 4 + 2, 0, (tz - margin + z) * 4 + 2));
                        }
                    }
                }
            }
        }
        // Temperature cells are ~2000 blocks across, so a biome may be missing from one 8192-block square:
        // look further out, sparsely, for any not found yet (32768 blocks across).
        for (int tx = -4 * SAMPLE; tx < 4 * SAMPLE && found.size() < OceanContent.biomes().size(); tx += 512) {
            for (int tz = -4 * SAMPLE; tz < 4 * SAMPLE; tz += 512) {
                int w = 64 + 2 * margin;
                Biome[] map = world.getBiomeProvider().getBiomesForGeneration(null, tx - margin, tz - margin, w, w);
                for (int z = margin; z < 64 + margin; z++) {
                    for (int x = margin; x < 64 + margin; x++) {
                        Biome b = map[x + z * w];
                        int id = Biome.getIdForBiome(b);
                        Integer n = counts.get(id);
                        counts.put(id, n == null ? 1 : n + 1);
                        if (b instanceof BiomeAquaticOcean && !found.containsKey(b) && uniform(map, w, x, z, b, 6)) {
                            found.put(b, new BlockPos((tx - margin + x) * 4 + 2, 0, (tz - margin + z) * 4 + 2));
                        }
                    }
                }
            }
        }
        StringBuilder summary = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : counts.entrySet()) {
            Biome b = Biome.getBiome(e.getKey());
            if (b != null && BiomeDictionary.hasType(b, BiomeDictionary.Type.OCEAN)) {
                summary.append(b.getRegistryName()).append('=').append(e.getValue()).append(' ');
            }
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST ocean biome map (4-block cells): {}", summary);
        for (Biome b : OceanContent.biomes()) {
            Integer n = counts.get(Biome.getIdForBiome(b));
            c.ok(n != null && n > 0, b.getRegistryName() + " generates in the biome map");
        }
        c.ok(counts.containsKey(Biome.getIdForBiome(Biomes.OCEAN)), "vanilla ocean still generates (normal temperature)");
        c.ok(counts.containsKey(Biome.getIdForBiome(Biomes.DEEP_OCEAN)), "vanilla deep ocean still generates");
        c.ok(!counts.containsKey(Biome.getIdForBiome(Biomes.FROZEN_OCEAN)), "vanilla frozen ocean never generates");
        c.ok(checked > 0, "warm or frozen cells checked for the coast rule: " + checked);
        c.eq(0, coastBreaks, "warm and frozen oceans never within 32 blocks of land");

        // The block-scale map agrees with the 4-block one where both are uniform.
        int agree = 0;
        for (Map.Entry<Biome, BlockPos> e : found.entrySet()) {
            agree += world.getBiome(e.getValue()) == e.getKey() ? 1 : 0;
        }
        c.eq(found.size(), agree, "block-scale biomes agree with the biome map");
        return found;
    }

    private static boolean uniform(Biome[] map, int w, int x, int z, Biome b, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (map[(x + dx) + (z + dz) * w] != b) {
                    return false;
                }
            }
        }
        return true;
    }

    // ------------------------------------------------------------------------------------------

    private static void hooks(Check c, WorldServer world) {
        IChunkGenerator gen = world.getChunkProvider().chunkGenerator;
        if (!c.ok(gen instanceof ChunkGeneratorOverworld, "overworld generator: " + gen)) {
            return;
        }
        Object ravine = ReflectionHelper.getPrivateValue(ChunkGeneratorOverworld.class, (ChunkGeneratorOverworld) gen, "ravineGenerator", "field_185979_A");
        c.ok(ravine instanceof UnderwaterCarvers, "ravine generator wrapped by the underwater carvers: " + ravine);
        Object monument = ReflectionHelper.getPrivateValue(ChunkGeneratorOverworld.class, (ChunkGeneratorOverworld) gen, "oceanMonumentGenerator", "field_185980_B");
        c.ok(monument instanceof StructureOceanMonument && monument.getClass() != StructureOceanMonument.class,
                "monument placed by 1.13's biome rule: " + monument);
    }

    // ------------------------------------------------------------------------------------------

    /** Icebergs and blue ice placed directly into a pool at sea level. */
    private static void features(Check c) {
        WorldServer world = c.world;
        int sea = world.getSeaLevel();
        Block blueIce = Owners.block(Owners.VILLAGE_AND_PILLAGE, "blue_ice");
        c.ok(blueIce != null, "blue ice available (this mod's copy, or the owner's)");

        int above = 0, below = 0, snow = 0, blue = 0;
        for (int seed = 0; seed < 6; seed++) {
            BlockPos corner = pool(c, sea);
            BlockPos center = corner.add(16, 0, 16);
            Icebergs.iceberg(world, new Random(seed), center, Blocks.PACKED_ICE.getDefaultState(), blueIce);
            for (BlockPos p : BlockPos.getAllInBox(corner.add(0, sea - 30, 0), corner.add(31, sea + 40, 31))) {
                Block b = world.getBlockState(p).getBlock();
                if (b == Blocks.PACKED_ICE) {
                    if (p.getY() >= sea) {
                        above++;
                    } else {
                        below++;
                    }
                } else if (b == Blocks.SNOW) {
                    snow++;
                }
            }
        }
        c.ok(above > 0, "icebergs rise above the sea: " + above + " packed ice");
        c.ok(below > 0, "icebergs reach below it: " + below);
        UpdateAquaticBackport.LOGGER.info("SELFTEST ocean icebergs: {} above, {} below, {} snow blocks", above, below, snow);

        if (blueIce != null) {
            BlockPos corner = pool(c, sea);
            Icebergs.iceberg(world, new Random(7), corner.add(16, 0, 16), blueIce.getDefaultState(), blueIce);
            for (BlockPos p : BlockPos.getAllInBox(corner.add(0, sea - 30, 0), corner.add(31, sea + 40, 31))) {
                blue += world.getBlockState(p).getBlock() == blueIce ? 1 : 0;
            }
            c.ok(blue > 0, "a blue iceberg is blue ice: " + blue);

            corner = pool(c, sea);
            BlockPos ice = corner.add(16, sea - 10, 16);
            world.setBlockState(ice, Blocks.PACKED_ICE.getDefaultState(), 2);
            c.ok(!Icebergs.blueIce(world, new Random(1), corner.add(4, sea - 10, 4), blueIce), "no blue ice away from packed ice");
            c.ok(!Icebergs.blueIce(world, new Random(1), corner.add(16, sea + 2, 15), blueIce), "no blue ice above the sea");
            c.ok(Icebergs.blueIce(world, new Random(1), ice.north(), blueIce), "blue ice grows on packed ice under water");
            int grown = 0;
            for (BlockPos p : BlockPos.getAllInBox(ice.add(-4, -6, -4), ice.add(4, 6, 4))) {
                grown += world.getBlockState(p).getBlock() == blueIce ? 1 : 0;
            }
            c.ok(grown > 1, "and spreads: " + grown + " blocks");
        }
    }

    /** @return the corner of a 32 x 32 pool of still water from sea level - 30 to the surface, open air above */
    private static BlockPos pool(Check c, int sea) {
        BlockPos corner = c.area();
        BlockPos base = new BlockPos(corner.getX(), 0, corner.getZ());
        c.fill(base.add(0, sea - 31, 0), base.add(31, sea - 31, 31), Blocks.STONE.getDefaultState());
        c.fill(base.add(0, sea - 30, 0), base.add(31, sea - 1, 31), Blocks.WATER.getDefaultState());
        c.fill(base.add(0, sea, 0), base.add(31, sea + 59, 31), Blocks.AIR.getDefaultState());
        return base;
    }

    // ------------------------------------------------------------------------------------------

    /** The carvers on a synthetic chunk: stone to y 50, water to the surface. */
    private static void carver(Check c, WorldServer world) {
        int sea = world.getSeaLevel();
        boolean magmaExpected = Owners.ourBlock("bubble_column") != null;
        int chunks = 0;
        int water = 0, obsidian = 0, magma = 0, lava = 0, aboveSea = 0, carvedChunks = 0;
        for (int i = 0; i < 40; i++) {
            ChunkPrimer primer = new ChunkPrimer();
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 1; y <= 50; y++) {
                        primer.setBlockState(x, y, z, Blocks.STONE.getDefaultState());
                    }
                    for (int y = 51; y < sea; y++) {
                        primer.setBlockState(x, y, z, Blocks.WATER.getDefaultState());
                    }
                    for (int y = sea; y < sea + 4; y++) {
                        primer.setBlockState(x, y, z, Blocks.STONE.getDefaultState());
                    }
                }
            }
            int cx = 3000 + i * 3;
            int cz = -2000 + i * 5;
            UnderwaterCarvers.carve(world, cx, cz, primer);
            chunks++;
            int before = water + obsidian + lava;
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    for (int y = 1; y <= 50; y++) {
                        Block b = primer.getBlockState(x, y, z).getBlock();
                        if (b == Blocks.WATER) {
                            water++;
                            c.ok(y > 10, "carved water only above y 10");
                        } else if (b == Blocks.OBSIDIAN) {
                            obsidian++;
                            c.eq(10, y, "obsidian only at y 10");
                        } else if (b == Blocks.MAGMA) {
                            magma++;
                        } else if (b == Blocks.LAVA) {
                            lava++;
                            c.ok(y < 10, "lava only below y 10");
                        }
                    }
                    for (int y = sea; y < sea + 4; y++) {
                        aboveSea += primer.getBlockState(x, y, z).getBlock() != Blocks.STONE ? 1 : 0;
                    }
                }
            }
            carvedChunks += water + obsidian + lava > before ? 1 : 0;
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST ocean carver: {} of {} chunks carved: {} water, {} obsidian, {} magma, {} lava",
                carvedChunks, chunks, water, obsidian, magma, lava);
        c.ok(carvedChunks > 0, "underwater carvers cut into the floor in " + carvedChunks + " of " + chunks + " chunks");
        c.ok(water > 0, "and fill it with water: " + water);
        c.eq(0, aboveSea, "nothing at or above sea level carved");
        if (!magmaExpected) {
            c.eq(0, magma, "no magma without bubble columns");
        }
    }

    /** Noted water beside air is set flowing when its chunk is populated. */
    private static void flowing(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        BlockPos water = corner.add(5, 5, 5);
        world.setBlockState(water, Blocks.WATER.getDefaultState(), 2);
        world.setBlockState(water.down(), Blocks.STONE.getDefaultState(), 2);
        BlockPos sealed = corner.add(9, 5, 9);
        c.fill(sealed.add(-1, -1, -1), sealed.add(1, 1, 1), Blocks.STONE.getDefaultState());
        world.setBlockState(sealed, Blocks.WATER.getDefaultState(), 2);
        int cx = water.getX() >> 4;
        int cz = water.getZ() >> 4;
        int[] packed = {(water.getX() & 15) | (water.getZ() & 15) << 4 | water.getY() << 8,
                (sealed.getX() & 15) | (sealed.getZ() & 15) << 4 | sealed.getY() << 8};
        OceanTerrain.noteFlowing(world, cx, cz, packed, 2);
        c.eq(1, OceanTerrain.flow(world, cx, cz), "one noted block had air beside it");
        c.eq(Blocks.FLOWING_WATER, world.getBlockState(water).getBlock(), "it flows");
        c.eq(Blocks.WATER, world.getBlockState(sealed).getBlock(), "water walled in stays still");
        c.eq(0, OceanTerrain.flow(world, cx, cz), "notes are used once");
    }

    // ------------------------------------------------------------------------------------------

    /** Real chunks from the world generator: floors, ice, icebergs. */
    private static int realChunks(Check c, WorldServer world, BiomeAquaticOcean biome, BlockPos at) {
        int cx = at.getX() >> 4;
        int cz = at.getZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.getChunk(cx + dx, cz + dz);
            }
        }
        int sea = world.getSeaLevel();
        int columns = 0, sand = 0, gravel = 0, surfaceIce = 0, surfaceWater = 0, packed = 0, blue = 0, deepFloor = 0;
        Block blueIce = Owners.block(Owners.VILLAGE_AND_PILLAGE, "blue_ice");
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x0 = (cx + dx) << 4;
                int z0 = (cz + dz) << 4;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        BlockPos column = new BlockPos(x0 + x, 0, z0 + z);
                        if (world.getBiome(column) != biome) {
                            continue;
                        }
                        columns++;
                        Block top = world.getBlockState(column.up(sea - 1)).getBlock();
                        surfaceIce += top == Blocks.ICE ? 1 : 0;
                        surfaceWater += top == Blocks.WATER ? 1 : 0;
                        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x0 + x, sea - 1, z0 + z);
                        while (p.getY() > 1 && !world.getBlockState(p).getMaterial().blocksMovement()) {
                            p.setY(p.getY() - 1);
                        }
                        Block floor = world.getBlockState(p).getBlock();
                        if (p.getY() < sea - 12) {
                            deepFloor++;
                            sand += floor == Blocks.SAND ? 1 : 0;
                            gravel += floor == Blocks.GRAVEL ? 1 : 0;
                        }
                        for (int y = sea - 20; y < sea + 30; y++) {
                            Block b = world.getBlockState(column.up(y)).getBlock();
                            packed += b == Blocks.PACKED_ICE ? 1 : 0;
                            blue += blueIce != null && b == blueIce ? 1 : 0;
                        }
                    }
                }
            }
        }
        String name = String.valueOf(biome.getRegistryName());
        UpdateAquaticBackport.LOGGER.info("SELFTEST ocean {} at {}: {} columns, deep floors {} ({} sand, {} gravel), surface {} ice / {} water, {} packed ice, {} blue ice",
                name, at, columns, deepFloor, sand, gravel, surfaceIce, surfaceWater, packed, blue);
        if (!c.ok(columns > 1000 && deepFloor > 200, name + ": real ocean columns with deep floors: " + columns + ", " + deepFloor)) {
            return packed;
        }
        if (biome.deepFloor().getBlock() == Blocks.SAND) {
            c.ok(sand * 10 > deepFloor * 6, name + " floors mostly sand: " + sand + " of " + deepFloor);
        } else {
            c.ok(gravel * 10 > deepFloor * 6, name + " floors mostly gravel: " + gravel + " of " + deepFloor);
        }
        if (biome == OceanContent.frozen()) {
            c.ok(surfaceIce > 0 && surfaceWater > 0, name + " frozen over in patches: " + surfaceIce + " ice, " + surfaceWater + " water");
        } else if (biome == OceanContent.deepFrozen()) {
            c.eq(0, surfaceIce, name + " sea ice: never (temperature 0.5 or 0.2)");
        } else {
            c.eq(0, surfaceIce, name + " never freezes");
            c.eq(0, packed, name + " has no icebergs");
        }
        return packed;
    }

    /** Underwater caves in real ocean chunks: obsidian at y 10, which vanilla never leaves there. */
    private static void caves(Check c, WorldServer world, Map<Biome, BlockPos> found) {
        int obsidian = 0;
        int columns = 0;
        for (BlockPos at : found.values()) {
            int cx = at.getX() >> 4;
            int cz = at.getZ() >> 4;
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            columns++;
                            BlockPos p = new BlockPos(((cx + dx) << 4) + x, 10, ((cz + dz) << 4) + z);
                            obsidian += world.getBlockState(p).getBlock() == Blocks.OBSIDIAN ? 1 : 0;
                        }
                    }
                }
            }
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST ocean caves: {} obsidian at y 10 in {} ocean columns", obsidian, columns);
        c.ok(obsidian > 0, "generated oceans have underwater caves reaching y 10: " + obsidian + " obsidian");
    }

    /** Monuments: found from several points, every one on a deep ocean, some on 1.13's new deep oceans. */
    private static void monuments(Check c, WorldServer world) {
        int found = 0, newDeep = 0, onDeep = 0;
        BlockPos[] from = {new BlockPos(0, 64, 0), new BlockPos(3000, 64, 3000), new BlockPos(-3000, 64, 2000),
                new BlockPos(2500, 64, -3500), new BlockPos(-4000, 64, -4000), new BlockPos(6000, 64, -1000),
                new BlockPos(-6000, 64, 500), new BlockPos(500, 64, 6000)};
        for (BlockPos start : from) {
            BlockPos m = world.findNearestStructure("Monument", start, false);
            if (m == null) {
                continue;
            }
            found++;
            Biome b = world.getBiomeProvider().getBiome(new BlockPos(m.getX() + 8, 0, m.getZ() + 8));
            if (b == Biomes.DEEP_OCEAN || (b instanceof BiomeAquaticOcean && ((BiomeAquaticOcean) b).isDeep())) {
                onDeep++;
            }
            if (b instanceof BiomeAquaticOcean) {
                newDeep++;
            }
            UpdateAquaticBackport.LOGGER.info("SELFTEST ocean monument near {}: {} in {}", start, m, b.getRegistryName());
        }
        c.ok(found > 0, "monuments found: " + found);
        c.eq(found, onDeep, "every monument on a deep ocean");
        c.ok(newDeep > 0, "monuments on 1.13's deep oceans too: " + newDeep);
    }
}
