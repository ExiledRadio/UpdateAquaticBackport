package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.bubble.BlockBubbleColumn;
import com.exiledradio.updateaquaticbackport.bubble.BoatRocking;
import com.exiledradio.updateaquaticbackport.bubble.BubbleColumns;
import com.exiledradio.updateaquaticbackport.bubble.BubbleContent;
import com.exiledradio.updateaquaticbackport.bubble.BubbleGenerator;
import com.exiledradio.backportcore.waterlog.Waterloggable;
import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Bubble columns: forming, changing, pushing, air, boats, buckets and generation. */
public final class BubbleSelfTest implements SelfTest {

    private static final int DEPTH = 12;

    @Override
    public String name() {
        return "bubble";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(BubbleContent.enabled(), "bubbles stream on");
        BlockBubbleColumn column = BubbleContent.column();
        if (column == null) {
            c.fail("no bubble column block");
            return;
        }

        // 1. registration ---------------------------------------------------------------------------
        Block reg = ForgeRegistries.BLOCKS.getValue(Content.id("bubble_column"));
        c.eq(column, reg, "bubble_column registered");
        c.eq(Items.AIR, Item.getItemFromBlock(column), "no bubble column item");
        c.ok(Content.multipartBlocks().contains(column), "bubble column is multipart");
        c.ok(column.getDefaultState().getMaterial() != Material.WATER, "the column itself is not water");
        c.eq(Waterloggable.Need.REQUIRED, column.waterNeed(column.getDefaultState()), "it needs water (always waterlogged)");
        c.ok(column.getDefaultState().getValue(BlockBubbleColumn.DRAG), "default state is a whirlpool");
        int stored = 0;
        for (IBlockState s : column.getBlockState().getValidStates()) {
            c.eq(s, column.getStateFromMeta(column.getMetaFromState(s)), "meta round trip " + s);
            stored++;
        }
        c.eq(2, stored, "two states, both stored");
        for (String n : new String[] {"upwards_ambient", "upwards_inside", "whirlpool_ambient", "whirlpool_inside"}) {
            SoundEvent e = ForgeRegistries.SOUND_EVENTS.getValue(Content.id("block.bubble_column." + n));
            c.ok(e != null, "sound " + n);
        }
        c.ok(column.isReplaceable(world, BlockPos.ORIGIN), "replaceable");
        c.eq(EnumPushReaction.DESTROY, column.getDefaultState().getPushReaction(), "pistons destroy it");
        NonNullList<ItemStack> drops = NonNullList.create();
        column.getDrops(drops, world, BlockPos.ORIGIN, column.getDefaultState(), 0);
        c.ok(drops.isEmpty(), "drops nothing");

        // 2. forming over soul sand and magma ------------------------------------------------------------
        BlockPos floor = Pools.pool(c, DEPTH);
        int top = floor.getY() + DEPTH;
        BlockPos up = floor.add(3, 0, 3);
        world.setBlockState(up, Blocks.SOUL_SAND.getDefaultState(), 3);
        Long due = BubbleColumns.due(world, up);
        c.ok(due != null && due == world.getTotalWorldTime() + 20, "soul sand waits 20 ticks: " + due);
        c.ok(water(world, up.up()), "nothing before the wait");
        BlockPos down = floor.add(6, 0, 3);
        world.setBlockState(down, Blocks.MAGMA.getDefaultState(), 3);
        c.ok(BubbleColumns.due(world, down) != null, "magma waits too");
        BubbleColumns.runDue(world, world.getTotalWorldTime() + 19);
        c.ok(water(world, up.up()), "not due after 19 ticks");
        BubbleColumns.runDue(world, world.getTotalWorldTime() + 20);
        c.ok(isColumn(world, column, up, DEPTH, false), "an upward column fills all " + DEPTH + " water blocks at once");
        c.ok(isColumn(world, column, down, DEPTH, true), "a whirlpool fills all " + DEPTH + " water blocks at once");
        c.ok(world.isAirBlock(up.up(DEPTH + 1)), "and stops at the surface");
        boolean wet = true;
        for (int i = 1; i <= DEPTH; i++) {
            wet &= Waterlogging.hasWaterSource(world, up.up(i)) && Waterlogging.hasWaterSource(world, down.up(i));
        }
        c.ok(wet, "every column block keeps the water source it formed in");
        BlockPos topBlock = new BlockPos(up.getX(), top, up.getZ());
        BlockPos besideTop = new BlockPos(up.getX() + 1, top, up.getZ() + 1);
        BlockPos midBlock = topBlock.down(DEPTH / 2);
        c.eq(world.getLightFor(EnumSkyBlock.SKY, besideTop.down(DEPTH / 2)), world.getLightFor(EnumSkyBlock.SKY, midBlock),
                "light in a column is the water's beside it");

        // Water appearing over a base.
        BlockPos dry = c.area().down();
        world.setBlockState(dry, Blocks.SOUL_SAND.getDefaultState(), 2);
        c.ok(BubbleColumns.due(world, dry) == null, "soul sand placed without updates waits for nothing");
        world.setBlockState(dry.up(), Blocks.WATER.getDefaultState(), 3);
        c.ok(BubbleColumns.due(world, dry) != null, "water placed over soul sand starts the wait");

        // Not in flowing water, not through kelp.
        BlockPos flowing = floor.add(9, 0, 3);
        world.setBlockState(flowing.up(), Blocks.FLOWING_WATER.getDefaultState().withProperty(BlockLiquid.LEVEL, 3), 2);
        world.setBlockState(flowing, Blocks.SOUL_SAND.getDefaultState(), 3);
        BlockPos kelped = floor.add(9, 0, 6);
        Block kelp = Owners.ourBlock("kelp");
        if (kelp != null) {
            world.setBlockState(kelped.up(2), kelp.getDefaultState(), 2);
        }
        world.setBlockState(kelped, Blocks.SOUL_SAND.getDefaultState(), 3);
        BubbleColumns.runDue(world, Long.MAX_VALUE);
        c.ok(world.getBlockState(flowing.up()).getBlock() != column && world.getBlockState(flowing.up(2)).getBlock() != column,
                "no column through flowing water");
        if (kelp != null) {
            c.eq(column, world.getBlockState(kelped.up()).getBlock(), "column under kelp");
            c.eq(kelp, world.getBlockState(kelped.up(2)).getBlock(), "kelp stays");
            c.ok(water(world, kelped.up(3)), "no column above kelp");
        }

        // 3. pushing and pulling -------------------------------------------------------------------------
        pushing(c, world, column, up, down, top);

        // 4. air ------------------------------------------------------------------------------------------
        air(c, world, up, floor.add(1, 0, 10));

        // 5. boats ----------------------------------------------------------------------------------------
        boats(c, world, column, up, down, floor.add(10, 0, 10), top);

        // 6. changing the base ------------------------------------------------------------------------------
        world.setBlockState(up, Blocks.MAGMA.getDefaultState(), 3);
        c.ok(isColumn(world, column, up, DEPTH, false), "a new base changes nothing at once");
        Pools.settle(world);
        c.ok(isColumn(world, column, up, DEPTH, true), "5 ticks later the whole column is a whirlpool");
        BubbleColumns.runDue(world, Long.MAX_VALUE);
        c.ok(isColumn(world, column, up, DEPTH, true), "and the magma's own tick leaves it so");
        world.setBlockState(up, Blocks.STONE.getDefaultState(), 3);
        Pools.settle(world);
        boolean allWater = true;
        for (int i = 1; i <= DEPTH; i++) {
            allWater &= water(world, up.up(i));
        }
        c.ok(allWater, "base gone: the whole column is water again");
        world.setBlockState(down, Blocks.AIR.getDefaultState(), 3);
        Pools.settle(world);
        c.ok(world.getBlockState(down).getMaterial() == Material.WATER, "base dug out: water flows down into it");
        c.ok(world.getBlockState(down.up(DEPTH / 2)).getBlock() != column, "and no column is left");

        // New water over the top.
        BlockPos grow = floor.add(3, 0, 9);
        world.setBlockState(grow, Blocks.SOUL_SAND.getDefaultState(), 3);
        BubbleColumns.runDue(world, Long.MAX_VALUE);
        BlockPos over = new BlockPos(grow.getX(), top + 1, grow.getZ());
        for (BlockPos side : new BlockPos[] {over.north(), over.south(), over.east(), over.west()}) {
            world.setBlockState(side, Blocks.STONE.getDefaultState(), 2);
        }
        world.setBlockState(over, Blocks.WATER.getDefaultState(), 3);
        Pools.settle(world);
        c.eq(column.withDrag(false), world.getBlockState(over), "a new water source over the top joins the column");

        // Freezing, in whatever biome the test lands: forced cold here, a column's own blocks never freeze,
        // plain water beside it does (vanilla's freezing pass and the frozen oceans' both read plain water only).
        BlockPos plainTop = new BlockPos(grow.getX() + 1, top, grow.getZ());
        byte[] biomes = world.getChunk(over).getBiomeArray();
        int[] cells = {(over.getZ() & 15) << 4 | (over.getX() & 15), (plainTop.getZ() & 15) << 4 | (plainTop.getX() & 15)};
        byte[] saved = {biomes[cells[0]], biomes[cells[1]]};
        byte cold = (byte) Biome.getIdForBiome(Biomes.ICE_PLAINS);
        biomes[cells[0]] = cold;
        biomes[cells[1]] = cold;
        c.ok(!world.canBlockFreezeWater(over), "a column never freezes, even in a frozen biome");
        c.ok(water(world, plainTop) && world.canBlockFreezeWater(plainTop), "plain water beside it can");
        biomes[cells[0]] = saved[0];
        biomes[cells[1]] = saved[1];

        // 7. buckets and ray traces -----------------------------------------------------------------------
        BlockPos colTop = new BlockPos(grow.getX(), top, grow.getZ());
        c.eq(column, world.getBlockState(colTop.up()).getBlock(), "the grown top is column");
        EntityPlayerMP player = Players.real(world, "bubbles");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        Vec3d eye = new Vec3d(grow.getX() + 0.5, top + 3.6, grow.getZ() + 0.5);
        RayTraceResult through = world.rayTraceBlocks(eye, eye.add(0, -DEPTH - 6, 0), false, false, false);
        c.ok(through != null && through.getBlockPos().equals(grow), "an ordinary ray trace goes through to the base: "
                + (through == null ? null : through.getBlockPos()));
        BlockPos bucketAt = colTop.up();
        player.setPositionAndRotation(bucketAt.getX() + 0.5, bucketAt.getY() + 1.0, bucketAt.getZ() + 0.5, 0.0F, 90.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.BUCKET));
        ActionResult<ItemStack> filled = Items.BUCKET.onItemRightClick(world, player, EnumHand.MAIN_HAND);
        c.eq(Items.WATER_BUCKET, filled.getResult().getItem(), "an empty bucket fills from a column");
        c.ok(world.isAirBlock(bucketAt), "and leaves air");
        c.eq(column, world.getBlockState(colTop).getBlock(), "the column below stays");

        // 8. generation -------------------------------------------------------------------------------------
        c.ok(ModConfig.bubbles.generate, "generation on");
        BlockPos gen = Pools.pool(c, 5);
        world.setBlockState(gen.add(2, 0, 2), Blocks.MAGMA.getDefaultState(), 2);
        world.setBlockState(gen.add(5, 0, 5), Blocks.SOUL_SAND.getDefaultState(), 2);
        int formed = BubbleGenerator.formInArea(world, gen.getX(), gen.getZ());
        c.eq(2, formed, "the generator starts a column over each base under water");
        c.ok(isColumn(world, column, gen.add(2, 0, 2), 5, true), "generated whirlpool");
        c.ok(isColumn(world, column, gen.add(5, 0, 5), 5, false), "generated upward column");
        c.eq(0, BubbleGenerator.formInArea(world, gen.getX(), gen.getZ()), "a second pass starts nothing");
    }

    private static void pushing(Check c, WorldServer world, BlockBubbleColumn column, BlockPos up, BlockPos down, int top) {
        BlockPos inUp = up.up(3);
        BlockPos inDown = down.up(3);
        BlockPos topUp = new BlockPos(up.getX(), top, up.getZ());
        BlockPos topDown = new BlockPos(down.getX(), top, down.getZ());
        EntityItem item = new EntityItem(world, inUp.getX() + 0.5, inUp.getY(), inUp.getZ() + 0.5, new ItemStack(Items.STICK));
        item.motionY = 0.0;
        item.fallDistance = 3.0F;
        column.onEntityCollision(world, inUp, world.getBlockState(inUp), item);
        close(c, 0.06, item.motionY, "inside an upward column +0.06");
        c.eq(0.0F, item.fallDistance, "inside, fall distance resets");
        item.motionY = 0.69;
        column.onEntityCollision(world, inUp, world.getBlockState(inUp), item);
        close(c, 0.7, item.motionY, "inside, up to 0.7");
        item.motionY = 0.0;
        column.onEntityCollision(world, inDown, world.getBlockState(inDown), item);
        close(c, -0.03, item.motionY, "inside a whirlpool -0.03");
        item.motionY = -0.29;
        column.onEntityCollision(world, inDown, world.getBlockState(inDown), item);
        close(c, -0.3, item.motionY, "inside, down to -0.3");
        item.motionY = -0.5;
        column.onEntityCollision(world, inDown, world.getBlockState(inDown), item);
        close(c, -0.3, item.motionY, "inside, a faster fall is slowed to -0.3");
        item.motionY = 0.0;
        item.fallDistance = 3.0F;
        column.onEntityCollision(world, topUp, world.getBlockState(topUp), item);
        close(c, 0.1, item.motionY, "on top of an upward column +0.1");
        c.eq(3.0F, item.fallDistance, "on top, fall distance kept");
        item.motionY = 1.75;
        column.onEntityCollision(world, topUp, world.getBlockState(topUp), item);
        close(c, 1.8, item.motionY, "on top, up to 1.8");
        item.motionY = -0.88;
        column.onEntityCollision(world, topDown, world.getBlockState(topDown), item);
        close(c, -0.9, item.motionY, "on top of a whirlpool, down to -0.9");

        // Moving for real, through the entity's own block collisions.
        EntityItem riser = new EntityItem(world, up.getX() + 0.5, up.getY() + 2.0, up.getZ() + 0.5, new ItemStack(Items.STICK));
        riser.motionX = riser.motionY = riser.motionZ = 0.0;
        world.spawnEntity(riser);
        double start = riser.posY;
        for (int i = 0; i < 40; i++) {
            riser.onUpdate();
        }
        c.ok(riser.posY > start + 1.0, "an item rises in an upward column: " + start + " -> " + riser.posY);
        riser.setDead();

        EntityPig sinking = pig(world, down.up(DEPTH / 2));
        EntityPig floating = pig(world, down.add(-2, 0, 0).up(DEPTH / 2));
        double s0 = sinking.posY;
        double f0 = floating.posY;
        for (int i = 0; i < 40; i++) {
            sinking.onUpdate();
            floating.onUpdate();
        }
        c.ok(sinking.posY < s0 - 0.5, "a pig is pulled down a whirlpool: " + s0 + " -> " + sinking.posY);
        c.ok(floating.posY > f0, "beside it, a pig swims up: " + f0 + " -> " + floating.posY);
        sinking.setDead();
        floating.setDead();
    }

    private static void air(Check c, WorldServer world, BlockPos up, BlockPos plain) {
        EntityPig inColumn = pig(world, up.up(2));
        inColumn.setAir(100);
        for (int i = 0; i < 10; i++) {
            inColumn.onUpdate();
        }
        c.eq(140, inColumn.getAir(), "air comes back 4 a tick with eyes in a column");
        inColumn.setPosition(up.getX() + 0.5, up.getY() + 2, up.getZ() + 0.5);
        inColumn.setAir(298);
        for (int i = 0; i < 3; i++) {
            inColumn.onUpdate();
        }
        c.ok(inColumn.getAir() >= 298 && inColumn.getAir() <= 300, "and stops at full: " + inColumn.getAir());
        inColumn.setDead();
        EntityPig inWater = pig(world, plain.up(2));
        inWater.setAir(100);
        for (int i = 0; i < 10; i++) {
            inWater.onUpdate();
        }
        c.eq(90, inWater.getAir(), "in plain water air still runs out");
        inWater.setDead();
    }

    private static void boats(Check c, WorldServer world, BlockBubbleColumn column, BlockPos up, BlockPos down, BlockPos plain, int top) {
        // Sinking, with a passenger thrown off.
        EntityBoat sinker = boat(world, down, top);
        EntityPig rider = new EntityPig(world);
        rider.setPosition(sinker.posX, sinker.posY, sinker.posZ);
        world.spawnEntity(rider);
        rider.startRiding(sinker, true);
        double launched = Double.NaN;
        int at = -1;
        int first = -1;
        for (int i = 0; i < 80 && at < 0; i++) {
            sinker.onUpdate();
            BoatRocking.tickServer(world);
            if (i == 0) {
                first = BoatRocking.ticks(sinker);
            }
            if (i > 0 && BoatRocking.ticks(sinker) == 0) {
                at = i;
                launched = sinker.motionY;
            }
        }
        c.eq(59, first, "a boat on a whirlpool starts rocking");
        c.eq(59, at, "and sinks after 60 ticks");
        c.ok(launched <= -0.6, "pulled down: motion " + launched);
        c.ok(!sinker.isBeingRidden(), "its passenger is thrown off");
        sinker.setDead();
        rider.setDead();

        // Thrown up, empty.
        EntityBoat thrown = boat(world, up, top);
        double upMotion = Double.NaN;
        for (int i = 0; i < 80 && Double.isNaN(upMotion); i++) {
            thrown.onUpdate();
            BoatRocking.tickServer(world);
            if (i > 0 && BoatRocking.ticks(thrown) == 0) {
                upMotion = thrown.motionY;
            }
        }
        close(c, 0.6, upMotion, "an empty boat on an upward column is thrown up at 0.6");
        thrown.setDead();

        // Leaving before the count ends.
        EntityBoat leaver = boat(world, down, top);
        for (int i = 0; i < 30; i++) {
            leaver.onUpdate();
            BoatRocking.tickServer(world);
        }
        c.ok(BoatRocking.ticks(leaver) > 0, "rocking after 30 ticks");
        leaver.setPosition(plain.getX() + 0.5, top + 0.75, plain.getZ() + 0.5);
        leaver.onUpdate();
        BoatRocking.tickServer(world);
        c.eq(0, BoatRocking.ticks(leaver), "a boat that leaves stops rocking");
        c.ok(leaver.motionY > -0.5, "and does not sink: " + leaver.motionY);
        leaver.setDead();
    }

    private static EntityBoat boat(WorldServer world, BlockPos base, int top) {
        EntityBoat boat = new EntityBoat(world, base.getX() + 0.5, top + 0.75, base.getZ() + 0.5);
        world.spawnEntity(boat);
        return boat;
    }

    private static EntityPig pig(WorldServer world, BlockPos at) {
        EntityPig pig = new EntityPig(world);
        pig.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        world.spawnEntity(pig);
        return pig;
    }

    private static void close(Check c, double expected, double actual, String what) {
        c.ok(Math.abs(expected - actual) < 1.0E-6, what + ": expected " + expected + ", got " + actual);
    }

    /** @return true if the {@code height} blocks over {@code base} are all this column with this drag */
    private static boolean isColumn(WorldServer world, BlockBubbleColumn column, BlockPos base, int height, boolean drag) {
        for (int i = 1; i <= height; i++) {
            if (world.getBlockState(base.up(i)) != column.withDrag(drag)) {
                return false;
            }
        }
        return true;
    }

    private static boolean water(WorldServer world, BlockPos pos) {
        Block b = world.getBlockState(pos).getBlock();
        return b == Blocks.WATER || b == Blocks.FLOWING_WATER;
    }
}
