package com.exiledradio.updateaquaticbackport.mechanics;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.event.entity.living.LivingSpawnEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.Iterator;

/**
 * Squid spawn only in oceans and rivers (1.13.1, 18w31a; current Java).
 *
 * <p>1.12.2's {@code Biome} constructor gives every biome a squid entry (weight 10, groups of 4) among its water
 * creatures, so squid spawn in swamp water, ponds and lakes anywhere. 1.13.1 kept them to rivers, oceans and
 * beaches; 1.14 dropped the beaches, and from then on (1.14.4's biome classes, current Java) only the oceans and
 * rivers list squid. Current Java wins (docs/PLAN.md): oceans and rivers.
 *
 * <ul>
 *   <li>At load complete, once every mod has made its biomes and spawn lists, the squid entry ({@code EntitySquid}
 *       exactly, not subclasses such as another mod's glow squid) comes out of every biome that is neither
 *       {@link BiomeDictionary.Type#OCEAN} nor {@link BiomeDictionary.Type#RIVER}. The other water creatures keep
 *       their weights, as in 1.13.1's lists.</li>
 *   <li>A natural squid spawn anywhere else (an entry a mod adds later, or {@code WorldEvent.PotentialSpawns}) is
 *       refused. Spawners, eggs and commands are left alone.</li>
 * </ul>
 * The ocean and river squid entries keep their 1.12.2 weight here: this mod's dolphins are weighted against it.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class SquidSpawning {

    private static boolean applied;

    private SquidSpawning() {
    }

    /** @return true if squid may spawn naturally in this biome */
    public static boolean squidBiome(Biome biome) {
        return BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER);
    }

    /** From the mod's load complete. */
    public static void loadComplete() {
        if (!ModConfig.mechanics.squidBiomes) {
            return;
        }
        applied = true;
        int removed = 0;
        for (Biome biome : ForgeRegistries.BIOMES) {
            if (squidBiome(biome)) {
                continue;
            }
            for (Iterator<Biome.SpawnListEntry> it = biome.getSpawnableList(EnumCreatureType.WATER_CREATURE).iterator(); it.hasNext(); ) {
                if (it.next().entityClass == EntitySquid.class) {
                    it.remove();
                    removed++;
                }
            }
        }
        UpdateAquaticBackport.LOGGER.info("Squid spawn only in oceans and rivers: removed {} squid spawn entries", removed);
    }

    @SubscribeEvent
    public static void checkSpawn(LivingSpawnEvent.CheckSpawn event) {
        if (applied && !event.isSpawner() && event.getEntity().getClass() == EntitySquid.class
                && !squidBiome(event.getWorld().getBiome(event.getEntity().getPosition()))) {
            event.setResult(Event.Result.DENY);
        }
    }
}
