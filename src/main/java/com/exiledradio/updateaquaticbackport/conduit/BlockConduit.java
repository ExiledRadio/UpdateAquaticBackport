package com.exiledradio.updateaquaticbackport.conduit;

import com.exiledradio.backportcore.waterlog.Waterloggable;

import net.minecraft.block.Block;
import net.minecraft.block.material.EnumPushReaction;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Explosion;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

/**
 * The conduit: 1.14.4's {@code ConduitBlock}, a waterloggable block with a {@link TileEntityConduit} that
 * does the work.
 *
 * <ul>
 *   <li>Waterloggable ({@link Waterloggable}, {@link Need#OPTIONAL}): placed into a water source it keeps the
 *       water, otherwise it is dry; a bucket empties or fills it. Its water is Fluidlogged API's, and the
 *       conduit needs it: its own block is the middle of the 3x3x3 of water it checks.</li>
 *   <li>Hardness and blast resistance 3, light 15, a 6x6x6 pixel box for collision and selection; drops
 *       itself, from an explosion too (1.14's conduit is immune to explosion losses); pickaxe fastest.</li>
 *   <li>Dry, it is glass: water does not flow into it. Pistons cannot move it. Mobs do not path through.</li>
 *   <li>Drawn by the tile entity's renderer; the block model is empty.</li>
 * </ul>
 */
public class BlockConduit extends Block implements Waterloggable {

    private static final AxisAlignedBB BOX = new AxisAlignedBB(5 / 16.0, 5 / 16.0, 5 / 16.0, 11 / 16.0, 11 / 16.0, 11 / 16.0);

    public BlockConduit() {
        super(Material.GLASS, MapColor.DIAMOND);
        // 1.12.2 keeps hardness * 5 as resistance and reports a fifth of it: 3, as 1.14's.
        this.setHardness(3.0F);
        this.setLightLevel(1.0F);
    }

    // ------------------------------------------------------------------------------------------
    // Tile entity
    // ------------------------------------------------------------------------------------------

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new TileEntityConduit();
    }

    // ------------------------------------------------------------------------------------------
    // Drops
    // ------------------------------------------------------------------------------------------

    @Override
    public Item getItemDropped(IBlockState state, java.util.Random rand, int fortune) {
        return Item.getItemFromBlock(this);
    }

    /** Explosions never lose it (1.14.4's loot tables list it as immune): it drops here, always. */
    @Override
    public boolean canDropFromExplosion(Explosion explosion) {
        return false;
    }

    @Override
    public void onBlockExploded(World world, BlockPos pos, Explosion explosion) {
        if (!world.isRemote) {
            spawnAsEntity(world, pos, new ItemStack(this));
        }
        super.onBlockExploded(world, pos, explosion);
    }

    // ------------------------------------------------------------------------------------------
    // Shape and movement
    // ------------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return BOX;
    }

    @Override
    @SuppressWarnings("deprecation")
    @Nullable
    public AxisAlignedBB getCollisionBoundingBox(IBlockState state, IBlockAccess world, BlockPos pos) {
        return BOX;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockFaceShape getBlockFaceShape(IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing face) {
        return BlockFaceShape.UNDEFINED;
    }

    /** The model is empty; cut-out keeps it out of the translucent pass. */
    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    /** 1.14.4: {@code allowsMovement} is false. */
    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return false;
    }

    /** A block with a tile entity: pistons cannot push it, as in 1.14. */
    @Override
    @SuppressWarnings("deprecation")
    public EnumPushReaction getPushReaction(IBlockState state) {
        return EnumPushReaction.BLOCK;
    }
}
