package com.exiledradio.updateaquaticbackport.dolphin;

import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackMelee;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityGuardian;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The dolphin, after 1.14.4's {@code DolphinEntity} and the current Java wiki.
 *
 * <ul>
 *   <li>Breathes air: 4800 ticks under water, refilled at once in air, drowning after that like any mob;
 *       swims up to breathe below 140 ({@link AIBreatheAir}).</li>
 *   <li>Dries out on land: 2400 ticks of moistness, kept full in water or rain, then 1 damage a tick;
 *       flops about while on the ground.</li>
 *   <li>Plays with items dropped in water, follows moving boats, leaps out of the water, swims with
 *       players who sprint in water and gives them Dolphin's Grace.</li>
 *   <li>Neutral: hit by anything but a guardian, it and the dolphins near it fight back. Avoids
 *       guardians.</li>
 *   <li>Fed a raw fish, leads to the nearest shipwreck or ocean ruin, through {@link com.exiledradio.backportcore.structure.Structures}.</li>
 * </ul>
 *
 * <p>It extends {@link EntityCreature} (1.12.2's water mob class is not one, and the AI tasks need one)
 * and counts against the water creature cap through {@link #isCreatureType}.
 */
public class EntityDolphin extends EntityCreature implements IAnimals {

    private static final DataParameter<BlockPos> TREASURE_POS = EntityDataManager.createKey(EntityDolphin.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> GOT_FISH = EntityDataManager.createKey(EntityDolphin.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> MOISTNESS = EntityDataManager.createKey(EntityDolphin.class, DataSerializers.VARINT);

    public static final int MAX_AIR = 4800;
    public static final int MAX_MOISTNESS = 2400;
    /** Below this much air the dolphin stops what it does and swims up. */
    public static final int LOW_AIR = 140;
    /** 1.14.4's entity event for the happy particles of a dolphin that knows the way. */
    public static final byte EVENT_HAPPY = 38;

    /** 1.14.4's {@code DamageSource.DRYOUT}: the death message is vanilla's {@code death.attack.dryout}. */
    public static final DamageSource DRYOUT = new DamageSource("dryout");

    /** Lukewarm and warm oceans: 1.13's weight there is a fifth of the normal ocean's, against squid. */
    public static final float WARM_SPAWN_CHANCE = 0.2F;

    public EntityDolphin(World world) {
        super(world);
        this.setSize(0.9F, 0.6F);
        this.moveHelper = new DolphinMoveHelper(this);
        // 1.12.2 keeps the look helper private (move and jump helpers are protected): set it by its SRG name.
        ObfuscationReflectionHelper.setPrivateValue(EntityLiving.class, this, new DolphinLookHelper(this, 10), "field_70749_g");
        this.setCanPickUpLoot(true);
        this.setAir(MAX_AIR);
    }

    // ---------------------------------------------------------------------------------------------
    // Data

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(TREASURE_POS, BlockPos.ORIGIN);
        this.dataManager.register(GOT_FISH, false);
        this.dataManager.register(MOISTNESS, MAX_MOISTNESS);
    }

    public BlockPos getTreasurePos() {
        return this.dataManager.get(TREASURE_POS);
    }

    public void setTreasurePos(BlockPos pos) {
        this.dataManager.set(TREASURE_POS, pos);
    }

    public boolean hasGotFish() {
        return this.dataManager.get(GOT_FISH);
    }

    public void setGotFish(boolean gotFish) {
        this.dataManager.set(GOT_FISH, gotFish);
    }

    public int getMoistness() {
        return this.dataManager.get(MOISTNESS);
    }

    public void setMoistness(int moistness) {
        this.dataManager.set(MOISTNESS, moistness);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("TreasurePosX", this.getTreasurePos().getX());
        compound.setInteger("TreasurePosY", this.getTreasurePos().getY());
        compound.setInteger("TreasurePosZ", this.getTreasurePos().getZ());
        compound.setBoolean("GotFish", this.hasGotFish());
        compound.setInteger("Moistness", this.getMoistness());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        this.setTreasurePos(new BlockPos(compound.getInteger("TreasurePosX"), compound.getInteger("TreasurePosY"),
                compound.getInteger("TreasurePosZ")));
        super.readEntityFromNBT(compound);
        this.setGotFish(compound.getBoolean("GotFish"));
        this.setMoistness(compound.hasKey("Moistness") ? compound.getInteger("Moistness") : MAX_MOISTNESS);
    }

    // ---------------------------------------------------------------------------------------------
    // AI and attributes

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new AIBreatheAir(this));
        this.tasks.addTask(0, new AIFindWater(this));
        this.tasks.addTask(1, new AISwimToTreasure(this));
        this.tasks.addTask(2, new AISwimWithPlayer(this, 4.0D));
        this.tasks.addTask(4, new AIRandomSwim(this, 1.0D, 10));
        this.tasks.addTask(4, new EntityAILookIdle(this));
        this.tasks.addTask(5, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
        this.tasks.addTask(5, new AIDolphinJump(this, 10));
        this.tasks.addTask(6, new EntityAIAttackMelee(this, 1.2F, true));
        this.tasks.addTask(8, new AIPlayWithItems(this));
        this.tasks.addTask(8, new AIFollowBoat(this));
        this.tasks.addTask(9, new EntityAIAvoidEntity<EntityGuardian>(this, EntityGuardian.class, 8.0F, 1.0D, 1.0D));
        this.targetTasks.addTask(1, new AIDefendPod(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.2F);
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(3.0D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new DolphinPathNavigator(this, world);
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.setAir(MAX_AIR);
        this.rotationPitch = 0.0F;
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    public boolean attackEntityAsMob(Entity target) {
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this),
                (float) (int) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue());
        if (hit) {
            this.applyEnchantments(this, target);
            this.playSound(DolphinContent.ATTACK, 1.0F, 1.0F);
        }
        return hit;
    }

    @Override
    public int getVerticalFaceSpeed() {
        return 1;
    }

    @Override
    public int getHorizontalFaceSpeed() {
        return 1;
    }

    @Override
    public float getEyeHeight() {
        return 0.3F;
    }

    // ---------------------------------------------------------------------------------------------
    // Breathing and drying out

    @Override
    public boolean canBreatheUnderwater() {
        return false;
    }

    /**
     * 1.12.2's living tick resets air to 300 whenever the eyes are out of water; a dolphin's supply is
     * 4800, refilled at once (1.14.4's {@code determineNextAir}). A bubble column of this mod counts as air.
     */
    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if (this.isEntityAlive() && (!this.isInsideOfMaterial(Material.WATER) || this.eyesInBubbleColumn())) {
            this.setAir(MAX_AIR);
        }
    }

    private boolean eyesInBubbleColumn() {
        Block column = Owners.ourBlock("bubble_column");
        return column != null && this.world.getBlockState(new BlockPos(this.posX, this.posY + this.getEyeHeight(), this.posZ)).getBlock() == column;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.isAIDisabled()) {
            return;
        }
        if (this.isWet()) {
            this.setMoistness(MAX_MOISTNESS);
        } else {
            this.setMoistness(this.getMoistness() - 1);
            if (this.getMoistness() <= 0) {
                this.attackEntityFrom(DRYOUT, 1.0F);
            }
            if (this.onGround) {
                this.motionX += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.2F;
                this.motionY += 0.5D;
                this.motionZ += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.2F;
                this.rotationYaw = this.rand.nextFloat() * 360.0F;
                this.onGround = false;
                this.isAirBorne = true;
            }
        }
        if (this.world.isRemote && this.isInWater()
                && this.motionX * this.motionX + this.motionY * this.motionY + this.motionZ * this.motionZ > 0.03D) {
            com.exiledradio.updateaquaticbackport.dolphin.client.DolphinClient.trail(this);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Movement

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater()) {
            this.moveRelative(strafe, vertical, forward, this.getAIMoveSpeed());
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
            if (this.getAttackTarget() == null) {
                this.motionY -= 0.005D;
            }
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    /** @return true when the navigator's target is within 12 blocks (1.14.4's {@code closeToTarget}) */
    boolean closeToTarget(@Nullable BlockPos target) {
        return target != null && target.distanceSq(this.posX, this.posY, this.posZ) < 144.0D;
    }

    // ---------------------------------------------------------------------------------------------
    // Items

    /** Only what goes in the main hand, and only into an empty one (1.14.4's {@code canPickUpItem}). */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem item) {
        ItemStack stack = item.getItem();
        if (this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND).isEmpty()
                && EntityLiving.getSlotForItemStack(stack) == EntityEquipmentSlot.MAINHAND) {
            this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, stack);
            this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
            this.onItemPickup(item, stack.getCount());
            item.setDead();
        }
    }

    /** Raw cod, raw salmon, tropical fish and pufferfish: 1.12.2's raw fish in all four kinds. */
    public static boolean isFood(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == Items.FISH;
    }

    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack stack = player.getHeldItem(hand);
        if (isFood(stack)) {
            if (!this.world.isRemote) {
                this.playSound(DolphinContent.EAT, 1.0F, 1.0F);
            }
            this.setGotFish(true);
            if (!player.capabilities.isCreativeMode) {
                stack.shrink(1);
            }
            return true;
        }
        return super.processInteract(player, hand);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleStatusUpdate(byte id) {
        if (id == EVENT_HAPPY) {
            for (int i = 0; i < 7; ++i) {
                double dx = this.rand.nextGaussian() * 0.01D;
                double dy = this.rand.nextGaussian() * 0.01D;
                double dz = this.rand.nextGaussian() * 0.01D;
                this.world.spawnParticle(EnumParticleTypes.VILLAGER_HAPPY,
                        this.posX + this.rand.nextFloat() * this.width * 2.0F - this.width,
                        this.posY + 0.2F + this.rand.nextFloat() * this.height,
                        this.posZ + this.rand.nextFloat() * this.width * 2.0F - this.width, dx, dy, dz);
            }
        } else {
            super.handleStatusUpdate(id);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Spawning, despawning, counting, drops

    /** Counts against the water creature cap, as 1.12.2's squid does, and nothing else. */
    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        if (forSpawnCount && this.isNoDespawnRequired()) {
            return false;
        }
        return type == EnumCreatureType.WATER_CREATURE;
    }

    @Override
    public boolean getCanSpawnHere() {
        OceanKind kind = OceanKind.at(this.world, new BlockPos(this));
        return canSpawnAt(this.world, new BlockPos(this), kind, this.rand);
    }

    /**
     * Current Java's surface water animal rule and 1.13's oceans: from 13 below sea level up to it, in
     * water with water above and below; not in cold or frozen oceans; in lukewarm and warm ones one try
     * in {@code 1 / WARM_SPAWN_CHANCE} (their dolphin weight is a fifth of the ocean's against squid).
     */
    public static boolean canSpawnAt(World world, BlockPos pos, OceanKind kind, Random random) {
        int sea = world.getSeaLevel();
        if (pos.getY() < sea - 13 || pos.getY() > sea) {
            return false;
        }
        if (world.getBlockState(pos).getMaterial() != Material.WATER
                || world.getBlockState(pos.down()).getMaterial() != Material.WATER
                || world.getBlockState(pos.up()).getMaterial() != Material.WATER) {
            return false;
        }
        switch (kind) {
            case NORMAL:
                return true;
            case LUKEWARM:
            case WARM:
                return random.nextFloat() < WARM_SPAWN_CHANCE;
            default:
                return false;
        }
    }

    /** Spawning in water: only other entities may be in the way (as 1.12.2's water mobs). */
    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    /** Despawns as any water mob, but never while on a lead (current Java). Public for the self-test. */
    @Override
    public boolean canDespawn() {
        return !this.getLeashed();
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return !this.getLeashed();
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    /** 1-3 experience. Public for the self-test. */
    @Override
    public int getExperiencePoints(EntityPlayer player) {
        return 1 + this.world.rand.nextInt(3);
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return DolphinContent.LOOT_TABLE;
    }

    // ---------------------------------------------------------------------------------------------
    // Sounds

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DolphinContent.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DolphinContent.DEATH;
    }

    /** Public for the self-test. */
    @Override
    @Nullable
    public SoundEvent getAmbientSound() {
        return this.isInWater() ? DolphinContent.AMBIENT_WATER : DolphinContent.AMBIENT;
    }

    @Override
    protected SoundEvent getSplashSound() {
        return DolphinContent.SPLASH;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return DolphinContent.SWIM;
    }
}
