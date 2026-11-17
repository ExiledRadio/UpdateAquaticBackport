package com.exiledradio.updateaquaticbackport.mechanics;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityHanging;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.List;

/**
 * 1.13's item frame on a floor or a ceiling (17w50a). 1.12.2's {@code EntityHanging} takes only the four
 * horizontal facings (its facing check, its box, its saved {@code Facing}), so a frame facing up or down is
 * this subclass of the vanilla frame: everything that works with item frames (maps, comparators, rotating the
 * item, breaking, dropping, picking) works with it.
 *
 * <p>From 1.14.4's {@code ItemFrameEntity}: a frame facing up or down has pitch -90 or 90 and yaw 0; its box is
 * 12 pixels wide each way and 1 pixel thick, pressed against the block it is on; it stays while that block is
 * solid and no other hanging entity overlaps it. {@code Facing} is saved as 1.13 saves it, the facing's index
 * (0 down, 1 up, then north, south, west, east).
 */
public class EntityFloorItemFrame extends EntityItemFrame implements IEntityAdditionalSpawnData {

    public EntityFloorItemFrame(World world) {
        super(world);
    }

    public EntityFloorItemFrame(World world, BlockPos pos, EnumFacing facing) {
        super(world);
        this.hangingPosition = pos;
        this.updateFacingWithBoundingBox(facing);
    }

    @Override
    protected void updateFacingWithBoundingBox(EnumFacing facing) {
        if (facing == null) {
            return;
        }
        this.facingDirection = facing;
        if (facing.getAxis().isHorizontal()) {
            this.rotationPitch = 0.0F;
            this.rotationYaw = facing.getHorizontalIndex() * 90;
        } else {
            this.rotationPitch = -90 * facing.getAxisDirection().getOffset();
            this.rotationYaw = 0.0F;
        }
        this.prevRotationPitch = this.rotationPitch;
        this.prevRotationYaw = this.rotationYaw;
        this.updateBoundingBox();
    }

    @Override
    protected void updateBoundingBox() {
        if (this.facingDirection == null || this.hangingPosition == null) {
            return;
        }
        EnumFacing f = this.facingDirection;
        this.posX = this.hangingPosition.getX() + 0.5D - f.getXOffset() * 0.46875D;
        this.posY = this.hangingPosition.getY() + 0.5D - f.getYOffset() * 0.46875D;
        this.posZ = this.hangingPosition.getZ() + 0.5D - f.getZOffset() * 0.46875D;
        double x = this.getWidthPixels();
        double y = this.getHeightPixels();
        double z = this.getWidthPixels();
        switch (f.getAxis()) {
            case X:
                x = 1.0D;
                break;
            case Y:
                y = 1.0D;
                break;
            default:
                z = 1.0D;
        }
        x /= 32.0D;
        y /= 32.0D;
        z /= 32.0D;
        this.setEntityBoundingBox(new AxisAlignedBB(this.posX - x, this.posY - y, this.posZ - z, this.posX + x, this.posY + y, this.posZ + z));
    }

    /**
     * 1.14.4's item frame: eye height 0, so its light is read in its own block. 1.12.2's default (0.425 up) reads it
     * inside the block above a frame hanging under a ceiling, which draws it black.
     */
    @Override
    public float getEyeHeight() {
        return 0.0F;
    }

    @Override
    public boolean onValidSurface() {
        if (!this.world.getCollisionBoxes(this, this.getEntityBoundingBox()).isEmpty()) {
            return false;
        }
        BlockPos behind = this.hangingPosition.offset(this.facingDirection.getOpposite());
        IBlockState state = this.world.getBlockState(behind);
        boolean holds = state.isSideSolid(this.world, behind, this.facingDirection) || state.getMaterial().isSolid()
                || this.facingDirection.getAxis().isHorizontal() && BlockRedstoneDiode.isDiode(state);
        if (!holds) {
            return false;
        }
        List<Entity> others = this.world.getEntitiesWithinAABBExcludingEntity(this, this.getEntityBoundingBox());
        for (Entity other : others) {
            if (other instanceof EntityHanging) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        compound.setByte("Facing", (byte) this.facingDirection.getIndex());
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        // The vanilla frame reads Facing as a horizontal index; read it again as 1.13's full index.
        super.readEntityFromNBT(compound);
        this.updateFacingWithBoundingBox(EnumFacing.byIndex(compound.getByte("Facing")));
    }

    @Override
    public void writeSpawnData(ByteBuf buffer) {
        buffer.writeLong(this.hangingPosition.toLong());
        buffer.writeByte(this.facingDirection.getIndex());
    }

    @Override
    public void readSpawnData(ByteBuf buffer) {
        this.hangingPosition = BlockPos.fromLong(buffer.readLong());
        this.updateFacingWithBoundingBox(EnumFacing.byIndex(buffer.readByte()));
    }
}
