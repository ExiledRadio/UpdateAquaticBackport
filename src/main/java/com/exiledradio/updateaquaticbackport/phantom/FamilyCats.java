package com.exiledradio.updateaquaticbackport.phantom;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.init.SoundEvents;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The cats a phantom fears: 1.12.2's tamed ocelots, and Village &amp; Pillage's cats when that mod is installed.
 *
 * <h2>Finding them</h2>
 * Village &amp; Pillage's cat is found by its registry name ({@code villagerbackport:cat}), never by linking
 * its class. A tamed ocelot counts with or without that mod: Village &amp; Pillage turns tamed ocelots into
 * cats at the end of the tick they join (or never, with its {@code convertTamedOcelots} off), and until then
 * the ocelot is the cat.
 *
 * <h2>Hissing</h2>
 * 1.14.4's cat hisses with its own volume and pitch. Village &amp; Pillage's cat has 1.14's {@code hiss()},
 * called by name. A tamed ocelot plays {@code entity.cat.hiss} at the ocelot's volume (0.4) and the usual
 * living-entity pitch, as a 1.14 cat would.
 *
 * <h2>Village &amp; Pillage's own phantom fear</h2>
 * Village &amp; Pillage makes any mob registered as {@code phantom} fear its cats, for phantoms that have no
 * cat check of their own: every 10 ticks, from {@link LivingEvent.LivingUpdateEvent}, a phantom with a target
 * and a cat within 16 blocks loses its target and is pushed upward, and the cats hiss. This phantom already
 * does 1.14.4's check (in its swoop, every 20 ticks, no push), so with both it would break off while circling
 * too, rise, and the cats would hiss twice. To keep 1.14.4's behavior, on exactly the ticks that handler
 * would act on this phantom, its target reads as {@code null} from the start of the update event to its end
 * (highest to lowest priority); its own goals, which run after the event, see the target as usual.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class FamilyCats {

    private static final ResourceLocation CAT = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "cat");
    /** Village &amp; Pillage's handler acts when {@code ticksExisted} is a multiple of this. */
    private static final int OWNER_FEAR_INTERVAL = 10;
    /** A tamed ocelot's volume ({@code EntityOcelot.getSoundVolume}); 1.14's cat has the same. */
    private static final float OCELOT_VOLUME = 0.4F;

    private static boolean looked;
    @Nullable
    private static Class<? extends Entity> catClass;
    @Nullable
    private static Method hiss;
    @Nullable
    private static Field ownerCats;
    @Nullable
    private static Field ownerPhantomsFearCats;

    private FamilyCats() {
    }

    /** @return Village &amp; Pillage's cat class, or null if that mod is absent */
    @Nullable
    static Class<? extends Entity> ownerCat() {
        if (!looked) {
            looked = true;
            if (Owners.present(Owners.VILLAGE_AND_PILLAGE)) {
                Class<? extends Entity> c = EntityList.getClass(CAT);
                if (c != null && EntityLiving.class.isAssignableFrom(c)) {
                    catClass = c;
                    try {
                        hiss = c.getMethod("hiss");
                    } catch (NoSuchMethodException e) {
                        hiss = null;
                    }
                }
                try {
                    Class<?> config = Class.forName("com.exiledradio.villagerbackport.ModConfig");
                    ownerCats = config.getField("cats");
                    ownerPhantomsFearCats = ownerCats.getType().getField("phantomsFearCats");
                } catch (ReflectiveOperationException | RuntimeException e) {
                    ownerCats = null;
                    ownerPhantomsFearCats = null;
                }
            }
        }
        return catClass;
    }

    /** @return the living cats in the box: Village &amp; Pillage's cats and tamed ocelots */
    static List<EntityLiving> catsIn(EntityLivingBase around, AxisAlignedBB box) {
        List<EntityLiving> cats = new ArrayList<EntityLiving>();
        Class<? extends Entity> owner = ownerCat();
        if (owner != null) {
            for (Entity cat : around.world.getEntitiesWithinAABB(owner, box)) {
                if (cat.isEntityAlive()) {
                    cats.add((EntityLiving) cat);
                }
            }
        }
        for (EntityOcelot ocelot : around.world.getEntitiesWithinAABB(EntityOcelot.class, box)) {
            if (ocelot.isEntityAlive() && ocelot.isTamed()) {
                cats.add(ocelot);
            }
        }
        return cats;
    }

    /** 1.14.4's {@code CatEntity.hiss}. */
    static void hiss(EntityLiving cat) {
        if (hiss != null && catClass != null && catClass.isInstance(cat)) {
            try {
                hiss.invoke(cat);
                return;
            } catch (ReflectiveOperationException | RuntimeException e) {
                hiss = null;
            }
        }
        Random r = cat.getRNG();
        float pitch = (r.nextFloat() - r.nextFloat()) * 0.2F + (cat.isChild() ? 1.5F : 1.0F);
        cat.playSound(SoundEvents.ENTITY_CAT_HISS, OCELOT_VOLUME, pitch);
    }

    /** @return true if Village &amp; Pillage's own phantom fear is switched on (its default when unreadable) */
    private static boolean ownerFearOn() {
        if (ownerCats == null || ownerPhantomsFearCats == null) {
            return true;
        }
        try {
            return ownerPhantomsFearCats.getBoolean(ownerCats.get(null));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return true;
        }
    }

    /** @return true if Village &amp; Pillage's phantom fear would act on this phantom in this update event */
    static boolean ownerFearWouldAct(EntityPhantom phantom) {
        Class<? extends Entity> owner = ownerCat();
        return owner != null
                && phantom.ticksExisted % OWNER_FEAR_INTERVAL == 0
                && phantom.realAttackTarget() != null
                && ownerFearOn()
                && !phantom.world.getEntitiesWithinAABB(owner, phantom.getEntityBoundingBox().grow(EntityPhantom.CAT_RANGE)).isEmpty();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void beforeUpdateHandlers(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityPhantom && !event.getEntityLiving().world.isRemote) {
            EntityPhantom phantom = (EntityPhantom) event.getEntityLiving();
            phantom.hideTargetFromOthers(ownerFearWouldAct(phantom));
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void afterUpdateHandlers(LivingEvent.LivingUpdateEvent event) {
        if (event.getEntityLiving() instanceof EntityPhantom) {
            ((EntityPhantom) event.getEntityLiving()).hideTargetFromOthers(false);
        }
    }
}
