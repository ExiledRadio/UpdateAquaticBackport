package com.exiledradio.updateaquaticbackport.ocean;

import com.exiledradio.updateaquaticbackport.common.OceanKind;

import net.minecraft.block.BlockSand;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.monster.EntityPolarBear;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeOcean;
import net.minecraft.world.chunk.ChunkPrimer;

import java.util.Random;

/**
 * One of 1.13's temperature oceans: warm, lukewarm, cold and their deep forms. Frozen ones are
 * {@link BiomeFrozenOcean}.
 *
 * <h2>Floors</h2>
 * 1.14.4's surface builder configurations, as three blocks: the top and filler of shallow floors near the
 * surface, and the one block laid on deep floors (vanilla 1.12.2 always uses gravel there):
 * <table>
 *   <tr><th></th><th>top, filler</th><th>deep floor</th></tr>
 *   <tr><td>warm</td><td>sand, sand</td><td>sand</td></tr>
 *   <tr><td>lukewarm (and deep)</td><td>grass, dirt</td><td>sand</td></tr>
 *   <tr><td>cold (and deep), frozen (and deep)</td><td>grass, dirt</td><td>gravel</td></tr>
 * </table>
 * Clay, sand and gravel disks come from vanilla's decorator, which places the same three kinds as 1.13.
 *
 * <h2>Numbers</h2>
 * 1.14.4's biome classes: depth -1.0 or -1.8 (deep), scale 0.1, temperature 0.5, downfall 0.5, and their
 * squid and polar bear spawns. Fish, dolphins and drowned are the other streams' to add; they find these
 * biomes through {@link OceanKind}.
 */
public class BiomeAquaticOcean extends BiomeOcean {

    private final OceanKind kind;
    private final boolean deep;
    private final IBlockState deepFloor;

    BiomeAquaticOcean(BiomeProperties properties, OceanKind kind, boolean deep, IBlockState top, IBlockState filler,
                      IBlockState deepFloor, int squidWeight, int squidMin, int squidMax) {
        super(properties);
        this.kind = kind;
        this.deep = deep;
        this.topBlock = top;
        this.fillerBlock = filler;
        this.deepFloor = deepFloor;
        this.spawnableWaterCreatureList.clear();
        this.spawnableWaterCreatureList.add(new SpawnListEntry(EntitySquid.class, squidWeight, squidMin, squidMax));
        if (kind == OceanKind.FROZEN) {
            this.spawnableCreatureList.add(new SpawnListEntry(EntityPolarBear.class, 1, 1, 2));
        }
    }

    public OceanKind kind() {
        return this.kind;
    }

    public boolean isDeep() {
        return this.deep;
    }

    /** @return the block laid on floors more than a few blocks under the surface */
    public IBlockState deepFloor() {
        return this.deepFloor;
    }

    @Override
    public void genTerrainBlocks(World world, Random rand, ChunkPrimer primer, int x, int z, double noise) {
        if (this.deepFloor.getBlock() == Blocks.GRAVEL) {
            // Exactly vanilla's: gravel is what it lays.
            this.generateBiomeTerrain(world, rand, primer, x, z, noise);
        } else {
            surface(world, rand, primer, x, z, noise, this.deepFloor);
        }
    }

    /**
     * Vanilla 1.12.2's {@code generateBiomeTerrain} with the deep floor block as a parameter, as 1.13's
     * default surface builder has it.
     *
     * <p>The primer is written at {@code (z & 15, y, x & 15)}, exactly as vanilla does, so this biome's
     * columns line up with the vanilla biomes next to it.
     */
    final void surface(World world, Random rand, ChunkPrimer primer, int x, int z, double noise, IBlockState floor) {
        int sea = world.getSeaLevel();
        IBlockState top = this.topBlock;
        IBlockState filler = this.fillerBlock;
        int depth = -1;
        int thickness = (int) (noise / 3.0D + 3.0D + rand.nextDouble() * 0.25D);
        int lx = x & 15;
        int lz = z & 15;

        for (int y = 255; y >= 0; --y) {
            if (y <= rand.nextInt(5)) {
                primer.setBlockState(lz, y, lx, BEDROCK);
                continue;
            }
            IBlockState here = primer.getBlockState(lz, y, lx);
            if (here.getMaterial() == Material.AIR) {
                depth = -1;
            } else if (here.getBlock() == Blocks.STONE) {
                if (depth == -1) {
                    if (thickness <= 0) {
                        top = AIR;
                        filler = STONE;
                    } else if (y >= sea - 4 && y <= sea + 1) {
                        top = this.topBlock;
                        filler = this.fillerBlock;
                    }
                    if (y < sea && (top == null || top.getMaterial() == Material.AIR)) {
                        top = WATER;
                    }
                    depth = thickness;
                    if (y >= sea - 1) {
                        primer.setBlockState(lz, y, lx, top);
                    } else if (y < sea - 7 - thickness) {
                        top = AIR;
                        filler = STONE;
                        primer.setBlockState(lz, y, lx, floor);
                    } else {
                        primer.setBlockState(lz, y, lx, filler);
                    }
                } else if (depth > 0) {
                    --depth;
                    primer.setBlockState(lz, y, lx, filler);
                    if (depth == 0 && filler.getBlock() == Blocks.SAND && thickness > 1) {
                        depth = rand.nextInt(4) + Math.max(0, y - 63);
                        filler = filler.getValue(BlockSand.VARIANT) == BlockSand.EnumType.RED_SAND ? RED_SANDSTONE : SANDSTONE;
                    }
                }
            }
        }
    }

    /** Biomes with temperature below this freeze water and hold snow, as in vanilla. */
    static final float FREEZING = 0.15F;

    /** @return true where a water source at this position would freeze at this temperature (vanilla's rule) */
    static boolean freezes(World world, BlockPos pos, float temperature) {
        if (temperature >= FREEZING || pos.getY() < 0 || pos.getY() >= 256
                || world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos) >= 10) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return (state.getBlock() == Blocks.WATER || state.getBlock() == Blocks.FLOWING_WATER)
                && state.getValue(net.minecraft.block.BlockLiquid.LEVEL) == 0;
    }

    /** @return true where snow would settle at this temperature (vanilla's rule) */
    static boolean snows(World world, BlockPos pos, float temperature) {
        if (temperature >= FREEZING || pos.getY() < 0 || pos.getY() >= 256
                || world.getLightFor(net.minecraft.world.EnumSkyBlock.BLOCK, pos) >= 10) {
            return false;
        }
        IBlockState state = world.getBlockState(pos);
        return state.getBlock().isAir(state, world, pos) && Blocks.SNOW_LAYER.canPlaceBlockAt(world, pos);
    }
}
