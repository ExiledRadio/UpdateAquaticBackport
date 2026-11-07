package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.util.List;

/**
 * Where and how fish spawn: current Java's water ambient spawn lists, picked by {@link OceanKind} so every
 * ocean of every biome mod gets them, and current Java's group sizes.
 *
 * <table>
 *   <tr><th>Where</th><th>Spawns (weight, group)</th></tr>
 *   <tr><td>ocean, deep ocean</td><td>cod 10, 3-6</td></tr>
 *   <tr><td>cold ocean</td><td>cod 15, 3-6; salmon 15, 1-5</td></tr>
 *   <tr><td>frozen ocean</td><td>salmon 15, 1-5</td></tr>
 *   <tr><td>lukewarm ocean</td><td>cod 15 (8 when deep), 3-6; pufferfish 5, 1-3; tropical fish 25, 8</td></tr>
 *   <tr><td>warm ocean</td><td>pufferfish 15, 1-3; tropical fish 25, 8</td></tr>
 *   <tr><td>river, frozen river</td><td>salmon 5, 1-5</td></tr>
 * </table>
 *
 * <h2>Groups</h2>
 * 1.12.2's spawner ignores a spawn entry's group size and spawns 1-4 at a time with fresh spawn data each
 * time, which would give schools of mixed tropical fish. So the spawner spawns one fish per round
 * ({@link EntityAbstractFish#getMaxSpawnedInChunk()}), and that fish brings the rest of its group: as
 * current Java does, each member a random step of up to 5 blocks across from the last, at the same
 * height, every one checked like a natural spawn, all sharing the first one's spawn data (its school and
 * a tropical fish's look). A tropical fish of a random look (1 in 10) spawns alone, as in 1.13.
 *
 * <p>Natural spawns more than 64 blocks from every player are refused, as current Java never spawns a mob
 * beyond the distance at which it despawns.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class FishSpawning {

    static final Biome.SpawnListEntry COD_10 = new Biome.SpawnListEntry(EntityCod.class, 10, 3, 6);
    static final Biome.SpawnListEntry COD_15 = new Biome.SpawnListEntry(EntityCod.class, 15, 3, 6);
    static final Biome.SpawnListEntry COD_8 = new Biome.SpawnListEntry(EntityCod.class, 8, 3, 6);
    static final Biome.SpawnListEntry SALMON_15 = new Biome.SpawnListEntry(EntitySalmon.class, 15, 1, 5);
    static final Biome.SpawnListEntry SALMON_RIVER = new Biome.SpawnListEntry(EntitySalmon.class, 5, 1, 5);
    static final Biome.SpawnListEntry PUFFERFISH_5 = new Biome.SpawnListEntry(EntityPufferfish.class, 5, 1, 3);
    static final Biome.SpawnListEntry PUFFERFISH_15 = new Biome.SpawnListEntry(EntityPufferfish.class, 15, 1, 3);
    static final Biome.SpawnListEntry TROPICAL_25 = new Biome.SpawnListEntry(EntityTropicalFish.class, 25, 8, 8);

    private FishSpawning() {
    }

    /** @return true if natural spawning is on (stream on and {@code fish.spawn}) */
    public static boolean spawns() {
        return FishContent.enabled() && ModConfig.fish.spawn && FishContent.waterAmbient() != null;
    }

    /**
     * Adds current Java's fish to the water ambient list at a position. The same entry objects every
     * time: the spawner checks a picked entry is still in the list by identity.
     */
    @SubscribeEvent
    public static void potentialSpawns(WorldEvent.PotentialSpawns event) {
        if (!spawns() || event.getType() != FishContent.waterAmbient()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        Biome biome = world.getBiome(pos);
        add(event.getList(), biome, OceanKind.of(biome, world.getSeed(), pos.getX(), pos.getZ()));
    }

    /** Fills a spawn list for a biome of this ocean kind. */
    static void add(List<Biome.SpawnListEntry> list, Biome biome, OceanKind kind) {
        switch (kind) {
            case NORMAL:
                list.add(COD_10);
                break;
            case COLD:
                list.add(COD_15);
                list.add(SALMON_15);
                break;
            case FROZEN:
                list.add(SALMON_15);
                break;
            case LUKEWARM:
                list.add(isDeep(biome) ? COD_8 : COD_15);
                list.add(PUFFERFISH_5);
                list.add(TROPICAL_25);
                break;
            case WARM:
                list.add(PUFFERFISH_15);
                list.add(TROPICAL_25);
                break;
            default:
                if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                    list.add(SALMON_RIVER);
                }
                break;
        }
    }

    /** Deep oceans sit at -1.8 (vanilla's), ordinary ones at -1.0. */
    private static boolean isDeep(Biome biome) {
        return biome.getBaseHeight() <= -1.5F;
    }

    /**
     * The natural spawner is about to spawn a fish. As in current Java, not beyond its despawn distance
     * (64 blocks from every player: 1.12.2 spawns up to 128 out, where a fish would vanish at once);
     * otherwise it will bring its group once set up.
     */
    @SubscribeEvent
    public static void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (!(event.getEntityLiving() instanceof EntityAbstractFish) || event.isSpawner() || !spawns()) {
            return;
        }
        if (event.getWorld().getClosestPlayer(event.getX(), event.getY(), event.getZ(), 64.0D, false) == null) {
            event.setResult(Event.Result.DENY);
            return;
        }
        ((EntityAbstractFish) event.getEntityLiving()).naturalSpawn = true;
    }

    /**
     * Spawns the rest of a naturally spawned fish's group, called from its {@code onInitialSpawn} before
     * it joins the world. Members never overlap the first fish (which still has to pass its own
     * collision check).
     */
    static void spawnGroup(EntityAbstractFish first, DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        World world = first.world;
        if (first.spawnsAlone()) {
            return;
        }
        int size = first.minGroup() + world.rand.nextInt(first.maxGroup() - first.minGroup() + 1);
        int x = MathHelper.floor(first.posX);
        int y = MathHelper.floor(first.posY);
        int z = MathHelper.floor(first.posZ);
        for (int i = 1; i < size; i++) {
            x += world.rand.nextInt(6) - world.rand.nextInt(6);
            z += world.rand.nextInt(6) - world.rand.nextInt(6);
            BlockPos pos = new BlockPos(x, y, z);
            if (!world.isBlockLoaded(pos)
                    || !WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.IN_WATER, world, pos)
                    || world.isAnyPlayerWithinRangeAt(x + 0.5D, y, z + 0.5D, 24.0D)) {
                continue;
            }
            EntityAbstractFish mate = create(first, world);
            if (mate == null) {
                return;
            }
            mate.setLocationAndAngles(x + 0.5D, y, z + 0.5D, world.rand.nextFloat() * 360.0F, 0.0F);
            Event.Result allowed = ForgeEventFactory.canEntitySpawn(mate, world, x + 0.5F, y, z + 0.5F, false);
            mate.naturalSpawn = false;
            boolean ok = allowed == Event.Result.ALLOW
                    || allowed == Event.Result.DEFAULT && mate.getCanSpawnHere() && mate.isNotColliding();
            if (!ok || mate.getEntityBoundingBox().intersects(first.getEntityBoundingBox())) {
                continue;
            }
            if (!ForgeEventFactory.doSpecialSpawn(mate, world, x + 0.5F, y, z + 0.5F)) {
                data = mate.onInitialSpawn(difficulty, data);
            }
            world.spawnEntity(mate);
        }
    }

    @Nullable
    private static EntityAbstractFish create(EntityAbstractFish like, World world) {
        try {
            Constructor<? extends EntityAbstractFish> c = like.getClass().getConstructor(World.class);
            return c.newInstance(world);
        } catch (ReflectiveOperationException e) {
            UpdateAquaticBackport.LOGGER.warn("Could not spawn a {} group member", like.getClass().getSimpleName(), e);
            return null;
        }
    }
}
