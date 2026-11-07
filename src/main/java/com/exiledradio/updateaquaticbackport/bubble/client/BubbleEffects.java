package com.exiledradio.updateaquaticbackport.bubble.client;

import com.exiledradio.updateaquaticbackport.bubble.BubbleContent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.entity.Entity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Random;

/**
 * 1.14.4's {@code BubbleColumnBlock.animateTick}: bubbles rising in an upward column and spiralling down a
 * whirlpool, and the ambient flow and whirl sounds, 1 time in 200.
 *
 * <p>Particles follow the particle setting as 1.14.4's "optional" particles do: none on Minimal (one in
 * ten let through as Decreased), a third fewer on Decreased, none beyond 32 blocks.
 */
@SideOnly(Side.CLIENT)
public final class BubbleEffects {

    private BubbleEffects() {
    }

    public static void animate(World world, BlockPos pos, boolean drag, Random rand) {
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        if (drag) {
            add(world, new ParticleCurrentDown(world, x + 0.5, y + 0.8, z), x, y, z);
            if (rand.nextInt(200) == 0) {
                world.playSound(x, y, z, BubbleContent.WHIRLPOOL_AMBIENT, SoundCategory.BLOCKS,
                        0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        } else {
            add(world, new ParticleBubbleColumnUp(world, x + 0.5, y, z + 0.5, 0.0, 0.04, 0.0), x, y, z);
            add(world, new ParticleBubbleColumnUp(world, x + rand.nextFloat(), y + rand.nextFloat(), z + rand.nextFloat(), 0.0, 0.04, 0.0), x, y, z);
            if (rand.nextInt(200) == 0) {
                world.playSound(x, y, z, BubbleContent.UPWARDS_AMBIENT, SoundCategory.BLOCKS,
                        0.2F + rand.nextFloat() * 0.2F, 0.9F + rand.nextFloat() * 0.15F, false);
            }
        }
    }

    private static void add(World world, Particle particle, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        Entity view = mc.getRenderViewEntity();
        if (view == null || mc.effectRenderer == null) {
            return;
        }
        int level = mc.gameSettings.particleSetting;
        if (level == 2 && world.rand.nextInt(10) == 0) {
            level = 1;
        }
        if (level == 1 && world.rand.nextInt(3) == 0) {
            level = 2;
        }
        if (level == 2) {
            return;
        }
        if (view.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) > 1024.0) {
            return;
        }
        mc.effectRenderer.addEffect(particle);
    }
}
