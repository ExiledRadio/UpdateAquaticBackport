package com.exiledradio.updateaquaticbackport.bubble.client;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14.4's {@code BubbleColumnUpParticle}: a bubble that rises (+0.005 a tick, 0.85 drag) for 40 to 200
 * ticks and pops when it leaves the water. Drawn with 1.12.2's own bubble (particle sheet index 32), the
 * texture 1.13's {@code bubble_column_up} uses too.
 */
@SideOnly(Side.CLIENT)
public class ParticleBubbleColumnUp extends Particle {

    public ParticleBubbleColumnUp(World world, double x, double y, double z, double vx, double vy, double vz) {
        super(world, x, y, z, vx, vy, vz);
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        this.setParticleTextureIndex(32);
        this.setSize(0.02F, 0.02F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.2F;
        this.motionX = vx * 0.2F + (Math.random() * 2.0 - 1.0) * 0.02F;
        this.motionY = vy * 0.2F + (Math.random() * 2.0 - 1.0) * 0.02F;
        this.motionZ = vz * 0.2F + (Math.random() * 2.0 - 1.0) * 0.02F;
        this.particleMaxAge = (int) (40.0 / (Math.random() * 0.8 + 0.2));
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        this.motionY += 0.005;
        if (this.particleMaxAge-- <= 0) {
            this.setExpired();
            return;
        }
        this.move(this.motionX, this.motionY, this.motionZ);
        this.motionX *= 0.85F;
        this.motionY *= 0.85F;
        this.motionZ *= 0.85F;
        if (!Waterlogging.hasWater(this.world, new BlockPos(this.posX, this.posY, this.posZ))) {
            this.setExpired();
        }
    }
}
