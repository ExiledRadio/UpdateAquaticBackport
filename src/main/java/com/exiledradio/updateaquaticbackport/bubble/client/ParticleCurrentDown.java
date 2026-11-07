package com.exiledradio.updateaquaticbackport.bubble.client;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 1.14.4's {@code CurrentDownParticle}: a bubble that spirals down a whirlpool for 30 to 90 ticks and pops
 * when it leaves the water or lands. Drawn with 1.12.2's bubble (particle sheet index 32).
 */
@SideOnly(Side.CLIENT)
public class ParticleCurrentDown extends Particle {

    private float spin;

    public ParticleCurrentDown(World world, double x, double y, double z) {
        super(world, x, y, z);
        this.particleRed = 1.0F;
        this.particleGreen = 1.0F;
        this.particleBlue = 1.0F;
        this.setParticleTextureIndex(32);
        this.particleMaxAge = (int) (Math.random() * 60.0) + 30;
        this.canCollide = false;
        this.motionX = 0.0;
        this.motionY = -0.05;
        this.motionZ = 0.0;
        this.setSize(0.02F, 0.02F);
        this.particleScale *= this.rand.nextFloat() * 0.6F + 0.2F;
        this.particleGravity = 0.002F;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
            return;
        }
        this.motionX += 0.6F * MathHelper.cos(this.spin);
        this.motionZ += 0.6F * MathHelper.sin(this.spin);
        this.motionX *= 0.07;
        this.motionZ *= 0.07;
        this.move(this.motionX, this.motionY, this.motionZ);
        if (!Waterlogging.hasWater(this.world, new BlockPos(this.posX, this.posY, this.posZ)) || this.onGround) {
            this.setExpired();
        }
        this.spin += 0.08F;
    }
}
