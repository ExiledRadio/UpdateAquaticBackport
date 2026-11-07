package com.exiledradio.updateaquaticbackport.turtle;

import com.google.common.base.Predicate;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityAgeable;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIPanic;
import net.minecraft.entity.ai.EntityAIWander;
import net.minecraft.entity.ai.EntityAIWatchClosest;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigate;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;

/**
 * The turtle, from 1.14.4's {@code TurtleEntity} with today's fixes.
 *
 * <ul>
 *   <li>30 health, speed 0.25, 1.2 x 0.4 (a baby is 0.3 of that); breathes underwater, is not pushed by
 *       water, cannot be leashed; lightning kills it outright and it drops a bowl.</li>
 *   <li>Remembers the beach where it spawned or hatched (its home). Fed seagrass, two turtles mate and one
 *       carries eggs home, digs for a while on sand and lays 1-4 eggs. Both wait 5 minutes before mating
 *       again (today's rule; 1.13 had no wait).</li>
 *   <li>Walks slowly on land and swims fast, wandering far out to sea; tempted by seagrass; panics toward
 *       water when hurt.</li>
 *   <li>A baby grown up drops a scute. An adult drops 0-2 seagrass (Looting adds up to 1 per level).</li>
 *   <li>The second and later turtles of a spawned group are babies one time in 20, as for every animal today.</li>
 * </ul>
 *
 * Its goals are 1.14's, ported as nested classes; the navigator and node processor are
 * {@link TurtleNavigator} and {@link TurtleNodeProcessor}.
 */
public class EntityTurtle extends EntityAnimal {

    private static final DataParameter<BlockPos> HOME_POS = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> HAS_EGG = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> IS_DIGGING = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<BlockPos> TRAVEL_POS = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Boolean> GOING_HOME = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> TRAVELLING = EntityDataManager.createKey(EntityTurtle.class, DataSerializers.BOOLEAN);

    /** What hunts baby turtles goes for: a baby that is out of the water. */
    public static final Predicate<EntityLivingBase> TARGET_DRY_BABY = new Predicate<EntityLivingBase>() {
        @Override
        public boolean apply(@Nullable EntityLivingBase entity) {
            return entity != null && entity.isChild() && !entity.isInWater();
        }
    };

    /** Today's chance for each later member of a spawned group to be a baby. */
    private static final float BABY_SPAWN_CHANCE = 0.05F;

    /** Ticks spent digging the nest; 1.14's {@code isDigging} counter. */
    private int digging;
    /** 1.14's turtle shambles every 0.15 blocks walked, not every block. */
    private float nextShamble;

    public EntityTurtle(World world) {
        super(world);
        this.setSize(1.2F, 0.4F);
        this.moveHelper = new MoveHelper(this);
        this.stepHeight = 1.0F;
        this.spawnableBlock = Blocks.SAND;
    }

    // --- synced state ----------------------------------------------------------------------------------

    public void setHome(BlockPos pos) {
        this.dataManager.set(HOME_POS, pos);
    }

    public BlockPos getHome() {
        return this.dataManager.get(HOME_POS);
    }

    void setTravelPos(BlockPos pos) {
        this.dataManager.set(TRAVEL_POS, pos);
    }

    BlockPos getTravelPos() {
        return this.dataManager.get(TRAVEL_POS);
    }

    public boolean hasEgg() {
        return this.dataManager.get(HAS_EGG);
    }

    public void setHasEgg(boolean egg) {
        this.dataManager.set(HAS_EGG, egg);
    }

    public boolean isDigging() {
        return this.dataManager.get(IS_DIGGING);
    }

    void setDigging(boolean dig) {
        this.digging = dig ? 1 : 0;
        this.dataManager.set(IS_DIGGING, dig);
    }

    public boolean isGoingHome() {
        return this.dataManager.get(GOING_HOME);
    }

    void setGoingHome(boolean home) {
        this.dataManager.set(GOING_HOME, home);
    }

    public boolean isTravelling() {
        return this.dataManager.get(TRAVELLING);
    }

    void setTravelling(boolean travelling) {
        this.dataManager.set(TRAVELLING, travelling);
    }

    private boolean homeWithin(double distance) {
        return getHome().distanceSqToCenter(this.posX, this.posY, this.posZ) < distance * distance;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(HOME_POS, BlockPos.ORIGIN);
        this.dataManager.register(HAS_EGG, false);
        this.dataManager.register(TRAVEL_POS, BlockPos.ORIGIN);
        this.dataManager.register(GOING_HOME, false);
        this.dataManager.register(TRAVELLING, false);
        this.dataManager.register(IS_DIGGING, false);
    }

    /** Same keys as 1.13's turtle. */
    @Override
    public void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        tag.setInteger("HomePosX", getHome().getX());
        tag.setInteger("HomePosY", getHome().getY());
        tag.setInteger("HomePosZ", getHome().getZ());
        tag.setBoolean("HasEgg", hasEgg());
        tag.setInteger("TravelPosX", getTravelPos().getX());
        tag.setInteger("TravelPosY", getTravelPos().getY());
        tag.setInteger("TravelPosZ", getTravelPos().getZ());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound tag) {
        setHome(new BlockPos(tag.getInteger("HomePosX"), tag.getInteger("HomePosY"), tag.getInteger("HomePosZ")));
        super.readEntityFromNBT(tag);
        setHasEgg(tag.getBoolean("HasEgg"));
        setTravelPos(new BlockPos(tag.getInteger("TravelPosX"), tag.getInteger("TravelPosY"), tag.getInteger("TravelPosZ")));
    }

    /** The data a spawned group shares: how many of it there are so far. */
    static final class GroupData implements IEntityLivingData {
        int size;
    }

    @Override
    @Nullable
    public IEntityLivingData onInitialSpawn(DifficultyInstance difficulty, @Nullable IEntityLivingData data) {
        setHome(new BlockPos(this));
        setTravelPos(BlockPos.ORIGIN);
        GroupData group = data instanceof GroupData ? (GroupData) data : new GroupData();
        if (group.size > 0 && this.rand.nextFloat() <= BABY_SPAWN_CHANCE) {
            setGrowingAge(-24000);
        }
        group.size++;
        return super.onInitialSpawn(difficulty, group);
    }

    /** 1.14.4: on sand (the animal check, with sand as this animal's block), lit, below sea level + 4. */
    @Override
    public boolean getCanSpawnHere() {
        return MathHelper.floor(getEntityBoundingBox().minY) < this.world.getSeaLevel() + 4 && super.getCanSpawnHere();
    }

    // --- AI --------------------------------------------------------------------------------------------

    @Override
    protected void initEntityAI() {
        this.tasks.addTask(0, new PanicGoal(this, 1.2D));
        this.tasks.addTask(1, new MateGoal(this, 1.0D));
        this.tasks.addTask(1, new LayEggGoal(this, 1.0D));
        this.tasks.addTask(2, new TemptGoal(this, 1.1D));
        this.tasks.addTask(3, new GoToWaterGoal(this, 1.0D));
        this.tasks.addTask(4, new GoHomeGoal(this, 1.0D));
        this.tasks.addTask(7, new TravelGoal(this, 1.0D));
        this.tasks.addTask(8, new EntityAIWatchClosest(this, EntityPlayer.class, 8.0F));
        this.tasks.addTask(9, new WanderGoal(this, 1.0D, 100));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(30.0D);
        this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.25D);
    }

    @Override
    protected PathNavigate createNavigator(World world) {
        return new TurtleNavigator(this, world);
    }

    @Override
    public float getBlockPathWeight(BlockPos pos) {
        if (!isGoingHome() && this.world.getBlockState(pos).getMaterial() == Material.WATER) {
            return 10.0F;
        }
        return this.world.getBlockState(pos.down()).getBlock() == Blocks.SAND ? 10.0F : this.world.getLightBrightness(pos) - 0.5F;
    }

    // --- breeding --------------------------------------------------------------------------------------

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        Item seagrass = TurtleContent.seagrass();
        return seagrass != null && !stack.isEmpty() && stack.getItem() == seagrass;
    }

    /** 1.14's {@code canBreed}: a turtle carrying eggs takes no more seagrass for breeding. */
    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (hasEgg() && !isChild() && isBreedingItem(player.getHeldItem(hand))) {
            return false;
        }
        return super.processInteract(player, hand);
    }

    @Override
    @Nullable
    public EntityAgeable createChild(EntityAgeable other) {
        return new EntityTurtle(this.world);
    }

    @Override
    public void setScaleForAge(boolean child) {
        // 1.14: a baby's render scale, and so its hitbox, is 0.3 of an adult's.
        this.setScale(child ? 0.3F : 1.0F);
    }

    @Override
    protected void onGrowingAdult() {
        super.onGrowingAdult();
        Item scute = TurtleContent.scute();
        if (!isChild() && scute != null && this.world.getGameRules().getBoolean("doMobLoot")) {
            this.dropItem(scute, 1);
        }
    }

    // --- movement --------------------------------------------------------------------------------------

    @Override
    public boolean isPushedByWater() {
        return false;
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    /** 1.14.4: in water, its own swimming (no water drag but its own), sinking slowly unless heading home close by. */
    @Override
    public void travel(float strafe, float vertical, float forward) {
        if (isServerWorld() && isInWater()) {
            this.moveRelative(strafe, vertical, forward, 0.1F);
            this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
            this.motionX *= 0.9D;
            this.motionY *= 0.9D;
            this.motionZ *= 0.9D;
            if (getAttackTarget() == null && (!isGoingHome() || !homeWithin(20.0D))) {
                this.motionY -= 0.005D;
            }
        } else {
            super.travel(strafe, vertical, forward);
        }
    }

    @Override
    public void onLivingUpdate() {
        super.onLivingUpdate();
        if (isEntityAlive() && isDigging() && this.digging >= 1 && this.digging % 5 == 0) {
            BlockPos pos = new BlockPos(this);
            if (this.world.getBlockState(pos.down()).getBlock() == Blocks.SAND) {
                this.world.playEvent(2001, pos, Block.getStateId(Blocks.SAND.getDefaultState()));
            }
        }
        if (!this.world.isRemote && this.onGround && !isInWater() && this.distanceWalkedOnStepModified > this.nextShamble) {
            this.nextShamble = this.distanceWalkedOnStepModified + 0.15F;
            IBlockState below = this.world.getBlockState(new BlockPos(this.posX, this.posY - 0.2D, this.posZ));
            if (below.getMaterial() != Material.AIR) {
                this.playSound(isChild() ? TurtleSounds.SHAMBLE_BABY : TurtleSounds.SHAMBLE, 0.15F, 1.0F);
            }
        }
    }

    // --- sounds ----------------------------------------------------------------------------------------

    @Override
    public int getTalkInterval() {
        return 200;
    }

    @Override
    @Nullable
    protected SoundEvent getAmbientSound() {
        return !isInWater() && this.onGround && !isChild() ? TurtleSounds.AMBIENT_LAND : null;
    }

    @Override
    protected SoundEvent getSwimSound() {
        return TurtleSounds.SWIM;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isChild() ? TurtleSounds.HURT_BABY : TurtleSounds.HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isChild() ? TurtleSounds.DEATH_BABY : TurtleSounds.DEATH;
    }

    /** Steps are played by {@link #onLivingUpdate} at 1.14's pace instead. */
    @Override
    protected void playStepSound(BlockPos pos, Block block) {
    }

    // --- death -----------------------------------------------------------------------------------------

    @Override
    public void onStruckByLightning(EntityLightningBolt bolt) {
        this.attackEntityFrom(DamageSource.LIGHTNING_BOLT, Float.MAX_VALUE);
    }

    /** 1.14.4's loot table: 0-2 seagrass plus Looting, and a bowl when lightning killed it. Babies drop nothing. */
    @Override
    protected void dropLoot(boolean recentlyHit, int looting, DamageSource source) {
        Item seagrass = TurtleContent.seagrass();
        if (seagrass != null) {
            int count = this.rand.nextInt(3);
            if (looting > 0) {
                count += Math.round(looting * this.rand.nextFloat());
            }
            if (count > 0) {
                this.dropItem(seagrass, count);
            }
        }
        if (source == DamageSource.LIGHTNING_BOLT) {
            this.dropItem(Items.BOWL, 1);
        }
    }

    // ===================================================================================================
    // Goals (1.14.4's, by the same names)
    // ===================================================================================================

    static boolean isWater(World world, BlockPos pos) {
        Block block = world.getBlockState(pos).getBlock();
        return block == Blocks.WATER || block == Blocks.FLOWING_WATER;
    }

    /** Hurt or burning: toward the nearest water within 7, else anywhere. */
    static final class PanicGoal extends EntityAIPanic {
        PanicGoal(EntityTurtle turtle, double speed) {
            super(turtle, speed);
        }

        @Override
        public boolean shouldExecute() {
            if (this.creature.getRevengeTarget() == null && !this.creature.isBurning()) {
                return false;
            }
            BlockPos water = nearestWater(this.creature, 7, 4);
            if (water != null) {
                this.randPosX = water.getX();
                this.randPosY = water.getY();
                this.randPosZ = water.getZ();
                return true;
            }
            return this.findRandomPosition();
        }

        @Nullable
        private static BlockPos nearestWater(net.minecraft.entity.EntityCreature turtle, int horizontal, int vertical) {
            BlockPos at = new BlockPos(turtle);
            float best = horizontal * horizontal * vertical * 2;
            BlockPos found = null;
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int x = at.getX() - horizontal; x <= at.getX() + horizontal; ++x) {
                for (int y = at.getY() - vertical; y <= at.getY() + vertical; ++y) {
                    for (int z = at.getZ() - horizontal; z <= at.getZ() + horizontal; ++z) {
                        pos.setPos(x, y, z);
                        if (turtle.world.getBlockState(pos).getMaterial() == Material.WATER) {
                            float d = (x - at.getX()) * (x - at.getX()) + (y - at.getY()) * (y - at.getY()) + (z - at.getZ()) * (z - at.getZ());
                            if (d < best) {
                                best = d;
                                found = pos.toImmutable();
                            }
                        }
                    }
                }
            }
            return found;
        }
    }

    /** Breeding: no baby, but one of the pair carries eggs. */
    static final class MateGoal extends EntityAIBase {
        private final EntityTurtle turtle;
        private final double speed;
        private EntityTurtle mate;
        private int delay;

        MateGoal(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (!this.turtle.isInLove() || this.turtle.hasEgg()) {
                return false;
            }
            this.mate = nearbyMate();
            return this.mate != null;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.mate.isEntityAlive() && this.mate.isInLove() && this.delay < 60;
        }

        @Override
        public void resetTask() {
            this.mate = null;
            this.delay = 0;
        }

        @Override
        public void updateTask() {
            this.turtle.getLookHelper().setLookPositionWithEntity(this.mate, 10.0F, this.turtle.getVerticalFaceSpeed());
            this.turtle.getNavigator().tryMoveToEntityLiving(this.mate, this.speed);
            ++this.delay;
            if (this.delay >= 60 && this.turtle.getDistanceSq(this.mate) < 9.0D) {
                breed();
            }
        }

        @Nullable
        private EntityTurtle nearbyMate() {
            List<EntityTurtle> list = this.turtle.world.getEntitiesWithinAABB(EntityTurtle.class, this.turtle.getEntityBoundingBox().grow(8.0D));
            double best = Double.MAX_VALUE;
            EntityTurtle found = null;
            for (EntityTurtle other : list) {
                if (this.turtle.canMateWith(other) && this.turtle.getDistanceSq(other) < best) {
                    found = other;
                    best = this.turtle.getDistanceSq(other);
                }
            }
            return found;
        }

        private void breed() {
            EntityPlayerMP player = this.turtle.getLoveCause();
            if (player == null && this.mate.getLoveCause() != null) {
                player = this.mate.getLoveCause();
            }
            if (player != null) {
                player.addStat(StatList.ANIMALS_BRED);
                CriteriaTriggers.BRED_ANIMALS.trigger(player, this.turtle, this.mate, null);
            }
            this.turtle.setHasEgg(true);
            // Today's turtles wait five minutes before breeding again, as every other animal.
            this.turtle.setGrowingAge(6000);
            this.mate.setGrowingAge(6000);
            this.turtle.resetInLove();
            this.mate.resetInLove();
            Random random = this.turtle.getRNG();
            if (this.turtle.world.getGameRules().getBoolean("doMobLoot")) {
                this.turtle.world.spawnEntity(new EntityXPOrb(this.turtle.world, this.turtle.posX, this.turtle.posY, this.turtle.posZ,
                        random.nextInt(7) + 1));
            }
        }
    }

    /** Carrying eggs within 9 of home: find sand with air above, dig for 200 ticks, lay 1-4 eggs. */
    static final class LayEggGoal extends MoveToBlockGoal {
        private final EntityTurtle turtle;

        LayEggGoal(EntityTurtle turtle, double speed) {
            super(turtle, speed, 16);
            this.turtle = turtle;
        }

        @Override
        public boolean shouldExecute() {
            return this.turtle.hasEgg() && this.turtle.homeWithin(9.0D) && super.shouldExecute();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return super.shouldContinueExecuting() && this.turtle.hasEgg() && this.turtle.homeWithin(9.0D);
        }

        @Override
        public void updateTask() {
            super.updateTask();
            BlockPos at = new BlockPos(this.turtle);
            if (!this.turtle.isInWater() && getIsAboveDestination()) {
                if (this.turtle.digging < 1) {
                    this.turtle.setDigging(true);
                } else if (this.turtle.digging > 200) {
                    World world = this.turtle.world;
                    world.playSound(null, at, TurtleSounds.LAY_EGG, SoundCategory.BLOCKS, 0.3F, 0.9F + world.rand.nextFloat() * 0.2F);
                    BlockTurtleEgg egg = TurtleContent.turtleEgg();
                    if (egg != null) {
                        world.setBlockState(this.destinationBlock.up(),
                                egg.getDefaultState().withProperty(BlockTurtleEgg.EGGS, this.turtle.rand.nextInt(4) + 1), 3);
                    }
                    this.turtle.setHasEgg(false);
                    this.turtle.setDigging(false);
                    this.turtle.setInLove(null);
                }
                if (this.turtle.isDigging()) {
                    this.turtle.digging++;
                }
            }
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return world.isAirBlock(pos.up()) && world.getBlockState(pos).getBlock() == Blocks.SAND;
        }
    }

    /** Follows a player within 10 holding seagrass in either hand. */
    static final class TemptGoal extends EntityAIBase {
        private final EntityTurtle turtle;
        private final double speed;
        private EntityPlayer tempter;
        private int cooldown;

        TemptGoal(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute() {
            if (this.cooldown > 0) {
                --this.cooldown;
                return false;
            }
            this.tempter = this.turtle.world.getClosestPlayerToEntity(this.turtle, 10.0D);
            return this.tempter != null && (this.turtle.isBreedingItem(this.tempter.getHeldItemMainhand())
                    || this.turtle.isBreedingItem(this.tempter.getHeldItemOffhand()));
        }

        @Override
        public boolean shouldContinueExecuting() {
            return shouldExecute();
        }

        @Override
        public void resetTask() {
            this.tempter = null;
            this.turtle.getNavigator().clearPath();
            this.cooldown = 100;
        }

        @Override
        public void updateTask() {
            this.turtle.getLookHelper().setLookPositionWithEntity(this.tempter, this.turtle.getHorizontalFaceSpeed() + 20,
                    this.turtle.getVerticalFaceSpeed());
            if (this.turtle.getDistanceSq(this.tempter) < 6.25D) {
                this.turtle.getNavigator().clearPath();
            } else {
                this.turtle.getNavigator().tryMoveToEntityLiving(this.tempter, this.speed);
            }
        }
    }

    /** On land: back to water within 24 (babies always; adults unless heading home or carrying eggs). */
    static final class GoToWaterGoal extends MoveToBlockGoal {
        private final EntityTurtle turtle;

        GoToWaterGoal(EntityTurtle turtle, double speed) {
            // 1.14 asks isChild() here, at construction, when no turtle is a baby yet: always the given speed.
            super(turtle, speed, 24);
            this.turtle = turtle;
            this.verticalSearchStart = -1;
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.turtle.isInWater() && this.timeoutCounter <= 1200 && shouldMoveTo(this.turtle.world, this.destinationBlock);
        }

        @Override
        public boolean shouldExecute() {
            if (this.turtle.isChild() && !this.turtle.isInWater()) {
                return super.shouldExecute();
            }
            return !this.turtle.isGoingHome() && !this.turtle.isInWater() && !this.turtle.hasEgg() && super.shouldExecute();
        }

        @Override
        public boolean shouldMove() {
            return this.timeoutCounter % 160 == 0;
        }

        @Override
        protected boolean shouldMoveTo(World world, BlockPos pos) {
            return isWater(world, pos);
        }
    }

    /** Back to the home beach: always when carrying eggs, otherwise now and then when more than 64 away. */
    static final class GoHomeGoal extends EntityAIBase {
        private final EntityTurtle turtle;
        private final double speed;
        private boolean noPath;
        private int closeTicks;

        GoHomeGoal(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
        }

        @Override
        public boolean shouldExecute() {
            if (this.turtle.isChild()) {
                return false;
            }
            if (this.turtle.hasEgg()) {
                return true;
            }
            return this.turtle.getRNG().nextInt(700) == 0 && !this.turtle.homeWithin(64.0D);
        }

        @Override
        public void startExecuting() {
            this.turtle.setGoingHome(true);
            this.noPath = false;
            this.closeTicks = 0;
        }

        @Override
        public void resetTask() {
            this.turtle.setGoingHome(false);
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.turtle.homeWithin(7.0D) && !this.noPath && this.closeTicks <= 600;
        }

        @Override
        public void updateTask() {
            BlockPos home = this.turtle.getHome();
            boolean close = this.turtle.homeWithin(16.0D);
            if (close) {
                ++this.closeTicks;
            }
            if (this.turtle.getNavigator().noPath()) {
                Vec3d target = new Vec3d(home.getX(), home.getY(), home.getZ());
                Vec3d next = RandomPositions.findRandomTargetTowardsScaled(this.turtle, 16, 3, target, Math.PI / 10);
                if (next == null) {
                    next = RandomPositions.findRandomTargetBlockTowards(this.turtle, 8, 7, target);
                }
                if (next != null && !close && !isWater(this.turtle.world, new BlockPos(next))) {
                    next = RandomPositions.findRandomTargetBlockTowards(this.turtle, 16, 5, target);
                }
                if (next == null) {
                    this.noPath = true;
                    return;
                }
                this.turtle.getNavigator().tryMoveToXYZ(next.x, next.y, next.z, this.speed);
            }
        }
    }

    /** In water and free: heads for a point up to 512 away, a stretch at a time. */
    static final class TravelGoal extends EntityAIBase {
        private final EntityTurtle turtle;
        private final double speed;
        private boolean noPath;

        TravelGoal(EntityTurtle turtle, double speed) {
            this.turtle = turtle;
            this.speed = speed;
        }

        @Override
        public boolean shouldExecute() {
            return !this.turtle.isGoingHome() && !this.turtle.hasEgg() && this.turtle.isInWater();
        }

        @Override
        public void startExecuting() {
            Random random = this.turtle.getRNG();
            int dx = random.nextInt(1025) - 512;
            int dy = random.nextInt(9) - 4;
            int dz = random.nextInt(1025) - 512;
            if (dy + this.turtle.posY > this.turtle.world.getSeaLevel() - 1) {
                dy = 0;
            }
            this.turtle.setTravelPos(new BlockPos(dx + this.turtle.posX, dy + this.turtle.posY, dz + this.turtle.posZ));
            this.turtle.setTravelling(true);
            this.noPath = false;
        }

        @Override
        public void updateTask() {
            if (this.turtle.getNavigator().noPath()) {
                BlockPos travel = this.turtle.getTravelPos();
                Vec3d target = new Vec3d(travel.getX(), travel.getY(), travel.getZ());
                Vec3d next = RandomPositions.findRandomTargetTowardsScaled(this.turtle, 16, 3, target, Math.PI / 10);
                if (next == null) {
                    next = RandomPositions.findRandomTargetBlockTowards(this.turtle, 8, 7, target);
                }
                if (next != null) {
                    int x = MathHelper.floor(next.x);
                    int z = MathHelper.floor(next.z);
                    if (!this.turtle.world.isAreaLoaded(new BlockPos(x - 34, 0, z - 34), new BlockPos(x + 34, 0, z + 34))) {
                        next = null;
                    }
                }
                if (next == null) {
                    this.noPath = true;
                    return;
                }
                this.turtle.getNavigator().tryMoveToXYZ(next.x, next.y, next.z, this.speed);
            }
        }

        @Override
        public boolean shouldContinueExecuting() {
            return !this.turtle.getNavigator().noPath() && !this.noPath && !this.turtle.isGoingHome() && !this.turtle.isInLove()
                    && !this.turtle.hasEgg();
        }

        @Override
        public void resetTask() {
            this.turtle.setTravelling(false);
            super.resetTask();
        }
    }

    /** On land, free: an ordinary wander, now and then. */
    static final class WanderGoal extends EntityAIWander {
        private final EntityTurtle turtle;

        WanderGoal(EntityTurtle turtle, double speed, int chance) {
            super(turtle, speed, chance);
            this.turtle = turtle;
        }

        @Override
        public boolean shouldExecute() {
            return !this.turtle.isInWater() && !this.turtle.isGoingHome() && !this.turtle.hasEgg() && super.shouldExecute();
        }

        @Override
        @Nullable
        protected Vec3d getPosition() {
            return RandomPositions.findRandomTarget(this.turtle, 10, 7);
        }
    }

    /** 1.14.4's turtle movement: slow on land, faster in water near home, slower still for babies. */
    static final class MoveHelper extends EntityMoveHelper {
        private final EntityTurtle turtle;

        MoveHelper(EntityTurtle turtle) {
            super(turtle);
            this.turtle = turtle;
        }

        private void updateSpeed() {
            if (this.turtle.isInWater()) {
                this.turtle.motionY += 0.005D;
                if (!this.turtle.homeWithin(16.0D)) {
                    this.turtle.setAIMoveSpeed(Math.max(this.turtle.getAIMoveSpeed() / 2.0F, 0.08F));
                }
                if (this.turtle.isChild()) {
                    this.turtle.setAIMoveSpeed(Math.max(this.turtle.getAIMoveSpeed() / 3.0F, 0.06F));
                }
            } else if (this.turtle.onGround) {
                this.turtle.setAIMoveSpeed(Math.max(this.turtle.getAIMoveSpeed() / 2.0F, 0.06F));
            }
        }

        @Override
        public void onUpdateMoveHelper() {
            updateSpeed();
            if (this.action == EntityMoveHelper.Action.MOVE_TO && !this.turtle.getNavigator().noPath()) {
                double dx = this.posX - this.turtle.posX;
                double dy = this.posY - this.turtle.posY;
                double dz = this.posZ - this.turtle.posZ;
                double distance = MathHelper.sqrt(dx * dx + dy * dy + dz * dz);
                if (distance < 1.0E-7D) {
                    this.turtle.setAIMoveSpeed(0.0F);
                    return;
                }
                dy /= distance;
                float yaw = (float) (MathHelper.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
                this.turtle.rotationYaw = this.limitAngle(this.turtle.rotationYaw, yaw, 90.0F);
                this.turtle.renderYawOffset = this.turtle.rotationYaw;
                float target = (float) (this.speed * this.turtle.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue());
                float current = this.turtle.getAIMoveSpeed();
                this.turtle.setAIMoveSpeed(current + 0.125F * (target - current));
                this.turtle.motionY += this.turtle.getAIMoveSpeed() * dy * 0.1D;
            } else {
                this.turtle.setAIMoveSpeed(0.0F);
            }
        }
    }
}
