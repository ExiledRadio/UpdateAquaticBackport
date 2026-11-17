package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.mechanics.ChestPairing;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityHopper;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ILockableContainer;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

/** 1.13's chest pairing (patch P11, docs/streams/mechanics.md). */
public final class ChestSelfTest implements SelfTest {

    private Check c;
    private EntityPlayerMP player;

    @Override
    public String name() {
        return "chests";
    }

    /** Places a chest by a real player's item use, clicking {@code face} of {@code clicked}; the player faces south. */
    private BlockPos place(Block block, BlockPos clicked, EnumFacing face, boolean sneaking) {
        return place(block, clicked, face, sneaking, 0.0F);
    }

    private BlockPos place(Block block, BlockPos clicked, EnumFacing face, boolean sneaking, float yaw) {
        BlockPos at = clicked.offset(face);
        this.player.setPositionAndRotation(at.getX() + 0.5, at.getY(), at.getZ() - 2.5, yaw, 30.0F);
        this.player.setSneaking(sneaking);
        ItemStack stack = new ItemStack(block);
        this.player.setHeldItem(EnumHand.MAIN_HAND, stack);
        EnumActionResult r = ForgeHooks.onPlaceItemIntoWorld(stack, this.player, this.c.world, clicked, face, 0.5F, 0.5F, 0.5F, EnumHand.MAIN_HAND);
        this.player.setSneaking(false);
        this.c.eq(EnumActionResult.SUCCESS, r, "placing a chest at " + at);
        this.c.eq(block, this.c.world.getBlockState(at).getBlock(), "the chest is there");
        return at;
    }

    private int size(BlockPos pos) {
        Block block = this.c.world.getBlockState(pos).getBlock();
        ILockableContainer container = ((BlockChest) block).getContainer(this.c.world, pos, true);
        return container == null ? -1 : container.getSizeInventory();
    }

    private TileEntityChest tile(BlockPos pos) {
        return (TileEntityChest) this.c.world.getTileEntity(pos);
    }

    private boolean single(BlockPos pos) {
        TileEntityChest t = tile(pos);
        t.updateContainingBlockInfo();
        t.checkForAdjacentChests();
        return ChestPairing.partner(this.c.world, pos) == null && size(pos) == 27 && t.adjacentChestXNeg == null
                && t.adjacentChestXPos == null && t.adjacentChestZNeg == null && t.adjacentChestZPos == null;
    }

    private boolean paired(BlockPos a, BlockPos b) {
        EnumFacing dir = ChestPairing.partner(this.c.world, a);
        TileEntityChest t = tile(a);
        t.updateContainingBlockInfo();
        t.checkForAdjacentChests();
        TileEntityChest other = tile(b);
        boolean fields = t.adjacentChestXNeg == other || t.adjacentChestXPos == other || t.adjacentChestZNeg == other || t.adjacentChestZPos == other;
        return dir != null && a.offset(dir).equals(b) && size(a) == 54 && size(b) == 54 && fields;
    }

    @Override
    public void run(Check c) throws Exception {
        this.c = c;
        this.player = Players.real(c.world, "chests");
        BlockPos base = c.area();
        BlockPos floor = base.down();

        // Two chests placed side by side while sneaking stay single.
        BlockPos a = place(Blocks.CHEST, floor.add(4, 0, 4), EnumFacing.UP, true);
        BlockPos b = place(Blocks.CHEST, floor.add(5, 0, 4), EnumFacing.UP, true);
        c.eq(EnumFacing.NORTH, c.world.getBlockState(a).getValue(BlockChest.FACING), "a chest faces the player");
        c.ok(single(a) && single(b), "two chests placed side by side while sneaking stay single");
        // A block update, and the tile entities saved and read back (as a reload does), keep them single.
        c.world.neighborChanged(a, Blocks.STONE, a.up());
        NBTTagCompound saved = tile(a).writeToNBT(new NBTTagCompound());
        TileEntityChest reread = new TileEntityChest();
        reread.readFromNBT(saved);
        c.eq((byte) -1, ChestPairing.stored(reread), "a single chest's pairing is saved");
        c.ok(single(a) && single(b), "still single after a block update");

        // A normal placement joins a single chest beside it that faces the same way.
        BlockPos d1 = place(Blocks.CHEST, floor.add(4, 0, 8), EnumFacing.UP, false);
        BlockPos d2 = place(Blocks.CHEST, floor.add(5, 0, 8), EnumFacing.UP, false);
        c.ok(paired(d1, d2) && paired(d2, d1), "a chest placed beside a single chest facing the same way joins it");
        NBTTagCompound pairTag = tile(d2).writeToNBT(new NBTTagCompound());
        TileEntityChest rereadPair = new TileEntityChest();
        rereadPair.readFromNBT(pairTag);
        c.eq((byte) EnumFacing.WEST.getIndex(), rereadPair.getTileData().getByte(ChestPairing.KEY), "a double chest's pairing is saved (the other half is west)");

        // A third chest beside the double: allowed, and single.
        c.ok(Blocks.CHEST.canPlaceBlockAt(c.world, d2.east()), "a chest can be placed beside a double chest");
        BlockPos third = place(Blocks.CHEST, floor.add(6, 0, 8), EnumFacing.UP, false);
        c.ok(single(third) && paired(d1, d2), "the third chest stays single and the double stays double");

        // Between two single chests: allowed; it joins one of them (1.13: the one on its left), the other stays single.
        BlockPos l = place(Blocks.CHEST, floor.add(4, 0, 12), EnumFacing.UP, true);
        BlockPos r = place(Blocks.CHEST, floor.add(6, 0, 12), EnumFacing.UP, true);
        c.ok(Blocks.CHEST.canPlaceBlockAt(c.world, floor.add(5, 1, 12)), "a chest can be placed between two chests");
        BlockPos mid = place(Blocks.CHEST, floor.add(5, 0, 12), EnumFacing.UP, false);
        boolean left = ChestPairing.partner(c.world, mid) == EnumFacing.EAST;
        c.ok(left ? paired(mid, r) && single(l) : paired(mid, l) && single(r), "the middle chest joins one neighbor only");
        c.eq(EnumFacing.EAST, ChestPairing.partner(c.world, mid), "1.13 joins the chest on the left (east, for a chest facing north)");

        // A chest facing another way does not join.
        BlockPos turned = place(Blocks.CHEST, floor.add(4, 0, 16), EnumFacing.UP, false, 90.0F);
        BlockPos plain = place(Blocks.CHEST, floor.add(5, 0, 16), EnumFacing.UP, false);
        c.ok(c.world.getBlockState(turned).getValue(BlockChest.FACING) != EnumFacing.NORTH && single(turned) && single(plain),
                "chests facing different ways do not join");

        // Sneaking against the side of a single chest joins it, with its facing (1.13).
        BlockPos s1 = place(Blocks.CHEST, floor.add(4, 0, 20), EnumFacing.UP, false);
        BlockPos s2 = place(Blocks.CHEST, s1, EnumFacing.EAST, true, 270.0F);
        c.ok(paired(s1, s2) && c.world.getBlockState(s2).getValue(BlockChest.FACING) == EnumFacing.NORTH,
                "a chest placed while sneaking against a single chest's side joins it");

        // A trapped chest and a chest never join, either way round.
        BlockPos t1 = place(Blocks.TRAPPED_CHEST, floor.add(8, 0, 4), EnumFacing.UP, false);
        BlockPos t2 = place(Blocks.CHEST, floor.add(9, 0, 4), EnumFacing.UP, false);
        BlockPos t3 = place(Blocks.TRAPPED_CHEST, floor.add(10, 0, 4), EnumFacing.UP, false);
        c.ok(single(t1) && single(t2) && single(t3), "a trapped chest and a chest never join");
        BlockPos t4 = place(Blocks.TRAPPED_CHEST, floor.add(8, 0, 6), EnumFacing.UP, false);
        BlockPos t5 = place(Blocks.TRAPPED_CHEST, floor.add(9, 0, 6), EnumFacing.UP, false);
        c.ok(paired(t4, t5), "two trapped chests join");

        // Hoppers, comparators and the item handler see the stored pairing.
        tile(d1).setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 64));
        tile(a).setInventorySlotContents(0, new ItemStack(Items.DIAMOND, 64));
        IInventory below = TileEntityHopper.getInventoryAtPosition(c.world, d2.getX() + 0.5, d2.getY() + 0.5, d2.getZ() + 0.5);
        c.ok(below != null && below.getSizeInventory() == 54, "a hopper sees the double chest whole: " + (below == null ? null : below.getSizeInventory()));
        IInventory beside = TileEntityHopper.getInventoryAtPosition(c.world, b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5);
        c.ok(beside != null && beside.getSizeInventory() == 27, "a hopper sees a single chest beside another as single");
        c.ok(Blocks.CHEST.getComparatorInputOverride(c.world.getBlockState(d2), c.world, d2) > 0,
                "a comparator on the empty half of a double chest reads the other half's items");
        c.eq(0, Blocks.CHEST.getComparatorInputOverride(c.world.getBlockState(b), c.world, b),
                "a comparator on an empty single chest beside a full one reads 0");
        IItemHandler handler = tile(d2).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        c.ok(handler != null && handler.getSlots() == 54, "the double chest's item handler has 54 slots");
        IItemHandler lone = tile(b).getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
        c.ok(lone != null && lone.getSlots() == 27, "a single chest's item handler has 27 slots");

        // The pairing goes to clients with the chunk and with an update packet.
        SPacketUpdateTileEntity packet = tile(d2).getUpdatePacket();
        c.ok(packet != null, "a paired chest sends an update packet (its update tag, below)");
        c.ok(tile(d2).getUpdateTag().getCompoundTag("ForgeData").hasKey(ChestPairing.KEY), "the chunk's update tag carries the pairing");

        // Breaking one half leaves the other single.
        tile(d1).clear();
        tile(a).clear();
        c.world.setBlockToAir(d1);
        c.ok(single(d2), "breaking one half leaves the other single");

        // Chests from before: no stored pairing pairs as 1.12.2 would, and is then stored.
        BlockPos o1 = base.add(14, 0, 4);
        BlockPos o2 = base.add(15, 0, 4);
        c.world.setBlockState(o1, Blocks.CHEST.getDefaultState(), 2);
        c.world.setBlockState(o2, Blocks.CHEST.getDefaultState(), 2);
        tile(o1).getTileData().removeTag(ChestPairing.KEY);
        tile(o2).getTileData().removeTag(ChestPairing.KEY);
        c.ok(paired(o1, o2), "two old chests side by side pair as in 1.12.2");
        c.ok(ChestPairing.stored(tile(o1)) != null && ChestPairing.stored(tile(o2)) != null, "and their pairing is stored");

        // Setting off: 1.12.2's pairing by neighbors (the two sneaking-placed singles merge).
        ModConfig.mechanics.chestPairing = false;
        try {
            tile(a).updateContainingBlockInfo();
            c.eq(54, size(a), "setting off: two chests side by side are one double chest again");
            c.ok(!Blocks.CHEST.canPlaceBlockAt(c.world, b.east()), "setting off: no chest beside what 1.12.2 sees as a double");
        } finally {
            ModConfig.mechanics.chestPairing = true;
        }
        tile(a).updateContainingBlockInfo();
        c.ok(single(a) && single(b), "setting on again: the two stay single");
    }
}
