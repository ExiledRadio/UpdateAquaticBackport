package com.exiledradio.updateaquaticbackport.bubble;

import com.exiledradio.backportcore.terrain.WorldHeights;
import com.exiledradio.updateaquaticbackport.ModConfig;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.ExtendedBlockStorage;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraftforge.fml.common.IWorldGenerator;

import java.util.Random;

/**
 * Columns in newly generated chunks: 1.13 starts a column over every magma block (and soul sand) that
 * world generation leaves under water, such as the magma at the bottom of underwater ravines, because its
 * magma block is post-processed when a chunk is finished. Here that is a generator that runs after all
 * the others ({@link BubbleContent#GENERATOR_WEIGHT}) and looks at the area they have just populated.
 *
 * <p>Nothing happens in dimensions where water boils away (the Nether's soul sand), or with
 * {@code bubbles.generate} off. The scan reads block storage directly and skips empty sections.
 *
 * <p>In Caves &amp; Cliffs Backport's 1.18 worlds this still runs: 1.18.2 post-processes the magma its
 * {@code underwater_magma} feature places, so a column rises from every one with water above, below y 0 too. The
 * sections are read by their y ({@code WorldHeights}): there slots 16 to 19 hold y -64 to -1.
 */
public final class BubbleGenerator implements IWorldGenerator {

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator generator, IChunkProvider provider) {
        if (!ModConfig.bubbles.generate || BubbleContent.column() == null || world.provider.doesWaterVaporize()) {
            return;
        }
        formInArea(world, chunkX * 16 + 8, chunkZ * 16 + 8);
    }

    /**
     * Starts a column over every soul sand and magma block with a water source above it in the 16x16
     * area from {@code (x0, z0)}: the area a chunk's population writes to.
     *
     * @return how many columns were started
     */
    public static int formInArea(World world, int x0, int z0) {
        int formed = 0;
        int minY = WorldHeights.minY(world) & ~15;
        int maxY = WorldHeights.maxYExclusive(world);
        for (int x = x0; x < x0 + 16; x++) {
            for (int z = z0; z < z0 + 16; z++) {
                Chunk chunk = world.getChunk(x >> 4, z >> 4);
                ExtendedBlockStorage[] sections = chunk.getBlockStorageArray();
                // By the section's own y: with Caves & Cliffs' extended height the slots past 15 are y -64 to -1.
                for (int sy = minY; sy < maxY; sy += 16) {
                    int s = WorldHeights.storageIndex(world, sy);
                    ExtendedBlockStorage section = s < 0 || s >= sections.length ? null : sections[s];
                    if (section == Chunk.NULL_BLOCK_STORAGE || section.isEmpty()) {
                        continue;
                    }
                    for (int y = 0; y < 16; y++) {
                        IBlockState state = section.get(x & 15, y, z & 15);
                        if (!BubbleColumns.isBase(state.getBlock())) {
                            continue;
                        }
                        BlockPos above = new BlockPos(x, sy + y + 1, z);
                        IBlockState up = chunk.getBlockState(above);
                        if (BubbleColumns.isVanillaSource(up) && BubbleColumns.updateColumn(world, above, state) > 0) {
                            formed++;
                        }
                    }
                }
            }
        }
        return formed;
    }
}
