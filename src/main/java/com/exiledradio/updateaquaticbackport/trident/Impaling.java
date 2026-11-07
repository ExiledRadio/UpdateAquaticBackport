package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Impaling: 2.5 more damage per level against aquatic mobs, as current Java (Bedrock's "anything in
 * water" rule is not Java's).
 *
 * <p>1.12.2 has no aquatic creature attribute, so "aquatic" is: any water mob (squid and every mod's
 * fish that extend vanilla's water mob), guardians and elder guardians, a mob whose creature attribute
 * another mod named {@code WATER}, and the entity ids in {@code trident.impalingMobs} (this mod's fish,
 * dolphins and turtles by default).
 *
 * <p>Thrown, the trident adds the bonus itself. In melee the bonus is added to the hit as vanilla adds
 * Smite's: scaled by the attack cooldown, before armor, with the enchanted-hit particles. The cooldown is
 * read when the attack starts, because the player's swing resets it before the damage is dealt.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class Impaling {

    /** 1.14.4: 2.5 per level against aquatic mobs. */
    public static final float PER_LEVEL = 2.5F;

    /** player -> attack strength at the start of their current melee hit */
    private static final Map<EntityPlayer, Float> STRENGTH = new WeakHashMap<EntityPlayer, Float>();

    private Impaling() {
    }

    /** @return the extra damage a trident stack deals to this target */
    public static float bonus(ItemStack weapon, EntityLivingBase target) {
        int level = TridentContent.impalingLevel(weapon);
        return level > 0 && isAquatic(target) ? level * PER_LEVEL : 0.0F;
    }

    public static boolean isAquatic(Entity entity) {
        if (entity instanceof EntityWaterMob || entity instanceof EntityGuardian) {
            return true;
        }
        if (entity instanceof EntityLivingBase && "WATER".equals(((EntityLivingBase) entity).getCreatureAttribute().name())) {
            return true;
        }
        ResourceLocation id = EntityList.getKey(entity);
        if (id != null) {
            String name = id.toString();
            for (String listed : ModConfig.trident.impalingMobs) {
                if (name.equals(listed.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (TridentContent.enabled() && !event.getEntityPlayer().world.isRemote) {
            synchronized (STRENGTH) {
                STRENGTH.put(event.getEntityPlayer(), event.getEntityPlayer().getCooledAttackStrength(0.5F));
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onHurt(LivingHurtEvent event) {
        if (!TridentContent.enabled()) {
            return;
        }
        Entity attacker = event.getSource().getTrueSource();
        // Melee only: a player's or mob's own hit (thorns has the same source entity, but its own type).
        String type = event.getSource().getDamageType();
        if (!(attacker instanceof EntityLivingBase) || event.getSource().getImmediateSource() != attacker
                || !("player".equals(type) || "mob".equals(type))) {
            return;
        }
        EntityLivingBase living = (EntityLivingBase) attacker;
        float bonus = bonus(living.getHeldItemMainhand(), event.getEntityLiving());
        if (bonus <= 0.0F) {
            return;
        }
        if (living instanceof EntityPlayer) {
            Float strength;
            synchronized (STRENGTH) {
                strength = STRENGTH.remove(living);
            }
            bonus *= strength == null ? 1.0F : strength;
            ((EntityPlayer) living).onEnchantmentCritical(event.getEntityLiving());
        }
        event.setAmount(event.getAmount() + bonus);
    }
}
