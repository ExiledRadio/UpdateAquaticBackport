package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.13's schooling fish (cod, salmon, tropical fish): a fish without a leader looks for one of its own
 * kind nearby every 10-11 seconds; a leader takes followers up to its school size; followers swim after
 * the leader while within 11 blocks. A natural group spawns as one school.
 */
public abstract class EntityAbstractGroupFish extends EntityAbstractFish {

    @Nullable
    private EntityAbstractGroupFish leader;
    private int schoolSize = 1;

    protected EntityAbstractGroupFish(World world) {
        super(world);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(5, new EntityAIFollowSchoolLeader(this));
    }

    /** @return how many fish a school of this kind can hold, the leader included (1.13: 8) */
    public int getMaxSchoolSize() {
        return 8;
    }

    @Override
    protected boolean canRandomSwim() {
        return !this.hasLeader();
    }

    public boolean hasLeader() {
        return this.leader != null && this.leader.isEntityAlive();
    }

    @Nullable
    public EntityAbstractGroupFish getLeader() {
        return this.leader;
    }

    /** Joins a leader's school. */
    public EntityAbstractGroupFish follow(EntityAbstractGroupFish newLeader) {
        this.leader = newLeader;
        newLeader.schoolSize++;
        return newLeader;
    }

    public void leaveSchool() {
        if (this.leader != null) {
            this.leader.schoolSize--;
            this.leader = null;
        }
    }

    public boolean canSchoolGrow() {
        return this.isLeader() && this.schoolSize < this.getMaxSchoolSize();
    }

    public boolean isLeader() {
        return this.schoolSize > 1;
    }

    public int getSchoolSize() {
        return this.schoolSize;
    }

    public boolean inRangeOfLeader() {
        return this.leader != null && this.getDistanceSq(this.leader) <= 121.0D;
    }

    public void moveToLeader() {
        if (this.hasLeader()) {
            this.getNavigator().tryMoveToEntityLiving(this.leader, 1.0D);
        }
    }

    /** Makes followers of the leaderless fish in this list, up to the school size. */
    void gather(List<EntityAbstractGroupFish> candidates) {
        int room = this.getMaxSchoolSize() - this.schoolSize;
        for (EntityAbstractGroupFish other : candidates) {
            if (room <= 0) {
                break;
            }
            if (other != this && !other.hasLeader()) {
                other.follow(this);
                room--;
            }
        }
    }

    /** A leader that finds itself alone (1 in 200 ticks) is a leader no more. */
    @Override
    public void onUpdate() {
        super.onUpdate();
        if (!this.world.isRemote && this.isLeader() && this.world.rand.nextInt(200) == 1) {
            List<? extends EntityAbstractGroupFish> near = this.world.getEntitiesWithinAABB(this.getClass(),
                    this.getEntityBoundingBox().grow(8.0D, 8.0D, 8.0D));
            if (near.size() <= 1) {
                this.schoolSize = 1;
            }
        }
    }

    @Override
    @Nullable
    protected IEntityLivingData prepareSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        if (data instanceof GroupData && ((GroupData) data).leader.getClass() == this.getClass()) {
            this.follow(((GroupData) data).leader);
        } else {
            data = new GroupData(this);
        }
        return super.prepareSpawn(difficulty, data);
    }

    /** Spawn data shared by a group: its leader. */
    public static class GroupData implements IEntityLivingData {
        public final EntityAbstractGroupFish leader;

        public GroupData(EntityAbstractGroupFish leader) {
            this.leader = leader;
        }
    }
}
