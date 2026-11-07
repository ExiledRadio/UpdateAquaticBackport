package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.fish.EntityAbstractFish;
import com.exiledradio.updateaquaticbackport.fish.EntityAbstractGroupFish;
import com.exiledradio.updateaquaticbackport.fish.EntityCod;
import com.exiledradio.updateaquaticbackport.fish.EntityPufferfish;
import com.exiledradio.updateaquaticbackport.fish.EntitySalmon;
import com.exiledradio.updateaquaticbackport.fish.EntityTropicalFish;
import com.exiledradio.updateaquaticbackport.fish.FishContent;
import com.exiledradio.updateaquaticbackport.fish.FishSpawning;
import com.exiledradio.updateaquaticbackport.fish.ItemFishBucket;
import com.exiledradio.updateaquaticbackport.fish.TropicalFishVariant;

import net.minecraft.advancements.Advancement;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Fish: registration, swimming, schooling, flopping and suffocating, pufferfish puffing and stinging,
 * buckets both ways (variant, name, health), dispensers, spawn lists and natural groups, drops, saving,
 * the pufferfish's food effects and both advancements.
 */
public final class FishSelfTest implements SelfTest {

    private Field idleTime;

    @Override
    public String name() {
        return "fish";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        MinecraftServer server = world.getMinecraftServer();
        this.idleTime = net.minecraft.entity.EntityLivingBase.class.getDeclaredField("idleTime");
        this.idleTime.setAccessible(true);

        // 1. registration ---------------------------------------------------------------------------
        c.ok(FishContent.enabled(), "fish stream on");
        String[] names = {"cod", "salmon", "pufferfish", "tropical_fish"};
        Class<?>[] classes = {EntityCod.class, EntitySalmon.class, EntityPufferfish.class, EntityTropicalFish.class};
        for (int i = 0; i < names.length; i++) {
            EntityEntry entry = ForgeRegistries.ENTITIES.getValue(Content.id(names[i]));
            c.ok(entry != null && entry.getEntityClass() == classes[i], names[i] + " entity registered");
            c.eq(i + 1, EntityRegistry.instance().lookupModSpawn(classes[i].asSubclass(Entity.class), false).getModEntityId(),
                    names[i] + " entity id");
            c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id(names[i])), names[i] + " has a spawn egg");
            c.eq(UpdateAquaticBackport.MOD_ID + "." + names[i], EntityList.getTranslationName(Content.id(names[i])),
                    names[i] + " translation name");
            Item bucket = Owners.ours(names[i] + "_bucket");
            c.ok(bucket instanceof ItemFishBucket, names[i] + "_bucket registered");
            c.eq(1, bucket == null ? -1 : bucket.getItemStackLimit(new ItemStack(bucket)), names[i] + " bucket stacks to 1");
            IBehaviorDispenseItem behavior = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(bucket);
            c.ok(behavior != null && behavior != IBehaviorDispenseItem.DEFAULT_BEHAVIOR, names[i] + " bucket has a dispenser behavior");
            c.ok(world.getLootTableManager().getLootTableFromLocation(Content.id("entities/" + names[i])) != LootTable.EMPTY_LOOT_TABLE,
                    names[i] + " loot table loads");
        }
        EnumCreatureType ambient = FishContent.waterAmbient();
        c.ok(ambient != null && Arrays.asList(EnumCreatureType.values()).contains(ambient), "water ambient creature type exists");
        if (ambient != null) {
            c.eq(20, ambient.getMaxNumberOfCreature(), "water ambient cap");
            c.ok(ambient.getPeacefulCreature() && !ambient.getAnimal(), "water ambient is peaceful, spawned every tick");
            c.eq(EntityAbstractFish.class, ambient.getCreatureClass(), "water ambient class");
        }
        for (String a : new String[] {"husbandry/fishy_business", "husbandry/tactical_fishing"}) {
            c.ok(server.getAdvancementManager().getAdvancement(Content.id(a)) != null, "advancement " + a + " loads");
        }

        // 2. pufferfish fear rules --------------------------------------------------------------------
        EntityPlayerMP player = Players.real(world, "fish");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        c.ok(EntityPufferfish.isScary(player), "a survival player is scary");
        player.interactionManager.setGameType(GameType.CREATIVE);
        c.ok(!EntityPufferfish.isScary(player), "a creative player is not");
        player.interactionManager.setGameType(GameType.SPECTATOR);
        c.ok(!EntityPufferfish.isScary(player), "a spectator is not");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        c.ok(EntityPufferfish.isScary(new EntityZombie(world)), "a zombie is scary");
        c.ok(!EntityPufferfish.isScary(new EntitySquid(world)), "a squid is not");
        c.ok(!EntityPufferfish.isScary(new EntityGuardian(world)), "a guardian is not");
        c.ok(!EntityPufferfish.isScary(new EntityCod(world)), "a cod is not");
        c.ok(!EntityPufferfish.isScary(new EntityPufferfish(world)), "another pufferfish is not");

        // 3. spawn lists by ocean kind -----------------------------------------------------------------
        spawnLists(c);
        BlockPos ocean = world.getBiomeProvider().findBiomePosition(0, 0, 2000, Arrays.asList(Biomes.OCEAN, Biomes.DEEP_OCEAN), new Random(1));
        if (ocean != null && ambient != null) {
            BlockPos at = new BlockPos(ocean.getX(), 55, ocean.getZ());
            // WorldServer asks the chunk generator, then posts Forge's event, as the spawner does.
            List<Biome.SpawnListEntry> real = ForgeEventFactory.getPotentialSpawns(world, ambient, at,
                    world.getChunkProvider().getPossibleCreatures(ambient, at));
            OceanKind kind = OceanKind.at(world, at);
            c.ok(real != null && !real.isEmpty(), "a real " + kind + " ocean at " + at + " offers fish: " + describe(real));
            c.ok(ForgeEventFactory.getPotentialSpawns(world, EnumCreatureType.WATER_CREATURE, at,
                    world.getChunkProvider().getPossibleCreatures(EnumCreatureType.WATER_CREATURE, at)).stream()
                    .noneMatch(e -> EntityAbstractFish.class.isAssignableFrom(e.entityClass)), "no fish among water creatures");
        } else {
            c.fail("no vanilla ocean within 2000 blocks to check the spawn event against");
        }

        // A deep pool from y=48 to 62, in a 40x40 stone box; sea level 63.
        BlockPos pool = deepPool(c);
        BlockPos mid = pool.add(20, 7, 20);
        loadAround(world, mid);

        // 4. swimming, spawn rules, saving ---------------------------------------------------------------
        for (Class<?> cls : classes) {
            EntityAbstractFish fish = (EntityAbstractFish) cls.getConstructor(net.minecraft.world.World.class).newInstance(world);
            fish.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
            c.ok(fish.getCanSpawnHere(), cls.getSimpleName() + " can spawn at y=" + mid.getY());
            fish.setLocationAndAngles(mid.getX() + 0.5, pool.getY() + 1, mid.getZ() + 0.5, 0, 0);
            c.ok(!fish.getCanSpawnHere(), cls.getSimpleName() + " cannot spawn with no water below");
            fish.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
            fish.onInitialSpawn(world.getDifficultyForLocation(mid), null);
            c.ok(world.spawnEntity(fish), cls.getSimpleName() + " spawned");
            double startX = fish.posX, startZ = fish.posZ;
            double moved = 0;
            for (int t = 0; t < 800; t++) {
                this.tick(world, fish);
                moved = Math.max(moved, Math.hypot(fish.posX - startX, fish.posZ - startZ));
            }
            c.ok(fish.isEntityAlive() && fish.isInWater(), cls.getSimpleName() + " alive in water after 800 ticks");
            c.eq(300, fish.getAir(), cls.getSimpleName() + " air full in water");
            c.ok(moved > 1.0, cls.getSimpleName() + " swam about (" + String.format("%.1f", moved) + " blocks)");
            c.ok(fish.isCreatureType(ambient, true) && !fish.isCreatureType(EnumCreatureType.WATER_CREATURE, true),
                    cls.getSimpleName() + " counts as water ambient only");
            c.eq(1, fish.getMaxSpawnedInChunk(), cls.getSimpleName() + " one per spawner round");

            // save and load
            if (fish instanceof EntityPufferfish) {
                ((EntityPufferfish) fish).setPuffState(2);
            }
            fish.setFromBucket(true);
            fish.setHealth(2.0F);
            NBTTagCompound nbt = new NBTTagCompound();
            c.ok(fish.writeToNBTOptional(nbt), cls.getSimpleName() + " writes to NBT");
            Entity copy = EntityList.createEntityFromNBT(nbt, world);
            c.ok(copy != null && copy.getClass() == cls, cls.getSimpleName() + " loads back as itself");
            if (copy instanceof EntityAbstractFish) {
                EntityAbstractFish back = (EntityAbstractFish) copy;
                c.ok(back.isFromBucket(), cls.getSimpleName() + " keeps FromBucket");
                c.eq(2.0F, back.getHealth(), cls.getSimpleName() + " keeps health");
                c.ok(!back.isCreatureType(ambient, true), cls.getSimpleName() + " from a bucket is left out of the cap");
                if (back instanceof EntityTropicalFish) {
                    c.eq(((EntityTropicalFish) fish).getVariant(), ((EntityTropicalFish) back).getVariant(), "tropical fish keeps its variant");
                }
                if (back instanceof EntityPufferfish) {
                    c.eq(2, ((EntityPufferfish) back).getPuffState(), "pufferfish keeps its puff state");
                    c.ok(Math.abs(back.width - 0.7F) < 1e-4, "a loaded puffed pufferfish is 0.7 wide, got " + back.width);
                }
            }
            fish.setDead();
        }

        // 5. schooling ----------------------------------------------------------------------------------
        List<EntityCod> school = new ArrayList<EntityCod>();
        for (int i = 0; i < 5; i++) {
            EntityCod cod = new EntityCod(world);
            cod.setLocationAndAngles(mid.getX() + 0.5 + i, mid.getY(), mid.getZ() + 0.5, 0, 0);
            world.spawnEntity(cod);
            school.add(cod);
        }
        for (int t = 0; t < 300; t++) {
            for (EntityCod cod : school) {
                this.tick(world, cod);
            }
        }
        // Cod wander for the 10 seconds before their first search, so one may have drifted out of reach.
        int leaders = 0, followers = 0;
        boolean consistent = true;
        for (EntityCod cod : school) {
            leaders += cod.isLeader() ? 1 : 0;
            followers += cod.hasLeader() ? 1 : 0;
            consistent &= !cod.hasLeader() || cod.getLeader().isLeader() && !cod.isLeader();
        }
        c.ok(leaders >= 1 && followers >= 2 && leaders + followers <= 5,
                "5 cod form a school after 300 ticks: leaders=" + leaders + " followers=" + followers);
        c.ok(consistent, "every follower's leader leads, and no leader follows");
        for (EntityCod cod : school) {
            cod.setDead();
        }

        // 6. natural group spawning ----------------------------------------------------------------------
        EntityCod far = new EntityCod(world);
        far.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
        c.eq(net.minecraftforge.fml.common.eventhandler.Event.Result.DENY,
                ForgeEventFactory.canEntitySpawn(far, world, (float) far.posX, (float) far.posY, (float) far.posZ, false),
                "no natural fish spawn with no player within 64 blocks");
        // A player 40 blocks above: near enough for fish to spawn, beyond the spawner's 24-block minimum.
        player.setPosition(mid.getX() + 0.5, mid.getY() + 40, mid.getZ() + 0.5);
        world.playerEntities.add(player);
        c.eq(net.minecraftforge.fml.common.eventhandler.Event.Result.DEFAULT,
                ForgeEventFactory.canEntitySpawn(far, world, (float) far.posX, (float) far.posY, (float) far.posZ, false),
                "with a player 40 blocks away they may");
        try {
        naturalGroups(c, world, mid, EntityTropicalFish.class, 8, 8);
        naturalGroups(c, world, mid, EntityCod.class, 3, 6);
        naturalGroups(c, world, mid, EntitySalmon.class, 1, 5);
        naturalGroups(c, world, mid, EntityPufferfish.class, 1, 3);
        } finally {
            world.playerEntities.remove(player);
        }

        // 7. flopping and suffocating on land --------------------------------------------------------------
        BlockPos land = c.area();
        loadAround(world, land.add(16, 0, 16));
        EntitySalmon dry = new EntitySalmon(world);
        dry.setLocationAndAngles(land.getX() + 16.5, land.getY(), land.getZ() + 16.5, 0, 0);
        world.spawnEntity(dry);
        boolean hopped = false;
        int firstHurt = -1;
        for (int t = 1; t <= 335 && dry.isEntityAlive(); t++) {
            this.tick(world, dry);
            hopped |= dry.posY > land.getY() + 0.05;
            if (firstHurt < 0 && dry.getHealth() < 3.0F) {
                firstHurt = t;
            }
        }
        c.ok(hopped, "a fish on land flops");
        c.ok(firstHurt >= 315 && firstHurt <= 325, "suffocation starts after 16 seconds, at tick " + firstHurt);
        c.eq(1.0F, dry.getHealth(), "one suffocation hit of 2");
        dry.setDead();

        // 8. pufferfish puffing and stinging ----------------------------------------------------------------
        pufferfish(c, world, mid, player);

        // 9. buckets -----------------------------------------------------------------------------------------
        buckets(c, world, mid, pool, player);

        // 10. drops ------------------------------------------------------------------------------------------
        drops(c, world, player);

        // 11. pufferfish food -------------------------------------------------------------------------------
        player.clearActivePotions();
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 1, 3));
        player.setActiveHand(EnumHand.MAIN_HAND);
        c.ok(player.isHandActive(), "eating a pufferfish");
        player.getActiveItemStack().onItemUseFinish(world, player);
        PotionEffect poison = player.getActivePotionEffect(MobEffects.POISON);
        PotionEffect nausea = player.getActivePotionEffect(MobEffects.NAUSEA);
        PotionEffect hunger = player.getActivePotionEffect(MobEffects.HUNGER);
        c.ok(poison != null && poison.getAmplifier() == 1 && poison.getDuration() == 1200, "pufferfish: Poison II for 60 s, got " + poison);
        c.ok(nausea != null && nausea.getAmplifier() == 0 && nausea.getDuration() == 300, "pufferfish: Nausea I for 15 s, got " + nausea);
        c.ok(hunger != null && hunger.getAmplifier() == 2 && hunger.getDuration() == 300, "pufferfish: Hunger III for 15 s, got " + hunger);
        player.resetActiveHand();
        player.clearActivePotions();
        player.addPotionEffect(new PotionEffect(MobEffects.POISON, 1200, 3));
        c.eq(3, player.getActivePotionEffect(MobEffects.POISON).getAmplifier(), "Poison IV from anything else is left alone");
        player.clearActivePotions();

        // 12. Fishy Business ----------------------------------------------------------------------------------
        Advancement fishy = server.getAdvancementManager().getAdvancement(Content.id("husbandry/fishy_business"));
        Advancement tactical = server.getAdvancementManager().getAdvancement(Content.id("husbandry/tactical_fishing"));
        if (fishy != null) {
            c.ok(!player.getAdvancements().getProgress(fishy).isDone(), "Fishy Business not yet done");
            EntityFishHook hook = new EntityFishHook(world, player);
            MinecraftForge.EVENT_BUS.post(new ItemFishedEvent(Arrays.asList(new ItemStack(Items.STICK)), 1, hook));
            c.ok(!player.getAdvancements().getProgress(fishy).isDone(), "a stick is not a fish");
            MinecraftForge.EVENT_BUS.post(new ItemFishedEvent(Arrays.asList(new ItemStack(Items.FISH, 1, 1)), 1, hook));
            c.ok(player.getAdvancements().getProgress(fishy).isDone(), "reeling in a salmon: Fishy Business");
            hook.setDead();
        }
        if (tactical != null) {
            c.ok(player.getAdvancements().getProgress(tactical).isDone(), "bucketing a fish: Tactical Fishing");
        }
    }

    // -------------------------------------------------------------------------------------------------

    /**
     * Loads the chunks within 48 blocks of a position: the world only ticks an entity whose surroundings
     * (32 blocks) are loaded.
     */
    private static void loadAround(WorldServer world, BlockPos pos) {
        for (int cx = (pos.getX() - 48) >> 4; cx <= (pos.getX() + 48) >> 4; cx++) {
            for (int cz = (pos.getZ() - 48) >> 4; cz <= (pos.getZ() + 48) >> 4; cz++) {
                world.getChunkProvider().provideChunk(cx, cz);
            }
        }
    }

    private void tick(WorldServer world, EntityLiving e) throws IllegalAccessException {
        // No player is near the test area, so a real idle timer would stop all wandering after 5 seconds.
        this.idleTime.setInt(e, 0);
        world.updateEntityWithOptionalForce(e, true);
    }

    private static String describe(List<Biome.SpawnListEntry> list) {
        StringBuilder b = new StringBuilder();
        if (list != null) {
            for (Biome.SpawnListEntry e : list) {
                b.append(e.entityClass.getSimpleName()).append('x').append(e.itemWeight).append(' ');
            }
        }
        return b.toString().trim();
    }

    private static void spawnLists(Check c) throws Exception {
        java.lang.reflect.Method add = FishSpawning.class.getDeclaredMethod("add", List.class, Biome.class, OceanKind.class);
        add.setAccessible(true);
        Object[][] cases = {
                {Biomes.OCEAN, OceanKind.NORMAL, 10, "EntityCodx10"},
                {Biomes.DEEP_OCEAN, OceanKind.COLD, 30, "EntityCodx15 EntitySalmonx15"},
                {Biomes.FROZEN_OCEAN, OceanKind.FROZEN, 15, "EntitySalmonx15"},
                {Biomes.OCEAN, OceanKind.LUKEWARM, 45, "EntityCodx15 EntityPufferfishx5 EntityTropicalFishx25"},
                {Biomes.DEEP_OCEAN, OceanKind.LUKEWARM, 38, "EntityCodx8 EntityPufferfishx5 EntityTropicalFishx25"},
                {Biomes.OCEAN, OceanKind.WARM, 40, "EntityPufferfishx15 EntityTropicalFishx25"},
                {Biomes.RIVER, OceanKind.NONE, 5, "EntitySalmonx5"},
                {Biomes.FROZEN_RIVER, OceanKind.NONE, 5, "EntitySalmonx5"},
                {Biomes.PLAINS, OceanKind.NONE, 0, ""},
        };
        for (Object[] k : cases) {
            List<Biome.SpawnListEntry> list = new ArrayList<Biome.SpawnListEntry>();
            add.invoke(null, list, k[0], k[1]);
            int total = 0;
            for (Biome.SpawnListEntry e : list) {
                total += e.itemWeight;
            }
            c.eq(k[3], describe(list), "spawn list for " + ((Biome) k[0]).getRegistryName() + " as " + k[1]);
            c.eq(k[2], total, "total weight for " + ((Biome) k[0]).getRegistryName() + " as " + k[1]);
        }
    }

    /** @return the stone floor corner (y=47) of a 40x40 pool of still water from y=48 to y=62, sea level 63 */
    private static BlockPos deepPool(Check c) {
        BlockPos corner = new BlockPos(-20000, 47, -20000);
        for (int x = -1; x <= 40; x++) {
            for (int z = -1; z <= 40; z++) {
                boolean wall = x < 0 || z < 0 || x >= 40 || z >= 40;
                for (int y = 0; y <= 30; y++) {
                    IBlockState s;
                    if (y == 0 || wall && y <= 16) {
                        s = Blocks.STONE.getDefaultState();
                    } else if (y <= 15) {
                        s = Blocks.WATER.getDefaultState();
                    } else {
                        s = Blocks.AIR.getDefaultState();
                    }
                    c.world.setBlockState(corner.add(x, y, z), s, 2 | 16);
                }
            }
        }
        return corner;
    }

    /** @return the living entities of a class in a box (dead ones stay listed until the next world tick) */
    private static <T extends Entity> List<T> alive(WorldServer world, Class<T> cls, AxisAlignedBB box) {
        return world.getEntitiesWithinAABB(cls, box, e -> e != null && !e.isDead);
    }

    private void naturalGroups(Check c, WorldServer world, BlockPos mid, Class<? extends EntityAbstractFish> cls, int min, int max)
            throws Exception {
        int groups = 0, sameLook = 0, checked = 0;
        boolean sizesOk = true;
        for (int g = 0; g < 6; g++) {
            EntityAbstractFish first = cls.getConstructor(net.minecraft.world.World.class).newInstance(world);
            first.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
            if (ForgeEventFactory.canEntitySpawn(first, world, (float) first.posX, (float) first.posY, (float) first.posZ, false)
                    == net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
                continue;
            }
            IEntityLivingData data = first.onInitialSpawn(world.getDifficultyForLocation(mid), null);
            if (!first.isNotColliding()) {
                continue;
            }
            world.spawnEntity(first);
            List<? extends EntityAbstractFish> group = alive(world, cls, new AxisAlignedBB(mid).grow(40));
            groups++;
            sizesOk &= group.size() >= 1 && group.size() <= max;
            if (min == max && !first.spawnsAlone() && group.size() < min / 2) {
                sizesOk = false;
            }
            if (first instanceof EntityTropicalFish) {
                int variant = ((EntityTropicalFish) first).getVariant();
                boolean predefined = TropicalFishVariant.predefinedIndex(variant) >= 0;
                if (first.spawnsAlone()) {
                    c.eq(1, group.size(), "a tropical fish of a random look spawns alone");
                }
                if (predefined && group.size() > 1) {
                    checked++;
                    boolean all = true;
                    for (EntityAbstractFish f : group) {
                        all &= ((EntityTropicalFish) f).getVariant() == variant;
                    }
                    sameLook += all ? 1 : 0;
                }
            }
            if (first instanceof EntityAbstractGroupFish && group.size() > 1) {
                boolean led = true;
                StringBuilder why = new StringBuilder();
                for (EntityAbstractFish f : group) {
                    boolean ok = f == first || ((EntityAbstractGroupFish) f).getLeader() == first;
                    led &= ok;
                    if (!ok) {
                        EntityAbstractGroupFish l = ((EntityAbstractGroupFish) f).getLeader();
                        why.append(" [").append(f.getEntityId()).append(" age ").append(f.ticksExisted).append(" leader ")
                                .append(l == null ? "none" : l.getEntityId() + (l.isDead ? " dead" : "")).append(']');
                    }
                }
                c.ok(led, cls.getSimpleName() + " group of " + group.size() + " all follow the first " + first.getEntityId() + why);
            }
            if (g == 0 && !first.spawnsAlone()) {
                c.ok(group.size() >= Math.min(min, 2) || max == 1, cls.getSimpleName() + " natural spawn brings a group: " + group.size());
            }
            for (EntityAbstractFish f : group) {
                f.setDead();
                world.removeEntityDangerously(f);
            }
        }
        c.ok(groups > 0, cls.getSimpleName() + " natural spawns happened");
        c.ok(sizesOk, cls.getSimpleName() + " group sizes within " + min + "-" + max);
        if (cls == EntityTropicalFish.class) {
            c.eq(checked, sameLook, "predefined tropical schools share one look");
        }
    }

    private void pufferfish(Check c, WorldServer world, BlockPos mid, EntityPlayerMP player) throws Exception {
        EntityPufferfish puffer = new EntityPufferfish(world);
        puffer.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
        world.spawnEntity(puffer);
        c.ok(Math.abs(puffer.width - 0.35F) < 1e-4, "deflated pufferfish is 0.35 wide");
        for (int t = 0; t < 20; t++) {
            this.tick(world, puffer);
        }
        c.eq(0, puffer.getPuffState(), "alone, a pufferfish stays deflated");

        EntityPig pig = new EntityPig(world);
        pig.setLocationAndAngles(puffer.posX + 1.0, puffer.posY, puffer.posZ, 0, 0);
        world.spawnEntity(pig);
        int half = -1, full = -1;
        for (int t = 1; t <= 80; t++) {
            pig.setPosition(puffer.posX + 1.0, puffer.posY, puffer.posZ);
            this.tick(world, puffer);
            if (half < 0 && puffer.getPuffState() == 1) {
                half = t;
            }
            if (full < 0 && puffer.getPuffState() == 2) {
                full = t;
            }
        }
        c.ok(half > 0 && half <= 5, "a pig within 2 blocks: half puffed at tick " + half);
        c.ok(full - half >= 38 && full - half <= 44, "fully puffed 2 seconds later, at tick " + full);
        c.ok(Math.abs(puffer.width - 0.7F) < 1e-4, "fully puffed pufferfish is 0.7 wide, got " + puffer.width);

        // Sting: the pig touching it.
        float before = pig.getHealth();
        pig.setPosition(puffer.posX, puffer.posY, puffer.posZ);
        pig.hurtResistantTime = 0;
        this.tick(world, puffer);
        c.ok(pig.getHealth() < before, "a touching pig is stung (" + before + " -> " + pig.getHealth() + ")");
        PotionEffect poison = pig.getActivePotionEffect(MobEffects.POISON);
        c.ok(poison != null && poison.getDuration() >= 118, "and poisoned for 6 seconds: " + poison);

        // Sting: a player touching it.
        Field invulnerable = EntityPlayerMP.class.getDeclaredField("respawnInvulnerabilityTicks");
        invulnerable.setAccessible(true);
        invulnerable.setInt(player, 0);
        player.setHealth(20.0F);
        player.clearActivePotions();
        player.hurtResistantTime = 0;
        puffer.onCollideWithPlayer(player);
        c.ok(player.getHealth() < 20.0F, "a player touching a puffed pufferfish is stung: " + player.getHealth());
        PotionEffect pp = player.getActivePotionEffect(MobEffects.POISON);
        c.ok(pp != null && pp.getDuration() == 120 && pp.getAmplifier() == 0, "Poison I for 6 seconds: " + pp);
        player.clearActivePotions();
        player.setHealth(20.0F);

        // Deflating once alone.
        pig.setDead();
        world.removeEntityDangerously(pig);
        int down1 = -1, down0 = -1;
        for (int t = 1; t <= 200; t++) {
            this.tick(world, puffer);
            if (down1 < 0 && puffer.getPuffState() == 1) {
                down1 = t;
            }
            if (down0 < 0 && puffer.getPuffState() == 0) {
                down0 = t;
            }
        }
        c.ok(down1 >= 60 && down1 <= 66, "alone again: half deflated after 3 seconds, tick " + down1);
        c.ok(down0 >= 100 && down0 <= 106, "deflated after 5, tick " + down0);
        c.ok(Math.abs(puffer.width - 0.35F) < 1e-4, "and small again");
        puffer.setDead();
    }

    private void buckets(Check c, WorldServer world, BlockPos mid, BlockPos pool, EntityPlayerMP player) throws Exception {
        player.interactionManager.setGameType(GameType.SURVIVAL);
        Item tropicalBucket = Owners.ours("tropical_fish_bucket");
        Item codBucket = Owners.ours("cod_bucket");

        // Scooping up a named tropical fish.
        EntityTropicalFish fish = new EntityTropicalFish(world);
        fish.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 0.5, 0, 0);
        fish.onInitialSpawn(world.getDifficultyForLocation(mid), null);
        int variant = TropicalFishVariant.pack(1, 3, 10, 5);
        fish.setVariant(variant);
        fish.setCustomNameTag("Wanda");
        fish.setHealth(2.5F);
        world.spawnEntity(fish);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        c.ok(player.interactOn(fish, EnumHand.MAIN_HAND) == net.minecraft.util.EnumActionResult.SUCCESS,
                "a water bucket on a tropical fish");
        ItemStack held = player.getHeldItem(EnumHand.MAIN_HAND);
        c.eq(tropicalBucket, held.getItem(), "gives a bucket of tropical fish");
        c.ok(fish.isDead, "and the fish is gone");
        c.ok(held.hasTagCompound() && held.getTagCompound().getInteger("BucketVariantTag") == variant, "the bucket keeps the look");
        c.eq("Wanda", held.getDisplayName(), "and the name");
        c.ok(held.hasTagCompound() && held.getTagCompound().getFloat("Health") == 2.5F, "and the health");

        // Letting it out by right-clicking the floor of a dry pit.
        BlockPos pit = c.area();
        BlockPos target = pit.add(16, 0, 16);
        player.setLocationAndAngles(target.getX() + 0.5, target.getY() + 2, target.getZ() + 0.5, 0, 90.0F);
        net.minecraft.util.ActionResult<ItemStack> used = held.getItem().onItemRightClick(world, player, EnumHand.MAIN_HAND);
        c.eq(net.minecraft.util.EnumActionResult.SUCCESS, used.getType(), "emptying the bucket on the floor");
        c.eq(Items.BUCKET, used.getResult().getItem(), "leaves an empty bucket");
        c.eq(Material.WATER, world.getBlockState(target).getMaterial(), "places water on the floor");
        List<EntityTropicalFish> out = alive(world, EntityTropicalFish.class, new AxisAlignedBB(target).grow(1));
        c.eq(1, out.size(), "and lets one tropical fish out");
        if (!out.isEmpty()) {
            EntityTropicalFish back = out.get(0);
            c.eq(variant, back.getVariant(), "with its look");
            c.eq("Wanda", back.getCustomNameTag(), "its name");
            c.eq(2.5F, back.getHealth(), "its health");
            c.ok(back.isFromBucket(), "and it never despawns");
            back.setDead();
        }

        // Tooltip keys exist for any variant.
        c.eq(5, TropicalFishVariant.predefinedIndex(TropicalFishVariant.pack(0, 0, 1, 0)), "orange-white kob is a clownfish");
        c.eq(-1, TropicalFishVariant.predefinedIndex(variant), "a purple-lime blockfish is not one of the 22");
        c.eq("entity.updateaquaticbackport.tropical_fish.type.blockfish", TropicalFishVariant.typeKey(variant), "type key");
        c.eq("color.updateaquaticbackport.purple", TropicalFishVariant.colorKey(TropicalFishVariant.bodyColor(variant)), "body color key");
        c.eq("color.updateaquaticbackport.lime", TropicalFishVariant.colorKey(TropicalFishVariant.patternColor(variant)), "pattern color key");

        // A cod into kelp: kelp is already water and stays.
        Block kelp = Owners.ourBlock("kelp");
        ItemFishBucket cb = (ItemFishBucket) codBucket;
        if (kelp != null) {
            BlockPos k = mid.add(5, 0, 5);
            world.setBlockState(k.down(), Blocks.SAND.getDefaultState(), 2);
            world.setBlockState(k, kelp.getDefaultState(), 2);
            Block before = world.getBlockState(k).getBlock();
            c.ok(cb.empty(player, world, k, new ItemStack(cb)), "a bucket of cod emptied into kelp");
            c.eq(before, world.getBlockState(k).getBlock(), "keeps the kelp");
            c.eq(1, alive(world, EntityCod.class, new AxisAlignedBB(k).grow(1)).size(), "with the cod in it");
            for (EntityCod cod : alive(world, EntityCod.class, new AxisAlignedBB(k).grow(1))) {
                cod.setDead();
            }
        }
        c.ok(!cb.empty(player, world, pool.add(-1, 5, 5), new ItemStack(cb)), "a bucket cannot be emptied into stone");

        // Dispenser.
        BlockPos d = pit.add(4, 0, 4);
        world.setBlockState(d, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 2);
        ItemStack dispensed = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(codBucket)
                .dispense(new BlockSourceImpl(world, d), new ItemStack(codBucket));
        c.eq(Items.BUCKET, dispensed.getItem(), "a dispenser empties a bucket of cod");
        c.eq(Material.WATER, world.getBlockState(d.east()).getMaterial(), "placing water in front");
        List<EntityCod> cods = alive(world, EntityCod.class, new AxisAlignedBB(d.east()).grow(1));
        c.eq(1, cods.size(), "and the cod");
        for (EntityCod cod : cods) {
            c.ok(cod.isFromBucket(), "a dispensed cod never despawns");
            cod.setDead();
        }

        // Creative keeps the water bucket and gets the fish bucket once.
        player.interactionManager.setGameType(GameType.CREATIVE);
        player.inventory.clear();
        EntitySalmon salmon = new EntitySalmon(world);
        salmon.setLocationAndAngles(mid.getX() + 0.5, mid.getY(), mid.getZ() + 3.5, 0, 0);
        world.spawnEntity(salmon);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        player.interactOn(salmon, EnumHand.MAIN_HAND);
        c.eq(Items.WATER_BUCKET, player.getHeldItem(EnumHand.MAIN_HAND).getItem(), "creative keeps the water bucket");
        c.ok(player.inventory.hasItemStack(new ItemStack(Owners.ours("salmon_bucket"))), "and gets a bucket of salmon");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.inventory.clear();
    }

    private void drops(Check c, WorldServer world, EntityPlayerMP player) throws Exception {
        BlockPos land = c.area();
        Object[][] cases = {
                {EntityCod.class, 0, false}, {EntitySalmon.class, 1, false}, {EntityTropicalFish.class, 2, false},
                {EntityPufferfish.class, 3, false}, {EntityCod.class, 0, true}, {EntitySalmon.class, 1, true},
        };
        int i = 0;
        for (Object[] k : cases) {
            @SuppressWarnings("unchecked")
            Class<? extends EntityAbstractFish> cls = (Class<? extends EntityAbstractFish>) k[0];
            int meta = (Integer) k[1];
            boolean fire = (Boolean) k[2];
            BlockPos at = land.add(4 + 4 * i++, 0, 16);
            EntityAbstractFish fish = cls.getConstructor(net.minecraft.world.World.class).newInstance(world);
            fish.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
            world.spawnEntity(fish);
            if (fire) {
                fish.setFire(10);
            }
            fish.attackEntityFrom(DamageSource.causePlayerDamage(player), 100.0F);
            Item want = fire ? Items.COOKED_FISH : Items.FISH;
            int n = 0;
            for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(2))) {
                ItemStack s = e.getItem();
                if (s.getItem() == want && s.getMetadata() == meta) {
                    n += s.getCount();
                }
                e.setDead();
            }
            c.eq(1, n, cls.getSimpleName() + (fire ? " on fire" : "") + " drops one " + want.getRegistryName() + "@" + meta);
        }
    }

}
