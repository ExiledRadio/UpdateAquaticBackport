package com.exiledradio.updateaquaticbackport.structure;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.BlockStone;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.template.TemplateManager;

import java.util.Random;

/**
 * The buried treasure chest: today's {@code BuriedTreasurePieces.BuriedTreasurePiece}. From the sea floor
 * at its column it goes down to the first block standing on sandstone, stone, andesite, granite or diorite,
 * walls that spot in (the stone below where the side has nothing under it, else the block that was there,
 * or sand), and puts the chest there with the buried treasure table.
 *
 * <p>It is one column in its own chunk's populated area, so it is built from the real world at populate
 * time, as today's piece is, with no prediction.
 */
public class BuriedTreasurePiece extends StructureComponent {

    /** For loading. */
    public BuriedTreasurePiece() {
    }

    BuriedTreasurePiece(BlockPos pos) {
        super(0);
        this.boundingBox = new StructureBoundingBox(pos, pos);
    }

    private static boolean isHost(IBlockState state) {
        return state == Blocks.SANDSTONE.getDefaultState()
                || state == Blocks.STONE.getDefaultState()
                || state == Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.ANDESITE)
                || state == Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.GRANITE)
                || state == Blocks.STONE.getDefaultState().withProperty(BlockStone.VARIANT, BlockStone.EnumType.DIORITE);
    }

    /** Today's {@code isAir() || isLiquid(..)}: air, or still water or lava. */
    private static boolean open(IBlockState state) {
        return state.getMaterial() == Material.AIR
                || state == Blocks.WATER.getDefaultState() || state == Blocks.LAVA.getDefaultState();
    }

    /** {@code OCEAN_FLOOR_WG} of the column as it stands: one above the highest block that stops movement. */
    static int oceanFloor(World world, int x, int z) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, world.getHeight(x, z), z);
        while (p.getY() > 0) {
            p.setY(p.getY() - 1);
            if (world.getBlockState(p).getMaterial().blocksMovement()) {
                return p.getY() + 1;
            }
        }
        return 0;
    }

    @Override
    public boolean addComponentParts(World world, Random random, StructureBoundingBox chunk) {
        int x = this.boundingBox.minX;
        int z = this.boundingBox.minZ;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, oceanFloor(world, x, z), z);
        while (p.getY() > 0) {
            IBlockState here = world.getBlockState(p);
            IBlockState below = world.getBlockState(p.down());
            if (isHost(below)) {
                IBlockState cover = !open(here) ? here : Blocks.SAND.getDefaultState();
                for (EnumFacing side : EnumFacing.values()) {
                    BlockPos next = p.offset(side);
                    if (open(world.getBlockState(next))) {
                        if (open(world.getBlockState(next.down())) && side != EnumFacing.UP) {
                            world.setBlockState(next, below, 3);
                        } else {
                            world.setBlockState(next, cover, 3);
                        }
                    }
                }
                BlockPos at = p.toImmutable();
                this.boundingBox = new StructureBoundingBox(at, at);
                placeChest(world, chunk, random, at);
                return true;
            }
            p.move(EnumFacing.DOWN);
        }
        return true;
    }

    /** Today's {@code createChest}: facing away from what is solid beside it, never in water. */
    private static void placeChest(World world, StructureBoundingBox chunk, Random random, BlockPos at) {
        if (!chunk.isVecInside(at) || world.getBlockState(at).getBlock() == Blocks.CHEST) {
            return;
        }
        IBlockState chest = Blocks.CHEST.correctFacing(world, at, Blocks.CHEST.getDefaultState());
        Waterlogging.placeDry(world, at, chest, 2);
        TileEntity tile = world.getTileEntity(at);
        if (tile instanceof TileEntityChest) {
            ((TileEntityChest) tile).setLootTable(StructureContent.BURIED_TREASURE, random.nextLong());
        }
    }

    @Override
    protected void writeStructureToNBT(NBTTagCompound tag) {
    }

    @Override
    protected void readStructureFromNBT(NBTTagCompound tag, TemplateManager manager) {
    }
}
