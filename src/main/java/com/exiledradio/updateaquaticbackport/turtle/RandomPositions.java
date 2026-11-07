package com.exiledradio.updateaquaticbackport.turtle;

import net.minecraft.entity.EntityCreature;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * 1.14.4's {@code RandomPositionGenerator}, the parts turtles use. 1.12.2's has no "towards, within an
 * angle" search, which the turtle's travel and homing goals are built on, and picks its candidates in a
 * square rather than 1.14's disc.
 */
final class RandomPositions {

    private static final double HALF_PI = Math.PI / 2;

    private RandomPositions() {
    }

    @Nullable
    static Vec3d findRandomTarget(EntityCreature creature, int xz, int y) {
        return generate(creature, xz, y, null, HALF_PI);
    }

    @Nullable
    static Vec3d findRandomTargetBlockTowards(EntityCreature creature, int xz, int y, Vec3d target) {
        return generate(creature, xz, y, target.subtract(creature.posX, creature.posY, creature.posZ), HALF_PI);
    }

    /** Towards a target, within an angle either side of the straight line to it. */
    @Nullable
    static Vec3d findRandomTargetTowardsScaled(EntityCreature creature, int xz, int y, Vec3d target, double angle) {
        return generate(creature, xz, y, target.subtract(creature.posX, creature.posY, creature.posZ), angle);
    }

    @Nullable
    private static Vec3d generate(EntityCreature creature, int xz, int y, @Nullable Vec3d direction, double angle) {
        PathNavigate navigator = creature.getNavigator();
        Random random = creature.getRNG();
        boolean nearHome = false;
        if (creature.hasHome()) {
            double r = creature.getMaximumHomeDistance() + xz + 1.0D;
            nearHome = creature.getHomePosition().distanceSqToCenter(creature.posX, creature.posY, creature.posZ) < r * r;
        }
        boolean found = false;
        double best = Double.NEGATIVE_INFINITY;
        BlockPos result = new BlockPos(creature);
        for (int i = 0; i < 10; ++i) {
            BlockPos offset = offset(random, xz, y, direction, angle);
            if (offset == null) {
                continue;
            }
            int dx = offset.getX();
            int dy = offset.getY();
            int dz = offset.getZ();
            if (creature.hasHome() && xz > 1) {
                BlockPos home = creature.getHomePosition();
                dx += creature.posX > home.getX() ? -random.nextInt(xz / 2) : random.nextInt(xz / 2);
                dz += creature.posZ > home.getZ() ? -random.nextInt(xz / 2) : random.nextInt(xz / 2);
            }
            BlockPos candidate = new BlockPos(dx + creature.posX, dy + creature.posY, dz + creature.posZ);
            if ((!nearHome || creature.isWithinHomeDistanceFromPosition(candidate)) && navigator.canEntityStandOnPos(candidate)) {
                double weight = creature.getBlockPathWeight(candidate);
                if (weight > best) {
                    best = weight;
                    result = candidate;
                    found = true;
                }
            }
        }
        return found ? new Vec3d(result) : null;
    }

    @Nullable
    private static BlockPos offset(Random random, int xz, int y, @Nullable Vec3d direction, double angle) {
        if (direction != null && angle < Math.PI) {
            double heading = MathHelper.atan2(direction.z, direction.x) - HALF_PI;
            double turn = heading + (2.0F * random.nextFloat() - 1.0F) * angle;
            double distance = Math.sqrt(random.nextDouble()) * MathHelper.SQRT_2 * xz;
            double x = -distance * Math.sin(turn);
            double z = distance * Math.cos(turn);
            if (Math.abs(x) > xz || Math.abs(z) > xz) {
                return null;
            }
            return new BlockPos(x, random.nextInt(2 * y + 1) - y, z);
        }
        return new BlockPos(random.nextInt(2 * xz + 1) - xz, random.nextInt(2 * y + 1) - y, random.nextInt(2 * xz + 1) - xz);
    }
}
