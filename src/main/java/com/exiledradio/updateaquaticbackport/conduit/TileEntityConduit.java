package com.exiledradio.updateaquaticbackport.conduit;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import com.google.common.base.Predicate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * The conduit's tile entity, 1.14.4's {@code ConduitTileEntity} line for line.
 *
 * <h2>Every 40 ticks of world time</h2>
 * <ul>
 *   <li>Active if the 3x3x3 around it (its own block included) is all water, and at least 16 of the 42
 *       frame positions - three 5x5 rings, one in each plane through the conduit - hold prismarine (any
 *       of the three), or sea lanterns. 42 is a full frame: the eye opens and it hunts.</li>
 *   <li>Active, on the server: Conduit Power (13 s, ambient) to every player within
 *       {@code frame / 7 * 16} blocks who is in water or rain; with a full frame, 4 magic damage to one
 *       hostile mob in water or rain within 8 blocks, kept as the target until it dies or leaves.</li>
 * </ul>
 * Both sides run the check (the client draws from it); sounds come from the server, particles from the
 * client. The target goes to the client through the tile entity's update packet.
 */
public class TileEntityConduit extends TileEntity implements ITickable {

    /** Frame size at which the conduit is active. */
    public static final int MIN_FRAME = 16;
    /** A full frame: the eye opens and the conduit attacks. */
    public static final int FULL_FRAME = 42;
    /** Conduit Power's length, 13 s, refreshed every 2 s. */
    public static final int EFFECT_TICKS = 260;

    public int ticksExisted;
    private float activeRotation;
    private boolean active;
    private boolean eyeOpen;
    private final List<BlockPos> frame = new ArrayList<BlockPos>();
    @Nullable
    private EntityLivingBase target;
    @Nullable
    private UUID targetUuid;
    private long nextSoundTime;

    // ------------------------------------------------------------------------------------------
    // Saving and syncing
    // ------------------------------------------------------------------------------------------

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        this.targetUuid = compound.hasKey("target_uuid") ? NBTUtil.getUUIDFromTag(compound.getCompoundTag("target_uuid")) : null;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        if (this.target != null) {
            compound.setTag("target_uuid", NBTUtil.createUUIDTag(this.target.getUniqueID()));
        }
        return compound;
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return this.writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(this.pos, 5, this.getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        this.readFromNBT(packet.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(net.minecraft.world.World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        // Emptying or filling it with a bucket keeps the tile entity.
        return oldState.getBlock() != newState.getBlock();
    }

    // ------------------------------------------------------------------------------------------
    // Ticking
    // ------------------------------------------------------------------------------------------

    @Override
    public void update() {
        ++this.ticksExisted;
        long time = this.world.getTotalWorldTime();
        if (time % 40L == 0L) {
            this.setActive(this.shouldBeActive());
            if (!this.world.isRemote && this.isActive()) {
                this.addEffectsToPlayers();
                this.attackMobs();
            }
        }
        if (time % 80L == 0L && this.isActive()) {
            this.playSound(ConduitContent.AMBIENT);
        }
        if (time > this.nextSoundTime && this.isActive()) {
            this.nextSoundTime = time + 60L + this.world.rand.nextInt(40);
            this.playSound(ConduitContent.AMBIENT_SHORT);
        }
        if (this.world.isRemote) {
            this.updateClientTarget();
            this.spawnParticles();
            if (this.isActive()) {
                ++this.activeRotation;
            }
        }
    }

    /** @return true if the frame offset (dx, dy, dz) is one of the 42 positions that count */
    public static boolean isFramePosition(int dx, int dy, int dz) {
        int ax = Math.abs(dx);
        int ay = Math.abs(dy);
        int az = Math.abs(dz);
        return (ax > 1 || ay > 1 || az > 1)
                && (dx == 0 && (ay == 2 || az == 2) || dy == 0 && (ax == 2 || az == 2) || dz == 0 && (ax == 2 || ay == 2));
    }

    /** @return true for prismarine (plain, bricks or dark) and sea lanterns */
    public static boolean isFrameBlock(IBlockState state) {
        return state.getBlock() == Blocks.PRISMARINE || state.getBlock() == Blocks.SEA_LANTERN;
    }

    private boolean shouldBeActive() {
        this.frame.clear();
        for (int x = -1; x <= 1; ++x) {
            for (int y = -1; y <= 1; ++y) {
                for (int z = -1; z <= 1; ++z) {
                    if (!Waterlogging.hasWater(this.world, this.pos.add(x, y, z))) {
                        return false;
                    }
                }
            }
        }
        for (int x = -2; x <= 2; ++x) {
            for (int y = -2; y <= 2; ++y) {
                for (int z = -2; z <= 2; ++z) {
                    if (isFramePosition(x, y, z)) {
                        BlockPos p = this.pos.add(x, y, z);
                        if (isFrameBlock(this.world.getBlockState(p))) {
                            this.frame.add(p);
                        }
                    }
                }
            }
        }
        this.eyeOpen = this.frame.size() >= FULL_FRAME;
        return this.frame.size() >= MIN_FRAME;
    }

    /** @return the effect radius for the last frame count: 16 per 7 frame blocks */
    public int range() {
        return this.frame.size() / 7 * 16;
    }

    private void addEffectsToPlayers() {
        int range = this.range();
        int x = this.pos.getX();
        int y = this.pos.getY();
        int z = this.pos.getZ();
        AxisAlignedBB box = new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).grow(range).expand(0.0D, this.world.getHeight(), 0.0D);
        for (EntityPlayer player : this.world.getEntitiesWithinAABB(EntityPlayer.class, box)) {
            if (this.pos.distanceSq(new BlockPos(player)) < (double) (range * range) && player.isWet()) {
                player.addPotionEffect(new PotionEffect(ConduitContent.CONDUIT_POWER, EFFECT_TICKS, 0, true, true));
            }
        }
    }

    private void attackMobs() {
        EntityLivingBase before = this.target;
        if (this.frame.size() < FULL_FRAME) {
            this.target = null;
        } else if (this.target == null && this.targetUuid != null) {
            this.target = this.findExistingTarget();
            this.targetUuid = null;
        } else if (this.target == null) {
            List<EntityLivingBase> list = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.areaOfEffect(),
                    new Predicate<EntityLivingBase>() {
                        @Override
                        public boolean apply(@Nullable EntityLivingBase e) {
                            return e instanceof IMob && e.isWet();
                        }
                    });
            if (!list.isEmpty()) {
                this.target = list.get(this.world.rand.nextInt(list.size()));
            }
        } else if (!this.target.isEntityAlive() || this.pos.distanceSq(new BlockPos(this.target)) >= 64.0D) {
            this.target = null;
        }

        if (this.target != null) {
            this.world.playSound(null, this.target.posX, this.target.posY, this.target.posZ, ConduitContent.ATTACK_TARGET,
                    SoundCategory.BLOCKS, 1.0F, 1.0F);
            this.target.attackEntityFrom(DamageSource.MAGIC, 4.0F);
        }
        if (before != this.target) {
            IBlockState state = this.world.getBlockState(this.pos);
            this.world.notifyBlockUpdate(this.pos, state, state, 2);
        }
    }

    private void updateClientTarget() {
        if (this.targetUuid == null) {
            this.target = null;
        } else if (this.target == null || !this.target.getUniqueID().equals(this.targetUuid)) {
            this.target = this.findExistingTarget();
            if (this.target == null) {
                this.targetUuid = null;
            }
        }
    }

    private AxisAlignedBB areaOfEffect() {
        int x = this.pos.getX();
        int y = this.pos.getY();
        int z = this.pos.getZ();
        return new AxisAlignedBB(x, y, z, x + 1, y + 1, z + 1).grow(8.0D);
    }

    @Nullable
    private EntityLivingBase findExistingTarget() {
        final UUID want = this.targetUuid;
        List<EntityLivingBase> list = this.world.getEntitiesWithinAABB(EntityLivingBase.class, this.areaOfEffect(),
                new Predicate<EntityLivingBase>() {
                    @Override
                    public boolean apply(@Nullable EntityLivingBase e) {
                        return e != null && e.getUniqueID().equals(want);
                    }
                });
        return list.size() == 1 ? list.get(0) : null;
    }

    /** Client only: nautilus particles drift from frame blocks to the conduit, and around the target. */
    private void spawnParticles() {
        Random random = this.world.rand;
        float bob = MathHelper.sin((this.ticksExisted + 35) * 0.1F) / 2.0F + 0.5F;
        bob = (bob * bob + bob) * 0.3F;
        Vec3d eye = new Vec3d(this.pos.getX() + 0.5F, this.pos.getY() + 1.5F + bob, this.pos.getZ() + 0.5F);
        for (BlockPos p : this.frame) {
            if (random.nextInt(50) == 0) {
                float fx = -0.5F + random.nextFloat();
                float fy = -2.0F + random.nextFloat();
                float fz = -0.5F + random.nextFloat();
                BlockPos rel = p.subtract(this.pos);
                com.exiledradio.updateaquaticbackport.conduit.client.ConduitClient.nautilus(this.world, eye.x, eye.y, eye.z,
                        fx + rel.getX(), fy + rel.getY(), fz + rel.getZ());
            }
        }
        if (this.target != null) {
            Vec3d at = new Vec3d(this.target.posX, this.target.posY + this.target.getEyeHeight(), this.target.posZ);
            float fx = (-0.5F + random.nextFloat()) * (3.0F + this.target.width);
            float fy = -1.0F + random.nextFloat() * this.target.height;
            float fz = (-0.5F + random.nextFloat()) * (3.0F + this.target.width);
            com.exiledradio.updateaquaticbackport.conduit.client.ConduitClient.nautilus(this.world, at.x, at.y, at.z, fx, fy, fz);
        }
    }

    // ------------------------------------------------------------------------------------------
    // State
    // ------------------------------------------------------------------------------------------

    public boolean isActive() {
        return this.active;
    }

    /** @return true with a full frame: the eye is drawn open */
    public boolean isEyeOpen() {
        return this.eyeOpen;
    }

    /** @return the frame blocks found by the last check, for tests */
    public List<BlockPos> frame() {
        return Collections.unmodifiableList(this.frame);
    }

    @Nullable
    public EntityLivingBase target() {
        return this.target;
    }

    private void setActive(boolean active) {
        if (active != this.active) {
            this.playSound(active ? ConduitContent.ACTIVATE : ConduitContent.DEACTIVATE);
        }
        this.active = active;
    }

    /** 1.14.4's rotation, in radians for the cage (the closed shell takes the same number as degrees). */
    public float getActiveRotation(float partialTicks) {
        return (this.activeRotation + partialTicks) * -0.0375F;
    }

    public void playSound(SoundEvent sound) {
        this.world.playSound(null, this.pos, sound, SoundCategory.BLOCKS, 1.0F, 1.0F);
    }

    /** Runs the 40-tick check now, as the tick would; for tests. */
    public void checkNow() {
        this.setActive(this.shouldBeActive());
        if (!this.world.isRemote && this.isActive()) {
            this.addEffectsToPlayers();
            this.attackMobs();
        }
    }

    @Override
    @net.minecraftforge.fml.relauncher.SideOnly(net.minecraftforge.fml.relauncher.Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        return new AxisAlignedBB(this.pos).grow(0.5D);
    }
}
