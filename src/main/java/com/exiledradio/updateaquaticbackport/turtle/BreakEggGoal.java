package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14.4's {@code BreakBlockGoal} as zombies use it on turtle eggs ({@code ZombieEntity.AttackTurtleEggGoal}):
 * seek an egg with two blocks of air above it within 24 blocks and 3 up or down, climb onto it and stomp
 * for three seconds until the whole cluster breaks. Only where mob griefing is allowed.
 */
public class BreakEggGoal extends MoveToBlockGoal {

    private final Block block;
    private int breakingTime;

    public BreakEggGoal(Block block, EntityCreature creature, double speed, int verticalRange) {
        super(creature, speed, 24, verticalRange);
        this.block = block;
    }

    @Override
    public boolean shouldExecute() {
        if (!canDestroy()) {
            return false;
        }
        if (this.runDelay > 0) {
            --this.runDelay;
            return false;
        }
        if (hasTarget()) {
            this.runDelay = 20;
            return true;
        }
        this.runDelay = getRunDelay(this.creature);
        return false;
    }

    /** Forge 1.14's {@code ForgeHooks.canEntityDestroy}: the mob griefing rule and the block's own say. */
    private boolean canDestroy() {
        World world = this.creature.world;
        if (!ForgeEventFactory.getMobGriefingEvent(world, this.creature)) {
            return false;
        }
        IBlockState state = world.getBlockState(this.destinationBlock);
        return state.getBlock().canEntityDestroy(state, world, this.destinationBlock, this.creature);
    }

    private boolean hasTarget() {
        return this.destinationBlock != null && shouldMoveTo(this.creature.world, this.destinationBlock) || searchForDestination();
    }

    @Override
    public void resetTask() {
        super.resetTask();
        this.creature.fallDistance = 1.0F;
    }

    @Override
    public void startExecuting() {
        super.startExecuting();
        this.breakingTime = 0;
    }

    @Override
    public double getTargetDistance() {
        return 1.14D;
    }

    @Override
    public void updateTask() {
        super.updateTask();
        World world = this.creature.world;
        BlockPos egg = findTarget(new BlockPos(this.creature), world);
        Random random = this.creature.getRNG();
        if (!getIsAboveDestination() || egg == null) {
            return;
        }
        if (this.breakingTime > 0) {
            this.creature.motionY = 0.3D;
            if (world instanceof WorldServer) {
                ((WorldServer) world).spawnParticle(EnumParticleTypes.ITEM_CRACK, egg.getX() + 0.5D, egg.getY() + 0.7D, egg.getZ() + 0.5D, 3,
                        (random.nextFloat() - 0.5D) * 0.08D, (random.nextFloat() - 0.5D) * 0.08D, (random.nextFloat() - 0.5D) * 0.08D,
                        0.15F, Item.getIdFromItem(Items.EGG));
            }
        }
        if (this.breakingTime % 2 == 0) {
            this.creature.motionY = -0.3D;
            if (this.breakingTime % 6 == 0) {
                world.playSound(null, this.destinationBlock, TurtleSounds.ZOMBIE_DESTROY_EGG, SoundCategory.HOSTILE, 0.5F,
                        0.9F + random.nextFloat() * 0.2F);
            }
        }
        if (this.breakingTime > 60) {
            world.setBlockToAir(egg);
            if (world instanceof WorldServer) {
                for (int i = 0; i < 20; ++i) {
                    ((WorldServer) world).spawnParticle(EnumParticleTypes.EXPLOSION_NORMAL, egg.getX() + 0.5D, egg.getY(), egg.getZ() + 0.5D, 1,
                            random.nextGaussian() * 0.02D, random.nextGaussian() * 0.02D, random.nextGaussian() * 0.02D, 0.15F);
                }
                world.playSound(null, egg, TurtleSounds.EGG_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.rand.nextFloat() * 0.2F);
            }
        }
        ++this.breakingTime;
    }

    /** The egg under or beside the mob's feet. */
    @Nullable
    private BlockPos findTarget(BlockPos pos, World world) {
        if (world.getBlockState(pos).getBlock() == this.block) {
            return pos;
        }
        for (BlockPos p : new BlockPos[] {pos.down(), pos.west(), pos.east(), pos.north(), pos.south(), pos.down().down()}) {
            if (world.getBlockState(p).getBlock() == this.block) {
                return p;
            }
        }
        return null;
    }

    @Override
    protected boolean shouldMoveTo(World world, BlockPos pos) {
        return world.isBlockLoaded(pos) && world.getBlockState(pos).getBlock() == this.block
                && world.isAirBlock(pos.up()) && world.isAirBlock(pos.up(2));
    }
}
