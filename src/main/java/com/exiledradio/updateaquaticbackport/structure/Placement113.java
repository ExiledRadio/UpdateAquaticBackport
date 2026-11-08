package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.init.Biomes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraft.world.gen.layer.GenLayer;
import net.minecraft.world.gen.layer.GenLayerHills;
import net.minecraft.world.gen.structure.MapGenScatteredFeature;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.common.BiomeManager;
import net.minecraftforge.event.terraingen.InitMapGenEvent;
import net.minecraftforge.event.terraingen.WorldTypeEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Random;

/**
 * 1.13's placement of the structures 1.12.2 already has, so a world matches 1.13's seed maps.
 *
 * <ul>
 *   <li><b>Temples</b>: 1.12.2 puts the desert pyramid, jungle temple, witch hut and igloo on one grid (salt
 *       14357617) and picks the kind by biome. 1.13 gives each its own grid (salts 14357617, 14357619,
 *       14357620 and 14357618, spacing 32, separation 8), as {@link Temples} does.</li>
 *   <li><b>Biome point</b>: every 1.13 structure that checks one biome reads the block-scale biome at block
 *       (9, 9) of the chunk; 1.12.2's temples read (8, 8) and its villages the 1:4 cell there. The village
 *       half is patch P4 ({@code asm/VillageBiomePatch}), which sends 1.12.2's village check here.</li>
 *   <li><b>Strongholds</b>: 1.13 places them in every biome that has them; next to 1.12.2's "above sea
 *       level" rule, that adds the mushroom field shore.</li>
 *   <li><b>Biome search</b>: strongholds and the spawn point search the 1:4 map with
 *       {@code findBiomePosition}, whose pick changed in 1.13 (patch P5, {@link #findBiomePosition}).</li>
 *   <li><b>Mutated biomes</b>: 1.12.2's hills layer reads a river noise whose two zoom layers are never
 *       seeded with the world seed ({@code GenLayerHills.initWorldGenSeed} seeds only its biome parent), so
 *       which areas turn into their rare variant (sunflower plains, tall birch forest, gravelly mountains and
 *       the like) differs from 1.13, whose layers all get the seed. {@link #onInitBiomeGens} seeds them.</li>
 * </ul>
 * Mansions, monuments and strongholds otherwise place as 1.12.2 does, which is 1.13's way: Core's vanilla
 * oracle ({@code tools/vanilla-oracle}) compares a world with 1.13.2's own code.
 */
public final class Placement113 {

    static final int DESERT_PYRAMID = 14357617, IGLOO = 14357618, JUNGLE_TEMPLE = 14357619, SWAMP_HUT = 14357620;
    private static final int[] SALTS = {DESERT_PYRAMID, IGLOO, JUNGLE_TEMPLE, SWAMP_HUT};
    /** 1.12.2's temple separation, which 1.13 keeps. */
    private static final int SEPARATION = 8;

    private Placement113() {
    }

    public static void init() {
        BiomeManager.addStrongholdBiome(Biomes.MUSHROOM_ISLAND_SHORE);
    }

    /** The overworld's temple generator becomes 1.13's, unless another mod has replaced it already. */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInitMapGen(InitMapGenEvent event) {
        if (event.getType() == InitMapGenEvent.EventType.SCATTERED_FEATURE
                && event.getNewGen().getClass() == MapGenScatteredFeature.class) {
            event.setNewGen(new Temples((MapGenScatteredFeature) event.getNewGen()));
        }
    }

    /**
     * Seeds the river noise of every vanilla hills layer in the map with the world seed, as 1.13's layer
     * contexts do. It walks the whole map, so it finds them under other mods' wrapping layers too.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onInitBiomeGens(WorldTypeEvent.InitBiomeGens event) {
        GenLayer[] layers = event.getNewBiomeGens();
        if (layers == null) {
            return;
        }
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
        Deque<GenLayer> todo = new ArrayDeque<GenLayer>();
        for (GenLayer layer : layers) {
            if (layer != null) {
                todo.add(layer);
            }
        }
        int seeded = 0;
        try {
            while (!todo.isEmpty()) {
                GenLayer layer = todo.poll();
                if (!seen.add(layer)) {
                    continue;
                }
                for (Class<?> c = layer.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        if (!GenLayer.class.isAssignableFrom(f.getType()) || Modifier.isStatic(f.getModifiers())) {
                            continue;
                        }
                        f.setAccessible(true);
                        GenLayer child = (GenLayer) f.get(layer);
                        if (child == null) {
                            continue;
                        }
                        // GenLayerHills' second parent (its river noise) is the field that is not 'parent'.
                        if (layer.getClass() == GenLayerHills.class && c == GenLayerHills.class) {
                            child.initWorldGenSeed(event.getSeed());
                            seeded++;
                        }
                        todo.add(child);
                    }
                }
            }
        } catch (IllegalAccessException e) {
            UpdateAquaticBackport.LOGGER.warn("Could not seed the hills layer's river noise; rare biome variants stay 1.12.2's", e);
        }
        UpdateAquaticBackport.LOGGER.debug("Seeded {} hills layer river noise(s) ({})", seeded, event.getWorldType().getName());
    }

    /** @return the biome a single-biome structure check reads, 1.13's way: block (9, 9) of the chunk */
    public static Biome biomeAt(BiomeProvider provider, int chunkX, int chunkZ) {
        return provider.getBiome(new BlockPos((chunkX << 4) + 9, 0, (chunkZ << 4) + 9));
    }

    /**
     * Patch P4's hook: {@code MapGenVillage.canSpawnStructureAtCoords} asks
     * {@code areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 0, VILLAGE_SPAWN_BIOMES)}; 1.13's village reads
     * the block biome one block further on. Any other radius is passed through.
     */
    public static boolean villageBiomes(BiomeProvider provider, int x, int z, int radius, List<Biome> allowed) {
        if (radius != 0) {
            return provider.areBiomesViable(x, z, radius, allowed);
        }
        Biome biome = provider.getBiome(new BlockPos(x + 1, 0, z + 1));
        return biome != null && allowed.contains(biome);
    }

    /**
     * Patch P5's hook, the body of {@code BiomeProvider.findBiomePosition}: 1.13's search over the 1:4 cells of
     * the square, which counts every matching cell (1.12.2 counts only the ones it keeps).
     */
    @Nullable
    public static BlockPos findBiomePosition(BiomeProvider provider, int x, int z, int range, List<Biome> biomes, Random random) {
        int x0 = x - range >> 2;
        int z0 = z - range >> 2;
        int w = (x + range >> 2) - x0 + 1;
        int h = (z + range >> 2) - z0 + 1;
        Biome[] cells = provider.getBiomesForGeneration(null, x0, z0, w, h);
        BlockPos found = null;
        int count = 0;
        for (int i = 0; i < w * h; i++) {
            if (biomes.contains(cells[i])) {
                if (found == null || random.nextInt(count + 1) == 0) {
                    found = new BlockPos(x0 + i % w << 2, 0, z0 + i / w << 2);
                }
                count++;
            }
        }
        return found;
    }

    /**
     * The temple grid a biome's temple uses, or 0 for none: 1.13's desert pyramid (desert, desert hills),
     * jungle temple (jungle, jungle hills, and 1.14's bamboo jungles where Village &amp; Pillage adds them),
     * witch hut (swamp) and igloo (snowy tundra, snowy taiga).
     */
    static int templeSalt(Biome biome) {
        if (biome == Biomes.DESERT || biome == Biomes.DESERT_HILLS) {
            return DESERT_PYRAMID;
        }
        if (biome == Biomes.JUNGLE || biome == Biomes.JUNGLE_HILLS || isBambooJungle(biome)) {
            return JUNGLE_TEMPLE;
        }
        if (biome == Biomes.SWAMPLAND) {
            return SWAMP_HUT;
        }
        if (biome == Biomes.ICE_PLAINS || biome == Biomes.COLD_TAIGA) {
            return IGLOO;
        }
        return 0;
    }

    /** 1.14's bamboo jungle, whichever mod adds it: a jungle temple biome in 1.14. */
    static boolean isBambooJungle(Biome biome) {
        ResourceLocation name = biome == null ? null : biome.getRegistryName();
        return name != null && ("bamboo_jungle".equals(name.getPath()) || "bamboo_jungle_hills".equals(name.getPath()));
    }

    /** 1.12.2's temple generator with 1.13's four grids and biome point. Saved and loaded as vanilla's ("Temple"). */
    public static final class Temples extends MapGenScatteredFeature {

        private static final Field SPACING = ReflectionHelper.findField(MapGenScatteredFeature.class,
                "maxDistanceBetweenScatteredFeatures", "field_82669_g");

        private final int spacing;

        Temples(MapGenScatteredFeature replaced) {
            int spacing = 32;
            try {
                spacing = SPACING.getInt(replaced);
            } catch (IllegalAccessException e) {
                UpdateAquaticBackport.LOGGER.warn("Temples: could not read the temple spacing; using 32", e);
            }
            this.spacing = spacing;
        }

        @Override
        protected boolean canSpawnStructureAtCoords(int chunkX, int chunkZ) {
            return startsAt(this.world, this.spacing, chunkX, chunkZ);
        }

        /**
         * 1.13's {@code ScatteredStructure.hasStartAt} for each of the four temples: the chunk is the region's
         * candidate on that temple's grid, and the biome at its (9, 9) is that temple's. The grids are asked
         * first, as vanilla does, so the biome is read only for candidate chunks.
         */
        static boolean startsAt(World world, int spacing, int chunkX, int chunkZ) {
            int x = chunkX < 0 ? chunkX - spacing + 1 : chunkX;
            int z = chunkZ < 0 ? chunkZ - spacing + 1 : chunkZ;
            int cellX = x / spacing;
            int cellZ = z / spacing;
            int biomeSalt = -1;
            for (int salt : SALTS) {
                Random random = world.setRandomSeed(cellX, cellZ, salt);
                if (chunkX == cellX * spacing + random.nextInt(spacing - SEPARATION)
                        && chunkZ == cellZ * spacing + random.nextInt(spacing - SEPARATION)) {
                    if (biomeSalt == -1) {
                        biomeSalt = templeSalt(biomeAt(world.getBiomeProvider(), chunkX, chunkZ));
                    }
                    if (biomeSalt == salt) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** The start builds the temple of the biome its grid was chosen by (a bamboo jungle builds a jungle temple). */
        @Override
        protected StructureStart getStructureStart(int chunkX, int chunkZ) {
            Biome biome = biomeAt(this.world.getBiomeProvider(), chunkX, chunkZ);
            if (isBambooJungle(biome)) {
                biome = Biomes.JUNGLE;
            }
            return new MapGenScatteredFeature.Start(this.world, this.rand, chunkX, chunkZ, biome);
        }

        /** {@code /locate Temple}: the nearest of any of the four kinds, ring by ring over the grid. */
        @Override
        public BlockPos getNearestStructurePos(World world, BlockPos pos, boolean findUnexplored) {
            this.world = world;
            BlockPos best = null;
            for (int salt : SALTS) {
                BlockPos found = nearestOnGrid(world, pos, salt);
                if (found != null && (best == null || found.distanceSq(pos) < best.distanceSq(pos))) {
                    best = found;
                }
            }
            return best;
        }

        private BlockPos nearestOnGrid(World world, BlockPos pos, int salt) {
            int cx = pos.getX() >> 4;
            int cz = pos.getZ() >> 4;
            for (int ring = 0; ring <= 100; ring++) {
                BlockPos best = null;
                for (int dx = -ring; dx <= ring; dx++) {
                    for (int dz = -ring; dz <= ring; dz++) {
                        if (Math.abs(dx) != ring && Math.abs(dz) != ring) {
                            continue;
                        }
                        int x = cx + dx * this.spacing;
                        int z = cz + dz * this.spacing;
                        int gx = Math.floorDiv(x, this.spacing);
                        int gz = Math.floorDiv(z, this.spacing);
                        Random random = world.setRandomSeed(gx, gz, salt);
                        int sx = gx * this.spacing + random.nextInt(this.spacing - SEPARATION);
                        int sz = gz * this.spacing + random.nextInt(this.spacing - SEPARATION);
                        if (templeSalt(biomeAt(world.getBiomeProvider(), sx, sz)) == salt) {
                            BlockPos p = new BlockPos((sx << 4) + 8, 64, (sz << 4) + 8);
                            if (best == null || p.distanceSq(pos) < best.distanceSq(pos)) {
                                best = p;
                            }
                        }
                    }
                }
                if (best != null) {
                    return best;
                }
            }
            return null;
        }
    }
}
