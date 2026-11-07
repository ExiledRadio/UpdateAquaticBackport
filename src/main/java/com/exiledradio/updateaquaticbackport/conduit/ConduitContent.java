package com.exiledradio.updateaquaticbackport.conduit;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.potion.Potion;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraftforge.common.IRarity;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;

/**
 * The conduit, Conduit Power, the nautilus shell and the heart of the sea.
 *
 * <ul>
 *   <li>Conduit ({@link BlockConduit}, {@link TileEntityConduit}): 8 nautilus shells round a heart of the
 *       sea. Its renderer is client-only ({@code conduit.client}).</li>
 *   <li>Conduit Power ({@link PotionConduitPower}, {@link ConduitPowerEvents}).</li>
 *   <li>Nautilus shell, also a fishing treasure (weight 1 beside the six of 1.12.2's treasure pool, as
 *       1.13 added it); the drowned's off-hand shell belongs to the trident stream, which finds it by name.</li>
 *   <li>Heart of the sea: the structure stream puts it in buried treasure, by name.</li>
 *   <li>All three items Uncommon, today's rarity (1.21.2 made the conduit and the shell Uncommon).</li>
 * </ul>
 * Off ({@code conduit.enabled}), nothing is registered and the events here do nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class ConduitContent {

    public static final SoundEvent ACTIVATE = event("block.conduit.activate");
    public static final SoundEvent AMBIENT = event("block.conduit.ambient");
    public static final SoundEvent AMBIENT_SHORT = event("block.conduit.ambient.short");
    public static final SoundEvent ATTACK_TARGET = event("block.conduit.attack.target");
    public static final SoundEvent DEACTIVATE = event("block.conduit.deactivate");

    /** Conduit Power; registered only when the stream is on, but always built so code may name it. */
    public static final Potion CONDUIT_POWER = new PotionConduitPower().setRegistryName(Content.id("conduit_power"));

    @Nullable
    private static BlockConduit conduit;
    @Nullable
    private static Item nautilusShell;
    @Nullable
    private static Item heartOfTheSea;

    private ConduitContent() {
    }

    public static boolean enabled() {
        return ModConfig.conduit.enabled;
    }

    @Nullable
    public static BlockConduit conduit() {
        return conduit;
    }

    @Nullable
    public static Item nautilusShell() {
        return nautilusShell;
    }

    @Nullable
    public static Item heartOfTheSea() {
        return heartOfTheSea;
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        return e;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(ACTIVATE, AMBIENT, AMBIENT_SHORT, ATTACK_TARGET, DEACTIVATE);
        }
    }

    @SubscribeEvent
    public static void registerPotions(RegistryEvent.Register<Potion> event) {
        if (enabled()) {
            event.getRegistry().register(CONDUIT_POWER);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        conduit = Content.block(event.getRegistry(), "conduit", new BlockConduit(), "pickaxe");
        Content.multipart(conduit);
        GameRegistry.registerTileEntity(TileEntityConduit.class, Content.id("conduit"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        Content.blockItem(registry, conduit, new ItemBlock(conduit) {
            @Override
            public IRarity getForgeRarity(ItemStack stack) {
                return EnumRarity.UNCOMMON;
            }
        });
        nautilusShell = Content.item(registry, "nautilus_shell", uncommon());
        heartOfTheSea = Content.item(registry, "heart_of_the_sea", uncommon());
    }

    private static Item uncommon() {
        return new Item() {
            @Override
            public IRarity getForgeRarity(ItemStack stack) {
                return EnumRarity.UNCOMMON;
            }
        };
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        Ingredient n = Ingredient.fromItem(nautilusShell);
        Ingredient h = Ingredient.fromItem(heartOfTheSea);
        ShapedRecipes recipe = new ShapedRecipes("", 3, 3, NonNullList.from(Ingredient.EMPTY, n, n, n, n, h, n, n, n, n),
                new ItemStack(conduit));
        recipe.setRegistryName(Content.id("conduit/conduit"));
        event.getRegistry().register(recipe);
    }

    /** The nautilus shell joins the fishing treasure pool with weight 1, as 1.13 added it. */
    @SubscribeEvent
    public static void loot(LootTableLoadEvent event) {
        if (!enabled() || !LootTableList.GAMEPLAY_FISHING_TREASURE.equals(event.getName())) {
            return;
        }
        LootPool pool = event.getTable().getPool("main");
        if (pool == null) {
            UpdateAquaticBackport.LOGGER.warn("The fishing treasure table has no main pool; nautilus shells are not fished up.");
            return;
        }
        pool.addEntry(new LootEntryItem(nautilusShell, 1, 0, new LootFunction[0], new LootCondition[0],
                Content.id("nautilus_shell").toString()));
    }
}
