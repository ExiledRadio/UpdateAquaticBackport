package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.pathfinding.NodeProcessor;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.IBlockAccess;

import javax.annotation.Nullable;

/**
 * 1.14.4's swim node processor as it is for dolphins: water, plus the air just above water ("breach"
 * in 1.14.4, which 1.12.2 has no node type for; it is {@link PathNodeType#OPEN} here) at a cost of 8
 * more, so a path may cross the surface but prefers to stay under it.
 */
final class DolphinNodeProcessor extends NodeProcessor {

    /** 1.14.4 adds this to every node that has no water in it. */
    private static final float OUT_OF_WATER_COST = 8.0F;

    @Override
    public PathPoint getStart() {
        // Start and end are plain points, as 1.14.4's (they never come back null).
        return this.openPoint(MathHelper.floor(this.entity.getEntityBoundingBox().minX),
                MathHelper.floor(this.entity.getEntityBoundingBox().minY + 0.5D),
                MathHelper.floor(this.entity.getEntityBoundingBox().minZ));
    }

    @Override
    public PathPoint getPathPointToCoords(double x, double y, double z) {
        return this.openPoint(MathHelper.floor(x - this.entity.width / 2.0F), MathHelper.floor(y + 0.5D),
                MathHelper.floor(z - this.entity.width / 2.0F));
    }

    @Override
    public int findPathOptions(PathPoint[] options, PathPoint current, PathPoint target, float maxDistance) {
        int n = 0;
        for (EnumFacing facing : EnumFacing.values()) {
            PathPoint point = this.node(current.x + facing.getXOffset(), current.y + facing.getYOffset(), current.z + facing.getZOffset());
            if (point != null && !point.visited && point.distanceTo(target) < maxDistance) {
                options[n++] = point;
            }
        }
        return n;
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z, EntityLiving mob, int xSize, int ySize,
                                        int zSize, boolean canBreakDoors, boolean canEnterDoors) {
        return this.getPathNodeType(world, x, y, z);
    }

    @Override
    public PathNodeType getPathNodeType(IBlockAccess world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        IBlockState state = world.getBlockState(pos);
        if (isBreach(world, pos, state)) {
            return PathNodeType.OPEN;
        }
        return isSwimmable(world, pos, state) ? PathNodeType.WATER : PathNodeType.BLOCKED;
    }

    /**
     * Water a mob can move through, 1.14.4's water fluid that allows movement: a water block, or a waterlogged
     * block whose material does not block movement (kelp, seagrass, living coral; not a conduit or a slab).
     */
    static boolean isSwimmable(IBlockAccess world, BlockPos pos, IBlockState state) {
        return Waterlogging.hasWater(world, pos) && !state.getMaterial().blocksMovement();
    }

    /** {@link #isSwimmable(IBlockAccess, BlockPos, IBlockState)} for the block at a position. */
    static boolean isSwimmable(IBlockAccess world, BlockPos pos) {
        return isSwimmable(world, pos, world.getBlockState(pos));
    }

    /** Air directly above water. */
    private static boolean isBreach(IBlockAccess world, BlockPos pos, IBlockState state) {
        return state.getMaterial() == Material.AIR && isSwimmable(world, pos.down());
    }

    @Nullable
    private PathPoint node(int x, int y, int z) {
        PathNodeType type = this.free(x, y, z);
        return type == PathNodeType.WATER || type == PathNodeType.OPEN ? this.open(x, y, z) : null;
    }

    @Nullable
    private PathPoint open(int x, int y, int z) {
        PathNodeType type = this.getPathNodeType(this.blockaccess, x, y, z);
        float priority = this.entity.getPathPriority(type);
        if (priority < 0.0F) {
            return null;
        }
        PathPoint point = this.openPoint(x, y, z);
        point.nodeType = type;
        point.costMalus = Math.max(point.costMalus, priority);
        if (type != PathNodeType.WATER) {
            point.costMalus += OUT_OF_WATER_COST;
        }
        return point;
    }

    /** 1.14.4's {@code isFree}: every block the mob would fill is water, or one is air just above water. */
    private PathNodeType free(int x, int y, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = x; i < x + this.entitySizeX; ++i) {
            for (int j = y; j < y + this.entitySizeY; ++j) {
                for (int k = z; k < z + this.entitySizeZ; ++k) {
                    pos.setPos(i, j, k);
                    IBlockState state = this.blockaccess.getBlockState(pos);
                    if (isBreach(this.blockaccess, pos, state)) {
                        return PathNodeType.OPEN;
                    }
                    if (!isSwimmable(this.blockaccess, pos, state)) {
                        return PathNodeType.BLOCKED;
                    }
                }
            }
        }
        return PathNodeType.WATER;
    }
}
