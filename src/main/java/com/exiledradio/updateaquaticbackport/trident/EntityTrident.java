package com.exiledradio.updateaquaticbackport.trident;

import io.netty.buffer.ByteBuf;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.UUID;

/**
 * A thrown trident, ported from 1.14.4's {@code TridentEntity} onto 1.12.2's arrow.
 *
 * <ul>
 *   <li>8 damage, plus Impaling against aquatic mobs. It hits one entity, then bounces off and falls;
 *       it sticks in blocks like an arrow and is picked up by its thrower (or anyone, if nobody threw it).</li>
 *   <li><b>Loyalty</b>: four ticks after it lands or once it has hit something, it flies back to its
 *       thrower through blocks, faster with each level, and a Loyalty trident a player can pick up never
 *       despawns. It also comes back from the void, as in current Java.</li>
 *   <li><b>Channeling</b>: a hit in a thunderstorm on an entity under open sky calls down lightning.</li>
 *   <li>Water slows it far less than an arrow (1.14.4's drag 0.99 instead of 0.6).</li>
 * </ul>
 *
 * <p>1.12.2's arrow keeps its in-ground timer private and moves with fixed water drag, so this class
 * corrects the drag after the arrow's tick, resets the despawn timer by reflection, and does the
 * returning flight itself (1.14's no-clip flight, which the 1.12.2 arrow does not have).
 */
public class EntityTrident extends EntityArrow implements IEntityAdditionalSpawnData {

    private static final DataParameter<Byte> LOYALTY = EntityDataManager.createKey(EntityTrident.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> FOIL = EntityDataManager.createKey(EntityTrident.class, DataSerializers.BOOLEAN);

    /** 1.14.4's thrown damage. */
    public static final float THROWN_DAMAGE = 8.0F;

    /** {@code EntityArrow.ticksInGround}: the arrow despawns when it reaches 1200. */
    private static Field ticksInGround;

    private ItemStack thrownStack = ItemStack.EMPTY;
    private boolean dealtDamage;
    /** 1.14.4's no-clip state: flying back to the thrower, through blocks. */
    private boolean returning;
    public int returningTicks;
    @Nullable
    private UUID ownerId;

    public EntityTrident(World world) {
        super(world);
        if (TridentContent.trident() != null) {
            this.thrownStack = new ItemStack(TridentContent.trident());
        }
    }

    public EntityTrident(World world, EntityLivingBase thrower, ItemStack stack) {
        super(world, thrower);
        this.thrownStack = stack.copy();
        this.ownerId = thrower.getUniqueID();
        this.updateFromStack();
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(LOYALTY, (byte) 0);
        this.dataManager.register(FOIL, false);
    }

    private void updateFromStack() {
        this.dataManager.set(LOYALTY, (byte) TridentContent.loyaltyLevel(this.thrownStack));
        this.dataManager.set(FOIL, this.thrownStack.isItemEnchanted());
    }

    public int loyalty() {
        return this.dataManager.get(LOYALTY);
    }

    /** @return true if the thrown trident was enchanted, for the renderer's glint */
    public boolean isFoil() {
        return this.dataManager.get(FOIL);
    }

    public boolean dealtDamage() {
        return this.dealtDamage;
    }

    public boolean isReturning() {
        return this.returning;
    }

    public boolean isInGround() {
        return this.inGround;
    }

    public ItemStack thrownStack() {
        return this.thrownStack;
    }

    /** @return the thrower: the shooting entity, or on a server the entity saved by id after a reload */
    @Nullable
    public Entity getOwner() {
        if (this.shootingEntity == null && this.ownerId != null && this.world instanceof WorldServer) {
            this.shootingEntity = ((WorldServer) this.world).getEntityFromUuid(this.ownerId);
        }
        return this.shootingEntity;
    }

    private boolean shouldReturnToThrower(Entity owner) {
        return owner.isEntityAlive() && !(owner instanceof EntityPlayerMP && ((EntityPlayerMP) owner).isSpectator());
    }

    @Override
    public void onUpdate() {
        if (this.timeInGround > 4) {
            this.dealtDamage = true;
        }
        Entity owner = this.getOwner();
        int loyalty = this.loyalty();
        if ((this.dealtDamage || this.returning) && owner != null && loyalty > 0) {
            if (!this.shouldReturnToThrower(owner)) {
                if (!this.world.isRemote && this.pickupStatus == PickupStatus.ALLOWED) {
                    this.entityDropItem(this.getArrowStack(), 0.1F);
                }
                this.setDead();
                return;
            }
            this.returning = true;
            this.inGround = false;
            Vec3d to = new Vec3d(owner.posX - this.posX, owner.posY + owner.getEyeHeight() - this.posY, owner.posZ - this.posZ);
            this.posY += to.y * 0.015D * loyalty;
            if (this.world.isRemote) {
                this.lastTickPosY = this.posY;
            }
            double pull = 0.05D * loyalty;
            Vec3d dir = to.normalize();
            this.motionX = this.motionX * 0.95D + dir.x * pull;
            this.motionY = this.motionY * 0.95D + dir.y * pull;
            this.motionZ = this.motionZ * 0.95D + dir.z * pull;
            if (this.returningTicks == 0) {
                this.playSound(TridentSounds.TRIDENT_RETURN, 10.0F, 1.0F);
            }
            ++this.returningTicks;
        }
        if (loyalty > 0 && this.pickupStatus == PickupStatus.ALLOWED) {
            this.keepFromDespawning();
        }

        if (this.returning) {
            this.returningTick();
            return;
        }
        boolean wasInGround = this.inGround;
        super.onUpdate();
        if (!wasInGround && !this.isDead && this.isInWater()) {
            // The arrow just multiplied by its water drag of 0.6 and took 0.05 for gravity; make it 0.99.
            double k = 0.99D / 0.6D;
            this.motionX *= k;
            this.motionZ *= k;
            this.motionY = this.hasNoGravity() ? this.motionY * k : (this.motionY + 0.05D) * k - 0.05D;
        }
    }

    /** The returning flight: 1.14.4's arrow tick with no clip - no block hits, no gravity, drag 0.99. */
    private void returningTick() {
        if (!this.world.isRemote) {
            this.setFlag(6, this.isGlowing());
        }
        this.onEntityUpdate();
        if (this.isDead) {
            return;
        }
        if (this.arrowShake > 0) {
            --this.arrowShake;
        }
        this.posX += this.motionX;
        this.posY += this.motionY;
        this.posZ += this.motionZ;
        float horizontal = MathHelper.sqrt(this.motionX * this.motionX + this.motionZ * this.motionZ);
        this.rotationYaw = (float) (MathHelper.atan2(this.motionX, this.motionZ) * (180D / Math.PI));
        this.rotationPitch = (float) (MathHelper.atan2(this.motionY, horizontal) * (180D / Math.PI));
        while (this.rotationPitch - this.prevRotationPitch < -180.0F) {
            this.prevRotationPitch -= 360.0F;
        }
        while (this.rotationPitch - this.prevRotationPitch >= 180.0F) {
            this.prevRotationPitch += 360.0F;
        }
        while (this.rotationYaw - this.prevRotationYaw < -180.0F) {
            this.prevRotationYaw -= 360.0F;
        }
        while (this.rotationYaw - this.prevRotationYaw >= 180.0F) {
            this.prevRotationYaw += 360.0F;
        }
        this.rotationPitch = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * 0.2F;
        this.rotationYaw = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * 0.2F;
        this.motionX *= 0.99D;
        this.motionY *= 0.99D;
        this.motionZ *= 0.99D;
        this.setPosition(this.posX, this.posY, this.posZ);
        this.doBlockCollisions();
    }

    private void keepFromDespawning() {
        try {
            if (ticksInGround == null) {
                ticksInGround = ObfuscationReflectionHelper.findField(EntityArrow.class, "field_70252_j");
            }
            ticksInGround.setInt(this, 0);
        } catch (Exception e) {
            // Without the field the trident despawns after a minute in the ground like an arrow; nothing worse.
        }
    }

    /** Current Java: a Loyalty trident thrown into the void comes back instead of being lost. */
    @Override
    protected void outOfWorld() {
        Entity owner = this.getOwner();
        if (this.loyalty() > 0 && owner != null && this.shouldReturnToThrower(owner)) {
            this.dealtDamage = true;
            return;
        }
        super.outOfWorld();
    }

    /** Once it has hit something it passes through everything until it is picked up. */
    @Override
    @Nullable
    protected Entity findEntityOnPath(Vec3d start, Vec3d end) {
        return this.dealtDamage ? null : super.findEntityOnPath(start, end);
    }

    @Override
    protected void onHit(RayTraceResult result) {
        Entity hit = result.entityHit;
        if (hit == null) {
            super.onHit(result);
            return;
        }
        float damage = THROWN_DAMAGE;
        if (hit instanceof EntityLivingBase) {
            damage += Impaling.bonus(this.thrownStack, (EntityLivingBase) hit);
        }
        Entity owner = this.getOwner();
        DamageSource source = TridentContent.tridentDamage(this, owner == null ? this : owner);
        this.dealtDamage = true;
        SoundEvent sound = TridentSounds.TRIDENT_HIT;
        if (hit.attackEntityFrom(source, damage) && hit instanceof EntityLivingBase) {
            EntityLivingBase living = (EntityLivingBase) hit;
            if (owner instanceof EntityLivingBase) {
                EnchantmentHelper.applyThornEnchantments(living, owner);
                EnchantmentHelper.applyArthropodEnchantments((EntityLivingBase) owner, living);
            }
            this.arrowHit(living);
        }
        this.motionX *= -0.01D;
        this.motionY *= -0.1D;
        this.motionZ *= -0.01D;
        float volume = 1.0F;
        if (this.world instanceof WorldServer && this.world.isThundering()
                && TridentContent.channelingLevel(this.thrownStack) > 0) {
            BlockPos at = new BlockPos(hit);
            if (this.world.canSeeSky(at)) {
                EntityLightningBolt bolt = new EntityLightningBolt(this.world, at.getX() + 0.5D, at.getY(), at.getZ() + 0.5D, false);
                Channeling.remember(bolt, owner instanceof EntityPlayerMP ? (EntityPlayerMP) owner : null);
                this.world.addWeatherEffect(bolt);
                sound = TridentSounds.TRIDENT_THUNDER;
                volume = 5.0F;
            }
        }
        this.playSound(sound, volume, 1.0F);
    }

    /** The arrow's block-hit sound becomes the trident's. */
    @Override
    public void playSound(SoundEvent sound, float volume, float pitch) {
        super.playSound(sound == SoundEvents.ENTITY_ARROW_HIT ? TridentSounds.TRIDENT_HIT_GROUND : sound, volume, pitch);
    }

    /** Only its thrower picks it up (or anyone, when nobody threw it); a returning one too. */
    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        Entity owner = this.getOwner();
        if (owner != null && !owner.getUniqueID().equals(player.getUniqueID())) {
            return;
        }
        if (this.world.isRemote || !(this.inGround || this.returning) || this.arrowShake > 0) {
            return;
        }
        boolean take = this.pickupStatus == PickupStatus.ALLOWED
                || this.pickupStatus == PickupStatus.CREATIVE_ONLY && player.capabilities.isCreativeMode
                || this.returning && owner != null;
        if (this.pickupStatus == PickupStatus.ALLOWED && !player.inventory.addItemStackToInventory(this.getArrowStack())) {
            take = false;
        }
        if (take) {
            player.onItemPickup(this, 1);
            this.setDead();
        }
    }

    @Override
    protected ItemStack getArrowStack() {
        return this.thrownStack.copy();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean isInRangeToRenderDist(double distance) {
        return true;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setTag("Trident", this.thrownStack.writeToNBT(new NBTTagCompound()));
        compound.setBoolean("DealtDamage", this.dealtDamage);
        if (this.ownerId != null) {
            compound.setUniqueId("Owner", this.ownerId);
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if (compound.hasKey("Trident", 10)) {
            this.thrownStack = new ItemStack(compound.getCompoundTag("Trident"));
        }
        this.dealtDamage = compound.getBoolean("DealtDamage");
        if (compound.hasUniqueId("Owner")) {
            this.ownerId = compound.getUniqueId("Owner");
        }
        this.updateFromStack();
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        Entity owner = this.getOwner();
        buffer.writeInt(owner == null ? -1 : owner.getEntityId());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        int id = buffer.readInt();
        Entity owner = id < 0 ? null : this.world.getEntityByID(id);
        if (owner != null) {
            this.shootingEntity = owner;
            this.ownerId = owner.getUniqueID();
        }
    }
}
