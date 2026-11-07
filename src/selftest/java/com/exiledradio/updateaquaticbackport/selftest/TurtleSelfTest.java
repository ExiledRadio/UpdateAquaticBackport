package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.turtle.BlockTurtleEgg;
import com.exiledradio.updateaquaticbackport.turtle.EntityTurtle;
import com.exiledradio.updateaquaticbackport.turtle.TurtleContent;
import com.exiledradio.updateaquaticbackport.turtle.TurtleEvents;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.passive.EntityCow;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityWolf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.Path;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.List;

/** Turtles, eggs, the scute and shell, Turtle Master, and what other mobs do about turtles. */
public final class TurtleSelfTest implements SelfTest {

    @Override
    public String name() {
        return "turtle";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(TurtleContent.enabled(), "turtle stream on");
        registration(c);
        basics(c, world);
        spawning(c, world);
        eggBlock(c, world);
        hatching(c, world);
        trampling(c, world);
        breedingAndLaying(c, world);
        growingAndDrops(c, world);
        otherMobs(c, world);
        movement(c, world);
        shell(c, world);
        potions(c, world);
    }

    // --- 1. registration -----------------------------------------------------------------------------

    private static void registration(Check c) {
        BlockTurtleEgg egg = TurtleContent.turtleEgg();
        c.ok(egg != null && Content.id("turtle_egg").equals(egg.getRegistryName()), "turtle egg block");
        c.ok(Item.getItemFromBlock(egg) != Items.AIR, "turtle egg item");
        c.ok(TurtleContent.scute() != null && Content.id("scute").equals(TurtleContent.scute().getRegistryName()), "scute");
        c.ok(TurtleContent.turtleHelmet() != null && Content.id("turtle_helmet").equals(TurtleContent.turtleHelmet().getRegistryName()),
                "turtle shell");
        EntityEntry entry = ForgeRegistries.ENTITIES.getValue(Content.id("turtle"));
        c.ok(entry != null && entry.getEntityClass() == EntityTurtle.class, "turtle entity entry");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("turtle")), "turtle spawn egg");
        EntityRegistry.EntityRegistration reg = EntityRegistry.instance().lookupModSpawn(EntityTurtle.class, false);
        c.ok(reg != null && reg.getModEntityId() == TurtleContent.TURTLE_ENTITY_ID, "entity network id 10");
        for (IBlockState s : egg.getBlockState().getValidStates()) {
            c.ok(egg.getStateFromMeta(egg.getMetaFromState(s)) == s, "egg meta round trip " + s);
        }
        c.eq(12, egg.getBlockState().getValidStates().size(), "egg states");
        for (String sound : new String[] {"entity.turtle.egg_hatch", "entity.zombie.destroy_egg", "item.armor.equip_turtle"}) {
            c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(Content.id(sound)), "sound " + sound);
        }
    }

    // --- 2. the turtle itself -------------------------------------------------------------------------

    private static void basics(Check c, WorldServer world) {
        EntityTurtle t = new EntityTurtle(world);
        c.eq(30.0D, t.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).getBaseValue(), "health 30");
        c.eq(0.25D, t.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getBaseValue(), "speed 0.25");
        c.ok(Math.abs(t.width - 1.2F) < 1e-4 && Math.abs(t.height - 0.4F) < 1e-4, "adult 1.2 x 0.4: " + t.width + " x " + t.height);
        t.setGrowingAge(-24000);
        c.ok(Math.abs(t.width - 0.36F) < 1e-4 && Math.abs(t.height - 0.12F) < 1e-4, "baby 0.36 x 0.12: " + t.width + " x " + t.height);
        c.ok(t.canBreatheUnderwater(), "breathes underwater");
        c.ok(!t.isPushedByWater(), "not pushed by water");
        c.ok(!t.canBeLeashedTo(null), "cannot be leashed");

        // Saving: write and read back, through the entity list as a chunk does.
        EntityTurtle saved = new EntityTurtle(world);
        saved.setPosition(1, 2, 3);
        saved.setHome(new BlockPos(10, 64, -20));
        saved.setHasEgg(true);
        NBTTagCompound tag = new NBTTagCompound();
        c.ok(saved.writeToNBTOptional(tag), "turtle writes to NBT");
        c.eq("updateaquaticbackport:turtle", tag.getString("id"), "saved id");
        c.eq(10, tag.getInteger("HomePosX"), "1.13's HomePosX key");
        Entity back = EntityList.createEntityFromNBT(tag, world);
        c.ok(back instanceof EntityTurtle, "turtle read back");
        if (back instanceof EntityTurtle) {
            c.eq(new BlockPos(10, 64, -20), ((EntityTurtle) back).getHome(), "home survives saving");
            c.ok(((EntityTurtle) back).hasEgg(), "carrying eggs survives saving");
        }

        // A spawned group: its first is grown, later ones babies about one time in 20.
        BlockPos at = c.area();
        EntityTurtle first = new EntityTurtle(world);
        first.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        Object group = first.onInitialSpawn(world.getDifficultyForLocation(at), null);
        c.ok(!first.isChild(), "a group's first turtle is grown");
        c.eq(at, first.getHome(), "home is where it spawned");
        int babies = 0;
        for (int i = 0; i < 2000; i++) {
            EntityTurtle more = new EntityTurtle(world);
            more.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            more.onInitialSpawn(world.getDifficultyForLocation(at), (net.minecraft.entity.IEntityLivingData) group);
            if (more.isChild()) {
                babies++;
            }
        }
        c.ok(babies > 40 && babies < 180, "about 5% of later group members are babies: " + babies + "/2000");
    }

    private static void spawning(Check c, WorldServer world) {
        List<Biome> biomes = TurtleContent.spawnBiomes();
        c.ok(biomes.contains(Biomes.BEACH), "spawns on beaches");
        c.ok(!biomes.contains(Biomes.COLD_BEACH), "not on snowy beaches");
        c.ok(!biomes.contains(Biomes.STONE_BEACH), "not on stone shores");
        boolean listed = false;
        for (Biome.SpawnListEntry e : Biomes.BEACH.getSpawnableList(EnumCreatureType.CREATURE)) {
            if (e.entityClass == EntityTurtle.class) {
                listed = e.itemWeight == 5 && e.minGroupCount == 2 && e.maxGroupCount == 5;
            }
        }
        c.ok(listed, "beach creature list has turtles, weight 5, groups of 2-5");

        BlockPos corner = c.area();
        int sea = world.getSeaLevel();
        BlockPos sandLow = new BlockPos(corner.getX() + 4, sea - 1, corner.getZ() + 4);
        world.setBlockState(sandLow, Blocks.SAND.getDefaultState(), 2);
        BlockPos grassLow = sandLow.east(3);
        world.setBlockState(grassLow, Blocks.GRASS.getDefaultState(), 2);
        BlockPos sandHigh = corner.add(10, -1, 10);
        world.setBlockState(sandHigh, Blocks.SAND.getDefaultState(), 2);
        // The test area's sky light is not recomputed after clearing; set the open sky's 15 where it is asked.
        for (BlockPos p : new BlockPos[] {sandLow.up(), grassLow.up(), sandHigh.up()}) {
            world.setLightFor(net.minecraft.world.EnumSkyBlock.SKY, p, 15);
        }
        c.ok(canSpawnOn(world, sandLow), "can spawn on sand at sea level (light " + world.getLight(sandLow.up()) + ")");
        c.ok(!canSpawnOn(world, grassLow), "cannot spawn on grass");
        c.ok(!canSpawnOn(world, sandHigh), "cannot spawn on sand far above sea level");
        world.setLightFor(net.minecraft.world.EnumSkyBlock.SKY, sandLow.up(), 4);
        c.ok(!canSpawnOn(world, sandLow), "cannot spawn in the dark");

        // A new beach chunk's animals: the generator's own spawning, on a sand flat at sea level.
        BlockPos flat = c.area();
        kill(world, flat);
        c.fill(flat.down(), flat.add(31, -1, 31), Blocks.AIR.getDefaultState());
        c.fill(new BlockPos(flat.getX(), sea - 1, flat.getZ()), new BlockPos(flat.getX() + 31, sea - 1, flat.getZ() + 31),
                Blocks.SAND.getDefaultState());
        java.util.Random random = new java.util.Random(1013L);
        List<EntityTurtle> spawned = java.util.Collections.emptyList();
        AxisAlignedBB box = new AxisAlignedBB(flat.getX(), sea - 2, flat.getZ(), flat.getX() + 32, sea + 4, flat.getZ() + 32);
        for (int i = 0; i < 400 && spawned.isEmpty(); i++) {
            net.minecraft.world.WorldEntitySpawner.performWorldGenSpawning(world, Biomes.BEACH, flat.getX() + 8, flat.getZ() + 8, 16, 16, random);
            spawned = world.getEntitiesWithinAABB(EntityTurtle.class, box);
        }
        BlockPos top = world.getTopSolidOrLiquidBlock(flat.add(12, 0, 12));
        c.ok(spawned.size() >= 2 && spawned.size() <= 5, "a new beach chunk gets a group of 2-5 turtles: " + spawned.size() + " [top " + top
                + " " + world.getBlockState(top.down()) + ", chance " + Biomes.BEACH.getSpawningChance() + ", list "
                + Biomes.BEACH.getSpawnableList(EnumCreatureType.CREATURE).size() + ", all here "
                + world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(flat).grow(48)).size() + "]");
        boolean homes = !spawned.isEmpty();
        for (EntityTurtle t : spawned) {
            homes &= t.getHome().equals(new BlockPos(t)) && t.getHome().getY() == sea;
            t.setDead();
        }
        c.ok(homes, "each spawned turtle's home is where it spawned, on the sand");
        kill(world, flat);
    }

    private static boolean canSpawnOn(WorldServer world, BlockPos ground) {
        EntityTurtle t = new EntityTurtle(world);
        t.setPosition(ground.getX() + 0.5, ground.getY() + 1, ground.getZ() + 0.5);
        return t.getCanSpawnHere();
    }

    // --- 3. the egg block ------------------------------------------------------------------------------

    private static void eggBlock(Check c, WorldServer world) {
        BlockTurtleEgg egg = TurtleContent.turtleEgg();
        Item eggItem = Item.getItemFromBlock(egg);
        BlockPos floor = c.area().down();
        EntityPlayerMP player = Players.real(world, "turtleegg");
        player.interactionManager.setGameType(GameType.SURVIVAL);

        place(player, world, new ItemStack(eggItem, 8), floor, EnumFacing.UP);
        BlockPos at = floor.up();
        c.eq(egg.getDefaultState(), world.getBlockState(at), "placed egg: one egg, uncracked");
        AxisAlignedBB one = world.getBlockState(at).getBoundingBox(world, at);
        place(player, world, player.getHeldItemMainhand(), at, EnumFacing.UP);
        c.eq(2, eggs(world, at), "an egg used on an egg adds one");
        place(player, world, player.getHeldItemMainhand(), floor, EnumFacing.UP);
        c.eq(3, eggs(world, at), "an egg used on the block below adds one too");
        place(player, world, player.getHeldItemMainhand(), at, EnumFacing.UP);
        c.eq(4, eggs(world, at), "up to four");
        c.eq(4, player.getHeldItemMainhand().getCount(), "each egg added uses one");
        place(player, world, player.getHeldItemMainhand(), at, EnumFacing.UP);
        c.eq(4, eggs(world, at), "never more than four");
        c.ok(world.getBlockState(at.up()).getBlock() == egg, "a fifth goes on top instead");
        world.setBlockToAir(at.up());
        AxisAlignedBB many = world.getBlockState(at).getBoundingBox(world, at);
        c.ok(one.maxX - one.minX < many.maxX - many.minX && Math.abs(one.maxY - 7 / 16.0) < 1e-6, "one egg is smaller than many");
        c.eq(0.5F, egg.getBlockHardness(egg.getDefaultState(), world, at), "hardness 0.5");
        c.ok(Math.abs(egg.getExplosionResistance(null) - 0.5F) < 1e-4, "blast resistance 0.5: " + egg.getExplosionResistance(null));

        // Breaking takes one egg at a time; only Silk Touch drops it.
        ItemStack pick = new ItemStack(Items.IRON_PICKAXE);
        breakWith(player, at, pick);
        c.eq(3, eggs(world, at), "breaking takes one egg");
        c.eq(0, Drops.count(world, at, eggItem), "no drop without Silk Touch");
        ItemStack silk = new ItemStack(Items.IRON_PICKAXE);
        silk.addEnchantment(Enchantments.SILK_TOUCH, 1);
        c.ok(EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, silk) == 1, "silk pick");
        breakWith(player, at, silk);
        c.eq(2, eggs(world, at), "Silk Touch takes one egg too");
        c.eq(1, Drops.count(world, at, eggItem), "Silk Touch drops one egg");
        breakWith(player, at, silk);
        breakWith(player, at, silk);
        c.ok(world.isAirBlock(at), "the last egg takes the block");
        c.eq(2, Drops.count(world, at, eggItem), "one egg each");
    }

    private static void hatching(Check c, WorldServer world) {
        BlockTurtleEgg egg = TurtleContent.turtleEgg();
        long time = world.getWorldTime();
        BlockPos corner = c.area();
        BlockPos onSand = corner.add(5, 0, 5);
        world.setBlockState(onSand.down(), Blocks.SAND.getDefaultState(), 2);
        BlockPos onStone = corner.add(15, 0, 5);
        try {
            world.setWorldTime(21500);
            boolean always = true;
            for (int i = 0; i < 50; i++) {
                always &= BlockTurtleEgg.canGrow(world);
            }
            c.ok(always, "about 3:30 am every random tick cracks an egg (angle " + world.getCelestialAngle(1.0F) + ")");
            world.setBlockState(onStone, egg.getDefaultState(), 2);
            egg.randomTick(world, onStone, world.getBlockState(onStone), world.rand);
            c.eq(0, world.getBlockState(onStone).getValue(BlockTurtleEgg.HATCH), "no cracking off sand");
            world.setBlockState(onSand, egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, 3), 2);
            egg.randomTick(world, onSand, world.getBlockState(onSand), world.rand);
            c.eq(1, world.getBlockState(onSand).getValue(BlockTurtleEgg.HATCH), "cracks once");
            egg.randomTick(world, onSand, world.getBlockState(onSand), world.rand);
            c.eq(2, world.getBlockState(onSand).getValue(BlockTurtleEgg.HATCH), "cracks twice");
            egg.randomTick(world, onSand, world.getBlockState(onSand), world.rand);
            c.ok(world.isAirBlock(onSand), "third time it hatches");
            List<EntityTurtle> born = world.getEntitiesWithinAABB(EntityTurtle.class, new AxisAlignedBB(onSand).grow(2));
            c.eq(3, born.size(), "one baby per egg");
            for (EntityTurtle t : born) {
                c.ok(t.isChild() && t.getGrowingAge() == -24000, "hatchling is a baby for a day");
                c.eq(onSand, t.getHome(), "hatchling's home is the nest");
                c.eq(0.0F, t.rotationYaw, "hatchling faces south");
                t.setDead();
            }

            world.setWorldTime(6000);
            int cracks = 0;
            for (int i = 0; i < 50000; i++) {
                if (BlockTurtleEgg.canGrow(world)) {
                    cracks++;
                }
            }
            c.ok(cracks > 60 && cracks < 140, "by day one random tick in 500: " + cracks + "/50000");
        } finally {
            world.setWorldTime(time);
        }
    }

    private static void trampling(Check c, WorldServer world) {
        BlockTurtleEgg egg = TurtleContent.turtleEgg();
        c.ok(!BlockTurtleEgg.canTrample(world, new EntityTurtle(world)), "turtles never trample");
        c.ok(!BlockTurtleEgg.canTrample(world, new EntityBat(world)), "bats never trample");
        c.ok(!BlockTurtleEgg.canTrample(world, new EntityItem(world)), "items never trample");
        c.ok(BlockTurtleEgg.canTrample(world, new EntityArmorStand(world)), "armor stands do");
        c.ok(BlockTurtleEgg.canTrample(world, new EntityZombie(world)), "zombies do");
        EntityPlayerMP player = Players.real(world, "turtletrample");
        c.ok(BlockTurtleEgg.canTrample(world, player), "players do");
        String griefing = world.getGameRules().getString("mobGriefing");
        world.getGameRules().setOrCreateGameRule("mobGriefing", "false");
        c.ok(!BlockTurtleEgg.canTrample(world, new EntityZombie(world)), "no mob trampling without mob griefing");
        c.ok(BlockTurtleEgg.canTrample(world, player), "players trample regardless");
        world.getGameRules().setOrCreateGameRule("mobGriefing", griefing);

        BlockPos at = c.area();
        world.setBlockState(at, egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, 4), 2);
        player.setSneaking(true);
        for (int i = 0; i < 2000; i++) {
            egg.onEntityWalk(world, at, player);
        }
        c.eq(4, eggs(world, at), "sneaking over eggs is safe");
        player.setSneaking(false);
        int steps = 0;
        while (eggs(world, at) == 4 && steps < 5000) {
            egg.onEntityWalk(world, at, player);
            steps++;
        }
        c.ok(eggs(world, at) == 3 && steps > 0, "walking over eggs breaks one now and then (" + steps + " steps)");
        int landed = 0;
        int broke = 0;
        for (int i = 0; i < 300; i++) {
            if (world.getBlockState(at).getBlock() != egg) {
                world.setBlockState(at, egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, 4), 2);
            }
            int before = eggs(world, at);
            egg.onFallenUpon(world, at, player, 1.0F);
            landed++;
            if (world.getBlockState(at).getBlock() != egg || eggs(world, at) < before) {
                broke++;
            }
        }
        c.ok(broke > 60 && broke < 140, "landing on eggs breaks one a third of the time: " + broke + "/" + landed);
        world.setBlockState(at, egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, 4), 2);
        EntityZombie zombie = new EntityZombie(world);
        for (int i = 0; i < 300; i++) {
            egg.onFallenUpon(world, at, zombie, 1.0F);
        }
        c.eq(4, eggs(world, at), "zombies landing on eggs do not break them");
    }

    // --- 4. breeding and nesting -----------------------------------------------------------------------

    private static void breedingAndLaying(Check c, WorldServer world) {
        Item seagrass = TurtleContent.seagrass();
        c.ok(seagrass != null, "seagrass found (kelp stream on)");
        if (seagrass == null) {
            return;
        }
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.SAND.getDefaultState());
        EntityTurtle a = turtle(world, corner.add(8, 0, 8));
        EntityTurtle b = turtle(world, corner.add(9, 0, 8));
        EntityPlayerMP player = Players.real(world, "turtlebreed");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        c.ok(a.isBreedingItem(new ItemStack(seagrass)), "seagrass is the breeding item");
        c.ok(!a.isBreedingItem(new ItemStack(Items.FISH)), "fish is not (it was in early 1.13 snapshots)");
        feed(player, a, seagrass);
        feed(player, b, seagrass);
        c.ok(a.isInLove() && b.isInLove(), "fed seagrass, both in love");
        int mating = 0;
        for (; mating < 1200 && !a.hasEgg() && !b.hasEgg(); mating++) {
            a.onUpdate();
            b.onUpdate();
        }
        EntityTurtle mother = a.hasEgg() ? a : b;
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST turtle: mated after {} ticks", mating);
        c.ok(a.hasEgg() != b.hasEgg(), "one of the pair carries eggs, after " + mating + " ticks [" + debug(a) + " | " + debug(b) + "]");
        c.ok(a.getGrowingAge() >= 5990 && b.getGrowingAge() >= 5990, "both wait five minutes: " + a.getGrowingAge() + ", " + b.getGrowingAge());
        c.ok(!world.getEntitiesWithinAABB(EntityXPOrb.class, new AxisAlignedBB(corner).grow(40)).isEmpty(), "breeding gives experience");
        c.eq(2, world.getEntitiesWithinAABB(EntityTurtle.class, new AxisAlignedBB(corner).grow(40)).size(), "no baby is born");
        // Cooldown aside (it would refuse anyway), a turtle carrying eggs is not fed.
        mother.setGrowingAge(0);
        int left = player.getHeldItemMainhand().getCount();
        feed(player, mother, seagrass);
        c.eq(left, player.getHeldItemMainhand().getCount(), "a turtle carrying eggs takes no seagrass");

        // Laying: at home on sand, dig for 200 ticks, then 1-4 eggs on the sand.
        mother.setHome(new BlockPos(mother));
        b.setDead();
        BlockTurtleEgg egg = TurtleContent.turtleEgg();
        BlockPos nest = null;
        boolean dug = false;
        int laying = 0;
        for (; laying < 1200 && nest == null; laying++) {
            mother.onUpdate();
            dug |= mother.isDigging();
            nest = find(world, corner, egg);
        }
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST turtle: laid after {} ticks", laying);
        c.ok(dug, "digs before laying");
        c.ok(nest != null, "lays eggs at home");
        if (nest != null) {
            c.ok(eggs(world, nest) >= 1 && eggs(world, nest) <= 4, "1-4 eggs: " + eggs(world, nest));
            c.eq(Blocks.SAND, world.getBlockState(nest.down()).getBlock(), "on sand");
            c.ok(nest.distanceSq(mother.getHome()) < 9 * 9, "near home");
        }
        c.ok(!mother.hasEgg() && !mother.isDigging(), "done laying");
        kill(world, corner);
    }

    private static void feed(EntityPlayerMP player, EntityTurtle turtle, Item seagrass) {
        if (player.getHeldItemMainhand().getItem() != seagrass) {
            player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(seagrass, 16));
        }
        turtle.processInteract(player, EnumHand.MAIN_HAND);
    }

    // --- 5. growing up and dying -----------------------------------------------------------------------

    private static void growingAndDrops(Check c, WorldServer world) {
        BlockPos at = c.area();
        EntityTurtle baby = turtle(world, at);
        baby.setGrowingAge(-1);
        baby.onUpdate();
        c.ok(!baby.isChild(), "grew up");
        c.eq(1, Drops.count(world, at, TurtleContent.scute()), "growing up drops a scute");
        baby.setDead();

        Item seagrass = TurtleContent.seagrass();
        int total = 0;
        int max = 0;
        for (int i = 0; i < 200; i++) {
            EntityTurtle t = turtle(world, at);
            t.onDeath(DamageSource.GENERIC);
            int n = seagrass == null ? 0 : Drops.count(world, at, seagrass);
            total += n;
            max = Math.max(max, n);
            t.setDead();
        }
        c.ok(seagrass == null || total > 140 && total < 260 && max <= 2, "0-2 seagrass, 1 on average: " + total + "/200, max " + max);

        EntityTurtle struck = turtle(world, at);
        struck.onStruckByLightning(null);
        c.ok(struck.getHealth() <= 0.0F, "lightning kills a turtle");
        c.eq(1, Drops.count(world, at, Items.BOWL), "killed by lightning, drops a bowl");
        struck.setDead();

        EntityTurtle young = turtle(world, at);
        young.setGrowingAge(-24000);
        young.onDeath(DamageSource.LIGHTNING_BOLT);
        c.eq(0, Drops.count(world, at, Items.BOWL) + (seagrass == null ? 0 : Drops.count(world, at, seagrass)), "babies drop nothing");
        young.setDead();
    }

    // --- 6. other mobs ---------------------------------------------------------------------------------

    private static void otherMobs(Check c, WorldServer world) {
        BlockPos at = c.area();
        EntityZombie zombie = spawn(world, new EntityZombie(world), at);
        c.ok(TurtleEvents.seeksEggs(zombie), "zombies seek eggs");
        c.ok(TurtleEvents.huntsBabyTurtles(zombie), "zombies hunt baby turtles");
        TurtleEvents.addGoals(zombie);
        int goals = zombie.tasks.taskEntries.size();
        TurtleEvents.addGoals(zombie);
        c.eq(goals, zombie.tasks.taskEntries.size(), "goals are added once");
        EntityPigZombie pigman = spawn(world, new EntityPigZombie(world), at);
        c.ok(TurtleEvents.seeksEggs(pigman) && !TurtleEvents.huntsBabyTurtles(pigman), "zombie pigmen seek eggs, leave turtles");
        c.ok(TurtleEvents.huntsBabyTurtles(spawn(world, new EntitySkeleton(world), at)), "skeletons hunt baby turtles");
        c.ok(TurtleEvents.huntsBabyTurtles(spawn(world, new EntityWolf(world), at)), "wolves hunt baby turtles");
        c.ok(TurtleEvents.huntsBabyTurtles(spawn(world, new EntityOcelot(world), at)), "ocelots hunt baby turtles");
        EntityCow cow = spawn(world, new EntityCow(world), at);
        c.ok(!TurtleEvents.huntsBabyTurtles(cow) && !TurtleEvents.seeksEggs(cow), "cows do neither");
        kill(world, at);

        // The predicate: a baby out of the water.
        EntityTurtle adult = turtle(world, at);
        EntityTurtle baby = turtle(world, at.east(4));
        baby.setGrowingAge(-24000);
        c.ok(EntityTurtle.TARGET_DRY_BABY.apply(baby), "a dry baby is prey");
        c.ok(!EntityTurtle.TARGET_DRY_BABY.apply(adult), "an adult is not");
        BlockPos pool = Pools.pool(c, 4);
        EntityTurtle wet = turtle(world, pool.add(3, 2, 3));
        wet.setGrowingAge(-24000);
        wet.onUpdate();
        c.ok(wet.isInWater() && !EntityTurtle.TARGET_DRY_BABY.apply(wet), "a baby in water is not");
        wet.setDead();
        adult.setDead();

        // A zombie finds the baby.
        EntityZombie hunter = spawn(world, new EntityZombie(world), at.add(0, 0, 8));
        hunter.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        for (int i = 0; i < 300 && hunter.getAttackTarget() != baby; i++) {
            hunter.onUpdate();
            baby.onUpdate();
        }
        c.ok(hunter.getAttackTarget() == baby, "a zombie targets a baby turtle on land");
        EntityWolf wolf = spawn(world, new EntityWolf(world), at.add(10, 0, 6));
        for (int i = 0; i < 300 && wolf.getAttackTarget() != baby; i++) {
            wolf.onUpdate();
        }
        c.ok(wolf.getAttackTarget() == baby, "a wild wolf targets a baby turtle on land [baby " + debug(baby) + " wolf " + wolf.getPositionVector()
                + " " + wolf.getAttackTarget() + "]");
        kill(world, at);

        // Stomping: a zombie on a cluster of eggs breaks it all within a few seconds; not without mob griefing.
        BlockTurtleEgg egg = TurtleContent.turtleEgg();
        BlockPos nestAt = c.area().add(8, 0, 8);
        world.setBlockState(nestAt, egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, 4), 2);
        EntityZombie stomper = spawn(world, new EntityZombie(world), nestAt);
        stomper.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
        stomper.setPosition(nestAt.getX() + 0.5, nestAt.getY() + 7 / 16.0, nestAt.getZ() + 0.5);
        int ticks = 0;
        while (world.getBlockState(nestAt).getBlock() == egg && ticks < 200) {
            stomper.onUpdate();
            ticks++;
        }
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST turtle: zombie stomped the nest in {} ticks", ticks);
        c.ok(world.getBlockState(nestAt).getBlock() != egg, "a zombie stomps a whole cluster (" + ticks + " ticks)");
        stomper.setDead();
        String griefing = world.getGameRules().getString("mobGriefing");
        world.getGameRules().setOrCreateGameRule("mobGriefing", "false");
        try {
            world.setBlockState(nestAt, egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, 4), 2);
            EntityZombie polite = spawn(world, new EntityZombie(world), nestAt);
            polite.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.LEATHER_HELMET));
            polite.setPosition(nestAt.getX() + 0.5, nestAt.getY() + 7 / 16.0, nestAt.getZ() + 0.5);
            for (int i = 0; i < 200; i++) {
                polite.onUpdate();
            }
            c.eq(4, eggs(world, nestAt), "no stomping without mob griefing");
            polite.setDead();
        } finally {
            world.getGameRules().setOrCreateGameRule("mobGriefing", griefing);
        }
    }

    // --- 7. getting about ------------------------------------------------------------------------------

    private static void movement(Check c, WorldServer world) {
        // A beach: sand floor, a pool sunk into it level with the sand.
        BlockPos corner = c.area();
        c.fill(corner.add(0, -1, 0), corner.add(31, -1, 31), Blocks.SAND.getDefaultState());
        c.fill(corner.add(0, -2, 0), corner.add(31, -4, 31), Blocks.STONE.getDefaultState());
        c.fill(corner.add(16, -3, 4), corner.add(27, -1, 27), Blocks.WATER.getDefaultState());
        // Turtles are slow on land (as in 1.14, about a block every 5 seconds with the path stops): start 4 blocks off.
        EntityTurtle walker = turtle(world, corner.add(12, 0, 12));
        walker.setHome(new BlockPos(walker));
        boolean wet = false;
        int walked = 0;
        for (; walked < 2400 && !wet; walked++) {
            walker.onUpdate();
            wet = walker.isInWater();
        }
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST turtle: into the water after {} ticks", walked);
        if (!wet) {
            for (int i = 0; i < 5; i++) {
                walker.onUpdate();
                StringBuilder pts = new StringBuilder();
                Path p = walker.getNavigator().getPath();
                if (p != null) {
                    pts.append("idx ").append(p.getCurrentPathIndex()).append(": ");
                    for (int j = 0; j < p.getCurrentPathLength(); j++) {
                        net.minecraft.pathfinding.PathPoint q = p.getPathPointFromIndex(j);
                        pts.append(q.x).append(',').append(q.y).append(',').append(q.z).append(' ').append(q.nodeType).append("; ");
                    }
                }
                com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST turtle walker {} move {} {} {} upd={} ai={} mot={} {} {} yaw={} fwd={} path {}",
                        walker.getPositionVector(), walker.getMoveHelper().getX(), walker.getMoveHelper().getY(), walker.getMoveHelper().getZ(),
                        walker.getMoveHelper().isUpdating(), walker.getAIMoveSpeed(), walker.motionX, walker.motionY, walker.motionZ,
                        walker.rotationYaw, walker.moveForward, pts);
            }
        }
        c.ok(wet, "a turtle on the beach finds its way into the water, after " + walked + " ticks [" + debug(walker) + " start "
                + corner.add(12, 0, 12) + "]");

        EntityTurtle swimmer = turtle(world, corner.add(18, -2, 6));
        swimmer.onUpdate();
        c.ok(swimmer.isInWater(), "swimmer in water");
        // Within the turtle's 16-block path range, as every path it makes.
        double tx = corner.getX() + 25.5, ty = corner.getY() - 3, tz = corner.getZ() + 14.5;
        Path path = swimmer.getNavigator().getPathToXYZ(tx, ty, tz);
        c.ok(path != null && path.getCurrentPathLength() > 1, "a path through the water");
        double before = swimmer.getDistanceSq(tx, ty, tz);
        swimmer.getNavigator().setPath(path, 1.0D);
        for (int i = 0; i < 200; i++) {
            swimmer.onUpdate();
            if (swimmer.getNavigator().noPath()) {
                swimmer.getNavigator().tryMoveToXYZ(tx, ty, tz, 1.0D);
            }
        }
        double after = swimmer.getDistanceSq(tx, ty, tz);
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST turtle: swam {} -> {} blocks from the target in 200 ticks", Math.sqrt(before), Math.sqrt(after));
        c.ok(after < before / 4, "it swims there: " + Math.sqrt(before) + " -> " + Math.sqrt(after));
        walker.setDead();
        swimmer.setDead();
    }

    // --- 8. the shell ----------------------------------------------------------------------------------

    private static void shell(Check c, WorldServer world) {
        ItemArmor helmet = TurtleContent.turtleHelmet();
        Item scute = TurtleContent.scute();
        c.eq(2, helmet.damageReduceAmount, "the shell gives 2 armor");
        c.eq(275, new ItemStack(helmet).getMaxDamage(), "durability 275");
        c.eq(9, helmet.getItemEnchantability(), "enchantability 9");
        c.ok(helmet.getIsRepairable(new ItemStack(helmet), new ItemStack(scute)), "repaired with scutes");
        c.ok(!helmet.getIsRepairable(new ItemStack(helmet), new ItemStack(Items.LEATHER)), "not with leather");
        c.eq(EntityEquipmentSlot.HEAD, helmet.armorType, "worn on the head");
        ItemStack s = new ItemStack(scute);
        Crafting.expect(c, Crafting.grid(3, 2, s, s, s, s, ItemStack.EMPTY, s), new ItemStack(helmet), "5 scutes make a shell");

        EntityPlayerMP player = Players.real(world, "turtleshell");
        BlockPos air = c.area();
        player.setPosition(air.getX() + 0.5, air.getY(), air.getZ() + 0.5);
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(helmet));
        TurtleEvents.shellTick(player);
        PotionEffect breathing = player.getActivePotionEffect(MobEffects.WATER_BREATHING);
        c.ok(breathing != null && breathing.getDuration() == 200 && !breathing.doesShowParticles(), "out of water: 10 s of Water Breathing");
        player.removePotionEffect(MobEffects.WATER_BREATHING);
        BlockPos pool = Pools.pool(c, 4);
        player.setPosition(pool.getX() + 3.5, pool.getY() + 1, pool.getZ() + 3.5);
        TurtleEvents.shellTick(player);
        c.ok(player.getActivePotionEffect(MobEffects.WATER_BREATHING) == null, "head underwater: none added");
        player.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        player.setPosition(air.getX() + 0.5, air.getY(), air.getZ() + 0.5);
        TurtleEvents.shellTick(player);
        c.ok(player.getActivePotionEffect(MobEffects.WATER_BREATHING) == null, "another helmet gives none");
    }

    // --- 9. Turtle Master ------------------------------------------------------------------------------

    private static void potions(Check c, WorldServer world) {
        effects(c, TurtleContent.turtleMaster(), 400, 3, 2, "Turtle Master");
        effects(c, TurtleContent.longTurtleMaster(), 800, 3, 2, "long Turtle Master");
        effects(c, TurtleContent.strongTurtleMaster(), 400, 5, 3, "strong Turtle Master");
        c.eq("potion.effect.turtle_master", TurtleContent.longTurtleMaster().getNamePrefixed("potion.effect."), "shared name");
        ItemStack shell = new ItemStack(TurtleContent.turtleHelmet());
        c.ok(BrewingRecipeRegistry.isValidIngredient(shell), "the shell goes in a brewing stand");
        brew(c, Items.POTIONITEM, PotionTypes.AWKWARD, shell, TurtleContent.turtleMaster(), "awkward + shell");
        brew(c, Items.SPLASH_POTION, PotionTypes.AWKWARD, shell, TurtleContent.turtleMaster(), "splash awkward + shell");
        brew(c, Items.POTIONITEM, TurtleContent.turtleMaster(), new ItemStack(Items.REDSTONE), TurtleContent.longTurtleMaster(), "+ redstone");
        brew(c, Items.POTIONITEM, TurtleContent.turtleMaster(), new ItemStack(Items.GLOWSTONE_DUST), TurtleContent.strongTurtleMaster(), "+ glowstone");
        ItemStack water = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER);
        c.ok(BrewingRecipeRegistry.getOutput(water, shell).isEmpty(), "water + shell makes nothing");
        ItemStack splash = BrewingRecipeRegistry.getOutput(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM),
                TurtleContent.turtleMaster()), new ItemStack(Items.GUNPOWDER));
        c.ok(splash.getItem() == Items.SPLASH_POTION && PotionUtils.getPotionFromItem(splash) == TurtleContent.turtleMaster(),
                "+ gunpowder makes it splash");
        ItemStack lingering = PotionUtils.addPotionToItemStack(new ItemStack(Items.LINGERING_POTION), TurtleContent.turtleMaster());
        ItemStack a = new ItemStack(Items.ARROW);
        ItemStack tipped = net.minecraft.item.crafting.CraftingManager.findMatchingResult(
                Crafting.grid(3, 3, a, a, a, a, lingering, a, a, a, a), world);
        c.ok(tipped.getItem() == Items.TIPPED_ARROW && tipped.getCount() == 8
                && PotionUtils.getPotionFromItem(tipped) == TurtleContent.turtleMaster(), "tipped arrows of the Turtle Master");
    }

    private static void effects(Check c, PotionType type, int ticks, int slowness, int resistance, String what) {
        c.ok(type != null && ForgeRegistries.POTION_TYPES.containsValue(type), what + " registered");
        if (type == null) {
            return;
        }
        boolean slow = false, resist = false;
        for (PotionEffect e : type.getEffects()) {
            if (e.getPotion() == MobEffects.SLOWNESS) {
                slow = e.getDuration() == ticks && e.getAmplifier() == slowness;
            } else if (e.getPotion() == MobEffects.RESISTANCE) {
                resist = e.getDuration() == ticks && e.getAmplifier() == resistance;
            }
        }
        c.ok(slow && resist && type.getEffects().size() == 2, what + ": Slowness " + (slowness + 1) + " and Resistance " + (resistance + 1)
                + " for " + ticks / 20 + " s");
    }

    private static void brew(Check c, Item item, PotionType from, ItemStack ingredient, PotionType to, String what) {
        ItemStack out = BrewingRecipeRegistry.getOutput(PotionUtils.addPotionToItemStack(new ItemStack(item), from), ingredient);
        c.ok(out.getItem() == item && PotionUtils.getPotionFromItem(out) == to, what + " -> " + to.getRegistryName() + ", got " + out
                + " " + PotionUtils.getPotionFromItem(out).getRegistryName());
    }

    // --- helpers ---------------------------------------------------------------------------------------

    private static EntityTurtle turtle(WorldServer world, BlockPos at) {
        EntityTurtle t = new EntityTurtle(world);
        t.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        t.setHome(at);
        world.spawnEntity(t);
        return t;
    }

    private static <E extends Entity> E spawn(WorldServer world, E entity, BlockPos at) {
        entity.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0.0F, 0.0F);
        world.spawnEntity(entity);
        return entity;
    }

    private static void kill(WorldServer world, BlockPos around) {
        for (Entity e : world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(around).grow(40))) {
            if (!(e instanceof EntityPlayerMP)) {
                e.setDead();
            }
        }
    }

    private static String debug(EntityTurtle t) {
        StringBuilder b = new StringBuilder();
        b.append(t.getPositionVector()).append(" alive=").append(t.isEntityAlive()).append(" love=").append(t.isInLove())
                .append(" age=").append(t.getGrowingAge()).append(" water=").append(t.isInWater()).append(" ground=").append(t.onGround)
                .append(" path=").append(t.getNavigator().getPath() == null ? "none" : t.getNavigator().getPath().getCurrentPathLength())
                .append(" running=");
        for (net.minecraft.entity.ai.EntityAITasks.EntityAITaskEntry e : t.tasks.taskEntries) {
            if (e.using) {
                b.append(e.action.getClass().getSimpleName()).append(',');
            }
        }
        return b.toString();
    }

    private static int eggs(WorldServer world, BlockPos pos) {
        IBlockState s = world.getBlockState(pos);
        return s.getBlock() == TurtleContent.turtleEgg() ? s.getValue(BlockTurtleEgg.EGGS) : 0;
    }

    private static BlockPos find(WorldServer world, BlockPos corner, BlockTurtleEgg egg) {
        for (BlockPos p : BlockPos.getAllInBox(corner.add(0, -1, 0), corner.add(31, 2, 31))) {
            if (world.getBlockState(p).getBlock() == egg) {
                return p.toImmutable();
            }
        }
        return null;
    }

    private static void place(EntityPlayerMP player, WorldServer world, ItemStack stack, BlockPos on, EnumFacing face) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, world, on, EnumHand.MAIN_HAND, face, 0.5F, 1.0F, 0.5F);
    }

    private static void breakWith(EntityPlayerMP player, BlockPos pos, ItemStack tool) {
        player.setHeldItem(EnumHand.MAIN_HAND, tool);
        player.interactionManager.tryHarvestBlock(pos);
    }
}
