package com.exiledradio.updateaquaticbackport.dolphin;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;

/**
 * 1.14.4's {@code SwimToTreasureGoal}, with today's target list: a dolphin fed a fish (and with at least
 * 100 air) looks up the nearest shipwreck or ocean ruin within 50 chunks (current Java's
 * {@code dolphin_located} tag; buried treasure is not in it), shows happy particles and swims toward it
 * in hops of up to 16 blocks, until it is within 4 blocks of it across. Nothing found, or no way on, and
 * it forgets the fish.
 */
final class AISwimToTreasure extends EntityAIBase {

    /** 1.14.4's search radius, in chunks. */
    static final int RADIUS_CHUNKS = 50;

    /** Current Java's {@code dolphin_located} structure tag. */
    static final String[] TARGETS = {StructureLocator.OCEAN_RUIN, StructureLocator.SHIPWRECK};

    private final EntityDolphin dolphin;
    private boolean stuck;

    AISwimToTreasure(EntityDolphin dolphin) {
        this.dolphin = dolphin;
        this.setMutexBits(3);
    }

    @Override
    public boolean isInterruptible() {
        return false;
    }

    @Override
    public boolean shouldExecute() {
        return this.dolphin.hasGotFish() && this.dolphin.getAir() >= 100;
    }

    @Override
    public boolean shouldContinueExecuting() {
        return !this.arrived() && !this.stuck && this.dolphin.getAir() >= 100;
    }

    private boolean arrived() {
        BlockPos treasure = this.dolphin.getTreasurePos();
        double dx = treasure.getX() - this.dolphin.posX;
        double dz = treasure.getZ() - this.dolphin.posZ;
        return dx * dx + dz * dz < 16.0D;
    }

    @Override
    public void startExecuting() {
        this.stuck = false;
        this.dolphin.getNavigator().clearPath();
        World world = this.dolphin.world;
        BlockPos found = world instanceof WorldServer
                ? StructureLocator.nearestOf((WorldServer) world, new BlockPos(this.dolphin), RADIUS_CHUNKS, TARGETS)
                : null;
        if (found == null) {
            this.stuck = true;
            return;
        }
        this.dolphin.setTreasurePos(found);
        world.setEntityState(this.dolphin, EntityDolphin.EVENT_HAPPY);
    }

    @Override
    public void resetTask() {
        if (this.arrived() || this.stuck) {
            this.dolphin.setGotFish(false);
        }
    }

    /**
     * 1.14.4's {@code findRandomTargetTowardsScaled}: a random spot up to {@code xz} away (and {@code y} up or
     * down) within {@code angle} of the direction to the target, that the dolphin could be in.
     */
    private Vec3d towardsWithin(int xz, int y, Vec3d target, double angle) {
        double toward = Math.atan2(target.z - this.dolphin.posZ, target.x - this.dolphin.posX) - Math.PI / 2.0D;
        for (int i = 0; i < 10; i++) {
            double a = toward + (2.0D * this.dolphin.getRNG().nextDouble() - 1.0D) * angle;
            double d = Math.sqrt(this.dolphin.getRNG().nextDouble()) * Math.sqrt(2.0D) * xz;
            double dx = -d * Math.sin(a);
            double dz = d * Math.cos(a);
            if (Math.abs(dx) > xz || Math.abs(dz) > xz) {
                continue;
            }
            int dy = this.dolphin.getRNG().nextInt(2 * y + 1) - y;
            BlockPos pos = new BlockPos(this.dolphin.posX + dx, this.dolphin.posY + dy, this.dolphin.posZ + dz);
            if (this.dolphin.getNavigator().canEntityStandOnPos(pos)) {
                return new Vec3d(this.dolphin.posX + dx, this.dolphin.posY + dy, this.dolphin.posZ + dz);
            }
        }
        return null;
    }

    @Override
    public void updateTask() {
        if (this.stuck) {
            return;
        }
        World world = this.dolphin.world;
        DolphinPathNavigator navigator = (DolphinPathNavigator) this.dolphin.getNavigator();
        if (this.dolphin.closeToTarget(navigator.target()) || navigator.noPath()) {
            BlockPos treasure = this.dolphin.getTreasurePos();
            Vec3d toward = new Vec3d(treasure.getX(), treasure.getY(), treasure.getZ());
            Vec3d next = this.towardsWithin(16, 1, toward, Math.PI / 8.0D);
            if (next == null) {
                next = RandomPositionGenerator.findRandomTargetBlockTowards(this.dolphin, 8, 4, toward);
            }
            if (next != null && !DolphinNodeProcessor.isSwimmable(world.getBlockState(new BlockPos(next)))) {
                next = RandomPositionGenerator.findRandomTargetBlockTowards(this.dolphin, 8, 5, toward);
            }
            if (next == null) {
                this.stuck = true;
                return;
            }
            this.dolphin.getLookHelper().setLookPosition(next.x, next.y, next.z,
                    this.dolphin.getHorizontalFaceSpeed() + 20, this.dolphin.getVerticalFaceSpeed());
            navigator.tryMoveToXYZ(next.x, next.y, next.z, 1.3D);
            if (world.rand.nextInt(80) == 0) {
                world.setEntityState(this.dolphin, EntityDolphin.EVENT_HAPPY);
            }
        }
    }
}
