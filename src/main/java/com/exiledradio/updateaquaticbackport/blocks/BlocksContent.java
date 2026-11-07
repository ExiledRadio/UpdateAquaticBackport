package com.exiledradio.updateaquaticbackport.blocks;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPrismarine;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.dispenser.IBlockSource;
import net.minecraft.init.Blocks;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemShulkerBox;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream X: 1.13's smaller blocks and items.
 *
 * <ul>
 *   <li>Prismarine, prismarine brick and dark prismarine stairs and slabs ({@link BlockPrismarineStairs},
 *       {@link BlockPrismarineSlab}), 4 stairs from 6 blocks and 6 slabs from 3; a 1.14 stonecutter that
 *       reads crafting recipes cuts them on its own.</li>
 *   <li>The carved pumpkin ({@link BlockCarvedPumpkin}), carved with shears ({@link PumpkinCarving});
 *       jack o'lanterns are made from it, golems built with it, dispensers place it on a golem's body
 *       or put it on a head; the plain pumpkin loses its face (client) and its golems
 *       ({@link PlacementRules}).</li>
 *   <li>Wood ({@link ItemWood}): 4 logs make 3, as vanilla's hidden all-bark log; 4 planks, charcoal,
 *       fuel and {@code logWood} like a log.</li>
 *   <li>Packed ice from 9 ice; the debug stick ({@link ItemDebugStick}); the undyed shulker box
 *       ({@link BlockUndyedShulkerBox}), washed out of a dyed one in a cauldron ({@link BlocksEvents}).</li>
 * </ul>
 * Off ({@code blocks.enabled}), nothing is registered and every handler in this package does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class BlocksContent {

    /** The three prismarines, by 1.13 name, with vanilla's state each is made of. */
    private static final Map<String, Integer> PRISMARINES = new LinkedHashMap<String, Integer>();

    static {
        PRISMARINES.put("prismarine", BlockPrismarine.ROUGH_META);
        PRISMARINES.put("prismarine_brick", BlockPrismarine.BRICKS_META);
        PRISMARINES.put("dark_prismarine", BlockPrismarine.DARK_META);
    }

    private static final Map<String, BlockPrismarineSlab.Half> SLABS = new LinkedHashMap<String, BlockPrismarineSlab.Half>();
    private static final Map<String, BlockPrismarineSlab.Double> DOUBLES = new LinkedHashMap<String, BlockPrismarineSlab.Double>();
    private static final Map<String, BlockPrismarineStairs> STAIRS = new LinkedHashMap<String, BlockPrismarineStairs>();
    private static final List<ItemWood> WOODS = new ArrayList<ItemWood>();

    private static BlockCarvedPumpkin carvedPumpkin;
    private static BlockUndyedShulkerBox shulkerBox;
    private static ItemShulkerBox shulkerBoxItem;
    private static ItemDebugStick debugStick;
    private static SoundEvent carveSound;

    private BlocksContent() {
    }

    public static boolean enabled() {
        return ModConfig.blocks.enabled;
    }

    @Nullable
    public static BlockCarvedPumpkin carvedPumpkin() {
        return carvedPumpkin;
    }

    @Nullable
    public static BlockUndyedShulkerBox shulkerBox() {
        return shulkerBox;
    }

    @Nullable
    public static ItemShulkerBox shulkerBoxItem() {
        return shulkerBoxItem;
    }

    @Nullable
    public static ItemDebugStick debugStick() {
        return debugStick;
    }

    public static SoundEvent carveSound() {
        return carveSound;
    }

    public static Map<String, BlockPrismarineSlab.Half> slabs() {
        return Collections.unmodifiableMap(SLABS);
    }

    public static Map<String, BlockPrismarineSlab.Double> doubleSlabs() {
        return Collections.unmodifiableMap(DOUBLES);
    }

    public static Map<String, BlockPrismarineStairs> stairs() {
        return Collections.unmodifiableMap(STAIRS);
    }

    public static List<ItemWood> woods() {
        return Collections.unmodifiableList(WOODS);
    }

    @Nullable
    static ItemWood wood(BlockPlanks.EnumType type) {
        for (ItemWood wood : WOODS) {
            if (wood.type == type) {
                return wood;
            }
        }
        return null;
    }

    private static IBlockState prismarine(int meta) {
        return Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.byMetadata(meta));
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();
        for (Map.Entry<String, Integer> p : PRISMARINES.entrySet()) {
            IBlockState base = prismarine(p.getValue());
            STAIRS.put(p.getKey(), Content.block(registry, p.getKey() + "_stairs", new BlockPrismarineStairs(base), "pickaxe"));
            BlockPrismarineSlab.Half half = Content.block(registry, p.getKey() + "_slab", new BlockPrismarineSlab.Half(base), "pickaxe");
            BlockPrismarineSlab.Double full = Content.block(registry, p.getKey() + "_double_slab",
                    new BlockPrismarineSlab.Double(base, half), "pickaxe");
            full.setCreativeTab(null);
            SLABS.put(p.getKey(), half);
            DOUBLES.put(p.getKey(), full);
        }
        carvedPumpkin = Content.block(registry, "carved_pumpkin", new BlockCarvedPumpkin(), "axe");
        shulkerBox = Content.block(registry, "shulker_box", new BlockUndyedShulkerBox(), "pickaxe");
        GameRegistry.registerTileEntity(TileEntityUndyedShulkerBox.class, Content.id("shulker_box"));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        for (String name : PRISMARINES.keySet()) {
            Content.registerBlockItem(registry, STAIRS.get(name));
            BlockPrismarineSlab.Half half = SLABS.get(name);
            Content.blockItem(registry, half, new ItemSlab(half, half, DOUBLES.get(name)));
        }
        ItemCarvedPumpkin carved = Content.blockItem(registry, carvedPumpkin, new ItemCarvedPumpkin(carvedPumpkin));
        shulkerBoxItem = Content.blockItem(registry, shulkerBox, new ItemShulkerBox(shulkerBox));

        for (BlockPlanks.EnumType type : BlockPlanks.EnumType.values()) {
            ItemWood wood = Content.item(registry, type.getName() + "_wood", new ItemWood(type));
            WOODS.add(wood);
            // 1.13's logs tag holds the wood blocks too.
            OreDictionary.registerOre("logWood", wood);
        }
        debugStick = Content.item(registry, "debug_stick", new ItemDebugStick());
        debugStick.setCreativeTab(null);

        dispensers(carved);
    }

    /**
     * Dispensers: a carved pumpkin goes onto a golem's body (building it) or onto a mob's head, as 1.12.2's
     * pumpkin did; the plain pumpkin is just dropped now (1.13); the undyed box is placed as the dyed
     * ones are, by vanilla's own shulker box behavior.
     */
    private static void dispensers(Item carved) {
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(carved, new Bootstrap.BehaviorDispenseOptional() {
            @Override
            protected ItemStack dispenseStack(IBlockSource source, ItemStack stack) {
                World world = source.getWorld();
                BlockPos pos = source.getBlockPos().offset(source.getBlockState().getValue(BlockDispenser.FACING));
                this.successful = true;
                if (world.isAirBlock(pos) && carvedPumpkin.canDispenserPlace(world, pos)) {
                    if (!world.isRemote) {
                        world.setBlockState(pos, carvedPumpkin.getDefaultState(), 3);
                    }
                    stack.shrink(1);
                } else if (ItemArmor.dispenseArmor(source, stack).isEmpty()) {
                    this.successful = false;
                }
                return stack;
            }
        });
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(Item.getItemFromBlock(Blocks.PUMPKIN), new BehaviorDefaultDispenseItem());
        IBehaviorDispenseItem box = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Item.getItemFromBlock(Blocks.PURPLE_SHULKER_BOX));
        if (box != null) {
            BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(shulkerBoxItem, box);
        }
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        ResourceLocation id = Content.id("block.pumpkin.carve");
        carveSound = new SoundEvent(id).setRegistryName(id);
        event.getRegistry().register(carveSound);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        for (Map.Entry<String, Integer> p : PRISMARINES.entrySet()) {
            Ingredient in = Ingredient.fromStacks(new ItemStack(Blocks.PRISMARINE, 1, p.getValue()));
            shaped(registry, p.getKey() + "_slab", "", new ItemStack(SLABS.get(p.getKey()), 6), 3, 1, in, in, in);
            shaped(registry, p.getKey() + "_stairs", "", new ItemStack(STAIRS.get(p.getKey()), 4), 3, 3,
                    in, Ingredient.EMPTY, Ingredient.EMPTY,
                    in, in, Ingredient.EMPTY,
                    in, in, in);
        }

        Ingredient ice = Ingredient.fromStacks(new ItemStack(Blocks.ICE));
        shaped(registry, "packed_ice", "", new ItemStack(Blocks.PACKED_ICE), 3, 3, ice, ice, ice, ice, ice, ice, ice, ice, ice);

        ItemStack charcoal = new ItemStack(Items.COAL, 1, 1);
        for (ItemWood wood : WOODS) {
            Ingredient log = Ingredient.fromStacks(ItemWood.log(wood.type, 1));
            shaped(registry, wood.type.getName() + "_wood", "bark", new ItemStack(wood, 3), 2, 2, log, log, log, log);
            ShapelessRecipes planks = new ShapelessRecipes("planks", new ItemStack(Blocks.PLANKS, 4, wood.type.getMetadata()),
                    NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(wood)));
            planks.setRegistryName(Content.id("blocks/" + wood.type.getName() + "_planks_from_wood"));
            registry.register(planks);
            FurnaceRecipes.instance().addSmelting(wood, charcoal.copy(), 0.15F);
        }

        jackOLanternFromCarved(registry);

        // 1.12.2's shells-and-chest recipe made a purple box; 1.13's makes the undyed one. Replaced under
        // vanilla's name so the recipe book and its unlocking advancement still find it. Dyeing (any box
        // plus a dye, vanilla's special recipe) already takes the undyed box, since it is a shulker box.
        Ingredient shell = Ingredient.fromItem(Items.SHULKER_SHELL);
        ShapedRecipes box = new ShapedRecipes("", 1, 3,
                NonNullList.from(Ingredient.EMPTY, shell, Ingredient.fromStacks(new ItemStack(Blocks.CHEST)), shell),
                new ItemStack(shulkerBox));
        box.setRegistryName(new ResourceLocation("minecraft", "purple_shulker_box"));
        registry.register(box);
    }

    /**
     * 1.13's jack o'lantern is a carved pumpkin and a torch. Vanilla's recipe is edited in place, as the
     * family narrows recipes: only the pumpkin slot changes, and the recipe keeps its name and advancement.
     */
    private static void jackOLanternFromCarved(IForgeRegistry<IRecipe> registry) {
        IRecipe recipe = registry.getValue(new ResourceLocation("minecraft", "lit_pumpkin"));
        if (!(recipe instanceof ShapedRecipes)) {
            UpdateAquaticBackport.LOGGER.warn("minecraft:lit_pumpkin is not vanilla's recipe; jack o'lanterns are left as they are.");
            return;
        }
        NonNullList<Ingredient> slots = ((ShapedRecipes) recipe).recipeItems;
        ItemStack pumpkin = new ItemStack(Blocks.PUMPKIN);
        Ingredient carved = Ingredient.fromStacks(new ItemStack(carvedPumpkin));
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i) != Ingredient.EMPTY && slots.get(i).apply(pumpkin)) {
                slots.set(i, carved);
            }
        }
    }

    private static void shaped(IForgeRegistry<IRecipe> registry, String name, String group, ItemStack result,
                               int width, int height, Ingredient... slots) {
        ShapedRecipes recipe = new ShapedRecipes(group, width, height, NonNullList.from(Ingredient.EMPTY, slots), result);
        recipe.setRegistryName(Content.id("blocks/" + name));
        registry.register(recipe);
    }
}
