package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.common.LegacyWater;
import com.exiledradio.updateaquaticbackport.common.RequiredWater;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.kelp.BlockKelp;
import com.exiledradio.updateaquaticbackport.kelp.BlockSeagrass;
import com.exiledradio.updateaquaticbackport.kelp.KelpContent;
import com.exiledradio.updateaquaticbackport.kelp.KelpGenerator;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ActionResult;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.Explosion;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Arrays;
import java.util.Random;

/** Kelp, seagrass, dried kelp and their generation. */
public final class KelpSelfTest implements SelfTest {

    @Override
    public String name() {
        return "kelp";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(KelpContent.enabled(), "kelp stream on");
        BlockKelp kelp = KelpContent.kelp();
        BlockKelp plant = KelpContent.kelpPlant();
        BlockSeagrass seagrass = KelpContent.seagrass();
        BlockSeagrass tall = KelpContent.tallSeagrass();
        Item kelpItem = Item.getItemFromBlock(kelp);
        Item seagrassItem = Item.getItemFromBlock(seagrass);

        // 1. registration ---------------------------------------------------------------------------
        for (String n : new String[] {"kelp", "kelp_plant", "seagrass", "tall_seagrass", "dried_kelp_block"}) {
            Block b = ForgeRegistries.BLOCKS.getValue(Content.id(n));
            c.ok(b != null && Content.id(n).equals(b.getRegistryName()), "block " + n);
        }
        c.ok(kelpItem != Items.AIR && seagrassItem != Items.AIR, "kelp and seagrass items");
        c.eq(Items.AIR, Item.getItemFromBlock(plant), "no kelp plant item");
        c.eq(Items.AIR, Item.getItemFromBlock(tall), "no tall seagrass item");
        for (Block b : new Block[] {kelp, plant, seagrass, tall}) {
            c.ok(Content.multipartBlocks().contains(b), "multipart " + b.getRegistryName());
            for (IBlockState s : b.getBlockState().getValidStates()) {
                IBlockState back = b.getStateFromMeta(b.getMetaFromState(s));
                boolean stored = b == kelp || (b == tall) || s == b.getDefaultState();
                if (stored) {
                    c.ok(back == s, "meta round trip " + s);
                }
            }
            c.ok(b.getDefaultState().getMaterial() != Material.WATER, b.getRegistryName() + " is not itself water");
            c.ok(b instanceof Waterloggable && ((Waterloggable) b).waterNeed(b.getDefaultState()) == Waterloggable.Need.REQUIRED,
                    b.getRegistryName() + " is waterloggable and needs water");
            c.ok(b.getBlockState().getProperties().size() <= 1, b.getRegistryName() + " has no water properties: "
                    + b.getBlockState().getProperties());
        }

        // 2. placing by hand -----------------------------------------------------------------------
        BlockPos floor = Pools.pool(c, 9);
        EntityPlayerMP player = Players.real(world, "kelp");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        BlockPos at = floor.add(1, 0, 1);
        place(player, world, new ItemStack(kelpItem), at, EnumFacing.UP);
        IBlockState placed = world.getBlockState(at.up());
        c.eq(kelp, placed.getBlock(), "kelp placed on the floor under water");
        c.ok(placed.getValue(BlockKelp.AGE) < 15, "placed kelp starts below full growth");
        c.ok(Waterlogging.hasWaterSource(world, at.up()), "kelp placed in water keeps it, as a source");

        BlockPos dry = c.area().down();
        place(player, world, new ItemStack(kelpItem), dry, EnumFacing.UP);
        c.ok(world.isAirBlock(dry.up()), "kelp cannot be placed on dry land");
        place(player, world, new ItemStack(seagrassItem), dry, EnumFacing.UP);
        c.ok(world.isAirBlock(dry.up()), "seagrass cannot be placed on dry land");
        BlockPos magma = floor.add(3, 0, 1);
        world.setBlockState(magma, Blocks.MAGMA.getDefaultState());
        place(player, world, new ItemStack(kelpItem), magma, EnumFacing.UP);
        c.ok(water(world, magma.up()), "no kelp on magma");

        // 3. it is water to everything that asks ------------------------------------------------------
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.getX() + 0.5, at.getY() + 1.1, at.getZ() + 0.5);
        world.spawnEntity(pig);
        for (int i = 0; i < 3; i++) {
            pig.onUpdate();
        }
        c.ok(pig.isInWater(), "a mob in kelp is in water");
        pig.setDead();
        EntityPlayerMP swimmer = Players.real(world, "kelpswimmer");
        swimmer.setPosition(at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5);
        c.ok(swimmer.isInsideOfMaterial(Material.WATER), "eyes in kelp are under water");
        // A boat over the column reads water level from every water block under it.
        growTo(world, kelp, at.up(), 7);
        EntityBoat boat = new EntityBoat(world, at.getX() + 0.5, floor.getY() + 9.2, at.getZ() + 0.5);
        world.spawnEntity(boat);
        try {
            for (int i = 0; i < 20; i++) {
                boat.onUpdate();
            }
            c.ok(true, "a boat over kelp ticks");
        } catch (RuntimeException e) {
            c.fail("a boat over kelp crashed: " + e);
        }
        boat.setDead();

        // 4. buckets ----------------------------------------------------------------------------------
        FillBucketEvent bucket = new FillBucketEvent(player, new ItemStack(Items.BUCKET), world,
                new RayTraceResult(new Vec3d(at.up()), EnumFacing.UP, at.up()));
        c.ok(MinecraftForge.EVENT_BUS.post(bucket), "an empty bucket takes nothing from kelp");
        c.eq(plant, world.getBlockState(at.up()).getBlock(), "kelp still there after the bucket");
        buckets(c, kelp, seagrass);

        // 5. growth ---------------------------------------------------------------------------------------
        BlockPos col = floor.add(5, 0, 5).up();
        world.setBlockState(col, kelp.getDefaultState().withProperty(BlockKelp.AGE, 0), 3);
        Random always = new Random() {
            @Override
            public double nextDouble() {
                return 0.0;
            }
        };
        for (int i = 0; i < 30; i++) {
            BlockPos top = topOf(world, kelp, plant, col);
            kelp.randomTick(world, top, world.getBlockState(top), always);
        }
        BlockPos top = topOf(world, kelp, plant, col);
        c.eq(floor.getY() + 9, top.getY(), "kelp grows up to the highest water block");
        c.eq(kelp, world.getBlockState(top).getBlock(), "the top is kelp");
        c.eq(plant, world.getBlockState(top.down()).getBlock(), "under it is kelp plant");
        boolean grownWet = true;
        for (BlockPos p = col; p.getY() <= top.getY(); p = p.up()) {
            grownWet &= Waterlogging.hasWaterSource(world, p);
        }
        c.ok(grownWet, "every piece kelp grows into keeps the water it grew into");
        BlockPos col2 = floor.add(7, 0, 5).up();
        world.setBlockState(col2, kelp.getDefaultState().withProperty(BlockKelp.AGE, 15), 3);
        kelp.randomTick(world, col2, world.getBlockState(col2), always);
        c.ok(water(world, col2.up()), "fully grown kelp does not grow");
        Random never = new Random() {
            @Override
            public double nextDouble() {
                return 0.99;
            }
        };
        world.setBlockState(col2, kelp.getDefaultState().withProperty(BlockKelp.AGE, 0), 3);
        kelp.randomTick(world, col2, world.getBlockState(col2), never);
        c.ok(water(world, col2.up()), "86% of ticks it does not grow");

        // 6. bone meal and shears -------------------------------------------------------------------------
        world.setBlockState(col2, kelp.getDefaultState().withProperty(BlockKelp.AGE, 15), 3);
        ItemStack meal = new ItemStack(Items.DYE, 5, EnumDyeColor.WHITE.getDyeDamage());
        ItemDye.applyBonemeal(meal, world, col2, player, EnumHand.MAIN_HAND);
        c.eq(kelp, world.getBlockState(col2.up()).getBlock(), "bone meal grows kelp, even fully grown");
        c.eq(plant, world.getBlockState(col2).getBlock(), "and the old top becomes plant");
        c.eq(4, meal.getCount(), "one bone meal used");
        ItemDye.applyBonemeal(meal, world, col2, player, EnumHand.MAIN_HAND);
        c.eq(kelp, world.getBlockState(col2.up(2)).getBlock(), "bone meal on the plant grows the top");
        BlockPos sheared = col2.up(2);
        world.setBlockState(sheared, kelp.getDefaultState().withProperty(BlockKelp.AGE, 3), 3);
        ItemStack shears = new ItemStack(Items.SHEARS);
        player.setHeldItem(EnumHand.MAIN_HAND, shears);
        kelp.onBlockActivated(world, sheared, world.getBlockState(sheared), player, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 0.5F, 0.5F);
        c.eq(15, world.getBlockState(sheared).getValue(BlockKelp.AGE).intValue(), "shears stop the top growing");
        c.eq(1, shears.getItemDamage(), "shears wear");

        // 7. breaking ---------------------------------------------------------------------------------------
        int pieces = top.getY() - col.getY() + 1;
        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
        player.interactionManager.tryHarvestBlock(col);
        c.ok(water(world, col), "breaking kelp leaves water");
        Pools.settle(world);
        boolean gone = true;
        for (BlockPos p = col; p.getY() <= top.getY(); p = p.up()) {
            gone &= water(world, p);
        }
        c.ok(gone, "everything above breaks too, leaving water");
        c.eq(pieces, drops(world, col, pieces + 2, kelpItem), "one kelp per piece");
        BlockPos boom = floor.add(9, 0, 9).up();
        world.setBlockState(boom, kelp.getDefaultState(), 3);
        kelp.onBlockExploded(world, boom, new Explosion(world, null, boom.getX(), boom.getY(), boom.getZ(), 1, false, true));
        c.ok(water(world, boom), "blown-up kelp leaves water");

        // 8. flowing water beside kelp leaves it alone ------------------------------------------------------
        BlockPos edge = floor.add(0, 1, 6);
        world.setBlockState(edge, kelp.getDefaultState(), 3);
        world.setBlockState(edge.north(), Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 3), 3);
        Pools.settle(world);
        c.eq(kelp, world.getBlockState(edge).getBlock(), "flowing water does not wash kelp away");

        // 9. seagrass ---------------------------------------------------------------------------------------
        BlockPos sg = floor.add(2, 0, 9);
        place(player, world, new ItemStack(seagrassItem), sg, EnumFacing.UP);
        c.eq(seagrass, world.getBlockState(sg.up()).getBlock(), "seagrass placed under water");
        player.interactionManager.tryHarvestBlock(sg.up());
        c.ok(water(world, sg.up()), "broken seagrass leaves water");
        c.eq(0, WoodsetSelfTest.countDrops(world, sg.up(), seagrassItem), "seagrass drops nothing by hand");
        world.setBlockState(sg.up(), seagrass.getDefaultState(), 3);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactionManager.tryHarvestBlock(sg.up());
        c.eq(1, WoodsetSelfTest.countDrops(world, sg.up(), seagrassItem), "sheared seagrass drops itself");
        c.ok(water(world, sg.up()), "sheared seagrass leaves water");
        world.setBlockState(sg.up(), seagrass.getDefaultState(), 3);
        ItemDye.applyBonemeal(meal, world, sg.up(), player, EnumHand.MAIN_HAND);
        c.ok(world.getBlockState(sg.up()).getBlock() == tall && world.getBlockState(sg.up(2)).getBlock() == tall
                && world.getBlockState(sg.up(2)).getValue(BlockSeagrass.HALF) == BlockSeagrass.Half.UPPER, "bone meal makes tall seagrass");
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.SHEARS));
        player.interactionManager.tryHarvestBlock(sg.up(2));
        c.eq(2, WoodsetSelfTest.countDrops(world, sg.up(), seagrassItem), "sheared tall seagrass drops two");
        c.ok(water(world, sg.up()) && water(world, sg.up(2)),
                "both halves go, leaving water");
        world.setBlockState(sg.up(), seagrass.getDefaultState(), 3);
        place(player, world, new ItemStack(Blocks.STONE), sg, EnumFacing.UP);
        c.eq(Blocks.STONE, world.getBlockState(sg.up()).getBlock(), "a block placed into seagrass replaces it");
        place(player, world, new ItemStack(Blocks.STONE), col2, EnumFacing.UP);
        c.ok(world.getBlockState(col2).getBlock() instanceof BlockKelp, "a block placed at kelp does not replace it");

        // 10. underwater bone meal ----------------------------------------------------------------------------
        BlockPos meadow = Pools.pool(c, 4).add(6, 0, 6);
        ItemStack meal2 = new ItemStack(Items.DYE, 3, EnumDyeColor.WHITE.getDyeDamage());
        boolean used = ItemDye.applyBonemeal(meal2, world, meadow, player, EnumHand.MAIN_HAND);
        int grass = count(world, meadow.up(), 6, seagrass) + count(world, meadow.up(), 6, tall);
        c.ok(used && meal2.getCount() == 2, "bone meal on the floor is used");
        c.ok(grass >= 8, "bone meal spreads seagrass: " + grass);
        ItemStack dryMeal = new ItemStack(Items.DYE, 1, EnumDyeColor.WHITE.getDyeDamage());
        c.ok(!ItemDye.applyBonemeal(dryMeal, world, dry, player, EnumHand.MAIN_HAND) && dryMeal.getCount() == 1, "bone meal on dry stone does nothing");

        // 11. dried kelp --------------------------------------------------------------------------------------
        Item dried = KelpContent.driedKelp();
        Block driedBlock = KelpContent.driedKelpBlock();
        c.ok(Crafting.same(FurnaceRecipes.instance().getSmeltingResult(new ItemStack(kelpItem)), new ItemStack(dried)), "kelp smelts to dried kelp");
        c.ok(Math.abs(FurnaceRecipes.instance().getSmeltingExperience(new ItemStack(dried)) - 0.1F) < 1e-6, "0.1 experience");
        ItemFood food = (ItemFood) dried;
        ItemStack one = new ItemStack(dried);
        c.ok(food.getHealAmount(one) == 1 && Math.abs(food.getSaturationModifier(one) - 0.3F) < 1e-6, "dried kelp feeds 1, saturation 0.3");
        c.eq(16, food.getMaxItemUseDuration(one), "eaten in 16 ticks");
        ItemStack d = new ItemStack(dried);
        Crafting.expect(c, Crafting.grid(3, 3, d, d, d, d, d, d, d, d, d), new ItemStack(driedBlock), "9 dried kelp make a block");
        Crafting.expect(c, Crafting.grid(1, 1, new ItemStack(driedBlock)), new ItemStack(dried, 9), "a block makes 9 dried kelp");
        c.eq(4001, TileEntityFurnace.getItemBurnTime(new ItemStack(driedBlock)), "dried kelp block burns 4001 ticks");
        c.ok(Math.abs(driedBlock.getBlockHardness(driedBlock.getDefaultState(), world, BlockPos.ORIGIN) - 0.5F) < 1e-6, "hardness 0.5");
        c.ok(Math.abs(driedBlock.getExplosionResistance(null) - 2.5F) < 1e-4, "blast resistance 2.5: " + driedBlock.getExplosionResistance(null));

        // 12. generation -------------------------------------------------------------------------------------
        BlockPos forest = Pools.pool(c, 9).add(6, 0, 6);
        Random r = new Random(5);
        int columns = 0;
        for (int i = 0; i < 20; i++) {
            columns += KelpGenerator.kelpColumn(world, KelpGenerator.oceanFloor(world, forest.getX() + r.nextInt(5) - 2, forest.getZ() + r.nextInt(5) - 2), r) ? 1 : 0;
        }
        int kelpBlocks = count(world, forest.up(), 4, kelp) + count(world, forest.up(), 4, plant);
        c.ok(columns > 0 && kelpBlocks > 0, "the kelp feature grows columns: " + columns + " tries placed " + kelpBlocks);
        boolean belowSurface = true;
        for (BlockPos p : BlockPos.getAllInBox(forest.add(-6, 9, -6), forest.add(6, 9, 6))) {
            belowSurface &= !(world.getBlockState(p).getBlock() instanceof BlockKelp);
        }
        c.ok(belowSurface, "generated kelp never reaches into the air");
        boolean genWet = true;
        for (BlockPos p : BlockPos.getAllInBox(forest.add(-4, -2, -4), forest.add(4, 9, 4))) {
            if (world.getBlockState(p).getBlock() instanceof BlockKelp) {
                genWet &= Waterlogging.hasWaterSource(world, p);
            }
        }
        c.ok(genWet, "generated kelp keeps the sea's water");
        waterlogging(c, kelp, plant, seagrass, tall);
        worldGen(c, kelp, plant, seagrass, tall);
    }

    /** Real chunks: an ocean near spawn, generated by the world generator, has kelp and seagrass. */
    private static void worldGen(Check c, BlockKelp kelp, BlockKelp plant, BlockSeagrass seagrass, BlockSeagrass tall) {
        WorldServer world = c.world;
        BlockPos ocean = world.getBiomeProvider().findBiomePosition(0, 0, 1500, Arrays.asList(Biomes.OCEAN, Biomes.DEEP_OCEAN), new Random(1));
        if (ocean == null) {
            c.fail("no ocean within 1500 blocks of spawn to test generation");
            return;
        }
        c.ok(ModConfig.kelp.generate, "generation on");
        int kelpCount = 0;
        int grassCount = 0;
        int dryCount = 0;
        int chunks = 0;
        int cx = ocean.getX() >> 4;
        int cz = ocean.getZ() >> 4;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                world.getChunk(cx + dx, cz + dz);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                chunks++;
                int x0 = (cx + dx) << 4;
                int z0 = (cz + dz) << 4;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = 20; y < 64; y++) {
                            BlockPos p = new BlockPos(x0 + x, y, z0 + z);
                            Block b = world.getBlockState(p).getBlock();
                            if (b == kelp || b == plant) {
                                kelpCount++;
                                dryCount += Waterlogging.hasWaterSource(world, p) ? 0 : 1;
                            } else if (b == seagrass || b == tall) {
                                grassCount++;
                                dryCount += Waterlogging.hasWaterSource(world, p) ? 0 : 1;
                            }
                        }
                    }
                }
            }
        }
        c.ok(grassCount > 0, "generated ocean has seagrass: " + grassCount + " in " + chunks + " chunks");
        c.ok(kelpCount > 0, "generated ocean has kelp: " + kelpCount);
        c.eq(0, dryCount, "generated kelp and seagrass without their water");
        com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST kelp worldgen at {}: {} kelp, {} seagrass in {} chunks",
                ocean, kelpCount, grassCount, chunks);
    }

    /** Real buckets, used by a player looking straight down at kelp and seagrass in a shallow pool. */
    private static void buckets(Check c, BlockKelp kelp, BlockSeagrass seagrass) {
        WorldServer world = c.world;
        BlockPos floor = Pools.pool(c, 1);
        BlockPos k = floor.add(2, 1, 2);
        BlockPos g = floor.add(6, 1, 2);
        world.setBlockState(k, kelp.getDefaultState(), 3);
        world.setBlockState(g, seagrass.getDefaultState(), 3);
        for (BlockPos target : new BlockPos[] {k, g}) {
            Block block = world.getBlockState(target).getBlock();
            EntityPlayerMP p = Players.real(world, "kelpbucket");
            p.interactionManager.setGameType(GameType.SURVIVAL);
            p.setLocationAndAngles(target.getX() + 0.5D, target.getY() + 0.2D, target.getZ() + 0.5D, 0.0F, 90.0F);
            p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET));
            ActionResult<ItemStack> empty = Items.BUCKET.onItemRightClick(world, p, EnumHand.MAIN_HAND);
            c.ok(empty.getResult().getItem() == Items.BUCKET, "a bucket on " + block.getRegistryName() + " stays empty: "
                    + empty.getType() + " " + empty.getResult());
            c.ok(world.getBlockState(target).getBlock() == block && Waterlogging.hasWaterSource(world, target),
                    "a bucket on " + block.getRegistryName() + " does nothing: " + world.getBlockState(target));
            p.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
            ActionResult<ItemStack> full = Items.WATER_BUCKET.onItemRightClick(world, p, EnumHand.MAIN_HAND);
            c.ok(world.getBlockState(target).getBlock() == block && Waterlogging.hasWaterSource(world, target),
                    "a water bucket leaves " + block.getRegistryName() + " as it is: " + full.getType() + " " + world.getBlockState(target));
            com.exiledradio.updateaquaticbackport.UpdateAquaticBackport.LOGGER.info("SELFTEST kelp: water bucket on {}: {} {}",
                    block.getRegistryName(), full.getType(), full.getResult());
        }
    }

    /** Waterlogging: light, draining, a command placing kelp in the air, falling water, old worlds. */
    private static void waterlogging(Check c, BlockKelp kelp, BlockKelp plant, BlockSeagrass seagrass, BlockSeagrass tall) {
        WorldServer world = c.world;

        // Light: a kelp column dims sky light exactly as the water beside it does, 1 a block.
        BlockPos floor = Pools.pool(c, 9);
        BlockPos col = floor.add(2, 1, 2);
        BlockPos plain = floor.add(9, 1, 9);
        for (int i = 0; i < 9; i++) {
            world.setBlockState(col.up(i), (i == 8 ? kelp : plant).getDefaultState(), 3);
        }
        BlockPos grass = floor.add(9, 1, 2);
        world.setBlockState(grass, seagrass.getDefaultState(), 3);
        boolean same = true;
        StringBuilder light = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            int inKelp = world.getLightFor(EnumSkyBlock.SKY, col.up(i));
            int inWater = world.getLightFor(EnumSkyBlock.SKY, plain.up(i));
            same &= inKelp == inWater;
            light.append(inKelp).append('/').append(inWater).append(' ');
        }
        c.ok(same, "sky light in kelp matches the water beside it, kelp/water by depth: " + light);
        c.eq(world.getLightFor(EnumSkyBlock.SKY, plain.up(8)) - 8, world.getLightFor(EnumSkyBlock.SKY, plain),
                "water dims sky light by 1 a block");
        c.eq(world.getLightFor(EnumSkyBlock.SKY, plain), world.getLightFor(EnumSkyBlock.SKY, grass), "seagrass dims light as water");
        c.eq(world.getBlockState(plain).getLightOpacity(world, plain), world.getBlockState(col).getLightOpacity(world, col) + 1,
                "kelp itself has no opacity; its water has the water's");

        // Drained (as by a sponge or a command), kelp goes with nothing left.
        BlockPos d = floor.add(5, 1, 5);
        world.setBlockState(d, kelp.getDefaultState(), 3);
        c.ok(Waterlogging.setWater(world, d, false, 3) && world.isAirBlock(d) && !Waterlogging.hasWater(world, d),
                "drained kelp is removed: " + world.getBlockState(d));
        c.eq(0, WoodsetSelfTest.countDrops(world, d, Item.getItemFromBlock(kelp)), "drained kelp drops nothing");
        world.setBlockState(d, Blocks.WATER.getDefaultState(), 2);

        // A command setting kelp or seagrass in the air: its water comes with it, at the end of the tick.
        BlockPos air = c.area();
        world.setBlockState(air, kelp.getDefaultState(), 3);
        world.setBlockState(air.east(3), seagrass.getDefaultState(), 3);
        c.ok(!Waterlogging.hasWater(world, air), "kelp set in the air has no water yet");
        c.ok(RequiredWater.flush(world) >= 2 && Waterlogging.hasWaterSource(world, air) && Waterlogging.hasWaterSource(world, air.east(3)),
                "at the end of the tick, kelp and seagrass set in the air hold a water source");

        // Falling water, 1.14's "level 8": kelp can be placed there and makes it a source.
        BlockPos fall = Pools.pool(c, 1).add(4, 1, 4);
        world.setBlockState(fall, Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 8), 2);
        c.ok(kelp.canPlaceBlockAt(world, fall), "kelp can be placed in falling water");
        world.setBlockState(fall.up(), Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 3), 2);
        c.ok(!kelp.canPlaceBlockAt(world, fall.up()) && !seagrass.canPlaceBlockAt(world, fall.up()), "nor in thin flowing water");
        world.setBlockState(fall.up(), Blocks.AIR.getDefaultState(), 2);
        world.setBlockState(fall, kelp.getDefaultState(), 3);
        RequiredWater.flush(world);
        c.ok(Waterlogging.hasWaterSource(world, fall), "kelp in falling water turns it into a source");

        // Old worlds: blocks that were the water in their space get it back once, found in the saved chunk.
        NBTTagCompound level = new NBTTagCompound();
        NBTTagList sections = new NBTTagList();
        NBTTagCompound section = new NBTTagCompound();
        byte[] blocks = new byte[4096];
        byte[] data = new byte[2048];
        byte[] add = new byte[2048];
        int id = Block.getIdFromBlock(kelp);
        int index = (5 << 8) | (3 << 4) | 2; // y 5, z 3, x 2 of the section
        blocks[index] = (byte) (id & 255);
        add[index >> 1] = (byte) (((id >> 8) & 15) << ((index & 1) << 2));
        section.setByte("Y", (byte) 6);
        section.setByteArray("Blocks", blocks);
        section.setByteArray("Data", data);
        section.setByteArray("Add", add);
        sections.appendTag(section);
        level.setTag("Sections", sections);
        c.eq(java.util.Collections.singletonList(new BlockPos(162, 101, -61)), LegacyWater.find(level, 10, -4),
                "an old chunk's kelp is found by its saved id");
        BlockPos old = Pools.pool(c, 1).add(3, 1, 3);
        world.setBlockState(old, kelp.getDefaultState(), 2 | Waterlogging.REMOVE_WATER);
        c.ok(!Waterlogging.hasWater(world, old), "kelp as an old world loads it: no water of its own");
        LegacyWater.queue(world, old);
        c.ok(LegacyWater.run(world) == 1 && Waterlogging.hasWaterSource(world, old), "an old world's kelp gets its water back");
        c.eq(0, LegacyWater.run(world), "and only once");
    }

    private static void place(EntityPlayerMP player, WorldServer world, ItemStack stack, BlockPos on, EnumFacing face) {
        player.setHeldItem(EnumHand.MAIN_HAND, stack);
        stack.getItem().onItemUse(player, world, on, EnumHand.MAIN_HAND, face, 0.5F, 1.0F, 0.5F);
    }

    private static BlockPos topOf(WorldServer world, Block kelp, Block plant, BlockPos from) {
        BlockPos p = from;
        while (world.getBlockState(p.up()).getBlock() == kelp || world.getBlockState(p.up()).getBlock() == plant) {
            p = p.up();
        }
        return p;
    }

    private static void growTo(WorldServer world, BlockKelp kelp, BlockPos bottom, int height) {
        for (int i = 0; i < height; i++) {
            world.setBlockState(bottom.up(i), kelp.getDefaultState(), 3);
        }
    }

    /** @return true for vanilla water, still or flowing */
    static boolean water(WorldServer world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        return b == Blocks.WATER || b == Blocks.FLOWING_WATER;
    }

    private static int drops(WorldServer world, BlockPos from, int height, Item want) {
        int n = 0;
        for (net.minecraft.entity.item.EntityItem e : world.getEntitiesWithinAABB(net.minecraft.entity.item.EntityItem.class,
                new net.minecraft.util.math.AxisAlignedBB(from.add(-2, -1, -2), from.add(3, height, 3)))) {
            if (e.getItem().getItem() == want) {
                n += e.getItem().getCount();
            }
            e.setDead();
        }
        return n;
    }

    private static int count(WorldServer world, BlockPos center, int radius, Block block) {
        int n = 0;
        for (BlockPos p : BlockPos.getAllInBox(center.add(-radius, -2, -radius), center.add(radius, 3, radius))) {
            if (world.getBlockState(p).getBlock() == block) {
                n++;
            }
        }
        return n;
    }
}
