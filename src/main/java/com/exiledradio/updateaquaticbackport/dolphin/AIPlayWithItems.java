package com.exiledradio.updateaquaticbackport.dolphin;

import com.google.common.base.Predicate;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 1.14.4's {@code PlayWithItemsGoal}: a dolphin chases items floating in water within 8 blocks, picks
 * one up (any item, into its mouth; see {@link EntityDolphin#updateEquipmentIfNeeded}), tosses it ahead
 * with 2 seconds before anything can pick it up again, and chases it again. When the game stops the
 * goal while it holds an item it tosses that too and leaves items alone for up to 5 seconds.
 */
final class AIPlayWithItems extends EntityAIBase {

    /** 1.14.4's {@code ITEM_SELECTOR}: can be picked up, alive, in water. */
    static final Predicate<EntityItem> IN_WATER = new Predicate<EntityItem>() {
        @Override
        public boolean apply(@Nullable EntityItem item) {
            return item != null && !item.cannotPickup() && item.isEntityAlive() && item.isInWater();
        }
    };

    /** 1.14.4's pickup delay on a tossed item. */
    static final int TOSS_PICKUP_DELAY = 40;

    private final EntityDolphin dolphin;
    private int cooldownUntil;

    AIPlayWithItems(EntityDolphin dolphin) {
        this.dolphin = dolphin;
    }

    private List<EntityItem> nearby() {
        return this.dolphin.world.getEntitiesWithinAABB(EntityItem.class, this.dolphin.getEntityBoundingBox().grow(8.0D), IN_WATER);
    }

    @Override
    public boolean shouldExecute() {
        if (this.cooldownUntil > this.dolphin.ticksExisted) {
            return false;
        }
        return !this.nearby().isEmpty() || !this.dolphin.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty();
    }

    @Override
    public void startExecuting() {
        List<EntityItem> items = this.nearby();
        if (!items.isEmpty()) {
            this.dolphin.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2F);
            this.dolphin.playSound(DolphinContent.PLAY, 1.0F, 1.0F);
        }
        this.cooldownUntil = 0;
    }

    @Override
    public void resetTask() {
        ItemStack held = this.dolphin.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!held.isEmpty()) {
            this.toss(held);
            this.dolphin.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.cooldownUntil = this.dolphin.ticksExisted + this.dolphin.getRNG().nextInt(100);
        }
    }

    @Override
    public void updateTask() {
        List<EntityItem> items = this.nearby();
        ItemStack held = this.dolphin.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        if (!held.isEmpty()) {
            this.toss(held);
            this.dolphin.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, ItemStack.EMPTY);
        } else if (!items.isEmpty()) {
            this.dolphin.getNavigator().tryMoveToEntityLiving(items.get(0), 1.2F);
        }
    }

    /** Tosses a stack forward along the dolphin's look, as 1.14.4's {@code func_220810_a}. */
    private void toss(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        EntityDolphin d = this.dolphin;
        double y = d.posY - 0.3F + d.getEyeHeight();
        EntityItem item = new EntityItem(d.world, d.posX, y, d.posZ, stack);
        item.setPickupDelay(TOSS_PICKUP_DELAY);
        item.setThrower(d.getName());
        float spin = d.getRNG().nextFloat() * ((float) Math.PI * 2.0F);
        float wobble = 0.02F * d.getRNG().nextFloat();
        float yaw = d.rotationYaw * 0.017453292F;
        float pitch = d.rotationPitch * 0.017453292F;
        item.motionX = 0.3F * -MathHelper.sin(yaw) * MathHelper.cos(pitch) + MathHelper.cos(spin) * wobble;
        item.motionY = 0.3F * MathHelper.sin(pitch) * 1.5F;
        item.motionZ = 0.3F * MathHelper.cos(yaw) * MathHelper.cos(pitch) + MathHelper.sin(spin) * wobble;
        d.world.spawnEntity(item);
    }
}
