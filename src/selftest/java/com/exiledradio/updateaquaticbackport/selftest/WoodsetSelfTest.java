package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.ModCreativeTab;
import com.exiledradio.updateaquaticbackport.common.Owners;
import com.exiledradio.updateaquaticbackport.woodset.AxeStripping;
import com.exiledradio.updateaquaticbackport.woodset.BlockSeaPickle;
import com.exiledradio.updateaquaticbackport.woodset.Wood;
import com.exiledradio.updateaquaticbackport.woodset.WoodsetContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPressurePlate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The copies of the owner's blocks: registration, every state, crafting, smelting, stripping, redstone,
 * blue ice and sea pickles, and the remap of a world that had the owner. Ported from the owner's own test
 * so both are held to the same checks.
 */
public final class WoodsetSelfTest implements SelfTest {

    @Override
    public String name() {
        return "woodset";
    }

    private static Block block(String name) {
        return ForgeRegistries.BLOCKS.getValue(Content.id(name));
    }

    private static Item item(String name) {
        return ForgeRegistries.ITEMS.getValue(Content.id(name));
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
            ownerPresent(c);
            return;
        }
        c.ok(WoodsetContent.enabled(), "copies are registered when the owner is absent");

        // 1. registration -----------------------------------------------------------------------
        List<String> names = new ArrayList<String>();
        for (Wood w : Wood.values()) {
            names.add("stripped_" + w.name + "_log");
            names.add("stripped_" + w.name + "_wood");
        }
        for (Wood w : Wood.values()) {
            if (w.hasOwnRedstone()) {
                names.add(w.name + "_trapdoor");
                names.add(w.name + "_button");
                names.add(w.name + "_pressure_plate");
            }
        }
        names.add("blue_ice");
        names.add("sea_pickle");
        for (String n : names) {
            Block b = block(n);
            c.ok(b != null && Content.id(n).equals(b.getRegistryName()), "block " + n);
            c.ok(item(n) instanceof ItemBlock && Item.getItemFromBlock(b) == item(n), "item " + n);
            c.ok(b != null && b.getCreativeTab() == ModCreativeTab.TAB, "creative tab " + n);
        }
        c.eq(29, WoodsetContent.blocks().size(), "block count");
        for (Block b : WoodsetContent.blocks()) {
            for (IBlockState s : b.getBlockState().getValidStates()) {
                c.ok(b.getStateFromMeta(b.getMetaFromState(s)) == s, "meta round trip " + s);
            }
        }

        // 2. crafting ----------------------------------------------------------------------------
        for (Wood w : Wood.values()) {
            ItemStack log = new ItemStack(block("stripped_" + w.name + "_log"));
            ItemStack bark = new ItemStack(block("stripped_" + w.name + "_wood"));
            ItemStack planks4 = new ItemStack(Blocks.PLANKS, 4, w.planksMeta());
            Crafting.expect(c, Crafting.grid(1, 1, log), planks4, w.name + " planks from stripped log");
            Crafting.expect(c, Crafting.grid(1, 1, bark), planks4, w.name + " planks from stripped wood");
            Crafting.expect(c, Crafting.grid(2, 2, log, log, log, log), new ItemStack(bark.getItem(), 3), w.name + " stripped wood");
            ItemStack p = new ItemStack(Blocks.PLANKS, 1, w.planksMeta());
            if (w.hasOwnRedstone()) {
                Crafting.expect(c, Crafting.grid(3, 2, p, p, p, p, p, p), new ItemStack(block(w.name + "_trapdoor"), 2), w.name + " trapdoor");
                Crafting.expect(c, Crafting.grid(1, 1, p), new ItemStack(block(w.name + "_button")), w.name + " button");
                Crafting.expect(c, Crafting.grid(2, 1, p, p), new ItemStack(block(w.name + "_pressure_plate")), w.name + " pressure plate");
            } else {
                Crafting.expect(c, Crafting.grid(3, 2, p, p, p, p, p, p), new ItemStack(Blocks.TRAPDOOR, 2), "oak trapdoor");
                Crafting.expect(c, Crafting.grid(1, 1, p), new ItemStack(Blocks.WOODEN_BUTTON), "oak button");
                Crafting.expect(c, Crafting.grid(2, 1, p, p), new ItemStack(Blocks.WOODEN_PRESSURE_PLATE), "oak pressure plate");
            }
        }
        for (String r : new String[] {"trapdoor", "wooden_button", "wooden_pressure_plate"}) {
            c.ok(ForgeRegistries.RECIPES.getValue(new ResourceLocation("minecraft", r)) != null, "vanilla recipe kept " + r);
        }
        ItemStack pi = new ItemStack(Blocks.PACKED_ICE);
        Crafting.expect(c, Crafting.grid(3, 3, pi, pi, pi, pi, pi, pi, pi, pi, pi), new ItemStack(block("blue_ice")), "blue ice");

        // 3. smelting, fuel, fire, ore dictionary ------------------------------------------------
        for (Wood w : Wood.values()) {
            for (String kind : new String[] {"_log", "_wood"}) {
                Block b = block("stripped_" + w.name + kind);
                c.ok(Crafting.same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(b)), new ItemStack(Items.COAL, 1, 1)), "charcoal " + b);
                c.eq(300, TileEntityFurnace.getItemBurnTime(new ItemStack(b)), "fuel " + b);
                c.ok(Blocks.FIRE.getFlammability(b) == 5 && Blocks.FIRE.getEncouragement(b) == 5, "flammable " + b);
                c.ok(Crafting.containsItem(OreDictionary.getOres("logWood"), new ItemStack(b)), "logWood " + b);
            }
            if (w.hasOwnRedstone()) {
                c.eq(100, TileEntityFurnace.getItemBurnTime(new ItemStack(block(w.name + "_button"))), "fuel button " + w.name);
                c.eq(300, TileEntityFurnace.getItemBurnTime(new ItemStack(block(w.name + "_trapdoor"))), "fuel trapdoor " + w.name);
                c.eq(300, TileEntityFurnace.getItemBurnTime(new ItemStack(block(w.name + "_pressure_plate"))), "fuel plate " + w.name);
            }
        }
        c.ok(Crafting.same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(block("sea_pickle"))), new ItemStack(Items.DYE, 1, 10)), "pickle -> lime dye");

        BlockPos origin = c.area().up();
        EntityPlayerMP player = Players.real(world, "woodset");

        // 4. axe stripping, through the server's real right-click path -------------------------
        player.interactionManager.setGameType(GameType.SURVIVAL);
        int i = 0;
        for (Block logBlock : new Block[] {Blocks.LOG, Blocks.LOG2}) {
            for (IBlockState s : logBlock.getBlockState().getValidStates()) {
                BlockPlanks.EnumType type = logBlock == Blocks.LOG ? s.getValue(BlockOldLog.VARIANT) : s.getValue(BlockNewLog.VARIANT);
                Wood w = Wood.of(type);
                BlockLog.EnumAxis axis = s.getValue(BlockLog.LOG_AXIS);
                BlockPos at = origin.add(i % 16 * 2, 0, i / 16 * 2);
                i++;
                world.setBlockState(at, s);
                ItemStack axe = new ItemStack(Items.IRON_AXE);
                player.setHeldItem(EnumHand.MAIN_HAND, axe);
                EnumActionResult r = player.interactionManager.processRightClickBlock(player, world, axe, EnumHand.MAIN_HAND, at,
                        EnumFacing.UP, 0.5F, 1.0F, 0.5F);
                IBlockState got = world.getBlockState(at);
                Block want = block("stripped_" + w.name + (axis == BlockLog.EnumAxis.NONE ? "_wood" : "_log"));
                BlockLog.EnumAxis wantAxis = axis == BlockLog.EnumAxis.NONE ? BlockLog.EnumAxis.Y : axis;
                c.eq(EnumActionResult.SUCCESS, r, "strip result " + s);
                c.ok(got.getBlock() == want && got.getValue(BlockLog.LOG_AXIS) == wantAxis, "strip " + s + " -> " + got);
                c.eq(1, axe.getItemDamage(), "axe damage");
                click(player, world, axe, at);
                c.ok(world.getBlockState(at) == got && axe.getItemDamage() == 1, "strip twice " + got);
            }
        }
        c.eq(24, i, "strip cases");
        BlockPos at = origin.add(0, 0, 8);
        IBlockState spruce = Blocks.LOG.getDefaultState().withProperty(BlockOldLog.VARIANT, BlockPlanks.EnumType.SPRUCE)
                .withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.X);
        world.setBlockState(at, spruce);
        ItemStack stick = new ItemStack(Items.STICK);
        player.setHeldItem(EnumHand.MAIN_HAND, stick);
        click(player, world, stick, at);
        c.ok(world.getBlockState(at) == spruce, "stick does not strip");
        ItemStack gold = new ItemStack(Items.GOLDEN_AXE);
        player.setHeldItem(EnumHand.MAIN_HAND, gold);
        ModConfig.woodset.axeStripping = false;
        click(player, world, gold, at);
        c.ok(world.getBlockState(at) == spruce, "axeStripping=false");
        ModConfig.woodset.axeStripping = true;
        click(player, world, gold, at);
        c.ok(world.getBlockState(at).getBlock() == block("stripped_spruce_log")
                && world.getBlockState(at).getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.X, "golden axe strips");
        c.ok(AxeStripping.isAxe(new ItemStack(Items.DIAMOND_AXE)) && !AxeStripping.isAxe(new ItemStack(Items.IRON_PICKAXE)), "is an axe");
        c.ok(AxeStripping.stripped(Blocks.PLANKS.getDefaultState()) == null, "planks not strippable");

        // 5. logs place with an axis and are wood ------------------------------------------------
        at = origin.add(0, 0, 10);
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        Block slog = block("stripped_birch_log");
        ItemStack held = new ItemStack(slog);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.EAST, 1.0F, 0.5F, 0.5F);
        IBlockState placed = world.getBlockState(at.east());
        c.ok(placed.getBlock() == slog && placed.getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.X, "log placed on east face " + placed);
        c.ok(slog.isWood(world, at.east()) && slog.canSustainLeaves(placed, world, at.east()), "log is wood");
        c.eq("axe", slog.getHarvestTool(placed), "log harvest tool");
        c.eq(BlockLog.EnumAxis.Z, placed.withRotation(Rotation.CLOCKWISE_90).getValue(BlockLog.LOG_AXIS), "log rotates");

        // 6. trapdoors, buttons and plates behave as the oak ones -------------------------------
        for (Wood w : Wood.values()) {
            if (!w.hasOwnRedstone()) {
                continue;
            }
            BlockPos row = origin.add(0, 0, 12 + w.ordinal() * 3);
            trapdoor(c, world, player, row, block(w.name + "_trapdoor"), w.name);
            trapdoor(c, world, player, row.add(6, 0, 0), Blocks.TRAPDOOR, "oak");
            button(c, world, player, row.add(12, 0, 0), block(w.name + "_button"), w.name);
            plate(c, world, row.add(18, 0, 0), block(w.name + "_pressure_plate"), w.name);
            c.eq(Blocks.WOODEN_BUTTON.tickRate(world), block(w.name + "_button").tickRate(world), "button tick rate " + w.name);
        }

        // 7. blue ice -----------------------------------------------------------------------------
        Block blue = block("blue_ice");
        BlockPos area = c.area().up();
        world.setBlockState(area, blue.getDefaultState());
        c.ok(Math.abs(blue.getSlipperiness(blue.getDefaultState(), world, area, null) - 0.989F) < 1e-6, "blue ice slipperiness");
        c.ok(blue.getDrops(world, area, blue.getDefaultState(), 0).isEmpty(), "blue ice drops nothing");
        c.ok(blue.canSilkHarvest(world, area, blue.getDefaultState(), player), "blue ice silk touch");
        c.ok(!blue.getTickRandomly() && blue.getDefaultState().getMaterial() == Material.PACKED_ICE, "blue ice never melts");
        c.eq("pickaxe", blue.getHarvestTool(blue.getDefaultState()), "blue ice pickaxe");

        // 8. sea pickles ----------------------------------------------------------------------------
        Block pickle = block("sea_pickle");
        Item pickleItem = Item.getItemFromBlock(pickle);
        at = area.add(4, 0, 0);
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        ItemStack pickles = new ItemStack(pickleItem, 8);
        player.setHeldItem(EnumHand.MAIN_HAND, pickles);
        pickleItem.onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        BlockPos p = at.up();
        IBlockState ps = world.getBlockState(p);
        c.ok(ps.getBlock() == pickle && ps.getValue(BlockSeaPickle.PICKLES) == 1 && !ps.getValue(BlockSeaPickle.WATERLOGGED), "pickle placed dry " + ps);
        c.eq(0, ps.getLightValue(world, p), "dry pickle dark");
        for (int n = 2; n <= 5; n++) {
            boolean added = pickle.onBlockActivated(world, p, world.getBlockState(p), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
            c.eq(n <= 4, added, "add pickle " + n);
        }
        c.eq(4, world.getBlockState(p).getValue(BlockSeaPickle.PICKLES), "four pickles");
        world.setBlockState(p.north(), Blocks.WATER.getDefaultState(), 3);
        ps = world.getBlockState(p);
        c.ok(ps.getValue(BlockSeaPickle.WATERLOGGED) && ps.getLightValue(world, p) == 15, "water beside lights " + ps);
        world.setBlockToAir(p.north());
        c.ok(!world.getBlockState(p).getValue(BlockSeaPickle.WATERLOGGED), "drained goes dark");
        for (int n = 1; n <= 4; n++) {
            IBlockState wet = pickle.getDefaultState().withProperty(BlockSeaPickle.PICKLES, n).withProperty(BlockSeaPickle.WATERLOGGED, true);
            c.eq(3 + 3 * n, wet.getLightValue(), "light " + n);
        }
        world.setBlockToAir(at);
        c.ok(world.isAirBlock(p), "pickle pops without support");
        c.eq(4, countDrops(world, p, pickleItem), "pickle drops 4");

        // 9. a world that had the owner's blocks, opened without it ------------------------------
        @SuppressWarnings("unchecked")
        IForgeRegistry<Block> blocks = (IForgeRegistry<Block>) (Object) ForgeRegistries.BLOCKS;
        RegistryEvent.MissingMappings.Mapping<Block> m = new RegistryEvent.MissingMappings.Mapping<Block>(blocks, blocks,
                new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "stripped_spruce_log"), 4000);
        RegistryEvent.MissingMappings.Mapping<Block> other = new RegistryEvent.MissingMappings.Mapping<Block>(blocks, blocks,
                new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "lectern"), 4001);
        WoodsetContent.remapOwner(new RegistryEvent.MissingMappings<Block>(new ResourceLocation("minecraft", "blocks"), blocks,
                java.util.Arrays.asList(m, other)));
        c.eq(RegistryEvent.MissingMappings.Action.REMAP, m.getAction(), "owner's stripped log remapped");
        c.ok(m.getTarget() == block("stripped_spruce_log"), "remapped onto ours");
        c.ok(other.getAction() != RegistryEvent.MissingMappings.Action.REMAP, "owner's other blocks left alone");
    }

    /** With the owner installed: no copies, every lookup is the owner's, and stripping makes the owner's. */
    private static void ownerPresent(Check c) {
        c.ok(!WoodsetContent.enabled(), "no copies with the owner installed");
        c.eq(0, WoodsetContent.blocks().size(), "nothing registered");
        for (String n : new String[] {"stripped_oak_log", "spruce_trapdoor", "dark_oak_button", "blue_ice", "sea_pickle"}) {
            c.ok(Owners.ourBlock(n) == null, "no copy of " + n);
            Block owner = Owners.block(Owners.VILLAGE_AND_PILLAGE, n);
            c.ok(owner != null && Owners.VILLAGE_AND_PILLAGE.equals(owner.getRegistryName().getNamespace()), "lookup gives the owner's " + n);
        }
        BlockPos at = c.area().up();
        WorldServer world = c.world;
        world.setBlockState(at, Blocks.LOG.getDefaultState());
        EntityPlayerMP player = Players.real(world, "woodsetowner");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setHeldItem(EnumHand.MAIN_HAND, axe);
        click(player, world, axe, at);
        IBlockState got = world.getBlockState(at);
        c.eq(new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "stripped_oak_log"), got.getBlock().getRegistryName(), "the owner strips logs");
        c.eq(1, axe.getItemDamage(), "stripped once, not twice");
    }

    private static void click(EntityPlayerMP player, WorldServer world, ItemStack stack, BlockPos at) {
        player.interactionManager.processRightClickBlock(player, world, stack, EnumHand.MAIN_HAND, at, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
    }

    private static void trapdoor(Check c, WorldServer world, EntityPlayerMP player, BlockPos at, Block trapdoor, String what) {
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        ItemStack held = new ItemStack(trapdoor);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.SOUTH, 0.5F, 0.75F, 1.0F);
        BlockPos t = at.south();
        IBlockState s = world.getBlockState(t);
        c.ok(s.getBlock() == trapdoor && s.getValue(BlockTrapDoor.FACING) == EnumFacing.SOUTH
                && s.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.TOP && !s.getValue(BlockTrapDoor.OPEN), what + " trapdoor placed " + s);
        trapdoor.onBlockActivated(world, t, s, player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.ok(world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor opens by hand");
        trapdoor.onBlockActivated(world, t, world.getBlockState(t), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        world.setBlockState(t.south(), Blocks.REDSTONE_BLOCK.getDefaultState(), 3);
        c.ok(world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor opens by redstone");
        world.setBlockToAir(t.south());
        c.ok(!world.getBlockState(t).getValue(BlockTrapDoor.OPEN), what + " trapdoor closes without redstone");
    }

    private static void button(Check c, WorldServer world, EntityPlayerMP player, BlockPos at, Block button, String what) {
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        ItemStack held = new ItemStack(button);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, at, EnumHand.MAIN_HAND, EnumFacing.EAST, 1.0F, 0.5F, 0.5F);
        BlockPos b = at.east();
        IBlockState s = world.getBlockState(b);
        c.ok(s.getBlock() == button && s.getValue(BlockButton.FACING) == EnumFacing.EAST && !s.getValue(BlockButton.POWERED), what + " button placed " + s);
        button.onBlockActivated(world, b, s, player, EnumHand.MAIN_HAND, EnumFacing.EAST, 0.5F, 0.5F, 0.5F);
        s = world.getBlockState(b);
        c.ok(s.getValue(BlockButton.POWERED) && s.getWeakPower(world, b, EnumFacing.EAST) == 15 && world.isBlockPowered(at), what + " button powers");
        c.ok(world.isUpdateScheduled(b, button), what + " button scheduled release");
    }

    private static void plate(Check c, WorldServer world, BlockPos at, Block plate, String what) {
        world.setBlockState(at, Blocks.STONE.getDefaultState());
        world.setBlockState(at.up(), plate.getDefaultState());
        EntityItem item = new EntityItem(world, at.getX() + 0.5, at.getY() + 1.05, at.getZ() + 0.5, new ItemStack(Items.STICK));
        world.spawnEntity(item);
        plate.onEntityCollision(world, at.up(), world.getBlockState(at.up()), item);
        IBlockState s = world.getBlockState(at.up());
        c.ok(s.getValue(BlockPressurePlate.POWERED) && world.isBlockPowered(at), what + " plate pressed by an item " + s);
        item.setDead();
    }

    static int countDrops(WorldServer world, BlockPos at, Item want) {
        int n = 0;
        for (EntityItem e : world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(at).grow(2))) {
            if (e.getItem().getItem() == want) {
                n += e.getItem().getCount();
            }
            e.setDead();
        }
        return n;
    }

    @SuppressWarnings("unused")
    private static final List<String> UNUSED = Collections.emptyList();
}
