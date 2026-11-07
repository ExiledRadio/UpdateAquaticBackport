package com.exiledradio.updateaquaticbackport.fish;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.EntityWaterMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Pufferfish: puffs up in three stages when something it fears comes within 2 blocks, stings whatever
 * touches it while puffed, and deflates when left alone.
 *
 * <ul>
 *   <li>Fears: players in survival or adventure, and every mob except current Java's
 *       {@code not_scary_for_pufferfish} ones (turtles, guardians, cod, pufferfish, salmon, squid, tropical
 *       fish). 1.12.2 has no aquatic mob type, so those are matched by class and by name.</li>
 *   <li>Puffing: half puffed at once, fully after 2 seconds more; once alone, half after 3 seconds and
 *       deflated after 5 (1.13's timers).</li>
 *   <li>Sting: 1 + puff stage damage (difficulty-scaled for players) and Poison for 3 seconds per stage;
 *       the player hears the sting sound.</li>
 *   <li>Size: 0.35, 0.49 and 0.7 blocks, the hitbox following the stage.</li>
 * </ul>
 */
public class EntityPufferfish extends EntityAbstractFish {

    private static final DataParameter<Integer> PUFF_STATE = EntityDataManager.createKey(EntityPufferfish.class, DataSerializers.VARINT);

    /** Registry names of mobs a pufferfish does not fear, beyond any {@code EntityWaterMob}, guardian or fish. */
    private static final Set<ResourceLocation> NOT_SCARY = new HashSet<ResourceLocation>(Arrays.asList(
            new ResourceLocation(UpdateAquaticBackport.MOD_ID, "turtle"),
            new ResourceLocation("minecraft", "squid"),
            new ResourceLocation("minecraft", "guardian"),
            new ResourceLocation("minecraft", "elder_guardian")));

    int puffTimer;
    int deflateTimer;

    public EntityPufferfish(World world) {
        super(world);
        this.setSize(0.35F, 0.35F);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(PUFF_STATE, 0);
    }

    @Override
    protected void initEntityAI() {
        super.initEntityAI();
        this.tasks.addTask(1, new PuffTask(this));
    }

    public int getPuffState() {
        return this.dataManager.get(PUFF_STATE);
    }

    public void setPuffState(int state) {
        this.dataManager.set(PUFF_STATE, state);
    }

    /** @return 1.13's scale of the 0.7-block body for a stage */
    public static float puffScale(int state) {
        switch (state) {
            case 0:
                return 0.5F;
            case 1:
                return 0.7F;
            default:
                return 1.0F;
        }
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (PUFF_STATE.equals(key)) {
            float size = 0.7F * puffScale(this.getPuffState());
            this.setSize(size, size);
        }
        super.notifyDataManagerChange(key);
    }

    @Override
    public void onUpdate() {
        if (!this.world.isRemote && this.isEntityAlive() && this.isServerWorld()) {
            if (this.puffTimer > 0) {
                if (this.getPuffState() == 0) {
                    this.playSound(FishContent.PUFFER_BLOW_UP, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(1);
                } else if (this.puffTimer > 40 && this.getPuffState() == 1) {
                    this.playSound(FishContent.PUFFER_BLOW_UP, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(2);
                }
                ++this.puffTimer;
            } else if (this.getPuffState() != 0) {
                if (this.deflateTimer > 60 && this.getPuffState() == 2) {
                    this.playSound(FishContent.PUFFER_BLOW_OUT, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(1);
                } else if (this.deflateTimer > 100 && this.getPuffState() == 1) {
                    this.playSound(FishContent.PUFFER_BLOW_OUT, this.getSoundVolume(), this.getSoundPitch());
                    this.setPuffState(0);
                }
                ++this.deflateTimer;
            }
        }
        super.onUpdate();
    }

    /** Stings every feared mob touching it while puffed. */
    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (this.isEntityAlive() && this.getPuffState() > 0) {
            for (EntityLiving mob : this.world.getEntitiesWithinAABB(EntityLiving.class, this.getEntityBoundingBox().grow(0.3D))) {
                if (mob != this && mob.isEntityAlive() && isScary(mob)) {
                    this.sting(mob);
                }
            }
        }
    }

    private void sting(EntityLiving mob) {
        int state = this.getPuffState();
        if (mob.attackEntityFrom(DamageSource.causeMobDamage(this), 1 + state)) {
            mob.addPotionEffect(new PotionEffect(MobEffects.POISON, 60 * state, 0));
            this.playSound(FishContent.PUFFER_STING, 1.0F, 1.0F);
        }
    }

    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        int state = this.getPuffState();
        if (player instanceof EntityPlayerMP && state > 0 && player.attackEntityFrom(DamageSource.causeMobDamage(this), 1 + state)) {
            // 1.13 tells the client to play the sting at the player with a game-state packet 1.12.2 lacks.
            EntityPlayerMP mp = (EntityPlayerMP) player;
            if (mp.connection != null) {
                mp.connection.sendPacket(new SPacketSoundEffect(FishContent.PUFFER_STING, SoundCategory.NEUTRAL,
                        player.posX, player.posY, player.posZ, 1.0F, 1.0F));
            }
            player.addPotionEffect(new PotionEffect(MobEffects.POISON, 60 * state, 0));
        }
    }

    /** @return true if a pufferfish puffs up at this entity */
    public static boolean isScary(@Nullable EntityLivingBase entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) entity;
            return !player.isSpectator() && !player.isCreative();
        }
        if (entity instanceof EntityWaterMob || entity instanceof EntityGuardian || entity instanceof EntityAbstractFish) {
            return false;
        }
        ResourceLocation id = EntityList.getKey(entity);
        return id == null || !NOT_SCARY.contains(id);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("PuffState", this.getPuffState());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setPuffState(compound.getInteger("PuffState"));
    }

    @Override
    public int minGroup() {
        return 1;
    }

    @Override
    public int maxGroup() {
        return 3;
    }

    @Override
    protected ItemStack bucket() {
        return FishContent.stack(FishContent.pufferfishBucket);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return FishContent.PUFFERFISH_LOOT;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return FishContent.PUFFER_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return FishContent.PUFFER_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return FishContent.PUFFER_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return FishContent.PUFFER_FLOP;
    }

    /**
     * 1.13's {@code PuffGoal}: runs while anything feared is within 2 blocks. Claims no movement bits, so
     * it is asked at every check whatever else the fish is doing.
     */
    static final class PuffTask extends EntityAIBase {
        private final EntityPufferfish fish;

        PuffTask(EntityPufferfish fish) {
            this.fish = fish;
            this.setMutexBits(0);
        }

        private boolean threatened() {
            for (EntityLivingBase e : this.fish.world.getEntitiesWithinAABB(EntityLivingBase.class, this.fish.getEntityBoundingBox().grow(2.0D))) {
                if (e != this.fish && isScary(e)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean shouldExecute() {
            return this.threatened();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.threatened();
        }

        @Override
        public void startExecuting() {
            this.fish.puffTimer = 1;
            this.fish.deflateTimer = 0;
        }

        @Override
        public void resetTask() {
            this.fish.puffTimer = 0;
        }
    }
}
