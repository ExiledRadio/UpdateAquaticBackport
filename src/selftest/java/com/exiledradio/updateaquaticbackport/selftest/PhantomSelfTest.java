package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.phantom.EntityPhantom;
import com.exiledradio.updateaquaticbackport.phantom.Insomnia;
import com.exiledradio.updateaquaticbackport.phantom.PhantomContent;

import net.minecraft.block.BlockBed;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.scoreboard.IScoreCriteria;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/** Phantoms, insomnia, the membrane, elytra repair and Slow Falling. */
public final class PhantomSelfTest implements SelfTest {

    private static final double EPS = 1.0E-4D;

    @Override
    public String name() {
        return "phantom";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(PhantomContent.enabled(), "phantom stream on");
        long time = world.getWorldTime();
        try {
            registration(c);
            names(c);
            basics(c, world);
            saving(c, world);
            flying(c, world);
            daylight(c, world);
            targeting(c, world);
            cats(c, world);
            drops(c, world);
            insomniaStat(c, world);
            spawner(c, world);
            slowFalling(c, world);
            brewing(c, world);
            elytra(c, world);
        } finally {
            setTime(world, time);
        }
    }

    // ------------------------------------------------------------------------------------------

    private static void setTime(WorldServer world, long time) {
        world.setWorldTime(time);
        world.setSkylightSubtracted(world.calculateSkylightSubtracted(1.0F));
    }

    private static void night(WorldServer world) {
        setTime(world, 18000L);
    }

    private static void day(WorldServer world) {
        setTime(world, 6000L);
    }

    private static EntityPhantom phantom(WorldServer world, double x, double y, double z) {
        EntityPhantom p = new EntityPhantom(world);
        p.setLocationAndAngles(x, y, z, 0.0F, 0.0F);
        p.onInitialSpawn(world.getDifficultyForLocation(new BlockPos(p)), null);
        world.spawnEntity(p);
        return p;
    }

    /** One world tick for one entity, as the world does it (which also counts its age). */
    private static void tick(Entity e) {
        // The world's own update wants 32 loaded blocks all round the entity; the test areas load less.
        e.lastTickPosX = e.posX;
        e.lastTickPosY = e.posY;
        e.lastTickPosZ = e.posZ;
        e.prevRotationYaw = e.rotationYaw;
        e.prevRotationPitch = e.rotationPitch;
        ++e.ticksExisted;
        e.onUpdate();
    }

    private static EntityPlayerMP player(WorldServer world, String name, BlockPos at) {
        EntityPlayerMP player = Players.real(world, name);
        player.setGameType(GameType.SURVIVAL);
        player.setPositionAndUpdate(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        player.setHealth(player.getMaxHealth());
        // A new player is spared all damage for 3 seconds, counted down by ticks the test never gives it.
        net.minecraftforge.fml.common.ObfuscationReflectionHelper.setPrivateValue(EntityPlayerMP.class, player, 0,
                "respawnInvulnerabilityTicks", "field_147101_bU");
        world.spawnEntity(player);
        return player;
    }

    private static void remove(WorldServer world, Entity... entities) {
        for (Entity e : entities) {
            world.removeEntity(e);
        }
    }

    // ------------------------------------------------------------------------------------------

    private static void registration(Check c) {
        ResourceLocation id = Content.id("phantom");
        c.eq(id, EntityList.getKey(EntityPhantom.class), "phantom registry name");
        c.eq("phantom", EntityList.getKey(EntityPhantom.class).getPath(), "other mods find it as 'phantom'");
        c.ok(EntityList.ENTITY_EGGS.containsKey(id), "phantom spawn egg");
        c.eq(0x43518A, EntityList.ENTITY_EGGS.get(id).primaryColor, "egg primary color");
        c.ok(LootTableList.getAll().contains(PhantomContent.LOOT_TABLE), "loot table registered");
        Item membrane = ForgeRegistries.ITEMS.getValue(Content.id("phantom_membrane"));
        c.ok(membrane != null && membrane == PhantomContent.membrane(), "phantom_membrane item");
        c.eq(PhantomContent.slowFalling(), ForgeRegistries.POTIONS.getValue(Content.id("slow_falling")), "slow_falling effect");
        c.ok(!PhantomContent.slowFalling().isBadEffect(), "slow falling is not a harmful effect");
        c.eq(0xFFEFD1, PhantomContent.slowFalling().getLiquidColor(), "slow falling color");
        PotionType normal = ForgeRegistries.POTION_TYPES.getValue(Content.id("slow_falling"));
        PotionType extended = ForgeRegistries.POTION_TYPES.getValue(Content.id("long_slow_falling"));
        c.ok(normal != null && normal == PhantomContent.slowFallingType(), "slow_falling potion type");
        c.ok(extended != null && extended == PhantomContent.longSlowFallingType(), "long_slow_falling potion type");
        c.eq(1800, normal.getEffects().get(0).getDuration(), "slow falling 1:30");
        c.eq(4800, extended.getEffects().get(0).getDuration(), "long slow falling 4:00");
        for (String s : new String[] {"ambient", "bite", "death", "flap", "hurt", "swoop"}) {
            c.ok(ForgeRegistries.SOUND_EVENTS.containsKey(Content.id("entity.phantom." + s)), "sound entity.phantom." + s);
        }
        c.ok(Insomnia.stat() != null, "time since rest statistic");
        c.ok(IScoreCriteria.INSTANCES.containsKey(Insomnia.STAT_ID), "time since rest is a scoreboard criterion");
    }

    /** Every translation key our content asks for is in en_us.lang. */
    private static void names(Check c) throws Exception {
        Set<String> keys = new HashSet<String>();
        InputStream in = PhantomSelfTest.class.getResourceAsStream("/assets/" + UpdateAquaticBackport.MOD_ID + "/lang/en_us.lang");
        c.ok(in != null, "en_us.lang in the jar");
        if (in == null) {
            return;
        }
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        for (String line; (line = r.readLine()) != null; ) {
            if (line.contains("=") && !line.startsWith("#")) {
                keys.add(line.substring(0, line.indexOf('=')));
            }
        }
        r.close();
        String entity = "entity." + EntityList.getTranslationName(Content.id("phantom")) + ".name";
        c.ok(keys.contains(entity), "entity name " + entity);
        c.ok(keys.contains(PhantomContent.slowFalling().getName()), "effect name " + PhantomContent.slowFalling().getName());
        for (String prefix : new String[] {"potion.effect.", "splash_potion.effect.", "lingering_potion.effect.", "tipped_arrow.effect."}) {
            for (PotionType type : new PotionType[] {PhantomContent.slowFallingType(), PhantomContent.longSlowFallingType()}) {
                String key = type.getNamePrefixed(prefix);
                c.ok(keys.contains(key), "potion name " + key);
            }
        }
        c.ok(keys.contains(Insomnia.STAT_ID), "statistic name");
        c.ok(keys.contains("config.updateaquaticbackport.phantoms.insomnia")
                && keys.contains("config.updateaquaticbackport.phantoms.insomnia.tooltip"), "config name and tooltip");
        for (String s : new String[] {"ambient", "bite", "death", "flap", "hurt", "swoop"}) {
            c.ok(keys.contains("subtitles.updateaquaticbackport.entity.phantom." + s), "subtitle " + s);
        }
    }

    private static void basics(Check c, WorldServer world) {
        BlockPos o = c.area();
        EntityPhantom p = phantom(world, o.getX() + 16, 130, o.getZ() + 16);
        c.eq(0, p.getPhantomSize(), "spawns at size 0");
        c.ok(Math.abs(p.width - 0.9F) < EPS && Math.abs(p.height - 0.5F) < EPS, "0.9 x 0.5 hitbox, got " + p.width + " x " + p.height);
        c.ok(Math.abs(p.getEyeHeight() - 0.175F) < EPS, "eye height 0.35 of height");
        c.eq(6.0D, p.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "attack damage 6");
        c.eq(20.0F, p.getMaxHealth(), "20 health");
        c.eq(EnumCreatureAttribute.UNDEAD, p.getCreatureAttribute(), "undead");
        c.ok(p.isCreatureType(EnumCreatureType.MONSTER, false), "counts as a monster");
        c.ok(new BlockPos(p).up(5).equals(p.anchor()), "anchor 5 above the spawn");

        p.setPhantomSize(5);
        c.ok(Math.abs(p.width - 1.9F) < EPS, "size 5 is 1.9 wide, got " + p.width);
        c.ok(Math.abs(p.height - 0.5F * 1.9F / 0.9F) < EPS, "size 5 height in proportion, got " + p.height);
        c.ok(Math.abs((p.getEntityBoundingBox().maxX - p.getEntityBoundingBox().minX) - 1.9F) < EPS, "size 5 bounding box");
        c.eq(11.0D, p.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "size 5 attack damage 11");
        p.setPhantomSize(200);
        c.eq(64, p.getPhantomSize(), "size capped at 64");
        p.setPhantomSize(-3);
        c.eq(0, p.getPhantomSize(), "size not below 0");

        // Undead: Instant Health hurts it.
        float before = p.getHealth();
        MobEffects.INSTANT_HEALTH.affectEntity(null, null, p, 0, 1.0D);
        c.ok(p.getHealth() < before, "instant health hurts a phantom");

        // Peaceful removes it on its next tick.
        net.minecraft.world.EnumDifficulty difficulty = world.getDifficulty();
        world.getWorldInfo().setDifficulty(net.minecraft.world.EnumDifficulty.PEACEFUL);
        EntityPhantom q = phantom(world, o.getX() + 10, 130, o.getZ() + 10);
        tick(q);
        c.ok(q.isDead, "removed in peaceful");
        world.getWorldInfo().setDifficulty(difficulty);
        remove(world, p, q);
    }

    private static void saving(Check c, WorldServer world) {
        BlockPos o = c.area();
        EntityPhantom p = phantom(world, o.getX() + 16, 130, o.getZ() + 16);
        p.setPhantomSize(7);
        NBTTagCompound tag = new NBTTagCompound();
        c.ok(p.writeToNBTOptional(tag), "writes with its id");
        c.eq(Content.id("phantom").toString(), tag.getString("id"), "saved id");
        c.eq(7, tag.getInteger("Size"), "saved Size");
        c.eq(p.anchor().getY(), tag.getInteger("AY"), "saved anchor");
        Entity back = EntityList.createEntityFromNBT(tag, world);
        c.ok(back instanceof EntityPhantom, "loads as a phantom");
        if (back instanceof EntityPhantom) {
            EntityPhantom b = (EntityPhantom) back;
            c.eq(7, b.getPhantomSize(), "loaded size");
            c.eq(p.anchor(), b.anchor(), "loaded anchor");
            c.ok(Math.abs(b.width - p.width) < EPS, "loaded hitbox follows size");
            c.eq(13.0D, b.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getBaseValue(), "loaded attack damage");
        }
        Entity summoned = EntityList.createEntityByIDFromName(Content.id("phantom"), world);
        c.ok(summoned instanceof EntityPhantom, "summoned by name");
        remove(world, p);
    }

    /** Idle: it circles its anchor in the air, turning and pitching, never landing. */
    private static void flying(Check c, WorldServer world) {
        night(world);
        BlockPos o = c.area();
        double x0 = o.getX() + 16;
        double z0 = o.getZ() + 16;
        EntityPhantom p = phantom(world, x0, 130, z0);
        double minY = 999;
        double maxDist = 0;
        float maxPitch = 0;
        Set<Integer> headings = new HashSet<Integer>();
        for (int i = 0; i < 400; i++) {
            tick(p);
            minY = Math.min(minY, p.posY);
            maxDist = Math.max(maxDist, Math.sqrt((p.posX - x0) * (p.posX - x0) + (p.posZ - z0) * (p.posZ - z0)));
            maxPitch = Math.max(maxPitch, Math.abs(p.rotationPitch));
            headings.add((int) Math.floor(((p.rotationYaw % 360) + 360) % 360 / 90));
        }
        c.ok(!p.isDead, "alive after 400 ticks at night");
        c.ok(minY > 101, "stays in the air, lowest " + minY);
        c.ok(maxDist > 3 && maxDist < 40, "circles near its anchor, farthest " + maxDist);
        c.eq(4, headings.size(), "turns all the way round");
        c.ok(maxPitch > 1.0F, "pitches with its flight (kept past 1.12.2's look helper), max " + maxPitch);
        c.ok(p.getAttackTarget() == null, "no target with no one around");
        remove(world, p);
    }

    private static void daylight(Check c, WorldServer world) {
        BlockPos o = c.area();
        day(world);
        EntityPhantom p = phantom(world, o.getX() + 16, 130, o.getZ() + 16);
        boolean burned = false;
        for (int i = 0; i < 200 && !burned; i++) {
            tick(p);
            burned = p.isBurning();
        }
        c.ok(burned, "burns in daylight under open sky");
        night(world);
        EntityPhantom q = phantom(world, o.getX() + 8, 130, o.getZ() + 8);
        boolean nightBurn = false;
        for (int i = 0; i < 200; i++) {
            tick(q);
            nightBurn |= q.isBurning();
        }
        c.ok(!nightBurn, "does not burn at night");
        remove(world, p, q);
    }

    /** Targets a survival player, circles above, swoops and bites; ignores creative and spectating players. */
    private static void targeting(Check c, WorldServer world) {
        night(world);
        BlockPos o = c.area();
        EntityPlayerMP player = player(world, "phantomprey", o.add(16, 1, 16));
        EntityPhantom p = phantom(world, o.getX() + 12, 125, o.getZ() + 12);
        for (int i = 0; i < 25; i++) {
            tick(p);
        }
        c.eq(player, p.getAttackTarget(), "targets the survival player within 64 blocks");
        boolean swooped = false;
        boolean bit = false;
        for (int i = 0; i < 1200 && !bit; i++) {
            tick(p);
            swooped |= p.swooping();
            if (player.getHealth() < player.getMaxHealth()) {
                bit = true;
            }
        }
        c.ok(swooped, "switches to a swoop");
        c.ok(bit, "bites the player (health " + player.getHealth() + ")");
        if (bit) {
            c.ok(!p.swooping(), "breaks off the swoop after a bite");
            c.ok(player.getRevengeTarget() == p || player.getLastDamageSource() != null, "damage came from the phantom");
        }
        player.setHealth(player.getMaxHealth());

        player.setGameType(GameType.CREATIVE);
        EntityPhantom q = phantom(world, o.getX() + 20, 125, o.getZ() + 20);
        for (int i = 0; i < 100; i++) {
            tick(q);
        }
        c.ok(q.getAttackTarget() == null, "ignores a creative player");
        player.setGameType(GameType.SPECTATOR);
        for (int i = 0; i < 100; i++) {
            tick(q);
        }
        c.ok(q.getAttackTarget() == null, "ignores a spectator");
        player.setGameType(GameType.SURVIVAL);
        remove(world, p, q, player);
    }

    /**
     * 1.14.4: a cat within 16 blocks ends a swoop - checked every 20 ticks of the phantom's age while it swoops,
     * each cat hissing once. 1.12.2's tamed ocelot is its cat; with Village &amp; Pillage installed its cat counts
     * too, and its own generic phantom fear must not add break-offs or hisses of its own.
     */
    private static void cats(Check c, WorldServer world) {
        EntityOcelot ocelot = new EntityOcelot(world);
        ocelot.setTamed(true);
        ocelot.setTameSkin(1);
        swoopEndsAtCat(c, world, "a tamed ocelot", ocelot);
        if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            Entity cat = EntityList.createEntityByIDFromName(new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "cat"), world);
            c.ok(cat instanceof EntityLiving, "Village & Pillage's cat is there to test with");
            if (cat instanceof EntityLiving) {
                swoopEndsAtCat(c, world, "Village & Pillage's cat", (EntityLiving) cat);
            }
        }

        night(world);
        BlockPos o = c.area();
        EntityPlayerMP player = player(world, "phantomwild", o.add(16, 1, 16));
        EntityOcelot wild = new EntityOcelot(world);
        wild.setLocationAndAngles(o.getX() + 18, 100, o.getZ() + 18, 0, 0);
        world.spawnEntity(wild);
        EntityPhantom q = phantom(world, o.getX() + 16, 140, o.getZ() + 16);
        int wildSwoops = 0;
        for (int i = 0; i < 400; i++) {
            tick(q);
            if (q.swooping()) {
                wildSwoops++;
            }
        }
        c.ok(wildSwoops > 0, "a wild ocelot does not stop it swooping");
        remove(world, q, wild, player);
        player.setHealth(player.getMaxHealth());
    }

    private static void swoopEndsAtCat(Check c, WorldServer world, String what, EntityLiving cat) {
        night(world);
        BlockPos o = c.area();
        EntityPlayerMP player = player(world, "phantomcat", o.add(16, 1, 16));
        player.capabilities.disableDamage = false;
        EntityPhantom p = phantom(world, o.getX() + 16, 140, o.getZ() + 16);
        cat.setLocationAndAngles(o.getX() + 18, 100, o.getZ() + 18, 0, 0);
        MobsSelfTest.Sounds sounds = new MobsSelfTest.Sounds();
        boolean swooped = false;
        boolean gaveUp = false;
        boolean swoopingWhenItGaveUp = false;
        int gaveUpAtAge = -1;
        int ticks = 0;
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            for (; ticks < 1200 && !gaveUp; ticks++) {
                if (ticks == 0) {
                    // Let it take its target and start a swoop with no cat about.
                    for (int i = 0; i < 200 && !p.swooping(); i++) {
                        tick(p);
                    }
                    swooped = p.swooping();
                    world.spawnEntity(cat);
                }
                boolean wasSwooping = p.swooping();
                tick(p);
                if (swooped && p.getAttackTarget() == null) {
                    gaveUp = true;
                    swoopingWhenItGaveUp = wasSwooping;
                    gaveUpAtAge = p.ticksExisted;
                }
            }
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
        }
        c.ok(swooped, what + ": swooping before the cat came");
        c.ok(gaveUp, what + ": within 16 blocks, it ends the swoop");
        c.ok(player.getHealth() == player.getMaxHealth() || ticks < 30, what + ": the cat came before the bite");
        c.ok(swoopingWhenItGaveUp && gaveUpAtAge % 20 == 0,
                what + ": the swoop ends on a 20th tick of its age, as 1.14.4's check runs (age " + gaveUpAtAge + ")");
        c.eq(1, sounds.count(SoundEvents.ENTITY_CAT_HISS), what + ": the cat hisses once");
        c.ok(!p.swooping(), what + ": the phantom goes back to circling");
        remove(world, p, cat, player);
        player.setHealth(player.getMaxHealth());
    }

    /** Membrane 0-1, only for a player's kill, Looting adding up to one per level. */
    private static void drops(Check c, WorldServer world) {
        night(world);
        BlockPos o = c.area();
        EntityPlayerMP player = player(world, "phantomhunter", o.add(2, 0, 2));
        Item membrane = PhantomContent.membrane();
        int plain = kills(world, o, player, membrane, ItemStack.EMPTY, 200);
        c.ok(plain >= 70 && plain <= 130, "about half a membrane per kill: " + plain + " from 200");
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.addEnchantment(Enchantments.LOOTING, 3);
        int looted = kills(world, o, player, membrane, sword, 200);
        c.ok(looted >= 300 && looted <= 500, "Looting III gives about 2 per kill: " + looted + " from 200");
        int none = kills(world, o, null, membrane, ItemStack.EMPTY, 100);
        c.eq(0, none, "nothing without a player's kill");
        remove(world, player);
    }

    private static int kills(WorldServer world, BlockPos o, EntityPlayerMP player, Item want, ItemStack weapon, int n) {
        if (player != null) {
            player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, weapon.copy());
        }
        int total = 0;
        BlockPos at = o.add(20, 30, 20);
        for (int i = 0; i < n; i++) {
            EntityPhantom p = phantom(world, at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            p.attackEntityFrom(player != null ? DamageSource.causePlayerDamage(player) : DamageSource.GENERIC, 1000.0F);
            total += Drops.count(world, new BlockPos(p), want);
            world.removeEntity(p);
            for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(4))) {
                e.setDead();
            }
        }
        return total;
    }

    private static void insomniaStat(Check c, WorldServer world) {
        night(world);
        BlockPos o = c.area();
        c.ok(world.getGameRules().hasRule(Insomnia.GAME_RULE), "doInsomnia game rule added");
        c.ok(world.getGameRules().getBoolean(Insomnia.GAME_RULE), "doInsomnia on by default");
        EntityPlayerMP player = player(world, "phantomsleeper", o.add(8, 0, 8));
        player.takeStat(Insomnia.stat());
        for (int i = 0; i < 50; i++) {
            MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        }
        c.eq(50, Insomnia.timeSinceRest(player), "one per tick awake");

        // Into a bed at night: cleared, and held at zero while in bed.
        BlockPos foot = o.add(10, 0, 8);
        world.setBlockState(foot, Blocks.BED.getDefaultState().withProperty(BlockBed.FACING, EnumFacing.EAST)
                .withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT), 2);
        world.setBlockState(foot.east(), Blocks.BED.getDefaultState().withProperty(BlockBed.FACING, EnumFacing.EAST)
                .withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 2);
        if (world.getTileEntity(foot) instanceof net.minecraft.tileentity.TileEntityBed) {
            ((net.minecraft.tileentity.TileEntityBed) world.getTileEntity(foot)).setColor(EnumDyeColor.RED);
        }
        player.setPositionAndUpdate(foot.getX() + 0.5, foot.getY(), foot.getZ() - 0.5);
        net.minecraft.entity.player.EntityPlayer.SleepResult result = player.trySleep(foot.east());
        c.eq(net.minecraft.entity.player.EntityPlayer.SleepResult.OK, result, "gets into bed at night");
        if (result == net.minecraft.entity.player.EntityPlayer.SleepResult.OK) {
            for (int i = 0; i < 5; i++) {
                MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
            }
            c.eq(0, Insomnia.timeSinceRest(player), "cleared in bed");
            player.wakeUpPlayer(true, true, false);
        }
        for (int i = 0; i < 7; i++) {
            MinecraftForge.EVENT_BUS.post(new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player));
        }
        c.eq(7, Insomnia.timeSinceRest(player), "counts again after waking");
        MinecraftForge.EVENT_BUS.post(new LivingDeathEvent(player, DamageSource.GENERIC));
        c.eq(0, Insomnia.timeSinceRest(player), "cleared by dying");
        remove(world, player);
        world.setBlockToAir(foot.east());
        world.setBlockToAir(foot);
    }

    private static void spawner(Check c, WorldServer world) {
        night(world);
        BlockPos o = c.area();
        BlockPos stand = o.add(16, 0, 16);
        EntityPlayerMP player = player(world, "phantominsomniac", stand);
        Random random = new Random(1234L);

        // Rested: never.
        player.getStatFile().unlockAchievement(player, Insomnia.stat(), 1000);
        int rested = 0;
        for (int i = 0; i < 300; i++) {
            rested += Insomnia.trySpawnOver(world, player, random);
        }
        c.eq(0, rested, "no phantoms for a rested player");

        // A week awake: often, in groups, 20-34 above and at most 10 to the side.
        player.getStatFile().unlockAchievement(player, Insomnia.stat(), 7 * 24000);
        int spawned = 0;
        int groups = 0;
        boolean placed = true;
        int largest = 0;
        for (int i = 0; i < 300; i++) {
            int n = Insomnia.trySpawnOver(world, player, random);
            if (n > 0) {
                groups++;
                largest = Math.max(largest, n);
                spawned += n;
            }
        }
        List<EntityPhantom> found = world.getEntitiesWithinAABB(EntityPhantom.class, new AxisAlignedBB(stand).grow(16, 40, 16));
        for (EntityPhantom p : found) {
            double dy = p.posY - stand.getY();
            placed &= dy >= 20 && dy <= 34 && Math.abs(p.posX - 0.5 - stand.getX()) <= 10.01 && Math.abs(p.posZ - 0.5 - stand.getZ()) <= 10.01;
            placed &= p.getPhantomSize() == 0;
        }
        c.ok(groups > 10, "an awake player gets phantoms: " + groups + " groups in 300 rolls");
        c.eq(spawned, found.size(), "every phantom counted is in the world");
        c.ok(placed, "all 20-34 above the player, 10 at most to the side, size 0");
        c.ok(largest <= world.getDifficulty().getId() + 1, "groups of at most difficulty + 1, largest " + largest);
        kill(found);

        // Under a roof: none.
        c.fill(stand.add(-1, 3, -1), stand.add(1, 3, 1), Blocks.STONE.getDefaultState());
        world.checkLight(stand);
        int roofed = 0;
        for (int i = 0; i < 200; i++) {
            roofed += Insomnia.trySpawnOver(world, player, random);
        }
        c.eq(0, roofed, "none under a roof");
        c.fill(stand.add(-1, 3, -1), stand.add(1, 3, 1), Blocks.AIR.getDefaultState());
        world.checkLight(stand);

        // Below sea level: none.
        player.setPositionAndUpdate(stand.getX() + 0.5, world.getSeaLevel() - 2, stand.getZ() + 0.5);
        int deep = 0;
        for (int i = 0; i < 200; i++) {
            deep += Insomnia.trySpawnOver(world, player, random);
        }
        c.eq(0, deep, "none below sea level");
        player.setPositionAndUpdate(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);

        // The world tick: day, spectators and the game rule.
        day(world);
        Insomnia.attemptNextTick();
        c.eq(0, Insomnia.tick(world), "no attempt in daytime");
        night(world);
        int fromTick = 0;
        for (int i = 0; i < 30 && fromTick == 0; i++) {
            Insomnia.attemptNextTick();
            fromTick = Insomnia.tick(world);
        }
        c.ok(fromTick > 0, "the world tick spawns at night");
        kill(world.getEntitiesWithinAABB(EntityPhantom.class, new AxisAlignedBB(stand).grow(16, 40, 16)));
        player.setGameType(GameType.SPECTATOR);
        int spectating = 0;
        for (int i = 0; i < 30; i++) {
            Insomnia.attemptNextTick();
            spectating += Insomnia.tick(world);
        }
        c.eq(0, spectating, "none over a spectator");
        player.setGameType(GameType.SURVIVAL);
        world.getGameRules().setOrCreateGameRule(Insomnia.GAME_RULE, "false");
        int ruled = 0;
        for (int i = 0; i < 30; i++) {
            Insomnia.attemptNextTick();
            ruled += Insomnia.tick(world);
        }
        c.eq(0, ruled, "none with doInsomnia false");
        world.getGameRules().setOrCreateGameRule(Insomnia.GAME_RULE, "true");
        kill(world.getEntitiesWithinAABB(EntityPhantom.class, new AxisAlignedBB(stand).grow(16, 40, 16)));
        player.takeStat(Insomnia.stat());
        remove(world, player);
    }

    private static void kill(List<EntityPhantom> phantoms) {
        for (EntityPhantom p : phantoms) {
            p.world.removeEntity(p);
        }
    }

    /** Falls exactly as 1.14 does with gravity 0.01, takes no fall damage; without it, a pig falls as before. */
    private static void slowFalling(Check c, WorldServer world) {
        BlockPos o = c.area();
        EntityPig slow = pig(world, o.add(8, 50, 8));
        slow.addPotionEffect(new PotionEffect(PhantomContent.slowFalling(), 400));
        EntityPig plain = pig(world, o.add(20, 50, 20));
        double startY = slow.posY;
        double v = 0;
        double y = startY;
        for (int i = 0; i < 20; i++) {
            tick(slow);
            tick(plain);
            // 1.14: move by the motion, then pull by 0.01 (motion at or below zero) and drag.
            y += v;
            v = (v - 0.01D) * 0.9800000190734863D;
        }
        c.ok(Math.abs(slow.posY - y) < 1.0E-3D, "falls as 1.14 with Slow Falling: " + slow.posY + " vs " + y);
        c.ok(plain.posY < slow.posY - 5, "falls faster without it: " + plain.posY);
        // Cleared every tick, as 1.14 clears it before each move: at most one tick's drop is ever counted.
        c.ok(slow.fallDistance < 0.6F, "no fall distance builds up, got " + slow.fallDistance);

        // Landing from 50 blocks: unhurt; a plain pig from 50 dies or is hurt.
        float health = slow.getHealth();
        for (int i = 0; i < 400 && !slow.onGround; i++) {
            tick(slow);
        }
        for (int i = 0; i < 400 && !plain.onGround && !plain.isDead; i++) {
            tick(plain);
        }
        c.ok(slow.onGround, "lands");
        c.eq(health, slow.getHealth(), "no fall damage");
        c.ok(plain.getHealth() < plain.getMaxHealth() || plain.isDead, "a plain pig is hurt by the same fall");

        // In water it is left to vanilla.
        BlockPos pool = o.add(4, 0, 24);
        c.fill(pool, pool.add(2, 3, 2), Blocks.WATER.getDefaultState());
        EntityPig swimmer = pig(world, pool.add(1, 2, 1));
        swimmer.addPotionEffect(new PotionEffect(PhantomContent.slowFalling(), 400));
        tick(swimmer);
        tick(swimmer);
        c.ok(swimmer.isInWater(), "pig in water");
        remove(world, slow, plain, swimmer);
    }

    private static EntityPig pig(WorldServer world, BlockPos at) {
        EntityPig pig = new EntityPig(world);
        pig.setLocationAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 0, 0);
        world.spawnEntity(pig);
        return pig;
    }

    private static void brewing(Check c, WorldServer world) {
        ItemStack membrane = new ItemStack(PhantomContent.membrane());
        for (Item bottle : new Item[] {Items.POTIONITEM, Items.SPLASH_POTION, Items.LINGERING_POTION}) {
            ItemStack awkward = PotionUtils.addPotionToItemStack(new ItemStack(bottle), PotionTypes.AWKWARD);
            ItemStack out = BrewingRecipeRegistry.getOutput(awkward, membrane);
            c.ok(out.getItem() == bottle && PotionUtils.getPotionFromItem(out) == PhantomContent.slowFallingType(),
                    "awkward + membrane = slow falling (" + bottle.getRegistryName() + ")");
            ItemStack longer = BrewingRecipeRegistry.getOutput(out, new ItemStack(Items.REDSTONE));
            c.ok(longer.getItem() == bottle && PotionUtils.getPotionFromItem(longer) == PhantomContent.longSlowFallingType(),
                    "slow falling + redstone = long (" + bottle.getRegistryName() + ")");
        }
        ItemStack plainPotion = PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PhantomContent.slowFallingType());
        ItemStack splash = BrewingRecipeRegistry.getOutput(plainPotion, new ItemStack(Items.GUNPOWDER));
        c.ok(splash.getItem() == Items.SPLASH_POTION && PotionUtils.getPotionFromItem(splash) == PhantomContent.slowFallingType(),
                "gunpowder makes it splash");
        ItemStack lingering = BrewingRecipeRegistry.getOutput(splash, new ItemStack(Items.DRAGON_BREATH));
        c.ok(lingering.getItem() == Items.LINGERING_POTION, "dragon's breath makes it lingering");
        c.ok(BrewingRecipeRegistry.getOutput(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER), membrane).isEmpty(),
                "water + membrane does nothing");

        // Tipped arrows: 8 arrows round a lingering potion.
        ItemStack arrow = new ItemStack(Items.ARROW);
        ItemStack lp = PotionUtils.addPotionToItemStack(new ItemStack(Items.LINGERING_POTION), PhantomContent.slowFallingType());
        ItemStack got = CraftingManager.findMatchingResult(Crafting.grid(3, 3, arrow, arrow, arrow, arrow, lp, arrow, arrow, arrow, arrow), world);
        c.ok(got.getItem() == Items.TIPPED_ARROW && got.getCount() == 8 && PotionUtils.getPotionFromItem(got) == PhantomContent.slowFallingType(),
                "8 arrows of slow falling, got " + got);
    }

    private static void elytra(Check c, WorldServer world) {
        ItemStack worn = new ItemStack(Items.ELYTRA);
        worn.setItemDamage(300);
        AnvilUpdateEvent one = anvil(worn, new ItemStack(PhantomContent.membrane(), 1), "");
        c.ok(!one.isCanceled() && one.getOutput().getItem() == Items.ELYTRA, "membrane repairs the elytra");
        c.eq(300 - 432 / 4, one.getOutput().getItemDamage(), "one membrane mends a quarter");
        c.eq(1, one.getCost(), "one level for one membrane");
        c.eq(1, one.getMaterialCost(), "uses one membrane");
        c.eq(1, one.getOutput().getRepairCost(), "prior-work penalty 1 after");
        AnvilUpdateEvent many = anvil(worn, new ItemStack(PhantomContent.membrane(), 10), "Wings");
        c.eq(0, many.getOutput().getItemDamage(), "enough membrane mends it fully");
        c.eq(3, many.getMaterialCost(), "uses only the three it needs");
        c.eq(4, many.getCost(), "three levels plus one for the name");
        c.eq("Wings", many.getOutput().getDisplayName(), "renamed");
        AnvilUpdateEvent leather = anvil(worn, new ItemStack(Items.LEATHER, 4), "");
        c.ok(leather.isCanceled(), "leather no longer repairs the elytra");
        AnvilUpdateEvent whole = anvil(new ItemStack(Items.ELYTRA), new ItemStack(PhantomContent.membrane()), "");
        c.ok(whole.isCanceled(), "nothing to mend on an undamaged elytra");
        ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.setItemDamage(100);
        AnvilUpdateEvent other = anvil(sword, new ItemStack(PhantomContent.membrane()), "");
        c.ok(!other.isCanceled() && other.getOutput().isEmpty(), "membrane leaves other items to vanilla");

        // Through a real anvil container.
        EntityPlayerMP player = Players.real(world, "phantomsmith");
        player.experienceLevel = 30;
        ContainerRepair anvilMenu = new ContainerRepair(player.inventory, world, BlockPos.ORIGIN, player);
        // Open for a listed player, so a refusal can find the anvil to clear.
        player.openContainer = anvilMenu;
        List<EntityPlayerMP> listed = world.getMinecraftServer().getPlayerList().getPlayers();
        listed.add(player);
        anvilMenu.getSlot(0).putStack(worn.copy());
        anvilMenu.getSlot(1).putStack(new ItemStack(PhantomContent.membrane(), 2));
        ItemStack result = anvilMenu.getSlot(2).getStack();
        c.ok(result.getItem() == Items.ELYTRA && result.getItemDamage() == 300 - 2 * 108, "the anvil shows the repair, got " + result);
        c.eq(2, anvilMenu.maximumCost, "the anvil asks 2 levels");
        // Leather swapped straight in for the membrane: the membrane's result must not stay to be taken.
        anvilMenu.getSlot(1).putStack(new ItemStack(Items.LEATHER, 2));
        c.ok(anvilMenu.getSlot(2).getStack().isEmpty(), "the anvil shows nothing for leather, even swapped in");
        c.eq(0, anvilMenu.materialCost, "and asks for no leather");
        listed.remove(player);
        player.openContainer = player.inventoryContainer;
        c.ok(EnchantmentHelper.getEnchantments(result).isEmpty(), "no stray enchantments");
    }

    private static AnvilUpdateEvent anvil(ItemStack left, ItemStack right, String name) {
        AnvilUpdateEvent event = new AnvilUpdateEvent(left, right, name, 0);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }
}
