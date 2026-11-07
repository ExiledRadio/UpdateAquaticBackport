package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineSlab;
import com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineStairs;
import com.exiledradio.updateaquaticbackport.blocks.BlockUndyedShulkerBox;
import com.exiledradio.updateaquaticbackport.blocks.BlocksContent;
import com.exiledradio.updateaquaticbackport.blocks.BlocksEvents;
import com.exiledradio.updateaquaticbackport.blocks.ItemDebugStick;
import com.exiledradio.updateaquaticbackport.blocks.ItemWood;
import com.exiledradio.updateaquaticbackport.blocks.TileEntityUndyedShulkerBox;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockPrismarine;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSourceImpl;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.dispenser.BehaviorDefaultDispenseItem;
import net.minecraft.dispenser.IBehaviorDispenseItem;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

/**
 * Stream X: prismarine stairs and slabs, carving and the carved pumpkin, golems, the looser placement
 * rules, wood, packed ice, the debug stick, the undyed shulker box and cauldron washing.
 */
public final class BlocksSelfTest implements SelfTest {

    @Override
    public String name() {
        return "blocks";
    }

    @Override
    public void run(Check c) throws Exception {
        c.ok(BlocksContent.enabled(), "blocks stream on");
        registration(c);
        recipes(c);
        EntityPlayerMP player = Players.real(c.world, "blocks");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        slabs(c, player);
        carving(c, player);
        golems(c, player);
        placement(c, player);
        wood(c, player);
        debugStick(c, player);
        shulkerBox(c, player);
        mobsAndDispensers(c);
        PumpkinFollowupsTest.run(c);
    }

    // ------------------------------------------------------------------------------------------------

    private static List<Block> ours() {
        List<Block> blocks = new ArrayList<Block>();
        blocks.addAll(BlocksContent.stairs().values());
        blocks.addAll(BlocksContent.slabs().values());
        blocks.addAll(BlocksContent.doubleSlabs().values());
        blocks.add(BlocksContent.carvedPumpkin());
        blocks.add(BlocksContent.shulkerBox());
        return blocks;
    }

    private static void registration(Check c) {
        c.eq(3, BlocksContent.stairs().size(), "3 prismarine stairs");
        c.eq(3, BlocksContent.slabs().size(), "3 prismarine slabs");
        c.eq(3, BlocksContent.doubleSlabs().size(), "3 double slabs");
        c.eq(6, BlocksContent.woods().size(), "6 wood items");
        c.ok(BlocksContent.debugStick() != null && BlocksContent.debugStick().getCreativeTab() == null, "debug stick, in no tab");
        for (Block b : ours()) {
            for (IBlockState s : b.getBlockState().getValidStates()) {
                if (b instanceof BlockPrismarineStairs && s.getValue(net.minecraft.block.BlockStairs.SHAPE)
                        != net.minecraft.block.BlockStairs.EnumShape.STRAIGHT) {
                    continue; // the shape is worked out from the neighbors, never stored
                }
                c.ok(b.getStateFromMeta(b.getMetaFromState(s)) == s, "meta round trip " + s);
            }
        }
        for (Block b : BlocksContent.doubleSlabs().values()) {
            c.eq(Items.AIR, Item.getItemFromBlock(b), "a double slab has no item: " + b.getRegistryName());
        }
        for (Block b : new ArrayList<Block>(BlocksContent.stairs().values())) {
            c.ok(Math.abs(b.getBlockHardness(b.getDefaultState(), c.world, BlockPos.ORIGIN) - 1.5F) < 1e-6
                    && Math.abs(b.getExplosionResistance(null) - 6.0F) < 1e-4, "stairs hardness 1.5, resistance 6: " + b.getRegistryName());
        }
        for (Block b : new ArrayList<Block>(BlocksContent.slabs().values())) {
            c.ok(Math.abs(b.getBlockHardness(b.getDefaultState(), c.world, BlockPos.ORIGIN) - 1.5F) < 1e-6
                    && Math.abs(b.getExplosionResistance(null) - 6.0F) < 1e-4, "slab hardness 1.5, resistance 6: " + b.getRegistryName());
            c.eq("pickaxe", b.getHarvestTool(b.getDefaultState()), "slab mined with a pickaxe");
        }
        Block slab = BlocksContent.slabs().get("prismarine");
        c.eq(MapColor.CYAN, slab.getDefaultState().getMapColor(c.world, BlockPos.ORIGIN), "prismarine slab cyan on maps");
        c.eq(MapColor.DIAMOND, BlocksContent.slabs().get("dark_prismarine").getDefaultState().getMapColor(c.world, BlockPos.ORIGIN),
                "dark prismarine slab diamond on maps");
        Block carved = BlocksContent.carvedPumpkin();
        c.eq(1.0F, carved.getBlockHardness(carved.getDefaultState(), c.world, BlockPos.ORIGIN), "carved pumpkin hardness 1");
        c.eq("axe", carved.getHarvestTool(carved.getDefaultState()), "carved pumpkin: axe");
        c.eq(EntityEquipmentSlot.HEAD, EntityLiving.getSlotForItemStack(new ItemStack(carved)), "carved pumpkin is worn on the head");
        c.eq(MapColor.PURPLE, BlocksContent.shulkerBox().getDefaultState().getMapColor(c.world, BlockPos.ORIGIN), "undyed box purple on maps");
        c.eq(1, new ItemStack(BlocksContent.shulkerBox()).getMaxStackSize(), "shulker box stacks to 1");
    }

    // ------------------------------------------------------------------------------------------------

    private static ItemStack prismarine(int meta) {
        return new ItemStack(Blocks.PRISMARINE, 1, meta);
    }

    private static void recipes(Check c) {
        ItemStack e = ItemStack.EMPTY;
        String[] names = {"prismarine", "prismarine_brick", "dark_prismarine"};
        int[] metas = {BlockPrismarine.ROUGH_META, BlockPrismarine.BRICKS_META, BlockPrismarine.DARK_META};
        for (int i = 0; i < 3; i++) {
            ItemStack p = prismarine(metas[i]);
            Crafting.expect(c, Crafting.grid(3, 1, p, p, p), new ItemStack(BlocksContent.slabs().get(names[i]), 6), "3 " + names[i] + " -> 6 slabs");
            Crafting.expect(c, Crafting.grid(3, 3, p, e, e, p, p, e, p, p, p), new ItemStack(BlocksContent.stairs().get(names[i]), 4),
                    "6 " + names[i] + " -> 4 stairs");
        }
        ItemStack ice = new ItemStack(Blocks.ICE);
        Crafting.expect(c, Crafting.grid(3, 3, ice, ice, ice, ice, ice, ice, ice, ice, ice), new ItemStack(Blocks.PACKED_ICE), "9 ice -> packed ice");

        for (ItemWood wood : BlocksContent.woods()) {
            BlockPlanks.EnumType type = type(wood);
            ItemStack log = type.getMetadata() < 4 ? new ItemStack(Blocks.LOG, 1, type.getMetadata())
                    : new ItemStack(Blocks.LOG2, 1, type.getMetadata() - 4);
            Crafting.expect(c, Crafting.grid(2, 2, log, log, log, log), new ItemStack(wood, 3), "4 " + type.getName() + " logs -> 3 wood");
            Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(wood)), new ItemStack(Blocks.PLANKS, 4, type.getMetadata()),
                    type.getName() + " wood -> 4 planks");
            ItemStack smelted = FurnaceRecipes.instance().getSmeltingResult(new ItemStack(wood));
            c.ok(smelted.getItem() == Items.COAL && smelted.getMetadata() == 1, type.getName() + " wood smelts to charcoal");
            c.eq(BlocksEvents.WOOD_BURN_TIME, TileEntityFurnace.getItemBurnTime(new ItemStack(wood)), "wood burns 300 ticks");
            boolean ore = false;
            for (ItemStack s : OreDictionary.getOres("logWood")) {
                ore |= s.getItem() == wood;
            }
            c.ok(ore, type.getName() + " wood is logWood");
        }

        ItemStack torch = new ItemStack(Blocks.TORCH);
        Crafting.expect(c, Crafting.grid(1, 2, new ItemStack(BlocksContent.carvedPumpkin()), torch), new ItemStack(Blocks.LIT_PUMPKIN),
                "carved pumpkin over a torch -> jack o'lantern");
        c.ok(net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(1, 2, new ItemStack(Blocks.PUMPKIN), torch), c.world)
                .isEmpty(), "a plain pumpkin makes no jack o'lantern");
        ItemStack shell = new ItemStack(Items.SHULKER_SHELL);
        Crafting.expect(c, Crafting.grid(1, 3, shell, new ItemStack(Blocks.CHEST), shell), new ItemStack(BlocksContent.shulkerBox()),
                "2 shells and a chest -> the undyed box");
        ItemStack dyed = net.minecraft.item.crafting.CraftingManager.findMatchingResult(
                Crafting.grid(2, 1, new ItemStack(BlocksContent.shulkerBox()), new ItemStack(Items.DYE, 1, EnumDyeColor.RED.getDyeDamage())), c.world);
        c.eq(Item.getItemFromBlock(Blocks.RED_SHULKER_BOX), dyed.getItem(), "the undyed box dyes red");
    }

    private static BlockPlanks.EnumType type(ItemWood wood) {
        IBlockState s = wood.state();
        return s.getBlock() == Blocks.LOG ? s.getValue(BlockOldLog.VARIANT) : s.getValue(BlockNewLog.VARIANT);
    }

    // ------------------------------------------------------------------------------------------------

    private static EnumActionResult click(EntityPlayerMP player, WorldServer world, ItemStack stack, BlockPos pos, EnumFacing face) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        float hx = face == EnumFacing.EAST ? 1.0F : face == EnumFacing.WEST ? 0.0F : 0.5F;
        float hy = face == EnumFacing.UP ? 1.0F : face == EnumFacing.DOWN ? 0.0F : 0.5F;
        float hz = face == EnumFacing.SOUTH ? 1.0F : face == EnumFacing.NORTH ? 0.0F : 0.5F;
        return player.interactionManager.processRightClickBlock(player, world, stack, EnumHand.MAIN_HAND, pos, face, hx, hy, hz);
    }

    private static void slabs(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos floor = c.area().add(4, -1, 4);
        BlockPrismarineSlab.Half slab = BlocksContent.slabs().get("prismarine_brick");
        click(player, world, new ItemStack(slab, 2), floor, EnumFacing.UP);
        IBlockState s = world.getBlockState(floor.up());
        c.ok(s.getBlock() == slab && s.getValue(BlockSlab.HALF) == BlockSlab.EnumBlockHalf.BOTTOM, "a slab placed on the floor is a bottom slab: " + s);
        click(player, world, player.getHeldItemMainhand(), floor.up(), EnumFacing.UP);
        c.eq(BlocksContent.doubleSlabs().get("prismarine_brick"), world.getBlockState(floor.up()).getBlock(), "a second slab makes the double");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        player.interactionManager.tryHarvestBlock(floor.up());
        c.eq(2, Drops.count(world, floor.up(), Item.getItemFromBlock(slab)), "a double slab drops two slabs");

        BlockPrismarineStairs stairs = BlocksContent.stairs().get("dark_prismarine");
        click(player, world, new ItemStack(stairs), floor.east(3), EnumFacing.UP);
        c.eq(stairs, world.getBlockState(floor.east(3).up()).getBlock(), "stairs place");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        player.interactionManager.tryHarvestBlock(floor.east(3).up());
        c.eq(1, Drops.count(world, floor.east(3).up(), Item.getItemFromBlock(stairs)), "stairs drop themselves");
    }

    private static void carving(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos p = c.area().add(4, 0, 4);
        Block carved = BlocksContent.carvedPumpkin();
        world.setBlockState(p, Blocks.PUMPKIN.getDefaultState(), 3);
        ItemStack shears = new ItemStack(Items.SHEARS);
        click(player, world, shears, p, EnumFacing.EAST);
        IBlockState s = world.getBlockState(p);
        c.ok(s.getBlock() == carved && s.getValue(BlockHorizontal.FACING) == EnumFacing.EAST, "shears on the east face carve it facing east: " + s);
        c.eq(4, Drops.count(world, p, Items.PUMPKIN_SEEDS), "carving drops 4 seeds");
        c.eq(1, shears.getItemDamage(), "the shears wear by 1");

        world.setBlockState(p, Blocks.PUMPKIN.getDefaultState(), 3);
        player.rotationYaw = 0.0F; // facing south
        click(player, world, shears, p, EnumFacing.UP);
        s = world.getBlockState(p);
        c.ok(s.getBlock() == carved && s.getValue(BlockHorizontal.FACING) == EnumFacing.NORTH, "carved on top, it faces the player: " + s);
        Drops.count(world, p, Items.PUMPKIN_SEEDS);
        click(player, world, shears, p, EnumFacing.EAST);
        c.eq(EnumFacing.NORTH, world.getBlockState(p).getValue(BlockHorizontal.FACING), "a carved pumpkin is not carved again");
        c.eq(0, Drops.count(world, p, Items.PUMPKIN_SEEDS), "and drops nothing");
    }

    private static int count(WorldServer world, Class<? extends Entity> type, BlockPos at) {
        int n = 0;
        for (Entity e : world.getEntitiesWithinAABB(type, new AxisAlignedBB(at).grow(4))) {
            if (!e.isDead) {
                n++;
            }
            e.setDead();
        }
        return n;
    }

    private static void golems(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos base = c.area().add(4, 0, 4);
        Block carved = BlocksContent.carvedPumpkin();

        // A carved pumpkin on two snow blocks.
        world.setBlockState(base, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(base.up(), Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(base.up(2), carved.getDefaultState(), 3);
        c.eq(1, count(world, EntitySnowman.class, base), "a carved pumpkin on snow makes a snow golem");
        c.ok(world.isAirBlock(base) && world.isAirBlock(base.up()) && world.isAirBlock(base.up(2)), "and uses up its blocks");

        // An iron golem.
        BlockPos iron = base.east(6);
        world.setBlockState(iron, Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up().east(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up().west(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up(2), carved.getDefaultState(), 3);
        c.eq(1, count(world, EntityIronGolem.class, iron), "a carved pumpkin on iron makes an iron golem");

        // A plain pumpkin placed by a player builds nothing.
        BlockPos plain = base.east(12);
        world.setBlockState(plain, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(plain.up(), Blocks.SNOW.getDefaultState(), 3);
        click(player, world, new ItemStack(Blocks.PUMPKIN), plain.up(), EnumFacing.UP);
        c.eq(Blocks.PUMPKIN, world.getBlockState(plain.up(2)).getBlock(), "a plain pumpkin is placed on snow");
        c.eq(0, count(world, EntitySnowman.class, plain), "and builds no golem");
        c.eq(Blocks.SNOW, world.getBlockState(plain).getBlock(), "the snow stays");
        c.ok(player.getHeldItemMainhand().isEmpty(), "the pumpkin was used");

        // A jack o'lantern still does (vanilla's own code).
        BlockPos lit = base.east(18);
        world.setBlockState(lit, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(lit.up(), Blocks.SNOW.getDefaultState(), 3);
        click(player, world, new ItemStack(Blocks.LIT_PUMPKIN), lit.up(), EnumFacing.UP);
        c.eq(1, count(world, EntitySnowman.class, lit), "a jack o'lantern on snow makes a snow golem");
        // A carved pumpkin placed by a player does too.
        BlockPos byHand = base.east(24);
        world.setBlockState(byHand, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(byHand.up(), Blocks.SNOW.getDefaultState(), 3);
        click(player, world, new ItemStack(carved), byHand.up(), EnumFacing.UP);
        c.eq(1, count(world, EntitySnowman.class, byHand), "a carved pumpkin placed by a player makes a snow golem");
    }

    private static void placement(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos corner = c.area();
        // A pillar standing in the air, clicked on its east face: nothing under the place.
        BlockPos pillar = corner.add(4, 5, 4);
        world.setBlockState(pillar, Blocks.STONE.getDefaultState(), 3);
        click(player, world, new ItemStack(Blocks.PUMPKIN), pillar, EnumFacing.EAST);
        c.eq(Blocks.PUMPKIN, world.getBlockState(pillar.east()).getBlock(), "a pumpkin needs no support");
        click(player, world, new ItemStack(Blocks.LIT_PUMPKIN), pillar, EnumFacing.WEST);
        c.eq(Blocks.LIT_PUMPKIN, world.getBlockState(pillar.west()).getBlock(), "nor does a jack o'lantern");
        click(player, world, new ItemStack(Blocks.OAK_FENCE_GATE), pillar, EnumFacing.NORTH);
        c.eq(Blocks.OAK_FENCE_GATE, world.getBlockState(pillar.north()).getBlock(), "nor does a fence gate");
        click(player, world, new ItemStack(Blocks.ACACIA_FENCE_GATE), pillar, EnumFacing.SOUTH);
        c.eq(Blocks.ACACIA_FENCE_GATE, world.getBlockState(pillar.south()).getBlock(), "nor any wood's fence gate");

        // The clicked block still takes the click: a trapdoor opens and nothing is placed beside it.
        BlockPos trap = corner.add(10, 5, 4);
        world.setBlockState(trap, Blocks.TRAPDOOR.getDefaultState(), 3);
        c.eq(EnumActionResult.SUCCESS, click(player, world, new ItemStack(Blocks.PUMPKIN), trap, EnumFacing.EAST), "the click succeeds");
        c.ok(world.getBlockState(trap).getValue(BlockTrapDoor.OPEN), "a trapdoor clicked with a pumpkin opens");
        c.ok(world.isAirBlock(trap.east()), "and no pumpkin is placed");

        // Switched off, 1.12.2's rule is back.
        ModConfig.blocks.placementRules = false;
        BlockPos off = corner.add(16, 5, 4);
        world.setBlockState(off, Blocks.STONE.getDefaultState(), 3);
        click(player, world, new ItemStack(Blocks.OAK_FENCE_GATE), off, EnumFacing.EAST);
        c.ok(world.isAirBlock(off.east()), "with placementRules off, a fence gate needs support again");
        world.setBlockState(corner.add(16, 0, 10), Blocks.STONE.getDefaultState(), 3);
        click(player, world, new ItemStack(Items.OAK_DOOR), corner.add(16, 0, 10), EnumFacing.EAST);
        c.ok(world.isAirBlock(corner.add(17, 0, 10)), "with placementRules off, a door on a side face is refused");
        ModConfig.blocks.placementRules = true;

        // Doors on a side face, standing on the floor.
        BlockPos wall = corner.add(4, 0, 10);
        world.setBlockState(wall, Blocks.STONE.getDefaultState(), 3);
        click(player, world, new ItemStack(Items.OAK_DOOR), wall, EnumFacing.EAST);
        IBlockState lower = world.getBlockState(wall.east());
        IBlockState upper = world.getBlockState(wall.east().up());
        c.ok(lower.getBlock() == Blocks.OAK_DOOR && lower.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.LOWER
                && upper.getBlock() == Blocks.OAK_DOOR && upper.getValue(BlockDoor.HALF) == BlockDoor.EnumDoorHalf.UPPER,
                "a door clicked on a side face stands in front of it: " + lower + " / " + upper);
        c.ok(player.getHeldItemMainhand().isEmpty(), "the door was used");
        // Not over nothing.
        click(player, world, new ItemStack(Items.SPRUCE_DOOR), pillar.up(2), EnumFacing.EAST);
        world.setBlockState(pillar.up(2), Blocks.STONE.getDefaultState(), 3);
        click(player, world, new ItemStack(Items.SPRUCE_DOOR), pillar.up(2), EnumFacing.EAST);
        c.ok(world.isAirBlock(pillar.up(2).east()), "a door over air is still refused");
    }

    private static void wood(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos floor = c.area().add(4, -1, 4);
        ItemWood oak = null;
        ItemWood darkOak = null;
        for (ItemWood w : BlocksContent.woods()) {
            if (type(w) == BlockPlanks.EnumType.OAK) {
                oak = w;
            } else if (type(w) == BlockPlanks.EnumType.DARK_OAK) {
                darkOak = w;
            }
        }
        click(player, world, new ItemStack(oak), floor, EnumFacing.UP);
        IBlockState s = world.getBlockState(floor.up());
        c.ok(s.getBlock() == Blocks.LOG && s.getValue(BlockOldLog.VARIANT) == BlockPlanks.EnumType.OAK
                && s.getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.NONE, "oak wood places vanilla's bark oak log: " + s);
        c.ok(player.getHeldItemMainhand().isEmpty(), "the wood was used");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.interactionManager.tryHarvestBlock(floor.up());
        c.eq(1, Drops.count(world, floor.up(), oak), "breaking it gives the wood back");

        click(player, world, new ItemStack(darkOak), floor.east(2), EnumFacing.UP);
        s = world.getBlockState(floor.east(2).up());
        c.ok(s.getBlock() == Blocks.LOG2 && s.getValue(BlockNewLog.VARIANT) == BlockPlanks.EnumType.DARK_OAK
                && s.getValue(BlockLog.LOG_AXIS) == BlockLog.EnumAxis.NONE, "dark oak wood places the bark dark oak log: " + s);
        ItemStack silk = new ItemStack(Items.IRON_AXE);
        silk.addEnchantment(Enchantments.SILK_TOUCH, 1);
        c.ok(EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, silk) == 1, "silk touch axe");
        player.setHeldItem(EnumHand.MAIN_HAND, silk);
        player.interactionManager.tryHarvestBlock(floor.east(2).up());
        c.eq(1, Drops.count(world, floor.east(2).up(), darkOak), "with Silk Touch too");

        world.setBlockState(floor.east(4).up(), Blocks.LOG.getDefaultState(), 3);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_AXE));
        player.interactionManager.tryHarvestBlock(floor.east(4).up());
        c.eq(1, Drops.count(world, floor.east(4).up(), Item.getItemFromBlock(Blocks.LOG)), "an ordinary log still drops a log");
    }

    private static void debugStick(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos p = c.area().add(4, 0, 4);
        Block carved = BlocksContent.carvedPumpkin();
        world.setBlockState(p, carved.getDefaultState().withProperty(BlockHorizontal.FACING, EnumFacing.NORTH), 3);
        ItemStack stick = new ItemStack(BlocksContent.debugStick());
        String id = String.valueOf(carved.getRegistryName());

        // Not allowed: nothing happens.
        player.interactionManager.setGameType(GameType.CREATIVE);
        click(player, world, stick, p, EnumFacing.UP);
        c.eq(EnumFacing.NORTH, world.getBlockState(p).getValue(BlockHorizontal.FACING), "a player who may not use command blocks changes nothing");

        world.getMinecraftServer().getPlayerList().addOp(player.getGameProfile());
        try {
            player.setHeldItem(EnumHand.MAIN_HAND, stick);
            player.interactionManager.onBlockClicked(p, EnumFacing.UP);
            c.eq(carved, world.getBlockState(p).getBlock(), "attacking with the stick breaks nothing, even in creative");
            NBTTagCompound tag = stick.getSubCompound("DebugProperty");
            c.ok(tag != null && "facing".equals(tag.getString(id)), "attacking selects the property: " + tag);
            click(player, world, stick, p, EnumFacing.UP);
            c.eq(EnumFacing.SOUTH, world.getBlockState(p).getValue(BlockHorizontal.FACING), "using it cycles north -> south");
            player.setSneaking(true);
            click(player, world, stick, p, EnumFacing.UP);
            player.setSneaking(false);
            c.eq(EnumFacing.NORTH, world.getBlockState(p).getValue(BlockHorizontal.FACING), "sneaking cycles back");

            // A log: two properties (axis, variant); two attacks select the second.
            BlockPos log = p.east(3);
            world.setBlockState(log, Blocks.LOG.getDefaultState(), 3);
            player.setHeldItem(EnumHand.MAIN_HAND, stick);
            player.interactionManager.onBlockClicked(log, EnumFacing.UP);
            player.interactionManager.onBlockClicked(log, EnumFacing.UP);
            c.eq("variant", stick.getSubCompound("DebugProperty").getString("minecraft:log"), "two attacks select the second property");
            click(player, world, stick, log, EnumFacing.UP);
            c.eq(BlockPlanks.EnumType.SPRUCE, world.getBlockState(log).getValue(BlockOldLog.VARIANT), "and using it steps that one: oak -> spruce");
            c.ok("facing".equals(stick.getSubCompound("DebugProperty").getString(id)), "each block type keeps its own selection");
        } finally {
            world.getMinecraftServer().getPlayerList().removeOp(player.getGameProfile());
            player.interactionManager.setGameType(GameType.SURVIVAL);
        }
        c.ok(ItemDebugStick.storedProperties(Blocks.OAK_FENCE.getDefaultState()).isEmpty(),
                "a fence's connections are not stored, so the stick offers none");
        c.ok(ItemDebugStick.storedProperties(Blocks.LEVER.getDefaultState()).size() == 2,
                "a lever's two properties are both stored");
    }

    private static void shulkerBox(Check c, EntityPlayerMP player) {
        WorldServer world = c.world;
        BlockPos floor = c.area().add(4, -1, 4);
        BlockUndyedShulkerBox undyed = BlocksContent.shulkerBox();

        click(player, world, new ItemStack(undyed), floor, EnumFacing.UP);
        BlockPos at = floor.up();
        c.eq(undyed, world.getBlockState(at).getBlock(), "the undyed box places");
        TileEntity te = world.getTileEntity(at);
        c.ok(te instanceof TileEntityUndyedShulkerBox, "with its own tile entity: " + te);
        if (te instanceof TileEntityUndyedShulkerBox) {
            ((TileEntityUndyedShulkerBox) te).setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 5));
            NBTTagCompound saved = te.writeToNBT(new NBTTagCompound());
            c.eq("updateaquaticbackport:shulker_box", saved.getString("id"), "saved under this mod's id");
            TileEntity loaded = TileEntity.create(world, saved);
            c.ok(loaded instanceof TileEntityUndyedShulkerBox
                    && ((TileEntityUndyedShulkerBox) loaded).getStackInSlot(0).getCount() == 5, "and read back with its contents");
        }
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
        player.interactionManager.tryHarvestBlock(at);
        ItemStack dropped = ItemStack.EMPTY;
        for (net.minecraft.entity.item.EntityItem e : world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                new AxisAlignedBB(at).grow(2))) {
            if (e.getItem().getItem() == Item.getItemFromBlock(undyed)) {
                dropped = e.getItem();
            }
            e.setDead();
        }
        c.ok(!dropped.isEmpty() && dropped.hasTagCompound() && dropped.getTagCompound().getCompoundTag("BlockEntityTag").hasKey("Items"),
                "broken, it drops itself with its contents: " + dropped + " " + dropped.getTagCompound());

        // Washing.
        BlockPos cauldron = floor.east(4).up();
        world.setBlockState(cauldron, Blocks.CAULDRON.getDefaultState().withProperty(BlockCauldron.LEVEL, 3), 3);
        ItemStack red = new ItemStack(Blocks.RED_SHULKER_BOX);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("BlockEntityTag", new NBTTagCompound());
        tag.getCompoundTag("BlockEntityTag").setString("Lock", "x");
        red.setTagCompound(tag);
        red.setStackDisplayName("Mine");
        click(player, world, red, cauldron, EnumFacing.UP);
        ItemStack held = player.getHeldItemMainhand();
        c.eq(Item.getItemFromBlock(undyed), held.getItem(), "a dyed box washed in a cauldron comes out undyed");
        c.ok("Mine".equals(held.getDisplayName()) && "x".equals(held.getTagCompound().getCompoundTag("BlockEntityTag").getString("Lock")),
                "with its name and contents kept");
        c.eq(2, world.getBlockState(cauldron).getValue(BlockCauldron.LEVEL), "the cauldron loses a level");
        c.ok(world.isAirBlock(cauldron.up()), "and nothing is placed on it");
        click(player, world, new ItemStack(undyed), cauldron, EnumFacing.UP);
        c.eq(2, world.getBlockState(cauldron).getValue(BlockCauldron.LEVEL), "an undyed box takes no water");
        world.setBlockToAir(cauldron.up());
        world.setBlockState(cauldron, Blocks.CAULDRON.getDefaultState(), 3);
        click(player, world, new ItemStack(Blocks.BLUE_SHULKER_BOX), cauldron, EnumFacing.UP);
        c.ok(player.getHeldItemMainhand().getItem() != Item.getItemFromBlock(undyed), "an empty cauldron washes nothing");
        world.setBlockToAir(cauldron.up());

        IBehaviorDispenseItem box = BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Item.getItemFromBlock(undyed));
        c.ok(box == BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Item.getItemFromBlock(Blocks.PURPLE_SHULKER_BOX)),
                "dispensers place the undyed box as the dyed ones");
    }

    private static void mobsAndDispensers(Check c) {
        WorldServer world = c.world;
        BlockPos p = c.area().add(4, 0, 4);
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        zombie.setItemStackToSlot(EntityEquipmentSlot.HEAD, new ItemStack(Blocks.PUMPKIN));
        world.spawnEntity(zombie);
        c.eq(Item.getItemFromBlock(BlocksContent.carvedPumpkin()), zombie.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem(),
                "a mob joining with a pumpkin on its head wears a carved one");
        zombie.setDead();

        c.ok(BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(Item.getItemFromBlock(Blocks.PUMPKIN)).getClass()
                == BehaviorDefaultDispenseItem.class, "dispensers just drop a plain pumpkin");
        // A dispenser west of the head place of a snow golem's body, facing east.
        BlockPos base = p.east(4);
        world.setBlockState(base, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(base.up(), Blocks.SNOW.getDefaultState(), 3);
        BlockPos dispenser = base.up(2).west();
        world.setBlockState(dispenser, Blocks.DISPENSER.getDefaultState().withProperty(BlockDispenser.FACING, EnumFacing.EAST), 3);
        ItemStack carved = new ItemStack(BlocksContent.carvedPumpkin());
        BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.getObject(carved.getItem()).dispense(new BlockSourceImpl(world, dispenser), carved);
        c.eq(1, count(world, EntitySnowman.class, base), "a dispenser puts a carved pumpkin on a snow golem's body");
        c.ok(carved.isEmpty(), "using the pumpkin");
    }
}
