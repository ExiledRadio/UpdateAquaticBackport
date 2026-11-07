package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.dolphin.DolphinContent;
import com.exiledradio.updateaquaticbackport.dolphin.DolphinsGrace;
import com.exiledradio.updateaquaticbackport.dolphin.EntityDolphin;
import com.exiledradio.backportcore.structure.Structures;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Random;

/** Dolphins: registration, breathing, drying out, AI, feeding and the structure locator, Dolphin's Grace, drops, saving. */
public final class DolphinSelfTest implements SelfTest {

    @Override
    public String name() {
        return "dolphin";
    }

    private Check c;
    private WorldServer world;

    @Override
    public void run(Check check) throws Exception {
        this.c = check;
        this.world = check.world;
        c.ok(DolphinContent.enabled(), "dolphin stream on");
        registration();
        spawning();
        realOcean();
        breathing();
        dryingOut();
        saving();
        feeding();
        treasure();
        grace();
        swimWithPlayer();
        playWithItems();
        boats();
        jumping();
        defense();
        drops();
    }

    // ---------------------------------------------------------------------------------------------

    private void registration() {
        ResourceLocation id = Content.id("dolphin");
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(id);
        c.ok(entry != null && entry.getEntityClass() == EntityDolphin.class, "entity registered as " + id);
        c.eq(UpdateAquaticBackport.MOD_ID + ".dolphin", entry == null ? null : entry.getName(), "entity name");
        EntityRegistry.EntityRegistration reg = EntityRegistry.instance().lookupModSpawn(EntityDolphin.class, false);
        c.eq(DolphinContent.DOLPHIN_ID, reg == null ? null : reg.getModEntityId(), "mod entity id 15");
        c.ok(EntityList.ENTITY_EGGS.containsKey(id), "spawn egg");
        if (EntityList.ENTITY_EGGS.containsKey(id)) {
            c.eq(DolphinContent.EGG_PRIMARY, EntityList.ENTITY_EGGS.get(id).primaryColor, "egg primary color");
            c.eq(DolphinContent.EGG_SECONDARY, EntityList.ENTITY_EGGS.get(id).secondaryColor, "egg secondary color");
        }
        Entity made = EntityList.createEntityByIDFromName(id, world);
        c.ok(made instanceof EntityDolphin, "EntityList makes a dolphin from its id");

        Potion grace = ForgeRegistries.POTIONS.getValue(Content.id("dolphins_grace"));
        c.ok(grace != null && grace == DolphinContent.dolphinsGrace(), "Dolphin's Grace registered");
        if (grace != null) {
            c.ok(!grace.isBadEffect(), "Dolphin's Grace is beneficial");
            c.eq(8954814, grace.getLiquidColor(), "Dolphin's Grace color");
            c.eq("effect." + UpdateAquaticBackport.MOD_ID + ".dolphins_grace", grace.getName(), "Dolphin's Grace name key");
        }
        int sounds = 0;
        for (String s : new String[] {"ambient", "ambient_water", "attack", "death", "eat", "hurt", "jump", "play", "splash", "swim"}) {
            SoundEvent e = ForgeRegistries.SOUND_EVENTS.getValue(Content.id("entity.dolphin." + s));
            if (e != null) {
                sounds++;
            }
        }
        c.eq(10, sounds, "10 dolphin sound events");

        EntityDolphin d = new EntityDolphin(world);
        c.eq(10.0D, d.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "health 10");
        c.eq((double) 1.2F, d.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue(), "speed 1.2");
        c.eq(3.0D, d.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "attack 3");
        c.eq(0.9F, d.width, "width 0.9");
        c.eq(0.6F, d.height, "height 0.6");
        c.eq(0.3F, d.getEyeHeight(), "eye height 0.3");
        c.eq(EntityDolphin.MAX_AIR, d.getAir(), "starts with 4800 air");
        c.eq(EntityDolphin.MAX_MOISTNESS, d.getMoistness(), "starts with 2400 moistness");
        c.ok(!d.canBreatheUnderwater(), "cannot breathe under water");
        c.ok(!d.isPushedByWater(), "not pushed by water");
        c.eq(120, d.getTalkInterval(), "talk interval 120");
        boolean xp = true;
        for (int i = 0; i < 100; i++) {
            int x = d.getExperiencePoints(null);
            xp &= x >= 1 && x <= 3;
        }
        c.ok(xp, "experience 1-3");
        c.ok(hasTask(d.tasks, EntityAIAvoidEntity.class), "avoids guardians");
        for (String task : new String[] {"AIBreatheAir", "AIFindWater", "AISwimToTreasure", "AISwimWithPlayer", "AIRandomSwim",
                "AIDolphinJump", "AIPlayWithItems", "AIFollowBoat"}) {
            c.ok(task(d.tasks, task) != null, "has task " + task);
        }
        c.ok(task(d.targetTasks, "AIDefendPod") != null, "has target task AIDefendPod");

        TextComponentTranslation death = (TextComponentTranslation) EntityDolphin.DRYOUT.getDeathMessage(d);
        c.eq("death.attack.dryout", death.getKey(), "dry out death message key");
    }

    private void spawning() {
        c.eq(EntityLiving.SpawnPlacementType.IN_WATER, EntitySpawnPlacementRegistry.getPlacementForEntity(EntityDolphin.class), "spawns in water");
        c.ok(spawnsIn(Biomes.OCEAN), "spawn entry in ocean");
        c.ok(spawnsIn(Biomes.DEEP_OCEAN), "spawn entry in deep ocean");
        c.ok(!spawnsIn(Biomes.FROZEN_OCEAN), "no spawn entry in frozen ocean");
        c.ok(!spawnsIn(Biomes.RIVER), "no spawn entry in river");
        c.ok(!spawnsIn(Biomes.PLAINS), "no spawn entry in plains");
        for (Biome.SpawnListEntry e : Biomes.OCEAN.getSpawnableList(EnumCreatureType.WATER_CREATURE)) {
            if (e.entityClass == EntityDolphin.class) {
                c.eq(10, e.itemWeight, "ocean weight 10");
                c.eq(1, e.minGroupCount, "group min 1");
                c.eq(2, e.maxGroupCount, "group max 2");
            }
        }
        EntityDolphin d = new EntityDolphin(world);
        c.ok(d.isCreatureType(EnumCreatureType.WATER_CREATURE, false), "is a water creature");
        c.ok(!d.isCreatureType(EnumCreatureType.CREATURE, false), "is not a land creature");
        c.ok(!d.isCreatureType(EnumCreatureType.MONSTER, false), "is not a monster");
        d.enablePersistence();
        c.ok(!d.isCreatureType(EnumCreatureType.WATER_CREATURE, true), "a persistent dolphin is not counted for the cap");

        // The surface water rule, in a column of water built across sea level.
        BlockPos corner = c.area();
        int sea = world.getSeaLevel();
        BlockPos col = new BlockPos(corner.getX() + 5, sea - 13, corner.getZ() + 5);
        for (int y = sea - 16; y <= sea + 2; y++) {
            BlockPos p = new BlockPos(col.getX(), y, col.getZ());
            world.setBlockState(p, y >= sea - 15 && y <= sea ? Blocks.WATER.getDefaultState() : Blocks.STONE.getDefaultState(), 2 | 16);
        }
        Random r = new Random(1);
        c.ok(EntityDolphin.canSpawnAt(world, col, OceanKind.NORMAL, r), "spawns 13 below sea level in a normal ocean");
        c.ok(EntityDolphin.canSpawnAt(world, col.up(12), OceanKind.NORMAL, r), "spawns just under sea level");
        c.ok(!EntityDolphin.canSpawnAt(world, col.down(), OceanKind.NORMAL, r), "not 14 below sea level");
        c.ok(!EntityDolphin.canSpawnAt(world, col.up(13), OceanKind.NORMAL, r), "not at the surface block (no water above)");
        c.ok(!EntityDolphin.canSpawnAt(world, col.up(3), OceanKind.COLD, r), "not in a cold ocean");
        c.ok(!EntityDolphin.canSpawnAt(world, col.up(3), OceanKind.FROZEN, r), "not in a frozen ocean");
        c.ok(!EntityDolphin.canSpawnAt(world, col.up(3), OceanKind.NONE, r), "not outside oceans");
        int warm = 0;
        for (int i = 0; i < 1000; i++) {
            if (EntityDolphin.canSpawnAt(world, col.up(3), OceanKind.WARM, r)) {
                warm++;
            }
        }
        c.ok(warm > 140 && warm < 260, "warm oceans pass about a fifth of tries: " + warm + "/1000");
        world.setBlockState(col.up(3), Blocks.STONE.getDefaultState(), 2 | 16);
        c.ok(!EntityDolphin.canSpawnAt(world, col.up(4), OceanKind.NORMAL, r), "not with stone below");

        EntityDolphin leashed = spawn(corner.add(2, 1, 2));
        c.ok(leashed.canDespawn(), "an ordinary dolphin can despawn");
        leashed.setLeashHolder(Players.real(world, "dolphinleash"), false);
        c.ok(!leashed.canDespawn(), "a leashed dolphin does not despawn");
        leashed.setDead();
    }

    /** A generated ocean near spawn: the spawn checks a natural spawn makes, at 400 spots in its water. */
    private void realOcean() {
        BlockPos ocean = world.getBiomeProvider().findBiomePosition(0, 0, 1500,
                java.util.Arrays.asList(Biomes.OCEAN, Biomes.DEEP_OCEAN), new Random(2));
        if (ocean == null) {
            c.fail("no ocean within 1500 blocks of spawn to test spawning");
            return;
        }
        int cx = ocean.getX() >> 4;
        int cz = ocean.getZ() >> 4;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getChunk(cx + dx, cz + dz);
            }
        }
        Random r = new Random(3);
        int tried = 0;
        int passed = 0;
        int wrong = 0;
        int sea = world.getSeaLevel();
        for (int i = 0; i < 400; i++) {
            BlockPos p = new BlockPos((cx << 4) - 32 + r.nextInt(80), sea - 13 + r.nextInt(14), (cz << 4) - 32 + r.nextInt(80));
            if (!net.minecraft.world.WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.IN_WATER, world, p)) {
                continue;
            }
            OceanKind kind = OceanKind.at(world, p);
            EntityDolphin d = new EntityDolphin(world);
            d.setLocationAndAngles(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0.0F, 0.0F);
            boolean ok = d.getCanSpawnHere() && d.isNotColliding();
            tried++;
            if (ok) {
                passed++;
            }
            boolean waterAround = world.getBlockState(p.up()).getMaterial() == net.minecraft.block.material.Material.WATER;
            if ((kind == OceanKind.COLD || kind == OceanKind.FROZEN) && ok) {
                wrong++;
            }
            if (kind == OceanKind.NORMAL && waterAround && !ok) {
                wrong++;
            }
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST dolphin: ocean at {}: {} spots in water, {} pass the spawn check", ocean, tried, passed);
        c.ok(tried > 50, "a generated ocean has water at dolphin height: " + tried + " spots");
        c.eq(0, wrong, "spawn check follows the ocean temperature in a generated ocean");
        c.ok(passed > 0 || tried == 0, "dolphins could spawn there: " + passed + " of " + tried);
    }

    private void breathing() {
        BlockPos floor = Pools.pool(c, 6);
        EntityDolphin d = spawn(floor.add(5, 1, 5));
        tick(d, 1);
        c.ok(d.isInWater(), "in water");
        int air = d.getAir();
        tick(d, 10);
        c.eq(air - 10, d.getAir(), "air falls one a tick under water");

        // Low air: it swims up and gets its air back.
        d.setAir(150);
        boolean refilled = false;
        for (int i = 0; i < 400 && !refilled; i++) {
            tick(d, 1);
            refilled = d.getAir() == EntityDolphin.MAX_AIR;
        }
        c.ok(refilled, "low on air it reaches the surface and refills to 4800");

        // Drowning: at -20 it takes 2 damage, like any mob.
        EntityDolphin deep = spawn(floor.add(2, 1, 2));
        tick(deep, 1);
        deep.setAir(-19);
        float health = deep.getHealth();
        deep.hurtResistantTime = 0;
        tick(deep, 1);
        c.eq(health - 2.0F, deep.getHealth(), "drowns at -20 air for 2 damage");

        // Water Breathing stops the air falling.
        EntityDolphin breathing = spawn(floor.add(8, 1, 8));
        tick(breathing, 1);
        breathing.addPotionEffect(new PotionEffect(net.minecraft.init.MobEffects.WATER_BREATHING, 200));
        int before = breathing.getAir();
        tick(breathing, 10);
        c.eq(before, breathing.getAir(), "Water Breathing keeps its air");
        d.setDead();
        deep.setDead();
        breathing.setDead();
    }

    private void dryingOut() {
        BlockPos corner = c.area();
        EntityDolphin d = spawn(corner.add(10, 1, 10));
        d.setAir(100);
        boolean flopped = false;
        for (int i = 0; i < 20; i++) {
            tick(d, 1);
            flopped |= d.motionY > 0.3D;
        }
        c.eq(EntityDolphin.MAX_AIR, d.getAir(), "air refills at once out of water");
        c.ok(d.getMoistness() < EntityDolphin.MAX_MOISTNESS && d.getMoistness() >= EntityDolphin.MAX_MOISTNESS - 20,
                "moistness falls one a tick on land: " + d.getMoistness());
        c.ok(flopped, "flops about on the ground");
        float health = d.getHealth();
        d.setMoistness(1);
        d.hurtResistantTime = 0;
        tick(d, 1);
        c.eq(health - 1.0F, d.getHealth(), "dried out: 1 damage");
        c.ok(d.getLastDamageSource() == EntityDolphin.DRYOUT, "damage source is dryout");

        BlockPos floor = Pools.pool(c, 3);
        EntityDolphin wet = spawn(floor.add(4, 1, 4));
        wet.setMoistness(5);
        tick(wet, 2);
        c.eq(EntityDolphin.MAX_MOISTNESS, wet.getMoistness(), "water restores moistness");
        d.setDead();
        wet.setDead();
    }

    private void saving() {
        BlockPos floor = Pools.pool(c, 3);
        EntityDolphin d = spawn(floor.add(4, 1, 4));
        d.setTreasurePos(new BlockPos(123, 45, -678));
        d.setGotFish(true);
        d.setMoistness(1234);
        d.setAir(4000);
        d.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.STICK, 3));
        NBTTagCompound tag = d.writeToNBT(new NBTTagCompound());
        c.ok(tag.hasKey("TreasurePosX") && tag.hasKey("GotFish") && tag.hasKey("Moistness"), "1.13's NBT keys written");
        Entity loaded = EntityList.createEntityFromNBT(withId(tag, d), world);
        c.ok(loaded instanceof EntityDolphin, "loads back as a dolphin");
        if (loaded instanceof EntityDolphin) {
            EntityDolphin l = (EntityDolphin) loaded;
            c.eq(new BlockPos(123, 45, -678), l.getTreasurePos(), "treasure position saved");
            c.ok(l.hasGotFish(), "got fish saved");
            c.eq(1234, l.getMoistness(), "moistness saved");
            c.eq(4000, l.getAir(), "air saved");
            c.eq(3, l.getHeldItemMainhand().getCount(), "held item saved");
        }
        d.setDead();
    }

    private static NBTTagCompound withId(NBTTagCompound tag, Entity e) {
        tag.setString("id", EntityList.getKey(e).toString());
        return tag;
    }

    private void feeding() {
        BlockPos floor = Pools.pool(c, 3);
        EntityDolphin d = spawn(floor.add(4, 1, 4));
        d.setNoAI(true);
        EntityPlayerMP player = Players.real(world, "dolphinfeed");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.setPosition(d.posX + 1, d.posY, d.posZ);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.STICK));
        c.ok(!d.processInitialInteract(player, EnumHand.MAIN_HAND) || !d.hasGotFish(), "a stick is not food");
        c.ok(!d.hasGotFish(), "no fish after a stick");
        for (int meta = 0; meta < 4; meta++) {
            d.setGotFish(false);
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 3, meta));
            c.ok(d.processInitialInteract(player, EnumHand.MAIN_HAND), "raw fish " + meta + " is eaten");
            c.ok(d.hasGotFish(), "got fish after raw fish " + meta);
            c.eq(2, player.getHeldItemMainhand().getCount(), "one raw fish " + meta + " used");
        }
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.COOKED_FISH, 3));
        d.setGotFish(false);
        d.processInitialInteract(player, EnumHand.MAIN_HAND);
        c.ok(!d.hasGotFish(), "cooked fish is not dolphin food");
        player.interactionManager.setGameType(GameType.CREATIVE);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.FISH, 3));
        d.processInitialInteract(player, EnumHand.MAIN_HAND);
        c.eq(3, player.getHeldItemMainhand().getCount(), "creative feeding uses no fish");
        d.setDead();
    }

    private void treasure() {
        // Nothing locates structures: a fed dolphin forgets the fish and just swims.
        Structures.unregister(Structures.SHIPWRECK);
        Structures.unregister(Structures.OCEAN_RUIN);
        BlockPos floor = bigPool(6);
        EntityDolphin d = spawn(floor.add(2, 2, 2));
        d.setGotFish(true);
        tick(d, 20);
        c.ok(!d.hasGotFish(), "with no locator a fed dolphin forgets the fish");

        // A shipwreck at the far corner of the pool: it swims there.
        final BlockPos wreck = floor.add(27, 2, 27);
        Structures.register(Structures.SHIPWRECK, (w, from, radius) -> wreck);
        final BlockPos ruin = floor.add(-2000, 2, 0);
        Structures.register(Structures.OCEAN_RUIN, (w, from, radius) -> ruin);
        EntityDolphin guide = spawn(floor.add(2, 2, 2));
        guide.setGotFish(true);
        double start = horizontal(guide, wreck);
        tick(guide, 6);
        c.eq(wreck, guide.getTreasurePos(), "fed, it picks the nearer of a shipwreck and an ocean ruin");
        double best = start;
        boolean arrived = false;
        for (int i = 0; i < 1200 && !arrived; i++) {
            tick(guide, 1);
            best = Math.min(best, horizontal(guide, wreck));
            arrived = !guide.hasGotFish();
        }
        c.ok(best < start - 10.0D, "it swims toward the structure: " + (int) start + " -> " + (int) best + " blocks");
        UpdateAquaticBackport.LOGGER.info("SELFTEST dolphin: led {} -> {} blocks from the shipwreck, arrived={}", (int) start, (int) best, arrived);
        c.ok(arrived && horizontal(guide, wreck) < 6.0D, "it arrives and forgets the fish (" + (int) horizontal(guide, wreck) + " blocks away)");

        // Low on air it does not set off.
        EntityDolphin low = spawn(floor.add(5, 2, 5));
        low.setGotFish(true);
        low.setAir(90);
        tick(low, 4);
        c.eq(BlockPos.ORIGIN, low.getTreasurePos(), "under 100 air it does not look for treasure");
        Structures.unregister(Structures.SHIPWRECK);
        Structures.unregister(Structures.OCEAN_RUIN);
        d.setDead();
        guide.setDead();
        low.setDead();
    }

    private void grace() {
        Potion grace = DolphinContent.dolphinsGrace();
        BlockPos floor = bigPool(4);
        double[] plain = slide(floor.add(3, 1, 3), null);
        double[] graced = slide(floor.add(3, 1, 13), grace);
        c.ok(Math.abs(plain[0] - 0.8D) < 0.02D, "without the effect water keeps 0.8 of speed: " + plain[0]);
        c.ok(Math.abs(graced[0] - DolphinsGrace.SLOWDOWN) < 0.02D, "with Dolphin's Grace water keeps 0.96: " + graced[0]);
        c.ok(Math.abs(plain[1] - graced[1]) < 1.0E-6D, "vertical motion is unchanged by the effect");
    }

    /** @return {horizontal speed kept over one tick of travel, vertical motion after it} for a pig in still water */
    private double[] slide(BlockPos at, Potion potion) {
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        // No goals, but not NoAI: 1.12.2 never runs travel for a NoAI mob.
        pig.tasks.taskEntries.clear();
        world.spawnEntity(pig);
        if (potion != null) {
            pig.addPotionEffect(new PotionEffect(potion, 200));
        }
        tick(pig, 1);
        pig.motionX = 0.3D;
        pig.motionZ = 0.0D;
        pig.motionY = 0.0D;
        tick(pig, 1);
        double[] out = {pig.motionX / 0.3D, pig.motionY};
        pig.setDead();
        return out;
    }

    private void swimWithPlayer() {
        BlockPos floor = bigPool(5);
        EntityDolphin d = spawn(floor.add(5, 2, 5));
        EntityPlayerMP player = Players.real(world, "dolphinswim");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.setPosition(d.posX + 4, d.posY, d.posZ);
        world.playerEntities.add(player);
        try {
            player.handleWaterMovement();
            c.ok(player.isInWater(), "the test player is in water");
            tick(d, 20);
            c.ok(!player.isPotionActive(DolphinContent.dolphinsGrace()), "a player swimming without sprinting gets nothing");
            player.setSprinting(true);
            tick(d, 20);
            PotionEffect effect = player.getActivePotionEffect(DolphinContent.dolphinsGrace());
            c.ok(effect != null, "a sprint-swimming player within 10 blocks gets Dolphin's Grace");
            c.ok(effect != null && effect.getDuration() <= 100 && effect.getDuration() > 0, "for up to 5 seconds");
            player.removePotionEffect(DolphinContent.dolphinsGrace());
            d.setDead();
            player.setInvisible(true);
            EntityDolphin near = spawn(new BlockPos(player.posX - 4, player.posY, player.posZ));
            tick(near, 20);
            c.ok(!player.isPotionActive(DolphinContent.dolphinsGrace()), "an invisible player with no armor is not noticed 4 blocks away");
            player.setInvisible(false);
            tick(near, 20);
            c.ok(player.isPotionActive(DolphinContent.dolphinsGrace()), "visible again, the same player is");
            near.setDead();
        } finally {
            world.playerEntities.remove(player);
        }
        d.setDead();
    }

    private void playWithItems() {
        BlockPos floor = bigPool(4);
        EntityDolphin d = spawn(floor.add(5, 1, 5));
        EntityItem item = new EntityItem(world, d.posX + 3, d.posY + 0.5, d.posZ, new ItemStack(Items.STICK));
        world.spawnEntity(item);
        boolean held = false;
        boolean tossed = false;
        for (int i = 0; i < 400 && !(held && tossed); i++) {
            tick(d, 1);
            for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, d.getEntityBoundingBox().grow(16))) {
                if (!e.isDead && e.getItem().getItem() == Items.STICK) {
                    tick(e, 1);
                    if (held && e != item && e.cannotPickup()) {
                        tossed = true;
                    }
                }
            }
            held |= d.getHeldItemMainhand().getItem() == Items.STICK;
        }
        c.ok(held, "it picks up an item floating in water");
        c.ok(tossed, "and tosses it away again");
        for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, d.getEntityBoundingBox().grow(32))) {
            e.setDead();
        }
        d.setDead();
    }

    private void boats() {
        BlockPos floor = bigPool(4);
        EntityDolphin d = spawn(floor.add(5, 1, 5));
        EntityAIBase follow = task(d.tasks, "AIFollowBoat");
        EntityBoat boat = new EntityBoat(world, d.posX + 2, floor.getY() + 4.5, d.posZ);
        world.spawnEntity(boat);
        EntityZombie driver = new EntityZombie(world);
        driver.setPosition(boat.posX, boat.posY, boat.posZ);
        world.spawnEntity(driver);
        driver.startRiding(boat, true);
        c.ok(boat.getControllingPassenger() == driver, "the boat has a driver");
        driver.moveForward = 0.0F;
        c.ok(follow != null && !follow.shouldExecute(), "a boat that is not moving is ignored");
        driver.moveForward = 1.0F;
        c.ok(follow != null && follow.shouldExecute(), "a boat being steered within 5 blocks draws it");
        driver.dismountRidingEntity();
        driver.setDead();
        boat.setDead();
        d.setDead();
    }

    private void jumping() {
        BlockPos floor = bigPool(4);
        EntityDolphin d = spawn(floor.add(3, 4, 15));
        d.rotationYaw = -90.0F; // facing east, along open water
        d.setNoAI(true);
        EntityAIBase jump = task(d.tasks, "AIDolphinJump");
        boolean ready = false;
        for (int i = 0; i < 200 && !ready && jump != null; i++) {
            ready = jump.shouldExecute();
        }
        c.ok(ready, "at the surface with open water ahead it may leap");
        if (jump != null && ready) {
            double before = d.motionY;
            jump.startExecuting();
            c.ok(d.motionY - before > 0.69D, "the leap goes 0.7 up");
            c.ok(d.motionX > 0.59D, "and 0.6 forward");
        }
        EntityDolphin under = spawn(floor.add(3, 1, 5));
        under.rotationYaw = -90.0F;
        boolean never = true;
        for (int i = 0; i < 100 && jump != null; i++) {
            never &= !task(under.tasks, "AIDolphinJump").shouldExecute();
        }
        c.ok(never, "deep under water with no air above it does not leap");
        d.setDead();
        under.setDead();
    }

    private void defense() {
        BlockPos floor = bigPool(4);
        EntityDolphin hit = spawn(floor.add(5, 1, 5));
        EntityDolphin friend = spawn(floor.add(8, 1, 6));
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(floor.getX() + 6.5, floor.getY() + 1, floor.getZ() + 8.5);
        world.spawnEntity(zombie);
        // Vanilla's revenge goal ignores a hit taken on a mob's very first tick (its timer starts at 0).
        tick(hit, 5);
        tick(friend, 5);
        c.ok(hit.attackEntityFrom(DamageSource.causeMobDamage(zombie), 1.0F), "a zombie hits a dolphin");
        for (int i = 0; i < 6; i++) {
            tick(hit, 1);
            tick(friend, 1);
        }
        c.eq(zombie, hit.getAttackTarget(), "the dolphin fights back");
        c.eq(zombie, friend.getAttackTarget(), "and its pod joins in");
        float zombieHealth = zombie.getHealth();
        for (int i = 0; i < 200 && zombie.getHealth() >= zombieHealth; i++) {
            tick(hit, 1);
            tick(friend, 1);
        }
        c.ok(zombie.getHealth() < zombieHealth, "the dolphins bite the zombie");

        EntityDolphin calm = spawn(floor.add(20, 1, 20));
        EntityGuardian guardian = new EntityGuardian(world);
        guardian.setPosition(floor.getX() + 22.5, floor.getY() + 1, floor.getZ() + 20.5);
        world.spawnEntity(guardian);
        tick(calm, 5);
        calm.attackEntityFrom(DamageSource.causeMobDamage(guardian), 1.0F);
        for (int i = 0; i < 6; i++) {
            tick(calm, 1);
        }
        c.ok(calm.getAttackTarget() == null, "it never fights back against a guardian");
        hit.setDead();
        friend.setDead();
        zombie.setDead();
        calm.setDead();
        guardian.setDead();
    }

    private void drops() {
        BlockPos corner = c.area();
        BlockPos at = corner.add(16, 1, 16);
        int raw = 0;
        int cooked = 0;
        int max = 0;
        for (int i = 0; i < 200; i++) {
            EntityDolphin d = spawn(at);
            d.onDeath(DamageSource.GENERIC);
            int n = Drops.count(world, at, Items.FISH);
            raw += n;
            max = Math.max(max, n);
            cooked += Drops.count(world, at, Items.COOKED_FISH);
            d.setDead();
        }
        UpdateAquaticBackport.LOGGER.info("SELFTEST dolphin: 200 deaths dropped {} raw cod", raw);
        c.ok(raw > 60 && raw < 140, "0-1 raw cod per dolphin: " + raw + " from 200");
        c.ok(max <= 1, "never more than 1 without Looting");
        c.eq(0, cooked, "no cooked cod from a dolphin not on fire");
        int burnt = 0;
        int rawWhenBurning = 0;
        for (int i = 0; i < 100; i++) {
            EntityDolphin d = spawn(at);
            d.setFire(5);
            d.onDeath(DamageSource.IN_FIRE);
            burnt += Drops.count(world, at, Items.COOKED_FISH);
            rawWhenBurning += Drops.count(world, at, Items.FISH);
            d.setDead();
        }
        c.ok(burnt > 25 && rawWhenBurning == 0, "a burning dolphin drops cooked cod only: " + burnt + " cooked, " + rawWhenBurning + " raw");

        EntityDolphin d = spawn(at);
        d.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.STICK));
        // What it picked up, it always drops (1.14.4 sets the hand's drop chance to 2).
        EntityItem pickup = new EntityItem(world, d.posX, d.posY, d.posZ, new ItemStack(Items.BONE));
        d.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        world.spawnEntity(pickup);
        d.onLivingUpdate();
        c.eq(Items.BONE, d.getHeldItemMainhand().getItem(), "it picks up into its mouth");
        d.onDeath(DamageSource.GENERIC);
        c.eq(1, Drops.count(world, at, Items.BONE), "and drops what it carries when it dies");
        Drops.count(world, at, Items.FISH);
        d.setDead();
    }

    // ---------------------------------------------------------------------------------------------

    private EntityDolphin spawn(BlockPos at) {
        EntityDolphin d = new EntityDolphin(world);
        d.setPosition(at.getX() + 0.5, at.getY() + 0.1, at.getZ() + 0.5);
        d.onInitialSpawn(world.getDifficultyForLocation(at), null);
        world.spawnEntity(d);
        return d;
    }

    /**
     * One world tick of one entity, as {@code World.updateEntityWithOptionalForce} does it. That method
     * skips entities without 32 loaded blocks around them, which the test areas do not have.
     */
    private void tick(Entity e, int ticks) {
        for (int i = 0; i < ticks && !e.isDead; i++) {
            e.lastTickPosX = e.posX;
            e.lastTickPosY = e.posY;
            e.lastTickPosZ = e.posZ;
            e.prevRotationYaw = e.rotationYaw;
            e.prevRotationPitch = e.rotationPitch;
            ++e.ticksExisted;
            if (e.isRiding()) {
                e.updateRidden();
            } else {
                e.onUpdate();
            }
        }
    }

    private static double horizontal(Entity e, BlockPos p) {
        double dx = p.getX() + 0.5 - e.posX;
        double dz = p.getZ() + 0.5 - e.posZ;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static boolean spawnsIn(Biome biome) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.WATER_CREATURE)) {
            if (e.entityClass == EntityDolphin.class) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTask(EntityAITasks tasks, Class<?> type) {
        for (EntityAITasks.EntityAITaskEntry e : tasks.taskEntries) {
            if (type.isInstance(e.action)) {
                return true;
            }
        }
        return false;
    }

    private static EntityAIBase task(EntityAITasks tasks, String simpleName) {
        for (EntityAITasks.EntityAITaskEntry e : tasks.taskEntries) {
            if (e.action.getClass().getSimpleName().equals(simpleName)) {
                return e.action;
            }
        }
        return null;
    }

    /**
     * A stone-walled pool of still water filling a whole test area: inside 30x30, floor at y=99, water
     * from y=100 up {@code depth} blocks, open air above.
     *
     * @return the floor block at the inner north-west corner
     */
    private BlockPos bigPool(int depth) {
        BlockPos corner = c.area();
        IBlockState stone = Blocks.STONE.getDefaultState();
        IBlockState water = Blocks.WATER.getDefaultState();
        for (int x = 0; x < 32; x++) {
            for (int z = 0; z < 32; z++) {
                boolean wall = x == 0 || z == 0 || x == 31 || z == 31;
                for (int y = 0; y < depth; y++) {
                    world.setBlockState(corner.add(x, y, z), wall ? stone : water, 2 | 16);
                }
            }
        }
        return corner.add(1, -1, 1);
    }
}
