package com.exiledradio.updateaquaticbackport.phantom;

import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityBodyHelper;
import net.minecraft.entity.EntityFlying;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.EnumCreatureAttribute;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAITarget;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.passive.EntityOcelot;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * The phantom: 1.14.4's {@code PhantomEntity}, with current Java's guard against a zero-length steer.
 *
 * <h2>How it flies</h2>
 * It never pathfinds. It steers toward one point, {@link #orbitOffset}, with its own move helper: turning at
 * most 4 degrees a tick and speeding up while it flies straight. Three goals pick the point:
 * <ul>
 *   <li>{@link OrbitPointGoal}: idle, or between swoops, circles {@link #orbitPosition} 5-15 blocks out;</li>
 *   <li>{@link PickAttackGoal}: with a target, moves the circle 20-40 blocks over it and every 8-12 seconds
 *       switches to a swoop;</li>
 *   <li>{@link SweepAttackGoal}: dives at the target, bites on contact, and breaks off after a bite, on hitting
 *       a wall, on being hurt, or when a cat is within 16 blocks.</li>
 * </ul>
 *
 * <h2>Size</h2>
 * {@code Size} (0-64, 0 when spawned) widens the hitbox by 0.2 per step, scales the model by 0.15 per step and
 * adds 1 attack damage per step to the base 6.
 *
 * <h2>1.12.2 differences in the plumbing only</h2>
 * 1.12.2's look helper zeroes the pitch every tick after the move helper has set it, and the look helper cannot be
 * replaced without reflection, so the move helper's pitch is put back in {@link #travel}. The target goal counts
 * its own delay in game ticks, because 1.12.2 asks a goal that is not running only every third tick.
 */
public class EntityPhantom extends EntityFlying implements IMob {

    private static final DataParameter<Integer> SIZE = EntityDataManager.createKey(EntityPhantom.class, DataSerializers.VARINT);

    public static final float BASE_WIDTH = 0.9F;
    public static final float BASE_HEIGHT = 0.5F;
    public static final int MAX_SIZE = 64;

    /** 1.14's cat check distance. */
    public static final double CAT_RANGE = 16.0D;

    private static final ResourceLocation FAMILY_CAT = new ResourceLocation(Owners.VILLAGE_AND_PILLAGE, "cat");

    enum AttackPhase {
        CIRCLE,
        SWOOP
    }

    Vec3d orbitOffset = Vec3d.ZERO;
    BlockPos orbitPosition = BlockPos.ORIGIN;
    AttackPhase attackPhase = AttackPhase.CIRCLE;

    /** The pitch the move helper chose this tick, restored after 1.12.2's look helper zeroes it. */
    private float steerPitch;

    public EntityPhantom(World world) {
        super(world);
        this.setSize(BASE_WIDTH, BASE_HEIGHT);
        this.experienceValue = 5;
        this.moveHelper = new MoveHelper(this);
    }

    @Override
    protected EntityBodyHelper createBodyHelper() {
        return new BodyHelper(this);
    }

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(1, new PickAttackGoal(this));
        this.tasks.addTask(2, new SweepAttackGoal(this));
        this.tasks.addTask(3, new OrbitPointGoal(this));
        this.targetTasks.addTask(1, new AttackPlayerGoal(this));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getAttributeMap().registerAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).setBaseValue(6.0D);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SIZE, 0);
    }

    public void setPhantomSize(int size) {
        this.dataManager.set(SIZE, MathHelper.clamp(size, 0, MAX_SIZE));
        // Set on a fresh entity before anything watches it, the change callback may not run; do its work here too.
        this.updatePhantomSize();
    }

    public int getPhantomSize() {
        return this.dataManager.get(SIZE);
    }

    /** 1.14.4: the hitbox widens 0.2 per size step, in proportion; damage is 6 + size. */
    private void updatePhantomSize() {
        int size = this.getPhantomSize();
        float scale = (BASE_WIDTH + 0.2F * size) / BASE_WIDTH;
        this.setSize(BASE_WIDTH * scale, BASE_HEIGHT * scale);
        IAttributeInstance damage = this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.setBaseValue(6 + size);
        }
    }

    @Override
    public void notifyDataManagerChange(DataParameter<?> key) {
        if (SIZE.equals(key)) {
            this.updatePhantomSize();
        }
        super.notifyDataManagerChange(key);
    }

    @Override
    public float getEyeHeight() {
        return this.height * 0.35F;
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (this.world.isRemote) {
            // 1.14.4: a flap sound once per wing beat, and a trail of mycelium particles from both wing tips.
            float beat = MathHelper.cos((this.getEntityId() * 3 + this.ticksExisted) * 0.13F + (float) Math.PI);
            float next = MathHelper.cos((this.getEntityId() * 3 + this.ticksExisted + 1) * 0.13F + (float) Math.PI);
            if (beat > 0.0F && next <= 0.0F && PhantomContent.soundFlap != null) {
                this.world.playSound(this.posX, this.posY, this.posZ, PhantomContent.soundFlap, this.getSoundCategory(),
                        0.95F + this.rand.nextFloat() * 0.05F, 0.95F + this.rand.nextFloat() * 0.05F, false);
            }
            int size = this.getPhantomSize();
            float dx = MathHelper.cos(this.rotationYaw * 0.017453292F) * (1.3F + 0.21F * size);
            float dz = MathHelper.sin(this.rotationYaw * 0.017453292F) * (1.3F + 0.21F * size);
            float dy = (0.3F + beat * 0.45F) * (size * 0.2F + 1.0F);
            this.world.spawnParticle(EnumParticleTypes.TOWN_AURA, this.posX + dx, this.posY + dy, this.posZ + dz, 0, 0, 0);
            this.world.spawnParticle(EnumParticleTypes.TOWN_AURA, this.posX - dx, this.posY + dy, this.posZ - dz, 0, 0, 0);
        }
        if (!this.world.isRemote && this.world.getDifficulty() == EnumDifficulty.PEACEFUL) {
            this.setDead();
        }
    }

    @Override
    public void onLivingUpdate() {
        if (this.isEntityAlive() && this.isInDaylight()) {
            this.setFire(8);
        }
        super.onLivingUpdate();
    }

    /** 1.14.4 {@code MobEntity.isInDaylight}: as a zombie's, from the block at its rounded height. */
    protected boolean isInDaylight() {
        if (this.world.isDaytime() && !this.world.isRemote) {
            float brightness = this.getBrightness();
            BlockPos pos = new BlockPos(this.posX, Math.round(this.posY), this.posZ);
            return brightness > 0.5F && this.rand.nextFloat() * 30.0F < (brightness - 0.4F) * 2.0F && this.world.canSeeSky(pos);
        }
        return false;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (!this.world.isRemote) {
            this.rotationPitch = this.steerPitch;
        }
        super.travel(strafe, vertical, forward);
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        this.orbitPosition = new BlockPos(this).up(5);
        this.setPhantomSize(0);
        return super.onInitialSpawn(difficulty, data);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("AX")) {
            this.orbitPosition = new BlockPos(compound.getInteger("AX"), compound.getInteger("AY"), compound.getInteger("AZ"));
        }
        this.setPhantomSize(compound.getInteger("Size"));
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setInteger("AX", this.orbitPosition.getX());
        compound.setInteger("AY", this.orbitPosition.getY());
        compound.setInteger("AZ", this.orbitPosition.getZ());
        compound.setInteger("Size", this.getPhantomSize());
    }

    /** 1.14.4: drawn at any distance the tracker sends it from. */
    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.HOSTILE;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return PhantomContent.soundAmbient;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return PhantomContent.soundHurt;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return PhantomContent.soundDeath;
    }

    @Override
    protected float getSoundVolume() {
        return 1.0F;
    }

    @Override
    public EnumCreatureAttribute getCreatureAttribute() {
        return EnumCreatureAttribute.UNDEAD;
    }

    @Override
    public boolean canAttackClass(Class<? extends EntityLivingBase> cls) {
        return true;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return PhantomContent.LOOT_TABLE;
    }

    /** 1.12.2 {@code EntityMob.attackEntityAsMob}: the attack damage attribute, enchantments, knockback, fire. */
    @Override
    public boolean attackEntityAsMob(Entity target) {
        float damage = (float) this.getEntityAttribute(SharedMonsterAttributes.ATTACK_DAMAGE).getAttributeValue();
        int knockback = 0;
        if (target instanceof EntityLivingBase) {
            damage += EnchantmentHelper.getModifierForCreature(this.getHeldItemMainhand(), ((EntityLivingBase) target).getCreatureAttribute());
            knockback += EnchantmentHelper.getKnockbackModifier(this);
        }
        boolean hit = target.attackEntityFrom(DamageSource.causeMobDamage(this), damage);
        if (hit) {
            if (knockback > 0 && target instanceof EntityLivingBase) {
                ((EntityLivingBase) target).knockBack(this, knockback * 0.5F,
                        MathHelper.sin(this.rotationYaw * 0.017453292F), -MathHelper.cos(this.rotationYaw * 0.017453292F));
                this.motionX *= 0.6D;
                this.motionZ *= 0.6D;
            }
            int fire = EnchantmentHelper.getFireAspectModifier(this);
            if (fire > 0) {
                target.setFire(fire * 4);
            }
            this.applyEnchantments(this, target);
        }
        return hit;
    }

    /** 1.14's {@code canTarget} with the default predicate: alive, attackable, not creative or spectating. */
    boolean canTarget(@Nullable EntityLivingBase target) {
        return target != null && EntityAITarget.isSuitableTarget(this, target, false, false);
    }

    /**
     * The cats within 16 blocks: 1.12.2's tamed ocelots (its cats), and Village &amp; Pillage's cats when that
     * mod is installed (it makes its own cats hiss; tamed ocelots hiss here).
     */
    List<EntityLiving> nearbyCats() {
        AxisAlignedBB box = this.getEntityBoundingBox().grow(CAT_RANGE);
        List<EntityLiving> cats = new ArrayList<EntityLiving>();
        Class<? extends Entity> familyCat = Owners.present(Owners.VILLAGE_AND_PILLAGE) ? EntityList.getClass(FAMILY_CAT) : null;
        if (familyCat != null && EntityLiving.class.isAssignableFrom(familyCat)) {
            for (Entity cat : this.world.getEntitiesWithinAABB(familyCat, box)) {
                if (cat.isEntityAlive()) {
                    cats.add((EntityLiving) cat);
                }
            }
        } else {
            for (EntityOcelot cat : this.world.getEntitiesWithinAABB(EntityOcelot.class, box)) {
                if (cat.isEntityAlive() && cat.isTamed()) {
                    cats.add(cat);
                }
            }
        }
        return cats;
    }

    // ------------------------------------------------------------------------------------------
    // Controllers
    // ------------------------------------------------------------------------------------------

    /** 1.14.4: the head follows the body, the body follows the heading. */
    static final class BodyHelper extends EntityBodyHelper {
        private final EntityLiving phantom;

        BodyHelper(EntityLiving phantom) {
            super(phantom);
            this.phantom = phantom;
        }

        @Override
        public void updateRenderAngles() {
            this.phantom.rotationYawHead = this.phantom.renderYawOffset;
            this.phantom.renderYawOffset = this.phantom.rotationYaw;
        }
    }

    /** 1.14.4's {@code MoveHelperController}: steer toward {@link #orbitOffset}. */
    static final class MoveHelper extends EntityMoveHelper {
        private final EntityPhantom phantom;
        private float speedFactor = 0.1F;

        MoveHelper(EntityPhantom phantom) {
            super(phantom);
            this.phantom = phantom;
        }

        @Override
        public void onUpdateMoveHelper() {
            EntityPhantom p = this.phantom;
            if (p.collidedHorizontally) {
                p.rotationYaw += 180.0F;
                this.speedFactor = 0.1F;
            }
            float dx = (float) (p.orbitOffset.x - p.posX);
            float dy = (float) (p.orbitOffset.y - p.posY);
            float dz = (float) (p.orbitOffset.z - p.posZ);
            double flat = MathHelper.sqrt(dx * dx + dz * dz);
            // Current Java: a point straight above or below would divide by zero.
            if (Math.abs(flat) <= 1.0E-5F) {
                return;
            }
            double level = 1.0D - MathHelper.abs(dy * 0.7F) / flat;
            dx = (float) (dx * level);
            dz = (float) (dz * level);
            flat = MathHelper.sqrt(dx * dx + dz * dz);
            double all = MathHelper.sqrt(dx * dx + dz * dz + dy * dy);
            float oldYaw = p.rotationYaw;
            float heading = (float) MathHelper.atan2(dz, dx);
            float yaw = MathHelper.wrapDegrees(p.rotationYaw + 90.0F);
            float want = MathHelper.wrapDegrees(heading * (180F / (float) Math.PI));
            p.rotationYaw = approachDegrees(yaw, want, 4.0F) - 90.0F;
            p.renderYawOffset = p.rotationYaw;
            if (Math.abs(MathHelper.wrapDegrees(p.rotationYaw - oldYaw)) < 3.0F) {
                this.speedFactor = approach(this.speedFactor, 1.8F, 0.005F * (1.8F / this.speedFactor));
            } else {
                this.speedFactor = approach(this.speedFactor, 0.2F, 0.025F);
            }
            float pitch = (float) (-(MathHelper.atan2(-dy, flat) * (180F / (float) Math.PI)));
            p.rotationPitch = pitch;
            p.steerPitch = pitch;
            float dir = p.rotationYaw + 90.0F;
            double mx = this.speedFactor * MathHelper.cos(dir * 0.017453292F) * Math.abs(dx / all);
            double mz = this.speedFactor * MathHelper.sin(dir * 0.017453292F) * Math.abs(dz / all);
            double my = this.speedFactor * MathHelper.sin(pitch * 0.017453292F) * Math.abs(dy / all);
            p.motionX += (mx - p.motionX) * 0.2D;
            p.motionY += (my - p.motionY) * 0.2D;
            p.motionZ += (mz - p.motionZ) * 0.2D;
        }
    }

    /** 1.14 {@code MathHelper.approach}. */
    static float approach(float current, float target, float step) {
        step = Math.abs(step);
        return current < target ? MathHelper.clamp(current + step, current, target) : MathHelper.clamp(current - step, target, current);
    }

    /** 1.14 {@code MathHelper.approachDegrees}. */
    static float approachDegrees(float current, float target, float step) {
        return approach(current, current + MathHelper.wrapDegrees(target - current), step);
    }

    // ------------------------------------------------------------------------------------------
    // Goals
    // ------------------------------------------------------------------------------------------

    /** 1.14's {@code MoveGoal}: holds the move flag; "close" is within 2 blocks of the steering point. */
    abstract static class MoveGoal extends EntityAIBase {
        final EntityPhantom phantom;

        MoveGoal(EntityPhantom phantom) {
            this.phantom = phantom;
            this.setMutexBits(1);
        }

        boolean nearSteeringPoint() {
            return this.phantom.orbitOffset.squareDistanceTo(this.phantom.posX, this.phantom.posY, this.phantom.posZ) < 4.0D;
        }
    }

    /** 1.14.4's {@code OrbitPointGoal}. */
    static final class OrbitPointGoal extends MoveGoal {
        private float angle;
        private float distance;
        private float height;
        private float direction;

        OrbitPointGoal(EntityPhantom phantom) {
            super(phantom);
        }

        @Override
        public boolean shouldExecute() {
            return this.phantom.getAttackTarget() == null || this.phantom.attackPhase == AttackPhase.CIRCLE;
        }

        @Override
        public void startExecuting() {
            this.distance = 5.0F + this.phantom.rand.nextFloat() * 10.0F;
            this.height = -4.0F + this.phantom.rand.nextFloat() * 9.0F;
            this.direction = this.phantom.rand.nextBoolean() ? 1.0F : -1.0F;
            this.selectNext();
        }

        @Override
        public void updateTask() {
            EntityPhantom p = this.phantom;
            if (p.rand.nextInt(350) == 0) {
                this.height = -4.0F + p.rand.nextFloat() * 9.0F;
            }
            if (p.rand.nextInt(250) == 0) {
                ++this.distance;
                if (this.distance > 15.0F) {
                    this.distance = 5.0F;
                    this.direction = -this.direction;
                }
            }
            if (p.rand.nextInt(450) == 0) {
                this.angle = p.rand.nextFloat() * 2.0F * (float) Math.PI;
                this.selectNext();
            }
            if (this.nearSteeringPoint()) {
                this.selectNext();
            }
            if (p.orbitOffset.y < p.posY && !p.world.isAirBlock(new BlockPos(p).down(1))) {
                this.height = Math.max(1.0F, this.height);
                this.selectNext();
            }
            if (p.orbitOffset.y > p.posY && !p.world.isAirBlock(new BlockPos(p).up(1))) {
                this.height = Math.min(-1.0F, this.height);
                this.selectNext();
            }
        }

        private void selectNext() {
            EntityPhantom p = this.phantom;
            if (BlockPos.ORIGIN.equals(p.orbitPosition)) {
                p.orbitPosition = new BlockPos(p);
            }
            this.angle += this.direction * 15.0F * 0.017453292F;
            p.orbitOffset = new Vec3d(p.orbitPosition).add(this.distance * MathHelper.cos(this.angle), -4.0F + this.height,
                    this.distance * MathHelper.sin(this.angle));
        }
    }

    /** 1.14.4's {@code PickAttackGoal}: circle high over the target, then swoop every 8-12 seconds. */
    static final class PickAttackGoal extends EntityAIBase {
        private final EntityPhantom phantom;
        private int tickDelay;

        PickAttackGoal(EntityPhantom phantom) {
            this.phantom = phantom;
        }

        @Override
        public boolean shouldExecute() {
            return this.phantom.canTarget(this.phantom.getAttackTarget());
        }

        @Override
        public void startExecuting() {
            this.tickDelay = 10;
            this.phantom.attackPhase = AttackPhase.CIRCLE;
            this.anchorAboveTarget();
        }

        @Override
        public void resetTask() {
            EntityPhantom p = this.phantom;
            // 1.14.4 uses the motion-blocking heightmap; 1.12.2's height map is the nearest it has.
            p.orbitPosition = p.world.getHeight(p.orbitPosition).up(10 + p.rand.nextInt(20));
        }

        @Override
        public void updateTask() {
            EntityPhantom p = this.phantom;
            if (p.attackPhase == AttackPhase.CIRCLE) {
                --this.tickDelay;
                if (this.tickDelay <= 0) {
                    p.attackPhase = AttackPhase.SWOOP;
                    this.anchorAboveTarget();
                    this.tickDelay = (8 + p.rand.nextInt(4)) * 20;
                    if (PhantomContent.soundSwoop != null) {
                        p.playSound(PhantomContent.soundSwoop, 10.0F, 0.95F + p.rand.nextFloat() * 0.1F);
                    }
                }
            }
        }

        private void anchorAboveTarget() {
            EntityPhantom p = this.phantom;
            EntityLivingBase target = p.getAttackTarget();
            if (target == null) {
                return;
            }
            p.orbitPosition = new BlockPos(target).up(20 + p.rand.nextInt(20));
            if (p.orbitPosition.getY() < p.world.getSeaLevel()) {
                p.orbitPosition = new BlockPos(p.orbitPosition.getX(), p.world.getSeaLevel() + 1, p.orbitPosition.getZ());
            }
        }
    }

    /** 1.14.4's {@code SweepAttackGoal}: dive, bite, break off. */
    static final class SweepAttackGoal extends MoveGoal {
        SweepAttackGoal(EntityPhantom phantom) {
            super(phantom);
        }

        @Override
        public boolean shouldExecute() {
            return this.phantom.getAttackTarget() != null && this.phantom.attackPhase == AttackPhase.SWOOP;
        }

        @Override
        public boolean shouldContinueExecuting() {
            EntityLivingBase target = this.phantom.getAttackTarget();
            if (target == null || !target.isEntityAlive()) {
                return false;
            }
            if (target instanceof EntityPlayer && (((EntityPlayer) target).isSpectator() || ((EntityPlayer) target).isCreative())) {
                return false;
            }
            if (!this.shouldExecute()) {
                return false;
            }
            if (this.phantom.ticksExisted % 20 == 0) {
                List<EntityLiving> cats = this.phantom.nearbyCats();
                if (!cats.isEmpty()) {
                    for (EntityLiving cat : cats) {
                        if (cat instanceof EntityOcelot) {
                            cat.playSound(SoundEvents.ENTITY_CAT_HISS, 1.0F, 1.0F);
                        }
                    }
                    return false;
                }
            }
            return true;
        }

        @Override
        public void resetTask() {
            this.phantom.setAttackTarget(null);
            this.phantom.attackPhase = AttackPhase.CIRCLE;
        }

        @Override
        public void updateTask() {
            EntityPhantom p = this.phantom;
            EntityLivingBase target = p.getAttackTarget();
            if (target == null) {
                return;
            }
            p.orbitOffset = new Vec3d(target.posX, target.posY + target.height * 0.5D, target.posZ);
            if (p.getEntityBoundingBox().grow(0.2F).intersects(target.getEntityBoundingBox())) {
                p.attackEntityAsMob(target);
                p.attackPhase = AttackPhase.CIRCLE;
                // 1.14.4's level event 1039: the bite, at 0.3 volume.
                if (!p.isSilent() && PhantomContent.soundBite != null) {
                    p.world.playSound(null, new BlockPos(p), PhantomContent.soundBite, SoundCategory.HOSTILE, 0.3F,
                            p.world.rand.nextFloat() * 0.1F + 0.9F);
                }
            } else if (p.collidedHorizontally || p.hurtTime > 0) {
                p.attackPhase = AttackPhase.CIRCLE;
            }
        }
    }

    /**
     * 1.14.4's {@code AttackPlayerGoal}: every 3 seconds, the highest suitable player within 64 blocks (in a box
     * 16 blocks wide each way and 64 up and down).
     */
    static final class AttackPlayerGoal extends EntityAIBase {
        private final EntityPhantom phantom;
        /** Game tick of the next scan; 1.14 waits 20 ticks for the first and 60 between. */
        private int nextScan = 20;

        AttackPlayerGoal(EntityPhantom phantom) {
            this.phantom = phantom;
        }

        @Override
        public boolean shouldExecute() {
            EntityPhantom p = this.phantom;
            if (p.ticksExisted < this.nextScan) {
                return false;
            }
            this.nextScan = p.ticksExisted + 60;
            List<EntityPlayer> players = p.world.getEntitiesWithinAABB(EntityPlayer.class, p.getEntityBoundingBox().grow(16.0D, 64.0D, 16.0D));
            if (players.isEmpty()) {
                return false;
            }
            Collections.sort(players, new Comparator<EntityPlayer>() {
                @Override
                public int compare(EntityPlayer a, EntityPlayer b) {
                    return Double.compare(b.posY, a.posY);
                }
            });
            for (EntityPlayer player : players) {
                if (withinRange(p, player) && p.canTarget(player)) {
                    p.setAttackTarget(player);
                    return true;
                }
            }
            return false;
        }

        /** 1.14's 64-block targeting range, shortened for sneaking and invisible players as 1.12.2's mobs do. */
        private static boolean withinRange(EntityPhantom p, EntityPlayer player) {
            double range = 64.0D;
            if (player.isSneaking()) {
                range *= 0.8D;
            }
            if (player.isInvisible()) {
                float visibility = player.getArmorVisibility();
                if (visibility < 0.1F) {
                    visibility = 0.1F;
                }
                range *= 0.7D * visibility;
            }
            range = Math.max(range, 2.0D);
            return p.getDistanceSq(player) <= range * range;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.phantom.canTarget(this.phantom.getAttackTarget());
        }
    }

    /** For the self-test: the steering point and the anchor. */
    public Vec3d steeringPoint() {
        return this.orbitOffset;
    }

    public BlockPos anchor() {
        return this.orbitPosition;
    }

    public boolean swooping() {
        return this.attackPhase == AttackPhase.SWOOP;
    }
}
