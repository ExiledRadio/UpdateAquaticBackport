package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.trident.Channeling;
import com.exiledradio.updateaquaticbackport.trident.DrownedConversion;
import com.exiledradio.updateaquaticbackport.trident.DrownedSpawning;
import com.exiledradio.updateaquaticbackport.trident.EnchantmentTrident;
import com.exiledradio.updateaquaticbackport.trident.EntityDrowned;
import com.exiledradio.updateaquaticbackport.trident.EntityTrident;
import com.exiledradio.updateaquaticbackport.trident.Impaling;
import com.exiledradio.updateaquaticbackport.trident.ItemTrident;
import com.exiledradio.updateaquaticbackport.trident.PacketDrowning;
import com.exiledradio.updateaquaticbackport.trident.PacketSpin;
import com.exiledradio.updateaquaticbackport.trident.Riptide;
import com.exiledradio.updateaquaticbackport.trident.TridentContent;
import com.exiledradio.updateaquaticbackport.trident.TridentSounds;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import net.minecraft.advancements.Advancement;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAISwimming;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.monster.EntityHusk;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.monster.EntityZombieVillager;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import com.google.common.collect.Multimap;

import java.util.List;
import java.util.Random;

/** The trident, its enchantments, the thrown trident, the drowned and zombies drowning. */
public final class TridentSelfTest implements SelfTest {

    @Override
    public String name() {
        return "trident";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(TridentContent.enabled(), "trident stream on");
        registration(c);
        throwing(c);
        hitting(c);
        channeling(c);
        sticking(c);
        loyalty(c);
        riptide(c);
        melee(c);
        drowned(c);
        spawning(c);
        conversion(c);
        packets(c);
    }

    // --------------------------------------------------------------------------------------------

    private static ItemStack trident() {
        return new ItemStack(TridentContent.trident());
    }

    private static ItemStack trident(Enchantment enchantment, int level) {
        ItemStack stack = trident();
        stack.addEnchantment(enchantment, level);
        return stack;
    }

    private static EntityPlayerMP player(Check c, String name, BlockPos at) {
        EntityPlayerMP player = Players.real(c.world, name);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.setPositionAndRotation(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, 0.0F, 0.0F);
        player.inventory.clear();
        return player;
    }

    /** A trident thrown by this player straight at a point, no spread. */
    private static EntityTrident throwAt(Check c, EntityLivingBase thrower, ItemStack stack, Vec3d target) {
        EntityTrident t = new EntityTrident(c.world, thrower, stack);
        t.shoot(target.x - t.posX, target.y - t.posY, target.z - t.posZ, 2.5F, 0.0F);
        c.world.spawnEntity(t);
        return t;
    }

    private static Vec3d middle(Entity e) {
        return new Vec3d(e.posX, e.posY + e.height / 2.0D, e.posZ);
    }

    /** Ticks a trident until it has hit something or stuck, at most 60 ticks. */
    private static void fly(EntityTrident t) {
        for (int i = 0; i < 60 && !t.isDead && !t.dealtDamage() && !t.isInGround(); i++) {
            t.onUpdate();
        }
    }

    private static <T extends Entity> T spawn(Check c, T entity, Vec3d at) {
        entity.setLocationAndAngles(at.x, at.y, at.z, 0.0F, 0.0F);
        c.world.spawnEntity(entity);
        return entity;
    }

    private static void clear(Check c, BlockPos corner) {
        for (Entity e : c.world.getEntitiesWithinAABB(Entity.class, new AxisAlignedBB(corner.add(-4, -60, -4), corner.add(36, 60, 36)))) {
            if (!(e instanceof EntityPlayerMP)) {
                e.setDead();
            }
        }
    }

    private static boolean done(EntityPlayerMP player, String advancement) {
        Advancement adv = player.getServer().getAdvancementManager().getAdvancement(Content.id(advancement));
        return adv != null && player.getAdvancements().getProgress(adv).isDone();
    }

    private static void readyToSwing(EntityPlayerMP player, ItemStack held) {
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        player.getAttributeMap().applyAttributeModifiers(held.getAttributeModifiers(EntityEquipmentSlot.MAINHAND));
        ObfuscationReflectionHelper.setPrivateValue(EntityLivingBase.class, player, 100, "field_184617_aD");
    }

    // --------------------------------------------------------------------------------------------

    private void registration(Check c) {
        ItemTrident item = TridentContent.trident();
        c.ok(item != null && Content.id("trident").equals(item.getRegistryName()), "item trident");
        c.eq(250, item.getMaxDamage(), "trident durability");
        c.eq(1, item.getItemStackLimit(trident()), "trident stack size");
        c.eq(1, item.getItemEnchantability(), "trident enchantability");
        c.ok(Content.itemModels().containsKey(item), "trident model request");
        Multimap<String, AttributeModifier> mods = trident().getAttributeModifiers(EntityEquipmentSlot.MAINHAND);
        double damage = 1.0D;
        double speed = 4.0D;
        for (AttributeModifier m : mods.get(SharedMonsterAttributes.ATTACK_DAMAGE.getName())) {
            damage += m.getAmount();
        }
        for (AttributeModifier m : mods.get(SharedMonsterAttributes.ATTACK_SPEED.getName())) {
            speed += m.getAmount();
        }
        c.eq(9.0D, damage, "melee damage");
        c.ok(Math.abs(speed - 1.1D) < 1e-6, "attack speed 1.1, got " + speed);
        c.ok(trident().getAttributeModifiers(EntityEquipmentSlot.OFFHAND).isEmpty(), "no off-hand modifiers");

        EnchantmentTrident[] all = {TridentContent.loyalty(), TridentContent.riptide(), TridentContent.impaling(), TridentContent.channeling()};
        String[] names = {"loyalty", "riptide", "impaling", "channeling"};
        int[] max = {3, 3, 5, 1};
        Enchantment.Rarity[] rarity = {Enchantment.Rarity.UNCOMMON, Enchantment.Rarity.RARE, Enchantment.Rarity.RARE, Enchantment.Rarity.VERY_RARE};
        for (int i = 0; i < all.length; i++) {
            Enchantment e = ForgeRegistries.ENCHANTMENTS.getValue(Content.id(names[i]));
            c.ok(e == all[i], "enchantment " + names[i]);
            c.eq(max[i], all[i].getMaxLevel(), names[i] + " max level");
            c.eq(rarity[i], all[i].getRarity(), names[i] + " rarity");
            c.ok(all[i].canApply(trident()), names[i] + " applies to the trident");
            c.ok(!all[i].canApply(new ItemStack(Items.DIAMOND_SWORD)), names[i] + " not on a sword");
            c.eq("enchantment.updateaquaticbackport." + names[i], all[i].getName(), names[i] + " name key");
        }
        c.ok(!TridentContent.riptide().isCompatibleWith(TridentContent.loyalty()), "riptide excludes loyalty");
        c.ok(!TridentContent.loyalty().isCompatibleWith(TridentContent.riptide()), "loyalty excludes riptide");
        c.ok(!TridentContent.riptide().isCompatibleWith(TridentContent.channeling()), "riptide excludes channeling");
        c.ok(TridentContent.loyalty().isCompatibleWith(TridentContent.channeling()), "loyalty with channeling");
        c.ok(TridentContent.impaling().isCompatibleWith(TridentContent.riptide()), "impaling with riptide");
        c.ok(Enchantments.UNBREAKING.canApply(trident()) && Enchantments.MENDING.canApply(trident()), "unbreaking and mending apply");
        c.ok(!Enchantments.SHARPNESS.canApply(trident()), "no sharpness on a trident");
        c.eq(26, TridentContent.loyalty().getMinEnchantability(3), "loyalty III min cost");
        c.eq(33, TridentContent.impaling().getMinEnchantability(5), "impaling V min cost");
        c.eq(53, TridentContent.impaling().getMaxEnchantability(5), "impaling V max cost");
        c.eq(25, TridentContent.channeling().getMinEnchantability(1), "channeling min cost");

        Random rand = new Random(7);
        int ours = 0;
        boolean sharp = false;
        for (int i = 0; i < 300; i++) {
            for (EnchantmentData d : EnchantmentHelper.buildEnchantmentList(rand, trident(), 30, false)) {
                ours += d.enchantment instanceof EnchantmentTrident ? 1 : 0;
                sharp |= d.enchantment == Enchantments.SHARPNESS;
            }
        }
        c.ok(ours > 100, "enchanting table gives trident enchantments: " + ours);
        c.ok(!sharp, "enchanting table never gives sharpness");

        c.eq(Content.id("trident"), EntityList.getKey(EntityTrident.class), "trident entity id");
        c.eq(Content.id("drowned"), EntityList.getKey(EntityDrowned.class), "drowned entity id");
        c.eq(TridentContent.TRIDENT_ENTITY_ID, EntityRegistry.instance().lookupModSpawn(EntityTrident.class, false).getModEntityId(), "trident network id");
        c.eq(TridentContent.DROWNED_ENTITY_ID, EntityRegistry.instance().lookupModSpawn(EntityDrowned.class, false).getModEntityId(), "drowned network id");
        c.ok(EntityList.ENTITY_EGGS.containsKey(Content.id("drowned")), "drowned spawn egg");
        c.eq("entity.updateaquaticbackport.drowned.name", "entity." + EntityList.getTranslationName(Content.id("drowned")) + ".name", "drowned name key");
        for (net.minecraft.util.SoundEvent s : TridentSounds.all()) {
            c.ok(ForgeRegistries.SOUND_EVENTS.getValue(s.getRegistryName()) == s, "sound " + s.getRegistryName());
        }
        DamageSource source = TridentContent.tridentDamage(new EntityPig(c.world), new EntityPig(c.world));
        c.ok("trident".equals(source.getDamageType()) && source.isProjectile(), "trident damage source");
        c.ok(LootTableList.getAll().contains(EntityDrowned.LOOT_TABLE), "drowned loot table registered");
        c.eq(EntityLiving.SpawnPlacementType.IN_WATER, EntitySpawnPlacementRegistry.getPlacementForEntity(EntityDrowned.class), "drowned spawn in water");
        c.eq(DrownedSpawning.OCEAN_WEIGHT, weight(Biomes.OCEAN), "ocean drowned weight");
        c.eq(DrownedSpawning.OCEAN_WEIGHT, weight(Biomes.DEEP_OCEAN), "deep ocean drowned weight");
        c.eq(DrownedSpawning.OCEAN_WEIGHT, weight(Biomes.FROZEN_OCEAN), "frozen ocean drowned weight");
        c.eq(DrownedSpawning.RIVER_WEIGHT, weight(Biomes.RIVER), "river drowned weight");
        c.eq(DrownedSpawning.FROZEN_RIVER_WEIGHT, weight(Biomes.FROZEN_RIVER), "frozen river drowned weight");
        c.eq(0, weight(Biomes.PLAINS), "no drowned on plains");
        c.eq(0, weight(Biomes.BEACH), "no drowned on beaches");
        c.ok(Impaling.isAquatic(new EntitySquid(c.world)) && Impaling.isAquatic(new EntityGuardian(c.world)), "squid and guardians are aquatic");
        c.ok(!Impaling.isAquatic(new EntityPig(c.world)) && !Impaling.isAquatic(new EntityDrowned(c.world)), "pigs and drowned are not aquatic");
    }

    private static int weight(Biome biome) {
        int w = 0;
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.MONSTER)) {
            if (e.entityClass == EntityDrowned.class) {
                w += e.itemWeight;
                if (e.minGroupCount != 1 || e.maxGroupCount != 1) {
                    return -1;
                }
            }
        }
        return w;
    }

    private void throwing(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        ItemTrident item = TridentContent.trident();
        EntityPlayerMP player = player(c, "thrower", corner.add(5, 0, 5));
        ItemStack stack = trident();
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        ActionResult<ItemStack> use = item.onItemRightClick(world, player, EnumHand.MAIN_HAND);
        c.eq(EnumActionResult.SUCCESS, use.getType(), "trident use starts");
        c.ok(player.isHandActive(), "hand active while aiming");
        player.resetActiveHand();

        item.onPlayerStoppedUsing(stack, world, player, 72000 - 9);
        AxisAlignedBB box = new AxisAlignedBB(corner, corner.add(32, 20, 32));
        c.eq(0, world.getEntitiesWithinAABB(EntityTrident.class, box).size(), "9 ticks: not thrown");
        c.ok(player.getHeldItemMainhand() == stack, "9 ticks: still held");

        item.onPlayerStoppedUsing(stack, world, player, 72000 - 10);
        List<EntityTrident> thrown = world.getEntitiesWithinAABB(EntityTrident.class, box);
        c.eq(1, thrown.size(), "10 ticks: thrown");
        c.ok(player.getHeldItemMainhand().isEmpty(), "thrown trident leaves the hand");
        if (!thrown.isEmpty()) {
            EntityTrident t = thrown.get(0);
            c.eq(1, t.thrownStack().getItemDamage(), "throwing costs 1 durability");
            c.ok(t.getOwner() == player, "thrower is the owner");
            c.eq(EntityArrow.PickupStatus.ALLOWED, t.pickupStatus, "player's trident can be picked up");
            c.ok(t.motionZ > 2.0D && Math.abs(t.motionX) < 0.3D, "thrown along the look at about 2.5 per tick: " + t.motionZ);
        }

        ItemStack worn = trident();
        worn.setItemDamage(249);
        player.setHeldItem(EnumHand.MAIN_HAND, worn);
        c.eq(EnumActionResult.FAIL, item.onItemRightClick(world, player, EnumHand.MAIN_HAND).getType(), "a trident one use from breaking cannot be used");
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);

        player.interactionManager.setGameType(GameType.CREATIVE);
        ItemStack creative = trident();
        player.setHeldItem(EnumHand.MAIN_HAND, creative);
        item.onPlayerStoppedUsing(creative, world, player, 72000 - 20);
        c.ok(player.getHeldItemMainhand() == creative, "creative keeps the trident");
        boolean creativeOnly = false;
        for (EntityTrident t : world.getEntitiesWithinAABB(EntityTrident.class, box)) {
            creativeOnly |= t.pickupStatus == EntityArrow.PickupStatus.CREATIVE_ONLY;
        }
        c.ok(creativeOnly, "creative throw is creative-only pickup");
        clear(c, corner);
    }

    private void hitting(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        EntityPlayerMP player = player(c, "hitter", corner.add(5, 0, 5));
        EntityPig pig = spawn(c, new EntityPig(world), new Vec3d(corner.getX() + 5.5D, corner.getY(), corner.getZ() + 11.5D));
        EntityTrident t = throwAt(c, player, trident(), middle(pig));
        fly(t);
        c.ok(t.dealtDamage(), "trident hits a pig");
        c.eq(2.0F, pig.getHealth(), "thrown trident deals 8");
        c.ok(done(player, "adventure/throw_trident"), "A Throwaway Joke granted");
        c.ok(t.motionZ < 0.1D, "it bounces off");
        for (int i = 0; i < 40; i++) {
            t.onUpdate();
        }
        c.eq(2.0F, pig.getHealth(), "no second hit after bouncing");

        EntitySquid squid = spawn(c, new EntitySquid(world), new Vec3d(corner.getX() + 8.5D, corner.getY(), corner.getZ() + 11.5D));
        EntityTrident impaled = throwAt(c, player, trident(TridentContent.impaling(), 5), middle(squid));
        fly(impaled);
        c.ok(squid.getHealth() <= 0.0F, "Impaling V thrown kills a squid (8 + 12.5)");
        EntityPig pig2 = spawn(c, new EntityPig(world), new Vec3d(corner.getX() + 2.5D, corner.getY(), corner.getZ() + 11.5D));
        EntityTrident notAquatic = throwAt(c, player, trident(TridentContent.impaling(), 5), middle(pig2));
        fly(notAquatic);
        c.eq(2.0F, pig2.getHealth(), "Impaling adds nothing against a pig");
        clear(c, corner);
    }

    private void channeling(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        EntityPlayerMP player = player(c, "caster", corner.add(5, 0, 5));
        float rain = world.rainingStrength;
        float prevRain = world.prevRainingStrength;
        float thunder = world.thunderingStrength;
        float prevThunder = world.prevThunderingStrength;
        try {
            // Clear weather: no lightning.
            world.rainingStrength = world.prevRainingStrength = 0.0F;
            world.thunderingStrength = world.prevThunderingStrength = 0.0F;
            EntityVillager calm = spawn(c, new EntityVillager(world), new Vec3d(corner.getX() + 5.5D, corner.getY(), corner.getZ() + 11.5D));
            int before = world.weatherEffects.size();
            fly(throwAt(c, player, trident(TridentContent.channeling(), 1), middle(calm)));
            c.eq(before, world.weatherEffects.size(), "no lightning without a thunderstorm");

            world.rainingStrength = world.prevRainingStrength = 1.0F;
            world.thunderingStrength = world.prevThunderingStrength = 1.0F;
            c.ok(world.isThundering(), "thunderstorm set up");
            EntityVillager plain = spawn(c, new EntityVillager(world), new Vec3d(corner.getX() + 20.5D, corner.getY(), corner.getZ() + 11.5D));
            EntityPlayerMP other = player(c, "nochannel", corner.add(20, 0, 5));
            before = world.weatherEffects.size();
            fly(throwAt(c, other, trident(), middle(plain)));
            c.eq(before, world.weatherEffects.size(), "no lightning without Channeling");

            BlockPos roofed = corner.add(12, 0, 11);
            c.fill(roofed.add(-1, 3, -1), roofed.add(1, 3, 1), Blocks.STONE.getDefaultState());
            EntityVillager under = spawn(c, new EntityVillager(world), new Vec3d(roofed.getX() + 0.5D, roofed.getY(), roofed.getZ() + 0.5D));
            EntityPlayerMP third = player(c, "roofed", corner.add(12, 0, 5));
            fly(throwAt(c, third, trident(TridentContent.channeling(), 1), middle(under)));
            c.eq(before, world.weatherEffects.size(), "no lightning under a roof");

            EntityVillager target = spawn(c, new EntityVillager(world), new Vec3d(corner.getX() + 28.5D, corner.getY(), corner.getZ() + 11.5D));
            EntityPlayerMP fourth = player(c, "striker", corner.add(28, 0, 5));
            fly(throwAt(c, fourth, trident(TridentContent.channeling(), 1), middle(target)));
            EntityLightningBolt bolt = null;
            for (Entity e : world.weatherEffects) {
                if (e instanceof EntityLightningBolt && Channeling.caster(e) == fourth) {
                    bolt = (EntityLightningBolt) e;
                }
            }
            c.ok(bolt != null, "Channeling in a thunderstorm calls lightning, cast by the thrower");
            if (bolt != null) {
                c.ok(Math.abs(bolt.posX - target.posX) < 1.0D && Math.abs(bolt.posZ - target.posZ) < 1.0D, "lightning strikes the target");
                bolt.onUpdate();
                c.ok(target.isDead, "the struck villager is gone");
                c.ok(!world.getEntitiesWithinAABB(EntityWitch.class, new AxisAlignedBB(new BlockPos(target)).grow(2)).isEmpty(), "and a witch stands there");
                c.ok(done(fourth, Channeling.VERY_VERY_FRIGHTENING), "Very Very Frightening granted");
                c.ok(!done(other, Channeling.VERY_VERY_FRIGHTENING), "not granted to someone else");
            }
        } finally {
            world.rainingStrength = rain;
            world.prevRainingStrength = prevRain;
            world.thunderingStrength = thunder;
            world.prevThunderingStrength = prevThunder;
        }
        clear(c, corner);
    }

    private void sticking(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        c.fill(corner.add(0, 0, 12), corner.add(31, 6, 12), Blocks.STONE.getDefaultState());
        EntityPlayerMP owner = player(c, "sticker", corner.add(5, 0, 5));
        EntityPlayerMP stranger = player(c, "stranger", corner.add(5, 0, 6));
        EntityTrident t = throwAt(c, owner, trident(), new Vec3d(corner.getX() + 5.5D, corner.getY() + 1.5D, corner.getZ() + 12.5D));
        fly(t);
        c.ok(t.isInGround(), "trident sticks in a wall");
        for (int i = 0; i < 10; i++) {
            t.onUpdate();
        }
        c.ok(t.isInGround() && !t.isDead, "still stuck after 10 ticks");
        t.onCollideWithPlayer(stranger);
        c.ok(!t.isDead && stranger.inventory.isEmpty(), "someone else cannot pick it up");
        t.onCollideWithPlayer(owner);
        c.ok(t.isDead, "the thrower picks it up");
        c.ok(owner.inventory.getStackInSlot(0).getItem() instanceof ItemTrident, "back in the inventory");

        EntityTrident old = throwAt(c, owner, trident(), new Vec3d(corner.getX() + 8.5D, corner.getY() + 1.5D, corner.getZ() + 12.5D));
        fly(old);
        for (int i = 0; i < 1210 && !old.isDead; i++) {
            old.onUpdate();
        }
        c.ok(old.isDead, "a plain trident in the ground despawns after a minute");

        EntityTrident loyal = throwAt(c, owner, trident(TridentContent.loyalty(), 1), new Vec3d(corner.getX() + 11.5D, corner.getY() + 1.5D, corner.getZ() + 12.5D));
        NBTTagCompound tag = new NBTTagCompound();
        loyal.writeToNBT(tag);
        tag.removeTag("OwnerMost");
        tag.removeTag("OwnerLeast");
        loyal.setDead();
        EntityTrident ownerless = new EntityTrident(world);
        ownerless.readFromNBT(tag);
        ownerless.setUniqueId(java.util.UUID.randomUUID());
        world.spawnEntity(ownerless);
        fly(ownerless);
        for (int i = 0; i < 1300 && !ownerless.isDead; i++) {
            ownerless.onUpdate();
        }
        c.ok(!ownerless.isDead && ownerless.isInGround(), "a Loyalty trident that can be picked up never despawns");
        c.eq(1, ownerless.loyalty(), "loyalty read back from saved data");

        NBTTagCompound saved = new NBTTagCompound();
        t = throwAt(c, owner, trident(TridentContent.impaling(), 2), new Vec3d(corner.getX() + 14.5D, corner.getY() + 1.5D, corner.getZ() + 12.5D));
        fly(t);
        t.writeToNBT(saved);
        EntityTrident back = new EntityTrident(world);
        back.readFromNBT(saved);
        c.eq(2, TridentContent.impalingLevel(back.thrownStack()), "thrown stack survives saving");
        c.ok(back.isInGround(), "in-ground state survives saving");
        c.ok(back.getOwner() == null || back.getOwner() == owner, "owner id saved");
        t.setDead();
        clear(c, corner);
    }

    private void loyalty(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        c.fill(corner.add(0, 0, 14), corner.add(31, 6, 14), Blocks.STONE.getDefaultState());
        EntityPlayerMP owner = player(c, "loyal", corner.add(5, 0, 5));
        EntityTrident t = throwAt(c, owner, trident(TridentContent.loyalty(), 3), new Vec3d(corner.getX() + 5.5D, corner.getY() + 1.5D, corner.getZ() + 14.5D));
        fly(t);
        c.ok(t.isInGround(), "loyalty trident sticks first");
        for (int i = 0; i < 6; i++) {
            t.onUpdate();
        }
        c.ok(t.isReturning(), "after 4 ticks in the ground it returns");
        double start = t.getDistance(owner);
        int ticks = 0;
        while (ticks < 200 && t.getDistance(owner.posX, owner.posY + owner.getEyeHeight(), owner.posZ) > 1.5D) {
            t.onUpdate();
            ticks++;
        }
        c.ok(ticks < 200, "it reaches its thrower (" + ticks + " ticks from " + (int) start + " blocks)");
        t.onCollideWithPlayer(owner);
        c.ok(t.isDead && owner.inventory.getStackInSlot(0).getItem() instanceof ItemTrident, "returned trident goes back to the inventory");

        // Thrower gone: it drops where it is.
        EntityPlayerMP gone = player(c, "gone", corner.add(10, 0, 5));
        EntityTrident lost = throwAt(c, gone, trident(TridentContent.loyalty(), 1), new Vec3d(corner.getX() + 10.5D, corner.getY() + 1.5D, corner.getZ() + 14.5D));
        fly(lost);
        gone.setDead();
        for (int i = 0; i < 8 && !lost.isDead; i++) {
            lost.onUpdate();
        }
        c.ok(lost.isDead, "thrower gone: the trident stops");
        c.eq(1, Drops.count(world, new BlockPos(lost), TridentContent.trident()), "and drops as an item");

        // The void.
        EntityPlayerMP voidOwner = player(c, "voidthrower", corner.add(15, 0, 5));
        EntityTrident deep = new EntityTrident(world, voidOwner, trident(TridentContent.loyalty(), 2));
        deep.setPosition(corner.getX() + 15.5D, -70.0D, corner.getZ() + 5.5D);
        deep.motionY = -1.0D;
        world.spawnEntity(deep);
        deep.onUpdate();
        deep.onUpdate();
        c.ok(!deep.isDead && deep.isReturning(), "a Loyalty trident comes back from the void");
        EntityTrident plain = new EntityTrident(world, voidOwner, trident());
        plain.setPosition(corner.getX() + 16.5D, -70.0D, corner.getZ() + 5.5D);
        world.spawnEntity(plain);
        plain.onUpdate();
        c.ok(plain.isDead, "a plain trident is lost in the void");
        deep.setDead();

        // Water drag: 0.99, not the arrow's 0.6.
        BlockPos floor = Pools.pool(c, 4);
        EntityTrident wet = new EntityTrident(world, voidOwner, trident());
        wet.setPosition(floor.getX() + 2.5D, floor.getY() + 2.5D, floor.getZ() + 2.5D);
        wet.shoot(0.0D, 0.0D, 1.0D, 0.5F, 0.0F);
        world.spawnEntity(wet);
        wet.onUpdate();
        wet.onUpdate();
        c.ok(wet.isInWater() && wet.motionZ > 0.47D, "water barely slows a trident: " + wet.motionZ);
        clear(c, corner);
    }

    private void riptide(Check c) {
        WorldServer world = c.world;
        ItemTrident item = TridentContent.trident();
        BlockPos dryCorner = c.area();
        EntityPlayerMP dry = player(c, "drypilot", dryCorner.add(5, 0, 5));
        ItemStack stack = trident(TridentContent.riptide(), 3);
        dry.setHeldItem(EnumHand.MAIN_HAND, stack);
        c.ok(!dry.isWet(), "dry land is dry");
        c.eq(EnumActionResult.FAIL, item.onItemRightClick(world, dry, EnumHand.MAIN_HAND).getType(), "riptide needs water or rain");

        BlockPos floor = Pools.pool(c, 3);
        EntityPlayerMP pilot = player(c, "pilot", floor.add(5, 1, 5));
        pilot.setHeldItem(EnumHand.MAIN_HAND, stack);
        pilot.handleWaterMovement();
        c.ok(pilot.isWet(), "player in the pool is wet");
        c.eq(EnumActionResult.SUCCESS, item.onItemRightClick(world, pilot, EnumHand.MAIN_HAND).getType(), "riptide use in water");
        pilot.resetActiveHand();
        item.onPlayerStoppedUsing(stack, world, pilot, 72000 - 10);
        c.ok(pilot.getHeldItemMainhand() == stack, "riptide keeps the trident in hand");
        c.eq(1, stack.getItemDamage(), "riptide costs 1 durability");
        c.eq(0, world.getEntitiesWithinAABB(EntityTrident.class, new AxisAlignedBB(floor, floor.add(14, 6, 14))).size(), "riptide throws nothing");
        double speed = Math.sqrt(pilot.motionX * pilot.motionX + pilot.motionY * pilot.motionY + pilot.motionZ * pilot.motionZ);
        c.ok(Math.abs(speed - 3.0D) < 0.01D, "riptide III launches at 3 blocks per tick: " + speed);
        c.ok(Riptide.isSpinning(pilot), "riptide starts a spin");
        c.eq(Riptide.SPIN_TICKS, Riptide.ticksLeft(pilot), "spin lasts 20 ticks");

        // Spin attack: the first mob touched takes a hit with the trident and the spin ends.
        readyToSwing(pilot, stack);
        EntityPig pig = spawn(c, new EntityPig(world), new Vec3d(pilot.posX, pilot.posY, pilot.posZ));
        pilot.motionX = 0.0D;
        pilot.motionY = 0.0D;
        pilot.motionZ = 1.0D;
        Riptide.tick(pilot);
        c.eq(1.0F, pig.getHealth(), "spin attack hits for the trident's 9");
        c.ok(!Riptide.isSpinning(pilot), "the hit ends the spin");
        c.ok(Math.abs(pilot.motionZ + 0.2D) < 1e-6, "and bounces the player back");
        pig.setDead();

        Riptide.start(pilot, Riptide.SPIN_TICKS);
        pilot.setPosition(floor.getX() + 10.5D, floor.getY() + 1.0D, floor.getZ() + 10.5D);
        for (int i = 0; i < Riptide.SPIN_TICKS - 1; i++) {
            Riptide.tick(pilot);
        }
        c.ok(Riptide.isSpinning(pilot), "still spinning after 19 ticks");
        Riptide.tick(pilot);
        c.ok(!Riptide.isSpinning(pilot), "spin over after 20 ticks");
        clear(c, floor);
    }

    private void melee(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        EntityPlayerMP fighter = player(c, "fighter", corner.add(5, 0, 5));
        ItemStack stack = trident(TridentContent.impaling(), 5);
        readyToSwing(fighter, stack);
        EntitySquid squid = spawn(c, new EntitySquid(world), new Vec3d(corner.getX() + 5.5D, corner.getY(), corner.getZ() + 6.5D));
        fighter.attackTargetEntityWithCurrentItem(squid);
        c.ok(squid.getHealth() <= 0.0F, "Impaling V in melee kills a squid (9 + 12.5)");
        c.eq(1, stack.getItemDamage(), "a melee hit costs 1 durability");
        readyToSwing(fighter, stack);
        EntityPig pig = spawn(c, new EntityPig(world), new Vec3d(corner.getX() + 6.5D, corner.getY(), corner.getZ() + 6.5D));
        fighter.attackTargetEntityWithCurrentItem(pig);
        c.eq(1.0F, pig.getHealth(), "the same trident hits a pig for 9");
        clear(c, corner);
    }

    private void drowned(Check c) {
        WorldServer world = c.world;
        BlockPos floor = Pools.pool(c, 4);
        EntityDrowned drowned = spawn(c, new EntityDrowned(world), new Vec3d(floor.getX() + 6.5D, floor.getY() + 1.0D, floor.getZ() + 6.5D));
        c.ok(drowned.canBreatheUnderwater(), "drowned breathe underwater");
        boolean ranged = false;
        boolean swim = false;
        for (EntityAITasks.EntityAITaskEntry e : drowned.tasks.taskEntries) {
            ranged |= e.action instanceof EntityAIAttackRanged;
            swim |= e.action instanceof EntityAISwimming;
        }
        c.ok(ranged, "drowned have the trident attack");
        c.ok(!swim, "drowned do not float");
        for (int i = 0; i < 40; i++) {
            drowned.onUpdate();
        }
        c.ok(drowned.isEntityAlive() && drowned.getHealth() == drowned.getMaxHealth(), "40 ticks underwater, unhurt");

        long time = world.getWorldTime();
        try {
            setTime(world, 1000L);
            EntityPig dryPig = new EntityPig(world);
            c.ok(!drowned.shouldAttack(dryPig), "by day it ignores targets out of water");
            EntityPig wetPig = spawn(c, new EntityPig(world), new Vec3d(floor.getX() + 3.5D, floor.getY() + 1.0D, floor.getZ() + 3.5D));
            wetPig.handleWaterMovement();
            c.ok(drowned.shouldAttack(wetPig), "by day it attacks targets in water");
            setTime(world, 18000L);
            c.ok(drowned.shouldAttack(dryPig), "at night it attacks anything");

            drowned.handleWaterMovement();
            drowned.setAttackTarget(wetPig);
            drowned.updateSwimming();
            c.ok(drowned.getNavigator() instanceof PathNavigateSwimmer && drowned.isSwimming(), "swims after a target in water");
            drowned.setAttackTarget(null);
            drowned.updateSwimming();
            c.ok(drowned.getNavigator() instanceof PathNavigateGround && !drowned.isSwimming(), "walks without one");
            wetPig.setDead();
        } finally {
            setTime(world, time);
        }

        // Equipment from spawning.
        int tridents = 0;
        int rods = 0;
        int armor = 0;
        int shells = 0;
        BlockPos at = floor.add(6, 1, 6);
        for (int i = 0; i < 1000; i++) {
            EntityDrowned d = new EntityDrowned(world);
            d.setPosition(at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D);
            d.onInitialSpawn(world.getDifficultyForLocation(at), null);
            ItemStack hand = d.getHeldItemMainhand();
            tridents += hand.getItem() instanceof ItemTrident ? 1 : 0;
            rods += hand.getItem() == Items.FISHING_ROD ? 1 : 0;
            shells += d.getHeldItemOffhand().isEmpty() ? 0 : 1;
            for (EntityEquipmentSlot slot : new EntityEquipmentSlot[] {EntityEquipmentSlot.HEAD, EntityEquipmentSlot.CHEST,
                    EntityEquipmentSlot.LEGS, EntityEquipmentSlot.FEET}) {
                // A Halloween pumpkin is the zombie's, not armor.
                ItemStack worn = d.getItemStackFromSlot(slot);
                armor += !worn.isEmpty() && worn.getItem() instanceof net.minecraft.item.ItemArmor ? 1 : 0;
            }
        }
        c.ok(tridents >= 35 && tridents <= 95, "about 6.25% spawn with a trident: " + tridents + "/1000");
        c.ok(rods >= 15 && rods <= 65, "about 3.75% with a fishing rod: " + rods + "/1000");
        c.eq(0, armor, "drowned never spawn with armor");
        if (Owners.ours("nautilus_shell") == null) {
            c.eq(0, shells, "no nautilus shell while its stream is absent (null-safe)");
        } else {
            c.ok(shells > 5 && shells < 60, "about 3% hold a nautilus shell: " + shells);
        }

        // Trident throw: not picked up, 8 damage.
        BlockPos land = c.area();
        EntityDrowned landThrower = spawn(c, new EntityDrowned(world), new Vec3d(land.getX() + 5.5D, land.getY(), land.getZ() + 5.5D));
        landThrower.setHeldItem(EnumHand.MAIN_HAND, trident());
        int hits = 0;
        for (int i = 0; i < 6; i++) {
            EntityPig pig = spawn(c, new EntityPig(world), new Vec3d(land.getX() + 5.5D, land.getY(), land.getZ() + 10.5D));
            landThrower.attackEntityWithRangedAttack(pig, 1.0F);
            List<EntityTrident> tridentsOut = world.getEntitiesWithinAABB(EntityTrident.class, landThrower.getEntityBoundingBox().grow(3));
            EntityTrident t = null;
            for (EntityTrident x : tridentsOut) {
                if (!x.isDead && !x.dealtDamage() && x.getOwner() == landThrower) {
                    t = x;
                }
            }
            if (t == null) {
                c.fail("drowned throw made no trident");
                break;
            }
            c.eq(EntityArrow.PickupStatus.DISALLOWED, t.pickupStatus, "a drowned's trident cannot be picked up");
            fly(t);
            if (pig.getHealth() == 2.0F) {
                hits++;
            }
            t.setDead();
            pig.setDead();
        }
        c.ok(hits > 0, "drowned throws hit for 8 (" + hits + " of 6)");

        // Picking up: keeps a trident over a sword, takes a better trident, takes a trident over a rod.
        EntityDrowned picker = spawn(c, new EntityDrowned(world), new Vec3d(land.getX() + 20.5D, land.getY(), land.getZ() + 20.5D));
        picker.setCanPickUpLoot(true);
        ItemStack held = trident();
        held.setItemDamage(50);
        picker.setHeldItem(EnumHand.MAIN_HAND, held);
        dropAt(c, picker, new ItemStack(Items.DIAMOND_SWORD));
        picker.onUpdate();
        c.ok(picker.getHeldItemMainhand().getItem() instanceof ItemTrident, "keeps its trident over a sword");
        ItemStack better = trident();
        better.setItemDamage(10);
        dropAt(c, picker, better);
        picker.onUpdate();
        c.eq(10, picker.getHeldItemMainhand().getItemDamage(), "swaps for a less damaged trident");

        // Saving.
        EntityDrowned baby = new EntityDrowned(world);
        baby.setChild(true);
        baby.setHeldItem(EnumHand.MAIN_HAND, trident());
        NBTTagCompound tag = new NBTTagCompound();
        baby.writeToNBT(tag);
        EntityDrowned read = new EntityDrowned(world);
        read.readFromNBT(tag);
        c.ok(read.isChild() && read.getHeldItemMainhand().getItem() instanceof ItemTrident, "drowned survive saving (baby, trident)");

        // Loot: 0-2 rotten flesh, nothing else.
        LootTable table = world.getLootTableManager().getLootTableFromLocation(EntityDrowned.LOOT_TABLE);
        c.ok(table != LootTable.EMPTY_LOOT_TABLE, "drowned loot table loads");
        EntityPlayerMP killer = player(c, "killer", land);
        int flesh = 0;
        boolean other = false;
        boolean tooMany = false;
        Random rand = new Random(3);
        for (int i = 0; i < 200; i++) {
            LootContext ctx = new LootContext.Builder(world).withLootedEntity(drowned)
                    .withDamageSource(DamageSource.causePlayerDamage(killer)).withPlayer(killer).build();
            int n = 0;
            for (ItemStack s : table.generateLootForPools(rand, ctx)) {
                if (s.getItem() == Items.ROTTEN_FLESH) {
                    n += s.getCount();
                } else {
                    other = true;
                }
            }
            flesh += n;
            tooMany |= n > 2;
        }
        c.ok(!other && !tooMany && flesh > 100 && flesh < 300, "drops 0-2 rotten flesh and nothing else: " + flesh + " in 200");
        clear(c, floor);
        clear(c, land);
    }

    private static void dropAt(Check c, EntityLiving mob, ItemStack stack) {
        EntityItem item = new EntityItem(c.world, mob.posX, mob.posY, mob.posZ, stack);
        item.setNoPickupDelay();
        c.world.spawnEntity(item);
    }

    private static void setTime(WorldServer world, long time) {
        world.setWorldTime(time);
        world.setSkylightSubtracted(world.calculateSkylightSubtracted(1.0F));
    }

    private void spawning(Check c) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        // A sealed dark water cell below sea level, under the cleared area.
        BlockPos cell = corner.add(4, -60, 4);
        c.fill(cell.add(-1, -1, -1), cell.add(5, 8, 5), Blocks.STONE.getDefaultState());
        c.fill(cell, cell.add(4, 7, 4), Blocks.WATER.getDefaultState());
        BlockPos deep = cell.add(2, 3, 2);
        c.ok(deep.getY() < world.getSeaLevel() - 5, "cell is deep");
        Random rand = new Random(11);
        int ocean = 0;
        int river = 0;
        int dry = 0;
        for (int i = 0; i < 6000; i++) {
            ocean += DrownedSpawning.waterRule(world, deep, Biomes.OCEAN, rand) ? 1 : 0;
            river += DrownedSpawning.waterRule(world, deep, Biomes.RIVER, rand) ? 1 : 0;
            dry += DrownedSpawning.waterRule(world, cell.add(-1, 3, 2), Biomes.OCEAN, rand) ? 1 : 0;
        }
        c.ok(ocean > 100 && ocean < 200, "ocean: 1 in 40 (" + ocean + " in 6000)");
        c.ok(river > 300 && river < 500, "river: 1 in 15 (" + river + " in 6000)");
        c.eq(0, dry, "never out of water");
        BlockPos shallow = corner.add(2, -40, 2);
        world.setBlockState(shallow, Blocks.WATER.getDefaultState(), 2 | 16);
        int high = 0;
        if (shallow.getY() >= world.getSeaLevel() - 5) {
            for (int i = 0; i < 2000; i++) {
                high += DrownedSpawning.waterRule(world, shallow, Biomes.OCEAN, rand) ? 1 : 0;
            }
            c.eq(0, high, "never in an ocean less than 6 below sea level");
        }
        int shallowRiver = 0;
        for (int i = 0; i < 2000; i++) {
            shallowRiver += DrownedSpawning.waterRule(world, shallow, Biomes.RIVER, rand) ? 1 : 0;
        }
        c.ok(shallowRiver > 0, "rivers at any depth");

        EntityDrowned d = new EntityDrowned(world);
        d.setPosition(deep.getX() + 0.5D, deep.getY(), deep.getZ() + 0.5D);
        int ok = 0;
        for (int i = 0; i < 200; i++) {
            ok += d.getCanSpawnHere() ? 1 : 0;
        }
        c.eq(200, ok, "dark water: light allows it");
        c.ok(d.isNotColliding(), "water is not in the way");
        // Light only spreads where every chunk 17 blocks around is loaded; load them, then relight.
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                world.getChunkProvider().provideChunk((cell.getX() >> 4) + dx, (cell.getZ() >> 4) + dz);
            }
        }
        BlockPos lamp = cell.add(0, 3, 0);
        world.setBlockState(lamp, Blocks.GLOWSTONE.getDefaultState(), 2 | 16);
        world.checkLight(lamp);
        c.ok(world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, deep) > 0, "glowstone lights the cell");
        ok = 0;
        for (int i = 0; i < 200; i++) {
            ok += d.getCanSpawnHere() ? 1 : 0;
        }
        c.eq(0, ok, "any block light stops it");

        EntityDrowned dryOne = new EntityDrowned(world);
        dryOne.setPosition(corner.getX() + 5.5D, corner.getY(), corner.getZ() + 5.5D);
        c.eq(Event.Result.DENY, ForgeEventFactory.canEntitySpawn(dryOne, world, (float) dryOne.posX, (float) dryOne.posY, (float) dryOne.posZ, false),
                "natural spawn out of water is denied");
        c.eq(Event.Result.DEFAULT, ForgeEventFactory.canEntitySpawn(dryOne, world, (float) dryOne.posX, (float) dryOne.posY, (float) dryOne.posZ, true),
                "spawners skip the water rule");
        c.fill(cell.add(-1, -1, -1), cell.add(5, 8, 5), Blocks.STONE.getDefaultState());
    }

    private void conversion(Check c) {
        WorldServer world = c.world;
        BlockPos floor = Pools.pool(c, 3);
        // Roof the pool, so a mob that floats up keeps its eyes in the water.
        c.fill(floor.add(-1, 4, -1), floor.add(12, 4, 12), Blocks.STONE.getDefaultState());
        EntityZombie zombie = spawn(c, new EntityZombie(world), new Vec3d(floor.getX() + 3.5D, floor.getY() + 1.0D, floor.getZ() + 3.5D));
        zombie.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_SWORD));
        zombie.setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
        zombie.setCustomNameTag("Bob");
        zombie.enablePersistence();
        EntityHusk husk = spawn(c, new EntityHusk(world), new Vec3d(floor.getX() + 8.5D, floor.getY() + 1.0D, floor.getZ() + 8.5D));
        EntityZombieVillager villager = spawn(c, new EntityZombieVillager(world), new Vec3d(floor.getX() + 3.5D, floor.getY() + 1.0D, floor.getZ() + 8.5D));
        EntityDrowned already = spawn(c, new EntityDrowned(world), new Vec3d(floor.getX() + 8.5D, floor.getY() + 1.0D, floor.getZ() + 3.5D));
        BlockPos dryCorner = c.area();
        EntityZombie dryZombie = spawn(c, new EntityZombie(world), new Vec3d(dryCorner.getX() + 5.5D, dryCorner.getY(), dryCorner.getZ() + 5.5D));

        Entity[] mobs = {zombie, husk, villager, already, dryZombie};
        for (int i = 0; i < DrownedConversion.UNDERWATER_TICKS - 5; i++) {
            for (Entity e : mobs) {
                if (!e.isDead) {
                    e.onUpdate();
                }
            }
        }
        c.ok(!DrownedConversion.isConverting(zombie) && zombie.getEntityData().hasKey(DrownedConversion.IN_WATER_TIME),
                "595 ticks underwater: counting, not yet converting");
        c.eq(20.0F, zombie.getHealth(), "a zombie with the clock running does not drown");
        c.ok(!dryZombie.getEntityData().hasKey(DrownedConversion.IN_WATER_TIME), "a dry zombie has no clock");
        for (int i = 0; i < 10; i++) {
            for (Entity e : mobs) {
                if (!e.isDead) {
                    e.onUpdate();
                }
            }
        }
        c.ok(DrownedConversion.isConverting(zombie), "after 30 seconds the zombie starts converting");
        c.ok(DrownedConversion.isConverting(husk), "so does the husk");
        c.ok(!DrownedConversion.isConverting(villager), "a zombie villager does not");
        c.ok(!already.getEntityData().hasKey(DrownedConversion.IN_WATER_TIME) && !DrownedConversion.isConverting(already), "a drowned does not");

        NBTTagCompound saved = new NBTTagCompound();
        zombie.writeToNBT(saved);
        EntityZombie reloaded = new EntityZombie(world);
        reloaded.readFromNBT(saved);
        c.ok(DrownedConversion.isConverting(reloaded), "the conversion clock survives saving");

        AxisAlignedBB pool = new AxisAlignedBB(floor, floor.add(13, 5, 13));
        for (int i = 0; i < DrownedConversion.CONVERSION_TICKS + 5; i++) {
            for (Entity e : mobs) {
                if (!e.isDead) {
                    e.onUpdate();
                }
            }
        }
        c.ok(zombie.isDead, "the zombie is gone after 15 more seconds");
        c.ok(husk.isDead, "and the husk");
        EntityDrowned made = null;
        EntityZombie fromHusk = null;
        for (EntityZombie z : world.getEntitiesWithinAABB(EntityZombie.class, pool)) {
            if (z.isDead || z == already) {
                continue;
            }
            if (z instanceof EntityDrowned) {
                made = (EntityDrowned) z;
            } else if (z.getClass() == EntityZombie.class) {
                fromHusk = z;
            }
        }
        c.ok(made != null, "a drowned stands in its place");
        if (made != null) {
            c.ok(made.getHeldItemMainhand().getItem() == Items.IRON_SWORD, "with the zombie's sword");
            NBTTagCompound chances = new NBTTagCompound();
            made.writeEntityToNBT(chances);
            c.ok(chances.getTagList("HandDropChances", 5).getFloatAt(0) == 2.0F, "and its drop chance");
            c.eq("Bob", made.getCustomNameTag(), "and its name");
            c.ok(made.isNoDespawnRequired(), "and its persistence");
            c.eq(made.getMaxHealth(), made.getHealth(), "at full health");
        }
        c.ok(fromHusk != null, "the husk became a zombie");
        c.ok(villager.isDead || !DrownedConversion.isConverting(villager), "the zombie villager never converted");

        // Off in the config: no clock.
        boolean was = ModConfig.trident.zombiesConvert;
        try {
            ModConfig.trident.zombiesConvert = false;
            EntityZombie off = spawn(c, new EntityZombie(world), new Vec3d(floor.getX() + 6.5D, floor.getY() + 1.0D, floor.getZ() + 6.5D));
            for (int i = 0; i < 20; i++) {
                off.onUpdate();
            }
            c.ok(!off.getEntityData().hasKey(DrownedConversion.IN_WATER_TIME), "config off: zombies do not convert");
        } finally {
            ModConfig.trident.zombiesConvert = was;
        }
        clear(c, floor);
        clear(c, dryCorner);
    }

    private void packets(Check c) {
        ByteBuf buf = Unpooled.buffer();
        new PacketSpin(1234, 20).toBytes(buf);
        PacketSpin spin = new PacketSpin();
        spin.fromBytes(buf);
        c.ok(spin.entityId() == 1234 && spin.ticks() == 20, "spin packet round trip");
        buf = Unpooled.buffer();
        new PacketDrowning(77, true).toBytes(buf);
        PacketDrowning drowning = new PacketDrowning();
        drowning.fromBytes(buf);
        c.ok(drowning.entityId() == 77 && drowning.converting(), "drowning packet round trip");
    }
}
