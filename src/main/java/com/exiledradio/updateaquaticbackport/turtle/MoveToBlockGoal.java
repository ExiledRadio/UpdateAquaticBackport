package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.14.4's {@code MoveToBlockGoal}: finds a block nearby that a subclass wants, walks to stand on it, and
 * stays a while. 1.12.2's own version searches differently (no vertical range, no start offset), and both
 * the turtle's goals and the zombie's egg stomping depend on 1.14's search order and reach.
 *
 * <p>Mutex: movement and jumping (1.12.2 bits 1 and 4).
 */
public abstract class MoveToBlockGoal extends EntityAIBase {

    protected final EntityCreature creature;
    public final double movementSpeed;
    protected int runDelay;
    protected int timeoutCounter;
    private int maxStayTicks;
    protected BlockPos destinationBlock = BlockPos.ORIGIN;
    private boolean isAboveDestination;
    private final int searchLength;
    private final int verticalSearchRange;
    /** The first layer searched, relative to the block under the mob's feet plus one; 1.14's {@code field_203112_e}. */
    protected int verticalSearchStart;

    public MoveToBlockGoal(EntityCreature creature, double speed, int length) {
        this(creature, speed, length, 1);
    }

    public MoveToBlockGoal(EntityCreature creature, double speed, int length, int verticalRange) {
        this.creature = creature;
        this.movementSpeed = speed;
        this.searchLength = length;
        this.verticalSearchStart = 0;
        this.verticalSearchRange = verticalRange;
        this.setMutexBits(5);
    }

    @Override
    public boolean shouldExecute() {
        if (this.runDelay > 0) {
            --this.runDelay;
            return false;
        }
        this.runDelay = getRunDelay(this.creature);
        return searchForDestination();
    }

    protected int getRunDelay(EntityCreature creature) {
        return 200 + creature.getRNG().nextInt(200);
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.timeoutCounter >= -this.maxStayTicks && this.timeoutCounter <= 1200
                && shouldMoveTo(this.creature.world, this.destinationBlock);
    }

    @Override
    public void startExecuting() {
        moveToDestination();
        this.timeoutCounter = 0;
        this.maxStayTicks = this.creature.getRNG().nextInt(this.creature.getRNG().nextInt(1200) + 1200) + 1200;
    }

    protected void moveToDestination() {
        this.creature.getNavigator().tryMoveToXYZ(this.destinationBlock.getX() + 0.5D, this.destinationBlock.getY() + 1,
                this.destinationBlock.getZ() + 0.5D, this.movementSpeed);
    }

    /** How close (in blocks, to the center of the block above the destination) counts as there. */
    public double getTargetDistance() {
        return 1.0D;
    }

    @Override
    public void updateTask() {
        double reach = getTargetDistance();
        if (this.destinationBlock.up().distanceSqToCenter(this.creature.posX, this.creature.posY, this.creature.posZ) >= reach * reach) {
            this.isAboveDestination = false;
            ++this.timeoutCounter;
            if (shouldMove()) {
                moveToDestination();
            }
        } else {
            this.isAboveDestination = true;
            --this.timeoutCounter;
        }
    }

    public boolean shouldMove() {
        return this.timeoutCounter % 40 == 0;
    }

    protected boolean getIsAboveDestination() {
        return this.isAboveDestination;
    }

    /** 1.14.4's search: layers from the start offset outward, rings of growing radius in each. */
    protected boolean searchForDestination() {
        BlockPos origin = new BlockPos(this.creature);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int k = this.verticalSearchStart; k <= this.verticalSearchRange; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < this.searchLength; ++l) {
                for (int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for (int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        pos.setPos(origin.getX() + i1, origin.getY() + k - 1, origin.getZ() + j1);
                        if (this.creature.isWithinHomeDistanceFromPosition(pos) && shouldMoveTo(this.creature.world, pos)) {
                            this.destinationBlock = pos.toImmutable();
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected abstract boolean shouldMoveTo(World world, BlockPos pos);
}
