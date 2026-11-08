package com.exiledradio.updateaquaticbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.Contribution;
import com.exiledradio.backportcore.presentation.Presentation;
import com.exiledradio.backportcore.presentation.client.PresentationHooks;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

/**
 * Stream Lf's client side: 1.13's F3+C and F3+I, following 1.14.4's {@code KeyboardHandler} (the same as 1.13's).
 * The commands they copy are written so 1.12.2's own commands accept them: {@code /tp @s x y z yaw pitch} (1.13.0's
 * form; 1.13.1 later prefixed {@code /execute in <dimension> run}, which 1.12.2 has no command for), and
 * {@code /setblock x y z id state replace {nbt}} where 1.13 writes {@code id[state]{nbt}}.
 */
@SideOnly(Side.CLIENT)
public final class LookAndFeelClient {

    /** For the client self-test. */
    public static volatile String lastClipboard;

    private LookAndFeelClient() {
    }

    public static void init() {
        Contribution v113 = Presentation.contribution("1.13");
        if (v113 != null) {
            v113.debugKey(Keyboard.KEY_C, "debug.copy_location.help", LookAndFeelClient::copyLocation);
            v113.debugKey(Keyboard.KEY_I, "debug.inspect.help", LookAndFeelClient::inspect);
        }
        UnderwaterFogStep.register();
        LookAndFeel113.register();
    }

    /** F3+C: the player's position and look as a teleport command. Holding F3+C still crashes the game, as 1.12.2's. */
    static boolean copyLocation() {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.hasReducedDebug()) {
            return false;
        }
        PresentationHooks.debugFeedback("debug.copy_location.message");
        clipboard(locationCommand(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch));
        return true;
    }

    public static String locationCommand(double x, double y, double z, float yaw, float pitch) {
        return String.format(Locale.ROOT, "/tp @s %.2f %.2f %.2f %.2f %.2f", x, y, z, yaw, pitch);
    }

    /**
     * F3+I: the targeted block or entity as a command that recreates it, with its data when the player may use
     * commands. Without Shift, 1.13 asks the server for the data; 1.12.2 has no packet for that, so the server's
     * copy is read only in singleplayer (on the server thread), and a multiplayer client copies its own data.
     */
    static boolean inspect() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null || player.hasReducedDebug()) {
            return true;
        }
        boolean privileged = player.canUseCommand(2, "");
        boolean askServer = !GuiScreen.isShiftKeyDown();
        RayTraceResult hit = mc.objectMouseOver;
        if (hit == null) {
            return true;
        }
        final IntegratedServer server = mc.getIntegratedServer();
        final int dimension = player.dimension;
        if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
            final BlockPos pos = hit.getBlockPos();
            final IBlockState state = mc.world.getBlockState(pos);
            if (privileged && askServer && server != null) {
                server.addScheduledTask(() -> {
                    WorldServer world = server.getWorld(dimension);
                    TileEntity te = world.getTileEntity(pos);
                    final NBTTagCompound nbt = te == null ? null : te.writeToNBT(new NBTTagCompound());
                    mc.addScheduledTask(() -> {
                        clipboard(blockCommand(state, pos, nbt));
                        PresentationHooks.debugFeedback("debug.inspect.server.block");
                    });
                });
            } else {
                TileEntity te = privileged ? mc.world.getTileEntity(pos) : null;
                clipboard(blockCommand(state, pos, te == null ? null : te.writeToNBT(new NBTTagCompound())));
                PresentationHooks.debugFeedback("debug.inspect.client.block");
            }
        } else if (hit.typeOfHit == RayTraceResult.Type.ENTITY && hit.entityHit != null) {
            final Entity entity = hit.entityHit;
            final ResourceLocation id = EntityList.getKey(entity);
            final double x = entity.posX;
            final double y = entity.posY;
            final double z = entity.posZ;
            if (id == null) {
                return true;
            }
            if (privileged && askServer && server != null) {
                final int entityId = entity.getEntityId();
                server.addScheduledTask(() -> {
                    Entity onServer = server.getWorld(dimension).getEntityByID(entityId);
                    final NBTTagCompound nbt = onServer == null ? null : onServer.writeToNBT(new NBTTagCompound());
                    mc.addScheduledTask(() -> {
                        clipboard(entityCommand(id, x, y, z, nbt));
                        PresentationHooks.debugFeedback("debug.inspect.server.entity");
                    });
                });
            } else {
                clipboard(entityCommand(id, x, y, z, privileged ? entity.writeToNBT(new NBTTagCompound()) : null));
                PresentationHooks.debugFeedback("debug.inspect.client.entity");
            }
        }
        return true;
    }

    /** 1.14.4 {@code copyCreateBlockCommand}, in 1.12.2's {@code /setblock} syntax. */
    public static String blockCommand(IBlockState state, BlockPos pos, @Nullable NBTTagCompound nbt) {
        StringBuilder b = new StringBuilder();
        b.append(String.format(Locale.ROOT, "/setblock %d %d %d %s %s", pos.getX(), pos.getY(), pos.getZ(),
                Block.REGISTRY.getNameForObject(state.getBlock()), properties(state)));
        if (nbt != null) {
            nbt.removeTag("x");
            nbt.removeTag("y");
            nbt.removeTag("z");
            nbt.removeTag("id");
            b.append(" replace ").append(nbt);
        }
        return b.toString();
    }

    /** 1.14.4 {@code copyCreateEntityCommand}. */
    public static String entityCommand(ResourceLocation id, double x, double y, double z, @Nullable NBTTagCompound nbt) {
        if (nbt == null) {
            return String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f", id, x, y, z);
        }
        nbt.removeTag("UUIDMost");
        nbt.removeTag("UUIDLeast");
        nbt.removeTag("Pos");
        nbt.removeTag("Dimension");
        return String.format(Locale.ROOT, "/summon %s %.2f %.2f %.2f %s", id, x, y, z, nbt);
    }

    /** The state as 1.12.2's commands read it: {@code facing=north,half=top}, or {@code default}. */
    @SuppressWarnings({"rawtypes", "unchecked"})
    static String properties(IBlockState state) {
        StringBuilder b = new StringBuilder();
        for (Map.Entry<IProperty<?>, Comparable<?>> e : state.getProperties().entrySet()) {
            if (b.length() > 0) {
                b.append(',');
            }
            IProperty p = e.getKey();
            b.append(p.getName()).append('=').append(p.getName((Comparable) e.getValue()));
        }
        return b.length() == 0 ? "default" : b.toString();
    }

    private static void clipboard(String text) {
        lastClipboard = text;
        GuiScreen.setClipboardString(text);
    }
}
