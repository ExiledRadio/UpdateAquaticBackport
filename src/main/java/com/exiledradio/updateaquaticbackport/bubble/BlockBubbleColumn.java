package com.exiledradio.updateaquaticbackport.bubble;

import com.exiledradio.updateaquaticbackport.common.BlockPseudoWater;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * The bubble column: water over soul sand (upward) or a magma block (downward, a whirlpool).
 *
 * <h2>The block is the water</h2>
 * A {@link BlockPseudoWater}, always water, like kelp: swimming, drowning, fog, boats and flowing water all
 * see a still water source. What differs from kelp: nothing here can be hit or seen except the water's
 * surface, so a player's ordinary ray trace passes through it as through water ({@link #canCollideCheck})
 * and only a bucket's stops on it; another block placed here replaces it; an empty bucket takes its water
 * and leaves air, as 1.13's ({@link #drained}).
 *
 * <h2>From 1.14.4's BubbleColumnBlock and today's BubbleColumnBlock</h2>
 * <ul>
 *   <li>{@link #DRAG} true is a whirlpool, false an upward column. Stored in metadata: 0 drag, 1 upward.</li>
 *   <li>Entities inside are pushed (1.14.4's numbers, unchanged today): upward +0.06 per collision up to
 *       0.7, downward -0.03 down to -0.3, and their fall distance resets. In the top block, under air,
 *       upward +0.1 up to 1.8 and downward -0.03 down to -0.9, with splash and bubble particles.</li>
 *   <li>A boat on top of a column rocks instead of being pushed, and after 60 ticks is thrown up or sinks
 *       ({@link BoatRocking}).</li>
 *   <li>Today's column changes as one: a change below, or a new water source above, schedules this block
 *       5 ticks out; its tick then sets the whole column above to the state the block below asks for
 *       ({@link BubbleColumns#updateColumn}).</li>
 *   <li>Particles and ambient sounds as 1.14.4's {@code animateTick} ({@code client.BubbleEffects}).</li>
 * </ul>
 */
public class BlockBubbleColumn extends BlockPseudoWater {

    /** 1.13's {@code drag}: true pulls down (magma), false pushes up (soul sand). */
    public static final PropertyBool DRAG = PropertyBool.create("drag");

    /** 1.14.4's tick rate. */
    static final int TICK_RATE = 5;

    public BlockBubbleColumn() {
        super(Material.WATER, MapColor.WATER);
        this.setDefaultState(this.baseState().withProperty(DRAG, true));
        // Water's: never broken by hand, blast resistance 100 (1.12.2 divides by 5 and multiplies by 3).
        this.setHardness(100.0F);
        this.setResistance(100.0F * 5.0F / 3.0F);
    }

    @Override
    public boolean canBeDry() {
        return false;
    }

    @Override
    protected IProperty<?>[] ownProperties() {
        return new IProperty<?>[] {DRAG};
    }

    /** @return this column's state for a drag direction */
    public IBlockState withDrag(boolean drag) {
        return this.getDefaultState().withProperty(DRAG, drag);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getStateFromMeta(int meta) {
        return this.withDrag((meta & 1) == 0);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(DRAG) ? 0 : 1;
    }

    // ------------------------------------------------------------------------------------------
    // Pushing and pulling
    // ------------------------------------------------------------------------------------------

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entity) {
        boolean drag = state.getValue(DRAG);
        if (world.isAirBlock(pos.up())) {
            if (entity instanceof EntityBoat) {
                BoatRocking.touch((EntityBoat) entity, drag);
                if (world.isRemote) {
                    Random rand = world.rand;
                    world.spawnParticle(EnumParticleTypes.WATER_SPLASH, entity.posX + rand.nextFloat(), entity.posY + 0.7,
                            entity.posZ + rand.nextFloat(), 0.0, 0.0, 0.0);
                    if (rand.nextInt(20) == 0) {
                        world.playSound(entity.posX, entity.posY, entity.posZ, SoundEvents.ENTITY_GENERIC_SPLASH,
                                entity.getSoundCategory(), 1.0F, 0.8F + 0.4F * rand.nextFloat(), false);
                    }
                }
            } else {
                entity.motionY = drag ? Math.max(-0.9, entity.motionY - 0.03) : Math.min(1.8, entity.motionY + 0.1);
            }
            if (world instanceof WorldServer) {
                WorldServer server = (WorldServer) world;
                for (int i = 0; i < 2; i++) {
                    server.spawnParticle(EnumParticleTypes.WATER_SPLASH, pos.getX() + world.rand.nextFloat(), pos.getY() + 1,
                            pos.getZ() + world.rand.nextFloat(), 1, 0.0, 0.0, 0.0, 1.0);
                    server.spawnParticle(EnumParticleTypes.WATER_BUBBLE, pos.getX() + world.rand.nextFloat(), pos.getY() + 1,
                            pos.getZ() + world.rand.nextFloat(), 1, 0.0, 0.01, 0.0, 0.2);
                }
            }
        } else {
            entity.motionY = drag ? Math.max(-0.3, entity.motionY - 0.03) : Math.min(0.7, entity.motionY + 0.06);
            entity.fallDistance = 0.0F;
        }
    }

    // ------------------------------------------------------------------------------------------
    // Keeping the column in step with its base
    // ------------------------------------------------------------------------------------------

    /** @return true if the block below can carry a column: soul sand, a magma block or more column */
    public boolean canSurvive(IBlockAccess world, BlockPos pos) {
        Block below = world.getBlockState(pos.down()).getBlock();
        return below == this || BubbleColumns.isBase(below);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block neighbor, BlockPos from) {
        if (world.isRemote) {
            return;
        }
        boolean schedule = !this.canSurvive(world, pos) || from.equals(pos.down());
        if (!schedule && from.equals(pos.up())) {
            IBlockState above = world.getBlockState(from);
            schedule = above.getBlock() != this && BubbleColumns.canExistIn(above);
        }
        if (schedule) {
            world.scheduleUpdate(pos, this, TICK_RATE);
        }
    }

    @Override
    public int tickRate(World world) {
        return TICK_RATE;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random random) {
        BubbleColumns.updateColumn(world, pos, world.getBlockState(pos.down()));
    }

    // ------------------------------------------------------------------------------------------
    // Water that nothing can hit
    // ------------------------------------------------------------------------------------------

    /** Only a ray trace that stops on liquids (a bucket's) stops here, as on water. */
    @Override
    public boolean canCollideCheck(IBlockState state, boolean hitIfLiquid) {
        return hitIfLiquid;
    }

    /** 1.13's bubble column is replaceable: a block placed here takes its place. */
    @Override
    public boolean isReplaceable(IBlockAccess world, BlockPos pos) {
        return true;
    }

    /** 1.13's bucket takes the water and leaves air. */
    @Override
    public IBlockState drained(IBlockState state) {
        return Blocks.AIR.getDefaultState();
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
    public ItemStack getItem(World world, BlockPos pos, IBlockState state) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canHarvestBlock(IBlockAccess world, BlockPos pos, EntityPlayer player) {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(IBlockState state, World world, BlockPos pos, Random rand) {
        com.exiledradio.updateaquaticbackport.bubble.client.BubbleEffects.animate(world, pos, state.getValue(DRAG), rand);
    }
}
