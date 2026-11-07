package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraft.entity.EntitySpawnPlacementRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Dolphins and Dolphin's Grace.
 *
 * <p>Registers the dolphin (entity id 15 of this mod, with a spawn egg), its sounds and loot table, the
 * Dolphin's Grace effect, and in {@link #postInit} the dolphin's natural spawning in every ocean biome
 * that is not snowy (so other mods' oceans, and this mod's own once the oceans stream adds them, get
 * dolphins too; {@link EntityDolphin#canSpawnAt} then sorts out temperatures). Off
 * ({@code dolphins.enabled}), nothing is registered and everything here does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class DolphinContent {

    /** This stream's entity ids are 15-19; the dolphin is the first. */
    public static final int DOLPHIN_ID = 15;

    /** 1.14.4's spawn egg colors. */
    public static final int EGG_PRIMARY = 2243405;
    public static final int EGG_SECONDARY = 16382457;

    /** 1.14.4's spawn group size. */
    public static final int GROUP_MIN = 1;
    public static final int GROUP_MAX = 2;

    public static final ResourceLocation LOOT_TABLE = Content.id("entities/dolphin");

    public static final SoundEvent AMBIENT = event("entity.dolphin.ambient");
    public static final SoundEvent AMBIENT_WATER = event("entity.dolphin.ambient_water");
    public static final SoundEvent ATTACK = event("entity.dolphin.attack");
    public static final SoundEvent DEATH = event("entity.dolphin.death");
    public static final SoundEvent EAT = event("entity.dolphin.eat");
    public static final SoundEvent HURT = event("entity.dolphin.hurt");
    public static final SoundEvent JUMP = event("entity.dolphin.jump");
    public static final SoundEvent PLAY = event("entity.dolphin.play");
    public static final SoundEvent SPLASH = event("entity.dolphin.splash");
    public static final SoundEvent SWIM = event("entity.dolphin.swim");

    private static final SoundEvent[] SOUNDS = {AMBIENT, AMBIENT_WATER, ATTACK, DEATH, EAT, HURT, JUMP, PLAY, SPLASH, SWIM};

    @Nullable
    private static PotionDolphinsGrace dolphinsGrace;

    /** Biomes the dolphin was added to, for the self-test. */
    private static final List<Biome> SPAWN_BIOMES = new ArrayList<Biome>();

    private DolphinContent() {
    }

    public static boolean enabled() {
        return ModConfig.dolphins.enabled;
    }

    /** @return Dolphin's Grace, or null when this stream is off */
    @Nullable
    public static Potion dolphinsGrace() {
        return dolphinsGrace;
    }

    public static List<Biome> spawnBiomes() {
        return SPAWN_BIOMES;
    }

    private static SoundEvent event(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(SOUNDS);
        }
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        if (!enabled()) {
            return;
        }
        dolphinsGrace = new PotionDolphinsGrace();
        dolphinsGrace.setRegistryName(Content.id("dolphins_grace"));
        event.getRegistry().register(dolphinsGrace);
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityDolphin.class)
                .id(Content.id("dolphin"), DOLPHIN_ID)
                // entity.<modid>.dolphin.name: matched to entity.minecraft.dolphin by the language tool.
                .name(UpdateAquaticBackport.MOD_ID + ".dolphin")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
        EntitySpawnPlacementRegistry.setPlacementType(EntityDolphin.class, EntityLiving.SpawnPlacementType.IN_WATER);
        LootTableList.register(LOOT_TABLE);
    }

    /**
     * Adds natural spawning to every ocean biome registered by now that is not snowy. Called from the mod's
     * postInit, after every mod has registered its biomes and their dictionary types.
     */
    public static void postInit() {
        int weight = ModConfig.dolphins.spawnWeight;
        if (!enabled() || weight <= 0) {
            return;
        }
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)) {
                EntityRegistry.addSpawn(EntityDolphin.class, weight, GROUP_MIN, GROUP_MAX, EnumCreatureType.WATER_CREATURE, biome);
                SPAWN_BIOMES.add(biome);
            }
        }
        UpdateAquaticBackport.LOGGER.info("Dolphins spawn in {} ocean biomes.", SPAWN_BIOMES.size());
    }
}
