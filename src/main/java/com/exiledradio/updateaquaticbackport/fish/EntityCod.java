package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/** Cod: schools of up to 8; drops raw cod (vanilla's raw fish, metadata 0), cooked when burning. */
public class EntityCod extends EntityAbstractGroupFish {

    public EntityCod(World world) {
        super(world);
        this.setSize(0.5F, 0.3F);
    }

    @Override
    public int minGroup() {
        return 3;
    }

    @Override
    public int maxGroup() {
        return 6;
    }

    @Override
    protected ItemStack bucket() {
        return FishContent.stack(FishContent.codBucket);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return FishContent.COD_LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FishContent.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FishContent.COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FishContent.COD_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return FishContent.COD_FLOP;
    }
}
