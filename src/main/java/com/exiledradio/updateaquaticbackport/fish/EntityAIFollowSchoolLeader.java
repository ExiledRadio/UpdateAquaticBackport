package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.entity.ai.EntityAIBase;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.13's {@code FollowSchoolLeaderGoal}. Uses no movement bits, as 1.13's goal claims no flags, so it runs
 * beside the swim and flee tasks and is asked whenever the task list checks.
 *
 * <p>A leaderless fish looks for a school every 200-219 ticks. 1.13 counts those down once per check,
 * which 1.13 does every tick and 1.12.2 every third, so the wait is kept in world ticks here instead.
 */
final class EntityAIFollowSchoolLeader extends EntityAIBase {

    private final EntityAbstractGroupFish fish;
    private int navigateTimer;
    private long nextSearch;

    EntityAIFollowSchoolLeader(EntityAbstractGroupFish fish) {
        this.fish = fish;
        this.nextSearch = fish.ticksExisted + wait(fish);
        this.setMutexBits(0);
    }

    private static int wait(EntityAbstractGroupFish fish) {
        return 200 + fish.getRNG().nextInt(200) % 20;
    }

    @Override
    public boolean shouldExecute() {
        if (this.fish.isLeader()) {
            return false;
        }
        if (this.fish.hasLeader()) {
            return true;
        }
        if (this.fish.ticksExisted < this.nextSearch) {
            return false;
        }
        this.nextSearch = this.fish.ticksExisted + wait(this.fish);
        List<? extends EntityAbstractGroupFish> near = this.fish.world.getEntitiesWithinAABB(this.fish.getClass(),
                this.fish.getEntityBoundingBox().grow(8.0D, 8.0D, 8.0D));
        List<EntityAbstractGroupFish> open = new ArrayList<EntityAbstractGroupFish>();
        EntityAbstractGroupFish leader = null;
        for (EntityAbstractGroupFish other : near) {
            if (other.canSchoolGrow() || !other.hasLeader()) {
                open.add(other);
                if (leader == null && other.canSchoolGrow()) {
                    leader = other;
                }
            }
        }
        if (leader == null) {
            leader = this.fish;
        }
        List<EntityAbstractGroupFish> leaderless = new ArrayList<EntityAbstractGroupFish>();
        for (EntityAbstractGroupFish other : open) {
            if (!other.hasLeader()) {
                leaderless.add(other);
            }
        }
        leader.gather(leaderless);
        return this.fish.hasLeader();
    }

    @Override
    public boolean shouldContinueExecuting() {
        return this.fish.hasLeader() && this.fish.inRangeOfLeader();
    }

    @Override
    public void startExecuting() {
        this.navigateTimer = 0;
    }

    @Override
    public void resetTask() {
        this.fish.leaveSchool();
    }

    @Override
    public void updateTask() {
        if (--this.navigateTimer <= 0) {
            this.navigateTimer = 10;
            this.fish.moveToLeader();
        }
    }
}
