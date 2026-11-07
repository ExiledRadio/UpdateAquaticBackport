package com.exiledradio.updateaquaticbackport.dolphin.client;

import net.minecraft.client.particle.ParticleSuspendedTown;
import net.minecraft.world.World;

/** 1.14.4's dolphin particle: a suspended-town speck colored (0.3, 0.5, 1.0), partly see-through, half as long-lived. */
final class ParticleDolphin extends ParticleSuspendedTown {

    ParticleDolphin(World world, double x, double y, double z) {
        super(world, x, y, z, 0.0D, 0.0D, 0.0D);
        this.setRBGColorF(0.3F, 0.5F, 1.0F);
        this.setAlphaF(1.0F - world.rand.nextFloat() * 0.7F);
        this.particleMaxAge = this.particleMaxAge / 2;
    }
}
