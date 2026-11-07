package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.asm.Asm;
import com.exiledradio.backportcore.pose.Pose;
import com.exiledradio.backportcore.pose.Poses;
import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.dolphin.DolphinContent;
import com.exiledradio.updateaquaticbackport.phantom.PhantomContent;
import com.exiledradio.updateaquaticbackport.swim.PacketSwimRules;
import com.exiledradio.updateaquaticbackport.swim.SwimContent;
import com.exiledradio.updateaquaticbackport.swim.SwimMovement;
import com.exiledradio.updateaquaticbackport.swim.SwimRules;
import com.exiledradio.updateaquaticbackport.trident.Riptide;
import com.exiledradio.updateaquaticbackport.trident.TridentContent;

import com.mojang.authlib.GameProfile;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;

import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.util.UUID;

/**
 * Stream Sw: swimming and crawling, through Backport Core's pose engine and its patches, on a dedicated
 * server with real server players (each with a connection that goes nowhere).
 */
final class SwimSelfTest implements SelfTest {

    @Override
    public String name() {
        return "swim";
    }

    private static EntityPlayerMP player(WorldServer world, String name) {
        EntityPlayerMP p = new EntityPlayerMP(world.getMinecraftServer(), world,
                new GameProfile(UUID.nameUUIDFromBytes(("selftest:swim" + name).getBytes()), "Swim" + name),
                new PlayerInteractionManager(world));
        // A channel that keeps what it is sent: entering water grants an advancement, whose recipe packet needs one.
        NetworkManager manager = new NetworkManager(EnumPacketDirection.SERVERBOUND);
        new EmbeddedChannel(manager);
        p.connection = new NetHandlerPlayServer(world.getMinecraftServer(), manager, p);
        p.interactionManager.setGameType(GameType.SURVIVAL);
        // A new server player cannot be hurt for 60 ticks; the suffocation check needs it to be.
        ObfuscationReflectionHelper.setPrivateValue(EntityPlayerMP.class, p, 0, "field_147101_bU");
        return p;
    }

    private static void at(EntityPlayer p, BlockPos pos, double dx, double dy, double dz, float pitch) {
        p.setLocationAndAngles(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz, 0.0F, pitch);
        p.motionX = p.motionY = p.motionZ = 0.0D;
    }

    private static boolean sized(EntityPlayer p, float w, float h, float eye) {
        return p.width == w && p.height == h && Math.abs(p.getEyeHeight() - eye) < 1.0E-6F;
    }

    private static String size(EntityPlayer p) {
        return p.width + " x " + p.height + ", eyes " + p.getEyeHeight() + ", pose " + Poses.get(p)
                + ", swimming " + SwimRules.swimming(p) + ", in water " + p.isInWater();
    }

    @Override
    public void run(Check c) throws Exception {
        WorldServer world = c.world;

        // 0. wiring ---------------------------------------------------------------------------------------
        c.ok(ModConfig.swimming.enabled && ModConfig.swimming.crawling, "swimming and crawling on by default");
        c.ok(Poses.active(), "pose engine active with the swimming rules");
        c.eq("applied", Asm.state("P1"), "Backport Core's water travel patch applied in this dev environment");
        boolean registered = false;
        for (IClassTransformer t : Launch.classLoader.getTransformers()) {
            registered |= t.getClass().getName().contains("com.exiledradio.backportcore.asm.");
        }
        c.ok(registered, "Backport Core's coremod found through its dev jar's manifest");

        // 1. sprint-swimming ------------------------------------------------------------------------------
        BlockPos pool = Pools.pool(c, 5);
        BlockPos mid = pool.add(5, 1, 5);
        EntityPlayerMP s = player(world, "swimmer");
        at(s, mid, 0.5, 1.0, 0.5, 0.0F);
        s.onUpdateEntity();
        c.ok(s.isInWater() && SwimRules.eyesInWater(s) && !SwimRules.swimming(s), "under water, not sprinting: not swimming: " + size(s));
        c.ok(sized(s, 0.6F, 1.8F, 1.62F), "not swimming: 0.6 x 1.8, eyes 1.62: " + size(s));
        s.setSprinting(true);
        s.onUpdateEntity();
        c.ok(SwimRules.swimming(s) && Poses.get(s) == Pose.SWIMMING, "sprinting with eyes under water: swimming: " + size(s));
        c.ok(sized(s, 0.6F, 0.6F, 0.4F), "swimming: 0.6 x 0.6 with eyes at 0.4: " + size(s));
        c.ok(Math.abs(s.getEntityBoundingBox().maxY - s.getEntityBoundingBox().minY - 0.6D) < 1.0E-6D, "swimming box is 0.6 tall");
        c.ok(!Poses.isCrawling(s), "swimming in water is not crawling");
        s.setSneaking(true);
        s.onUpdateEntity();
        c.ok(Poses.get(s) == Pose.SWIMMING && sized(s, 0.6F, 0.6F, 0.4F), "sneaking while swimming: still swimming, eyes 0.4: " + size(s));
        s.setSneaking(false);
        s.setSprinting(false);
        s.onUpdateEntity();
        c.ok(!SwimRules.swimming(s) && Poses.get(s) == Pose.STANDING && sized(s, 0.6F, 1.8F, 1.62F),
                "sprint stops: swimming stops and the player stands (room above): " + size(s));

        // eyes above water: no swim; once swimming, shallow water keeps it going
        BlockPos shallow = Pools.pool(c, 1);
        EntityPlayerMP w = player(world, "wader");
        at(w, shallow.add(5, 1, 5), 0.5, 0.0, 0.5, 0.0F);
        w.setSprinting(true);
        w.onUpdateEntity();
        c.ok(w.isInWater() && !SwimRules.eyesInWater(w) && !SwimRules.swimming(w), "sprinting with eyes above water: no swim: " + size(w));
        at(s, mid, 0.5, 1.0, 0.5, 0.0F);
        s.setSprinting(true);
        s.onUpdateEntity();
        c.fill(mid.add(-4, 1, -4), mid.add(5, 3, 5), Blocks.AIR.getDefaultState());
        at(s, mid, 0.5, 0.0, 0.5, 0.0F);
        s.onUpdateEntity();
        c.ok(SwimRules.swimming(s) && Poses.get(s) == Pose.SWIMMING, "a swim keeps going in water one block deep: " + size(s));

        // not while flying, riding or in spectator
        s.capabilities.isFlying = true;
        s.onUpdateEntity();
        c.ok(!SwimRules.swimming(s), "flying: no swim: " + size(s));
        s.capabilities.isFlying = false;
        EntityPig pig = new EntityPig(world);
        pig.setLocationAndAngles(mid.getX() + 0.5D, mid.getY(), mid.getZ() + 0.5D, 0.0F, 0.0F);
        world.spawnEntity(pig);
        s.startRiding(pig, true);
        s.onUpdateEntity();
        c.ok(!SwimRules.swimming(s) && Poses.get(s) != Pose.SWIMMING, "riding: no swim: " + size(s));
        s.dismountRidingEntity();
        pig.setDead();

        // 2. stepping down off the swim ------------------------------------------------------------------
        // Out onto open land: the swim ends and the player stands.
        at(s, mid, 0.5, 0.0, 0.5, 0.0F);
        s.setSprinting(true);
        s.onUpdateEntity();
        c.fill(mid.add(-4, 0, -4), mid.add(5, 0, 5), Blocks.AIR.getDefaultState());
        s.onUpdateEntity();
        s.onUpdateEntity();
        c.ok(!SwimRules.swimming(s) && Poses.get(s) == Pose.STANDING && sized(s, 0.6F, 1.8F, 1.62F),
                "swimming out of the water onto open land: stands: " + size(s));
        // Into a one-block tunnel: the swim ends but there is no room to stand, so the player crawls.
        BlockPos tunnel = Pools.pool(c, 1);
        c.fill(tunnel.add(0, 2, 0), tunnel.add(11, 2, 11), Blocks.STONE.getDefaultState());
        EntityPlayerMP t = player(world, "tunnel");
        at(t, tunnel.add(5, 1, 5), 0.5, 0.0, 0.5, 0.0F);
        t.setSprinting(true);
        t.onUpdateEntity();
        // Put there standing, the eyes start in the roof; once down they are under water and the swim starts.
        t.onUpdateEntity();
        c.ok(SwimRules.swimming(t) && Poses.get(t) == Pose.SWIMMING, "swimming in a one-block water tunnel: " + size(t));
        c.fill(tunnel.add(0, 1, 0), tunnel.add(11, 1, 11), Blocks.AIR.getDefaultState());
        t.onUpdateEntity();
        t.onUpdateEntity();
        c.ok(!SwimRules.swimming(t) && Poses.get(t) == Pose.SWIMMING && Poses.isCrawling(t) && sized(t, 0.6F, 0.6F, 0.4F),
                "the tunnel drains: the swim ends and the player crawls, still 0.6 x 0.6: " + size(t));
        t.setSprinting(false);
        c.fill(tunnel.add(0, 2, 0), tunnel.add(11, 2, 11), Blocks.AIR.getDefaultState());
        t.onUpdateEntity();
        c.ok(Poses.get(t) == Pose.STANDING && sized(t, 0.6F, 1.8F, 1.62F), "the roof goes: the player stands: " + size(t));

        // 3. crawling in a one-block gap -----------------------------------------------------------------
        BlockPos gap = c.area();
        // Floor y=99; a gap one block tall (roof at y=101) from x+3 to x+14; open air from x+0 to x+2.
        c.fill(gap.add(3, 1, 0), gap.add(14, 1, 4), Blocks.STONE.getDefaultState());
        EntityPlayerMP k = player(world, "crawler");
        at(k, gap.add(6, 0, 2), 0.5, 0.0, 0.5, 0.0F);
        k.onUpdateEntity();
        c.ok(Poses.get(k) == Pose.SWIMMING && Poses.isCrawling(k) && sized(k, 0.6F, 0.6F, 0.4F), "no room to stand: crawling, 0.6 x 0.6: " + size(k));
        // The first tick, put there standing, can hurt: vanilla checks before the pose changes, as 1.14 does.
        float start = k.getHealth();
        float lowest = start;
        boolean inside = false;
        boolean stood = false;
        for (int i = 0; i < 200; i++) {
            k.onUpdateEntity();
            inside |= k.isEntityInsideOpaqueBlock();
            stood |= Poses.get(k) != Pose.SWIMMING || k.height != 0.6F;
            lowest = Math.min(lowest, k.getHealth());
        }
        c.ok(!stood, "200 ticks in the gap: stays down: " + size(k));
        c.ok(!inside, "200 ticks in the gap: eyes never inside the roof");
        c.ok(lowest >= start, "200 ticks in the gap: no suffocation damage: lowest " + lowest + " from " + start);

        // 4. the server's movement check accepts a crawl through the gap ---------------------------------
        boolean accepted = true;
        String why = "";
        for (int i = 1; i <= 24; i++) {
            double x = gap.getX() + 6.5D + 0.25D * i;
            if (!move(k, x, gap.getY(), gap.getZ() + 2.5D)) {
                accepted = false;
                why = "rejected at step " + i + " (x " + x + "): player at " + k.posX;
                break;
            }
            k.onUpdateEntity();
        }
        c.ok(accepted, "crawling 6 blocks along the gap, 0.25 a packet: every move accepted (no 'moved wrongly'): " + why);
        c.ok(Poses.get(k) == Pose.SWIMMING, "still crawling at the far end: " + size(k));
        world.getPlayerChunkMap().removePlayer(k);

        // Control: with crawling off a standing player walking into the same gap is pulled back.
        ModConfig.swimming.crawling = false;
        EntityPlayerMP walker = player(world, "walker");
        at(walker, gap.add(1, 0, 2), 0.5, 0.0, 0.5, 0.0F);
        walker.onUpdateEntity();
        boolean pulledBack = !move(walker, gap.getX() + 3.4D, gap.getY(), gap.getZ() + 2.5D);
        c.ok(pulledBack && walker.posX < gap.getX() + 2.0D, "control: a standing player walking into the gap is pulled back: at " + walker.posX);
        world.getPlayerChunkMap().removePlayer(walker);
        // and crawling off, a player put in the gap keeps vanilla's box (eyes in the roof)
        EntityPlayerMP stuck = player(world, "stuck");
        at(stuck, gap.add(6, 0, 2), 0.5, 0.0, 0.5, 0.0F);
        stuck.onUpdateEntity();
        c.ok(Poses.get(stuck) != Pose.SWIMMING && stuck.height == 1.8F && stuck.isEntityInsideOpaqueBlock(),
                "control: crawling off, no crawl in the gap (eyes in the roof): " + size(stuck));
        ModConfig.swimming.crawling = true;

        c.fill(gap.add(3, 1, 0), gap.add(14, 1, 4), Blocks.AIR.getDefaultState());
        k.onUpdateEntity();
        c.ok(Poses.get(k) == Pose.STANDING && sized(k, 0.6F, 1.8F, 1.62F), "the roof goes: the crawler stands up: " + size(k));

        // 5. swim motion (1.14.4's numbers, through travel) ----------------------------------------------
        BlockPos deep = Pools.pool(c, 5);
        EntityPlayerMP m = player(world, "motion");
        at(m, deep.add(5, 1, 5), 0.5, 1.0, 0.5, 0.0F);
        m.setSprinting(true);
        m.onUpdateEntity();
        c.ok(SwimRules.swimming(m), "motion: swimming: " + size(m));
        // level look: vertical motion eases toward 0 (k 0.06), slowdown 0.9, no pull
        travel(m, 0.0F, 0.1D, 0.05D);
        c.ok(near(m.motionX, 0.1D * 0.9F), "swimming: horizontal slowdown 0.9: " + m.motionX);
        c.ok(near(m.motionY, (0.05D + (0.0D - 0.05D) * 0.06D) * 0.800000011920929D), "swimming level: vertical eases to the look (0.06), no pull: " + m.motionY);
        // looking down 45 degrees: k 0.085 toward -0.707
        travel(m, 45.0F, 0.0D, 0.0D);
        double down = m.getLookVec().y;
        c.ok(near(m.motionY, (0.0D + (down - 0.0D) * 0.085D) * 0.800000011920929D), "swimming down: eases to the look by 0.085: " + m.motionY);
        // looking up under water: follows the look up
        travel(m, -30.0F, 0.0D, 0.0D);
        double up = m.getLookVec().y;
        c.ok(near(m.motionY, (up * 0.06D) * 0.800000011920929D), "swimming up under water: eases to the look: " + m.motionY);
        // at the surface (nothing at head height) looking up without jumping: no lift
        BlockPos top = deep.add(5, 5, 5);
        at(m, top, 0.5, 0.2, 0.5, -30.0F);
        m.onUpdateEntity();
        at(m, top, 0.5, 0.2, 0.5, -30.0F);
        c.ok(SwimRules.swimming(m) && SwimRules.eyesInWater(m), "surface: still swimming, eyes just under: " + size(m));
        travel(m, -30.0F, 0.0D, 0.0D);
        c.ok(near(m.motionY, 0.0D), "at the surface looking up, not jumping: no lift: " + m.motionY);
        at(m, top, 0.5, 0.2, 0.5, -30.0F);
        m.setJumping(true);
        travel(m, -30.0F, 0.0D, 0.0D);
        m.setJumping(false);
        c.ok(near(m.motionY, (m.getLookVec().y * 0.06D) * 0.800000011920929D), "at the surface looking up and jumping: lifts: " + m.motionY);
        at(m, deep.add(5, 1, 5), 0.5, 1.0, 0.5, 0.0F);
        // in water, not sprinting: slowdown 0.8 and 1.13's lighter pull (0.005)
        m.setSprinting(false);
        m.onUpdateEntity();
        travel(m, 0.0F, 0.1D, 0.0D);
        c.ok(near(m.motionX, 0.1D * 0.8F) && near(m.motionY, -0.005D), "in water, not sprinting: 0.8 and a 0.005 pull: " + m.motionX + " " + m.motionY);
        // Slow Falling while sinking: 0.01 / 16; settled at the slow pull's speed: -0.003
        if (PhantomContent.slowFalling() != null) {
            m.addPotionEffect(new PotionEffect(PhantomContent.slowFalling(), 200));
            travel(m, 0.0F, 0.0D, -0.01D);
            c.ok(near(m.motionY, -0.01D * 0.800000011920929D - 0.01D / 16.0D), "Slow Falling, sinking: pull 0.01 / 16: " + m.motionY);
            travel(m, 0.0F, 0.0D, 0.0D);
            c.ok(near(m.motionY, -0.003D), "Slow Falling, at rest: settles at -0.003: " + m.motionY);
            m.removePotionEffect(PhantomContent.slowFalling());
        } else {
            c.fail("phantom stream off: Slow Falling untested");
        }
        // sprinting at the surface without swimming still gets 1.13's 0.9 and no pull
        BlockPos wade = Pools.pool(c, 1);
        EntityPlayerMP v = player(world, "sprintwade");
        at(v, wade.add(5, 1, 5), 0.5, 0.0, 0.5, 0.0F);
        v.setSprinting(true);
        v.onUpdateEntity();
        at(v, wade.add(5, 1, 5), 0.5, 0.0, 0.5, 0.0F);
        travel(v, 0.0F, 0.1D, 0.0D);
        c.ok(!SwimRules.swimming(v) && near(v.motionX, 0.1D * 0.9F) && near(v.motionY, 0.0D), "sprinting in water, not swimming: 0.9 and no pull: "
                + v.motionX + " " + v.motionY);
        // Dolphin's Grace: left to the dolphin stream (vanilla's slowdown here, 0.96 there)
        if (DolphinContent.dolphinsGrace() != null) {
            m.setSprinting(true);
            m.addPotionEffect(new PotionEffect(DolphinContent.dolphinsGrace(), 200));
            travel(m, 0.0F, 0.1D, 0.0D);
            c.ok(near(m.motionX, 0.1D * 0.8F), "Dolphin's Grace: slowdown left at vanilla's for the dolphin stream to raise: " + m.motionX);
            m.removePotionEffect(DolphinContent.dolphinsGrace());
            m.setSprinting(false);
        }
        c.eq(0.9F, SwimMovement.SPRINT_SLOWDOWN, "sprint slowdown constant");

        // 6. Riptide's spin pose -------------------------------------------------------------------------
        if (TridentContent.trident() != null) {
            BlockPos dry = c.area();
            EntityPlayerMP r = player(world, "spin");
            at(r, dry.add(5, 0, 5), 0.5, 0.0, 0.5, 0.0F);
            r.onUpdateEntity();
            Riptide.start(r, Riptide.SPIN_TICKS);
            r.onUpdateEntity();
            c.ok(Poses.get(r) == Pose.SPIN_ATTACK && sized(r, 0.6F, 0.6F, 0.4F), "Riptide spin: SPIN_ATTACK, 0.6 x 0.6, eyes 0.4: " + size(r));
            Riptide.stop(r);
            r.onUpdateEntity();
            c.ok(Poses.get(r) == Pose.STANDING && sized(r, 0.6F, 1.8F, 1.62F), "spin over: stands: " + size(r));
        }

        // 7. switches and the server's rules -------------------------------------------------------------
        at(s, mid, 0.5, 0.0, 0.5, 0.0F);
        c.fill(mid.add(-4, 0, -4), mid.add(5, 3, 5), Blocks.WATER.getDefaultState());
        s.setSprinting(true);
        s.onUpdateEntity();
        c.ok(Poses.get(s) == Pose.SWIMMING, "swimming again before the switch: " + size(s));
        ModConfig.swimming.enabled = false;
        s.onUpdateEntity();
        c.ok(!Poses.active() && Poses.get(s) == Pose.STANDING && sized(s, 0.6F, 1.8F, 1.62F), "swimming switched off: vanilla's 1.8 at once: " + size(s));
        ModConfig.swimming.enabled = true;
        ByteBuf buf = Unpooled.buffer();
        new PacketSwimRules(true, false).toBytes(buf);
        PacketSwimRules back = new PacketSwimRules();
        back.fromBytes(buf);
        c.ok(back.enabled() && !back.crawling() && buf.writerIndex() == 1, "packet 75 round trip (1 byte)");
        new PacketSwimRules.Handler().onMessage(new PacketSwimRules(false, false), null);
        c.ok(!SwimContent.swimmingOn() && !SwimContent.crawlingOn(), "a server's switches win over the client's own");
        SwimContent.clearServerRules();
        c.ok(SwimContent.swimmingOn() && SwimContent.crawlingOn(), "after leaving the server, the client's own switches count");
    }

    /** One tick of vanilla travel from a given motion and look, through Backport Core's water hooks. */
    private static void travel(EntityPlayerMP p, float pitch, double motionX, double motionY) {
        p.rotationPitch = pitch;
        p.motionX = motionX;
        p.motionY = motionY;
        p.motionZ = 0.0D;
        p.travel(0.0F, 0.0F, 0.0F);
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1.0E-7D;
    }

    /** Hands the server a client position packet; true if the server kept the position it was given. */
    private static boolean move(EntityPlayerMP p, double x, double y, double z) {
        try {
            PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
            buf.writeDouble(x);
            buf.writeDouble(y);
            buf.writeDouble(z);
            buf.writeByte(1);
            CPacketPlayer.Position packet = new CPacketPlayer.Position();
            packet.readPacketData(buf);
            p.connection.processPlayer(packet);
        } catch (Exception e) {
            return false;
        }
        return Math.abs(p.posX - x) < 1.0E-9D && Math.abs(p.posY - y) < 1.0E-9D && Math.abs(p.posZ - z) < 1.0E-9D;
    }
}
