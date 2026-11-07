package com.exiledradio.updateaquaticbackport.mobs.client;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.particle.Particle;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * 1.14.4's {@code SquidInkParticle}: a black puff on vanilla's generic smoke sprites (1.12.2's particle
 * sheet indices 0-7, which 1.14's {@code generic_0}..{@code generic_7} are), shrinking through them from
 * the largest, fading out over its second half, drifting with its starting speed, sinking only in air.
 * Always drawn fully bright.
 */
public class ParticleSquidInk extends Particle {

    /** 1.14.4's particle scale; 1.12.2 draws {@code 0.1 * particleScale}, so it is set ten times larger. */
    private static final float SCALE = 0.5F;

    public ParticleSquidInk(World world, double x, double y, double z, double motionX, double motionY, double motionZ) {
        super(world, x, y, z);
        this.particleScale = SCALE * 10.0F;
        this.setRBGColorF(0.0F, 0.0F, 0.0F);
        this.particleAlpha = 1.0F;
        this.particleMaxAge = (int) ((double) (SCALE * 12.0F) / (Math.random() * (double) 0.8F + (double) 0.2F));
        this.canCollide = false;
        this.motionX = motionX;
        this.motionY = motionY;
        this.motionZ = motionZ;
        this.selectSprite();
    }

    /** 1.14.4's {@code selectSpriteWithAge} over {@code generic_7}..{@code generic_0}. */
    private void selectSprite() {
        this.setParticleTextureIndex(7 - this.particleAge * 7 / Math.max(1, this.particleMaxAge));
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
        this.selectSprite();
        if (this.particleAge > this.particleMaxAge / 2) {
            this.setAlphaF(1.0F - ((float) this.particleAge - (float) (this.particleMaxAge / 2)) / (float) this.particleMaxAge);
        }
        this.move(this.motionX, this.motionY, this.motionZ);
        BlockPos pos = new BlockPos(this.posX, this.posY, this.posZ);
        IBlockState state = this.world.getBlockState(pos);
        if (state.getBlock().isAir(state, this.world, pos)) {
            this.motionY -= (double) 0.008F;
        }
        this.motionX *= (double) 0.92F;
        this.motionY *= (double) 0.92F;
        this.motionZ *= (double) 0.92F;
        if (this.onGround) {
            this.motionX *= (double) 0.7F;
            this.motionZ *= (double) 0.7F;
        }
    }

    @Override
    public int getBrightnessForRender(float partialTick) {
        return 15728880;
    }
}
