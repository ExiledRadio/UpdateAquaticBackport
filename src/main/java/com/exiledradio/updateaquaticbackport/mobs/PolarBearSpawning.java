package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.init.Blocks;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Polar bears spawn on the ice of frozen oceans (1.13).
 *
 * <p>Current Java's rule ({@code PolarBear.checkPolarBearSpawnRules}): in a frozen or deep frozen ocean a
 * polar bear spawns only on ice, in light above 8; anywhere else, as any animal, on grass. Frozen oceans
 * also list polar bears (weight 1, groups of 1-2). Here "frozen ocean" is {@link OceanKind#FROZEN}: 1.12.2's
 * frozen ocean, the oceans stream's frozen oceans, and any other mod's snowy ocean.
 *
 * <p>1.12.2 never spawns anything on ice: ice is not solid enough for its spawn placement check, and its
 * animals need grass. So, with no patch:
 * <ul>
 *   <li>the polar bear gets its own placement type, vanilla's on-ground check or standing on ice;</li>
 *   <li>Forge's spawn check event gives current Java's answer for a polar bear in a frozen ocean (yes on
 *       ice in light above 8, no anywhere else there), which covers the regular spawning cycle;</li>
 *   <li>chunk generation, which 1.12.2 runs with the on-ground check written in, gets the polar bears' part
 *       of it again once the chunk is fully populated - its ice is frozen only at the end - with the ice
 *       allowed;</li>
 *   <li>every frozen ocean without a polar bear entry gets one, at post-init.</li>
 * </ul>
 * Spawners are left alone, as are polar bears outside frozen oceans.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class PolarBearSpawning {

    /** 1.14.4's frozen ocean entry: weight 1, groups of 1 to 2. */
    public static final int WEIGHT = 1, MIN_GROUP = 1, MAX_GROUP = 2;

    private static EntityLiving.SpawnPlacementType placement;

    private PolarBearSpawning() {
    }

    private static boolean on() {
        return MobsContent.enabled() && ModConfig.mobs.polarBearsOnIce;
    }

    /** From post-init: the placement type and the frozen oceans' spawn entries. */
    static void register() {
        if (placement == null) {
            placement = EnumHelper.addSpawnPlacementType("UPDATEAQUATICBACKPORT_ON_GROUND_OR_ICE",
                    PolarBearSpawning::canStandAt);
        }
        EntitySpawnPlacementRegistry.setPlacementType(EntityPolarBear.class, placement);
        if (!ModConfig.mobs.polarBearsOnIce) {
            return;
        }
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (OceanKind.of(biome, 0L, 0, 0) != OceanKind.FROZEN) {
                continue;
            }
            List<Biome.SpawnListEntry> creatures = biome.getSpawnableList(EnumCreatureType.CREATURE);
            boolean listed = false;
            for (Biome.SpawnListEntry entry : creatures) {
                listed |= entry.entityClass == EntityPolarBear.class;
            }
            if (!listed) {
                creatures.add(new Biome.SpawnListEntry(EntityPolarBear.class, WEIGHT, MIN_GROUP, MAX_GROUP));
            }
        }
    }

    /** The polar bear's placement: vanilla's on-ground check, or standing on ice. */
    static boolean canStandAt(IBlockAccess access, BlockPos pos) {
        World world = (World) access;
        if (WorldEntitySpawner.canCreatureTypeSpawnBody(EntityLiving.SpawnPlacementType.ON_GROUND, world, pos)) {
            return true;
        }
        return on() && onIce(world, pos);
    }

    /** @return true if the block below is ice and the two blocks of the body are free */
    static boolean onIce(World world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() == Blocks.ICE
                && WorldEntitySpawner.isValidEmptySpawnBlock(world.getBlockState(pos))
                && WorldEntitySpawner.isValidEmptySpawnBlock(world.getBlockState(pos.up()));
    }

    static boolean inFrozenOcean(World world, BlockPos pos) {
        return OceanKind.at(world, pos) == OceanKind.FROZEN;
    }

    /** Current Java's rule for a polar bear in a frozen ocean. */
    @SubscribeEvent
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntityLiving() instanceof EntityPolarBear) || event.isSpawner() || event.getWorld().isRemote || !on()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
        if (!inFrozenOcean(world, pos)) {
            return;
        }
        EntityLiving bear = (EntityLiving) event.getEntityLiving();
        boolean allowed = world.getBlockState(pos.down()).getBlock() == Blocks.ICE && world.getLight(pos) > 8
                && bear.isNotColliding();
        event.setResult(allowed ? Event.Result.ALLOW : Event.Result.DENY);
    }

    @SubscribeEvent
    public static void onPopulated(PopulateChunkEvent.Post event) {
        World world = event.getWorld();
        if (world.isRemote || !on()) {
            return;
        }
        int x = event.getChunkX() * 16 + 8;
        int z = event.getChunkZ() * 16 + 8;
        // Vanilla populates the chunk from its corner and picks the biome half a chunk further in.
        Biome biome = world.getBiome(new BlockPos(x + 8, 0, z + 8));
        if (OceanKind.of(biome, world.getSeed(), x + 8, z + 8) == OceanKind.FROZEN) {
            spawnOnIce(world, biome, x, z, 16, 16, event.getRand());
        }
    }

    /**
     * 1.12.2's {@code WorldEntitySpawner.performWorldGenSpawning} for the polar bear entries of a biome's
     * creature list, with the ice allowed: the same chance per pass, the same weighted pick (any other pick
     * is left to vanilla's own pass), the same group walk.
     *
     * @return the number of polar bears spawned
     */
    public static int spawnOnIce(World world, Biome biome, int centerX, int centerZ, int sizeX, int sizeZ, Random random) {
        List<Biome.SpawnListEntry> list = biome.getSpawnableList(EnumCreatureType.CREATURE);
        if (list.isEmpty()) {
            return 0;
        }
        int spawned = 0;
        while (random.nextFloat() < biome.getSpawningChance()) {
            Biome.SpawnListEntry entry = WeightedRandom.getRandomItem(world.rand, list);
            if (entry.entityClass != EntityPolarBear.class) {
                continue;
            }
            int count = entry.minGroupCount + random.nextInt(1 + entry.maxGroupCount - entry.minGroupCount);
            IEntityLivingData data = null;
            int x = centerX + random.nextInt(sizeX);
            int z = centerZ + random.nextInt(sizeZ);
            int startX = x;
            int startZ = z;
            for (int i = 0; i < count; ++i) {
                boolean done = false;
                for (int tries = 0; !done && tries < 4; ++tries) {
                    BlockPos pos = world.getTopSolidOrLiquidBlock(new BlockPos(x, 0, z));
                    if (world.getWorldBorder().contains(pos) && onIce(world, pos)) {
                        EntityPolarBear bear = new EntityPolarBear(world);
                        bear.setLocationAndAngles((double) ((float) x + 0.5F), (double) pos.getY(), (double) ((float) z + 0.5F),
                                random.nextFloat() * 360.0F, 0.0F);
                        Event.Result result = ForgeEventFactory.canEntitySpawn(bear, world, x + 0.5F, (float) pos.getY(), z + 0.5F, false);
                        if (result != Event.Result.DENY) {
                            world.spawnEntity(bear);
                            data = bear.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(bear)), data);
                            spawned++;
                            done = true;
                        }
                    }
                    x += random.nextInt(5) - random.nextInt(5);
                    for (z += random.nextInt(5) - random.nextInt(5);
                         x < centerX || x >= centerX + sizeX || z < centerZ || z >= centerZ + sizeX;
                         z = startZ + random.nextInt(5) - random.nextInt(5)) {
                        x = startX + random.nextInt(5) - random.nextInt(5);
                    }
                }
            }
        }
        return spawned;
    }

    /** For the self-test: the polar bear entries of a biome's creature list. */
    public static List<Biome.SpawnListEntry> entries(Biome biome) {
        List<Biome.SpawnListEntry> out = new ArrayList<Biome.SpawnListEntry>();
        for (Biome.SpawnListEntry entry : biome.getSpawnableList(EnumCreatureType.CREATURE)) {
            if (entry.entityClass == EntityPolarBear.class) {
                out.add(entry);
            }
        }
        return out;
    }
}
