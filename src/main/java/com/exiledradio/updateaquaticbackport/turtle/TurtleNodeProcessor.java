package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.init.Blocks;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * 1.13's {@code WalkAndSwimNodeProcessor}: paths that walk on land and swim through water in all three
 * dimensions, preferring water (cost 0) to land (cost 6), and water beside a solid block least of all
 * among water (cost 4). 1.12.2 has walking paths that float on the surface and swimming paths that never
 * leave the water, nothing between.
 *
 * <p>1.12.2 has no {@code WATER_BORDER} node type, so the border cost is added here, where 1.13 looks it
 * up as the type's priority. Doors and leaves are never passed through, as 1.13.
 */
public class TurtleNodeProcessor extends WalkNodeProcessor {

    /** 1.13's priorities for land and for water touching a solid block. */
    private static final float WALKABLE_COST = 6.0F, WATER_BORDER_COST = 4.0F;

    private float savedWalkable;

    @Override
    public void init(IBlockAccess source, EntityLiving mob) {
        super.init(source, mob);
        mob.setPathPriority(PathNodeType.WATER, 0.0F);
        this.savedWalkable = mob.getPathPriority(PathNodeType.WALKABLE);
        mob.setPathPriority(PathNodeType.WALKABLE, WALKABLE_COST);
    }

    @Override
    public void postProcess() {
        this.entity.setPathPriority(PathNodeType.WALKABLE, this.savedWalkable);
        super.postProcess();
    }

    @Override
    public PathPoint getStart() {
        AxisAlignedBB box = this.entity.getEntityBoundingBox();
        return this.openPoint(MathHelper.floor(box.minX), MathHelper.floor(box.minY + 0.5D), MathHelper.floor(box.minZ));
    }

    /**
     * The target's node. 1.14 asks with a block's own coordinates and takes {@code floor(y + 0.5)}; 1.12.2's path
     * finder already adds 0.5 to a target block (and passes an entity's feet as they are), so the extra half here
     * would aim one block above every target - over the water, never into it.
     */
    @Override
    public PathPoint getPathPointToCoords(double x, double y, double z) {
        return this.openPoint(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
    }

    @Override
    public int findPathOptions(PathPoint[] options, PathPoint current, PathPoint target, float maxDistance) {
        int i = 0;
        double start = groundHeight(new BlockPos(current.x, current.y, current.z));
        PathPoint south = point(current.x, current.y, current.z + 1, 1, start);
        PathPoint west = point(current.x - 1, current.y, current.z, 1, start);
        PathPoint east = point(current.x + 1, current.y, current.z, 1, start);
        PathPoint north = point(current.x, current.y, current.z - 1, 1, start);
        PathPoint up = point(current.x, current.y + 1, current.z, 0, start);
        PathPoint down = point(current.x, current.y - 1, current.z, 1, start);
        for (PathPoint p : new PathPoint[] {south, west, east, north, up, down}) {
            if (usable(p, target, maxDistance)) {
                options[i++] = p;
            }
        }
        boolean n = open(north), s = open(south), e = open(east), w = open(west);
        if (n && w) {
            PathPoint p = point(current.x - 1, current.y, current.z - 1, 1, start);
            if (usable(p, target, maxDistance)) {
                options[i++] = p;
            }
        }
        if (n && e) {
            PathPoint p = point(current.x + 1, current.y, current.z - 1, 1, start);
            if (usable(p, target, maxDistance)) {
                options[i++] = p;
            }
        }
        if (s && w) {
            PathPoint p = point(current.x - 1, current.y, current.z + 1, 1, start);
            if (usable(p, target, maxDistance)) {
                options[i++] = p;
            }
        }
        if (s && e) {
            PathPoint p = point(current.x + 1, current.y, current.z + 1, 1, start);
            if (usable(p, target, maxDistance)) {
                options[i++] = p;
            }
        }
        return i;
    }

    private static boolean usable(@Nullable PathPoint p, PathPoint target, float maxDistance) {
        return p != null && !p.visited && p.distanceTo(target) < maxDistance;
    }

    /** 1.13: a diagonal is tried past a side that is open, costs something, or is missing. */
    private static boolean open(@Nullable PathPoint p) {
        return p == null || p.nodeType == PathNodeType.OPEN || p.costMalus != 0.0F;
    }

    /** Where the mob would stand in a block: the top of what is under it on land, mid-block in water. */
    private double groundHeight(BlockPos pos) {
        if (!this.entity.isInWater()) {
            BlockPos below = pos.down();
            IBlockState state = this.blockaccess.getBlockState(below);
            AxisAlignedBB box = state.getCollisionBoundingBox(this.blockaccess, below);
            return below.getY() + (box == null ? 0.0D : box.maxY);
        }
        return pos.getY() + 0.5D;
    }

    @Nullable
    private PathPoint point(int x, int y, int z, int stepsUp, double startHeight) {
        BlockPos pos = new BlockPos(x, y, z);
        if (groundHeight(pos) - startHeight > 1.125D) {
            return null;
        }
        PathNodeType type = nodeType(x, y, z);
        boolean border = type == PathNodeType.WATER && isWaterBorder(x, y, z);
        float cost = border ? WATER_BORDER_COST : this.entity.getPathPriority(type);
        double half = this.entity.width / 2.0D;
        PathPoint point = null;
        if (cost >= 0.0F) {
            point = this.openPoint(x, y, z);
            point.nodeType = type;
            point.costMalus = Math.max(point.costMalus, cost);
        }
        if (!border && (type == PathNodeType.WATER || type == PathNodeType.WALKABLE)) {
            if (y < this.entity.world.getSeaLevel() - 10 && point != null) {
                ++point.costMalus;
            }
            return point;
        }
        if (point == null && stepsUp > 0 && type != PathNodeType.FENCE && type != PathNodeType.TRAPDOOR) {
            point = point(x, y + 1, z, stepsUp - 1, startHeight);
        }
        if (type == PathNodeType.OPEN) {
            AxisAlignedBB room = new AxisAlignedBB(x - half + 0.5D, y + 0.001D, z - half + 0.5D,
                    x + half + 0.5D, y + this.entity.height, z + half + 0.5D);
            if (this.entity.world.collidesWithAnyBlock(room)) {
                return null;
            }
            PathNodeType below = nodeType(x, y - 1, z);
            if (below == PathNodeType.BLOCKED) {
                point = this.openPoint(x, y, z);
                point.nodeType = PathNodeType.WALKABLE;
                point.costMalus = Math.max(point.costMalus, cost);
                return point;
            }
            if (below == PathNodeType.WATER) {
                point = this.openPoint(x, y, z);
                point.nodeType = PathNodeType.WATER;
                point.costMalus = Math.max(point.costMalus, cost);
                return point;
            }
            int fall = 0;
            while (y > 0 && type == PathNodeType.OPEN) {
                --y;
                if (fall++ >= this.entity.getMaxFallHeight()) {
                    return null;
                }
                type = nodeType(x, y, z);
                cost = this.entity.getPathPriority(type);
                if (type != PathNodeType.OPEN && cost >= 0.0F) {
                    point = this.openPoint(x, y, z);
                    point.nodeType = type;
                    point.costMalus = Math.max(point.costMalus, cost);
                    break;
                }
                if (cost < 0.0F) {
                    return null;
                }
            }
        }
        return point;
    }

    /** The type over the mob's whole size, doors closed to it, as 1.13 asks it. */
    private PathNodeType nodeType(int x, int y, int z) {
        return this.getPathNodeType(this.blockaccess, x, y, z, this.entity, this.entitySizeX, this.entitySizeY, this.entitySizeZ, false, false);
    }

    private boolean isWaterBorder(int x, int y, int z) {
        for (EnumFacing facing : EnumFacing.values()) {
            if (this.getPathNodeTypeRaw(this.blockaccess, x + facing.getXOffset(), y + facing.getYOffset(), z + facing.getZOffset())
                    == PathNodeType.BLOCKED) {
                return true;
            }
        }
        return false;
    }

    /** 1.13's single-block type: open air over water counts as walkable (the surface), doors are walls. */
    @Override
    public PathNodeType getPathNodeType(IBlockAccess access, int x, int y, int z) {
        PathNodeType type = this.getPathNodeTypeRaw(access, x, y, z);
        if (type == PathNodeType.WATER) {
            return type;
        }
        if (type == PathNodeType.OPEN && y >= 1) {
            Block block = access.getBlockState(new BlockPos(x, y - 1, z)).getBlock();
            PathNodeType below = this.getPathNodeTypeRaw(access, x, y - 1, z);
            type = below != PathNodeType.WALKABLE && below != PathNodeType.OPEN && below != PathNodeType.LAVA
                    ? PathNodeType.WALKABLE : PathNodeType.OPEN;
            if (below == PathNodeType.DAMAGE_FIRE || block == Blocks.MAGMA) {
                type = PathNodeType.DAMAGE_FIRE;
            }
            if (below == PathNodeType.DAMAGE_CACTUS) {
                type = PathNodeType.DAMAGE_CACTUS;
            }
            if (below == PathNodeType.DAMAGE_OTHER) {
                type = PathNodeType.DAMAGE_OTHER;
            }
        }
        type = this.checkNeighborBlocks(access, x, y, z, type);
        if (type == PathNodeType.DOOR_OPEN || type == PathNodeType.DOOR_WOOD_CLOSED || type == PathNodeType.DOOR_IRON_CLOSED) {
            type = PathNodeType.BLOCKED;
        }
        return type;
    }
}
