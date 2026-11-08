package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraft.entity.EntitySpawnPlacementRegistry;

import javax.annotation.Nullable;

/**
 * The trident, its four enchantments, the thrown trident and the drowned.
 *
 * <p>Registers the item, the enchantments (on a trident-only enchantment type), the two entities (ids
 * 25 and 26 of this mod), the drowned's loot table and in-water spawn placement; {@link #init} adds the
 * drowned to the spawn lists once every biome exists. Off ({@code trident.enabled}), nothing is
 * registered and everything here does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class TridentContent {

    public static final int TRIDENT_ENTITY_ID = 25;
    public static final int DROWNED_ENTITY_ID = 26;

    /** 1.14.4's drowned spawn egg colors. */
    public static final int EGG_PRIMARY = 0x8FF1D7;
    public static final int EGG_SECONDARY = 0x799C65;

    private static ItemTrident trident;
    private static EnumEnchantmentType tridentType;
    private static EnchantmentTrident loyalty;
    private static EnchantmentTrident riptide;
    private static EnchantmentTrident impaling;
    private static EnchantmentTrident channeling;

    private TridentContent() {
    }

    public static boolean enabled() {
        return ModConfig.trident.enabled;
    }

    @Nullable
    public static ItemTrident trident() {
        return trident;
    }

    @Nullable
    public static EnumEnchantmentType tridentType() {
        return tridentType;
    }

    @Nullable
    public static EnchantmentTrident loyalty() {
        return loyalty;
    }

    @Nullable
    public static EnchantmentTrident riptide() {
        return riptide;
    }

    @Nullable
    public static EnchantmentTrident impaling() {
        return impaling;
    }

    @Nullable
    public static EnchantmentTrident channeling() {
        return channeling;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        trident = Content.item(event.getRegistry(), "trident", new ItemTrident());
    }

    @SubscribeEvent
    public static void registerEnchantments(RegistryEvent.Register<Enchantment> event) {
        if (!enabled()) {
            return;
        }
        tridentType = EnumHelper.addEnchantmentType("UPDATEAQUATICBACKPORT_TRIDENT", item -> item instanceof ItemTrident);
        loyalty = enchantment(event, "loyalty", EnchantmentTrident.Kind.LOYALTY);
        riptide = enchantment(event, "riptide", EnchantmentTrident.Kind.RIPTIDE);
        impaling = enchantment(event, "impaling", EnchantmentTrident.Kind.IMPALING);
        channeling = enchantment(event, "channeling", EnchantmentTrident.Kind.CHANNELING);
        // Combat shows a book of each at its highest level, with the bow's and the sword's, as in vanilla.
        com.exiledradio.backportcore.creative.CreativePlacement.addEnchantmentType(
                net.minecraft.creativetab.CreativeTabs.COMBAT, tridentType);
    }

    private static EnchantmentTrident enchantment(RegistryEvent.Register<Enchantment> event, String name, EnchantmentTrident.Kind kind) {
        EnchantmentTrident e = new EnchantmentTrident(kind, tridentType);
        e.setName(UpdateAquaticBackport.MOD_ID + "." + name);
        e.setRegistryName(Content.id(name));
        event.getRegistry().register(e);
        return e;
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityTrident.class)
                .id(Content.id("trident"), TRIDENT_ENTITY_ID)
                .name(UpdateAquaticBackport.MOD_ID + ".trident")
                .tracker(64, 20, true)
                .build());
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityDrowned.class)
                .id(Content.id("drowned"), DROWNED_ENTITY_ID)
                .name(UpdateAquaticBackport.MOD_ID + ".drowned")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
        EntitySpawnPlacementRegistry.setPlacementType(EntityDrowned.class, EntityLiving.SpawnPlacementType.IN_WATER);
        LootTableList.register(EntityDrowned.LOOT_TABLE);
    }

    /** From the mod's init: the spawn lists, once every mod's biomes are registered and typed. */
    public static void init() {
        DrownedSpawning.addSpawns();
    }

    // ------------------------------------------------------------------------------------------

    private static int level(@Nullable Enchantment enchantment, ItemStack stack) {
        return enchantment == null || stack.isEmpty() ? 0 : EnchantmentHelper.getEnchantmentLevel(enchantment, stack);
    }

    public static int loyaltyLevel(ItemStack stack) {
        return level(loyalty, stack);
    }

    public static int riptideLevel(ItemStack stack) {
        return level(riptide, stack);
    }

    public static int impalingLevel(ItemStack stack) {
        return level(impaling, stack);
    }

    public static int channelingLevel(ItemStack stack) {
        return level(channeling, stack);
    }

    /** 1.14.4's {@code DamageSource.causeTridentDamage}: death messages "death.attack.trident". */
    public static DamageSource tridentDamage(Entity trident, Entity thrower) {
        return new EntityDamageSourceIndirect("trident", trident, thrower).setProjectile();
    }

    /** @return true if a packet can be sent to this player (a real connection, not a fake or test player) */
    public static boolean connected(EntityPlayerMP player) {
        return !(player instanceof FakePlayer) && player.connection != null
                && player.connection.getNetworkManager() != null
                && player.connection.getNetworkManager().channel() != null;
    }
}
