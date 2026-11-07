package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The turtle egg: one to four eggs in a block, cracking in three stages on sand until they hatch.
 *
 * <p>From 1.14.4's {@code TurtleEggBlock}, with today's trampling rule: turtles and bats never break eggs,
 * nor does anything that is not alive; sneaking over them is safe, landing on them is not. A random tick
 * between 0.65 and 0.69 of the day's turn (about 3 to 4 am) always cracks an egg, any other one in 500.
 * Eggs need no support and drop only with Silk Touch, one egg per break.
 *
 * <p>Metadata: {@code eggs - 1} in bits 0-1, {@code hatch} in bits 2-3.
 */
public class BlockTurtleEgg extends Block {

    public static final PropertyInteger HATCH = PropertyInteger.create("hatch", 0, 2);
    public static final PropertyInteger EGGS = PropertyInteger.create("eggs", 1, 4);

    private static final AxisAlignedBB ONE_EGG = new AxisAlignedBB(3 / 16.0, 0, 3 / 16.0, 12 / 16.0, 7 / 16.0, 12 / 16.0);
    private static final AxisAlignedBB MULTI_EGG = new AxisAlignedBB(1 / 16.0, 0, 1 / 16.0, 15 / 16.0, 7 / 16.0, 15 / 16.0);

    public BlockTurtleEgg() {
        // 1.14.4: dragon egg material (pistons break it), sand color, hardness and resistance 0.5, metal sounds.
        super(Material.DRAGON_EGG, MapColor.SAND);
        setHardness(0.5F);
        // 1.12.2 multiplies by 3 and divides by 5 when asked; 1.14's 0.5 is 0.5 * 5 / 3 here.
        setResistance(0.5F * 5.0F / 3.0F);
        setSoundType(SoundType.METAL);
        setTickRandomly(true);
        setDefaultState(this.blockState.getBaseState().withProperty(HATCH, 0).withProperty(EGGS, 1));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, HATCH, EGGS);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return (state.getValue(EGGS) - 1) | state.getValue(HATCH) << 2;
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(EGGS, (meta & 3) + 1).withProperty(HATCH, Math.min(2, meta >> 2 & 3));
    }

    // --- trampling ------------------------------------------------------------------------------------

    @Override
    public void onEntityWalk(World world, BlockPos pos, Entity entity) {
        if (!entity.isSneaking()) {
            tryTrample(world, pos, entity, 100);
        }
        super.onEntityWalk(world, pos, entity);
    }

    @Override
    public void onFallenUpon(World world, BlockPos pos, Entity entity, float fallDistance) {
        if (!(entity instanceof EntityZombie)) {
            tryTrample(world, pos, entity, 3);
        }
        super.onFallenUpon(world, pos, entity, fallDistance);
    }

    private void tryTrample(World world, BlockPos pos, Entity entity, int chance) {
        if (canTrample(world, entity) && !world.isRemote && world.rand.nextInt(chance) == 0) {
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == this) {
                removeOneEgg(world, pos, state);
            }
        }
    }

    /** Today's rule: never turtles or bats or anything not alive; players always; other mobs if mob griefing allows. */
    public static boolean canTrample(World world, Entity entity) {
        if (entity instanceof EntityTurtle || entity instanceof EntityBat || !(entity instanceof EntityLivingBase)) {
            return false;
        }
        return entity instanceof EntityPlayer || ForgeEventFactory.getMobGriefingEvent(world, entity);
    }

    /** Breaks one egg: the last one takes the block with it. */
    public void removeOneEgg(World world, BlockPos pos, IBlockState state) {
        world.playSound(null, pos, TurtleSounds.EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.rand.nextFloat() * 0.2F);
        int eggs = state.getValue(EGGS);
        if (eggs <= 1) {
            world.destroyBlock(pos, false);
        } else {
            world.setBlockState(pos, state.withProperty(EGGS, eggs - 1), 2);
            world.playEvent(2001, pos, Block.getStateId(state));
        }
    }

    // --- hatching --------------------------------------------------------------------------------------

    @Override
    public void randomTick(World world, BlockPos pos, IBlockState state, Random random) {
        if (!canGrow(world) || !onSand(world, pos)) {
            return;
        }
        int hatch = state.getValue(HATCH);
        if (hatch < 2) {
            world.playSound(null, pos, TurtleSounds.EGG_CRACK, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            world.setBlockState(pos, state.withProperty(HATCH, hatch + 1), 2);
        } else {
            hatch(world, pos, state, random);
        }
    }

    /** The eggs hatch: one baby turtle each, at home where they hatched, facing south. */
    public void hatch(World world, BlockPos pos, IBlockState state, Random random) {
        world.playSound(null, pos, TurtleSounds.EGG_HATCH, SoundCategory.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
        world.setBlockToAir(pos);
        if (world.isRemote) {
            return;
        }
        for (int j = 0; j < state.getValue(EGGS); j++) {
            world.playEvent(2001, pos, Block.getStateId(state));
            EntityTurtle turtle = new EntityTurtle(world);
            turtle.setGrowingAge(-24000);
            turtle.setHome(pos);
            turtle.setLocationAndAngles(pos.getX() + 0.3 + j * 0.2, pos.getY(), pos.getZ() + 0.3, 0.0F, 0.0F);
            world.spawnEntity(turtle);
        }
    }

    /** Scheduled ticks do nothing; random ticks crack and hatch. */
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
    }

    /** 1.14.4: about 3 to 4 am every tick counts, otherwise one in 500. */
    public static boolean canGrow(World world) {
        float angle = world.getCelestialAngle(1.0F);
        return angle < 0.69 && angle > 0.65 || world.rand.nextInt(500) == 0;
    }

    /** Sand or red sand underneath (1.12.2's sand block is both). */
    public static boolean onSand(IBlockAccess world, BlockPos pos) {
        return world.getBlockState(pos.down()).getBlock() == Blocks.SAND;
    }

    @Override
    public void onBlockAdded(World world, BlockPos pos, IBlockState state) {
        if (onSand(world, pos) && !world.isRemote) {
            world.playEvent(2005, pos, 0);
        }
    }

    // --- breaking and drops ----------------------------------------------------------------------------

    @Override
    public void harvestBlock(World world, EntityPlayer player, BlockPos pos, IBlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(world, player, pos, state, te, stack);
        // The player's break took the whole block; put back all but one egg.
        removeOneEgg(world, pos, state);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Items.AIR;
    }

    @Override
    public int quantityDropped(Random random) {
        return 0;
    }

    @Override
    public boolean canSilkHarvest(World world, BlockPos pos, IBlockState state, EntityPlayer player) {
        return true;
    }

    @Override
    protected ItemStack getSilkTouchDrop(IBlockState state) {
        return new ItemStack(Item.getItemFromBlock(this));
    }

    // --- shape -----------------------------------------------------------------------------------------

    @Override
    @SuppressWarnings("deprecation")
    public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
        return state.getValue(EGGS) > 1 ? MULTI_EGG : ONE_EGG;
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

    @Override
    @SideOnly(Side.CLIENT)
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }
}
