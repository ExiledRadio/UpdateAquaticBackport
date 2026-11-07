package com.exiledradio.updateaquaticbackport.bubble.client;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.bubble.BlockBubbleColumn;
import com.exiledradio.updateaquaticbackport.bubble.BoatRocking;
import com.exiledradio.updateaquaticbackport.bubble.BubbleContent;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;

/**
 * The client's side of bubble columns, registered by Forge on the client only:
 * <ul>
 *   <li>1.14.4's {@code BubbleColumnAmbientSoundHandler}: the "woosh" or "zoom" when the player enters a
 *       column (not on the first tick in a world, nor in spectator mode);</li>
 *   <li>the client's count of rocking boats ({@link BoatRocking#tickClient});</li>
 *   <li>{@link RenderBoatRocking} put in front of the boat renderer, once, on the first client tick (every
 *       renderer exists by then, other mods' included).</li>
 * </ul>
 */
@Mod.EventBusSubscriber(value = Side.CLIENT, modid = UpdateAquaticBackport.MOD_ID)
public final class BubbleClient {

    private static boolean rendererInstalled;
    private static boolean wasInside;
    private static boolean firstTick = true;
    @Nullable
    private static World lastWorld;

    private BubbleClient() {
    }

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || BubbleContent.column() == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (!rendererInstalled && mc.getRenderManager() != null) {
            rendererInstalled = true;
            RenderManager manager = mc.getRenderManager();
            Render<? extends Entity> boat = manager.entityRenderMap.get(EntityBoat.class);
            if (boat != null && !(boat instanceof RenderBoatRocking)) {
                manager.entityRenderMap.put(EntityBoat.class, new RenderBoatRocking(manager, (Render<EntityBoat>) boat));
            }
        }
        if (mc.world == null || mc.player == null) {
            lastWorld = null;
            return;
        }
        if (mc.isGamePaused()) {
            return;
        }
        if (mc.world != lastWorld) {
            lastWorld = mc.world;
            firstTick = true;
            wasInside = false;
        }
        BoatRocking.tickClient(mc.world);
        insideSound(mc.player);
    }

    private static void insideSound(EntityPlayerSP player) {
        BlockBubbleColumn column = BubbleContent.column();
        IBlockState found = null;
        AxisAlignedBB box = player.getEntityBoundingBox().grow(0.0, -0.4, 0.0).shrink(0.001);
        if (box.maxY > box.minY) {
            for (BlockPos p : BlockPos.getAllInBoxMutable(new BlockPos(box.minX, box.minY, box.minZ), new BlockPos(box.maxX, box.maxY, box.maxZ))) {
                IBlockState state = player.world.getBlockState(p);
                if (state.getBlock() == column) {
                    found = state;
                    break;
                }
            }
        }
        if (found != null) {
            if (!wasInside && !firstTick && !player.isSpectator()) {
                player.playSound(found.getValue(BlockBubbleColumn.DRAG) ? BubbleContent.WHIRLPOOL_INSIDE : BubbleContent.UPWARDS_INSIDE,
                        1.0F, 1.0F);
            }
            wasInside = true;
        } else {
            wasInside = false;
        }
        firstTick = false;
    }
}
