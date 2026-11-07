package com.exiledradio.updateaquaticbackport.mobs;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.network.NetworkHandler;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Squid squirt ink and flee when hurt (1.13).
 *
 * <p>Current Java's squid, once hurt by something it can blame ({@code getRevengeTarget}), plays its
 * squirt sound and blows 30 ink particles out behind itself; and a flee goal (priority 1, alongside its
 * random swimming) steers it away from that attacker while the attacker is within 10 blocks.
 *
 * <p>1.12.2 has no hook after a hit lands, so a hit that got through to damage marks the squid, and the
 * squid squirts at the start of its next tick if it then has someone to blame - which is when 1.12.2 has
 * set it. The ink particles are drawn by the client from one packet ({@link PacketSquidInk}); the goal is
 * added when the squid joins a world. Every {@link EntitySquid}, other mods' subclasses included.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class SquidInk {

    /** Squid hurt since their last tick. */
    private static final Set<EntitySquid> HURT = Collections.newSetFromMap(new WeakHashMap<EntitySquid, Boolean>());

    /** Vanilla's squid sound volume ({@code EntitySquid.getSoundVolume}). */
    private static final float SOUND_VOLUME = 0.4F;

    /** Particles are sent as far as vanilla sends ordinary particles. */
    private static final double PARTICLE_RANGE = 32.0D;

    private SquidInk() {
    }

    private static boolean on() {
        return MobsContent.enabled() && ModConfig.mobs.squidInk;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (entity.world.isRemote || !(entity instanceof EntitySquid) || !on()) {
            return;
        }
        EntitySquid squid = (EntitySquid) entity;
        for (EntityAITasks.EntityAITaskEntry entry : squid.tasks.taskEntries) {
            if (entry.action instanceof EntityAISquidFlee) {
                return;
            }
        }
        squid.tasks.addTask(1, new EntityAISquidFlee(squid));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDamage(LivingDamageEvent event) {
        if (event.getEntityLiving() instanceof EntitySquid && !event.getEntityLiving().world.isRemote && on()) {
            HURT.add((EntitySquid) event.getEntityLiving());
        }
    }

    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        if (!(entity instanceof EntitySquid) || entity.world.isRemote || HURT.isEmpty() || !HURT.remove(entity)) {
            return;
        }
        if (entity.isEntityAlive() && entity.getRevengeTarget() != null) {
            squirt((EntitySquid) entity);
        }
    }

    /** Current Java's {@code Squid.spawnInk}: the sound, and the particles drawn by nearby clients. */
    public static void squirt(EntitySquid squid) {
        if (!squid.isSilent()) {
            squid.world.playSound(null, squid.posX, squid.posY, squid.posZ, MobsContent.SQUID_SQUIRT,
                    squid.getSoundCategory(), SOUND_VOLUME, pitch(squid));
        }
        NetworkHandler.channel.sendToAllAround(new PacketSquidInk(squid), new NetworkRegistry.TargetPoint(
                squid.dimension, squid.posX, squid.posY, squid.posZ, PARTICLE_RANGE));
    }

    /** 1.12.2's living sound pitch ({@code EntityLivingBase.getSoundPitch}). */
    private static float pitch(EntitySquid squid) {
        float spread = (squid.getRNG().nextFloat() - squid.getRNG().nextFloat()) * 0.2F;
        return squid.isChild() ? spread + 1.5F : spread + 1.0F;
    }

    /** For the self-test: whether a squid is waiting to squirt. */
    public static boolean pending(EntitySquid squid) {
        return HURT.contains(squid);
    }

    /** @return true if this squid's goals include the flee goal */
    public static boolean flees(EntitySquid squid) {
        for (EntityAITasks.EntityAITaskEntry entry : squid.tasks.taskEntries) {
            if (entry.action instanceof EntityAISquidFlee) {
                return true;
            }
        }
        return false;
    }
}
