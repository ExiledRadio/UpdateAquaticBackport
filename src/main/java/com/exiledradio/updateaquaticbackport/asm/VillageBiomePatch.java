package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * A village starts where 1.13 says, by the biome 1.13 reads.
 *
 * <p>{@code MapGenVillage.canSpawnStructureAtCoords} asks
 * {@code world.getBiomeProvider().areBiomesViable(chunkX * 16 + 8, chunkZ * 16 + 8, 0, VILLAGE_SPAWN_BIOMES)}:
 * the 1:4 biome cell at the chunk's block (8, 8). 1.13's {@code VillageStructure.hasStartAt} reads the
 * block-scale biome at (9, 9). The one {@code BiomeProvider.areBiomesViable} call becomes
 * {@code Placement113.villageBiomes(provider, x, z, radius, biomes)}, which does that for radius 0 and passes
 * anything else through. Subclasses that call the vanilla method (Village &amp; Pillage's 1.14 villages) get
 * it too; the grid, the salt and the biome list are untouched.
 */
public final class VillageBiomePatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.world.gen.structure.MapGenVillage";
    private static final String HOOK = "com/exiledradio/updateaquaticbackport/structure/Placement113";
    private static final String DESC = "(IIILjava/util/List;)Z";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "canSpawnStructureAtCoords", "func_75047_a", "(II)Z");
        List<MethodInsnNode> calls = new ArrayList<MethodInsnNode>();
        if (method != null) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.owner.equals("net/minecraft/world/biome/BiomeProvider")
                            && Patches.named(call.name, "areBiomesViable", "func_76940_a") && call.desc.equals(DESC)) {
                        calls.add(call);
                    }
                }
            }
        }
        if (calls.size() != 1) {
            Patches.LOGGER.warn("Patch VillageBiome skipped: expected one areBiomesViable call in MapGenVillage.canSpawnStructureAtCoords, "
                    + "found {}; villages keep 1.12.2's biome point", calls.size());
            return bytes;
        }
        method.instructions.set(calls.get(0), new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "villageBiomes",
                "(Lnet/minecraft/world/biome/BiomeProvider;IIILjava/util/List;)Z", false));
        Patches.LOGGER.info("Patch VillageBiome applied to MapGenVillage.canSpawnStructureAtCoords");
        return Patches.write(node);
    }
}
