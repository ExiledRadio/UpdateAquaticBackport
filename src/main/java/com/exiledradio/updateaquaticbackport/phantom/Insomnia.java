package com.exiledradio.updateaquaticbackport.phantom;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.IEntityLivingData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatBasic;
import net.minecraft.stats.StatList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.WorldEntitySpawner;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Insomnia: the "time since rest" statistic, and phantoms spawning over players who have let it grow.
 *
 * <h2>The statistic</h2>
 * 1.13's {@code time_since_rest} is a general statistic here too ({@code stat.timeSinceRest}, shown under
 * General and usable as a scoreboard criterion). It goes up one per tick while the player is not in a bed, is
 * cleared on getting into one (sleeping through the night is not needed, as current Java) and on dying. Kept
 * by the statistics file, it survives logging out.
 *
 * <h2>The spawner</h2>
 * 1.14.4's {@code PhantomSpawner}, which current Java keeps: in the overworld, every 1-2 minutes while mob
 * spawning and hostile mobs are on, at night or in a thunderstorm; for each player not spectating, at or above
 * sea level under full sky light, when the local difficulty beats a random 0-3 and a random number below the
 * statistic reaches 72000 (three days), a group of 1 + random(difficulty + 1) phantoms appears 20-34 blocks
 * above, up to 10 blocks off sideways, in an empty space. The {@code doInsomnia} game rule (added to every world
 * that lacks it) and {@code phantoms.insomnia} switch it off.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class Insomnia {

    public static final String STAT_ID = "stat.timeSinceRest";
    public static final String GAME_RULE = "doInsomnia";

    /** Three in-game days. */
    public static final int THRESHOLD = 72000;

    private static StatBase timeSinceRest;

    /** Ticks until the next attempt, in the overworld. 1.14 keeps one counter per world; only one world spawns. */
    private static int ticksUntilSpawn;

    private Insomnia() {
    }

    /** Called once, while entities register. Reuses the statistic if another mod has already made it. */
    static void registerStat() {
        StatBase existing = StatList.getOneShotStat(STAT_ID);
        if (existing != null) {
            timeSinceRest = existing;
            return;
        }
        timeSinceRest = new StatBasic(STAT_ID, new TextComponentTranslation(STAT_ID), StatBase.timeStatType)
                .initIndependentStat().registerStat();
    }

    public static StatBase stat() {
        return timeSinceRest;
    }

    /** @return the player's ticks since rest */
    public static int timeSinceRest(EntityPlayerMP player) {
        return timeSinceRest == null ? 0 : player.getStatFile().readStat(timeSinceRest);
    }

    private static boolean active() {
        return PhantomContent.enabled() && timeSinceRest != null;
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !active()
                || !(event.player instanceof EntityPlayerMP) || event.player instanceof FakePlayer) {
            return;
        }
        EntityPlayerMP player = (EntityPlayerMP) event.player;
        if (player.isPlayerSleeping()) {
            // Current Java clears it on getting into bed; staying in bed keeps it at zero.
            if (timeSinceRest(player) != 0) {
                player.takeStat(timeSinceRest);
            }
        } else {
            player.addStat(timeSinceRest);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent event) {
        if (active() && event.getEntityLiving() instanceof EntityPlayerMP && !(event.getEntityLiving() instanceof FakePlayer)) {
            ((EntityPlayerMP) event.getEntityLiving()).takeStat(timeSinceRest);
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(WorldEvent.Load event) {
        if (!PhantomContent.enabled() || event.getWorld().isRemote) {
            return;
        }
        GameRules rules = event.getWorld().getGameRules();
        if (!rules.hasRule(GAME_RULE)) {
            rules.addGameRule(GAME_RULE, "true", GameRules.ValueType.BOOLEAN_VALUE);
        }
        if (event.getWorld().provider.getDimensionType() == DimensionType.OVERWORLD) {
            ticksUntilSpawn = 0;
        }
    }

    @SubscribeEvent
    public static void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.side.isClient() || !active() || !ModConfig.phantoms.insomnia
                || !(event.world instanceof WorldServer) || event.world.provider.getDimensionType() != DimensionType.OVERWORLD) {
            return;
        }
        WorldServer world = (WorldServer) event.world;
        GameRules rules = world.getGameRules();
        if (!rules.getBoolean("doMobSpawning")) {
            return;
        }
        tick(world);
    }

    /** 1.14.4 {@code PhantomSpawner.tick}, with current Java's game rule check. @return phantoms spawned */
    public static int tick(WorldServer world) {
        MinecraftServer server = world.getMinecraftServer();
        boolean hostile = world.getDifficulty() != EnumDifficulty.PEACEFUL && (server == null || server.allowSpawnMonsters());
        if (!hostile || !world.getGameRules().getBoolean(GAME_RULE)) {
            return 0;
        }
        Random random = world.rand;
        --ticksUntilSpawn;
        if (ticksUntilSpawn > 0) {
            return 0;
        }
        ticksUntilSpawn += (60 + random.nextInt(60)) * 20;
        if (world.getSkylightSubtracted() < 5 && world.provider.hasSkyLight()) {
            return 0;
        }
        int spawned = 0;
        for (EntityPlayer player : new ArrayList<EntityPlayer>(world.playerEntities)) {
            if (player instanceof EntityPlayerMP && !(player instanceof FakePlayer) && !player.isSpectator()) {
                spawned += trySpawnOver(world, (EntityPlayerMP) player, random);
            }
        }
        return spawned;
    }

    /** One player's roll. @return phantoms spawned */
    public static int trySpawnOver(WorldServer world, EntityPlayerMP player, Random random) {
        BlockPos pos = new BlockPos(player);
        if (world.provider.hasSkyLight() && (pos.getY() < world.getSeaLevel() || world.getLightFor(EnumSkyBlock.SKY, pos) < 15)) {
            return 0;
        }
        DifficultyInstance difficulty = world.getDifficultyForLocation(pos);
        if (!difficulty.isHarderThan(random.nextFloat() * 3.0F)) {
            return 0;
        }
        int rest = MathHelper.clamp(timeSinceRest(player), 1, Integer.MAX_VALUE);
        if (random.nextInt(rest) < THRESHOLD) {
            return 0;
        }
        BlockPos at = pos.up(20 + random.nextInt(15)).east(-10 + random.nextInt(21)).south(-10 + random.nextInt(21));
        if (!WorldEntitySpawner.isValidEmptySpawnBlock(world.getBlockState(at))) {
            return 0;
        }
        return spawnGroup(world, at, difficulty, 1 + random.nextInt(world.getDifficulty().getId() + 1)).size();
    }

    /** Spawns a group at a position as natural spawns, through Forge's spawn events. */
    public static List<EntityPhantom> spawnGroup(World world, BlockPos at, DifficultyInstance difficulty, int count) {
        List<EntityPhantom> out = new ArrayList<EntityPhantom>();
        IEntityLivingData data = null;
        float x = at.getX() + 0.5F;
        float y = at.getY();
        float z = at.getZ() + 0.5F;
        for (int i = 0; i < count; i++) {
            EntityPhantom phantom = new EntityPhantom(world);
            phantom.moveToBlockPosAndAngles(at, 0.0F, 0.0F);
            if (ForgeEventFactory.canEntitySpawn(phantom, world, x, y, z, false) == Event.Result.DENY) {
                continue;
            }
            if (!ForgeEventFactory.doSpecialSpawn(phantom, world, x, y, z)) {
                data = phantom.onInitialSpawn(difficulty, data);
            }
            world.spawnEntity(phantom);
            out.add(phantom);
        }
        return out;
    }

    /** For the self-test: makes the next {@link #tick} attempt at once. */
    public static void attemptNextTick() {
        ticksUntilSpawn = 0;
    }
}
