package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.ocean.OceanChunks;
import com.exiledradio.updateaquaticbackport.ocean.OceanContent;

import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A world made without the oceans stream, then played with it: its old chunks keep the noise temperature,
 * its new chunks have the real biomes. Three runs of the same world, never part of {@code all}:
 *
 * <ol>
 *   <li>{@code oceans.enabled=false}, {@code -Pselftest=oceanlegacy}, fresh world: finds a stretch of
 *       vanilla ocean the noise calls warm and one it calls cold, generates 5 x 5 chunks at each, and writes
 *       their positions to {@code selftest-oceanlegacy.txt} in the world folder.</li>
 *   <li>{@code oceans.enabled=true}, same world: loads those chunks from disk (no flag, so old: every
 *       vanilla ocean column takes the noise, warm and cold both found), then generates new chunks far from
 *       them (flag: vanilla ocean normal, the new biomes their own kinds), and writes those down too.</li>
 *   <li>Same again: both sets loaded from disk; the old ones are still old (saving did not make them new),
 *       the new ones still new (the flag was saved).</li>
 * </ol>
 */
public final class OceanLegacySelfTest implements SelfTest {

    private static final String FILE = "selftest-oceanlegacy.txt";
    private static final int HALF = 2;

    @Override
    public String name() {
        return "oceanlegacy";
    }

    @Override
    public boolean inAll() {
        return false;
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        File file = new File(world.getSaveHandler().getWorldDirectory(), FILE);
        List<String> lines = file.exists() ? Files.readAllLines(file.toPath(), StandardCharsets.UTF_8) : new ArrayList<String>();
        boolean haveNew = false;
        for (String line : lines) {
            haveNew |= line.startsWith("new ");
        }
        if (!OceanContent.enabled()) {
            c.ok(lines.isEmpty(), "run 1 is a fresh world");
            runWithout(c, world, file);
        } else if (!lines.isEmpty() && !haveNew) {
            UpdateAquaticBackport.LOGGER.info("SELFTEST oceanlegacy run 2: stream on, old chunks on disk");
            List<String> added = new ArrayList<String>(lines);
            checkOld(c, world, lines);
            added.addAll(makeNew(c, world, lines));
            write(file, added);
        } else if (haveNew) {
            UpdateAquaticBackport.LOGGER.info("SELFTEST oceanlegacy run 3: both sets reloaded");
            checkOld(c, world, lines);
            checkNewReloaded(c, world, lines);
        } else {
            c.fail("stream on but no run-1 file: run 1 needs oceans.enabled=false in a fresh world");
        }
    }

    // ------------------------------------------------------------------------------------------------

    private static void runWithout(Check c, WorldServer world, File file) throws Exception {
        UpdateAquaticBackport.LOGGER.info("SELFTEST oceanlegacy run 1: stream off");
        c.ok(OceanContent.biomes().isEmpty(), "run 1: no new ocean biomes");
        List<String> out = new ArrayList<String>();
        int columns = 0, noise = 0;
        for (OceanKind want : new OceanKind[] {OceanKind.WARM, OceanKind.COLD}) {
            BlockPos at = find(world, want);
            if (!c.ok(at != null, "run 1: a vanilla ocean the noise calls " + want)) {
                continue;
            }
            UpdateAquaticBackport.LOGGER.info("SELFTEST oceanlegacy run 1: {} vanilla ocean by the noise at {}", want, at);
            for (Chunk chunk : around(world, at)) {
                out.add("old " + chunk.x + " " + chunk.z);
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        BlockPos p = new BlockPos(chunk.x * 16 + dx, 62, chunk.z * 16 + dz);
                        Biome b = world.getBiome(p);
                        if (b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN) {
                            columns++;
                            noise += OceanKind.at(world, p) == OceanKind.fromNoise(world.getSeed(), p.getX(), p.getZ()) ? 1 : 0;
                        }
                    }
                }
            }
        }
        c.ok(columns > 50 * 200, "run 1: the chunks are mostly vanilla ocean: " + columns);
        c.eq(columns, noise, "run 1: without the stream every vanilla ocean column takes the noise");
        write(file, out);
    }

    /** @return the middle of a 5 x 5 chunk stretch of vanilla ocean whose noise temperature is this kind */
    private static BlockPos find(WorldServer world, OceanKind want) {
        for (int r = 1; r < 120; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    int x = dx * 96, z = dz * 96;
                    if (OceanKind.fromNoise(world.getSeed(), x, z) != want) {
                        continue;
                    }
                    boolean ocean = true;
                    for (int ox = -48; ox <= 48 && ocean; ox += 16) {
                        for (int oz = -48; oz <= 48; oz += 16) {
                            Biome b = world.getBiomeProvider().getBiome(new BlockPos(x + ox, 0, z + oz));
                            if (b != Biomes.OCEAN && b != Biomes.DEEP_OCEAN) {
                                ocean = false;
                                break;
                            }
                        }
                    }
                    if (ocean) {
                        return new BlockPos(x, 0, z);
                    }
                }
            }
        }
        return null;
    }

    private static List<Chunk> around(WorldServer world, BlockPos at) {
        List<Chunk> chunks = new ArrayList<Chunk>();
        for (int cx = (at.getX() >> 4) - HALF; cx <= (at.getX() >> 4) + HALF; cx++) {
            for (int cz = (at.getZ() >> 4) - HALF; cz <= (at.getZ() >> 4) + HALF; cz++) {
                chunks.add(world.getChunk(cx, cz));
            }
        }
        return chunks;
    }

    private static List<Chunk> chunks(WorldServer world, List<String> lines, String prefix) {
        List<Chunk> chunks = new ArrayList<Chunk>();
        for (String line : lines) {
            if (line.startsWith(prefix + " ")) {
                String[] parts = line.split(" ");
                chunks.add(world.getChunk(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            }
        }
        return chunks;
    }

    private static void checkOld(Check c, WorldServer world, List<String> lines) {
        Map<OceanKind, Integer> kinds = new EnumMap<OceanKind, Integer>(OceanKind.class);
        int columns = 0, noise = 0, fromDisk = 0, newBiome = 0;
        List<Chunk> old = chunks(world, lines, "old");
        for (Chunk chunk : old) {
            fromDisk += Boolean.FALSE.equals(OceanChunks.loadedFlag(chunk)) ? 1 : 0;
            for (byte id : chunk.getBiomeArray()) {
                newBiome += OceanContent.biomes().contains(Biome.getBiome(id & 255)) ? 1 : 0;
            }
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    BlockPos p = new BlockPos(chunk.x * 16 + dx, 62, chunk.z * 16 + dz);
                    Biome b = world.getBiome(p);
                    if (b != Biomes.OCEAN && b != Biomes.DEEP_OCEAN) {
                        continue;
                    }
                    columns++;
                    OceanKind kind = OceanKind.at(world, p);
                    noise += kind == OceanKind.fromNoise(world.getSeed(), p.getX(), p.getZ()) ? 1 : 0;
                    Integer n = kinds.get(kind);
                    kinds.put(kind, n == null ? 1 : n + 1);
                }
            }
        }
        c.eq(50, old.size(), "50 old chunks listed");
        c.eq(old.size(), fromDisk, "every old chunk was loaded from disk without the flag");
        c.eq(0, newBiome, "old chunks keep their 1.12 biomes");
        c.ok(columns > 50 * 200, "old chunks are mostly vanilla ocean: " + columns + " columns");
        c.eq(columns, noise, "every vanilla ocean column of an old chunk takes the noise");
        c.ok(kinds.containsKey(OceanKind.WARM) && kinds.containsKey(OceanKind.COLD), "old chunks are warm and cold by the noise: " + kinds);
        UpdateAquaticBackport.LOGGER.info("SELFTEST oceanlegacy old chunks: {} vanilla ocean columns, kinds {}", columns, kinds);
    }

    private static List<String> makeNew(Check c, WorldServer world, List<String> lines) {
        List<String> out = new ArrayList<String>();
        c.ok(OceanChunks.layered(world.getBiomeProvider()), "run 2: the world's map now has the layers");
        // Far from the old chunks (all within 120 x 96 blocks of spawn), so none of them is reused.
        BlockPos vanilla = findBiome(world, 20000, Biomes.OCEAN, Biomes.DEEP_OCEAN);
        BlockPos warm = findBiome(world, 20000, OceanContent.warm(), OceanContent.warm());
        BlockPos cold = findBiome(world, 20000, OceanContent.cold(), OceanContent.deepCold());
        if (!c.ok(vanilla != null && warm != null && cold != null, "run 2: found vanilla, warm and cold ocean far out")) {
            return out;
        }
        int columns = 0, normal = 0, warmCols = 0, warmOk = 0, coldCols = 0, coldOk = 0, flagged = 0, total = 0;
        for (BlockPos at : new BlockPos[] {vanilla, warm, cold}) {
            for (Chunk chunk : around(world, at)) {
                out.add("new " + chunk.x + " " + chunk.z);
                total++;
                flagged += OceanChunks.loadedFlag(chunk) == null && OceanChunks.realTemperatures(chunk) ? 1 : 0;
                for (int dx = 0; dx < 16; dx++) {
                    for (int dz = 0; dz < 16; dz++) {
                        BlockPos p = new BlockPos(chunk.x * 16 + dx, 62, chunk.z * 16 + dz);
                        Biome b = world.getBiome(p);
                        OceanKind kind = OceanKind.at(world, p);
                        if (b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN) {
                            columns++;
                            normal += kind == OceanKind.NORMAL ? 1 : 0;
                        } else if (b == OceanContent.warm()) {
                            warmCols++;
                            warmOk += kind == OceanKind.WARM ? 1 : 0;
                        } else if (b == OceanContent.cold() || b == OceanContent.deepCold()) {
                            coldCols++;
                            coldOk += kind == OceanKind.COLD ? 1 : 0;
                        }
                    }
                }
            }
        }
        c.eq(total, flagged, "every new chunk was generated now, with real temperatures");
        c.ok(columns > 1000, "new chunks have vanilla ocean: " + columns);
        c.eq(columns, normal, "every vanilla ocean column of a new chunk is the normal temperature");
        c.ok(warmCols > 1000 && warmCols == warmOk, "new warm ocean columns are warm: " + warmOk + "/" + warmCols);
        c.ok(coldCols > 1000 && coldCols == coldOk, "new cold ocean columns are cold: " + coldOk + "/" + coldCols);
        UpdateAquaticBackport.LOGGER.info("SELFTEST oceanlegacy new chunks: {} vanilla ocean (normal), {} warm, {} cold columns",
                columns, warmCols, coldCols);
        return out;
    }

    /** @return a point at least {@code from} blocks east whose 5 x 5 chunks are all one of two biomes */
    private static BlockPos findBiome(WorldServer world, int from, Biome a, Biome b) {
        for (int i = 0; i < 4000; i++) {
            int x = from + (i % 40) * 160, z = (i / 40) * 160 - 8000;
            boolean all = true;
            for (int ox = -32; ox <= 32 && all; ox += 8) {
                for (int oz = -32; oz <= 32; oz += 8) {
                    Biome here = world.getBiomeProvider().getBiome(new BlockPos(x + ox, 0, z + oz));
                    if (here != a && here != b) {
                        all = false;
                        break;
                    }
                }
            }
            if (all) {
                return new BlockPos(x, 0, z);
            }
        }
        return null;
    }

    private static void checkNewReloaded(Check c, WorldServer world, List<String> lines) {
        int fromDisk = 0, columns = 0, normal = 0;
        List<Chunk> fresh = chunks(world, lines, "new");
        for (Chunk chunk : fresh) {
            fromDisk += Boolean.TRUE.equals(OceanChunks.loadedFlag(chunk)) ? 1 : 0;
            for (int dx = 0; dx < 16; dx++) {
                for (int dz = 0; dz < 16; dz++) {
                    BlockPos p = new BlockPos(chunk.x * 16 + dx, 62, chunk.z * 16 + dz);
                    Biome b = world.getBiome(p);
                    if (b == Biomes.OCEAN || b == Biomes.DEEP_OCEAN) {
                        columns++;
                        normal += OceanKind.at(world, p) == OceanKind.NORMAL ? 1 : 0;
                    }
                }
            }
        }
        c.eq(75, fresh.size(), "75 new chunks listed");
        c.eq(fresh.size(), fromDisk, "every new chunk was loaded from disk with the flag");
        c.ok(columns > 1000, "reloaded new chunks have vanilla ocean: " + columns);
        c.eq(columns, normal, "and it is still the normal temperature");
    }

    private static void write(File file, List<String> lines) throws Exception {
        PrintWriter out = new PrintWriter(file, "UTF-8");
        for (String line : lines) {
            out.println(line);
        }
        out.close();
    }
}
