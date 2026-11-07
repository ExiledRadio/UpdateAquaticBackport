package com.exiledradio.updateaquaticbackport.phantom;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionHelper;
import net.minecraft.potion.PotionType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

import javax.annotation.Nullable;

/**
 * Phantoms, the phantom membrane and Slow Falling.
 *
 * <ul>
 *   <li>{@link EntityPhantom}: the mob, registered as {@code phantom} (other mods of the family find it by
 *       that name: cats scare it, a crossbow advancement counts it).</li>
 *   <li>{@link Insomnia}: the "time since rest" statistic and the spawner that uses it.</li>
 *   <li>{@code phantom_membrane}: the phantom's drop, which brews Slow Falling and repairs the elytra
 *       ({@link ElytraRepair}).</li>
 *   <li>{@link PotionSlowFalling} and its two potions (1:30 and 4:00); splash, lingering and tipped arrow forms
 *       come from vanilla's own conversions.</li>
 * </ul>
 * Off ({@code phantoms.enabled}), nothing is registered and every handler does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class PhantomContent {

    /** This stream's block of entity ids (20-24). */
    public static final int PHANTOM_ENTITY_ID = 20;

    /** 1.14.4's spawn egg colors. */
    public static final int EGG_PRIMARY = 0x43518A;
    public static final int EGG_SECONDARY = 0x88FF00;

    /** 1.14.4: Slow Falling lasts 1:30, its long form 4:00. */
    public static final int SLOW_FALLING_TICKS = 1800;
    public static final int LONG_SLOW_FALLING_TICKS = 4800;

    public static final ResourceLocation LOOT_TABLE = Content.id("entities/phantom");

    public static SoundEvent soundAmbient;
    public static SoundEvent soundBite;
    public static SoundEvent soundDeath;
    public static SoundEvent soundFlap;
    public static SoundEvent soundHurt;
    public static SoundEvent soundSwoop;

    private static Item membrane;
    private static PotionSlowFalling slowFalling;
    private static PotionType slowFallingType;
    private static PotionType longSlowFallingType;

    private PhantomContent() {
    }

    public static boolean enabled() {
        return ModConfig.phantoms.enabled;
    }

    @Nullable
    public static Item membrane() {
        return membrane;
    }

    @Nullable
    public static PotionSlowFalling slowFalling() {
        return slowFalling;
    }

    @Nullable
    public static PotionType slowFallingType() {
        return slowFallingType;
    }

    @Nullable
    public static PotionType longSlowFallingType() {
        return longSlowFallingType;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        membrane = Content.item(event.getRegistry(), "phantom_membrane", new Item());
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        if (!enabled()) {
            return;
        }
        slowFalling = new PotionSlowFalling();
        slowFalling.setRegistryName(Content.id("slow_falling"));
        event.getRegistry().register(slowFalling);
    }

    @SubscribeEvent
    public static void registerPotionTypes(RegistryEvent.Register<PotionType> event) {
        if (!enabled()) {
            return;
        }
        // Both share one base name, as 1.14's do: it names the bottle ("potion.effect.<base>").
        String base = UpdateAquaticBackport.MOD_ID + ".slow_falling";
        slowFallingType = new PotionType(base, new PotionEffect(slowFalling, SLOW_FALLING_TICKS));
        slowFallingType.setRegistryName(Content.id("slow_falling"));
        longSlowFallingType = new PotionType(base, new PotionEffect(slowFalling, LONG_SLOW_FALLING_TICKS));
        longSlowFallingType.setRegistryName(Content.id("long_slow_falling"));
        event.getRegistry().registerAll(slowFallingType, longSlowFallingType);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        soundAmbient = sound(event, "entity.phantom.ambient");
        soundBite = sound(event, "entity.phantom.bite");
        soundDeath = sound(event, "entity.phantom.death");
        soundFlap = sound(event, "entity.phantom.flap");
        soundHurt = sound(event, "entity.phantom.hurt");
        soundSwoop = sound(event, "entity.phantom.swoop");
    }

    private static SoundEvent sound(RegistryEvent.Register<SoundEvent> event, String name) {
        SoundEvent sound = new SoundEvent(Content.id(name));
        sound.setRegistryName(Content.id(name));
        event.getRegistry().register(sound);
        return sound;
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        // 1.14.4's tracking: 5 chunks (80 blocks), every 3 ticks, with velocity - it flies.
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityPhantom.class)
                .id(Content.id("phantom"), PHANTOM_ENTITY_ID)
                .name(UpdateAquaticBackport.MOD_ID + ".phantom")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
        LootTableList.register(LOOT_TABLE);
        Insomnia.registerStat();
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        // 1.14.4 PotionBrewing: awkward + membrane, then redstone for the long form. Vanilla's own mixes, so
        // splash, lingering and every brewing viewer pick them up.
        PotionHelper.addMix(PotionTypes.AWKWARD, membrane, slowFallingType);
        PotionHelper.addMix(slowFallingType, Items.REDSTONE, longSlowFallingType);
    }
}
