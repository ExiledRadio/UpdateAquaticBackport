package com.exiledradio.updateaquaticbackport.lookfeel.client;

import com.exiledradio.backportcore.presentation.client.DebugText;
import com.exiledradio.backportcore.presentation.client.GameMusic;
import com.exiledradio.updateaquaticbackport.common.SharedSounds;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

/**
 * 1.13's in-game music and F3 text, as steps of Backport Core's music owner and debug-text pipeline.
 * <ul>
 *   <li>Music (1.13-pre1): under water in an ocean or river the underwater music (Axolotl, Dragon Fish, Shuniji),
 *       1.14.4's {@code getSituationalMusic}: kept while it plays, never in place of the nether, End or a
 *       dimension's own music, and not when the game music is already playing.</li>
 *   <li>F3 (18w22c to 1.13.1): the integrated server's tick time and packets, the dimension and its forced chunks
 *       above the position, "Looking at block" and "Looking at liquid" (20 blocks), and the right column's
 *       "Targeted Block", "Targeted Fluid" and "Targeted Entity" sections (1.14.4's {@code DebugScreenOverlay}).</li>
 * </ul>
 */
@SideOnly(Side.CLIENT)
final class LookAndFeel113 {

    /** For the client self-test. */
    static volatile int debugEdits;

    private LookAndFeel113() {
    }

    static void register() {
        GameMusic.setUnderWater(SharedSounds.MUSIC_UNDER_WATER);
        GameMusic.add("1.13", "under water", LookAndFeel113::music);
        DebugText.add("1.13", "targeted block, fluid and entity", LookAndFeel113::debugText);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new Events());
    }

    static void music(GameMusic.Situation s) {
        Minecraft mc = s.mc;
        if (mc.player == null || mc.world == null) {
            return;
        }
        MusicTicker.MusicType vanilla = s.vanilla;
        // 1.14.4: the nether, the End and a dimension's own music come first; only game and creative music give way.
        if (vanilla != MusicTicker.MusicType.GAME && vanilla != MusicTicker.MusicType.CREATIVE || mc.world.provider.getMusicType() != null) {
            return;
        }
        boolean playing = s.isPlaying(SharedSounds.MUSIC_UNDER_WATER);
        if (playing || isUnderWater(mc) && !s.isPlaying(MusicTicker.MusicType.GAME.getMusicLocation()) && oceanOrRiver(mc)) {
            s.choose(new GameMusic.Track(SharedSounds.MUSIC_UNDER_WATER, 12000, 24000, true));
        }
    }

    static boolean isUnderWater(Minecraft mc) {
        return mc.player.isInsideOfMaterial(Material.WATER);
    }

    static boolean oceanOrRiver(Minecraft mc) {
        Biome biome = mc.world.getBiome(new BlockPos(mc.player));
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }

    static void debugText(DebugText.Lines lines) {
        Minecraft mc = lines.mc;
        if (mc.world == null || mc.getRenderViewEntity() == null || mc.gameSettings.reducedDebugInfo) {
            return;
        }
        Entity eye = mc.getRenderViewEntity();
        List<String> left = lines.left;
        // Second line on: the server line (18w22c's tick time and packets).
        if (left.size() > 2 && mc.getConnection() != null) {
            if (mc.getIntegratedServer() != null) {
                float ms = mean(mc.getIntegratedServer().tickTimeArray) * 1.0E-6F;
                left.add(2, String.format("Integrated server @ %.0f ms ticks, %.0f tx, %.0f rx", ms, Packets.sent, Packets.received));
            } else {
                left.add(2, String.format("\"%s\" server, %.0f tx, %.0f rx", mc.player.getServerBrand(), Packets.sent, Packets.received));
            }
        }
        // 1.13.1: the dimension and its forced chunks, above the blank line before XYZ.
        int xyz = DebugText.Lines.find(left, "XYZ: ");
        if (xyz > 0) {
            int forced = ForgeChunkManager.getPersistentChunksFor(mc.world).size();
            left.add(xyz - 1, "minecraft:" + mc.world.provider.getDimensionType().getName() + " FC: " + forced);
        }
        // "Looking at" becomes "Looking at block", 20 blocks out (pick(20, 0, false)), and the fluid gets its own line.
        Vec3d from = eye.getPositionEyes(lines.partialTicks);
        Vec3d look = eye.getLook(lines.partialTicks);
        Vec3d to = from.add(look.x * 20.0, look.y * 20.0, look.z * 20.0);
        RayTraceResult block = mc.world.rayTraceBlocks(from, to, false, false, false);
        RayTraceResult liquid = mc.world.rayTraceBlocks(from, to, true, false, false);
        int looking = DebugText.Lines.find(left, "Looking at: ");
        if (looking >= 0) {
            left.remove(looking);
        }
        int at = looking >= 0 ? looking : left.size();
        if (block != null && block.typeOfHit == RayTraceResult.Type.BLOCK) {
            BlockPos p = block.getBlockPos();
            left.add(at++, String.format("Looking at block: %d %d %d", p.getX(), p.getY(), p.getZ()));
        }
        Fluid fluid = liquid == null || liquid.typeOfHit != RayTraceResult.Type.BLOCK ? null : fluidAt(mc, liquid.getBlockPos());
        if (fluid != null) {
            BlockPos p = liquid.getBlockPos();
            left.add(at, String.format("Looking at liquid: %d %d %d", p.getX(), p.getY(), p.getZ()));
        }
        // Right column: headings for the block 1.12.2 lists, then the fluid and the entity.
        List<String> right = lines.right;
        RayTraceResult hit = mc.objectMouseOver;
        if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && hit.getBlockPos() != null) {
            IBlockState state = mc.world.getBlockState(hit.getBlockPos());
            int name = right.indexOf(String.valueOf(Block.REGISTRY.getNameForObject(state.getBlock())));
            if (name > 0) {
                right.add(name, TextFormatting.UNDERLINE + "Targeted Block");
            }
        }
        if (fluid != null) {
            right.add("");
            right.add(TextFormatting.UNDERLINE + "Targeted Fluid");
            right.add(fluid == FluidRegistry.WATER ? "minecraft:water" : fluid == FluidRegistry.LAVA ? "minecraft:lava" : fluid.getName());
            IBlockState there = mc.world.getBlockState(liquid.getBlockPos());
            for (net.minecraft.block.properties.IProperty<?> prop : there.getPropertyKeys()) {
                if (prop.getName().equals("level")) {
                    right.add("level: " + there.getValue(prop));
                }
            }
        }
        Entity pointed = mc.pointedEntity;
        if (pointed != null) {
            ResourceLocation key = EntityList.getKey(pointed);
            right.add("");
            right.add(TextFormatting.UNDERLINE + "Targeted Entity");
            right.add(String.valueOf(key));
        }
        debugEdits++;
    }

    /**
     * 1.13's {@code NetworkManager} packet averages ({@code tick}: {@code lerp(0.75, count, average)} each client
     * tick), counted by a small handler put into the connection's pipeline when it opens.
     */
    static final class Packets extends io.netty.channel.ChannelDuplexHandler {
        static volatile float sent;
        static volatile float received;
        private static final java.util.concurrent.atomic.AtomicInteger SENT = new java.util.concurrent.atomic.AtomicInteger();
        private static final java.util.concurrent.atomic.AtomicInteger RECEIVED = new java.util.concurrent.atomic.AtomicInteger();

        @Override
        public void channelRead(io.netty.channel.ChannelHandlerContext ctx, Object msg) throws Exception {
            RECEIVED.incrementAndGet();
            super.channelRead(ctx, msg);
        }

        @Override
        public void write(io.netty.channel.ChannelHandlerContext ctx, Object msg, io.netty.channel.ChannelPromise promise) throws Exception {
            SENT.incrementAndGet();
            super.write(ctx, msg, promise);
        }

        static void tick() {
            sent = SENT.getAndSet(0) * 0.25F + sent * 0.75F;
            received = RECEIVED.getAndSet(0) * 0.25F + received * 0.75F;
        }
    }

    /** Forge's events for the packet counter. */
    static final class Events {
        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onConnect(net.minecraftforge.fml.common.network.FMLNetworkEvent.ClientConnectedToServerEvent event) {
            try {
                io.netty.channel.ChannelPipeline pipeline = event.getManager().channel().pipeline();
                if (pipeline.get("updateaquaticbackport:packets") == null && pipeline.get("packet_handler") != null) {
                    pipeline.addBefore("packet_handler", "updateaquaticbackport:packets", new Packets());
                }
            } catch (RuntimeException e) {
                // The F3 line then shows no traffic; nothing else depends on it.
            }
        }

        @net.minecraftforge.fml.common.eventhandler.SubscribeEvent
        public void onTick(net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent event) {
            if (event.phase == net.minecraftforge.fml.common.gameevent.TickEvent.Phase.END) {
                Packets.tick();
            }
        }
    }

    private static Fluid fluidAt(Minecraft mc, BlockPos pos) {
        Material m = mc.world.getBlockState(pos).getMaterial();
        return m == Material.WATER ? FluidRegistry.WATER : m == Material.LAVA ? FluidRegistry.LAVA : null;
    }

    private static float mean(long[] values) {
        if (values == null || values.length == 0) {
            return 0.0F;
        }
        long sum = 0L;
        for (long v : values) {
            sum += v;
        }
        return (float) sum / values.length;
    }
}
