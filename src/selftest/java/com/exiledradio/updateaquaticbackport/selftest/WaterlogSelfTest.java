package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineSlab;
import com.exiledradio.updateaquaticbackport.blocks.BlockPrismarineStairs;
import com.exiledradio.updateaquaticbackport.blocks.BlocksContent;
import com.exiledradio.updateaquaticbackport.common.BlockUnderwater;
import com.exiledradio.updateaquaticbackport.coral.CoralContent;
import com.exiledradio.updateaquaticbackport.coral.CoralType;
import com.exiledradio.updateaquaticbackport.fish.EntityAbstractFish;
import com.exiledradio.updateaquaticbackport.fish.FishContent;
import com.exiledradio.updateaquaticbackport.fish.ItemFishBucket;
import com.exiledradio.updateaquaticbackport.kelp.KelpContent;
import com.exiledradio.updateaquaticbackport.woodset.WoodsetContent;

import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;

/**
 * Waterlogging across streams (stream Wl): water mobs spawning in waterlogged blocks, the vanilla-shaped
 * blocks of this mod that Fluidlogged API's own defaults waterlog, fish buckets on waterloggable blocks, mobs
 * and boats in waterlogged water. Each stream's own blocks are tested in its own test.
 */
public final class WaterlogSelfTest implements SelfTest {

    @Override
    public String name() {
        return "waterlog";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(net.minecraftforge.fml.common.Loader.isModLoaded("fluidlogged_api"), "Fluidlogged API is loaded");

        // 1. water mobs spawn in waterlogged blocks, as 1.14's IN_WATER test allows --------------------------
        BlockPos floor = Pools.pool(c, 4);
        Block kelp = KelpContent.kelp();
        Block plant = KelpContent.kelpPlant();
        BlockPos col = floor.add(3, 1, 3);
        if (kelp != null) {
            for (int i = 0; i < 4; i++) {
                world.setBlockState(col.up(i), (i == 3 ? kelp : plant).getDefaultState(), 3);
            }
            c.ok(WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.IN_WATER, world, col.up()),
                    "water mobs can spawn in kelp");
        }
        c.ok(!WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.IN_WATER, world, floor.add(3, 6, 3)),
                "but not in the air");
        c.ok(WorldEntitySpawner.canCreatureTypeSpawnAtLocation(EntityLiving.SpawnPlacementType.IN_WATER, world, floor.add(8, 2, 8)),
                "and still in plain water");

        // 2. mobs and boats in waterlogged water ----------------------------------------------------------------
        if (kelp != null) {
            EntityPig pig = new EntityPig(world);
            pig.setPosition(col.getX() + 0.5, col.getY() + 1, col.getZ() + 0.5);
            world.spawnEntity(pig);
            pig.setAir(300);
            for (int i = 0; i < 10; i++) {
                pig.onUpdate();
            }
            c.ok(pig.isInWater() && pig.getAir() < 300, "a mob in kelp is in water and loses air: " + pig.getAir());
            pig.setDead();
            EntityBoat boat = new EntityBoat(world, col.getX() + 0.5, col.getY() + 4.2, col.getZ() + 0.5);
            world.spawnEntity(boat);
            for (int i = 0; i < 40; i++) {
                boat.onUpdate();
            }
            c.ok(boat.posY > col.getY() + 3.0, "a boat over kelp at the surface floats: " + boat.posY);
            boat.setDead();
        }

        // 3. prismarine stairs and slabs hold water by Fluidlogged API's defaults; a double slab does not --------
        if (BlocksContent.enabled()) {
            BlockPos row = Pools.pool(c, 1).add(2, 1, 2);
            int i = 0;
            for (BlockPrismarineStairs stairs : BlocksContent.stairs().values()) {
                BlockPos p = row.east(i++);
                world.setBlockState(p, stairs.getDefaultState(), 3);
                c.ok(Waterlogging.hasWaterSource(world, p), stairs.getRegistryName() + " placed in water keeps it");
            }
            for (BlockPrismarineSlab.Half slab : BlocksContent.slabs().values()) {
                BlockPos p = row.east(i++);
                world.setBlockState(p, slab.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.BOTTOM), 3);
                c.ok(Waterlogging.hasWaterSource(world, p), slab.getRegistryName() + " placed in water keeps it");
                BlockPos q = row.east(i++);
                world.setBlockState(q, slab.getDefaultState().withProperty(BlockSlab.HALF, BlockSlab.EnumBlockHalf.TOP), 3);
                c.ok(Waterlogging.hasWaterSource(world, q), slab.getRegistryName() + " (top) placed in water keeps it");
            }
            for (BlockPrismarineSlab.Double slab : BlocksContent.doubleSlabs().values()) {
                BlockPos p = row.south(4).east(i++);
                world.setBlockState(p, slab.getDefaultState(), 3);
                c.ok(!Waterlogging.hasWater(world, p) && !Waterlogging.canHoldWater(slab.getDefaultState(), world, p),
                        slab.getRegistryName() + " refuses water");
            }
        }

        // 4. the sea pickle copy keeps its owner's own water rule -------------------------------------------------
        Block pickle = WoodsetContent.seaPickle();
        if (pickle != null) {
            BlockPos p = Pools.pool(c, 1).add(4, 1, 4);
            world.setBlockState(p, pickle.getDefaultState(), 3);
            UpdateAquaticBackport.LOGGER.info("SELFTEST waterlog: the sea pickle copy holds Fluidlogged API water: {} (its own rule: {})",
                    Waterlogging.hasWater(world, p), world.getBlockState(p));
        }

        // 5. fish buckets on waterloggable blocks ------------------------------------------------------------------
        if (FishContent.codBucket instanceof ItemFishBucket) {
            ItemFishBucket bucket = (ItemFishBucket) FishContent.codBucket;
            BlockPos pool = Pools.pool(c, 2);
            if (kelp != null) {
                BlockPos k = pool.add(2, 1, 2);
                world.setBlockState(k, kelp.getDefaultState(), 3);
                c.ok(bucket.empty(null, world, k, new ItemStack(bucket)), "a fish bucket on kelp lets the fish out");
                c.ok(world.getBlockState(k).getBlock() == kelp && Waterlogging.hasWaterSource(world, k), "and the kelp stays as it was");
                killFish(world, k);
            }
            if (CoralContent.enabled()) {
                BlockPos dryCup = c.area();
                for (net.minecraft.util.EnumFacing f : net.minecraft.util.EnumFacing.HORIZONTALS) {
                    world.setBlockState(dryCup.offset(f), Blocks.STONE.getDefaultState(), 2);
                }
                Block coral = CoralContent.CORALS.get(CoralType.TUBE);
                world.setBlockState(dryCup, coral.getDefaultState(), 2);
                c.ok(bucket.empty(null, world, dryCup, new ItemStack(bucket)) && world.getBlockState(dryCup).getBlock() == coral
                        && Waterlogging.hasWaterSource(world, dryCup), "a fish bucket on a dry coral waterlogs it");
                killFish(world, dryCup);
            }
        }

        // 6. every water-holding block of ours is a waterloggable plant, not water ----------------------------------
        int underwater = 0;
        for (Block b : com.exiledradio.updateaquaticbackport.common.Content.blocks()) {
            IBlockState s = b.getDefaultState();
            if (b instanceof BlockUnderwater) {
                underwater++;
                c.ok(s.getMaterial() != net.minecraft.block.material.Material.WATER && !s.getMaterial().isLiquid(),
                        b.getRegistryName() + " is not a liquid itself");
            }
        }
        c.ok(underwater > 0, "underwater blocks found: " + underwater);
    }

    private static void killFish(WorldServer world, BlockPos at) {
        for (EntityAbstractFish f : world.getEntitiesWithinAABB(EntityAbstractFish.class, new AxisAlignedBB(at).grow(3))) {
            f.setDead();
        }
    }
}
