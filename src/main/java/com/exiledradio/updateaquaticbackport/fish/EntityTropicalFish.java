package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;

/**
 * Tropical fish: one of 2 shapes x 6 patterns x 15 body colors x 15 pattern colors
 * ({@link TropicalFishVariant}). A wild school shares one look; 9 schools in 10 are one of the 22
 * predefined common fish; the rest are random and spawn alone. The look goes into a bucket and comes back out of it.
 */
public class EntityTropicalFish extends EntityAbstractGroupFish {

    private static final DataParameter<Integer> VARIANT = EntityDataManager.createKey(EntityTropicalFish.class, DataSerializers.VARINT);

    /** False for a fish of a random look: it spawns alone (not saved, as in 1.13). */
    private boolean school = true;

    public EntityTropicalFish(World world) {
        super(world);
        this.setSize(0.5F, 0.4F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(VARIANT, 0);
    }

    public int getVariant() {
        return this.dataManager.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.dataManager.set(VARIANT, variant);
    }

    @Override
    @Nullable
    protected IEntityLivingData prepareSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.prepareSpawn(difficulty, data);
        int variant;
        if (data instanceof TropicalFishData) {
            variant = ((TropicalFishData) data).variant;
        } else if (this.rand.nextFloat() < 0.9F) {
            variant = TropicalFishVariant.PREDEFINED[this.rand.nextInt(TropicalFishVariant.PREDEFINED.length)];
            data = new TropicalFishData(this, variant);
        } else {
            this.school = false;
            variant = TropicalFishVariant.pack(this.rand.nextInt(2), this.rand.nextInt(6), this.rand.nextInt(15), this.rand.nextInt(15));
        }
        this.setVariant(variant);
        return data;
    }

    @Override
    public boolean spawnsAlone() {
        return !this.school;
    }

    @Override
    protected void writeBucketData(ItemStack bucket) {
        super.writeBucketData(bucket);
        bucket.getTagCompound().setInteger("BucketVariantTag", this.getVariant());
    }

    @Override
    protected void readBucketData(NBTTagCompound tag) {
        super.readBucketData(tag);
        if (tag.hasKey("BucketVariantTag", 3)) {
            this.setVariant(tag.getInteger("BucketVariantTag"));
        }
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("Variant", this.getVariant());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setVariant(compound.getInteger("Variant"));
    }

    @Override
    public int minGroup() {
        return 8;
    }

    @Override
    public int maxGroup() {
        return 8;
    }

    @Override
    protected ItemStack bucket() {
        return FishContent.stack(FishContent.tropicalFishBucket);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return FishContent.TROPICAL_FISH_LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FishContent.TROPICAL_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FishContent.TROPICAL_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FishContent.TROPICAL_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return FishContent.TROPICAL_FLOP;
    }

    /** A predefined school's look, shared by the whole group. */
    static final class TropicalFishData extends GroupData {
        final int variant;

        TropicalFishData(EntityTropicalFish leader, int variant) {
            super(leader);
            this.variant = variant;
        }
    }
}
