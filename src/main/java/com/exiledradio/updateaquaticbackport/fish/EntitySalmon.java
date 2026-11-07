package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Salmon: schools of up to 5; drops raw salmon (raw fish metadata 1), cooked when burning. */
public class EntitySalmon extends EntityAbstractGroupFish {

    public EntitySalmon(World world) {
        super(world);
        this.setSize(0.7F, 0.4F);
    }

    @Override
    public int getMaxSchoolSize() {
        return 5;
    }

    @Override
    public int minGroup() {
        return 1;
    }

    @Override
    public int maxGroup() {
        return 5;
    }

    @Override
    protected ItemStack bucket() {
        return FishContent.stack(FishContent.salmonBucket);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return FishContent.SALMON_LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FishContent.SALMON_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FishContent.SALMON_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FishContent.SALMON_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return FishContent.SALMON_FLOP;
    }
}
