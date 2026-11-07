package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.common.BlockPseudoWater;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.conduit.BlockConduit;
import com.exiledradio.updateaquaticbackport.conduit.ConduitContent;
import com.exiledradio.updateaquaticbackport.conduit.TileEntityConduit;

import net.minecraft.block.BlockPrismarine;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootTable;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** The conduit, its frame, range, Conduit Power and hunting; the nautilus shell and heart of the sea. */
public final class ConduitSelfTest implements SelfTest {

    @Override
    public String name() {
        return "conduit";
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;
        c.ok(ConduitContent.enabled(), "conduit stream on");
        BlockConduit conduit = ConduitContent.conduit();
        Item conduitItem = Item.getItemFromBlock(conduit);
        Item shell = ConduitContent.nautilusShell();
        Item heart = ConduitContent.heartOfTheSea();
        Potion power = ConduitContent.CONDUIT_POWER;

        // 1. registration and properties -----------------------------------------------------------
        c.eq(conduit, ForgeRegistries.BLOCKS.getValue(Content.id("conduit")), "conduit block registered");
        c.ok(conduitItem != Items.AIR, "conduit item");
        c.eq(shell, ForgeRegistries.ITEMS.getValue(Content.id("nautilus_shell")), "nautilus shell registered");
        c.eq(heart, ForgeRegistries.ITEMS.getValue(Content.id("heart_of_the_sea")), "heart of the sea registered");
        c.eq(power, ForgeRegistries.POTIONS.getValue(Content.id("conduit_power")), "Conduit Power registered");
        c.ok(!power.isBadEffect(), "Conduit Power is not harmful");
        c.eq(0x1DC2D1, power.getLiquidColor(), "Conduit Power color");
        c.eq("effect.updateaquaticbackport.conduit_power", power.getName(), "Conduit Power name key");
        c.eq(Content.id("conduit"), TileEntity.getKey(TileEntityConduit.class), "tile entity id");
        for (String s : new String[] {"activate", "ambient", "ambient.short", "attack.target", "deactivate"}) {
            SoundEvent e = ForgeRegistries.SOUND_EVENTS.getValue(Content.id("block.conduit." + s));
            c.ok(e != null, "sound block.conduit." + s);
        }
        for (Item item : new Item[] {conduitItem, shell, heart}) {
            c.eq(EnumRarity.UNCOMMON, item.getForgeRarity(new ItemStack(item)), item.getRegistryName() + " is Uncommon");
            c.eq(64, item.getItemStackLimit(new ItemStack(item)), item.getRegistryName() + " stacks to 64");
        }
        c.ok(Content.multipartBlocks().contains(conduit), "conduit blockstate is multipart");
        for (IBlockState s : conduit.getBlockState().getValidStates()) {
            if (s.getValue(BlockPseudoWater.LEVEL) == 0 && !s.getValue(BlockPseudoWater.SURFACE)) {
                c.ok(conduit.getStateFromMeta(conduit.getMetaFromState(s)) == s, "meta round trip " + s);
            }
        }
        IBlockState dryState = conduit.getDefaultState();
        IBlockState wetState = dryState.withProperty(BlockPseudoWater.WATERLOGGED, true);
        c.ok(!dryState.getValue(BlockPseudoWater.WATERLOGGED), "default state is dry, as today's");
        c.eq(Material.WATER, wetState.getMaterial(), "waterlogged conduit is water");
        c.eq(Material.GLASS, dryState.getMaterial(), "dry conduit is glass");
        c.eq(3.0F, dryState.getBlockHardness(world, BlockPos.ORIGIN), "hardness 3");
        c.eq(3.0F, conduit.getExplosionResistance(null), "blast resistance 3");
        c.eq(15, dryState.getLightValue(), "light 15");
        c.eq("pickaxe", conduit.getHarvestTool(dryState), "pickaxe");
        c.eq(EnumPushReaction.BLOCK, dryState.getPushReaction(), "pistons cannot move it");
        c.ok(!conduit.isPassable(world, BlockPos.ORIGIN), "mobs do not path through");
        c.eq(new AxisAlignedBB(5 / 16.0, 5 / 16.0, 5 / 16.0, 11 / 16.0, 11 / 16.0, 11 / 16.0),
                dryState.getCollisionBoundingBox(world, BlockPos.ORIGIN), "6x6x6 pixel collision box");
        c.ok(conduit.hasTileEntity(dryState), "has a tile entity");

        // 2. recipe and loot --------------------------------------------------------------------------
        ItemStack n = new ItemStack(shell);
        ItemStack h = new ItemStack(heart);
        Crafting.expect(c, Crafting.grid(3, 3, n, n, n, n, h, n, n, n, n), new ItemStack(conduitItem), "8 shells round a heart");
        c.ok(net.minecraft.item.crafting.CraftingManager.findMatchingResult(Crafting.grid(3, 3, n, n, n, n, n, n, n, n, n), world).isEmpty(),
                "9 shells make nothing");
        LootTable treasure = world.getLootTableManager().getLootTableFromLocation(LootTableList.GAMEPLAY_FISHING_TREASURE);
        Random rand = new Random(7);
        int shells = 0;
        int rolls = 7000;
        for (int i = 0; i < rolls; i++) {
            for (ItemStack s : treasure.generateLootForPools(rand, new LootContext.Builder(world).build())) {
                if (s.getItem() == shell) {
                    shells += s.getCount();
                }
            }
        }
        // Weight 1 of 7: about 1000.
        c.ok(shells > 850 && shells < 1150, "nautilus shell is 1 in 7 fishing treasures: " + shells + " of " + rolls);

        // 3. placing ----------------------------------------------------------------------------------
        BlockPos floor = Pools.pool(c, 7);
        EntityPlayerMP player = Players.real(world, "conduit");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        ItemStack held = new ItemStack(conduitItem);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, floor.add(1, 0, 1), EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        IBlockState placed = world.getBlockState(floor.add(1, 1, 1));
        c.eq(conduit, placed.getBlock(), "placed under water");
        c.ok(placed.getBlock() == conduit && placed.getValue(BlockPseudoWater.WATERLOGGED), "placed into water: waterlogged");
        c.ok(world.getTileEntity(floor.add(1, 1, 1)) instanceof TileEntityConduit, "placing makes its tile entity");
        BlockPos dryFloor = c.area().down();
        held = new ItemStack(conduitItem);
        player.setHeldItem(EnumHand.MAIN_HAND, held);
        held.getItem().onItemUse(player, world, dryFloor, EnumHand.MAIN_HAND, EnumFacing.UP, 0.5F, 1.0F, 0.5F);
        IBlockState dryPlaced = world.getBlockState(dryFloor.up());
        c.ok(dryPlaced.getBlock() == conduit && !dryPlaced.getValue(BlockPseudoWater.WATERLOGGED), "placed on land: dry");

        // 4. breaking: leaves its water, drops itself; explosions never lose it ----------------------------
        BlockPos wetAt = floor.add(1, 1, 1);
        player.interactionManager.tryHarvestBlock(wetAt);
        c.eq(Blocks.WATER, world.getBlockState(wetAt).getBlock(), "broken under water: water left");
        c.eq(1, Drops.count(world, wetAt, conduitItem), "broken: drops itself");
        player.interactionManager.tryHarvestBlock(dryFloor.up());
        c.ok(world.isAirBlock(dryFloor.up()), "broken on land: air left");
        c.eq(1, Drops.count(world, dryFloor.up(), conduitItem), "broken on land: drops itself");
        int blown = 0;
        for (int i = 0; i < 4; i++) {
            BlockPos at = dryFloor.add(4 + 6 * i, 1, 8);
            world.setBlockState(at, dryState, 3);
            world.newExplosion(null, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 4.0F, false, true);
            if (world.getBlockState(at).getBlock() != conduit) {
                blown++;
                c.eq(1, Drops.count(world, at, conduitItem), "blown up: always drops (" + i + ")");
            }
        }
        c.eq(4, blown, "explosions break it");

        // 5. the frame -------------------------------------------------------------------------------
        int positions = 0;
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (TileEntityConduit.isFramePosition(x, y, z)) {
                        positions++;
                        c.ok(Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z))) == 2, "frame positions on the 5x5 rings");
                    }
                }
            }
        }
        c.eq(42, positions, "42 frame positions");
        c.ok(TileEntityConduit.isFrameBlock(Blocks.PRISMARINE.getDefaultState()), "prismarine counts");
        c.ok(TileEntityConduit.isFrameBlock(Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.BRICKS)),
                "prismarine bricks count");
        c.ok(TileEntityConduit.isFrameBlock(Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.DARK)),
                "dark prismarine counts");
        c.ok(TileEntityConduit.isFrameBlock(Blocks.SEA_LANTERN.getDefaultState()), "sea lanterns count");
        c.ok(!TileEntityConduit.isFrameBlock(Blocks.GLASS.getDefaultState()), "glass does not");

        // A pool 12x12 and 11 deep; the conduit in the middle with water all round.
        BlockPos pool = Pools.pool(c, 11);
        BlockPos at = pool.add(6, 6, 6);
        world.setBlockState(at, wetState, 3);
        TileEntityConduit te = (TileEntityConduit) world.getTileEntity(at);
        List<BlockPos> frame = framePositions(at);
        int[][] steps = {{15, 0, 0}, {16, 1, 32}, {20, 1, 32}, {21, 1, 48}, {28, 1, 64}, {35, 1, 80}, {41, 1, 80}, {42, 1, 96}};
        for (int[] step : steps) {
            fillFrame(world, frame, step[0]);
            te.checkNow();
            c.eq(step[1] == 1, te.isActive(), "frame of " + step[0] + " active");
            c.eq(step[0], te.frame().size(), "frame of " + step[0] + " counted");
            if (step[1] == 1) {
                c.eq(step[2], te.range(), "frame of " + step[0] + " range");
            }
            c.eq(step[0] >= 42, te.isEyeOpen(), "frame of " + step[0] + " eye open");
        }
        // Mixed frame blocks: every fourth a sea lantern, every fifth dark prismarine.
        for (int i = 0; i < frame.size(); i++) {
            IBlockState s = i % 4 == 0 ? Blocks.SEA_LANTERN.getDefaultState()
                    : i % 5 == 0 ? Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.DARK)
                    : Blocks.PRISMARINE.getDefaultState().withProperty(BlockPrismarine.VARIANT, BlockPrismarine.EnumType.BRICKS);
            world.setBlockState(frame.get(i), s, 2 | 16);
        }
        te.checkNow();
        c.ok(te.isActive() && te.isEyeOpen(), "a mixed full frame is full");
        world.setBlockState(at, dryState, 3);
        c.ok(world.getTileEntity(at) == te, "emptying it keeps the tile entity");
        te.checkNow();
        c.ok(!te.isActive(), "a dry conduit is not active");
        world.setBlockState(at, wetState, 3);
        world.setBlockState(at.add(1, 1, 1), Blocks.STONE.getDefaultState(), 2 | 16);
        te.checkNow();
        c.ok(!te.isActive(), "a solid block in the 3x3x3 stops it");
        world.setBlockState(at.add(1, 1, 1), Blocks.FLOWING_WATER.getDefaultState().withProperty(net.minecraft.block.BlockLiquid.LEVEL, 3), 2 | 16);
        te.checkNow();
        c.ok(te.isActive(), "flowing water counts as water");
        world.setBlockState(at.add(1, 1, 1), Blocks.WATER.getDefaultState(), 2 | 16);

        // Its own tick: one check per 40 ticks of world time.
        world.setBlockState(at, dryState, 3);
        te.checkNow();
        world.setBlockState(at, wetState, 3);
        long time = world.getTotalWorldTime();
        long start = (time / 40L + 1L) * 40L + 1L;
        int inactive = 0;
        for (long t = start; t < start + 40L; t++) {
            world.getWorldInfo().setWorldTotalTime(t);
            te.update();
            if (!te.isActive()) {
                inactive++;
            }
        }
        c.eq(39, inactive, "its own tick checks once in 40 ticks of world time");
        c.ok(te.isActive(), "and then it is active");
        world.getWorldInfo().setWorldTotalTime(time);

        // 6. Conduit Power ----------------------------------------------------------------------------
        fillFrame(world, frame, 16); // range 32
        EntityPlayerMP near = Players.real(world, "conduitnear");
        near.setPosition(at.getX() + 0.5, at.getY() + 3, at.getZ() + 0.5);
        world.spawnEntity(near);
        near.handleWaterMovement();
        c.ok(near.isWet(), "a player in the pool is wet");
        EntityPlayerMP far = Players.real(world, "conduitfar");
        BlockPos farAt = at.up(33);
        world.setBlockState(farAt, Blocks.WATER.getDefaultState(), 2 | 16);
        world.setBlockState(farAt.up(), Blocks.WATER.getDefaultState(), 2 | 16);
        far.setPosition(farAt.getX() + 0.5, farAt.getY(), farAt.getZ() + 0.5);
        world.spawnEntity(far);
        far.handleWaterMovement();
        EntityPlayerMP edge = Players.real(world, "conduitedge");
        BlockPos edgeAt = at.up(31);
        world.setBlockState(edgeAt, Blocks.WATER.getDefaultState(), 2 | 16);
        world.setBlockState(edgeAt.up(), Blocks.WATER.getDefaultState(), 2 | 16);
        edge.setPosition(edgeAt.getX() + 0.5, edgeAt.getY(), edgeAt.getZ() + 0.5);
        world.spawnEntity(edge);
        edge.handleWaterMovement();
        EntityPlayerMP dry = Players.real(world, "conduitdry");
        BlockPos dryAt = at.up(20).east(3);
        dry.setPosition(dryAt.getX() + 0.5, dryAt.getY(), dryAt.getZ() + 0.5);
        world.spawnEntity(dry);
        dry.handleWaterMovement();
        te.checkNow();
        PotionEffect got = near.getActivePotionEffect(power);
        c.ok(got != null, "a player in water within range gets Conduit Power");
        if (got != null) {
            c.eq(260, got.getDuration(), "Conduit Power lasts 13 s");
            c.eq(0, got.getAmplifier(), "Conduit Power I");
            c.ok(got.getIsAmbient() && got.doesShowParticles(), "ambient, with particles");
        }
        c.ok(edge.getActivePotionEffect(power) != null, "31 blocks away, in range 32: gets it");
        c.ok(far.getActivePotionEffect(power) == null, "33 blocks away, in range 32: does not");
        c.ok(!world.isRaining() && dry.getActivePotionEffect(power) == null, "a dry player in range does not");
        for (EntityPlayerMP p : new EntityPlayerMP[] {near, far, edge, dry}) {
            world.removeEntity(p);
        }

        // Breath: a pig under water with Conduit Power gains 4 air a tick and never drowns.
        BlockPos deep = pool.add(2, 2, 2);
        EntityPig pig = new EntityPig(world);
        pig.setPosition(deep.getX() + 0.5, deep.getY(), deep.getZ() + 0.5);
        world.spawnEntity(pig);
        pig.setNoAI(true);
        pig.setAir(100);
        for (int i = 0; i < 10; i++) {
            pig.onUpdate();
        }
        c.ok(pig.isInsideOfMaterial(Material.WATER), "the pig stays under water");
        int without = pig.getAir();
        c.ok(without < 100, "a pig under water loses air: " + without);
        pig.addPotionEffect(new PotionEffect(power, 2000, 0, true, true));
        pig.setAir(100);
        for (int i = 0; i < 10; i++) {
            pig.onUpdate();
        }
        c.eq(140, pig.getAir(), "with Conduit Power: 4 air back a tick");
        for (int i = 0; i < 100; i++) {
            pig.onUpdate();
        }
        c.ok(pig.getAir() >= 299 && pig.getAir() <= 300, "refills to full: " + pig.getAir());
        pig.setAir(-15);
        float health = pig.getHealth();
        for (int i = 0; i < 10; i++) {
            pig.onUpdate();
        }
        c.ok(pig.getHealth() == health && pig.getAir() > -15, "out of air with Conduit Power: no drowning");
        pig.setDead();

        // Mining speed: as Haste, the stronger of the two.
        EntityPlayerMP miner = Players.real(world, "conduitminer");
        miner.setPosition(dryFloor.getX() + 0.5, dryFloor.getY() + 1, dryFloor.getZ() + 0.5);
        IBlockState stone = Blocks.STONE.getDefaultState();
        float base = miner.getDigSpeed(stone, dryFloor);
        miner.addPotionEffect(new PotionEffect(power, 200, 0));
        c.ok(near(base * 1.2F, miner.getDigSpeed(stone, dryFloor)), "Conduit Power I digs 20% faster");
        miner.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 1));
        float hasteTwo = miner.getDigSpeed(stone, dryFloor);
        c.ok(near(base * 1.4F, hasteTwo), "Haste II beside Conduit Power I: Haste II only, not both (" + hasteTwo / base + ")");
        miner.removePotionEffect(MobEffects.HASTE);
        miner.removePotionEffect(power);
        miner.addPotionEffect(new PotionEffect(power, 200, 1));
        miner.addPotionEffect(new PotionEffect(MobEffects.HASTE, 200, 0));
        c.ok(near(base * 1.4F, miner.getDigSpeed(stone, dryFloor)), "Conduit Power II beside Haste I: as Haste II");

        // 7. hunting ----------------------------------------------------------------------------------
        fillFrame(world, frame, 42);
        EntityZombie zombie = new EntityZombie(world);
        zombie.setPosition(at.getX() + 3.5, at.getY() - 2, at.getZ() + 0.5);
        world.spawnEntity(zombie);
        zombie.handleWaterMovement();
        EntityPig bystander = new EntityPig(world);
        bystander.setPosition(at.getX() - 2.5, at.getY() - 2, at.getZ() + 0.5);
        world.spawnEntity(bystander);
        bystander.handleWaterMovement();
        float zombieHealth = zombie.getHealth();
        te.checkNow();
        c.eq(zombie, te.target(), "a full frame targets a hostile mob in water");
        c.eq(zombieHealth - 4.0F, zombie.getHealth(), "4 damage");
        c.eq(bystander.getMaxHealth(), bystander.getHealth(), "a pig is left alone");
        zombie.hurtResistantTime = 0;
        te.checkNow();
        c.eq(zombieHealth - 8.0F, zombie.getHealth(), "the same target, 4 more");
        NBTTagCompound tag = te.getUpdateTag();
        c.ok(tag.hasKey("target_uuid"), "the target goes to the client");

        // Saving: the target comes back by its id.
        NBTTagCompound saved = te.writeToNBT(new NBTTagCompound());
        world.setBlockState(at, Blocks.WATER.getDefaultState(), 3);
        world.setBlockState(at, wetState, 3);
        TileEntityConduit reloaded = (TileEntityConduit) world.getTileEntity(at);
        c.ok(reloaded != te && reloaded.target() == null, "a fresh tile entity has no target");
        reloaded.readFromNBT(saved);
        zombie.hurtResistantTime = 0;
        reloaded.checkNow();
        c.eq(zombie, reloaded.target(), "after loading, the saved target is found again");
        c.eq(zombieHealth - 12.0F, zombie.getHealth(), "and attacked");
        te = reloaded;

        // Out of range or out of water: no target.
        zombie.setPosition(at.getX() + 8.5, at.getY() - 2, at.getZ() + 0.5);
        te.checkNow();
        c.ok(te.target() == null, "a target 8 blocks away is dropped");
        world.removeEntityDangerously(zombie);
        EntityZombie dryZombie = new EntityZombie(world);
        BlockPos above = at.up(6);
        dryZombie.setPosition(above.getX() + 0.5, above.getY(), above.getZ() + 0.5);
        world.spawnEntity(dryZombie);
        dryZombie.handleWaterMovement();
        c.ok(!dryZombie.isWet(), "a zombie above the water is dry");
        te.checkNow();
        c.ok(te.target() == null, "a dry hostile mob in range is not targeted");
        world.removeEntityDangerously(dryZombie);
        EntityZombie wet = new EntityZombie(world);
        wet.setPosition(at.getX() - 3.5, at.getY() + 1, at.getZ() + 0.5);
        world.spawnEntity(wet);
        wet.handleWaterMovement();
        fillFrame(world, frame, 41);
        te.checkNow();
        c.ok(te.isActive() && te.target() == null, "41 frame blocks: active but not hunting");
        fillFrame(world, frame, 42);
        te.checkNow();
        c.eq(wet, te.target(), "42 again: hunting");
        wet.setDead();
        te.checkNow();
        c.ok(te.target() == null, "a dead target is dropped");
        world.removeEntityDangerously(wet);
        world.removeEntityDangerously(bystander);
    }

    private static boolean near(float want, float got) {
        return Math.abs(want - got) < 0.0005F * Math.max(1.0F, Math.abs(want));
    }

    private static List<BlockPos> framePositions(BlockPos center) {
        List<BlockPos> out = new ArrayList<BlockPos>();
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (TileEntityConduit.isFramePosition(x, y, z)) {
                        out.add(center.add(x, y, z));
                    }
                }
            }
        }
        return out;
    }

    /** Puts prismarine in the first {@code count} frame positions and water in the rest. */
    private static void fillFrame(WorldServer world, List<BlockPos> frame, int count) {
        for (int i = 0; i < frame.size(); i++) {
            world.setBlockState(frame.get(i), i < count ? Blocks.PRISMARINE.getDefaultState() : Blocks.WATER.getDefaultState(), 2 | 16);
        }
    }
}
