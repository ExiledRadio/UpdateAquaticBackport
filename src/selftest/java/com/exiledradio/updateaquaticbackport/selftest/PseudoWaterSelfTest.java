package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.waterlog.Waterlogging;
import com.exiledradio.updateaquaticbackport.bubble.BlockBubbleColumn;
import com.exiledradio.updateaquaticbackport.bubble.BubbleContent;
import com.exiledradio.updateaquaticbackport.common.BlockUnderwater;
import com.exiledradio.updateaquaticbackport.common.PseudoWater;
import com.exiledradio.updateaquaticbackport.common.PseudoWaterBlock;
import com.exiledradio.updateaquaticbackport.conduit.BlockConduit;
import com.exiledradio.updateaquaticbackport.conduit.ConduitContent;
import com.exiledradio.updateaquaticbackport.coral.BlockCoral;
import com.exiledradio.updateaquaticbackport.coral.BlockCoralWallFan;
import com.exiledradio.updateaquaticbackport.coral.CoralContent;
import com.exiledradio.updateaquaticbackport.coral.CoralType;
import com.exiledradio.updateaquaticbackport.kelp.BlockKelp;
import com.exiledradio.updateaquaticbackport.kelp.BlockSeagrass;
import com.exiledradio.updateaquaticbackport.kelp.KelpContent;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

/**
 * The model used without Fluidlogged API ({@code common.PseudoWater}): the blocks are the water in their space.
 *
 * <p>The self-tests run with Fluidlogged API, so this checks two things. The registered blocks use waterlogging
 * and carry nothing of the other model. And blocks constructed here as they would be without Fluidlogged API
 * ({@link PseudoWater#forceForTest}) are the water: water material with {@code LEVEL} 0, water's light opacity,
 * coral's {@code waterlogged} in its metadata bit, water left behind, the bucket's answers. They are never
 * registered or placed; a real game without Fluidlogged API is the rest of the test.
 */
public final class PseudoWaterSelfTest implements SelfTest {

    @Override
    public String name() {
        return "pseudowater";
    }

    @Override
    public void run(Check c) throws Exception {
        // 1. with Fluidlogged API, the registered blocks are waterlogged, not water -------------------------------
        c.ok(Waterlogging.isAvailable() && !PseudoWater.active(), "Fluidlogged API is installed, so the blocks are waterlogged");
        for (Block block : new Block[] {KelpContent.kelp(), KelpContent.kelpPlant(), KelpContent.seagrass(),
                BubbleContent.column(), ConduitContent.conduit(), CoralContent.CORALS.get(CoralType.TUBE),
                CoralContent.WALL_FANS.get(CoralType.FIRE)}) {
            if (block == null) {
                continue;
            }
            IBlockState state = block.getDefaultState();
            c.ok(!PseudoWater.isPseudoWater(block) && !((PseudoWaterBlock) block).isPseudoWater()
                            && !state.getPropertyKeys().contains(PseudoWater.LEVEL)
                            && !state.getPropertyKeys().contains(PseudoWater.SURFACE)
                            && !state.getPropertyKeys().contains(PseudoWater.WATERLOGGED)
                            && state.getMaterial() != Material.WATER,
                    block.getRegistryName() + " is not water itself: " + state);
        }

        // 2. made as without Fluidlogged API ---------------------------------------------------------------------
        BlockKelp kelp;
        BlockSeagrass seagrass;
        BlockBubbleColumn column;
        BlockCoral coral;
        BlockCoral dead;
        BlockCoralWallFan fan;
        BlockConduit conduit;
        PseudoWater.forceForTest(true);
        try {
            c.ok(PseudoWater.active(), "forced: the model is used");
            kelp = new BlockKelp(true);
            seagrass = new BlockSeagrass(false);
            column = new BlockBubbleColumn();
            coral = new BlockCoral(CoralType.TUBE, false, true, SoundType.PLANT);
            dead = new BlockCoral(CoralType.TUBE, true, false, SoundType.STONE);
            fan = new BlockCoralWallFan(CoralType.BRAIN, true, SoundType.PLANT);
            conduit = new BlockConduit();
        } finally {
            PseudoWater.forceForTest(null);
        }
        c.ok(!PseudoWater.active(), "unforced again");

        // Always water: kelp, seagrass, bubble columns.
        for (BlockUnderwater b : new BlockUnderwater[] {kelp, seagrass, column}) {
            IBlockState s = b.getDefaultState();
            String n = b.getClass().getSimpleName();
            c.ok(b.isPseudoWater() && PseudoWater.isPseudoWater(b), n + " is the water in its space");
            c.ok(s.getPropertyKeys().contains(PseudoWater.LEVEL) && s.getPropertyKeys().contains(PseudoWater.SURFACE)
                    && !s.getPropertyKeys().contains(PseudoWater.WATERLOGGED), n + ": LEVEL and SURFACE, no waterlogged: " + s);
            c.ok(s.getValue(PseudoWater.LEVEL) == 0 && !s.getValue(PseudoWater.SURFACE), n + ": default LEVEL 0, no surface: " + s);
            c.eq(Material.WATER, s.getMaterial(), n + " material");
            c.eq(PseudoWater.WATER_OPACITY, s.getLightOpacity(), n + " light opacity");
            c.ok(b.isWater(s), n + " is water");
            c.eq(Blocks.WATER.getDefaultState(), PseudoWater.leftBehind(s), n + " leaves water behind");
            c.ok(b.filled(s) == null, n + " cannot be filled");
            c.ok(!b.isReplaceable(c.world, BlockPos.ORIGIN) || b == seagrass || b == column,
                    n + " is not replaced as water would be");
        }
        c.ok(kelp.drained(kelp.getDefaultState()) == null && seagrass.drained(seagrass.getDefaultState()) == null,
                "an empty bucket takes nothing from kelp or seagrass");
        c.eq(Blocks.AIR.getDefaultState(), column.drained(column.getDefaultState()), "an empty bucket on a bubble column leaves air");
        IBlockState aged = kelp.getDefaultState().withProperty(BlockKelp.AGE, 7);
        c.eq(aged, kelp.getStateFromMeta(kelp.getMetaFromState(aged)), "kelp metadata round trip");
        c.eq(7, kelp.getMetaFromState(aged), "kelp metadata is its age alone");
        c.eq(1, column.getMetaFromState(column.withDrag(false)), "bubble column metadata is its drag alone");

        // Coral, fans and the conduit: water when waterlogged.
        IBlockState wet = coral.getDefaultState();
        IBlockState dry = BlockUnderwater.dry(wet);
        c.ok(coral.isPseudoWater() && wet.getPropertyKeys().contains(PseudoWater.WATERLOGGED), "coral has waterlogged: " + wet);
        c.ok(wet.getValue(PseudoWater.WATERLOGGED), "coral is made waterlogged by default, as generation places it");
        c.eq(Material.WATER, wet.getMaterial(), "waterlogged coral material");
        c.ok(dry.getMaterial() != Material.WATER && dry.getMaterial().blocksMovement() && !dry.getMaterial().isSolid(),
                "dry coral blocks liquids without being solid: " + dry.getMaterial());
        c.eq(PseudoWater.WATER_OPACITY, wet.getLightOpacity(), "waterlogged coral light opacity");
        c.eq(0, dry.getLightOpacity(), "dry coral light opacity");
        c.eq(1, coral.getMetaFromState(wet), "waterlogged coral: metadata bit 1");
        c.eq(0, coral.getMetaFromState(dry), "dry coral: metadata 0");
        c.eq(wet, coral.getStateFromMeta(1), "coral metadata 1 reads waterlogged");
        c.eq(dry, coral.getStateFromMeta(0), "coral metadata 0 reads dry");
        c.eq(dry, coral.drained(wet), "an empty bucket dries coral");
        c.eq(wet, coral.filled(dry), "a water bucket fills coral");
        c.ok(coral.drained(dry) == null && coral.filled(wet) == null, "nothing to take from dry coral or add to wet");
        c.eq(Blocks.AIR.getDefaultState(), PseudoWater.leftBehind(dry), "dry coral leaves air");
        c.ok(dead.isPseudoWater() && dead.getStateFromMeta(1).getMaterial() == Material.WATER, "dead coral can be waterlogged too");
        c.ok(!coral.isReplaceable(c.world, BlockPos.ORIGIN), "coral is not replaced as water would be");

        IBlockState east = fan.getDefaultState().withProperty(BlockCoralWallFan.FACING, EnumFacing.EAST);
        int eastMeta = EnumFacing.EAST.getHorizontalIndex();
        c.eq(eastMeta | 4, fan.getMetaFromState(east), "waterlogged wall fan: facing plus bit 4");
        c.eq(eastMeta, fan.getMetaFromState(BlockUnderwater.dry(east)), "dry wall fan: facing alone");
        c.eq(east, fan.getStateFromMeta(eastMeta | 4), "wall fan metadata round trip, waterlogged");
        c.eq(BlockUnderwater.dry(east), fan.getStateFromMeta(eastMeta), "wall fan metadata round trip, dry");

        IBlockState conduitDry = conduit.getDefaultState();
        c.ok(conduit.isPseudoWater() && !conduitDry.getValue(PseudoWater.WATERLOGGED), "the conduit is made dry: " + conduitDry);
        c.eq(Material.GLASS, conduitDry.getMaterial(), "a dry conduit is glass");
        IBlockState conduitWet = conduit.filled(conduitDry);
        c.ok(conduitWet != null && conduitWet.getMaterial() == Material.WATER, "a filled conduit is water");
        c.eq(1, conduit.getMetaFromState(conduitWet), "waterlogged conduit: metadata bit 1");
        c.eq(conduitWet, conduit.getStateFromMeta(1), "conduit metadata round trip");

        // Placement reads the water it replaces; drawing reads the block above.
        BlockPos at = c.area().add(4, 4, 4);
        c.world.setBlockState(at, Blocks.WATER.getDefaultState(), 2);
        c.world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), 2);
        c.ok(coral.placedIn(dry, c.world, at).getValue(PseudoWater.WATERLOGGED), "coral placed into a water source is waterlogged");
        c.ok(PseudoWater.actualState(wet, c.world, at).getValue(PseudoWater.SURFACE), "under air, wet coral draws the water surface");
        c.ok(!PseudoWater.actualState(dry, c.world, at).getValue(PseudoWater.SURFACE), "dry coral draws none");
        c.world.setBlockState(at.up(), Blocks.WATER.getDefaultState(), 2);
        c.ok(!PseudoWater.actualState(wet, c.world, at).getValue(PseudoWater.SURFACE), "under water, no surface");
        c.world.setBlockState(at, Blocks.AIR.getDefaultState(), 2);
        c.ok(!coral.placedIn(wet, c.world, at).getValue(PseudoWater.WATERLOGGED), "coral placed into air is dry");
        c.world.setBlockState(at.up(), Blocks.AIR.getDefaultState(), 2);

        // With Fluidlogged API, placing leaves the state alone: Fluidlogged API keeps the water.
        Block registered = CoralContent.CORALS.get(CoralType.TUBE);
        if (registered instanceof BlockUnderwater) {
            IBlockState s = registered.getDefaultState();
            c.eq(s, ((BlockUnderwater) registered).placedIn(s, c.world, at), "with Fluidlogged API placing changes nothing");
            c.eq(s, BlockUnderwater.dry(s), "and drying changes no state");
        }
    }
}
