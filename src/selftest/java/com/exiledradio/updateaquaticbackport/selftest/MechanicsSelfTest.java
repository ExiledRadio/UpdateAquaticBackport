package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.mechanics.BlockShape;
import com.exiledradio.updateaquaticbackport.mechanics.EntityFloorItemFrame;
import com.exiledradio.updateaquaticbackport.mechanics.MechanicsHooks;
import com.exiledradio.updateaquaticbackport.mechanics.SquidSpawning;

import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockLeaves;
import net.minecraft.block.BlockOldLeaf;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.monster.EntityWitch;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFlowerPot;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.PlaySoundAtEntityEvent;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** 1.13's behavior changes to existing things (stream mechanics, docs/streams/mechanics.md). */
public final class MechanicsSelfTest implements SelfTest {

    @Override
    public String name() {
        return "mechanics";
    }

    @Override
    public void run(Check c) throws Exception {
        leaves(c);
        squid(c);
        frames(c);
        cactus(c);
        sounds(c);
        witchHut(c);
        sleep(c);
        outlines(c);
    }

    // ------------------------------------------------------------------------------------------ leaves

    private static IBlockState leaf(boolean check) {
        return Blocks.LEAVES.getDefaultState().withProperty(BlockOldLeaf.VARIANT, BlockPlanks.EnumType.OAK)
                .withProperty(BlockLeaves.DECAYABLE, true).withProperty(BlockLeaves.CHECK_DECAY, check);
    }

    /** A log at x+8 and a row of 8 leaves east of it, at distances 1 to 8. */
    private static BlockPos leafRow(Check c, BlockPos corner, boolean check) {
        BlockPos log = corner.add(8, 6, 8);
        c.world.setBlockState(log, Blocks.LOG.getDefaultState(), 2);
        for (int d = 1; d <= 8; d++) {
            c.world.setBlockState(log.east(d), leaf(check), 2 | 16);
        }
        return log;
    }

    private static void tickRow(Check c, BlockPos log) {
        Random rand = new Random(1);
        for (int d = 8; d >= 1; d--) {
            IBlockState state = c.world.getBlockState(log.east(d));
            if (state.getBlock() == Blocks.LEAVES) {
                state.getBlock().updateTick(c.world, log.east(d), state, rand);
            }
        }
    }

    private void leaves(Check c) {
        BlockPos corner = c.area();
        BlockPos log = leafRow(c, corner, true);
        for (int d = 1; d <= 8; d++) {
            c.eq(Math.min(d, 7), MechanicsHooks.distanceToLog(c.world, log.east(d)), "leaf distance " + d);
        }
        tickRow(c, log);
        for (int d = 1; d <= 6; d++) {
            IBlockState s = c.world.getBlockState(log.east(d));
            c.ok(s.getBlock() == Blocks.LEAVES && !s.getValue(BlockLeaves.CHECK_DECAY), "leaf " + d + " from a log stays, unmarked");
        }
        c.ok(c.world.isAirBlock(log.east(7)) && c.world.isAirBlock(log.east(8)), "leaves 7 and 8 from a log decay");

        // A diagonal step is not a connection: leaves reach through faces only (1.12.2 and 1.13 alike).
        c.world.setBlockState(log.east(1), Blocks.AIR.getDefaultState(), 2 | 16);
        c.world.setBlockState(log.add(1, 1, 1), leaf(false), 2 | 16);
        c.eq(7, MechanicsHooks.distanceToLog(c.world, log.add(1, 1, 1)), "a leaf touching a log only by an edge is not held");

        // Setting off: 1.12.2's 4.
        c.fill(corner.add(0, 1, 0), corner.add(31, 30, 31), Blocks.AIR.getDefaultState());
        ModConfig.mechanics.leafDistance = false;
        try {
            log = leafRow(c, corner, true);
            tickRow(c, log);
            c.ok(c.world.getBlockState(log.east(4)).getBlock() == Blocks.LEAVES, "setting off: leaf 4 stays");
            c.ok(c.world.isAirBlock(log.east(5)), "setting off: leaf 5 decays as in 1.12.2");
        } finally {
            ModConfig.mechanics.leafDistance = true;
        }

        // A broken log marks the leaves 6 around (1.12.2: 4), so leaves it held 5 and 6 away get checked.
        c.fill(corner.add(0, 1, 0), corner.add(31, 30, 31), Blocks.AIR.getDefaultState());
        log = leafRow(c, corner, false);
        c.world.setBlockToAir(log);
        c.ok(c.world.getBlockState(log.east(6)).getValue(BlockLeaves.CHECK_DECAY), "a broken log marks the leaf 6 away to check");
        c.ok(!c.world.getBlockState(log.east(7)).getValue(BlockLeaves.CHECK_DECAY), "a broken log leaves the leaf 7 away alone");
        tickRow(c, log);
        c.ok(c.world.isAirBlock(log.east(5)) && c.world.isAirBlock(log.east(6)), "leaves 5 and 6 away from a broken log decay");
        c.fill(corner.add(0, 1, 0), corner.add(31, 30, 31), Blocks.AIR.getDefaultState());
    }

    // ------------------------------------------------------------------------------------------- squid

    private static boolean listsSquid(Biome biome) {
        for (Biome.SpawnListEntry e : biome.getSpawnableList(EnumCreatureType.WATER_CREATURE)) {
            if (e.entityClass == EntitySquid.class) {
                return true;
            }
        }
        return false;
    }

    private void squid(Check c) {
        for (Biome b : new Biome[] {Biomes.OCEAN, Biomes.DEEP_OCEAN, Biomes.FROZEN_OCEAN, Biomes.RIVER, Biomes.FROZEN_RIVER}) {
            c.ok(listsSquid(b), "squid still spawn in " + b.getRegistryName());
        }
        for (Biome b : new Biome[] {Biomes.SWAMPLAND, Biomes.PLAINS, Biomes.BEACH, Biomes.FOREST, Biomes.JUNGLE, Biomes.TAIGA, Biomes.MUSHROOM_ISLAND}) {
            c.ok(!listsSquid(b), "no squid in " + b.getRegistryName());
        }
        for (Biome b : net.minecraftforge.fml.common.registry.ForgeRegistries.BIOMES) {
            if (b.getRegistryName().getNamespace().equals("updateaquaticbackport")) {
                c.ok(listsSquid(b), "squid spawn in this mod's " + b.getRegistryName());
            }
        }
        // A natural spawn outside the oceans and rivers is refused; a spawner's is not.
        BlockPos swamp = c.world.getBiomeProvider().findBiomePosition(0, 0, 2000, Collections.singletonList(Biomes.SWAMPLAND), new Random(4));
        if (swamp != null) {
            EntitySquid squid = new EntitySquid(c.world);
            squid.setPosition(swamp.getX() + 0.5, 55, swamp.getZ() + 0.5);
            c.eq(Biomes.SWAMPLAND, c.world.getBiome(squid.getPosition()), "the swamp found");
            LivingSpawnEvent.CheckSpawn natural = new LivingSpawnEvent.CheckSpawn(squid, c.world, (float) squid.posX, 55, (float) squid.posZ, false);
            MinecraftForge.EVENT_BUS.post(natural);
            c.eq(Event.Result.DENY, natural.getResult(), "a squid spawning naturally in a swamp");
            LivingSpawnEvent.CheckSpawn spawner = new LivingSpawnEvent.CheckSpawn(squid, c.world, (float) squid.posX, 55, (float) squid.posZ, true);
            MinecraftForge.EVENT_BUS.post(spawner);
            c.eq(Event.Result.DEFAULT, spawner.getResult(), "a squid from a spawner in a swamp");
        } else {
            c.fail("no swamp found within 2000 blocks of 0, 0 to test natural squid spawns");
        }
        c.ok(SquidSpawning.squidBiome(Biomes.RIVER) && !SquidSpawning.squidBiome(Biomes.SWAMPLAND), "squid biomes: rivers yes, swamps no");
    }

    // ------------------------------------------------------------------------------------- item frames

    private static List<EntityFloorItemFrame> frames(Check c, BlockPos corner) {
        return c.world.getEntitiesWithinAABB(EntityFloorItemFrame.class, new AxisAlignedBB(corner, corner.add(32, 60, 32)));
    }

    private void frames(Check c) {
        BlockPos corner = c.area();
        BlockPos floor = corner.add(4, -1, 4);
        EntityFloorItemFrame up = new EntityFloorItemFrame(c.world, floor.up(), EnumFacing.UP);
        c.ok(up.onValidSurface(), "a frame on the floor stays");
        c.eq(-90.0F, up.rotationPitch, "a floor frame's pitch");
        AxisAlignedBB box = up.getEntityBoundingBox();
        c.ok(Math.abs(box.maxY - box.minY - 1 / 16.0) < 1e-6 && Math.abs(box.maxX - box.minX - 0.75) < 1e-6
                && Math.abs(box.minY - (floor.getY() + 1)) < 1e-6, "a floor frame's box is 12 by 12 pixels, 1 thick, on the floor: " + box);
        EntityFloorItemFrame hanging = new EntityFloorItemFrame(c.world, floor.up(5), EnumFacing.DOWN);
        c.ok(!hanging.onValidSurface(), "a ceiling frame with nothing above falls");
        c.world.setBlockState(floor.up(6), Blocks.STONE.getDefaultState(), 2);
        c.ok(hanging.onValidSurface(), "a ceiling frame under stone stays");
        c.eq(90.0F, hanging.rotationPitch, "a ceiling frame's pitch");

        NBTTagCompound tag = new NBTTagCompound();
        up.setDisplayedItem(new ItemStack(Items.DIAMOND));
        up.writeToNBT(tag);
        c.eq((byte) 1, tag.getByte("Facing"), "Facing saved as 1.13's index (up = 1)");
        EntityFloorItemFrame read = new EntityFloorItemFrame(c.world);
        read.readFromNBT(tag);
        c.eq(EnumFacing.UP, read.facingDirection, "a floor frame read back faces up");
        c.eq(floor.up(), read.getHangingPosition(), "a floor frame read back is where it was");
        c.eq(Items.DIAMOND, read.getDisplayedItem().getItem(), "a floor frame read back keeps its item");

        // A player using an item frame looking down at the floor places one; the stack goes down by one.
        EntityPlayerMP player = Players.real(c.world, "frame");
        player.setPositionAndRotation(corner.getX() + 10.5, corner.getY(), corner.getZ() + 10.5, 0.0F, 90.0F);
        player.setHeldItem(EnumHand.MAIN_HAND, new ItemStack(Items.ITEM_FRAME, 2));
        int before = frames(c, corner).size();
        EnumActionResult result = ForgeHooks.onItemRightClick(player, EnumHand.MAIN_HAND);
        c.eq(EnumActionResult.SUCCESS, result, "using an item frame on the floor");
        List<EntityFloorItemFrame> after = frames(c, corner);
        c.eq(before + 1, after.size(), "frames on the floor after using one");
        c.eq(1, player.getHeldItemMainhand().getCount(), "item frames left");
        boolean placed = false;
        for (EntityFloorItemFrame f : after) {
            placed |= f.getHangingPosition().equals(corner.add(10, 0, 10)) && f.facingDirection == EnumFacing.UP;
        }
        c.ok(placed, "the frame is on the floor block looked at, facing up");
        // Looking at a wall leaves the frame to vanilla (no floor frame, nothing used up here).
        player.setPositionAndRotation(corner.getX() + 10.5, corner.getY(), corner.getZ() + 10.5, 0.0F, 0.0F);
        c.ok(ForgeHooks.onItemRightClick(player, EnumHand.MAIN_HAND) == null, "an item frame used at nothing is vanilla's");
        ModConfig.mechanics.floorItemFrames = false;
        try {
            player.setPositionAndRotation(corner.getX() + 14.5, corner.getY(), corner.getZ() + 14.5, 0.0F, 90.0F);
            c.ok(ForgeHooks.onItemRightClick(player, EnumHand.MAIN_HAND) == null, "setting off: no floor frame");
        } finally {
            ModConfig.mechanics.floorItemFrames = true;
        }
        for (Entity e : frames(c, corner)) {
            e.setDead();
        }
    }

    // ------------------------------------------------------------------------------------ cactus signs

    private void cactus(Check c) {
        BlockPos corner = c.area();
        BlockPos sand = corner.add(4, 0, 4);
        c.world.setBlockState(sand, Blocks.SAND.getDefaultState(), 2);
        c.world.setBlockState(sand.up(), Blocks.CACTUS.getDefaultState(), 2);
        BlockPos beside = sand.up().east();
        c.ok(Blocks.STANDING_SIGN.canPlaceBlockAt(c.world, beside), "a sign can be placed beside a cactus");
        c.ok(Blocks.STANDING_BANNER.canPlaceBlockAt(c.world, beside), "a banner can be placed beside a cactus");
        ModConfig.mechanics.cactusBreaksSigns = false;
        try {
            c.ok(!Blocks.STANDING_SIGN.canPlaceBlockAt(c.world, beside), "setting off: no sign beside a cactus");
            c.ok(!Blocks.STANDING_BANNER.canPlaceBlockAt(c.world, beside), "setting off: no banner beside a cactus");
        } finally {
            ModConfig.mechanics.cactusBreaksSigns = true;
        }
        c.world.setBlockState(beside, Blocks.STANDING_SIGN.getDefaultState(), 3);
        c.ok(c.world.getBlockState(sand.up()).getBlock() != Blocks.CACTUS, "the cactus breaks beside a sign");
    }

    // ------------------------------------------------------------------------------------ place sounds

    public static final class Sounds {
        final List<SoundEvent> played = new ArrayList<SoundEvent>();

        @SubscribeEvent
        public void onSound(PlaySoundAtEntityEvent event) {
            this.played.add(event.getSound());
        }
    }

    private void sounds(Check c) {
        BlockPos corner = c.area();
        EntityPlayerMP player = Players.real(c.world, "sounds");
        player.setPosition(corner.getX() + 2.5, corner.getY(), corner.getZ() + 2.5);
        Object[][] cases = {
                {new ItemStack(Items.SIGN), SoundEvents.BLOCK_WOOD_PLACE, "a sign"},
                {new ItemStack(Items.BANNER), SoundEvents.BLOCK_WOOD_PLACE, "a banner"},
                {new ItemStack(Items.SKULL), SoundEvents.BLOCK_STONE_PLACE, "a head"},
                {new ItemStack(Items.REDSTONE), SoundEvents.BLOCK_STONE_PLACE, "redstone dust"},
        };
        int x = 6;
        for (Object[] k : cases) {
            Sounds sounds = new Sounds();
            MinecraftForge.EVENT_BUS.register(sounds);
            try {
                ItemStack stack = (ItemStack) k[0];
                player.setHeldItem(EnumHand.MAIN_HAND, stack);
                BlockPos on = corner.add(x, -1, 6);
                x += 3;
                EnumActionResult r = ForgeHooks.onPlaceItemIntoWorld(stack, player, c.world, on, EnumFacing.UP, 0.5F, 1.0F, 0.5F, EnumHand.MAIN_HAND);
                c.eq(EnumActionResult.SUCCESS, r, "placing " + k[2]);
                c.ok(sounds.played.contains(k[1]), k[2] + " makes its place sound: " + sounds.played);
            } finally {
                MinecraftForge.EVENT_BUS.unregister(sounds);
            }
        }
        ModConfig.mechanics.placementSounds = false;
        Sounds sounds = new Sounds();
        MinecraftForge.EVENT_BUS.register(sounds);
        try {
            ItemStack stack = new ItemStack(Items.SIGN);
            player.setHeldItem(EnumHand.MAIN_HAND, stack);
            ForgeHooks.onPlaceItemIntoWorld(stack, player, c.world, corner.add(20, -1, 6), EnumFacing.UP, 0.5F, 1.0F, 0.5F, EnumHand.MAIN_HAND);
            c.ok(!sounds.played.contains(SoundEvents.BLOCK_WOOD_PLACE), "setting off: a sign is placed silently");
        } finally {
            MinecraftForge.EVENT_BUS.unregister(sounds);
            ModConfig.mechanics.placementSounds = true;
        }
    }

    // --------------------------------------------------------------------------------------- witch hut

    private static ItemStack hutPot(Check c, BlockPos corner, boolean on) {
        ModConfig.mechanics.witchHutMushroom = on;
        try {
            Random rand = new Random(7);
            ComponentScatteredFeaturePieces.SwampHut hut = new ComponentScatteredFeaturePieces.SwampHut(rand, corner.getX() + 4, corner.getZ() + 4);
            StructureBoundingBox all = new StructureBoundingBox(corner.getX(), 1, corner.getZ(), corner.getX() + 31, 255, corner.getZ() + 31);
            hut.addComponentParts(c.world, rand, all);
            StructureBoundingBox box = hut.getBoundingBox();
            for (BlockPos p : BlockPos.getAllInBox(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
                TileEntity tile = c.world.getTileEntity(p);
                if (tile instanceof TileEntityFlowerPot) {
                    return ((TileEntityFlowerPot) tile).getFlowerItemStack();
                }
            }
            return null;
        } finally {
            ModConfig.mechanics.witchHutMushroom = true;
            for (EntityWitch w : c.world.getEntitiesWithinAABB(EntityWitch.class, new AxisAlignedBB(corner.add(-8, -40, -8), corner.add(40, 60, 40)))) {
                w.setDead();
            }
        }
    }

    private void witchHut(Check c) {
        ItemStack on = hutPot(c, c.area(), true);
        c.ok(on != null && on.getItem() == itemOf(Blocks.RED_MUSHROOM), "a new witch hut's pot holds a red mushroom: " + on);
        ItemStack off = hutPot(c, c.area(), false);
        c.ok(off != null && off.isEmpty(), "setting off: the pot is empty as in 1.12.2: " + off);
    }

    private static net.minecraft.item.Item itemOf(net.minecraft.block.Block block) {
        return net.minecraft.item.Item.getItemFromBlock(block);
    }

    // ----------------------------------------------------------------------------------- creative sleep

    private void sleep(Check c) {
        BlockPos corner = c.area();
        WorldServer world = c.world;
        long time = world.getWorldTime();
        BlockPos foot = corner.add(8, 0, 8);
        world.setBlockState(foot, Blocks.BED.getDefaultState().withProperty(BlockBed.FACING, EnumFacing.NORTH)
                .withProperty(BlockBed.PART, BlockBed.EnumPartType.FOOT), 2);
        world.setBlockState(foot.north(), Blocks.BED.getDefaultState().withProperty(BlockBed.FACING, EnumFacing.NORTH)
                .withProperty(BlockBed.PART, BlockBed.EnumPartType.HEAD), 2);
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(foot.getX() + 4.5, foot.getY(), foot.getZ() + 0.5);
        world.spawnEntity(zombie);
        EntityPlayerMP player = Players.real(world, "sleep");
        try {
            world.setWorldTime(18000);
            world.calculateInitialSkylight();
            player.setPosition(foot.getX() + 1.5, foot.getY(), foot.getZ() + 0.5);
            player.interactionManager.setGameType(GameType.SURVIVAL);
            c.eq(EntityPlayer.SleepResult.NOT_SAFE, player.trySleep(foot.north()), "survival with a zombie near");
            player.interactionManager.setGameType(GameType.CREATIVE);
            c.eq(EntityPlayer.SleepResult.OK, player.trySleep(foot.north()), "creative with a zombie near");
            if (player.isPlayerSleeping()) {
                player.wakeUpPlayer(true, false, false);
            }
            ModConfig.mechanics.creativeSleep = false;
            player.setPosition(foot.getX() + 1.5, foot.getY(), foot.getZ() + 0.5);
            c.eq(EntityPlayer.SleepResult.NOT_SAFE, player.trySleep(foot.north()), "setting off: creative with a zombie near");
        } finally {
            ModConfig.mechanics.creativeSleep = true;
            if (player.isPlayerSleeping()) {
                player.wakeUpPlayer(true, false, false);
            }
            world.setWorldTime(time);
            world.calculateInitialSkylight();
            zombie.setDead();
        }
    }

    // ---------------------------------------------------------------------------------------- outlines

    private void outlines(Check c) {
        BlockPos corner = c.area();
        BlockPos p = corner.add(4, 0, 4);
        c.world.setBlockState(p, Blocks.STONE.getDefaultState(), 2);
        c.ok(BlockShape.edges(c.world, p, c.world.getBlockState(p), null) == null, "stone keeps the plain box");

        IBlockState stairs = Blocks.OAK_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.NORTH);
        c.world.setBlockState(p, stairs, 2);
        List<double[]> edges = BlockShape.edges(c.world, p, stairs.getActualState(c.world, p), null);
        c.eq(18, edges == null ? -1 : edges.size(), "a straight stair's outline: an L-shaped prism has 18 edges");

        // A fence with arms north and east: an L seen from above, 1 block high (its collision is 1.5).
        BlockPos f = corner.add(10, 0, 10);
        c.world.setBlockState(f, Blocks.OAK_FENCE.getDefaultState(), 2);
        c.world.setBlockState(f.north(), Blocks.STONE.getDefaultState(), 2);
        c.world.setBlockState(f.east(), Blocks.STONE.getDefaultState(), 2);
        edges = BlockShape.edges(c.world, f, Blocks.OAK_FENCE.getActualState(Blocks.OAK_FENCE.getDefaultState(), c.world, f), null);
        c.eq(18, edges == null ? -1 : edges.size(), "a corner fence's outline");
        double top = 0;
        if (edges != null) {
            for (double[] e : edges) {
                top = Math.max(top, Math.max(e[1], e[4]));
            }
        }
        c.eq(1.0, top, "a fence's outline is 1 block high");

        c.world.setBlockState(f.north(), Blocks.AIR.getDefaultState(), 2);
        c.world.setBlockState(f.east(), Blocks.AIR.getDefaultState(), 2);
        c.ok(BlockShape.edges(c.world, f, Blocks.OAK_FENCE.getActualState(Blocks.OAK_FENCE.getDefaultState(), c.world, f), null) == null,
                "a lone fence post keeps one box");

        IBlockState hopper = Blocks.HOPPER.getDefaultState().withProperty(BlockHopper.FACING, EnumFacing.DOWN);
        edges = BlockShape.edges(c.world, p, hopper, null);
        c.ok(edges != null && edges.size() > 24, "a hopper is outlined by its 1.13 shape: " + (edges == null ? null : edges.size()));
        edges = BlockShape.edges(c.world, p, Blocks.CAULDRON.getDefaultState(), null);
        c.ok(edges != null && edges.size() > 24, "a cauldron is outlined by its 1.13 shape");
        edges = BlockShape.edges(c.world, p, Blocks.ANVIL.getDefaultState(), null);
        c.ok(edges != null && edges.size() >= 48, "an anvil is outlined by its 1.13 shape");
        c.ok(BlockShape.edges(c.world, p, Blocks.OAK_FENCE_GATE.getDefaultState().withProperty(BlockFenceGate.OPEN, false), null) == null,
                "a fence gate keeps one box");
    }
}
