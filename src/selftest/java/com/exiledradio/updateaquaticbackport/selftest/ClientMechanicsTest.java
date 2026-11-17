package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.mechanics.EntityFloorItemFrame;

import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockStairs;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.ForgeHooks;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * {@code runClient -Pselftest=clientmech}: the mechanics stream on a real client. In a flat Creative world, floor
 * and ceiling item frames (with items) and stairs, a corner fence and a hopper are built in front of the player,
 * who looks at each block in turn; screenshots ({@code eclipse/screenshots/ua_mech_*.png}) show the frames lying
 * flat and the shaped outlines. Checks that the client sees the floor frames and that the crosshair is on the
 * block meant. Logs {@code CLIENT SELFTEST DONE pass=N fail=M}, then closes.
 */
@SideOnly(Side.CLIENT)
public final class ClientMechanicsTest {

    private int ticks;
    private int pass;
    private int fail;
    private int step = -1;
    private int wait;
    private boolean done;

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ClientMechanicsTest());
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST mechanics registered");
    }

    private void ok(boolean condition, String what) {
        if (condition) {
            this.pass++;
            UpdateAquaticBackport.LOGGER.info("MECH ok {}", what);
        } else {
            this.fail++;
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST FAIL {}", what);
        }
    }

    private static void shot(Minecraft mc, String name) {
        net.minecraft.util.ScreenShotHelper.saveScreenshot(mc.gameDir, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
        UpdateAquaticBackport.LOGGER.info("MECH screenshot {}", name);
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || this.done) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            if (this.step < 0) {
                if (!(mc.currentScreen instanceof GuiMainMenu) || ++this.ticks < 100) {
                    return;
                }
                mc.gameSettings.pauseOnLostFocus = false;
                mc.gameSettings.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
                FileUtils.deleteDirectory(new File(mc.gameDir, "saves/uamech"));
                WorldSettings settings = new WorldSettings(1L, GameType.CREATIVE, false, false, WorldType.FLAT);
                settings.enableCommands();
                mc.launchIntegratedServer("uamech", "uamech", settings);
                this.step = 0;
                return;
            }
            if (mc.player == null || mc.world == null || mc.getIntegratedServer() == null) {
                return;
            }
            if (this.wait > 0) {
                this.wait--;
                return;
            }
            run(mc);
        } catch (Throwable t) {
            UpdateAquaticBackport.LOGGER.error("CLIENT SELFTEST threw", t);
            this.fail++;
            finish(mc);
        }
    }

    private static EntityPlayerMP serverPlayer(Minecraft mc) {
        IntegratedServer server = mc.getIntegratedServer();
        return server.getPlayerList().getPlayerByUUID(mc.player.getUniqueID());
    }

    /** Moves the player (on the server, which tells the client) and points the camera. */
    private static void look(Minecraft mc, double x, double y, double z, float yaw, float pitch) {
        final EntityPlayerMP p = serverPlayer(mc);
        mc.getIntegratedServer().addScheduledTask(() -> p.connection.setPlayerLocation(x, y, z, yaw, pitch));
    }

    private void run(Minecraft mc) {
        switch (this.step++) {
            case 0:
                this.wait = 60;
                return;
            case 1: {
                mc.gameSettings.hideGUI = false;
                final IntegratedServer server = mc.getIntegratedServer();
                server.addScheduledTask(() -> {
                    WorldServer w = server.getWorld(0);
                    w.setWorldTime(6000);
                    w.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
                    int y = 4;
                    // Floor frames in a row at z 3, a ceiling frame under a stone slab block at z 6.
                    for (int x = -2; x <= 2; x++) {
                        w.setBlockState(new BlockPos(x, y - 1, 3), Blocks.STONE.getDefaultState());
                    }
                    ItemStack[] items = {new ItemStack(Items.DIAMOND_SWORD), new ItemStack(Items.COMPASS), new ItemStack(Blocks.RED_FLOWER)};
                    for (int i = 0; i < 3; i++) {
                        EntityFloorItemFrame f = new EntityFloorItemFrame(w, new BlockPos(-1 + i, y, 3), EnumFacing.UP);
                        f.setDisplayedItem(items[i]);
                        w.spawnEntity(f);
                    }
                    w.setBlockState(new BlockPos(0, y + 4, 5), Blocks.STONE.getDefaultState());
                    EntityFloorItemFrame ceiling = new EntityFloorItemFrame(w, new BlockPos(0, y + 3, 5), EnumFacing.DOWN);
                    ceiling.setDisplayedItem(new ItemStack(Items.GOLDEN_APPLE));
                    w.spawnEntity(ceiling);
                    // Blocks to outline, at z 10.
                    w.setBlockState(new BlockPos(-3, y, 10), Blocks.OAK_STAIRS.getDefaultState().withProperty(BlockStairs.FACING, EnumFacing.SOUTH));
                    w.setBlockState(new BlockPos(0, y, 10), Blocks.OAK_FENCE.getDefaultState());
                    w.setBlockState(new BlockPos(0, y, 11), Blocks.OAK_FENCE.getDefaultState());
                    w.setBlockState(new BlockPos(1, y, 10), Blocks.OAK_FENCE.getDefaultState());
                    w.setBlockState(new BlockPos(3, y, 10), Blocks.HOPPER.getDefaultState().withProperty(BlockHopper.FACING, EnumFacing.DOWN));
                });
                look(mc, 0.5, y(), 0.5, 0.0F, 45.0F);
                this.wait = 60;
                return;
            }
            case 2: {
                int frames = mc.world.getEntities(EntityFloorItemFrame.class, e -> true).size();
                ok(frames == 4, "the client has the four floor and ceiling frames: " + frames);
                shot(mc, "ua_mech_floor_frames");
                look(mc, 0.5, y(), 3.5, 0.0F, -60.0F);
                this.wait = 30;
                return;
            }
            case 3:
                shot(mc, "ua_mech_ceiling_frame");
                look(mc, -2.5, y(), 8.0, 0.0F, 30.0F);
                this.wait = 30;
                return;
            case 4:
                checkTarget(mc, new BlockPos(-3, 4, 10), "the stairs");
                shot(mc, "ua_mech_outline_stairs");
                look(mc, 0.5, y() + 1, 8.6, 0.0F, 40.0F);
                this.wait = 30;
                return;
            case 5:
                checkTarget(mc, new BlockPos(0, 4, 10), "the corner fence");
                shot(mc, "ua_mech_outline_fence");
                ModConfig.mechanics.shapedOutlines = false;
                this.wait = 5;
                return;
            case 6:
                shot(mc, "ua_mech_outline_fence_off");
                ModConfig.mechanics.shapedOutlines = true;
                look(mc, 3.5, y() + 1, 8.6, 0.0F, 40.0F);
                this.wait = 30;
                return;
            case 7: {
                checkTarget(mc, new BlockPos(3, 4, 10), "the hopper");
                shot(mc, "ua_mech_outline_hopper");
                // Chests at z 14, placed by the server player's own item use: two side by side while sneaking (singles),
                // and two placed normally (a double).
                final EntityPlayerMP p = serverPlayer(mc);
                mc.getIntegratedServer().addScheduledTask(() -> {
                    placeChest(p, new BlockPos(-2, 3, 14), true);
                    placeChest(p, new BlockPos(-1, 3, 14), true);
                    placeChest(p, new BlockPos(1, 3, 14), false);
                    placeChest(p, new BlockPos(2, 3, 14), false);
                });
                look(mc, 0.5, y(), 11.5, 0.0F, 30.0F);
                this.wait = 30;
                return;
            }
            case 8:
                shot(mc, "ua_mech_chests");
                open(mc, new BlockPos(-2, 4, 14));
                this.wait = 20;
                return;
            case 9:
                ok(chestSize(mc) == 27, "a chest placed beside another while sneaking opens single: " + chestSize(mc));
                mc.player.closeScreen();
                open(mc, new BlockPos(2, 4, 14));
                this.wait = 20;
                return;
            case 10:
                ok(chestSize(mc) == 54, "a chest placed normally beside one opens double: " + chestSize(mc));
                mc.player.closeScreen();
                finish(mc);
                return;
            default:
                finish(mc);
        }
    }

    private static void placeChest(EntityPlayerMP p, BlockPos floor, boolean sneaking) {
        p.setPositionAndRotation(floor.getX() + 0.5, floor.getY() + 1, floor.getZ() - 2.5, 0.0F, 30.0F);
        p.setSneaking(sneaking);
        ItemStack stack = new ItemStack(Blocks.CHEST);
        p.setHeldItem(EnumHand.MAIN_HAND, stack);
        ForgeHooks.onPlaceItemIntoWorld(stack, p, p.world, floor, EnumFacing.UP, 0.5F, 1.0F, 0.5F, EnumHand.MAIN_HAND);
        p.setSneaking(false);
        p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
    }

    private static void open(Minecraft mc, BlockPos pos) {
        mc.playerController.processRightClickBlock(mc.player, mc.world, pos, EnumFacing.UP,
                new Vec3d(pos.getX() + 0.5, pos.getY() + 0.875, pos.getZ() + 0.5), EnumHand.MAIN_HAND);
    }

    private static int chestSize(Minecraft mc) {
        return mc.player.openContainer instanceof ContainerChest
                ? ((ContainerChest) mc.player.openContainer).getLowerChestInventory().getSizeInventory() : -1;
    }

    private static double y() {
        return 4.0;
    }

    private void checkTarget(Minecraft mc, BlockPos pos, String what) {
        RayTraceResult hit = mc.objectMouseOver;
        ok(hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && pos.equals(hit.getBlockPos()),
                "the crosshair is on " + what + ": " + (hit == null ? null : hit.getBlockPos()));
    }

    private void finish(Minecraft mc) {
        this.done = true;
        UpdateAquaticBackport.LOGGER.info("CLIENT SELFTEST DONE pass={} fail={}", this.pass, this.fail);
        mc.shutdown();
    }
}
