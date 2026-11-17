package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * The witch hut's flower pot holds a red mushroom (1.13, 17w47a).
 *
 * <p>1.12.2's {@code ComponentScatteredFeaturePieces.SwampHut.addComponentParts} places
 * {@code FLOWER_POT.withProperty(CONTENTS, MUSHROOM_RED)}, but a pot's plant lives in its tile entity (the property is
 * only the drawn state), so the pot is empty. Right after the {@code setBlockState} call that follows
 * {@code Blocks.FLOWER_POT}, this adds {@code MechanicsHooks.swampHutPot(this, world, box)}, which puts a red
 * mushroom in the hut's empty pots.
 */
public final class SwampHutPotPatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.world.gen.structure.ComponentScatteredFeaturePieces$SwampHut";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "addComponentParts", "func_74875_a",
                "(Lnet/minecraft/world/World;Ljava/util/Random;Lnet/minecraft/world/gen/structure/StructureBoundingBox;)Z");
        MethodInsnNode call = null;
        if (method != null && !LeafDecayPatch.callsHook(method, "swampHutPot")) {
            boolean pot = false;
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.GETSTATIC && "net/minecraft/init/Blocks".equals(((FieldInsnNode) insn).owner)
                        && Patches.named(((FieldInsnNode) insn).name, "FLOWER_POT", "field_150457_bL")) {
                    pot = true;
                } else if (pot && insn.getOpcode() == Opcodes.INVOKEVIRTUAL
                        && Patches.named(((MethodInsnNode) insn).name, "setBlockState", "func_175811_a")) {
                    call = (MethodInsnNode) insn;
                    break;
                }
            }
        }
        if (call == null) {
            Patches.LOGGER.warn("Patch SwampHutPot skipped: no flower pot placement found in SwampHut.addComponentParts; witch huts keep an empty pot");
            return bytes;
        }
        InsnList after = new InsnList();
        after.add(new VarInsnNode(Opcodes.ALOAD, 0));
        after.add(new VarInsnNode(Opcodes.ALOAD, 1));
        after.add(new VarInsnNode(Opcodes.ALOAD, 3));
        after.add(new MethodInsnNode(Opcodes.INVOKESTATIC, LeafDecayPatch.HOOKS, "swampHutPot",
                "(Lnet/minecraft/world/gen/structure/StructureComponent;Lnet/minecraft/world/World;"
                        + "Lnet/minecraft/world/gen/structure/StructureBoundingBox;)V", false));
        method.instructions.insert(call, after);
        Patches.LOGGER.info("Patch SwampHutPot applied to SwampHut.addComponentParts");
        return Patches.write(node);
    }
}
