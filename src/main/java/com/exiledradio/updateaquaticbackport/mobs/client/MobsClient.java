package com.exiledradio.updateaquaticbackport.mobs.client;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.mobs.MobsContent;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntitySkeletonHorse;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

/**
 * The stream's client side. FML loads this class only on a client.
 *
 * <ul>
 *   <li>Squid ink: the 30 particles of current Java's {@code Squid.spawnInk}, from {@code PacketSquidInk}.</li>
 *   <li>Skeleton horses' underwater sounds (1.13). A 1.12.2 entity's sounds reach the client as plain
 *       positioned sounds, with nothing saying whose they are, and the server cannot change what a vanilla
 *       horse plays without a patch. So the client swaps them as they arrive: a sound played exactly where a
 *       skeleton horse is, and where no other mob is closer, is that horse's. Its ambient sound with its
 *       eyes under water becomes {@code ambient_water}; its swimming sound becomes 1.14.4's
 *       {@code SkeletonHorseEntity.getSwimSound} choice (steps and gallops on the bottom, swimming off
 *       it) at that method's volumes.</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID, value = Side.CLIENT)
public final class MobsClient {

    /**
     * How far from a sound's position its horse may be: positions are sent to an eighth of a block, and a
     * horse someone else rides is drawn a little behind where the server has it.
     */
    private static final double MATCH_RADIUS = 1.5D;

    private static final Random RANDOM = new Random();

    /** 1.14.4's {@code gallopTime} for the water sounds, per horse, counted here. */
    private static final Map<EntitySkeletonHorse, Integer> GALLOP = new WeakHashMap<EntitySkeletonHorse, Integer>();

    private MobsClient() {
    }

    // ---------------------------------------------------------------------------------------------- squid ink

    /** Called from the packet's handler, on the network thread. */
    public static void squidInk(final double x, final double y, final double z, final float pitch, final float yaw) {
        Minecraft.getMinecraft().addScheduledTask(new Runnable() {
            @Override
            public void run() {
                spawnInk(x, y, z, pitch, yaw);
            }
        });
    }

    static void spawnInk(double x, double y, double z, float pitch, float yaw) {
        Minecraft mc = Minecraft.getMinecraft();
        WorldClient world = mc.world;
        if (world == null) {
            return;
        }
        Vec3d origin = rotate(new Vec3d(0.0D, -1.0D, 0.0D), pitch, yaw).add(x, y, z);
        for (int i = 0; i < 30; ++i) {
            Vec3d direction = rotate(new Vec3d((double) RANDOM.nextFloat() * 0.6D - 0.3D, -1.0D,
                    (double) RANDOM.nextFloat() * 0.6D - 0.3D), pitch, yaw);
            // The server sends each particle with a count of 0 and a speed of 0.1: that speed times this.
            Vec3d motion = direction.scale(0.3D + (double) (RANDOM.nextFloat() * 2.0F)).scale((double) 0.1F);
            mc.effectRenderer.addEffect(new ParticleSquidInk(world, origin.x, origin.y + 0.5D, origin.z,
                    motion.x, motion.y, motion.z));
        }
    }

    /** Current Java's {@code Squid.rotateVector}: by the body's pitch, then its yaw. */
    private static Vec3d rotate(Vec3d vector, float pitch, float yaw) {
        return vector.rotatePitch(pitch * ((float) Math.PI / 180F)).rotateYaw(-yaw * ((float) Math.PI / 180F));
    }

    // ------------------------------------------------------------------------------ skeleton horse sounds

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        ISound sound = event.getResultSound();
        if (!(sound instanceof PositionedSound) || !MobsContent.enabled() || !ModConfig.mobs.skeletonHorsesUnderwater) {
            return;
        }
        ResourceLocation name = sound.getSoundLocation();
        boolean ambient = name.equals(SoundEvents.ENTITY_SKELETON_HORSE_AMBIENT.getRegistryName());
        boolean swim = !ambient && name.equals(SoundEvents.ENTITY_GENERIC_SWIM.getRegistryName());
        if (!ambient && !swim) {
            return;
        }
        EntitySkeletonHorse horse = horseAt(sound.getXPosF(), sound.getYPosF(), sound.getZPosF());
        if (horse == null) {
            return;
        }
        float volume = volume((PositionedSound) sound);
        float pitch = pitch((PositionedSound) sound);
        if (ambient) {
            if (horse.isInsideOfMaterial(Material.WATER)) {
                event.setResultSound(replace(sound, MobsContent.SKELETON_HORSE_AMBIENT_WATER, volume, pitch));
            }
            return;
        }
        // 1.14.4's SkeletonHorseEntity.playSwimSound and getSwimSound.
        if (!horse.isBeingRidden()) {
            GALLOP.remove(horse);
        }
        SoundEvent water = MobsContent.SKELETON_HORSE_SWIM;
        float waterVolume = Math.min(0.1F, volume * 25.0F);
        if (horse.onGround) {
            waterVolume = 0.3F;
            if (!horse.isBeingRidden()) {
                water = MobsContent.SKELETON_HORSE_STEP_WATER;
            } else {
                Integer before = GALLOP.get(horse);
                int gallop = (before == null ? 0 : before) + 1;
                GALLOP.put(horse, gallop);
                if (gallop > 5 && gallop % 3 == 0) {
                    water = MobsContent.SKELETON_HORSE_GALLOP_WATER;
                } else if (gallop <= 5) {
                    water = MobsContent.SKELETON_HORSE_STEP_WATER;
                }
            }
        }
        event.setResultSound(replace(sound, water, waterVolume, pitch));
    }

    /** @return the skeleton horse a sound at this position belongs to, or null */
    private static EntitySkeletonHorse horseAt(double x, double y, double z) {
        WorldClient world = Minecraft.getMinecraft().world;
        if (world == null) {
            return null;
        }
        AxisAlignedBB box = new AxisAlignedBB(x - MATCH_RADIUS, y - MATCH_RADIUS, z - MATCH_RADIUS,
                x + MATCH_RADIUS, y + MATCH_RADIUS, z + MATCH_RADIUS);
        EntityLivingBase nearest = null;
        double best = Double.MAX_VALUE;
        for (EntityLivingBase entity : world.getEntitiesWithinAABB(EntityLivingBase.class, box)) {
            if (entity.isRiding()) {
                // A rider sits right above its horse and makes no swimming sounds of its own.
                continue;
            }
            double d = entity.getDistanceSq(x, y, z);
            if (d < best) {
                best = d;
                nearest = entity;
            }
        }
        return nearest instanceof EntitySkeletonHorse ? (EntitySkeletonHorse) nearest : null;
    }

    private static ISound replace(ISound sound, SoundEvent event, float volume, float pitch) {
        return new PositionedSoundRecord(event.getSoundName(), sound.getCategory(), volume, pitch, false, 0,
                sound.getAttenuationType(), sound.getXPosF(), sound.getYPosF(), sound.getZPosF());
    }

    private static float volume(PositionedSound sound) {
        return ReflectionHelper.<Float, PositionedSound>getPrivateValue(PositionedSound.class, sound, "volume", "field_147662_b");
    }

    private static float pitch(PositionedSound sound) {
        return ReflectionHelper.<Float, PositionedSound>getPrivateValue(PositionedSound.class, sound, "pitch", "field_147663_c");
    }
}
