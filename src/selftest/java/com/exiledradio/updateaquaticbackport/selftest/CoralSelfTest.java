package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.common.BlockPseudoWater;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.coral.BlockCoral;
import com.exiledradio.updateaquaticbackport.coral.BlockCoralBlock;
import com.exiledradio.updateaquaticbackport.coral.BlockCoralWallFan;
import com.exiledradio.updateaquaticbackport.coral.CoralContent;
import com.exiledradio.updateaquaticbackport.coral.CoralGenerator;
import com.exiledradio.updateaquaticbackport.coral.CoralPickles;
import com.exiledradio.updateaquaticbackport.coral.CoralType;
import com.exiledradio.updateaquaticbackport.woodset.BlockSeaPickle;
import com.exiledradio.updateaquaticbackport.woodset.WoodsetContent;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import java.util.Random;

/** Coral blocks, coral, fans and wall fans, living and dead; reefs; pickles spreading over coral. */
public final class CoralSelfTest implements SelfTest {

    @Override
    public String name() {
        return "coral";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(CoralContent.enabled(), "coral stream on");
        c.eq(40, CoralContent.all().size(), "40 coral blocks");
        int items = 0;
        for (Block b : CoralContent.all()) {
            Item item = Item.getItemFromBlock(b);
            if (item != Items.AIR) {
                items++;
            }
            for (IBlockState s : b.getBlockState().getValidStates()) {
                if (b instanceof BlockPseudoWater && (s.getValue(BlockPseudoWater.LEVEL) != 0 || s.getValue(BlockPseudoWater.SURFACE))) {
                    continue;
                }
                c.ok(b.getStateFromMeta(b.getMetaFromState(s)) == s, "meta round trip " + s);
            }
        }
        c.eq(30, items, "30 items (wall fans have none)");
        CoralType t = CoralType.BRAIN;
        BlockCoralBlock coralBlock = CoralContent.BLOCKS.get(t);
        BlockCoralBlock deadBlock = CoralContent.DEAD_BLOCKS.get(t);
        BlockCoral coral = CoralContent.CORALS.get(t);
        BlockCoral fan = CoralContent.FANS.get(t);
        BlockCoralWallFan wall = CoralContent.WALL_FANS.get(t);
        c.eq(new ItemStack(fan).getItem(), wall.getItem(world, BlockPos.ORIGIN, wall.getDefaultState()).getItem(), "a wall fan picks as the fan");

        EntityPlayerMP player = Players.real(world, "coral");
        player.interactionManager.setGameType(GameType.SURVIVAL);

        // 1. coral blocks die out of water ------------------------------------------------------------
        BlockPos land = c.area();
        place(player, world, new ItemStack(coralBlock), land.down(), EnumFacing.UP);
        c.eq(coralBlock, world.getBlockState(land).getBlock(), "coral block placed on land");
        c.ok(world.isUpdateScheduled(land, coralBlock), "a dry coral block will be checked 3-5 seconds later");
        Pools.settle(world);
        c.eq(deadBlock, world.getBlockState(land).getBlock(), "and dies");
        // One enclosed water block beside it, so the water cannot spread anywhere else.
        BlockPos wet = land.east(2);
        BlockPos cell = wet.north();
        for (BlockPos q : new BlockPos[] {cell.north(), cell.east(), cell.west(), cell.up()}) {
            world.setBlockState(q, Blocks.STONE.getDefaultState(), 2);
        }
        world.setBlockState(cell, Blocks.WATER.getDefaultState(), 3);
        place(player, world, new ItemStack(coralBlock), wet.down(), EnumFacing.UP);
        Pools.settle(world);
        c.eq(coralBlock, world.getBlockState(wet).getBlock(), "a coral block touching water lives");
        world.setBlockToAir(cell);
        Pools.settle(world);
        c.eq(deadBlock, world.getBlockState(wet).getBlock(), "it dies when the water goes");
        c.ok(Math.abs(coralBlock.getBlockHardness(coralBlock.getDefaultState(), world, wet) - 1.5F) < 1e-6
                && Math.abs(coralBlock.getExplosionResistance(null) - 6.0F) < 1e-4, "hardness 1.5, blast resistance 6");
        c.eq("pickaxe", coralBlock.getHarvestTool(coralBlock.getDefaultState()), "mined with a pickaxe");
        world.setBlockState(wet, coralBlock.getDefaultState(), 2);
        breakWith(player, world, wet, new ItemStack(Items.IRON_PICKAXE));
        c.eq(1, Drops.count(world, wet, Item.getItemFromBlock(deadBlock)), "a living block mined drops the dead block");
        world.setBlockState(wet, coralBlock.getDefaultState(), 2);
        breakWith(player, world, wet, silk(new ItemStack(Items.IRON_PICKAXE)));
        c.eq(1, Drops.count(world, wet, Item.getItemFromBlock(coralBlock)), "with Silk Touch, itself");

        // 2. coral and fans, in water and out -------------------------------------------------------------
        BlockPos floor = Pools.pool(c, 5);
        BlockPos a = floor.add(2, 0, 2);
        place(player, world, new ItemStack(coral), a, EnumFacing.UP);
        IBlockState s = world.getBlockState(a.up());
        c.ok(s.getBlock() == coral && s.getValue(BlockPseudoWater.WATERLOGGED), "coral placed in water is waterlogged");
        c.eq(Material.WATER, s.getMaterial(), "and is water");
        Pools.settle(world);
        c.eq(coral, world.getBlockState(a.up()).getBlock(), "and lives");
        place(player, world, new ItemStack(fan), land.east(4).down(), EnumFacing.UP);
        BlockPos dryFan = land.east(4);
        s = world.getBlockState(dryFan);
        c.ok(s.getBlock() == fan && !s.getValue(BlockPseudoWater.WATERLOGGED), "a fan placed on land is dry");
        c.ok(s.getMaterial() != Material.WATER && s.getMaterial().blocksMovement(), "dry, it is not water and blocks flow");
        c.ok(world.isUpdateScheduled(dryFan, fan), "a dry fan will be checked");
        Pools.settle(world);
        s = world.getBlockState(dryFan);
        c.ok(s.getBlock() == CoralContent.DEAD_FANS.get(t) && !s.getValue(BlockPseudoWater.WATERLOGGED), "and dies, dry");

        // A dry coral touching water lives (1.14.4 checks all six sides).
        BlockPos side = floor.add(0, 1, 6).west();
        world.setBlockState(side.down(), Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(side, coral.getDefaultState().withProperty(BlockPseudoWater.WATERLOGGED, false), 3);
        Pools.settle(world);
        c.eq(coral, world.getBlockState(side).getBlock(), "a dry coral touching water lives");

        // 3. buckets ---------------------------------------------------------------------------------------
        BlockPos b = land.east(6);
        world.setBlockState(b.down(), Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(b, coral.getDefaultState().withProperty(BlockPseudoWater.WATERLOGGED, false), 2);
        FillBucketEvent fill = new FillBucketEvent(player, new ItemStack(Items.WATER_BUCKET), world, new RayTraceResult(new Vec3d(b), EnumFacing.UP, b));
        MinecraftForge.EVENT_BUS.post(fill);
        c.ok(world.getBlockState(b).getValue(BlockPseudoWater.WATERLOGGED) && fill.getFilledBucket().getItem() == Items.BUCKET,
                "a water bucket waterlogs a dry coral and comes back empty");
        FillBucketEvent drain = new FillBucketEvent(player, new ItemStack(Items.BUCKET), world, new RayTraceResult(new Vec3d(b), EnumFacing.UP, b));
        MinecraftForge.EVENT_BUS.post(drain);
        c.ok(!world.getBlockState(b).getValue(BlockPseudoWater.WATERLOGGED) && drain.getFilledBucket().getItem() == Items.WATER_BUCKET,
                "an empty bucket takes its water");

        // 4. drops: Silk Touch only; water left behind -----------------------------------------------------
        breakWith(player, world, a.up(), new ItemStack(Items.IRON_PICKAXE));
        c.eq(0, Drops.count(world, a.up(), Item.getItemFromBlock(coral)), "coral drops nothing without Silk Touch");
        c.ok(KelpSelfTest.water(world, a.up()), "broken waterlogged coral leaves water");
        world.setBlockState(a.up(), coral.getDefaultState(), 3);
        breakWith(player, world, a.up(), silk(new ItemStack(Items.IRON_SHOVEL)));
        c.eq(1, Drops.count(world, a.up(), Item.getItemFromBlock(coral)), "with Silk Touch on any tool, itself");

        // 5. support ---------------------------------------------------------------------------------------
        BlockPos p = floor.add(4, 0, 4);
        world.setBlockState(p.up(), coral.getDefaultState(), 3);
        world.setBlockState(p, Blocks.WATER.getDefaultState(), 3);
        c.ok(KelpSelfTest.water(world, p.up()), "coral without support goes, leaving water");
        c.eq(0, Drops.count(world, p.up(), Item.getItemFromBlock(coral)), "and drops nothing");
        world.setBlockState(p, Blocks.STONE.getDefaultState(), 3);

        // 6. wall fans ---------------------------------------------------------------------------------------
        BlockPos pillar = floor.add(6, 1, 6);
        world.setBlockState(pillar, Blocks.STONE.getDefaultState(), 3);
        world.setBlockState(pillar.up(), Blocks.STONE.getDefaultState(), 3);
        place(player, world, new ItemStack(fan), pillar.up(), EnumFacing.EAST);
        s = world.getBlockState(pillar.up().east());
        c.ok(s.getBlock() == wall && s.getValue(BlockCoralWallFan.FACING) == EnumFacing.EAST && s.getValue(BlockPseudoWater.WATERLOGGED),
                "a fan placed on a side is a wall fan facing out, waterlogged: " + s);
        place(player, world, new ItemStack(fan), pillar.up(), EnumFacing.UP);
        c.eq(fan, world.getBlockState(pillar.up(2)).getBlock(), "a fan placed on top is a floor fan");
        c.eq(EnumFacing.SOUTH, s.withRotation(Rotation.CLOCKWISE_90).getValue(BlockCoralWallFan.FACING), "wall fans rotate");
        breakWith(player, world, pillar.up().east(), silk(new ItemStack(Items.IRON_PICKAXE)));
        c.eq(1, Drops.count(world, pillar.up().east(), Item.getItemFromBlock(fan)), "a wall fan drops the fan item with Silk Touch");
        world.setBlockState(pillar.up().east(), wall.getDefaultState().withProperty(BlockCoralWallFan.FACING, EnumFacing.EAST), 3);
        world.setBlockState(pillar.up(), Blocks.WATER.getDefaultState(), 3);
        c.ok(KelpSelfTest.water(world, pillar.up().east()), "a wall fan goes with the block it hangs on");
        // A dry wall fan dies facing the same way.
        BlockPos post = land.east(8);
        world.setBlockState(post, Blocks.STONE.getDefaultState(), 2);
        world.setBlockState(post.west(), wall.getDefaultState().withProperty(BlockCoralWallFan.FACING, EnumFacing.WEST)
                .withProperty(BlockPseudoWater.WATERLOGGED, false), 3);
        world.scheduleUpdate(post.west(), wall, 1);
        Pools.settle(world);
        s = world.getBlockState(post.west());
        c.ok(s.getBlock() == CoralContent.DEAD_WALL_FANS.get(t) && s.getValue(BlockCoralWallFan.FACING) == EnumFacing.WEST,
                "a dry wall fan dies facing the same way: " + s);

        // 7. sea pickles on coral and bone meal -----------------------------------------------------------
        BlockPos reef = Pools.pool(c, 4);
        for (BlockPos q : BlockPos.getAllInBox(reef.add(2, 0, 2), reef.add(8, 0, 8))) {
            world.setBlockState(q, coralBlock.getDefaultState(), 2);
        }
        BlockPos pickleAt = reef.add(5, 1, 5);
        c.ok(WoodsetContent.seaPickle() != null, "our sea pickle exists here (owner absent)");
        IBlockState pickle = WoodsetContent.seaPickle().getDefaultState().withProperty(BlockSeaPickle.WATERLOGGED, true);
        world.setBlockState(pickleAt, pickle, 2);
        Random always = new Random() {
            @Override
            public int nextInt(int bound) {
                return 0;
            }
        };
        CoralPickles.spread(world, pickleAt, pickle, always);
        int pickles = 0;
        for (BlockPos q : BlockPos.getAllInBox(reef.add(2, 1, 2), reef.add(8, 1, 8))) {
            if (world.getBlockState(q).getBlock() == WoodsetContent.seaPickle()) {
                pickles++;
            }
        }
        c.eq(4, world.getBlockState(pickleAt).getValue(BlockSeaPickle.PICKLES).intValue(), "bone meal makes the pickle four");
        c.eq(13, pickles, "pickles spread over the coral around, in a diamond (12 + itself)");
        ItemStack meal = new ItemStack(Items.DYE, 2, EnumDyeColor.WHITE.getDyeDamage());
        BlockPos second = reef.add(3, 1, 5);
        world.setBlockState(second, WoodsetContent.seaPickle().getDefaultState().withProperty(BlockSeaPickle.WATERLOGGED, true), 2);
        c.ok(ItemDye.applyBonemeal(meal, world, second, player, EnumHand.MAIN_HAND) && meal.getCount() == 1, "bone meal is used on a pickle on coral");

        // 8. reef shapes in a deep pool ----------------------------------------------------------------------
        BlockPos deep = Pools.pool(c, 12).add(6, 1, 6);
        Random r = new Random(42);
        CoralGenerator.tree(world, r, deep, coralBlock.getDefaultState());
        CoralGenerator.claw(world, r, deep.add(-3, 0, -3), CoralContent.BLOCKS.get(CoralType.FIRE).getDefaultState());
        CoralGenerator.mushroom(world, r, deep.add(2, 0, -4), CoralContent.BLOCKS.get(CoralType.TUBE).getDefaultState());
        int reefBlocks = countCoralBlocks(world, deep, 7, 12);
        c.ok(reefBlocks >= 10, "tree, claw and mushroom build a reef: " + reefBlocks + " coral blocks");

        // 9. generation ------------------------------------------------------------------------------------------
        worldGen(c);
    }

    private static int countCoralBlocks(WorldServer world, BlockPos center, int radius, int height) {
        int n = 0;
        for (BlockPos q : BlockPos.getAllInBox(center.add(-radius, -4, -radius), center.add(radius, height, radius))) {
            if (world.getBlockState(q).getBlock() instanceof BlockCoralBlock) {
                n++;
            }
        }
        return n;
    }

    private static final net.minecraft.world.gen.NoiseGeneratorPerlin REEF_NOISE =
            new net.minecraft.world.gen.NoiseGeneratorPerlin(new Random(2345L), 1);

    /**
     * A warm ocean near spawn grows reefs; a cold one none. With the ocean stream on these are its
     * {@code warm_ocean} and {@code cold_ocean} (looked up by name); with it off, warm and cold stretches of
     * vanilla's ocean by the temperature noise.
     */
    private static void worldGen(Check c) {
        WorldServer world = c.world;
        Biome warmOcean = net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.getValue(
                com.exiledradio.updateaquaticbackport.common.Content.id("warm_ocean"));
        Biome coldOcean = net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES.getValue(
                com.exiledradio.updateaquaticbackport.common.Content.id("cold_ocean"));
        if (warmOcean != null && coldOcean != null) {
            worldGenOceanBiomes(c, world, warmOcean, coldOcean);
            return;
        }
        BlockPos warm = null;
        BlockPos cold = null;
        for (int x = -6000; x <= 6000 && (warm == null || cold == null); x += 96) {
            for (int z = -6000; z <= 6000 && (warm == null || cold == null); z += 96) {
                BlockPos pos = new BlockPos(x, 0, z);
                Biome biome = world.getBiomeProvider().getBiome(pos);
                if (biome != Biomes.OCEAN && biome != Biomes.DEEP_OCEAN) {
                    continue;
                }
                double temp = OceanKind.temperature(world.getSeed(), x, z);
                // Reefs come in patches by 1.13's density noise (half of warm sea has none, as in vanilla):
                // look where both say yes.
                if (warm == null && temp > 0.6 && REEF_NOISE.getValue(x / 400.0, z / 400.0) > 0.3) {
                    warm = pos;
                } else if (cold == null && temp < -0.5) {
                    cold = pos;
                }
            }
        }
        if (warm == null || cold == null) {
            c.fail("no warm or cold vanilla ocean found near spawn: " + warm + " " + cold);
            return;
        }
        int reef = coralIn(world, warm);
        int none = coralIn(world, cold);
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST coral worldgen: warm {} -> {} coral, cold {} -> {}",
                warm, reef, cold, none);
        c.ok(reef > 0, "a warm ocean has coral reefs: " + reef + " blocks at " + warm);
        c.eq(0, none, "a cold ocean has none");
    }

    /** As {@link #worldGen}, in the ocean stream's warm and cold ocean biomes. */
    private static void worldGenOceanBiomes(Check c, WorldServer world, Biome warmOcean, Biome coldOcean) {
        BlockPos warm = null;
        BlockPos cold = null;
        for (int x = -6000; x <= 6000 && (warm == null || cold == null); x += 96) {
            for (int z = -6000; z <= 6000 && (warm == null || cold == null); z += 96) {
                BlockPos pos = new BlockPos(x, 0, z);
                Biome biome = world.getBiomeProvider().getBiome(pos);
                // Reefs come in patches by 1.13's density noise: look where it says yes.
                if (warm == null && biome == warmOcean && REEF_NOISE.getValue(x / 400.0, z / 400.0) > 0.3
                        && world.getBiomeProvider().areBiomesViable(x, z, 40, java.util.Collections.singletonList(warmOcean))) {
                    warm = pos;
                } else if (cold == null && biome == coldOcean) {
                    cold = pos;
                }
            }
        }
        if (warm == null || cold == null) {
            c.fail("no warm or cold ocean biome found near spawn: " + warm + " " + cold);
            return;
        }
        int reef = coralIn(world, warm);
        int none = coralIn(world, cold);
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST coral worldgen (ocean biomes): warm {} -> {} coral, cold {} -> {}",
                warm, reef, cold, none);
        c.ok(reef > 0, "a warm ocean biome has coral reefs: " + reef + " blocks at " + warm);
        c.eq(0, none, "a cold ocean biome has none");
    }

    private static int coralIn(WorldServer world, BlockPos at) {
        int cx = at.getX() >> 4;
        int cz = at.getZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.getChunk(cx + dx, cz + dz);
            }
        }
        int n = 0;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int x0 = (cx + dx) << 4;
                int z0 = (cz + dz) << 4;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 20; y < 64; y++) {
                            Block b = world.getBlockState(new BlockPos(x0 + x, y, z0 + z)).getBlock();
                            if (b instanceof BlockCoralBlock || b instanceof BlockCoral) {
                                n++;
                            }
                        }
                    }
                }
            }
        }
        return n;
    }

    private static ItemStack silk(ItemStack tool) {
        tool.addEnchantment(Enchantments.SILK_TOUCH, 1);
        return tool;
    }

    private static void breakWith(EntityPlayerMP player, WorldServer world, BlockPos pos, ItemStack tool) {
        player.setHeldItem(EnumHand.MAIN_HAND, tool);
        player.interactionManager.tryHarvestBlock(pos);
    }

    private static void place(EntityPlayerMP player, WorldServer world, ItemStack stack, BlockPos on, EnumFacing face) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        float hx = face == EnumFacing.EAST ? 1.0F : 0.5F;
        float hy = face == EnumFacing.UP ? 1.0F : 0.5F;
        stack.getItem().onItemUse(player, world, on, EnumHand.MAIN_HAND, face, hx, hy, 0.5F);
    }

    @SuppressWarnings("unused")
    private static Enchantment unused;
}
