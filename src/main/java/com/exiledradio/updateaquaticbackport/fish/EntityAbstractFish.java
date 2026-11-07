package com.exiledradio.updateaquaticbackport.fish;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAvoidEntity;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.common.eventhandler.Event;

import javax.annotation.Nullable;

/**
 * 1.13's {@code AbstractFishEntity}: a small water mob that swims about at random, flees players, panics
 * when hurt, flops and suffocates on land, and goes into a bucket.
 *
 * <h2>Why not {@code EntityWaterMob}</h2>
 * 1.12.2's water mob extends {@code EntityLiving}, and every AI task fish need (wandering, avoiding,
 * panicking) takes an {@code EntityCreature} - 1.13 moved water mobs under it for the same reason. So
 * this is a creature with everything {@code EntityWaterMob} has copied in: breathing only in water,
 * {@code isNotColliding}, the talk interval, 1-3 experience, not pushed by water.
 *
 * <h2>Spawning and despawning</h2>
 * Fish are current Java's "water ambient" mobs: their own creature type with a cap of 20
 * ({@link FishContent#waterAmbient()}), spawning only between 13 blocks under sea level and sea level, in
 * water with water above and below, and despawning at once beyond 64 blocks from every player (at random
 * beyond 32). A fish from a bucket, or with a name, never despawns and does not count toward the cap.
 * A natural spawn brings its whole group with it ({@link FishSpawning}).
 */
public abstract class EntityAbstractFish extends EntityCreature implements IAnimals {

    private static final DataParameter<Boolean> FROM_BUCKET = EntityDataManager.createKey(EntityAbstractFish.class, DataSerializers.BOOLEAN);

    /** Set by {@link FishSpawning} when the natural spawner is about to spawn this fish; read once. */
    boolean naturalSpawn;

    protected EntityAbstractFish(World world) {
        super(world);
        this.moveHelper = new FishMoveHelper(this);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(FROM_BUCKET, false);
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(3.0D);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new EntityAIPanic(this, 1.25D));
        this.tasks.addTask(2, new EntityAIAvoidEntity<EntityPlayer>(this, EntityPlayer.class, 8.0F, 1.6D, 1.4D));
        this.tasks.addTask(4, new EntityAIFishSwim(this));
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new PathNavigateSwimmer(this, world);
    }

    // --- being a water mob ------------------------------------------------------------------------

    @Override
    public float getEyeHeight() {
        return this.height * 0.65F;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    @Override
    public int getTalkInterval() {
        return 120;
    }

    @Override
    protected int getExperiencePoints(EntityPlayer player) {
        return 1 + this.world.rand.nextInt(3);
    }

    /** Out of water a fish loses air for 16 seconds, then 1 health a second (2 damage). */
    @Override
    public void onEntityUpdate() {
        int air = this.getAir();
        super.onEntityUpdate();
        if (this.isEntityAlive() && !this.isInWater()) {
            this.setAir(air - 1);
            if (this.getAir() == -20) {
                this.setAir(0);
                this.attackEntityFrom(DamageSource.DROWN, 2.0F);
            }
        } else {
            this.setAir(300);
        }
    }

    /** Flops: a fish on land, on the ground, jumps 0.4 up and a little sideways and plays its flop sound. */
    @Override
    public void onLivingUpdate() {
        if (!this.isInWater() && this.onGround && this.collidedVertically) {
            this.motionX += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
            this.motionY += 0.4F;
            this.motionZ += (this.rand.nextFloat() * 2.0F - 1.0F) * 0.05F;
            this.onGround = false;
            this.isAirBorne = true;
            this.playSound(this.getFlopSound(), this.getSoundVolume(), this.getSoundPitch());
        }
        super.onLivingUpdate();
    }

    /** 1.13's swimming: a slow push along the heading, 0.9 drag, and a slight sink when not chasing. */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater()) {
            this.moveRelative(strafe, vertical, forward, 0.01F);
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

    // --- spawning ----------------------------------------------------------------------------------

    /** Current Java: from 13 below sea level to sea level, water below and above. */
    @Override
    public boolean getCanSpawnHere() {
        BlockPos pos = new BlockPos(this);
        int sea = this.world.getSeaLevel();
        return pos.getY() >= sea - 13 && pos.getY() <= sea
                && this.world.getBlockState(pos).getMaterial() == Material.WATER
                && this.world.getBlockState(pos.down()).getMaterial() == Material.WATER
                && this.world.getBlockState(pos.up()).getMaterial() == Material.WATER;
    }

    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    /** The natural spawner spawns one fish per round; that fish brings its group ({@link FishSpawning}). */
    @Override
    public int getMaxSpawnedInChunk() {
        return 1;
    }

    /** @return the smallest group a natural spawn brings (current Java's spawn entries) */
    public abstract int minGroup();

    /** @return the largest group a natural spawn brings */
    public abstract int maxGroup();

    /** @return true if this fish, spawned first, brings no group (1.13's {@code isMaxGroupSizeReached}) */
    public boolean spawnsAlone() {
        return false;
    }

    /**
     * Counts as a water ambient mob for the spawn cap, except when it never despawns (from a bucket or
     * named), as current Java leaves persistent mobs out of the count.
     */
    @Override
    public boolean isCreatureType(EnumCreatureType type, boolean forSpawnCount) {
        if (forSpawnCount && (this.isFromBucket() || this.isNoDespawnRequired())) {
            return false;
        }
        return type != null && type == FishContent.waterAmbient();
    }

    /**
     * Final so a natural spawn's group is made only once this fish is fully set up (a tropical fish's
     * look is part of the group data); kinds override {@link #prepareSpawn} instead.
     */
    @Override
    @Nullable
    public final IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        IEntityLivingData out = this.prepareSpawn(difficulty, data);
        if (this.naturalSpawn) {
            this.naturalSpawn = false;
            FishSpawning.spawnGroup(this, difficulty, out);
        }
        return out;
    }

    /** What {@code onInitialSpawn} does for this kind. */
    @Nullable
    protected IEntityLivingData prepareSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    protected boolean canDespawn() {
        return !this.isFromBucket() && !this.hasCustomName();
    }

    /** Current Java's water ambient distances: at once beyond 64 blocks, at random beyond 32. */
    @Override
    protected void despawnEntity() {
        if (this.isNoDespawnRequired()) {
            this.idleTime = 0;
            return;
        }
        Event.Result result;
        if ((this.idleTime & 0x1F) == 0x1F && (result = ForgeEventFactory.canEntityDespawn(this)) != Event.Result.DEFAULT) {
            if (result == Event.Result.DENY) {
                this.idleTime = 0;
            } else {
                this.setDead();
            }
            return;
        }
        Entity player = this.world.getClosestPlayerToEntity(this, -1.0D);
        if (player != null) {
            double d = player.getDistanceSq(this);
            if (this.canDespawn() && d > 64.0D * 64.0D) {
                this.setDead();
            }
            if (this.idleTime > 600 && this.rand.nextInt(800) == 0 && d > 32.0D * 32.0D && this.canDespawn()) {
                this.setDead();
            } else if (d < 32.0D * 32.0D) {
                this.idleTime = 0;
            }
        }
    }

    // --- buckets -----------------------------------------------------------------------------------

    public boolean isFromBucket() {
        return this.dataManager.get(FROM_BUCKET);
    }

    public void setFromBucket(boolean fromBucket) {
        this.dataManager.set(FROM_BUCKET, fromBucket);
    }

    /** @return this kind's bucket, empty of data */
    protected abstract ItemStack bucket();

    /** A water bucket on a living fish scoops it up into its bucket, which keeps its name, health and look. */
    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() != Items.WATER_BUCKET || !this.isEntityAlive()) {
            return super.processInteract(player, hand);
        }
        ItemStack filled = this.bucket();
        if (filled.isEmpty()) {
            return super.processInteract(player, hand);
        }
        this.playSound(FishContent.BUCKET_FILL_FISH, 1.0F, 1.0F);
        this.writeBucketData(filled);
        if (player instanceof EntityPlayerMP && !(player instanceof FakePlayer)) {
            FishTriggers.FILLED_BUCKET.trigger((EntityPlayerMP) player, filled);
        }
        if (player.capabilities.isCreativeMode) {
            // Current Java: creative keeps the water bucket and gets the fish bucket once.
            if (!player.inventory.hasItemStack(filled)) {
                player.inventory.addItemStackToInventory(filled);
            }
        } else {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setHeldItem(hand, filled);
            } else if (!player.inventory.addItemStackToInventory(filled)) {
                player.dropItem(filled, false);
            }
        }
        this.setDead();
        return true;
    }

    /** Current Java's bucket data: the name, health and the few flags a command can set. */
    protected void writeBucketData(ItemStack bucket) {
        if (this.hasCustomName()) {
            bucket.setStackDisplayName(this.getCustomNameTag());
        }
        NBTTagCompound tag = bucket.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
            bucket.setTagCompound(tag);
        }
        if (this.isAIDisabled()) {
            tag.setBoolean("NoAI", true);
        }
        if (this.isSilent()) {
            tag.setBoolean("Silent", true);
        }
        if (this.hasNoGravity()) {
            tag.setBoolean("NoGravity", true);
        }
        if (this.isGlowing()) {
            tag.setBoolean("Glowing", true);
        }
        if (this.getIsInvulnerable()) {
            tag.setBoolean("Invulnerable", true);
        }
        tag.setFloat("Health", this.getHealth());
    }

    /** Reads what {@link #writeBucketData} wrote; the fish is not yet in the world. */
    protected void readBucketData(NBTTagCompound tag) {
        if (tag.hasKey("NoAI")) {
            this.setNoAI(tag.getBoolean("NoAI"));
        }
        if (tag.hasKey("Silent")) {
            this.setSilent(tag.getBoolean("Silent"));
        }
        if (tag.hasKey("NoGravity")) {
            this.setNoGravity(tag.getBoolean("NoGravity"));
        }
        if (tag.hasKey("Glowing")) {
            this.setGlowing(tag.getBoolean("Glowing"));
        }
        if (tag.hasKey("Invulnerable")) {
            this.setEntityInvulnerable(tag.getBoolean("Invulnerable"));
        }
        if (tag.hasKey("Health", 99)) {
            this.setHealth(tag.getFloat("Health"));
        }
    }

    /**
     * Sets up a fish just let out of a bucket: as a new spawn, then whatever the bucket kept.
     * Called before the fish is added to the world.
     */
    void releasedFrom(ItemStack bucket) {
        NBTTagCompound tag = bucket.getTagCompound();
        this.onInitialSpawn(this.world.getDifficultyForLocation(new BlockPos(this)), null);
        if (tag != null) {
            this.readBucketData(tag);
        }
        if (bucket.hasDisplayName()) {
            this.setCustomNameTag(bucket.getDisplayName());
        }
        this.setFromBucket(true);
    }

    // --- saving ------------------------------------------------------------------------------------

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setBoolean("FromBucket", this.isFromBucket());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        this.setFromBucket(compound.getBoolean("FromBucket"));
    }

    // --- sounds ------------------------------------------------------------------------------------

    protected abstract SoundEvent getFlopSound();

    @Override
    protected SoundEvent getSwimSound() {
        return FishContent.FISH_SWIM;
    }

    /** True when the random swim may run; a schooling fish that has a leader follows it instead. */
    protected boolean canRandomSwim() {
        return true;
    }
}
