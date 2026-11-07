package com.exiledradio.updateaquaticbackport.conduit.client;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.World;

/**
 * 1.14.4's nautilus particle: the enchanting table's "fly towards a position" particle with 1.18's
 * nautilus sprite. It starts at {@code (x, y, z) + motion} and drifts to {@code (x, y, z)}, sinking a
 * little at the end. 1.12.2's own enchanting particle is the same motion, but it draws from the old
 * particle sheet and cannot take a sprite, so the motion is copied here.
 */
public class ParticleNautilus extends Particle {

    private final double coordX;
    private final double coordY;
    private final double coordZ;

    public ParticleNautilus(World world, double x, double y, double z, double dx, double dy, double dz, TextureAtlasSprite sprite) {
        super(world, x, y, z, dx, dy, dz);
        this.motionX = dx;
        this.motionY = dy;
        this.motionZ = dz;
        this.coordX = x;
        this.coordY = y;
        this.coordZ = z;
        this.prevPosX = x + dx;
        this.prevPosY = y + dy;
        this.prevPosZ = z + dz;
        this.posX = this.prevPosX;
        this.posY = this.prevPosY;
        this.posZ = this.prevPosZ;
        float shade = this.rand.nextFloat() * 0.6F + 0.4F;
        this.particleScale = this.rand.nextFloat() * 0.5F + 0.2F;
        this.particleRed = 0.9F * shade;
        this.particleGreen = 0.9F * shade;
        this.particleBlue = shade;
        this.canCollide = false;
        this.particleMaxAge = (int) (Math.random() * 10.0D) + 30;
        this.setParticleTexture(sprite);
    }

    /** The block atlas, where the sprite is stitched. */
    @Override
    public int getFXLayer() {
        return 1;
    }

    @Override
    public void move(double x, double y, double z) {
        this.setBoundingBox(this.getBoundingBox().offset(x, y, z));
        this.resetPositionToBB();
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        int i = super.getBrightnessForRender(partialTick);
        float f = (float) this.particleAge / (float) this.particleMaxAge;
        f = f * f;
        f = f * f;
        int sky = i & 255;
        int block = (i >> 16 & 255) + (int) (f * 15.0F * 16.0F);
        return sky | Math.min(block, 240) << 16;
    }

    @Override
    public void onUpdate() {
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
        float f = 1.0F - (float) this.particleAge / (float) this.particleMaxAge;
        float f1 = 1.0F - f;
        f1 = f1 * f1;
        f1 = f1 * f1;
        this.posX = this.coordX + this.motionX * f;
        this.posY = this.coordY + this.motionY * f - f1 * 1.2F;
        this.posZ = this.coordZ + this.motionZ * f;
        if (this.particleAge++ >= this.particleMaxAge) {
            this.setExpired();
        }
    }
}
