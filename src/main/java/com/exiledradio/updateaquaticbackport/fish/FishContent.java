package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.block.BlockDispenser;
import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntitySpawnPlacementRegistry;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.function.Function;

/**
 * Fish: cod, salmon, pufferfish and tropical fish as mobs (entity ids 1-4 of this stream's 1-9), their
 * four buckets, their sounds, loot tables, spawn eggs and advancement triggers, and the water ambient
 * creature type they spawn under. Spawning is {@link FishSpawning}; the raw fish names, the pufferfish's
 * food effects and the fishing trigger are {@link FishEvents}. Off ({@code fish.enabled}), nothing is
 * registered and every handler does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class FishContent {

    public static final int COD_ID = 1, SALMON_ID = 2, PUFFERFISH_ID = 3, TROPICAL_FISH_ID = 4;

    public static final SoundEvent COD_AMBIENT = event("entity.cod.ambient");
    public static final SoundEvent COD_DEATH = event("entity.cod.death");
    public static final SoundEvent COD_FLOP = event("entity.cod.flop");
    public static final SoundEvent COD_HURT = event("entity.cod.hurt");
    public static final SoundEvent SALMON_AMBIENT = event("entity.salmon.ambient");
    public static final SoundEvent SALMON_DEATH = event("entity.salmon.death");
    public static final SoundEvent SALMON_FLOP = event("entity.salmon.flop");
    public static final SoundEvent SALMON_HURT = event("entity.salmon.hurt");
    public static final SoundEvent PUFFER_AMBIENT = event("entity.puffer_fish.ambient");
    public static final SoundEvent PUFFER_BLOW_OUT = event("entity.puffer_fish.blow_out");
    public static final SoundEvent PUFFER_BLOW_UP = event("entity.puffer_fish.blow_up");
    public static final SoundEvent PUFFER_DEATH = event("entity.puffer_fish.death");
    public static final SoundEvent PUFFER_FLOP = event("entity.puffer_fish.flop");
    public static final SoundEvent PUFFER_HURT = event("entity.puffer_fish.hurt");
    public static final SoundEvent PUFFER_STING = event("entity.puffer_fish.sting");
    public static final SoundEvent TROPICAL_AMBIENT = event("entity.tropical_fish.ambient");
    public static final SoundEvent TROPICAL_DEATH = event("entity.tropical_fish.death");
    public static final SoundEvent TROPICAL_FLOP = event("entity.tropical_fish.flop");
    public static final SoundEvent TROPICAL_HURT = event("entity.tropical_fish.hurt");
    public static final SoundEvent FISH_SWIM = event("entity.fish.swim");
    public static final SoundEvent BUCKET_FILL_FISH = event("item.bucket.fill_fish");
    public static final SoundEvent BUCKET_EMPTY_FISH = event("item.bucket.empty_fish");

    private static final SoundEvent[] SOUNDS = {COD_AMBIENT, COD_DEATH, COD_FLOP, COD_HURT, SALMON_AMBIENT, SALMON_DEATH,
            SALMON_FLOP, SALMON_HURT, PUFFER_AMBIENT, PUFFER_BLOW_OUT, PUFFER_BLOW_UP, PUFFER_DEATH, PUFFER_FLOP, PUFFER_HURT,
            PUFFER_STING, TROPICAL_AMBIENT, TROPICAL_DEATH, TROPICAL_FLOP, TROPICAL_HURT, FISH_SWIM, BUCKET_FILL_FISH,
            BUCKET_EMPTY_FISH};

    public static final ResourceLocation COD_LOOT = Content.id("entities/cod");
    public static final ResourceLocation SALMON_LOOT = Content.id("entities/salmon");
    public static final ResourceLocation PUFFERFISH_LOOT = Content.id("entities/pufferfish");
    public static final ResourceLocation TROPICAL_FISH_LOOT = Content.id("entities/tropical_fish");

    @Nullable
    public static Item codBucket, salmonBucket, pufferfishBucket, tropicalFishBucket;

    @Nullable
    private static EnumCreatureType waterAmbient;

    private FishContent() {
    }

    public static boolean enabled() {
        return ModConfig.fish.enabled;
    }

    /**
     * Current Java's water ambient category, which 1.12.2 lacks: its own cap of 20 (water creatures, the
     * squid's, have 5), peaceful, spawned every tick like monsters and water creatures.
     *
     * @return the creature type, or null when the stream is off
     */
    @Nullable
    public static EnumCreatureType waterAmbient() {
        return waterAmbient;
    }

    static ItemStack stack(@Nullable Item item) {
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        return e;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(SOUNDS);
        }
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        codBucket = bucket(r, "cod_bucket", EntityCod::new);
        salmonBucket = bucket(r, "salmon_bucket", EntitySalmon::new);
        pufferfishBucket = bucket(r, "pufferfish_bucket", EntityPufferfish::new);
        tropicalFishBucket = bucket(r, "tropical_fish_bucket", EntityTropicalFish::new);
        FishTriggers.register();
    }

    private static Item bucket(IForgeRegistry<Item> r, String name, Function<World, ? extends EntityAbstractFish> fish) {
        Item item = Content.item(r, name, new ItemFishBucket(fish));
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, new ItemFishBucket.Dispense());
        return item;
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<EntityEntry> r = event.getRegistry();
        // Spawn egg colors are 1.13's.
        r.register(entry(EntityCod.class, "cod", COD_ID, 12691306, 15058059));
        r.register(entry(EntitySalmon.class, "salmon", SALMON_ID, 10489616, 951412));
        r.register(entry(EntityPufferfish.class, "pufferfish", PUFFERFISH_ID, 16167425, 3654642));
        r.register(entry(EntityTropicalFish.class, "tropical_fish", TROPICAL_FISH_ID, 15690005, 16775663));

        for (Class<? extends EntityLiving> c : new Class[] {EntityCod.class, EntitySalmon.class, EntityPufferfish.class, EntityTropicalFish.class}) {
            EntitySpawnPlacementRegistry.setPlacementType(c, EntityLiving.SpawnPlacementType.IN_WATER);
        }
        for (ResourceLocation table : new ResourceLocation[] {COD_LOOT, SALMON_LOOT, PUFFERFISH_LOOT, TROPICAL_FISH_LOOT}) {
            LootTableList.register(table);
        }
        if (waterAmbient == null) {
            waterAmbient = EnumHelper.addCreatureType("UPDATEAQUATICBACKPORT_WATER_AMBIENT", EntityAbstractFish.class, 20,
                    Material.WATER, true, false);
        }
        UpdateAquaticBackport.LOGGER.info("Registered 4 fish.");
    }

    private static EntityEntry entry(Class<? extends EntityAbstractFish> c, String name, int id, int egg, int spots) {
        return EntityEntryBuilder.create()
                .entity(c)
                .id(Content.id(name), id)
                .name(UpdateAquaticBackport.MOD_ID + "." + name)
                .tracker(64, 3, true)
                .egg(egg, spots)
                .build();
    }
}
