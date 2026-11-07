package com.exiledradio.updateaquaticbackport.turtle;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.PotionTypes;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.util.NonNullList;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Turtles, turtle eggs, scutes, the turtle shell and the Potion of the Turtle Master.
 *
 * <p>Registers the egg block and its item, the scute and the shell, the turtle (entity id 10, with its
 * spawn egg), the three Turtle Master potions and their brewing, the shell's recipe, and in
 * {@link #postInit} the beach spawns. The AI that other mobs get for turtles is {@link TurtleEvents}.
 * Off ({@code turtles.enabled}), nothing is registered and everything here does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class TurtleContent {

    /** This stream's entity ids (10-14 in the plan); the turtle is the first. */
    public static final int TURTLE_ENTITY_ID = 10;

    /** 1.14.4's spawn egg colors. */
    private static final int EGG_PRIMARY = 15198183, EGG_SECONDARY = 44975;

    private static BlockTurtleEgg turtleEgg;
    private static Item scute;
    private static ItemArmor turtleHelmet;
    private static ItemArmor.ArmorMaterial turtleMaterial;
    private static PotionType turtleMaster, longTurtleMaster, strongTurtleMaster;

    /** Seagrass is the kelp stream's: looked up by name once registries are done, null when it is off. */
    private static Item seagrass;
    private static boolean seagrassLooked;

    private TurtleContent() {
    }

    public static boolean enabled() {
        return ModConfig.turtles.enabled;
    }

    @Nullable
    public static BlockTurtleEgg turtleEgg() {
        return turtleEgg;
    }

    @Nullable
    public static Item scute() {
        return scute;
    }

    @Nullable
    public static ItemArmor turtleHelmet() {
        return turtleHelmet;
    }

    @Nullable
    public static PotionType turtleMaster() {
        return turtleMaster;
    }

    @Nullable
    public static PotionType longTurtleMaster() {
        return longTurtleMaster;
    }

    @Nullable
    public static PotionType strongTurtleMaster() {
        return strongTurtleMaster;
    }

    /** @return the seagrass item turtles eat and drop, or null when the kelp stream is switched off */
    @Nullable
    public static Item seagrass() {
        if (!seagrassLooked) {
            // Settled for good in postInit; before then (nothing asks that early) it is looked up each time.
            return Owners.ours("seagrass");
        }
        return seagrass;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        turtleEgg = Content.block(event.getRegistry(), "turtle_egg", new BlockTurtleEgg(), null);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        Content.blockItem(registry, turtleEgg, new ItemTurtleEgg(turtleEgg));
        scute = Content.item(registry, "scute", new Item());

        // 1.14.4 ArmorMaterial.TURTLE: durability factor 25 (275 for a helmet), 2 armor on the head,
        // enchantability 9, no toughness, repaired with scutes. The texture is
        // textures/models/armor/turtle_layer_1.png under this mod's id.
        if (turtleMaterial == null) {
            turtleMaterial = EnumHelper.addArmorMaterial("UPDATEAQUATICBACKPORT_TURTLE", UpdateAquaticBackport.MOD_ID + ":turtle",
                    25, new int[] {2, 5, 6, 2}, 9, TurtleSounds.EQUIP_TURTLE, 0.0F);
            turtleMaterial.setRepairItem(new ItemStack(scute));
        }
        turtleHelmet = Content.item(registry, "turtle_helmet", new ItemArmor(turtleMaterial, 0, EntityEquipmentSlot.HEAD));
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().register(EntityEntryBuilder.create()
                .entity(EntityTurtle.class)
                .id(Content.id("turtle"), TURTLE_ENTITY_ID)
                .name(UpdateAquaticBackport.MOD_ID + ".turtle")
                .tracker(80, 3, true)
                .egg(EGG_PRIMARY, EGG_SECONDARY)
                .build());
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<PotionType> event) {
        if (!enabled()) {
            return;
        }
        // Current Java: Slowness IV and Resistance III for 20 s; long 40 s; strong Slowness VI and Resistance IV.
        // All three share the base name, so every form is "... of the Turtle Master".
        turtleMaster = potion(event.getRegistry(), "turtle_master", 400, 3, 2);
        longTurtleMaster = potion(event.getRegistry(), "long_turtle_master", 800, 3, 2);
        strongTurtleMaster = potion(event.getRegistry(), "strong_turtle_master", 400, 5, 3);
    }

    private static PotionType potion(IForgeRegistry<PotionType> registry, String name, int ticks, int slowness, int resistance) {
        PotionType type = new PotionType("turtle_master",
                new PotionEffect(MobEffects.SLOWNESS, ticks, slowness), new PotionEffect(MobEffects.RESISTANCE, ticks, resistance));
        type.setRegistryName(Content.id(name));
        registry.register(type);
        return type;
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Ingredient s = Ingredient.fromItem(scute);
        ShapedRecipes shell = new ShapedRecipes("", 3, 2, NonNullList.from(Ingredient.EMPTY, s, s, s, s, Ingredient.EMPTY, s),
                new ItemStack(turtleHelmet));
        shell.setRegistryName(Content.id("turtle/turtle_helmet"));
        event.getRegistry().register(shell);

        // 1.14.4 PotionBrewing: awkward + turtle shell, then redstone for long and glowstone for strong.
        // Gunpowder, dragon's breath and tipped arrows work on any potion type by themselves.
        BrewingRecipeRegistry.addRecipe(new TurtleBrewing(PotionTypes.AWKWARD, turtleHelmet, turtleMaster));
        BrewingRecipeRegistry.addRecipe(new TurtleBrewing(turtleMaster, Items.REDSTONE, longTurtleMaster));
        BrewingRecipeRegistry.addRecipe(new TurtleBrewing(turtleMaster, Items.GLOWSTONE_DUST, strongTurtleMaster));
    }

    /** @return the biomes turtles spawn in: sandy beaches that are not snowy (1.13's beach, not snowy beach or stone shore) */
    public static List<Biome> spawnBiomes() {
        List<Biome> out = new ArrayList<Biome>();
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH) && !BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY)
                    && biome.topBlock.getBlock() == Blocks.SAND) {
                out.add(biome);
            }
        }
        return out;
    }

    /** Beach spawns, once every mod has registered its biomes. Called from the mod's postInit. */
    public static void postInit() {
        seagrass = Owners.ours("seagrass");
        seagrassLooked = true;
        if (!enabled() || ModConfig.turtles.spawnWeight <= 0) {
            return;
        }
        List<Biome> biomes = spawnBiomes();
        // 1.14.4 BeachBiome: weight 5, groups of 2 to 5.
        EntityRegistry.addSpawn(EntityTurtle.class, ModConfig.turtles.spawnWeight, 2, 5, EnumCreatureType.CREATURE,
                biomes.toArray(new Biome[0]));
        UpdateAquaticBackport.LOGGER.info("Turtles spawn in {} beach biomes.", biomes.size());
    }
}
