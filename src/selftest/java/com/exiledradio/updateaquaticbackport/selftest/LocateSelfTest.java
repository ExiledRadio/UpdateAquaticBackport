package com.exiledradio.updateaquaticbackport.selftest;

import com.exiledradio.backportcore.locate.LocateChecks;
import com.exiledradio.backportcore.locate.StructureLocate;
import com.exiledradio.backportcore.structure.ScatteredStructure;
import com.exiledradio.backportcore.structure.Structures;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.StructureStart;
import net.minecraftforge.fml.common.FMLCommonHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Shipwrecks, ocean ruins and buried treasure in Backport Core's {@code /locate}: each id finds a start, in today's
 * order, that the generator then lays out as that variant; the tags; buried treasure at the chest's column; the
 * command and tab completion.
 */
final class LocateSelfTest implements SelfTest {

    /** {id, kind}. */
    private static final String[][] IDS = {
            {"minecraft:shipwreck", Structures.SHIPWRECK},
            {"minecraft:shipwreck_beached", Structures.SHIPWRECK},
            {"minecraft:ocean_ruin_cold", Structures.OCEAN_RUIN},
            {"minecraft:ocean_ruin_warm", Structures.OCEAN_RUIN},
            {"minecraft:buried_treasure", Structures.BURIED_TREASURE}};

    @Override
    public String name() {
        return "locate";
    }

    @Override
    public void run(Check c) throws Exception {
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        WorldServer world = c.world;
        BlockPos from = world.getSpawnPoint();

        for (String[] row : IDS) {
            String id = row[0];
            StructureLocate.Found found = StructureLocate.nearest(world, from, id);
            if (!c.ok(found != null && id.equals(found.id), "locate finds " + id + ": " + found)) {
                continue;
            }
            UpdateAquaticBackport.LOGGER.info("SELFTEST locate {} from {}", found, from);
            String why = LocateChecks.checkRingOrder(world, from, id, found);
            c.ok(why == null, id + " is today's ring order: " + why);
            boolean treasure = Structures.BURIED_TREASURE.equals(row[1]);
            c.ok((found.pos.getX() & 15) == (treasure ? 9 : 0) && (found.pos.getZ() & 15) == (treasure ? 9 : 0),
                    id + " is reported at " + (treasure ? "the chest's column" : "the chunk's corner") + ": " + found);
            int cx = found.pos.getX() >> 4;
            int cz = found.pos.getZ() >> 4;
            LocateChecks.generateAround(world, cx, cz, 2);
            StructureStart start = LocateChecks.savedStart(ScatteredStructure.of(world, row[1]), cx, cz);
            if (!c.ok(start != null && start.isSizeableStructure(), "the generator laid out a start where " + id + " was located")) {
                continue;
            }
            NBTTagCompound piece = start.getComponents().get(0).createStructureBaseNBT();
            if (Structures.SHIPWRECK.equals(row[1])) {
                c.eq(id.endsWith("_beached"), piece.getBoolean("isBeached"), id + " is laid out beached or sunken as named");
            } else if (Structures.OCEAN_RUIN.equals(row[1])) {
                c.eq(id.endsWith("_warm") ? "WARM" : "COLD", piece.getString("BiomeType"), id + " is laid out warm or cold as named");
            }
        }

        for (String tag : new String[]{"#minecraft:shipwreck", "#minecraft:ocean_ruin"}) {
            StructureLocate.Found found = StructureLocate.nearest(world, from, tag);
            if (c.ok(found != null && StructureLocate.members(tag).contains(found.id), tag + " finds a member: " + found)) {
                String why = LocateChecks.checkRingOrder(world, from, tag, found);
                c.ok(why == null, tag + " is today's ring order: " + why);
                List<String> said = LocateChecks.run(server, world, from, "locate structure " + tag);
                c.ok(said.size() == 1 && said.get(0).startsWith("The nearest " + tag + " (" + found.id + ") is at [" + found.pos.getX()
                        + ", ~, " + found.pos.getZ() + "]"), tag + "'s reply names what it found: " + said);
            }
        }
        List<String> said = LocateChecks.run(server, world, from, "locate structure buried_treasure");
        c.ok(said.size() == 1 && said.get(0).startsWith("The nearest minecraft:buried_treasure is at ["), "the reply: " + said);

        WorldServer nether = server.getWorld(-1);
        c.ok(StructureLocate.nearest(nether, BlockPos.ORIGIN, "#minecraft:shipwreck") == null, "no shipwrecks in the nether");

        List<String> ids = LocateChecks.complete(server, world, from, "locate structure ");
        c.ok(ids.containsAll(Arrays.asList("minecraft:shipwreck", "minecraft:shipwreck_beached", "minecraft:ocean_ruin_cold",
                "minecraft:ocean_ruin_warm", "minecraft:buried_treasure")) && !ids.contains("#minecraft:shipwreck"),
                "tab completion lists the ids, tags only after '#' (Core df1c4b6): " + ids);
        List<String> tags = LocateChecks.complete(server, world, from, "locate structure #");
        c.ok(tags.containsAll(Arrays.asList("#minecraft:shipwreck", "#minecraft:ocean_ruin")), "after '#' it lists the tags: " + tags);
        List<String> ruins = LocateChecks.complete(server, world, from, "locate structure ocean_ruin_");
        c.eq(Arrays.asList("minecraft:ocean_ruin_cold", "minecraft:ocean_ruin_warm"), ruins, "completion by path");
    }
}
