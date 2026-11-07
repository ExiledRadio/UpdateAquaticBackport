package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * 1.14.4's {@code SquidEntity.FleeGoal}: while the squid is in water and whoever last hurt it is within 10
 * blocks, it swims straight away from them, at up to three times its random swimming speed when they are
 * close, slower as they get further, and never up out of the water.
 *
 * <p>No mutex, as in 1.14.4: it runs alongside the squid's random swimming and, being started after it,
 * sets the swimming direction last each tick. It only steers; it changes no goal list.
 */
public class EntityAISquidFlee extends EntityAIBase {

    private final EntitySquid squid;
    private int tickCounter;

    public EntityAISquidFlee(EntitySquid squid) {
        this.squid = squid;
        this.setMutexBits(0);
    }

    @Override
    public boolean shouldExecute() {
        EntityLivingBase attacker = this.squid.getRevengeTarget();
        return this.squid.isInWater() && attacker != null && this.squid.getDistanceSq(attacker) < 100.0D;
    }

    @Override
    public void startExecuting() {
        this.tickCounter = 0;
    }

    @Override
    public void updateTask() {
        ++this.tickCounter;
        EntityLivingBase attacker = this.squid.getRevengeTarget();
        if (attacker == null) {
            return;
        }
        Vec3d away = new Vec3d(this.squid.posX - attacker.posX, this.squid.posY - attacker.posY, this.squid.posZ - attacker.posZ);
        BlockPos ahead = new BlockPos(this.squid.posX + away.x, this.squid.posY + away.y, this.squid.posZ + away.z);
        IBlockState state = this.squid.world.getBlockState(ahead);
        boolean air = state.getBlock().isAir(state, this.squid.world, ahead);
        if (Waterlogging.hasWater(this.squid.world, ahead) || air) {
            double distance = away.length();
            if (distance > 0.0D) {
                // 1.14.4 normalizes here and throws the result away, so the speed below scales the full
                // offset; kept as it is, since that is how fast a squid flees in the game.
                float speed = 3.0F;
                if (distance > 5.0D) {
                    speed = (float) ((double) speed - (distance - 5.0D) / 5.0D);
                }
                if (speed > 0.0F) {
                    away = away.scale((double) speed);
                }
            }
            if (air) {
                away = away.subtract(0.0D, away.y, 0.0D);
            }
            this.squid.setMovementVector((float) away.x / 20.0F, (float) away.y / 20.0F, (float) away.z / 20.0F);
        }
        // 1.14.4 also adds a bubble particle every 10 ticks, but from the server, where it is never seen.
    }
}
