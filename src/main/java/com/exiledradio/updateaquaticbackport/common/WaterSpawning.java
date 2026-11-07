package com.exiledradio.updateaquaticbackport.common;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.EntityLiving;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;

import java.lang.reflect.Field;
import java.util.function.BiPredicate;

/**
 * Water mobs spawn in waterlogged blocks, as today's do.
 *
 * <p>1.12.2's in-water spawn test ({@code WorldEntitySpawner.canCreatureTypeSpawnBody}) asks for water
 * <em>material</em> at the spot and below it, so the water of a waterlogged block, which is kept beside the
 * block, never counts: no squid, fish or dolphin would ever spawn in a kelp forest's kelp or a reef's coral.
 * 1.14.4's test asks for a water fluid there instead, which a waterlogged block has. Forge lets a placement type
 * carry its own test; this gives {@code IN_WATER} 1.14.4's: water at the spot and below it (any water, kept in
 * a block or not), and no full cube above.
 */
public final class WaterSpawning {

    /** 1.14.4's {@code IN_WATER} placement test. */
    public static final BiPredicate<IBlockAccess, BlockPos> IN_WATER = new BiPredicate<IBlockAccess, BlockPos>() {
        @Override
        public boolean test(IBlockAccess world, BlockPos pos) {
            return Waterlogging.hasWater(world, pos) && Waterlogging.hasWater(world, pos.down())
                    && !world.getBlockState(pos.up()).isNormalCube();
        }
    };

    private WaterSpawning() {
    }

    /** Sets {@link #IN_WATER} on {@code SpawnPlacementType.IN_WATER}, unless something else already gave it a test. */
    public static void init() {
        try {
            // Forge's own field (not renamed by obfuscation).
            Field field = EntityLiving.SpawnPlacementType.class.getDeclaredField("spawnPredicate");
            field.setAccessible(true);
            if (field.get(EntityLiving.SpawnPlacementType.IN_WATER) == null) {
                field.set(EntityLiving.SpawnPlacementType.IN_WATER, IN_WATER);
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            UpdateAquaticBackport.LOGGER.warn("Water mobs will not spawn in waterlogged blocks: could not set the in-water spawn test.", e);
        }
    }
}
