package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.blocks.BlocksContent;
import com.exiledradio.updateaquaticbackport.blocks.EndermanStare;
import com.exiledradio.updateaquaticbackport.blocks.ItemWood;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockNewLog;
import net.minecraft.block.BlockOldLog;
import net.minecraft.block.BlockPlanks;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;

import java.util.ArrayList;
import java.util.List;

/**
 * The pumpkin follow-ups to stream X: endermen calmed only by a carved pumpkin (their target task
 * replaced), and the three patches - the plain pumpkin is not worn, builds no golem however it is placed,
 * and middle-clicking wood picks the wood item. Run by {@link BlocksSelfTest}.
 */
final class PumpkinFollowupsTest {

    private PumpkinFollowupsTest() {
    }

    static void run(Check c) {
        wearing(c);
        golems(c);
        pickBlock(c);
        endermen(c);
    }

    // ------------------------------------------------------------------------------------------------

    private static void wearing(Check c) {
        WorldServer world = c.world;
        Block carved = BlocksContent.carvedPumpkin();
        c.eq(EntityEquipmentSlot.MAINHAND, EntityLiving.getSlotForItemStack(new ItemStack(Blocks.PUMPKIN)), "a plain pumpkin is not worn (main hand)");
        c.eq(EntityEquipmentSlot.HEAD, EntityLiving.getSlotForItemStack(new ItemStack(carved)), "a carved pumpkin is");
        c.eq(EntityEquipmentSlot.MAINHAND, EntityLiving.getSlotForItemStack(new ItemStack(Blocks.LIT_PUMPKIN)), "a jack o'lantern is not (as vanilla)");
        c.eq(EntityEquipmentSlot.HEAD, EntityLiving.getSlotForItemStack(new ItemStack(Items.SKULL, 1, 2)), "a skull still is");
        c.eq(EntityEquipmentSlot.HEAD, EntityLiving.getSlotForItemStack(new ItemStack(Items.IRON_HELMET)), "a helmet still is");

        EntityPlayerMP player = Players.real(world, "wearer");
        player.interactionManager.setGameType(GameType.SURVIVAL);
        // ContainerPlayer: 5 is the helmet slot, 9-35 the main inventory, 36-44 the hotbar.
        c.ok(!player.inventoryContainer.getSlot(5).isItemValid(new ItemStack(Blocks.PUMPKIN)), "the helmet slot refuses a plain pumpkin");
        c.ok(player.inventoryContainer.getSlot(5).isItemValid(new ItemStack(carved)), "and takes a carved one");
        player.inventory.setInventorySlotContents(9, new ItemStack(Blocks.PUMPKIN));
        player.inventoryContainer.transferStackInSlot(player, 9);
        c.ok(player.inventory.armorInventory.get(3).isEmpty(), "shift-clicking a plain pumpkin does not put it on the head");
        c.eq(Item.getItemFromBlock(Blocks.PUMPKIN), player.inventory.getStackInSlot(0).getItem(), "it goes to the hotbar");
        player.inventory.clear();
        player.inventory.setInventorySlotContents(9, new ItemStack(carved));
        player.inventoryContainer.transferStackInSlot(player, 9);
        c.eq(Item.getItemFromBlock(carved), player.inventory.armorInventory.get(3).getItem(), "shift-clicking a carved pumpkin puts it on the head");

        // An armor stand (no arms) given a plain pumpkin takes nothing; given a carved one, wears it.
        BlockPos p = c.area().add(4, 0, 4);
        EntityArmorStand stand = new EntityArmorStand(world, p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
        world.spawnEntity(stand);
        player.inventory.clear();
        player.setPosition(p.getX() + 0.5, p.getY(), p.getZ() + 2.5);
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(Blocks.PUMPKIN));
        stand.applyPlayerInteraction(player, new Vec3d(0.0, 1.0, 0.0), net.minecraft.util.EnumHand.MAIN_HAND);
        c.ok(stand.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty(), "an armor stand does not wear a plain pumpkin");
        player.setHeldItem(net.minecraft.util.EnumHand.MAIN_HAND, new ItemStack(carved));
        stand.applyPlayerInteraction(player, new Vec3d(0.0, 1.8, 0.0), net.minecraft.util.EnumHand.MAIN_HAND);
        c.eq(Item.getItemFromBlock(carved), stand.getItemStackFromSlot(EntityEquipmentSlot.HEAD).getItem(), "it wears a carved one");
        stand.setDead();
    }

    private static int count(WorldServer world, Class<? extends net.minecraft.entity.Entity> type, BlockPos at) {
        int n = 0;
        for (net.minecraft.entity.Entity e : world.getEntitiesWithinAABB(type, new AxisAlignedBB(at).grow(4))) {
            n += e.isDead ? 0 : 1;
            e.setDead();
        }
        return n;
    }

    /** Placed without a player (as a dispenser-like block, another mod or a command would): setBlockState. */
    private static void golems(Check c) {
        WorldServer world = c.world;
        BlockPos base = c.area().add(4, 0, 4);
        world.setBlockState(base, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(base.up(), Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(base.up(2), Blocks.PUMPKIN.getDefaultState(), 3);
        c.eq(0, count(world, EntitySnowman.class, base), "a plain pumpkin set on snow by code builds no snow golem");
        c.ok(world.getBlockState(base).getBlock() == Blocks.SNOW && world.getBlockState(base.up(2)).getBlock() == Blocks.PUMPKIN,
                "the snow and the pumpkin stay");

        BlockPos iron = base.east(6);
        world.setBlockState(iron, Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up().east(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up().west(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(iron.up(2), Blocks.PUMPKIN.getDefaultState(), 3);
        c.eq(0, count(world, EntityIronGolem.class, iron), "nor an iron golem");
        c.eq(Blocks.IRON_BLOCK, world.getBlockState(iron.up()).getBlock(), "the iron stays");

        BlockPos lit = base.east(12);
        world.setBlockState(lit, Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(lit.up(), Blocks.SNOW.getDefaultState(), 3);
        world.setBlockState(lit.up(2), Blocks.LIT_PUMPKIN.getDefaultState(), 3);
        c.eq(1, count(world, EntitySnowman.class, lit), "a jack o'lantern set by code still builds one");

        BlockPos ironLit = base.east(18);
        world.setBlockState(ironLit, Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(ironLit.up(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(ironLit.up().east(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(ironLit.up().west(), Blocks.IRON_BLOCK.getDefaultState(), 3);
        world.setBlockState(ironLit.up(2), BlocksContent.carvedPumpkin().getDefaultState(), 3);
        c.eq(1, count(world, EntityIronGolem.class, ironLit), "and a carved pumpkin set by code an iron golem");
    }

    private static void pickBlock(Check c) {
        WorldServer world = c.world;
        try {
            BlockLog.class.getDeclaredMethod("getPickBlock", IBlockState.class, RayTraceResult.class, net.minecraft.world.World.class,
                    BlockPos.class, EntityPlayer.class);
            c.ok(true, "BlockLog has the patched getPickBlock");
        } catch (NoSuchMethodException e) {
            c.fail("BlockLog has no getPickBlock: the WoodPickBlock patch did not run");
        }
        EntityPlayerMP player = Players.real(world, "picker");
        BlockPos p = c.area().add(4, 0, 4);
        for (BlockPlanks.EnumType type : BlockPlanks.EnumType.values()) {
            ItemWood wood = null;
            for (ItemWood w : BlocksContent.woods()) {
                if (w.state().getBlock() == (type.getMetadata() < 4 ? Blocks.LOG : Blocks.LOG2)
                        && (type.getMetadata() < 4 ? w.state().getValue(BlockOldLog.VARIANT) : w.state().getValue(BlockNewLog.VARIANT)) == type) {
                    wood = w;
                }
            }
            IBlockState bark = wood.state();
            world.setBlockState(p, bark, 2);
            RayTraceResult hit = new RayTraceResult(new Vec3d(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5), EnumFacing.UP, p);
            ItemStack picked = bark.getBlock().getPickBlock(bark, hit, world, p, player);
            c.ok(picked.getItem() == wood && picked.getCount() == 1, "middle-clicking " + type.getName() + " wood picks the wood item: " + picked);

            IBlockState log = bark.withProperty(BlockLog.LOG_AXIS, BlockLog.EnumAxis.Y);
            world.setBlockState(p, log, 2);
            picked = log.getBlock().getPickBlock(log, hit, world, p, player);
            c.ok(picked.getItem() == Item.getItemFromBlock(log.getBlock()) && picked.getMetadata() == type.getMetadata() % 4,
                    "an upright " + type.getName() + " log still picks its log: " + picked);
        }
        world.setBlockState(p, Blocks.STONE.getDefaultState(), 2);
        RayTraceResult hit = new RayTraceResult(new Vec3d(p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5), EnumFacing.UP, p);
        c.eq(Item.getItemFromBlock(Blocks.STONE), Blocks.STONE.getPickBlock(Blocks.STONE.getDefaultState(), hit, world, p, player).getItem(),
                "other blocks pick as before");
        world.setBlockToAir(p);
    }

    // ------------------------------------------------------------------------------------------------

    /** A real server player at {@code eyeOf}'s distance, looking straight into the enderman's eyes. */
    private static EntityPlayerMP starer(WorldServer world, String name, EntityEnderman enderman, BlockPos at, ItemStack head) {
        EntityPlayerMP player = Players.real(world, name);
        player.interactionManager.setGameType(GameType.SURVIVAL);
        player.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        double dx = enderman.posX - player.posX;
        double dy = enderman.getEntityBoundingBox().minY + enderman.getEyeHeight() - (player.posY + player.getEyeHeight());
        double dz = enderman.posZ - player.posZ;
        float yaw = (float) (Math.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
        float pitch = (float) -(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0D / Math.PI);
        player.rotationYaw = player.rotationYawHead = player.prevRotationYaw = player.prevRotationYawHead = yaw;
        player.rotationPitch = player.prevRotationPitch = pitch;
        player.inventory.armorInventory.set(3, head);
        return player;
    }

    private static void endermen(Check c) {
        WorldServer world = c.world;
        c.ok(EntityEnderman.getCarriable(BlocksContent.carvedPumpkin()), "endermen can carry a carved pumpkin (1.13's tag)");
        c.ok(EntityEnderman.getCarriable(Blocks.PUMPKIN), "and still a pumpkin");

        BlockPos corner = c.area();
        BlockPos at = corner.add(16, 0, 8);
        EntityEnderman enderman = new EntityEnderman(world);
        enderman.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        enderman.rotationYaw = enderman.rotationYawHead = 0.0F;
        world.spawnEntity(enderman);

        // The target task list: vanilla's order and priorities, only the stare task swapped.
        List<EntityAITasks.EntityAITaskEntry> entries = new ArrayList<EntityAITasks.EntityAITaskEntry>(enderman.targetTasks.taskEntries);
        c.eq(3, entries.size(), "three target tasks");
        if (entries.size() == 3) {
            c.eq(1, entries.get(0).priority, "first at priority 1");
            c.ok(entries.get(0).action.getClass().getName().endsWith("EndermanStare$FindPlayer"), "is the carved-pumpkin stare task: "
                    + entries.get(0).action.getClass().getName());
            c.ok(entries.get(1).priority == 2 && entries.get(1).action instanceof EntityAIHurtByTarget, "then hurt-by-target at 2");
            c.ok(entries.get(2).priority == 3 && entries.get(2).action instanceof EntityAINearestAttackableTarget, "then the endermite target at 3");
        }
        for (EntityAITasks.EntityAITaskEntry e : entries) {
            c.ok(!e.action.getClass().getName().equals("net.minecraft.entity.monster.EntityEnderman$AIFindPlayer"), "vanilla's stare task is gone");
        }

        // An enderman read back from its saved data (as on chunk load) gets the same.
        NBTTagCompound tag = new NBTTagCompound();
        enderman.writeToNBTOptional(tag);
        EntityEnderman reloaded = (EntityEnderman) net.minecraft.entity.EntityList.createEntityFromNBT(tag, world);
        reloaded.setUniqueId(java.util.UUID.randomUUID());
        reloaded.setPosition(at.getX() + 0.5, at.getY(), at.getZ() + 6.5);
        world.spawnEntity(reloaded);
        boolean swapped = false;
        for (EntityAITasks.EntityAITaskEntry e : reloaded.targetTasks.taskEntries) {
            swapped |= e.action.getClass().getName().endsWith("EndermanStare$FindPlayer");
        }
        c.ok(swapped, "a reloaded enderman gets the task too");
        reloaded.setDead();

        EntityAIBase task = entries.isEmpty() ? null : entries.get(0).action;
        Object[][] heads = {
                {"bare", ItemStack.EMPTY, true},
                {"pumpkin", new ItemStack(Blocks.PUMPKIN), true},
                {"carved", new ItemStack(BlocksContent.carvedPumpkin()), false},
                {"lantern", new ItemStack(Blocks.LIT_PUMPKIN), true},
                {"skull", new ItemStack(Items.SKULL, 1, 0), true},
                {"helmet", new ItemStack(Items.IRON_HELMET), true},
        };
        BlockPos stand = at.add(0, 0, -8);
        for (Object[] h : heads) {
            String name = (String) h[0];
            boolean angers = (Boolean) h[2];
            EntityPlayerMP player = starer(world, "stare" + name, enderman, stand, (ItemStack) h[1]);
            world.playerEntities.add(player);
            try {
                enderman.setAttackTarget(null);
                c.eq(angers, EndermanStare.shouldAttackPlayer(enderman, player), name + ": stare angers it");
                if (task != null) {
                    boolean starts = task.shouldExecute();
                    c.eq(angers, starts, name + ": the task finds the staring player");
                    if (starts) {
                        task.startExecuting();
                        for (int i = 0; i < 5 && task.shouldContinueExecuting(); i++) {
                            task.updateTask();
                        }
                        c.eq(player, enderman.getAttackTarget(), name + ": after five ticks it attacks");
                        task.resetTask();
                    }
                }
            } finally {
                world.playerEntities.remove(player);
                enderman.setAttackTarget(null);
            }
        }

        // Looking away: no anger, whatever is worn.
        EntityPlayerMP away = starer(world, "away", enderman, stand, ItemStack.EMPTY);
        away.rotationYaw = away.rotationYawHead = away.rotationYaw + 180.0F;
        c.ok(!EndermanStare.shouldAttackPlayer(enderman, away), "a player looking away does not anger it");

        // Once angry, putting on a carved pumpkin does not calm it (as vanilla: the task keeps its target).
        if (task != null) {
            EntityPlayerMP player = starer(world, "late", enderman, stand, ItemStack.EMPTY);
            world.playerEntities.add(player);
            try {
                c.ok(task.shouldExecute(), "late: angers it bare");
                task.startExecuting();
                for (int i = 0; i < 5; i++) {
                    task.updateTask();
                }
                player.inventory.armorInventory.set(3, new ItemStack(BlocksContent.carvedPumpkin()));
                c.ok(task.shouldContinueExecuting(), "late: a carved pumpkin put on afterwards does not end the attack");
                c.eq(player, enderman.getAttackTarget(), "late: still its target");
                task.resetTask();
            } finally {
                world.playerEntities.remove(player);
            }
        }
        enderman.setDead();
    }
}
