package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.mobs.EntityAISquidFlee;
import com.exiledradio.updateaquaticbackport.mobs.MobsContent;
import com.exiledradio.updateaquaticbackport.mobs.ParrotImitation;
import com.exiledradio.updateaquaticbackport.mobs.PolarBearSpawning;
import com.exiledradio.updateaquaticbackport.mobs.SkeletonHorses;
import com.exiledradio.updateaquaticbackport.mobs.SquidInk;
import com.exiledradio.updateaquaticbackport.mobs.UndeadInWater;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityStray;
import net.minecraft.entity.monster.EntityWitherSkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityParrot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityZombieHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.terraingen.PopulateChunkEvent;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** 1.13's mob changes: undead in water, baby zombies, squid, polar bears, parrots and skeleton horses. */
public final class MobsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mobs";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(MobsContent.enabled(), "mobs stream on");
        sounds(c);
        sinking(c);
        breathing(c);
        babyZombies(c);
        squid(c);
        polarBears(c);
        parrots(c);
        skeletonHorses(c);
    }

    // ---------------------------------------------------------------------------------------------- helpers

    /** A glass-walled pool in a fresh area: water from the floor (y 100) up to {@code depth} blocks. */
    private static BlockPos pool(Check c, int depth) {
        BlockPos corner = c.area();
        c.fill(corner.add(0, 0, 0), corner.add(10, depth, 10), Blocks.GLASS.getDefaultState());
        c.fill(corner.add(1, 0, 1), corner.add(9, depth - 1, 9), Blocks.WATER.getDefaultState());
        return corner.add(5, 0, 5);
    }

    private static <T extends Entity> T spawn(World world, T entity, double x, double y, double z) {
        entity.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        world.spawnEntity(entity);
        return entity;
    }

    private static int swimGoals(EntityLiving mob) {
        int n = 0;
        for (EntityAITasks.EntityAITaskEntry entry : mob.tasks.taskEntries) {
            if (entry.action instanceof EntityAISwimming) {
                n++;
            }
        }
        return n;
    }

    private static boolean canSwim(EntityLiving mob) {
        return mob.getNavigator() instanceof PathNavigateGround && ((PathNavigateGround) mob.getNavigator()).getCanSwim();
    }

    /** Saves a mob and loads it into the world again, as a chunk reload does. */
    private static Entity reload(World world, Entity entity) {
        NBTTagCompound tag = new NBTTagCompound();
        entity.writeToNBTOptional(tag);
        // Gone at once, so the loaded copy may take its id.
        world.removeEntityDangerously(entity);
        Entity loaded = EntityList.createEntityFromNBT(tag, world);
        world.spawnEntity(loaded);
        return loaded;
    }

    private static void ticks(Entity entity, int n) {
        for (int i = 0; i < n && !entity.isDead; i++) {
            entity.onUpdate();
        }
    }

    // ----------------------------------------------------------------------------------------------- sounds

    private static void sounds(Check c) {
        for (String name : new String[]{"entity.squid.squirt", "entity.parrot.imitate.phantom", "entity.parrot.imitate.drowned",
                "entity.skeleton_horse.ambient_water", "entity.skeleton_horse.gallop_water", "entity.skeleton_horse.step_water",
                "entity.skeleton_horse.swim"}) {
            SoundEvent sound = ForgeRegistries.SOUND_EVENTS.getValue(Content.id(name));
            c.ok(sound != null && Content.id(name).equals(sound.getRegistryName()), "sound event registered: " + name);
        }
    }

    // --------------------------------------------------------------------------------------- undead sinking

    private static void sinking(Check c) {
        WorldServer world = c.world;
        c.ok(ModConfig.mobs.undeadSink, "undead sinking on");
        BlockPos at = pool(c, 10);
        double top = at.getY() + 8.5;
        EntityLiving[] sinkers = {
                new EntityZombie(world), new EntityHusk(world), new EntityZombieVillager(world), new EntityPigZombie(world),
                new EntitySkeleton(world), new EntityStray(world), new EntityWitherSkeleton(world),
                new EntitySkeletonHorse(world), new EntityZombieHorse(world)};
        for (EntityLiving mob : sinkers) {
            String what = EntityList.getKey(mob) + "";
            c.ok(swimGoals(mob) > 0, what + " has 1.12.2's swim goal before joining a world");
            spawn(world, mob, at.getX() + 0.5, top, at.getZ() + 0.5);
            c.eq(0, swimGoals(mob), what + " swim goals after joining");
            c.ok(!canSwim(mob), what + " navigator no longer swims");
        }
        EntityPig pig = spawn(world, new EntityPig(world), at.getX() + 0.5, top, at.getZ() + 0.5);
        c.ok(swimGoals(pig) > 0 && canSwim(pig), "a pig keeps its swim goal");

        for (int i = 0; i < 200; i++) {
            for (EntityLiving mob : sinkers) {
                mob.onUpdate();
            }
            pig.onUpdate();
        }
        for (EntityLiving mob : sinkers) {
            String what = EntityList.getKey(mob) + "";
            UpdateAquaticBackport.LOGGER.info("SELFTEST mobs: {} after 200 ticks in 9 deep water at y {}", what, mob.posY - at.getY());
            c.ok(mob.posY < at.getY() + 0.6, what + " sank to the bottom (y " + mob.posY + ")");
            c.ok(mob.isEntityAlive() && mob.getHealth() == mob.getMaxHealth(), what + " did not drown");
        }
        c.ok(pig.posY > at.getY() + 7.0, "the pig swam at the surface (y " + pig.posY + ")");

        // Saving and loading builds the mob again; it joins again and loses its new swim goal again.
        Entity zombie = reload(world, sinkers[0]);
        c.ok(zombie instanceof EntityZombie && swimGoals((EntityLiving) zombie) == 0, "a reloaded zombie has no swim goal");
        Entity horse = reload(world, sinkers[7]);
        c.ok(horse instanceof EntitySkeletonHorse && swimGoals((EntityLiving) horse) == 0, "a reloaded skeleton horse has no swim goal");

        // Switched off, a new mob keeps its swim goal.
        ModConfig.mobs.undeadSink = false;
        EntityZombie kept = spawn(world, new EntityZombie(world), at.getX() + 0.5, top, at.getZ() + 0.5);
        ModConfig.mobs.undeadSink = true;
        c.ok(swimGoals(kept) > 0, "with undeadSink off a zombie keeps its swim goal");

        for (EntityLiving mob : sinkers) {
            mob.setDead();
        }
        zombie.setDead();
        horse.setDead();
        pig.setDead();
        kept.setDead();
    }

    private static void breathing(Check c) {
        WorldServer world = c.world;
        BlockPos at = pool(c, 6);
        EntityZombie zombie = spawn(world, new EntityZombie(world), at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        EntitySkeleton skeleton = spawn(world, new EntitySkeleton(world), at.getX() + 1.5, at.getY(), at.getZ() + 0.5);
        EntityPig pig = spawn(world, new EntityPig(world), at.getX() - 1.5, at.getY(), at.getZ() + 0.5);
        pig.setNoAI(true);
        ticks(zombie, 3);
        c.ok(zombie.isInsideOfMaterial(Material.WATER), "zombie's eyes under water");
        for (int i = 0; i < 400; i++) {
            zombie.onUpdate();
            skeleton.onUpdate();
            pig.onUpdate();
        }
        c.ok(zombie.getAir() >= 299, "a zombie under water keeps its air (" + zombie.getAir() + ")");
        c.ok(skeleton.getAir() >= 299, "a skeleton under water keeps its air (" + skeleton.getAir() + ")");
        c.ok(zombie.getHealth() == zombie.getMaxHealth() && skeleton.getHealth() == skeleton.getMaxHealth(),
                "undead under water for 20 seconds take no damage");
        c.ok(pig.getHealth() < pig.getMaxHealth(), "a pig held under water drowns (control)");
        float before = zombie.getHealth();
        zombie.hurtResistantTime = 0;
        c.ok(!zombie.attackEntityFrom(DamageSource.DROWN, 2.0F) && zombie.getHealth() == before, "drowning damage never hurts undead");
        zombie.setDead();
        skeleton.setDead();
        pig.setDead();
    }

    // ----------------------------------------------------------------------------------------- baby zombies

    private static void babyZombies(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(ModConfig.mobs.babyZombiesBurn, "baby zombie burning on");
        long time = world.getWorldTime();
        float rain = world.getRainStrength(1.0F);
        float thunder = world.getThunderStrength(1.0F);
        ReflectionHelper.setPrivateValue(World.class, world, 0.0F, "rainingStrength", "field_73004_o");
        ReflectionHelper.setPrivateValue(World.class, world, 0.0F, "thunderingStrength", "field_73017_q");
        world.setWorldTime(6000L);
        world.calculateInitialSkylight();
        try {
            BlockPos at = c.area();
            c.ok(world.isDaytime() && world.canSeeSky(at.up()), "noon under open sky");

            EntityZombie baby = spawn(world, new EntityZombie(world), at.getX() + 4.5, at.getY(), at.getZ() + 4.5);
            baby.setChild(true);
            c.ok(baby.getBrightness() > 0.5F, "the test spot is bright (" + baby.getBrightness() + ")");
            c.ok(burnsWithin(baby, 2000), "a baby zombie catches fire in daylight (through its update event)");

            EntityHusk husk = spawn(world, new EntityHusk(world), at.getX() + 8.5, at.getY(), at.getZ() + 4.5);
            husk.setChild(true);
            c.ok(!burnsWithin(husk, 2000), "a baby husk never burns");

            EntityZombie helmeted = spawn(world, new EntityZombie(world), at.getX() + 12.5, at.getY(), at.getZ() + 4.5);
            helmeted.setChild(true);
            helmeted.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            boolean burned = burnsWithin(helmeted, 400);
            ItemStack helmet = helmeted.getItemStackFromSlot(EntityEquipmentSlot.HEAD);
            c.ok(!burned && !helmet.isEmpty() && helmet.getItemDamage() > 0,
                    "a baby zombie's helmet wears down instead (" + helmet.getItemDamage() + ")");

            EntityZombie adult = spawn(world, new EntityZombie(world), at.getX() + 16.5, at.getY(), at.getZ() + 4.5);
            c.ok(!burnsWithin(adult, 2000), "an adult zombie is left to vanilla (the event does not burn it)");

            ModConfig.mobs.babyZombiesBurn = false;
            EntityZombie off = spawn(world, new EntityZombie(world), at.getX() + 20.5, at.getY(), at.getZ() + 4.5);
            off.setChild(true);
            c.ok(!burnsWithin(off, 2000), "with babyZombiesBurn off a baby does not burn");
            ModConfig.mobs.babyZombiesBurn = true;

            // At night nothing burns.
            world.setWorldTime(18000L);
            world.calculateInitialSkylight();
            EntityZombie night = spawn(world, new EntityZombie(world), at.getX() + 24.5, at.getY(), at.getZ() + 4.5);
            night.setChild(true);
            c.ok(!burnsWithin(night, 2000), "a baby zombie does not burn at night");

            for (Entity e : new Entity[]{baby, husk, helmeted, adult, off, night}) {
                e.setDead();
            }
        } finally {
            ModConfig.mobs.babyZombiesBurn = true;
            world.setWorldTime(time);
            ReflectionHelper.setPrivateValue(World.class, world, rain, "rainingStrength", "field_73004_o");
            ReflectionHelper.setPrivateValue(World.class, world, thunder, "thunderingStrength", "field_73017_q");
            world.calculateInitialSkylight();
        }
    }

    /** Posts the mob's update event (the stream's handler and everyone else's) until it is on fire. */
    private static boolean burnsWithin(EntityZombie zombie, int tries) {
        for (int i = 0; i < tries; i++) {
            MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(zombie));
            if (zombie.isBurning()) {
                zombie.extinguish();
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------------ squid

    /** Collects the sounds played while it is registered. */
    public static final class Sounds {
        final List<SoundEvent> played = new ArrayList<SoundEvent>();

        @SubscribeEvent
        public void onSound(PlaySoundAtEntityEvent event) {
            this.played.add(event.getSound());
        }

        int count(SoundEvent sound) {
            int n = 0;
            for (SoundEvent s : this.played) {
                n += s == sound ? 1 : 0;
            }
            return n;
        }
    }

    private static float motionVec(EntitySquid squid, String axis, String srg) {
        return ReflectionHelper.<Float, EntitySquid>getPrivateValue(EntitySquid.class, squid, "randomMotionVec" + axis, srg);
    }

    private static void squid(Check c) {
        WorldServer world = c.world;
        c.ok(ModConfig.mobs.squidInk, "squid ink on");
        BlockPos at = pool(c, 8);
        EntitySquid squid = spawn(world, new EntitySquid(world), at.getX() + 0.5, at.getY() + 3.0, at.getZ() + 0.5);
        c.ok(SquidInk.flees(squid), "a squid that joins a world has the flee goal");
        int flee = 0;
        for (EntityAITasks.EntityAITaskEntry entry : squid.tasks.taskEntries) {
            if (entry.action instanceof EntityAISquidFlee) {
                flee++;
                c.eq(1, entry.priority, "flee goal priority");
                c.eq(0, entry.action.getMutexBits(), "flee goal mutex");
            }
        }
        c.eq(1, flee, "exactly one flee goal");
        ticks(squid, 3);
        c.ok(squid.isInWater(), "the squid is in water");

        Sounds sounds = new Sounds();
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            // Hurt by nothing it can blame: no ink.
            squid.attackEntityFrom(DamageSource.GENERIC, 1.0F);
            c.ok(SquidInk.pending(squid), "a hurt squid is marked");
            squid.onUpdate();
            c.ok(!SquidInk.pending(squid), "the mark is used on its next tick");
            c.eq(0, sounds.count(MobsContent.SQUID_SQUIRT), "no ink without an attacker");

            // Hurt by a player: ink, once.
            FakePlayer player = c.player("squid", at.west(2).up(3));
            squid.hurtResistantTime = 0;
            c.ok(squid.attackEntityFrom(DamageSource.causePlayerDamage(player), 1.0F), "the player hurts the squid");
            c.ok(squid.getRevengeTarget() == player, "the squid blames the player");
            squid.onUpdate();
            c.eq(1, sounds.count(MobsContent.SQUID_SQUIRT), "a squid hurt by a player squirts ink once");
            squid.onUpdate();
            c.eq(1, sounds.count(MobsContent.SQUID_SQUIRT), "and not again on the next tick");

            // A hit inside the invulnerability time that does nothing does not squirt.
            squid.attackEntityFrom(DamageSource.causePlayerDamage(player), 0.5F);
            squid.onUpdate();
            c.eq(1, sounds.count(MobsContent.SQUID_SQUIRT), "a hit blocked by invulnerability makes no ink");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
        }

        // Fleeing: the goal steers straight away, at 1.14.4's speed.
        squid.setLocationAndAngles(at.getX() + 0.5, at.getY() + 3.0, at.getZ() + 0.5, 0.0F, 0.0F);
        FakePlayer attacker = c.player("squid2", at.add(-2, 3, 0));
        attacker.setPosition(squid.posX - 2.0, squid.posY, squid.posZ);
        squid.setRevengeTarget(attacker);
        EntityAISquidFlee goal = new EntityAISquidFlee(squid);
        c.ok(goal.shouldExecute(), "the flee goal starts with the attacker 2 blocks away");
        goal.startExecuting();
        goal.updateTask();
        c.ok(Math.abs(motionVec(squid, "X", "field_70872_bC") - 0.3F) < 1e-4F
                        && motionVec(squid, "Y", "field_70869_bD") == 0.0F && motionVec(squid, "Z", "field_70870_bE") == 0.0F,
                "the squid swims straight away at 3 x 2 / 20 = 0.3 (" + motionVec(squid, "X", "field_70872_bC") + ")");
        attacker.setPosition(squid.posX - 12.0, squid.posY, squid.posZ);
        c.ok(!goal.shouldExecute(), "no fleeing from an attacker 12 blocks away");
        attacker.setPosition(squid.posX - 2.0, squid.posY, squid.posZ);
        // Ticked for real: the squid ends up further from its attacker.
        double start = squid.getDistance(attacker);
        for (int i = 0; i < 40; i++) {
            squid.onUpdate();
            attacker.setPosition(at.getX() - 1.5, squid.posY, squid.posZ);
        }
        c.ok(squid.posX > at.getX() + 1.5, "a ticked squid swims away from its attacker (x " + squid.posX + ", started " + start + ")");

        // Saving and loading: one flee goal again.
        Entity loaded = reload(world, squid);
        c.ok(loaded instanceof EntitySquid && SquidInk.flees((EntitySquid) loaded), "a reloaded squid flees too");
        loaded.setDead();
    }

    // ------------------------------------------------------------------------------------------ polar bears

    private static void setBiome(World world, BlockPos inChunk, Biome biome) {
        Chunk chunk = world.getChunk(inChunk);
        byte[] biomes = chunk.getBiomeArray();
        for (int i = 0; i < biomes.length; i++) {
            biomes[i] = (byte) Biome.getIdForBiome(biome);
        }
    }

    private static void polarBears(Check c) {
        WorldServer world = c.world;
        c.ok(ModConfig.mobs.polarBearsOnIce, "polar bears on ice on");
        EntityLiving.SpawnPlacementType placement = EntitySpawnPlacementRegistry.getPlacementForEntity(EntityPolarBear.class);
        c.ok(placement != EntityLiving.SpawnPlacementType.ON_GROUND && placement.name().contains("ICE"),
                "the polar bear has its own placement (" + placement + ")");
        List<Biome.SpawnListEntry> entries = PolarBearSpawning.entries(Biomes.FROZEN_OCEAN);
        c.eq(1, entries.size(), "the frozen ocean lists polar bears once");
        if (!entries.isEmpty()) {
            c.ok(entries.get(0).itemWeight == 1 && entries.get(0).minGroupCount == 1 && entries.get(0).maxGroupCount == 2,
                    "with weight 1 in groups of 1-2");
        }
        c.eq(1, PolarBearSpawning.entries(Biomes.ICE_PLAINS).size(), "the ice plains still list them once (vanilla)");
        c.eq(0, PolarBearSpawning.entries(Biomes.OCEAN).size(), "a plain ocean does not");

        // A fresh area is two chunks by two, chunk-aligned. Chunk generation for its first chunk spawns in
        // the 16x16 half a chunk further in (vanilla's populated area) and reads the biome of the last chunk.
        BlockPos corner = c.area();
        c.ok((corner.getX() & 15) == 0 && (corner.getZ() & 15) == 0, "the test area is chunk-aligned");
        int cx = corner.getX() >> 4;
        int cz = corner.getZ() >> 4;
        BlockPos base = corner.add(8, -1, 8);
        c.fill(corner.down(), corner.add(31, -1, 31), Blocks.ICE.getDefaultState());
        c.fill(base.add(0, 0, 12), base.add(15, 0, 15), Blocks.GRASS.getDefaultState());
        for (int dx = 0; dx < 2; dx++) {
            for (int dz = 0; dz < 2; dz++) {
                setBiome(world, corner.add(16 * dx, 0, 16 * dz), Biomes.FROZEN_OCEAN);
            }
        }
        c.ok(world.getBiome(base) == Biomes.FROZEN_OCEAN, "the test chunks are frozen ocean");
        BlockPos onIce = base.add(4, 1, 4);
        BlockPos onGrass = base.add(4, 1, 14);
        c.ok(world.getLight(onIce) > 8, "light above the ice (" + world.getLight(onIce) + ")");

        c.ok(WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placement, world, onIce), "a polar bear can be placed on ice");
        c.ok(!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.ON_GROUND, world, onIce),
                "other animals still cannot (control)");
        c.ok(WorldEntitySpawner.canCreatureTypeSpawnAtLocation(placement, world, onGrass), "a polar bear can still be placed on grass");

        EntityPolarBear bear = new EntityPolarBear(world);
        bear.setLocationAndAngles(onIce.getX() + 0.5, onIce.getY(), onIce.getZ() + 0.5, 0.0F, 0.0F);
        c.eq(Event.Result.ALLOW, ForgeEventFactory.canEntitySpawn(bear, world, onIce.getX() + 0.5F, onIce.getY(), onIce.getZ() + 0.5F, false),
                "frozen ocean, on ice: allowed");
        c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(bear, world, onIce.getX() + 0.5F, onIce.getY(), onIce.getZ() + 0.5F, true),
                "from a spawner: left to vanilla");
        bear.setLocationAndAngles(onGrass.getX() + 0.5, onGrass.getY(), onGrass.getZ() + 0.5, 0.0F, 0.0F);
        c.eq(Event.Result.DENY, ForgeEventFactory.canEntitySpawn(bear, world, onGrass.getX() + 0.5F, onGrass.getY(), onGrass.getZ() + 0.5F, false),
                "frozen ocean, on grass: denied");
        c.fill(base.add(8, 1, 8), base.add(8, 3, 8), Blocks.STONE.getDefaultState());
        bear.setLocationAndAngles(base.getX() + 8.5, base.getY() + 1, base.getZ() + 8.5, 0.0F, 0.0F);
        c.eq(Event.Result.DENY, ForgeEventFactory.canEntitySpawn(bear, world, base.getX() + 8.5F, base.getY() + 1, base.getZ() + 8.5F, false),
                "on ice but inside a block: denied");
        c.fill(base.add(8, 1, 8), base.add(8, 3, 8), Blocks.AIR.getDefaultState());

        // Outside a frozen ocean nothing changes.
        BlockPos plain = c.area();
        world.setBlockState(plain.add(4, -1, 4), Blocks.GRASS.getDefaultState());
        setBiome(world, plain.add(4, 0, 4), Biomes.PLAINS);
        bear.setLocationAndAngles(plain.getX() + 4.5, plain.getY(), plain.getZ() + 4.5, 0.0F, 0.0F);
        c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(bear, world, plain.getX() + 4.5F, plain.getY(), plain.getZ() + 4.5F, false),
                "outside a frozen ocean: left to vanilla");

        // Chunk generation: the populated event spawns polar bears on the ice only.
        int events = 0;
        List<EntityPolarBear> bears = new ArrayList<EntityPolarBear>();
        AxisAlignedBB chunkBox = new AxisAlignedBB(corner.getX(), 95, corner.getZ(), corner.getX() + 32, 110, corner.getZ() + 32);
        Random random = new Random(42);
        while (events < 400 && bears.size() < 6) {
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(world.getChunkProvider().chunkGenerator, world, random, cx, cz, false));
            events++;
            bears = world.getEntitiesWithinAABB(EntityPolarBear.class, chunkBox);
        }
        c.ok(!bears.isEmpty(), "chunk generation spawned polar bears in a frozen ocean (" + bears.size() + " in " + events + " chunks)");
        UpdateAquaticBackport.LOGGER.info("SELFTEST mobs: {} polar bears from {} populated frozen-ocean chunks", bears.size(), events);
        boolean allOnIce = true;
        boolean inArea = true;
        for (EntityPolarBear b : bears) {
            allOnIce &= world.getBlockState(new BlockPos(b).down()).getBlock() == Blocks.ICE;
            inArea &= b.posX >= base.getX() && b.posX < base.getX() + 16 && b.posZ >= base.getZ() && b.posZ < base.getZ() + 16;
            b.setDead();
        }
        c.ok(allOnIce, "every one stands on ice, none on the grass");
        c.ok(inArea, "every one inside the populated area");

        // A frozen ocean of water, no ice: none.
        c.fill(corner.down(), corner.add(31, -1, 31), Blocks.WATER.getDefaultState());
        for (int i = 0; i < 100; i++) {
            MinecraftForge.EVENT_BUS.post(new PopulateChunkEvent.Post(world.getChunkProvider().chunkGenerator, world, random, cx, cz, false));
        }
        int afloat = 0;
        for (EntityPolarBear b : world.getEntitiesWithinAABB(EntityPolarBear.class, chunkBox)) {
            afloat += b.isEntityAlive() ? 1 : 0;
        }
        c.eq(0, afloat, "no polar bears on open water");
    }

    // ---------------------------------------------------------------------------------------------- parrots

    private static final String BUZZY_BEES = "buzzybeesbackport";

    /** 1.12.2's {@code EntityParrot} static block. */
    private static final Map<Class<? extends Entity>, SoundEvent> VANILLA_IMITATIONS = new java.util.LinkedHashMap<Class<? extends Entity>, SoundEvent>();
    private static final java.util.Set<Class<? extends Entity>> DROPPED_IN_1_15 = new java.util.HashSet<Class<? extends Entity>>();

    static {
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityBlaze.class, SoundEvents.E_PARROT_IM_BLAZE);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityCaveSpider.class, SoundEvents.E_PARROT_IM_SPIDER);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityCreeper.class, SoundEvents.E_PARROT_IM_CREEPER);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityElderGuardian.class, SoundEvents.E_PARROT_IM_ELDER_GUARDIAN);
        VANILLA_IMITATIONS.put(net.minecraft.entity.boss.EntityDragon.class, SoundEvents.E_PARROT_IM_ENDERDRAGON);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityEnderman.class, SoundEvents.E_PARROT_IM_ENDERMAN);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityEndermite.class, SoundEvents.E_PARROT_IM_ENDERMITE);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityEvoker.class, SoundEvents.E_PARROT_IM_EVOCATION_ILLAGER);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityGhast.class, SoundEvents.E_PARROT_IM_GHAST);
        VANILLA_IMITATIONS.put(EntityHusk.class, SoundEvents.E_PARROT_IM_HUSK);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityIllusionIllager.class, SoundEvents.E_PARROT_IM_ILLUSION_ILLAGER);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityMagmaCube.class, SoundEvents.E_PARROT_IM_MAGMACUBE);
        VANILLA_IMITATIONS.put(EntityPigZombie.class, SoundEvents.E_PARROT_IM_ZOMBIE_PIGMAN);
        VANILLA_IMITATIONS.put(EntityPolarBear.class, SoundEvents.E_PARROT_IM_POLAR_BEAR);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityShulker.class, SoundEvents.E_PARROT_IM_SHULKER);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntitySilverfish.class, SoundEvents.E_PARROT_IM_SILVERFISH);
        VANILLA_IMITATIONS.put(EntitySkeleton.class, SoundEvents.E_PARROT_IM_SKELETON);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntitySlime.class, SoundEvents.E_PARROT_IM_SLIME);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntitySpider.class, SoundEvents.E_PARROT_IM_SPIDER);
        VANILLA_IMITATIONS.put(EntityStray.class, SoundEvents.E_PARROT_IM_STRAY);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityVex.class, SoundEvents.E_PARROT_IM_VEX);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityVindicator.class, SoundEvents.E_PARROT_IM_VINDICATION_ILLAGER);
        VANILLA_IMITATIONS.put(net.minecraft.entity.monster.EntityWitch.class, SoundEvents.E_PARROT_IM_WITCH);
        VANILLA_IMITATIONS.put(net.minecraft.entity.boss.EntityWither.class, SoundEvents.E_PARROT_IM_WITHER);
        VANILLA_IMITATIONS.put(EntityWitherSkeleton.class, SoundEvents.E_PARROT_IM_WITHER_SKELETON);
        VANILLA_IMITATIONS.put(net.minecraft.entity.passive.EntityWolf.class, SoundEvents.E_PARROT_IM_WOLF);
        VANILLA_IMITATIONS.put(EntityZombie.class, SoundEvents.E_PARROT_IM_ZOMBIE);
        VANILLA_IMITATIONS.put(EntityZombieVillager.class, SoundEvents.E_PARROT_IM_ZOMBIE_VILLAGER);
        DROPPED_IN_1_15.add(net.minecraft.entity.monster.EntityEnderman.class);
        DROPPED_IN_1_15.add(EntityPolarBear.class);
        DROPPED_IN_1_15.add(net.minecraft.entity.passive.EntityWolf.class);
        DROPPED_IN_1_15.add(EntityPigZombie.class);
    }

    @SuppressWarnings("unchecked")
    private static void parrots(Check c) {
        c.ok(ModConfig.mobs.parrotsImitate, "parrot imitation on");
        Map<Class<? extends Entity>, SoundEvent> mimic = (Map<Class<? extends Entity>, SoundEvent>)
                ReflectionHelper.getPrivateValue(EntityParrot.class, null, "MIMIC_SOUNDS");
        String[] names = {ParrotImitation.PHANTOM, ParrotImitation.DROWNED};
        SoundEvent[] sounds = {MobsContent.PARROT_IMITATE_PHANTOM, MobsContent.PARROT_IMITATE_DROWNED};
        for (int i = 0; i < names.length; i++) {
            Class<? extends Entity> mob = ParrotImitation.ourMob(names[i]);
            if (mob == null) {
                c.ok(!mimic.containsValue(sounds[i]), names[i] + " is not in this build, and nothing imitates it");
            } else {
                c.ok(mimic.get(mob) == sounds[i], "parrots imitate the " + names[i] + " with " + sounds[i].getRegistryName());
            }
        }
        // Vanilla 1.12.2's 28, each with its own sound. Buzzy Bees Backport takes out the four neutral mobs as 1.15
        // did (endermen, polar bears, wolves, zombie pigmen); every other entry stays whatever else is installed.
        int vanilla = 0;
        for (Map.Entry<Class<? extends Entity>, SoundEvent> e : VANILLA_IMITATIONS.entrySet()) {
            SoundEvent now = mimic.get(e.getKey());
            if (now == null && DROPPED_IN_1_15.contains(e.getKey()) && Loader.isModLoaded(BUZZY_BEES)) {
                continue;
            }
            vanilla++;
            c.ok(now == e.getValue(), "parrots still imitate the " + e.getKey().getSimpleName() + " with vanilla's sound");
        }
        c.ok(vanilla >= 24, "vanilla's imitations are untouched (" + vanilla + " of 28 here, " + mimic.size() + " in all)");
        c.ok(ParrotImitation.ourMob("no_such_mob") == null, "an unknown name finds nothing");
        ResourceLocation pig = new ResourceLocation("minecraft", "pig");
        c.ok(ForgeRegistries.ENTITIES.getValue(pig) != null && ParrotImitation.ourMob("pig") == null,
                "a vanilla mob's name is not taken for ours");
    }

    // -------------------------------------------------------------------------------------- skeleton horses

    private static void skeletonHorses(Check c) {
        WorldServer world = c.world;
        c.ok(ModConfig.mobs.skeletonHorsesUnderwater, "skeleton horses underwater on");
        BlockPos at = pool(c, 8);

        // Riders stay on a skeleton horse with their eyes under water; a normal horse throws them.
        EntitySkeletonHorse horse = spawn(world, new EntitySkeletonHorse(world), at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        horse.setNoAI(true);
        EntitySkeleton rider = spawn(world, new EntitySkeleton(world), at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5);
        c.ok(rider.startRiding(horse, true), "a skeleton mounts the skeleton horse");
        EntityHorse plain = spawn(world, new EntityHorse(world), at.getX() - 2.5, at.getY(), at.getZ() + 0.5);
        plain.setNoAI(true);
        EntitySkeleton thrown = spawn(world, new EntitySkeleton(world), at.getX() - 2.5, at.getY() + 1, at.getZ() + 0.5);
        c.ok(thrown.startRiding(plain, true), "a skeleton mounts a normal horse");
        for (int i = 0; i < 40; i++) {
            horse.onUpdate();
            if (rider.getRidingEntity() == horse) {
                rider.updateRidden();
            }
            plain.onUpdate();
            if (thrown.getRidingEntity() == plain) {
                thrown.updateRidden();
            }
        }
        c.ok(rider.isInsideOfMaterial(Material.WATER), "the rider's eyes are under water");
        c.ok(rider.getRidingEntity() == horse, "the rider is still on the skeleton horse after 40 ticks under water");
        c.ok(thrown.getRidingEntity() == null, "a normal horse's rider is thrown off under water (control)");
        rider.dismountRidingEntity();
        c.ok(rider.getRidingEntity() == null, "the rider can still get off on purpose under water");

        // A real player: the same under-water check, through the player's own tick; sneaking still gets off.
        EntityPlayerMP player = Players.real(world, "horserider");
        player.setPosition(horse.posX, horse.posY + 1, horse.posZ);
        world.spawnEntity(player);
        c.ok(player.startRiding(horse, true), "a player mounts the skeleton horse");
        for (int i = 0; i < 40; i++) {
            horse.onUpdate();
            if (player.getRidingEntity() == horse) {
                // The part of the player's tick that has the under-water check (the rest of it needs a
                // real connection).
                player.onEntityUpdate();
                horse.updatePassenger(player);
            }
        }
        c.ok(player.isInsideOfMaterial(Material.WATER), "the player's eyes are under water");
        c.ok(player.getRidingEntity() == horse, "the player is still riding after 40 ticks under water");
        player.setSneaking(true);
        player.updateRidden();
        c.ok(player.getRidingEntity() == null, "sneaking gets the player off under water");
        player.setSneaking(false);
        c.ok(player.startRiding(plain, true), "the player mounts the normal horse");
        horse.updatePassenger(player);
        plain.updatePassenger(player);
        player.onEntityUpdate();
        c.ok(player.getRidingEntity() == null, "a normal horse throws the player off under water (control)");
        world.removeEntityDangerously(player);

        // Speed: 0.96 kept per tick in water instead of 0.8.
        EntitySkeletonHorse swimmer = spawn(world, new EntitySkeletonHorse(world), at.getX() + 2.5, at.getY() + 3, at.getZ() + 2.5);
        EntityZombieHorse zombieHorse = spawn(world, new EntityZombieHorse(world), at.getX() - 2.5, at.getY() + 3, at.getZ() + 2.5);
        ticks(swimmer, 2);
        ticks(zombieHorse, 2);
        c.ok(swimmer.isInWater() && zombieHorse.isInWater(), "both horses in water");
        swimmer.motionX = 0.1;
        swimmer.motionZ = -0.05;
        zombieHorse.motionX = 0.1;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(swimmer));
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(zombieHorse));
        c.ok(Math.abs(swimmer.motionX - 0.12) < 1e-6 && Math.abs(swimmer.motionZ + 0.06) < 1e-6,
                "a skeleton horse keeps 96% of its speed in water (" + swimmer.motionX + ")");
        c.ok(Math.abs(zombieHorse.motionX - 0.1) < 1e-9, "a zombie horse keeps vanilla's 80% (control)");
        c.ok(Math.abs(SkeletonHorses.WATER_SLOW_DOWN / SkeletonHorses.VANILLA_WATER_SLOW_DOWN - 1.2F) < 1e-6F, "scale 1.2");
        swimmer.setPosition(swimmer.posX, swimmer.posY + 50.0, swimmer.posZ);
        swimmer.onUpdate();
        swimmer.motionX = 0.1;
        MinecraftForge.EVENT_BUS.post(new LivingEvent.LivingUpdateEvent(swimmer));
        c.ok(!swimmer.isInWater() && Math.abs(swimmer.motionX - 0.1) < 1e-9, "out of water nothing changes");

        // And it sinks while it swims: a ticked skeleton horse ends at the bottom.
        swimmer.setLocationAndAngles(at.getX() + 2.5, at.getY() + 6, at.getZ() + 2.5, 0.0F, 0.0F);
        ticks(swimmer, 200);
        c.ok(swimmer.posY < at.getY() + 0.6, "an unridden skeleton horse sinks (y " + swimmer.posY + ")");

        for (Entity e : new Entity[]{horse, rider, plain, thrown, swimmer, zombieHorse}) {
            e.setDead();
        }
    }
}
