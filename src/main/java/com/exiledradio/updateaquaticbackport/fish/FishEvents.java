package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.PotionEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Changes to vanilla's fish items and fishing.
 *
 * <ul>
 *   <li>Eating a pufferfish gives current Java's effects: Poison II for 60 seconds (1.12.2: IV) and
 *       Nausea I for 15 seconds (1.12.2: II); Hunger III for 15 seconds is unchanged. The item's own code
 *       adds 1.12.2's; while a pufferfish is being eaten, those two are swapped for today's as they are
 *       applied.</li>
 *   <li>Every item reeled in fires the {@code fishing_rod_hooked} trigger (<i>Fishy Business</i>).</li>
 * </ul>
 * The raw fish names (Raw Cod, Cooked Cod, Tropical Fish) are only language lines.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class FishEvents {

    private FishEvents() {
    }

    private static boolean eatingPufferfish(EntityLivingBase entity) {
        if (!entity.isHandActive()) {
            return false;
        }
        ItemStack using = entity.getActiveItemStack();
        return using.getItem() == Items.FISH
                && ItemFishFood.FishType.byItemStack(using) == ItemFishFood.FishType.PUFFERFISH;
    }

    @SubscribeEvent
    public static void potionApplicable(PotionEvent.PotionApplicableEvent event) {
        if (!FishContent.enabled()) {
            return;
        }
        PotionEffect effect = event.getPotionEffect();
        EntityLivingBase entity = event.getEntityLiving();
        PotionEffect today = null;
        if (effect.getPotion() == MobEffects.POISON && effect.getAmplifier() == 3 && effect.getDuration() == 1200) {
            today = new PotionEffect(MobEffects.POISON, 1200, 1);
        } else if (effect.getPotion() == MobEffects.NAUSEA && effect.getAmplifier() == 1 && effect.getDuration() == 300) {
            today = new PotionEffect(MobEffects.NAUSEA, 300, 0);
        }
        if (today != null && eatingPufferfish(entity)) {
            event.setResult(Event.Result.DENY);
            entity.addPotionEffect(today);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void fished(ItemFishedEvent event) {
        if (!FishContent.enabled() || !(event.getEntityPlayer() instanceof EntityPlayerMP)
                || event.getEntityPlayer() instanceof FakePlayer) {
            return;
        }
        for (ItemStack stack : event.getDrops()) {
            FishTriggers.FISHING_ROD_HOOKED.trigger((EntityPlayerMP) event.getEntityPlayer(), stack);
        }
    }
}
