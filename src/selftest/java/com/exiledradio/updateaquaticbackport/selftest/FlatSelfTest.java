package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;
import com.exiledradio.backportcore.worldgen.FlatWorlds;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.bubble.BubbleContent;
import com.exiledradio.updateaquaticbackport.bubble.BubbleGenerator;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.coral.CoralGenerator;
import com.exiledradio.updateaquaticbackport.kelp.KelpGenerator;
import com.exiledradio.updateaquaticbackport.ocean.OceanContent;
import com.exiledradio.updateaquaticbackport.structure.StructureContent;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

/**
 * Superflat worlds generate only what vanilla 1.12 does for their preset (Backport Core's {@link FlatWorlds}).
 *
 * <p>In the suite's own world ({@code all}) the rules are checked by making its world info say superflat for a
 * moment: no ocean structure, no kelp, seagrass, reef or bubble column from the generators, no polar bears from
 * chunk generation, no freezing pass. In a real superflat world ({@code tools/flat/selftest.sh <preset>}) it
 * generates chunks well away from spawn (whose chunks have ticked, so may hold naturally spawned mobs) and scans
 * them: never a shipwreck, ruin or buried treasure, nor any mob from generation; this mod's plants only with
 * {@code decoration}, and then where the biome would have them.
 */
public final class FlatSelfTest implements SelfTest {

    private static final String[] KINDS = {Structures.SHIPWRECK, Structures.OCEAN_RUIN, Structures.BURIED_TREASURE};
    private static final String[] IDS = {"minecraft:shipwreck", "minecraft:shipwreck_beached", "minecraft:ocean_ruin_cold",
            "minecraft:ocean_ruin_warm", "minecraft:buried_treasure", "#minecraft:shipwreck", "#minecraft:ocean_ruin"};

    /** The ocean preset the suite's world pretends to be: no decoration. */
    private static final String OCEAN = "3;minecraft:bedrock,5*minecraft:stone,5*minecraft:sand,40*minecraft:water;0;";

    /** Chunks scanned in a real superflat world: this far east of spawn, and this many either side. */
    private static final int AWAY = 48, HALF = 5;

    /** CoralGenerator's reef noise. */
    private static final NoiseGeneratorPerlin REEF_NOISE = new NoiseGeneratorPerlin(new Random(2345L), 1);

    @Override
    public String name() {
        return "flat";
    }

    @Override
    public void run(Check c) throws Exception {
        if (FlatWorlds.isFlat(c.world)) {
            flatWorld(c);
        } else {
            rules(c);
        }
    }

    // ---------------------------------------------------------------------------------------------- the suite's world

    private static void rules(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(!FlatWorlds.isFlat(world) && FlatWorlds.decoration(world), "the suite's world is not superflat");
        if (StructureContent.enabled()) {
            for (String kind : KINDS) {
                c.ok(StructureContent.generates(world, kind), kind + " generates in the suite's world");
            }
        }

        // Areas first: a test area loads chunks, which must not populate under the fake preset.
        BlockPos pool = Pools.pool(c, 5);
        world.setBlockState(pool.add(2, 0, 2), Blocks.MAGMA.getDefaultState(), 2);
        BlockPos ice = c.area();
        c.fill(ice.down(), ice.add(31, -1, 31), Blocks.ICE.getDefaultState());
        Biome frozen = OceanContent.enabled() ? OceanContent.frozen() : net.minecraft.init.Biomes.FROZEN_OCEAN;
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                byte[] biomes = world.getChunk(ice.add(16 * dx, 0, 16 * dz)).getBiomeArray();
                java.util.Arrays.fill(biomes, (byte) Biome.getIdForBiome(frozen));
            }
        }
        int poolChunkX = (pool.getX() >> 4) - 1;
        int poolChunkZ = (pool.getZ() >> 4) - 1;
        BlockPos from = new BlockPos(pool.getX() - 24, 0, pool.getZ() - 24);
        Map<String, Integer> before = scan(world, from.getX(), from.getZ(), 64, 64, 0, 256);

        WorldInfo info = world.getWorldInfo();
        Field type = ReflectionHelper.findField(WorldInfo.class, "terrainType", "field_76098_b");
        Field options = ReflectionHelper.findField(WorldInfo.class, "generatorOptions", "field_82576_c");
        Object oldType = type.get(info);
        Object oldOptions = options.get(info);
        try {
            type.set(info, WorldType.FLAT);
            options.set(info, OCEAN);
            FlatWorlds.forget(world);
            c.ok(FlatWorlds.isFlat(world) && !FlatWorlds.decoration(world), "pretending: an ocean preset without decoration");
            structures(c, world, "pretending", false);

            IChunkGenerator generator = world.getChunkProvider().chunkGenerator;
            Random random = new Random(7);
            for (int i = 0; i < 4; i++) {
                new KelpGenerator().generate(random, poolChunkX + i % 2, poolChunkZ + i / 2, world, generator, world.getChunkProvider());
                new CoralGenerator().generate(random, poolChunkX + i % 2, poolChunkZ + i / 2, world, generator, world.getChunkProvider());
                new BubbleGenerator().generate(random, poolChunkX + i % 2, poolChunkZ + i / 2, world, generator, world.getChunkProvider());
            }
            c.eq(before, scan(world, from.getX(), from.getZ(), 64, 64, 0, 256),
                    "pretending: the kelp, coral and bubble generators place nothing");

            int cx = ice.getX() >> 4;
            int cz = ice.getZ() >> 4;
            for (int i = 0; i < 200; i++) {
                MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(generator, world, random, cx, cz, false));
            }
            AxisAlignedBB box = new AxisAlignedBB(ice.getX(), 90, ice.getZ(), ice.getX() + 32, 110, ice.getZ() + 32);
            c.eq(0, world.getEntitiesWithinAABB(EntityPolarBear.class, box).size(),
                    "pretending: chunk generation spawns no polar bears on the ice of a frozen ocean");
            PopulateChunkEvent.Populate freeze = new PopulateChunkEvent.Populate(generator, world, random, cx, cz, false,
                    PopulateChunkEvent.Populate.EventType.ICE);
            MinecraftForge.TERRAIN_GEN_BUS.post(freeze);
            c.ok(freeze.getResult() != Event.Result.DENY, "pretending: no freezing pass of this mod's");
        } finally {
            type.set(info, oldType);
            options.set(info, oldOptions);
            FlatWorlds.forget(world);
        }
        c.ok(!FlatWorlds.isFlat(world) && FlatWorlds.decoration(world), "the world is itself again");

        // Controls: the same calls in the suite's world do generate.
        new BubbleGenerator().generate(new Random(7), poolChunkX, poolChunkZ, world,
                world.getChunkProvider().chunkGenerator, world.getChunkProvider());
        if (BubbleContent.column() != null) {
            c.eq(BubbleContent.column(), world.getBlockState(pool.add(2, 1, 2)).getBlock(),
                    "control: outside superflat the bubble generator starts a column over the magma");
        }
        if (OceanContent.enabled()) {
            PopulateChunkEvent.Populate freeze = new PopulateChunkEvent.Populate(world.getChunkProvider().chunkGenerator,
                    world, new Random(7), ice.getX() >> 4, ice.getZ() >> 4, false, PopulateChunkEvent.Populate.EventType.ICE);
            MinecraftForge.TERRAIN_GEN_BUS.post(freeze);
            c.eq(Event.Result.DENY, freeze.getResult(), "control: outside superflat the frozen ocean's freezing pass is this mod's");
        }
    }

    // ---------------------------------------------------------------------------------------------- a real superflat world

    private static void flatWorld(Check c) throws Exception {
        WorldServer world = c.world;
        String preset = world.getWorldInfo().getGeneratorOptions();
        boolean decoration = preset.contains(FlatWorlds.DECORATION);
        UpdateAquaticBackport.LOGGER.info("SELFTEST flat: preset {}", preset);
        for (Biome biome : OceanContent.biomes()) {
            UpdateAquaticBackport.LOGGER.info("SELFTEST flat: biome {} has id {}", biome.getRegistryName(), Biome.getIdForBiome(biome));
        }
        c.ok(FlatWorlds.isFlat(world), "superflat: " + preset);
        c.eq(decoration, FlatWorlds.decoration(world), "decoration as the preset says");

        // Chunks no player or spawn area has ticked: everything in them is from generation.
        BlockPos spawn = world.getSpawnPoint();
        int cx0 = (spawn.getX() >> 4) + AWAY;
        int cz0 = spawn.getZ() >> 4;
        // Where the reef noise is high, so a warm ocean preset grows reefs there (as the same seed would).
        for (int i = 0; i < 64 && REEF_NOISE.getValue((cx0 * 16 + 8) / 400.0, (cz0 * 16 + 8) / 400.0) < 0.3; i++) {
            cx0 += 2 * HALF + 3;
        }
        for (int x = cx0 - HALF - 1; x <= cx0 + HALF + 1; x++) {
            for (int z = cz0 - HALF - 1; z <= cz0 + HALF + 1; z++) {
                world.getChunkProvider().provideChunk(x, z);
            }
        }
        boolean populated = true;
        for (int x = cx0 - HALF; x <= cx0 + HALF; x++) {
            for (int z = cz0 - HALF; z <= cz0 + HALF; z++) {
                populated &= world.getChunk(x, z).isTerrainPopulated();
            }
        }
        c.ok(populated, "the scanned chunks are populated");

        structures(c, world, "superflat", true);
        int x0 = (cx0 - HALF) * 16;
        int z0 = (cz0 - HALF) * 16;
        int size = (2 * HALF + 1) * 16;
        Map<String, Integer> found = scan(world, x0, z0, size, size, 0, 256);
        UpdateAquaticBackport.LOGGER.info("SELFTEST flat: found {}", found);

        int chests = 0;
        for (int x = cx0 - HALF; x <= cx0 + HALF; x++) {
            for (int z = cz0 - HALF; z <= cz0 + HALF; z++) {
                for (TileEntity te : world.getChunk(x, z).getTileEntityMap().values()) {
                    chests += te instanceof TileEntityChest ? 1 : 0;
                }
            }
        }
        c.eq(0, chests, "no chests (no shipwreck, ruin or treasure)");

        AxisAlignedBB box = new AxisAlignedBB(x0, 0, z0, x0 + size, 256, z0 + size);
        c.eq(0, world.getEntitiesWithinAABB(EntityPolarBear.class, box).size(), "no polar bears from generation");
        c.eq(0, world.getEntitiesWithinAABB(EntityLiving.class, box).size(), "no mobs at all from generation");

        int kelp = count(found, "kelp");
        int seagrass = count(found, "seagrass");
        int coral = count(found, "coral");
        int pickles = count(found, "sea_pickle");
        int bubbles = count(found, "bubble_column");
        if (!decoration) {
            c.eq(new TreeMap<String, Integer>(), found, "without decoration none of this mod's blocks generate");
            return;
        }

        // With decoration, where the biome would have them (KelpGenerator's and CoralGenerator's tables).
        boolean water = hasWater(world, x0 + 8, z0 + 8);
        int grassy = 0, kelpy = 0, warm = 0, reefPieces = 0;
        for (int x = cx0 - HALF; x <= cx0 + HALF; x++) {
            for (int z = cz0 - HALF; z <= cz0 + HALF; z++) {
                OceanKind kind = OceanKind.at(world, new BlockPos(x * 16 + 16, 0, z * 16 + 16));
                grassy += kind.isOcean() && kind != OceanKind.FROZEN ? 1 : 0;
                kelpy += kind.isOcean() && kind != OceanKind.FROZEN && kind != OceanKind.WARM ? 1 : 0;
                if (kind == OceanKind.WARM) {
                    // CoralGenerator's reef pieces for this chunk: none where the noise is below zero.
                    warm++;
                    reefPieces += Math.max(0, (int) Math.ceil(REEF_NOISE.getValue((x * 16 + 8) / 400.0, (z * 16 + 8) / 400.0) * 20));
                }
            }
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST flat: water {}, chunks with seagrass {}, kelp {}, warm {} ({} reef pieces)",
                water, grassy, kelpy, warm, reefPieces);
        if (!water) {
            c.eq(new TreeMap<String, Integer>(), found, "a dry preset gets none of this mod's blocks");
            return;
        }
        c.ok(grassy == 0 || seagrass > 0, "seagrass in an ocean with decoration (" + seagrass + ")");
        c.ok(grassy > 0 || seagrass == 0, "no seagrass where the ocean has none (" + seagrass + ")");
        c.ok(kelpy == 0 || kelp > 0, "kelp in an ocean with decoration (" + kelp + ")");
        c.ok(kelpy > 0 || kelp == 0, "no kelp where the ocean has none (" + kelp + ")");
        c.ok(warm > 0 || coral + pickles == 0, "reefs only in warm oceans (" + coral + " coral, " + pickles + " pickles)");
        // A piece grows only where its own floor block is warm too, so a few tries at a warm patch's edge may all miss.
        c.ok(reefPieces < 20 || coral > 0, "reefs in a warm ocean with decoration (" + coral + " coral, "
                + reefPieces + " pieces tried)");
        c.eq(0, bubbles, "no bubble columns without magma or soul sand");
    }

    private static boolean hasWater(WorldServer world, int x, int z) {
        for (int y = 0; y < 256; y++) {
            if (world.getBlockState(new BlockPos(x, y, z)).getBlock() == Blocks.WATER) {
                return true;
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------- shared

    /**
     * Neither Core nor this mod generates or finds any of the three ocean structures.
     *
     * @param fresh a real superflat world, whose structure instances were first asked for as superflat
     */
    private static void structures(Check c, WorldServer world, String where, boolean fresh) throws Exception {
        for (String kind : KINDS) {
            c.ok(!StructureContent.generates(world, kind), where + ": " + kind + " does not generate");
            if (fresh) {
                // Core keeps each world's instance; the suite's world made its own before it pretended.
                c.ok(ScatteredStructure.of(world, kind) == null, where + ": " + kind + " has no instance");
            }
            c.ok(Structures.nearest(world, kind, world.getSpawnPoint(), 64) == null, where + ": no " + kind + " near spawn");
        }
        for (String id : IDS) {
            StructureLocate.Found found = null;
            try {
                found = StructureLocate.nearest(world, world.getSpawnPoint(), id);
            } catch (StructureLocate.Unavailable e) {
                // Unavailable is not found.
            }
            c.ok(found == null, where + ": /locate structure " + id + " finds none");
        }
    }

    /**
     * Counts the blocks this mod generates (kelp, seagrass, coral, sea pickles, bubble columns, blue ice, by
     * registry path, whoever owns them) in a box, reading block storage directly.
     */
    private static Map<String, Integer> scan(WorldServer world, int x0, int z0, int sizeX, int sizeZ, int minY, int maxY) {
        Map<String, Integer> found = new TreeMap<String, Integer>();
        for (int cx = x0 >> 4; cx <= (x0 + sizeX - 1) >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= (z0 + sizeZ - 1) >> 4; cz++) {
                Chunk chunk = world.getChunk(cx, cz);
                for (ExtendedBlockStorage section : chunk.getBlockStorageArray()) {
                    if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
                        continue;
                    }
                    int sy = section.getYLocation();
                    if (sy + 16 <= minY || sy >= maxY) {
                        continue;
                    }
                    for (int x = 0; x < 16; x++) {
                        int bx = cx * 16 + x;
                        if (bx < x0 || bx >= x0 + sizeX) {
                            continue;
                        }
                        for (int z = 0; z < 16; z++) {
                            int bz = cz * 16 + z;
                            if (bz < z0 || bz >= z0 + sizeZ) {
                                continue;
                            }
                            for (int y = 0; y < 16; y++) {
                                IBlockState state = section.get(x, y, z);
                                Block block = state.getBlock();
                                if (block == Blocks.AIR || block.getRegistryName() == null) {
                                    continue;
                                }
                                String path = block.getRegistryName().getPath();
                                if (path.contains("kelp") || path.contains("seagrass") || path.contains("coral")
                                        || path.contains("sea_pickle") || path.contains("bubble_column") || path.equals("blue_ice")) {
                                    String key = block.getRegistryName().toString();
                                    Integer n = found.get(key);
                                    found.put(key, n == null ? 1 : n + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        return found;
    }

    private static int count(Map<String, Integer> found, String part) {
        int n = 0;
        for (Map.Entry<String, Integer> e : found.entrySet()) {
            n += e.getKey().contains(part) ? e.getValue() : 0;
        }
        return n;
    }
}
