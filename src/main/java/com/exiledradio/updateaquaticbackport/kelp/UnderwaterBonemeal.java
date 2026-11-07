package com.exiledradio.updateaquaticbackport.kelp;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.OceanKind;
import com.exiledradio.updateaquaticbackport.common.Owners;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.BonemealEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bone meal under water, from 1.14.4's {@code BoneMealItem.growSeagrass}: used on the sea floor (or by a
 * dispenser into water) it spreads seagrass over the floor around, and grows some of the seagrass already
 * there tall. In warm oceans a quarter of it is coral and coral fans instead, when the coral stream is on
 * (looked up by name).
 *
 * <p>Through Forge's {@link BonemealEvent}, which both a player's bone meal and a dispenser's fire. A player
 * aims at the floor, so the water is the block beside the face they aimed at; a dispenser aims at the
 * water itself.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class UnderwaterBonemeal {

    /** 1.14.4's {@code underwater_bonemeals} tag: the coral plants and fans bone meal can make. */
    private static final String[] CORALS = {"tube", "brain", "bubble", "fire", "horn"};

    private UnderwaterBonemeal() {
    }

    @SubscribeEvent
    public static void onBonemeal(BonemealEvent event) {
        if (!KelpContent.enabled() || event.getResult() != Event.Result.DEFAULT || event.isCanceled()) {
            return;
        }
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (event.getBlock().getBlock() instanceof net.minecraft.block.IGrowable) {
            return;
        }
        BlockPos water = pos;
        if (!BlockKelp.isVanillaWaterSource(world, water)) {
            water = pos.offset(face(event.getEntityPlayer(), pos));
        }
        if (!BlockKelp.isVanillaWaterSource(world, water)) {
            return;
        }
        if (!world.isRemote) {
            grow(world, water, world.rand);
        }
        // Forge takes the bone meal and the caller shows the particles.
        event.setResult(Event.Result.ALLOW);
    }

    /** The face of {@code pos} a player is looking at; up when there is no player or no hit. */
    private static EnumFacing face(EntityPlayer player, BlockPos pos) {
        if (player != null) {
            // Entity.rayTrace is client-only in 1.12.2; this is the same trace, built from parts a server has.
            Vec3d eyes = player.getPositionEyes(1.0F);
            Vec3d end = eyes.add(player.getLook(1.0F).scale(6.0));
            RayTraceResult hit = player.world.rayTraceBlocks(eyes, end, false, false, true);
            if (hit != null && hit.typeOfHit == RayTraceResult.Type.BLOCK && pos.equals(hit.getBlockPos()) && hit.sideHit != null) {
                return hit.sideHit;
            }
        }
        return EnumFacing.UP;
    }

    /** 1.14.4's 128 tries, each a short random walk from the water bone meal was used in. */
    static void grow(World world, BlockPos start, Random random) {
        BlockSeagrass seagrass = KelpContent.seagrass();
        List<Block> corals = corals();
        tries:
        for (int i = 0; i < 128; i++) {
            BlockPos pos = start;
            for (int j = 0; j < i / 16; j++) {
                pos = pos.add(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
                if (world.getBlockState(pos).isNormalCube()) {
                    continue tries;
                }
            }
            Block block = seagrass;
            if (!corals.isEmpty() && OceanKind.at(world, pos) == OceanKind.WARM && random.nextInt(4) == 0) {
                block = corals.get(random.nextInt(corals.size()));
            }
            IBlockState here = world.getBlockState(pos);
            if (BlockKelp.isVanillaWaterSource(world, pos) && block.canPlaceBlockAt(world, pos)) {
                world.setBlockState(pos, block.getDefaultState(), 3);
            } else if (here.getBlock() == seagrass && random.nextInt(10) == 0) {
                seagrass.growTall(world, pos);
            }
        }
    }

    private static List<Block> corals() {
        List<Block> out = new ArrayList<Block>();
        for (String c : CORALS) {
            for (String form : new String[] {"_coral", "_coral_fan"}) {
                Block block = Owners.ourBlock(c + form);
                if (block != null) {
                    out.add(block);
                }
            }
        }
        return out;
    }
}
