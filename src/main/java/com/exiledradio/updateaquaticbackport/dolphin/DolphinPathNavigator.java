package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * 1.14.4's swimmer navigator as dolphins have it: it can plan in or out of water (a dolphin flopping on
 * a beach still heads for the water), paths may break the surface ({@link DolphinNodeProcessor}), and it
 * hands the move helper the path point itself, without 1.12.2's ground-height correction.
 */
final class DolphinPathNavigator extends PathNavigateSwimmer {

    @Nullable
    private BlockPos target;

    DolphinPathNavigator(EntityLiving entity, World world) {
        super(entity, world);
    }

    @Override
    protected PathFinder getPathFinder() {
        this.nodeProcessor = new DolphinNodeProcessor();
        return new PathFinder(this.nodeProcessor);
    }

    @Override
    protected boolean canNavigate() {
        return true;
    }

    @Override
    public boolean tryMoveToXYZ(double x, double y, double z, double speed) {
        this.target = new BlockPos(x, y, z);
        return super.tryMoveToXYZ(x, y, z, speed);
    }

    /** @return where the last move asked to go (1.14.4's {@code getTargetPos}), or null */
    @Nullable
    BlockPos target() {
        return this.noPath() ? null : this.target;
    }

    @Override
    public void onUpdateNavigation() {
        ++this.totalTicks;
        if (this.tryUpdatePath) {
            this.updatePath();
        }
        if (this.noPath()) {
            return;
        }
        if (this.canNavigate()) {
            this.pathFollow();
        } else if (this.currentPath != null && this.currentPath.getCurrentPathIndex() < this.currentPath.getCurrentPathLength()) {
            Vec3d next = this.currentPath.getVectorFromIndex(this.entity, this.currentPath.getCurrentPathIndex());
            if (MathHelper.floor(this.entity.posX) == MathHelper.floor(next.x)
                    && MathHelper.floor(this.entity.posY) == MathHelper.floor(next.y)
                    && MathHelper.floor(this.entity.posZ) == MathHelper.floor(next.z)) {
                this.currentPath.setCurrentPathIndex(this.currentPath.getCurrentPathIndex() + 1);
            }
        }
        if (!this.noPath()) {
            Vec3d at = this.currentPath.getPosition(this.entity);
            this.entity.getMoveHelper().setMoveTo(at.x, at.y, at.z, this.speed);
        }
    }

    /** 1.14.4's: reaching a point is judged by the mob's width, more loosely the faster it goes. */
    @Override
    protected void pathFollow() {
        if (this.currentPath == null) {
            return;
        }
        Vec3d here = this.getEntityPosition();
        float width = this.entity.width;
        float reach = width > 0.75F ? width / 2.0F : 0.75F - width / 2.0F;
        if (Math.abs(this.entity.motionX) > 0.2D || Math.abs(this.entity.motionZ) > 0.2D) {
            double speed = Math.sqrt(this.entity.motionX * this.entity.motionX + this.entity.motionY * this.entity.motionY
                    + this.entity.motionZ * this.entity.motionZ);
            reach = (float) (reach * speed * 6.0D);
        }
        Vec3d point = this.currentPath.getCurrentPos();
        // Forge's MC-94054 fix, as 1.14.4 carries it.
        double half = (int) (width + 1.0F) / 2.0D;
        if (Math.abs(this.entity.posX - (point.x + half)) < reach && Math.abs(this.entity.posZ - (point.z + half)) < reach
                && Math.abs(this.entity.posY - point.y) < reach * 2.0F) {
            this.currentPath.incrementPathIndex();
        }
        for (int j = Math.min(this.currentPath.getCurrentPathIndex() + 6, this.currentPath.getCurrentPathLength() - 1);
             j > this.currentPath.getCurrentPathIndex(); --j) {
            Vec3d ahead = this.currentPath.getVectorFromIndex(this.entity, j);
            if (ahead.squareDistanceTo(here) <= 36.0D && this.isDirectPathBetweenPoints(here, ahead, 0, 0, 0)) {
                this.currentPath.setCurrentPathIndex(j);
                break;
            }
        }
        this.checkForStuck(here);
    }

    /** 1.14.4's: anything that is not a full opaque cube can be swum to. */
    @Override
    public boolean canEntityStandOnPos(BlockPos pos) {
        return !this.world.getBlockState(pos).isOpaqueCube();
    }
}
