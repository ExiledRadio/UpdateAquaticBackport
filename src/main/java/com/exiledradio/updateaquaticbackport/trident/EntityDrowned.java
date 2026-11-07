package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIAttackRanged;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIHurtByTarget;
import net.minecraft.entity.ai.EntityAILookIdle;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntityPigZombie;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathNavigateSwimmer;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Random;

/**
 * The drowned: an underwater zombie, ported from 1.14.4's {@code DrownedEntity} onto 1.12.2's zombie.
 *
 * <ul>
 *   <li>Swims after targets that are in water, with its own navigator and move helper; walks on land.
 *       Attacks players at night, or by day when they are in water; also villagers and iron golems.</li>
 *   <li>By day it heads for water; at night it swims up and walks out onto the shore.</li>
 *   <li>6.25% spawn with a trident (which they throw: 8 damage, not picked up) and 3.75% with a fishing
 *       rod; 3% hold a nautilus shell in the off hand, always dropped. Never spawns with armor.</li>
 *   <li>Breathes underwater, never breaks doors, drops no head; drops 0-2 rotten flesh (current Java's
 *       loot without the copper ingot of a later update).</li>
 *   <li>Burns in daylight out of water, like a zombie.</li>
 * </ul>
 *
 * <p>Spawning rules are in {@link DrownedSpawning}; zombies become drowned in {@link DrownedConversion}.
 */
public class EntityDrowned extends EntityZombie implements IRangedAttackMob {

    private static final DataParameter<Boolean> SWIMMING = EntityDataManager.createKey(EntityDrowned.class, DataSerializers.BOOLEAN);

    public static final ResourceLocation LOOT_TABLE = Content.id("entities/drowned");

    private boolean swimmingUp;
    protected final PathNavigateSwimmer waterNavigator;
    protected final PathNavigateGround groundNavigator;

    /** Client: 1.14.4's swim animation, 0 to 1. */
    private float swimAnimation;
    private float prevSwimAnimation;

    public EntityDrowned(World world) {
        super(world);
        this.stepHeight = 1.0F;
        this.moveHelper = new MoveHelper(this);
        this.setPathPriority(PathNodeType.WATER, 0.0F);
        this.waterNavigator = new PathNavigateSwimmer(this, world);
        this.groundNavigator = new PathNavigateGround(this, world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(SWIMMING, false);
    }

    /** 1.14.4's zombie goals without the swim goal (drowned do not float), and the drowned's own. */
    @Override
    protected void initEntityAI() {
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(8, new EntityAILookIdle(this));
        this.applyEntityAI();
    }

    @Override
    protected void applyEntityAI() {
        this.tasks.addTask(1, new AIGoToWater(this, 1.0D));
        this.tasks.addTask(2, new AITridentAttack(this, 1.0D, 40, 10.0F));
        this.tasks.addTask(2, new AIAttack(this, 1.0D, false));
        this.tasks.addTask(5, new AIGoToBeach(this, 1.0D));
        this.tasks.addTask(6, new AISwimUp(this, 1.0D, this.world.getSeaLevel()));
        this.tasks.addTask(7, new EntityAIWander(this, 1.0D));
        this.targetTasks.addTask(1, new EntityAIHurtByTarget(this, true, EntityPigZombie.class));
        this.targetTasks.addTask(2, new EntityAINearestAttackableTarget<EntityPlayer>(this, EntityPlayer.class, 10, true, false,
                target -> this.shouldAttack(target)));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityVillager>(this, EntityVillager.class, false));
        this.targetTasks.addTask(3, new EntityAINearestAttackableTarget<EntityIronGolem>(this, EntityIronGolem.class, true));
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        data = super.onInitialSpawn(difficulty, data);
        Item shell = Owners.ours("nautilus_shell");
        if (shell != null && this.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND).isEmpty() && this.rand.nextFloat() < 0.03F) {
            this.setItemStackToSlot(EntityEquipmentSlot.OFFHAND, new ItemStack(shell));
            this.inventoryHandsDropChances[EntityEquipmentSlot.OFFHAND.getIndex()] = 2.0F;
        }
        return data;
    }

    /** 1.14.4: one in ten get a hand item, 10 in 16 of them a trident, the rest a fishing rod; never armor. */
    @Override
    protected void setEquipmentBasedOnDifficulty(DifficultyInstance difficulty) {
        if (this.rand.nextFloat() > 0.9F) {
            int i = this.rand.nextInt(16);
            Item trident = TridentContent.trident();
            if (i < 10 && trident != null) {
                this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(trident));
            } else {
                this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, new ItemStack(Items.FISHING_ROD));
            }
        }
    }

    /**
     * 1.14.4's equipment preference: a held trident is only swapped for a less damaged one, any trident is
     * taken over anything else, and the rest is the zombie's rule.
     */
    @Override
    protected void updateEquipmentIfNeeded(EntityItem itemEntity) {
        ItemStack candidate = itemEntity.getItem();
        ItemStack held = this.getItemStackFromSlot(EntityEquipmentSlot.MAINHAND);
        boolean candidateTrident = candidate.getItem() instanceof ItemTrident;
        if (held.getItem() instanceof ItemTrident) {
            if (!candidateTrident || candidate.getItemDamage() >= held.getItemDamage()) {
                return;
            }
        } else if (!candidateTrident) {
            super.updateEquipmentIfNeeded(itemEntity);
            return;
        }
        if (!this.canEquipItem(candidate)) {
            return;
        }
        if (!held.isEmpty() && this.rand.nextFloat() - 0.1F < this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()]) {
            this.entityDropItem(held, 0.0F);
        }
        this.setItemStackToSlot(EntityEquipmentSlot.MAINHAND, candidate);
        this.inventoryHandsDropChances[EntityEquipmentSlot.MAINHAND.getIndex()] = 2.0F;
        this.enablePersistence();
        this.onItemPickup(itemEntity, candidate.getCount());
        itemEntity.setDead();
    }

    /** Drowned never break doors (and their navigator is not always a ground one). */
    @Override
    public void setBreakDoorsAItask(boolean enabled) {
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    /** Spawned in water, so only other entities count as in the way. */
    @Override
    public boolean isNotColliding() {
        return this.world.checkNoEntityCollision(this.getEntityBoundingBox(), this);
    }

    /** Monsters' light rule as current Java: no block light at all, and dark enough by sky and time. */
    @Override
    protected boolean isValidLightLevel() {
        BlockPos pos = new BlockPos(this.posX, this.getEntityBoundingBox().minY, this.posZ);
        return this.world.getLightFor(EnumSkyBlock.BLOCK, pos) <= 0 && super.isValidLightLevel();
    }

    @Override
    public boolean getCanSpawnHere() {
        return this.world.getDifficulty() != EnumDifficulty.PEACEFUL && this.isValidLightLevel();
    }

    public boolean shouldAttack(@Nullable EntityLivingBase target) {
        return target != null && (!this.world.isDaytime() || target.isInWater());
    }

    /** 1.14.4: swims when swimming up, or when its target is in the water. */
    public boolean wantsToSwim() {
        if (this.swimmingUp) {
            return true;
        }
        EntityLivingBase target = this.getAttackTarget();
        return target != null && target.isInWater();
    }

    public void setSwimmingUp(boolean swimmingUp) {
        this.swimmingUp = swimmingUp;
    }

    public boolean isSwimming() {
        return this.dataManager.get(SWIMMING);
    }

    @Override
    public boolean isPushedByWater() {
        return !this.isSwimming();
    }

    @Override
    public void onUpdate() {
        if (!this.world.isRemote) {
            this.updateSwimming();
        }
        super.onUpdate();
        if (this.world.isRemote) {
            this.prevSwimAnimation = this.swimAnimation;
            this.swimAnimation = this.isSwimming() ? Math.min(1.0F, this.swimAnimation + 0.09F) : Math.max(0.0F, this.swimAnimation - 0.09F);
        }
    }

    /** Server: swaps between the water and ground navigators. */
    public void updateSwimming() {
        if (this.isServerWorld() && this.isInWater() && this.wantsToSwim()) {
            this.navigator = this.waterNavigator;
            this.dataManager.set(SWIMMING, true);
        } else {
            this.navigator = this.groundNavigator;
            this.dataManager.set(SWIMMING, false);
        }
    }

    /** Client: the swim animation, 0 to 1. */
    public float getSwimAnimation(float partialTicks) {
        return this.prevSwimAnimation + (this.swimAnimation - this.prevSwimAnimation) * partialTicks;
    }

    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (this.isServerWorld() && this.isInWater() && this.wantsToSwim()) {
            this.moveRelative(strafe, vertical, forward, 0.01F);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    protected boolean isCloseToPathTarget() {
        Path path = this.getNavigator().getPath();
        if (path != null) {
            PathPoint end = path.getFinalPathPoint();
            if (end != null && this.getDistanceSq(end.x, end.y, end.z) < 4.0D) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void attackEntityWithRangedAttack(EntityLivingBase target, float distanceFactor) {
        Item item = TridentContent.trident();
        if (item == null) {
            return;
        }
        EntityTrident trident = new EntityTrident(this.world, this, new ItemStack(item));
        double dx = target.posX - this.posX;
        double dy = target.getEntityBoundingBox().minY + target.height / 3.0F - trident.posY;
        double dz = target.posZ - this.posZ;
        double horizontal = MathHelper.sqrt(dx * dx + dz * dz);
        trident.shoot(dx, dy + horizontal * 0.2F, dz, 1.6F, 14 - this.world.getDifficulty().getId() * 4);
        this.playSound(TridentSounds.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRNG().nextFloat() * 0.4F + 0.8F));
        this.world.spawnEntity(trident);
    }

    @Override
    public void setSwingingArms(boolean swingingArms) {
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return this.isInWater() ? TridentSounds.DROWNED_AMBIENT_WATER : TridentSounds.DROWNED_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return this.isInWater() ? TridentSounds.DROWNED_HURT_WATER : TridentSounds.DROWNED_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.isInWater() ? TridentSounds.DROWNED_DEATH_WATER : TridentSounds.DROWNED_DEATH;
    }

    @Override
    protected SoundEvent getStepSound() {
        return TridentSounds.DROWNED_STEP;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return TridentSounds.DROWNED_SWIM;
    }

    @Override
    protected ItemStack getSkullDrop() {
        return ItemStack.EMPTY;
    }

    @Override
    @Nullable
    protected ResourceLocation getLootTable() {
        return LOOT_TABLE;
    }

    /**
     * 1.14.4's zombie attribute bonuses for a mob made by conversion: knockback resistance, follow range
     * and, rarely, the leader bonus. A spawned zombie gets them in {@code onInitialSpawn} instead.
     */
    public static void applyAttributeBonuses(EntityZombie zombie, float difficulty) {
        Random rand = zombie.getRNG();
        zombie.getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE)
                .applyModifier(new AttributeModifier("Random spawn bonus", rand.nextDouble() * 0.05D, 0));
        double range = rand.nextDouble() * 1.5D * difficulty;
        if (range > 1.0D) {
            zombie.getEntityAttribute(SharedMonsterAttributes.FOLLOW_RANGE)
                    .applyModifier(new AttributeModifier("Random zombie-spawn bonus", range, 2));
        }
        if (rand.nextFloat() < difficulty * 0.05F) {
            zombie.getEntityAttribute(SPAWN_REINFORCEMENTS_CHANCE)
                    .applyModifier(new AttributeModifier("Leader zombie bonus", rand.nextDouble() * 0.25D + 0.5D, 0));
            zombie.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH)
                    .applyModifier(new AttributeModifier("Leader zombie bonus", rand.nextDouble() * 3.0D + 1.0D, 2));
            zombie.setBreakDoorsAItask(true);
        }
    }

    // ------------------------------------------------------------------------------------------
    // AI, ported from 1.14.4's inner classes
    // ------------------------------------------------------------------------------------------

    /** Melee only when it should attack at all (night, or the target in water). */
    static final class AIAttack extends EntityAIZombieAttack {
        private final EntityDrowned drowned;

        AIAttack(EntityDrowned drowned, double speed, boolean longMemory) {
            super(drowned, speed, longMemory);
            this.drowned = drowned;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && this.drowned.shouldAttack(this.drowned.getAttackTarget());
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && this.drowned.shouldAttack(this.drowned.getAttackTarget());
        }
    }

    /** Throws its trident every 40 ticks from within 10 blocks, spear raised. */
    static final class AITridentAttack extends EntityAIAttackRanged {
        private final EntityDrowned drowned;

        AITridentAttack(EntityDrowned drowned, double speed, int interval, float range) {
            super(drowned, speed, interval, range);
            this.drowned = drowned;
        }

        @Override
        public boolean shouldExecute() {
            return super.shouldExecute() && this.drowned.getHeldItemMainhand().getItem() instanceof ItemTrident;
        }

        @Override
        public void startExecuting() {
            super.startExecuting();
            this.drowned.setArmsRaised(true);
            this.drowned.setActiveHand(EnumHand.MAIN_HAND);
        }

        @Override
        public void resetTask() {
            super.resetTask();
            this.drowned.resetActiveHand();
            this.drowned.setArmsRaised(false);
        }
    }

    /** By night, near the surface: walk out onto a solid block with two air blocks above it. */
    static final class AIGoToBeach extends EntityAIMoveToBlock {
        private final EntityDrowned drowned;

        AIGoToBeach(EntityDrowned drowned, double speed) {
            super(drowned, speed, 8);
            this.drowned = drowned;
        }

        @Override
        public boolean shouldExecute() {
            return !this.drowned.world.isDaytime() && this.drowned.isInWater()
                    && this.drowned.posY >= this.drowned.world.getSeaLevel() - 3 && super.shouldExecute();
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            BlockPos up = pos.up();
            return world.isAirBlock(up) && world.isAirBlock(up.up()) && world.getBlockState(pos).isTopSolid();
        }

        @Override
        public void startExecuting() {
            this.drowned.setSwimmingUp(false);
            this.drowned.navigator = this.drowned.groundNavigator;
            super.startExecuting();
        }
    }

    /** By day, out of water: walk to water within 10 blocks. */
    static final class AIGoToWater extends EntityAIBase {
        private final EntityCreature creature;
        private final double speed;
        private double x;
        private double y;
        private double z;

        AIGoToWater(EntityCreature creature, double speed) {
            this.creature = creature;
            this.speed = speed;
            this.setMutexBits(1);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.creature.world.isDaytime() || this.creature.isInWater()) {
                return false;
            }
            Vec3d water = this.findWater();
            if (water == null) {
                return false;
            }
            this.x = water.x;
            this.y = water.y;
            this.z = water.z;
            return true;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.creature.getNavigator().noPath();
        }

        @Override
        public void startExecuting() {
            this.creature.getNavigator().tryMoveToXYZ(this.x, this.y, this.z, this.speed);
        }

        @Nullable
        private Vec3d findWater() {
            Random random = this.creature.getRNG();
            BlockPos at = new BlockPos(this.creature.posX, this.creature.getEntityBoundingBox().minY, this.creature.posZ);
            for (int i = 0; i < 10; ++i) {
                BlockPos p = at.add(random.nextInt(20) - 10, 2 - random.nextInt(8), random.nextInt(20) - 10);
                if (this.creature.world.getBlockState(p).getMaterial() == Material.WATER) {
                    return new Vec3d(p.getX(), p.getY(), p.getZ());
                }
            }
            return null;
        }
    }

    /** At night, deep under: swim up toward the surface. */
    static final class AISwimUp extends EntityAIBase {
        private final EntityDrowned drowned;
        private final double speed;
        private final int targetY;
        private boolean obstructed;

        AISwimUp(EntityDrowned drowned, double speed, int targetY) {
            this.drowned = drowned;
            this.speed = speed;
            this.targetY = targetY;
        }

        @Override
        public boolean shouldExecute() {
            return !this.drowned.world.isDaytime() && this.drowned.isInWater() && this.drowned.posY < this.targetY - 2;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.shouldExecute() && !this.obstructed;
        }

        @Override
        public void updateTask() {
            if (this.drowned.posY < this.targetY - 1
                    && (this.drowned.getNavigator().noPath() || this.drowned.isCloseToPathTarget())) {
                Vec3d to = RandomPositionGenerator.findRandomTargetBlockTowards(this.drowned, 4, 8,
                        new Vec3d(this.drowned.posX, this.targetY - 1, this.drowned.posZ));
                if (to == null) {
                    this.obstructed = true;
                    return;
                }
                this.drowned.getNavigator().tryMoveToXYZ(to.x, to.y, to.z, this.speed);
            }
        }

        @Override
        public void startExecuting() {
            this.drowned.setSwimmingUp(true);
            this.obstructed = false;
        }

        @Override
        public void resetTask() {
            this.drowned.setSwimmingUp(false);
        }
    }

    /** Swims toward its path point when swimming; on land, the usual walk with a little extra fall. */
    static final class MoveHelper extends EntityMoveHelper {
        private final EntityDrowned drowned;

        MoveHelper(EntityDrowned drowned) {
            super(drowned);
            this.drowned = drowned;
        }

        @Override
        public void onUpdateMoveHelper() {
            EntityLivingBase target = this.drowned.getAttackTarget();
            if (this.drowned.wantsToSwim() && this.drowned.isInWater()) {
                if (target != null && target.posY > this.drowned.posY || this.drowned.swimmingUp) {
                    this.drowned.motionY += 0.002D;
                }
                if (this.action != Action.MOVE_TO || this.drowned.getNavigator().noPath()) {
                    this.drowned.setAIMoveSpeed(0.0F);
                    return;
                }
                double dx = this.posX - this.drowned.posX;
                double dy = this.posY - this.drowned.posY;
                double dz = this.posZ - this.drowned.posZ;
                double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
                dy = dy / distance;
                float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                this.drowned.rotationYaw = this.limitAngle(this.drowned.rotationYaw, yaw, 90.0F);
                this.drowned.renderYawOffset = this.drowned.rotationYaw;
                float wanted = (float) (this.speed * this.drowned.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                float speed = this.drowned.getAIMoveSpeed() + (wanted - this.drowned.getAIMoveSpeed()) * 0.125F;
                this.drowned.setAIMoveSpeed(speed);
                this.drowned.motionX += speed * dx * 0.005D;
                this.drowned.motionY += speed * dy * 0.1D;
                this.drowned.motionZ += speed * dz * 0.005D;
            } else {
                if (!this.drowned.onGround) {
                    this.drowned.motionY -= 0.008D;
                }
                super.onUpdateMoveHelper();
            }
        }
    }
}
