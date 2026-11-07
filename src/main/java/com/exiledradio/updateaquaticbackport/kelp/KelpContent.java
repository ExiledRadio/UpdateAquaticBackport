package com.exiledradio.updateaquaticbackport.kelp;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Compat;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.LegacyWater;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.init.Items;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;

/**
 * Kelp, dried kelp, the dried kelp block and seagrass.
 *
 * <p>Registers the blocks and items, kelp's smelting, the dried kelp block's recipes and fuel, compost
 * values for a composter if one is installed, and the generator ({@link KelpGenerator}). Underwater bone
 * meal is {@link UnderwaterBonemeal}. An empty bucket takes nothing from kelp or seagrass ({@link #onBucket}). Off ({@code kelp.enabled}), nothing is registered and everything
 * here does nothing.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class KelpContent {

    /** 1.14.4's dried kelp block burn time: 20 items, plus the tick that makes it exactly 20. */
    public static final int DRIED_KELP_BLOCK_BURN_TIME = 4001;

    private static BlockKelp kelp;
    private static BlockKelp kelpPlant;
    private static BlockSeagrass seagrass;
    private static BlockSeagrass tallSeagrass;
    private static Block driedKelpBlock;
    private static Item driedKelp;

    private KelpContent() {
    }

    public static boolean enabled() {
        return ModConfig.kelp.enabled;
    }

    @Nullable
    public static BlockKelp kelp() {
        return kelp;
    }

    @Nullable
    public static BlockKelp kelpPlant() {
        return kelpPlant;
    }

    @Nullable
    public static BlockSeagrass seagrass() {
        return seagrass;
    }

    @Nullable
    public static BlockSeagrass tallSeagrass() {
        return tallSeagrass;
    }

    @Nullable
    public static Block driedKelpBlock() {
        return driedKelpBlock;
    }

    @Nullable
    public static Item driedKelp() {
        return driedKelp;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> registry = event.getRegistry();
        kelp = Content.block(registry, "kelp", new BlockKelp(true), null);
        kelpPlant = Content.block(registry, "kelp_plant", new BlockKelp(false), null);
        kelp.other = kelpPlant;
        kelpPlant.other = kelp;
        kelpPlant.setCreativeTab(null);

        seagrass = Content.block(registry, "seagrass", new BlockSeagrass(false), null);
        tallSeagrass = Content.block(registry, "tall_seagrass", new BlockSeagrass(true), null);
        seagrass.shortForm = seagrass;
        seagrass.tallForm = tallSeagrass;
        tallSeagrass.shortForm = seagrass;
        tallSeagrass.tallForm = tallSeagrass;
        tallSeagrass.setCreativeTab(null);
        for (Block block : new Block[] {kelp, kelpPlant, seagrass, tallSeagrass}) {
            Content.multipart(block);
            LegacyWater.always(block);
        }

        // 1.14.4: organic, green on maps, hardness 0.5, blast resistance 2.5, plant sounds.
        driedKelpBlock = Content.block(registry, "dried_kelp_block", new Block(Material.GRASS, MapColor.GREEN) {
            {
                this.setSoundType(SoundType.PLANT);
            }
        }, null);
        driedKelpBlock.setHardness(0.5F);
        // 1.12.2 multiplies by 3 and divides by 5 when asked; 1.14's 2.5 is 2.5 * 5 / 3 here.
        driedKelpBlock.setResistance(2.5F * 5.0F / 3.0F);

        // 1.14.4 FireBlock: catches like hay, burns away quicker.
        net.minecraft.init.Blocks.FIRE.setFireInfo(driedKelpBlock, 30, 60);
        GameRegistry.registerWorldGenerator(new KelpGenerator(), 5);
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> registry = event.getRegistry();
        // Kelp and seagrass items place their block; tall seagrass and the kelp plant have no item.
        Content.registerBlockItem(registry, kelp);
        Content.registerBlockItem(registry, seagrass);
        Content.registerBlockItem(registry, driedKelpBlock);
        driedKelp = Content.item(registry, "dried_kelp", new ItemFood(1, 0.3F, false) {
            /** 1.13: eaten in 16 ticks, twice as fast as other food. */
            @Override
            public int getMaxItemUseDuration(ItemStack stack) {
                return 16;
            }
        });

        Compat.compost(0.3F, Item.getItemFromBlock(kelp));
        Compat.compost(0.3F, Item.getItemFromBlock(seagrass));
        Compat.compost(0.3F, driedKelp);
        Compat.compost(0.5F, Item.getItemFromBlock(driedKelpBlock));
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<IRecipe> registry = event.getRegistry();
        Ingredient dried = Ingredient.fromItem(driedKelp);
        ShapedRecipes block = new ShapedRecipes("", 3, 3,
                NonNullList.from(Ingredient.EMPTY, dried, dried, dried, dried, dried, dried, dried, dried, dried),
                new ItemStack(driedKelpBlock));
        block.setRegistryName(Content.id("kelp/dried_kelp_block"));
        registry.register(block);
        ShapelessRecipes back = new ShapelessRecipes("", new ItemStack(driedKelp, 9),
                NonNullList.from(Ingredient.EMPTY, Ingredient.fromItem(Item.getItemFromBlock(driedKelpBlock))));
        back.setRegistryName(Content.id("kelp/dried_kelp_from_block"));
        registry.register(back);

        FurnaceRecipes.instance().addSmelting(Item.getItemFromBlock(kelp), new ItemStack(driedKelp), 0.1F);
    }

    /**
     * An empty bucket aimed at kelp or seagrass does nothing, as today's: they hold water but are not a bucket's
     * to take (1.14.4's kelp and seagrass are not {@code IBucketPickupHandler}s). Without this, Fluidlogged API
     * would drain them, and a block that needs water would go with it.
     */
    @SubscribeEvent
    public static void onBucket(FillBucketEvent event) {
        RayTraceResult hit = event.getTarget();
        if (!enabled() || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK
                || event.getEmptyBucket().getItem() != Items.BUCKET) {
            return;
        }
        Block block = event.getWorld().getBlockState(hit.getBlockPos()).getBlock();
        if (block instanceof BlockKelp || block instanceof BlockSeagrass) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fuel(FurnaceFuelBurnTimeEvent event) {
        if (enabled() && !event.getItemStack().isEmpty()
                && event.getItemStack().getItem() == Item.getItemFromBlock(driedKelpBlock)) {
            event.setBurnTime(DRIED_KELP_BLOCK_BURN_TIME);
        }
    }
}
