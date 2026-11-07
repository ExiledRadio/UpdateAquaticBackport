package com.exiledradio.updateaquaticbackport.trident;

import com.exiledradio.backportcore.waterlog.Waterlogging;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Where drowned spawn: 1.14.4's spawn lists and rule.
 *
 * <ul>
 *   <li>Every ocean (weight 5), every river (weight 100), frozen rivers (weight 1); groups of one, as
 *       monsters, in water at least two blocks deep. Oceans and rivers are found by the biome dictionary,
 *       so other mods' and this mod's own ocean biomes are included.</li>
 *   <li>Natural spawns need water at the spot. In a river one try in 15 succeeds; in an ocean one in 40,
 *       and only more than 5 blocks below sea level.</li>
 *   <li>Then the monster light rule (no block light, dark enough by sky, which is why more spawn at
 *       night), on any difficulty but peaceful ({@link EntityDrowned#getCanSpawnHere}).</li>
 * </ul>
 * Spawners skip the water and biome rule, as 1.14.4's do.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class DrownedSpawning {

    public static final int OCEAN_WEIGHT = 5;
    public static final int RIVER_WEIGHT = 100;
    public static final int FROZEN_RIVER_WEIGHT = 1;

    private DrownedSpawning() {
    }

    /** Adds the drowned to every ocean's and river's monster list. Called from init, after every biome exists. */
    public static void addSpawns() {
        if (!TridentContent.enabled() || !ModConfig.trident.drownedSpawn) {
            return;
        }
        List<Biome> oceans = new ArrayList<Biome>();
        List<Biome> rivers = new ArrayList<Biome>();
        List<Biome> frozenRivers = new ArrayList<Biome>();
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)) {
                oceans.add(biome);
            } else if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)) {
                (isFrozenRiver(biome) ? frozenRivers : rivers).add(biome);
            }
        }
        EntityRegistry.addSpawn(EntityDrowned.class, OCEAN_WEIGHT, 1, 1, EnumCreatureType.MONSTER, oceans.toArray(new Biome[0]));
        EntityRegistry.addSpawn(EntityDrowned.class, RIVER_WEIGHT, 1, 1, EnumCreatureType.MONSTER, rivers.toArray(new Biome[0]));
        EntityRegistry.addSpawn(EntityDrowned.class, FROZEN_RIVER_WEIGHT, 1, 1, EnumCreatureType.MONSTER, frozenRivers.toArray(new Biome[0]));
        UpdateAquaticBackport.LOGGER.info("Drowned spawn in {} oceans, {} rivers and {} frozen rivers.",
                oceans.size(), rivers.size(), frozenRivers.size());
    }

    static boolean isFrozenRiver(Biome biome) {
        return biome == Biomes.FROZEN_RIVER || BiomeDictionary.hasType(biome, BiomeDictionary.Type.SNOWY);
    }

    static boolean isRiver(Biome biome) {
        return biome == Biomes.RIVER || biome == Biomes.FROZEN_RIVER || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }

    /**
     * 1.14.4's {@code DrownedEntity} spawn rule without the light check: water at the spot, then 1 in 15 in
     * a river, or 1 in 40 and more than 5 below sea level anywhere else.
     */
    public static boolean waterRule(World world, BlockPos pos, Biome biome, Random random) {
        if (!Waterlogging.hasWater(world, pos)) {
            return false;
        }
        if (isRiver(biome)) {
            return random.nextInt(15) == 0;
        }
        return random.nextInt(40) == 0 && pos.getY() < world.getSeaLevel() - 5;
    }

    @SubscribeEvent
    public static void onCheckSpawn(LivingSpawnEvent.CheckSpawn event) {
        EntityLiving entity = event.getEntityLiving() instanceof EntityLiving ? (EntityLiving) event.getEntityLiving() : null;
        if (!(entity instanceof EntityDrowned) || event.isSpawner() || event.getResult() != Event.Result.DEFAULT) {
            return;
        }
        BlockPos pos = new BlockPos(event.getX(), event.getY(), event.getZ());
        if (!waterRule(event.getWorld(), pos, event.getWorld().getBiome(pos), event.getWorld().rand)) {
            event.setResult(Event.Result.DENY);
        }
    }
}
