package com.exiledradio.updateaquaticbackport.bubble;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.MobEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * The parts of bubble columns that happen outside the block: soul sand and magma starting a column, the
 * world tick that runs their wait and the boats' rocking, and air refilling inside a column.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class BubbleEvents {

    /** 1.14.4's {@code determineNextAir}: air comes back 4 a tick, as out of water. */
    static final int AIR_REFILL = 4;

    private BubbleEvents() {
    }

    /**
     * 1.13's soul sand and magma block: placed, or with water appearing above them, they start a column
     * 20 ticks later.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        World world = event.getWorld();
        if (world.isRemote || BubbleContent.column() == null) {
            return;
        }
        IBlockState state = event.getState();
        BlockPos pos = event.getPos();
        if (BubbleColumns.isBase(state.getBlock())) {
            BubbleColumns.scheduleBase(world, pos);
        } else if (BubbleColumns.isVanillaSource(state) && BubbleColumns.isBase(world.getBlockState(pos.down()).getBlock())) {
            BubbleColumns.scheduleBase(world, pos.down());
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.world.isRemote || BubbleContent.column() == null) {
            return;
        }
        BubbleColumns.runDue(event.world, event.world.getTotalWorldTime());
        BoatRocking.tickServer(event.world);
    }

    /**
     * 1.13: a mob or player whose eyes are in a bubble column gets its air back instead of losing it.
     *
     * <p>Runs at the start of the entity's tick; vanilla's air rule comes after it in the same tick and
     * still takes its one point, so that point is added here first. The result is +4 a tick to full (seen
     * as 299 at the top, which draws as ten full bubbles), the rate of 1.14.4's {@code determineNextAir}.
     */
    @SubscribeEvent
    public static void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
        BlockBubbleColumn column = BubbleContent.column();
        EntityLivingBase entity = event.getEntityLiving();
        if (column == null || !entity.isEntityAlive()) {
            return;
        }
        BlockPos eyes = new BlockPos(entity.posX, entity.posY + entity.getEyeHeight(), entity.posZ);
        if (entity.world.getBlockState(eyes).getBlock() != column || !entity.isInsideOfMaterial(Material.WATER)) {
            return;
        }
        int max = 300;
        int air = entity.getAir();
        boolean loses = !entity.canBreatheUnderwater() && !entity.isPotionActive(MobEffects.WATER_BREATHING)
                && !(entity instanceof EntityPlayer && ((EntityPlayer) entity).capabilities.disableDamage);
        int next = air < max ? Math.min(max, air + AIR_REFILL) : air;
        if (loses) {
            next = Math.min(max, next + 1);
        }
        if (next != air) {
            entity.setAir(next);
        }
    }
}
