package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.PathFinder;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * 1.13's turtle navigator: a swimmer's path following, on land as in water, over
 * {@link TurtleNodeProcessor}'s walk-and-swim paths.
 *
 * <p>While travelling the open sea a turtle only picks water to head for; otherwise anything with a block
 * under it.
 */
public class TurtleNavigator extends PathNavigate {

    public TurtleNavigator(EntityLiving entity, World world) {
        super(entity, world);
    }

    @Override
    protected PathFinder getPathFinder() {
        this.nodeProcessor = new TurtleNodeProcessor();
        this.nodeProcessor.setCanSwim(true);
        return new PathFinder(this.nodeProcessor);
    }

    @Override
    protected boolean canNavigate() {
        return true;
    }

    @Override
    protected Vec3d getEntityPosition() {
        return new Vec3d(this.entity.posX, this.entity.posY + this.entity.height * 0.5D, this.entity.posZ);
    }

    /** 1.13 {@code SwimmerPathNavigator.tick}: no ground snapping of the next point, as a swimmer. */
    @Override
    public void onUpdateNavigation() {
        ++this.totalTicks;
        if (this.tryUpdatePath) {
            this.updatePath();
        }
        if (!this.noPath()) {
            this.pathFollow();
            if (!this.noPath()) {
                Vec3d next = this.currentPath.getPosition(this.entity);
                this.entity.getMoveHelper().setMoveTo(next.x, next.y, next.z, this.speed);
            }
        }
    }

    /** 1.13 {@code SwimmerPathNavigator.pathFollow}, with Forge's MC-94054 centering fix. */
    @Override
    protected void pathFollow() {
        if (this.currentPath == null) {
            return;
        }
        Vec3d pos = this.getEntityPosition();
        float width = this.entity.width;
        float reach = width > 0.75F ? width / 2.0F : 0.75F - width / 2.0F;
        if (Math.abs(this.entity.motionX) > 0.2D || Math.abs(this.entity.motionZ) > 0.2D) {
            double speed = Math.sqrt(this.entity.motionX * this.entity.motionX + this.entity.motionY * this.entity.motionY
                    + this.entity.motionZ * this.entity.motionZ);
            reach = (float) (reach * speed * 6.0D);
        }
        Vec3d current = this.currentPath.getCurrentPos();
        double center = (int) (width + 1.0F) / 2.0D;
        // Vertically, today's navigation's "within one block" rather than 1.14's twice the reach (1.2 for a
        // turtle): with 1.2, a turtle on the shore counts the water block beside and below it as reached, its
        // path ends, and it stands at the water's edge for good.
        if (Math.abs(this.entity.posX - (current.x + center)) < reach && Math.abs(this.entity.posZ - (current.z + center)) < reach
                && Math.abs(this.entity.posY - current.y) < Math.min(1.0D, reach * 2.0F)) {
            this.currentPath.incrementPathIndex();
        }
        for (int j = Math.min(this.currentPath.getCurrentPathIndex() + 6, this.currentPath.getCurrentPathLength() - 1);
             j > this.currentPath.getCurrentPathIndex(); --j) {
            Vec3d ahead = this.currentPath.getVectorFromIndex(this.entity, j);
            if (ahead.squareDistanceTo(pos) <= 36.0D && this.isDirectPathBetweenPoints(pos, ahead, 0, 0, 0)) {
                this.currentPath.setCurrentPathIndex(j);
                break;
            }
        }
        this.checkForStuck(pos);
    }

    @Override
    protected boolean isDirectPathBetweenPoints(Vec3d from, Vec3d to, int sizeX, int sizeY, int sizeZ) {
        RayTraceResult hit = this.world.rayTraceBlocks(from, new Vec3d(to.x, to.y + this.entity.height * 0.5D, to.z), false, true, false);
        return hit == null || hit.typeOfHit == RayTraceResult.Type.MISS;
    }

    @Override
    public boolean canEntityStandOnPos(BlockPos pos) {
        if (this.entity instanceof EntityTurtle && ((EntityTurtle) this.entity).isTravelling()) {
            return this.world.getBlockState(pos).getMaterial() == Material.WATER;
        }
        return !this.world.isAirBlock(pos.down());
    }
}
