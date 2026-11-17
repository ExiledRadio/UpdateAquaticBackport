package com.exiledradio.updateaquaticbackport.mechanics;

import com.exiledradio.updateaquaticbackport.ModConfig;

import net.minecraft.block.Block;
import net.minecraft.block.BlockChest;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.InventoryLargeChest;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.ILockableContainer;
import net.minecraft.world.World;
import net.minecraftforge.items.VanillaDoubleChestItemHandler;

import javax.annotation.Nullable;

/**
 * 1.13's chest pairing (patch P11, {@code asm/ChestPairingPatch}): which chest a chest is joined to is stored, as
 * 1.13's {@code type} property (single, left, right) stores it, instead of 1.12.2's "any chest of the same kind beside
 * it". So two single chests can stand side by side (place the second while sneaking), and a chest can be placed beside
 * a double chest or between two chests and stays single.
 *
 * <p>Stored in the chest tile entity's Forge data ({@code getTileData()}, saved with it and sent to clients with the
 * chunk) under {@link #KEY}: -1 single, else the {@link EnumFacing#getIndex() index} of the direction to the other
 * half. A chest without it (from before this, or placed by a structure or command) pairs as
 * 1.12.2 would, with a neighbor of the same kind that has no stored pairing either and pairs back; the server then
 * stores that. A stored pairing counts only while the other half stores the way back to it.
 *
 * <p>Placement is 1.13's {@code ChestBlock.getStateForPlacement}: the chest faces the player; placed while sneaking
 * against the side of a single chest it joins that chest (taking its facing); placed while sneaking otherwise it stays
 * single; placed normally it joins a single chest of the same kind on its left or right that faces the same way.
 * Breaking a half leaves the other single.
 *
 * <p>Only vanilla's chest and trapped chest (and their own tile entity class) are handled; another mod's chest
 * subclass keeps 1.12.2's rules. Off ({@code mechanics.chestPairing}), everything is 1.12.2's, stored pairings are
 * kept but not used.
 */
public final class ChestPairing {

    /** The stored pairing, in the tile entity's Forge data. */
    public static final String KEY = "UpdateAquaticChestPair";
    private static final byte SINGLE = -1;

    private static final AxisAlignedBB NORTH = new AxisAlignedBB(0.0625D, 0.0D, 0.0D, 0.9375D, 0.875D, 0.9375D);
    private static final AxisAlignedBB SOUTH = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 1.0D);
    private static final AxisAlignedBB WEST = new AxisAlignedBB(0.0D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);
    private static final AxisAlignedBB EAST = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 1.0D, 0.875D, 0.9375D);
    private static final AxisAlignedBB ALONE = new AxisAlignedBB(0.0625D, 0.0D, 0.0625D, 0.9375D, 0.875D, 0.9375D);

    /** 1.12.2's order of looking at neighbors for an unstored chest (its tile entity's: west, east, north, south). */
    private static final EnumFacing[] LEGACY_ORDER = {EnumFacing.WEST, EnumFacing.EAST, EnumFacing.NORTH, EnumFacing.SOUTH};

    /** The partner chosen when a chest is placed, from {@code getStateForPlacement} to {@code onBlockPlacedBy}. */
    private static final ThreadLocal<Pending> PENDING = new ThreadLocal<Pending>();

    private static final class Pending {
        final BlockPos pos;
        @Nullable
        final EnumFacing partner;

        Pending(BlockPos pos, @Nullable EnumFacing partner) {
            this.pos = pos;
            this.partner = partner;
        }
    }

    private ChestPairing() {
    }

    // ------------------------------------------------------------------------------------------ what counts

    public static boolean on() {
        return ModConfig.mechanics.chestPairing;
    }

    /** Vanilla's chest or trapped chest, with the setting on. */
    public static boolean handles(Block block) {
        return on() && (block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST);
    }

    /** A vanilla chest tile entity in a world, on a vanilla chest block, with the setting on. */
    public static boolean handles(TileEntityChest chest) {
        if (!on() || chest.getClass() != TileEntityChest.class || chest.getWorld() == null) {
            return false;
        }
        Block block = chest.getWorld().getBlockState(chest.getPos()).getBlock();
        return block == Blocks.CHEST || block == Blocks.TRAPPED_CHEST;
    }

    @Nullable
    private static TileEntityChest chestAt(IBlockAccess world, BlockPos pos, Block block) {
        if (world.getBlockState(pos).getBlock() != block) {
            return null;
        }
        TileEntity tile = world.getTileEntity(pos);
        return tile != null && tile.getClass() == TileEntityChest.class ? (TileEntityChest) tile : null;
    }

    /** @return the stored pairing: null unknown, {@link #SINGLE}, or a facing index */
    @Nullable
    public static Byte stored(TileEntityChest chest) {
        NBTTagCompound data = chest.getTileData();
        return data.hasKey(KEY, 99) ? data.getByte(KEY) : null;
    }

    private static void store(TileEntityChest chest, @Nullable EnumFacing partner) {
        chest.getTileData().setByte(KEY, partner == null ? SINGLE : (byte) partner.getIndex());
        chest.markDirty();
    }

    // ---------------------------------------------------------------------------------------- the pairing

    /**
     * @return the direction from the chest at {@code pos} to the other half of its double chest, or null when it is
     * single (or not a handled chest)
     */
    @Nullable
    public static EnumFacing partner(IBlockAccess world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        TileEntityChest chest = chestAt(world, pos, block);
        if (chest == null) {
            return null;
        }
        Byte stored = stored(chest);
        if (stored != null) {
            if (stored == SINGLE) {
                return null;
            }
            EnumFacing dir = EnumFacing.byIndex(stored);
            if (dir.getAxis().isVertical()) {
                return null;
            }
            TileEntityChest other = chestAt(world, pos.offset(dir), block);
            Byte back = other == null ? null : stored(other);
            return back != null && back == dir.getOpposite().getIndex() ? dir : null;
        }
        // Not stored: 1.12.2's pairing, with an unstored neighbor that pairs back.
        EnumFacing pick = legacyPick(world, pos, block);
        if (pick != null && legacyPick(world, pos.offset(pick), block) != pick.getOpposite()) {
            pick = null;
        }
        if (world instanceof World && !((World) world).isRemote && ((World) world).isAreaLoaded(pos, 2)) {
            store(chest, pick);
            if (pick != null) {
                TileEntityChest other = chestAt(world, pos.offset(pick), block);
                if (other != null) {
                    store(other, pick.getOpposite());
                }
            }
        }
        return pick;
    }

    @Nullable
    private static EnumFacing legacyPick(IBlockAccess world, BlockPos pos, Block block) {
        for (EnumFacing dir : LEGACY_ORDER) {
            TileEntityChest other = chestAt(world, pos.offset(dir), block);
            if (other != null && stored(other) == null) {
                return dir;
            }
        }
        return null;
    }

    /** Sets both halves' pairing (or one chest single) and tells clients, comparators and caches. */
    private static void join(World world, BlockPos pos, @Nullable EnumFacing partner) {
        Block block = world.getBlockState(pos).getBlock();
        TileEntityChest chest = chestAt(world, pos, block);
        if (chest == null) {
            return;
        }
        store(chest, partner);
        changed(world, pos, block);
        if (partner != null) {
            TileEntityChest other = chestAt(world, pos.offset(partner), block);
            if (other != null) {
                store(other, partner.getOpposite());
                changed(world, pos.offset(partner), block);
            }
        }
    }

    private static void changed(World world, BlockPos pos, Block block) {
        for (EnumFacing dir : EnumFacing.HORIZONTALS) {
            TileEntity tile = world.getTileEntity(pos.offset(dir));
            if (tile instanceof TileEntityChest) {
                tile.updateContainingBlockInfo();
            }
        }
        TileEntity tile = world.getTileEntity(pos);
        if (tile != null) {
            tile.updateContainingBlockInfo();
        }
        if (!world.isRemote) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
            world.updateComparatorOutputLevel(pos, block);
        }
    }

    // ------------------------------------------------------------------------------------ BlockChest hooks

    /** {@code getStateForPlacement}: 1.13's facing and partner (the partner kept for {@link #placed}). */
    public static IBlockState placement(BlockChest block, World world, BlockPos pos, EnumFacing clicked, EntityLivingBase placer) {
        EnumFacing facing = placer.getHorizontalFacing().getOpposite();
        EnumFacing partner = null;
        boolean sneaking = placer.isSneaking();
        if (sneaking && clicked != null && clicked.getAxis().isHorizontal()) {
            EnumFacing attach = attachable(world, pos, clicked.getOpposite(), block);
            if (attach != null && attach.getAxis() != clicked.getAxis()) {
                facing = attach;
                partner = clicked.getOpposite();
            }
        }
        if (partner == null && !sneaking) {
            if (facing == attachable(world, pos, facing.rotateY(), block)) {
                partner = facing.rotateY();
            } else if (facing == attachable(world, pos, facing.rotateYCCW(), block)) {
                partner = facing.rotateYCCW();
            }
        }
        PENDING.set(new Pending(pos.toImmutable(), partner));
        return block.getDefaultState().withProperty(BlockChest.FACING, facing);
    }

    /** 1.13's {@code getDirectionToAttach}: the facing of a single chest of this kind on that side, else null. */
    @Nullable
    private static EnumFacing attachable(World world, BlockPos pos, EnumFacing side, Block block) {
        BlockPos at = pos.offset(side);
        IBlockState state = world.getBlockState(at);
        if (state.getBlock() != block || chestAt(world, at, block) == null || partner(world, at) != null) {
            return null;
        }
        return state.getValue(BlockChest.FACING);
    }

    /** {@code onBlockPlacedBy}: stores the pairing chosen at placement, and names the chest as 1.12.2 does. */
    public static void placed(BlockChest block, World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        Pending pending = PENDING.get();
        PENDING.remove();
        // Stored single first, so a neighbor without a stored pairing does not take this new chest as its 1.12.2 pair.
        TileEntity self = world.getTileEntity(pos);
        if (self instanceof TileEntityChest && self.getClass() == TileEntityChest.class) {
            store((TileEntityChest) self, null);
        }
        EnumFacing partner;
        if (pending != null && pending.pos.equals(pos)) {
            partner = pending.partner;
        } else {
            // Placed without our placement state (another mod's code): the normal, non-sneaking rule.
            EnumFacing facing = state.getValue(BlockChest.FACING);
            partner = facing == attachable(world, pos, facing.rotateY(), block) ? facing.rotateY()
                    : facing == attachable(world, pos, facing.rotateYCCW(), block) ? facing.rotateYCCW() : null;
        }
        if (partner != null && attachable(world, pos, partner, block) == null) {
            partner = null;
        }
        join(world, pos, partner);
        if (stack.hasDisplayName()) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof TileEntityChest) {
                ((TileEntityChest) tile).setCustomName(stack.getDisplayName());
            }
        }
    }

    /** {@code canPlaceBlockAt}: anywhere the block can go (1.12.2 refused beside a double chest or two chests). */
    public static boolean canPlace(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock().isReplaceable(world, pos);
    }

    /**
     * Run first in {@code breakBlock}: the other half becomes single. The chunk already holds the new block here, so
     * the pairing is read from the tile entity, which is still there.
     */
    public static void broken(BlockChest block, World world, BlockPos pos) {
        if (!handles(block) || world.isRemote) {
            return;
        }
        TileEntity tile = world.getTileEntity(pos);
        Byte stored = tile instanceof TileEntityChest && tile.getClass() == TileEntityChest.class ? stored((TileEntityChest) tile) : null;
        if (stored == null || stored == SINGLE || EnumFacing.byIndex(stored).getAxis().isVertical()) {
            return;
        }
        EnumFacing dir = EnumFacing.byIndex(stored);
        TileEntityChest other = chestAt(world, pos.offset(dir), block);
        Byte back = other == null ? null : stored(other);
        if (back != null && back == dir.getOpposite().getIndex()) {
            join(world, pos.offset(dir), null);
        }
    }

    /** {@code getBoundingBox}: the box reaches toward the other half only. */
    public static AxisAlignedBB box(IBlockAccess world, BlockPos pos) {
        EnumFacing partner = partner(world, pos);
        if (partner == null) {
            return ALONE;
        }
        switch (partner) {
            case NORTH:
                return NORTH;
            case SOUTH:
                return SOUTH;
            case WEST:
                return WEST;
            default:
                return EAST;
        }
    }

    /** {@code getContainer}: this chest's inventory, joined to its other half's only (1.12.2's order of halves). */
    @Nullable
    public static ILockableContainer container(BlockChest block, World world, BlockPos pos, boolean allowBlocking) {
        TileEntity tile = world.getTileEntity(pos);
        if (!(tile instanceof TileEntityChest)) {
            return null;
        }
        ILockableContainer container = (TileEntityChest) tile;
        if (!allowBlocking && blocked(world, pos)) {
            return null;
        }
        EnumFacing partner = partner(world, pos);
        if (partner != null) {
            BlockPos at = pos.offset(partner);
            if (!allowBlocking && blocked(world, at)) {
                return null;
            }
            TileEntity other = world.getTileEntity(at);
            if (other instanceof TileEntityChest) {
                container = partner == EnumFacing.WEST || partner == EnumFacing.NORTH
                        ? new InventoryLargeChest("container.chestDouble", (TileEntityChest) other, container)
                        : new InventoryLargeChest("container.chestDouble", container, (TileEntityChest) other);
            }
        }
        return container;
    }

    /** 1.12.2's private {@code isBlocked}: a block above that stops the lid, or a cat sitting on it. */
    private static boolean blocked(World world, BlockPos pos) {
        if (world.getBlockState(pos.up()).doesSideBlockChestOpening(world, pos.up(), EnumFacing.DOWN)) {
            return true;
        }
        for (EntityOcelot cat : world.getEntitiesWithinAABB(EntityOcelot.class, new AxisAlignedBB(pos.getX(), pos.getY() + 1,
                pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1))) {
            if (cat.isSitting()) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------------------- TileEntityChest hooks

    /**
     * {@code TileEntityChest.getAdjacentChest(side)}: the other half if it is on that side, else null. This is what fills
     * the {@code adjacentChestXNeg/XPos/ZNeg/ZPos} fields every reader uses (the renderer, the lid, other mods). Tells
     * the other half, as 1.12.2's {@code setNeighbor} does, to look again if it saw something else there.
     */
    @Nullable
    public static TileEntityChest adjacent(TileEntityChest chest, EnumFacing side) {
        World world = chest.getWorld();
        if (partner(world, chest.getPos()) != side) {
            return null;
        }
        TileEntity tile = world.getTileEntity(chest.getPos().offset(side));
        if (!(tile instanceof TileEntityChest)) {
            return null;
        }
        TileEntityChest other = (TileEntityChest) tile;
        if (other.isInvalid()) {
            chest.adjacentChestChecked = false;
        } else if (other.adjacentChestChecked) {
            TileEntityChest seen;
            switch (side.getOpposite()) {
                case NORTH:
                    seen = other.adjacentChestZNeg;
                    break;
                case SOUTH:
                    seen = other.adjacentChestZPos;
                    break;
                case EAST:
                    seen = other.adjacentChestXPos;
                    break;
                default:
                    seen = other.adjacentChestXNeg;
            }
            if (seen != chest) {
                other.adjacentChestChecked = false;
            }
        }
        return other;
    }

    /** Added {@code TileEntityChest.getUpdatePacket}: the update tag (with the pairing) for clients, when handled. */
    @Nullable
    public static SPacketUpdateTileEntity updatePacket(TileEntityChest chest) {
        return handles(chest) ? new SPacketUpdateTileEntity(chest.getPos(), 0, chest.getUpdateTag()) : null;
    }

    /** Added {@code TileEntityChest.onDataPacket}: takes the pairing from the server's update and looks again. */
    public static void dataPacket(TileEntityChest chest, SPacketUpdateTileEntity packet) {
        NBTTagCompound tag = packet.getNbtCompound();
        NBTTagCompound forge = tag.getCompoundTag("ForgeData");
        if (forge.hasKey(KEY, 99)) {
            chest.getTileData().setByte(KEY, forge.getByte(KEY));
        } else {
            chest.getTileData().removeTag(KEY);
        }
        World world = chest.getWorld();
        if (world != null) {
            for (EnumFacing dir : EnumFacing.HORIZONTALS) {
                TileEntity tile = world.getTileEntity(chest.getPos().offset(dir));
                if (tile instanceof TileEntityChest) {
                    tile.updateContainingBlockInfo();
                }
            }
        }
        chest.updateContainingBlockInfo();
    }

    /** Forge's {@code VanillaDoubleChestItemHandler.get}: the item handler of the double chest by the stored pairing. */
    @Nullable
    public static VanillaDoubleChestItemHandler itemHandler(TileEntityChest chest) {
        World world = chest.getWorld();
        if (!world.isBlockLoaded(chest.getPos())) {
            return null;
        }
        EnumFacing partner = partner(world, chest.getPos());
        if (partner != null) {
            TileEntity other = world.getTileEntity(chest.getPos().offset(partner));
            if (other instanceof TileEntityChest) {
                return new VanillaDoubleChestItemHandler(chest, (TileEntityChest) other,
                        partner != EnumFacing.WEST && partner != EnumFacing.NORTH);
            }
        }
        return VanillaDoubleChestItemHandler.NO_ADJACENT_CHESTS_INSTANCE;
    }
}
