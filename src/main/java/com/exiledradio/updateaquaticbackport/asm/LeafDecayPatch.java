package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Leaves stay up to 6 blocks from a log (1.13; 1.12.2: 4). Two classes:
 *
 * <ul>
 *   <li>{@code BlockLeaves.updateTick} starts with
 *       {@code if (MechanicsHooks.leavesTick(this, world, pos, state)) return;}. The hook runs 1.12.2's check with
 *       1.13's range and answers true, or false (setting off) to let vanilla's run. Every leaves block that
 *       extends {@code BlockLeaves} and keeps its {@code updateTick} (vanilla's, most mods') gets it.</li>
 *   <li>{@code BlockLog.breakBlock} starts with {@code MechanicsHooks.logBroken(world, pos)}, which marks the
 *       leaves up to 6 blocks around to check (vanilla's marking, 4 around, follows).</li>
 * </ul>
 * The one branch added is at the head of {@code updateTick}, with its frame (the method's starting frame).
 */
public final class LeafDecayPatch implements IClassTransformer {

    static final String HOOKS = "com/exiledradio/updateaquaticbackport/mechanics/MechanicsHooks";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return bytes;
        }
        if ("net.minecraft.block.BlockLeaves".equals(transformedName)) {
            return leaves(bytes);
        }
        if ("net.minecraft.block.BlockLog".equals(transformedName)) {
            return log(bytes);
        }
        return bytes;
    }

    private static byte[] leaves(byte[] bytes) {
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "updateTick", "func_180650_b", "(" + WORLD + POS + STATE + "Ljava/util/Random;)V");
        if (method == null || callsHook(method, "leavesTick")) {
            Patches.LOGGER.warn("Patch LeafDecay skipped: no BlockLeaves.updateTick to patch; leaves decay past 4 blocks as in 1.12.2");
            return bytes;
        }
        LabelNode vanilla = new LabelNode();
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 0));
        head.add(new VarInsnNode(Opcodes.ALOAD, 1));
        head.add(new VarInsnNode(Opcodes.ALOAD, 2));
        head.add(new VarInsnNode(Opcodes.ALOAD, 3));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "leavesTick",
                "(Lnet/minecraft/block/BlockLeaves;" + WORLD + POS + STATE + ")Z", false));
        head.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        head.add(new InsnNode(Opcodes.RETURN));
        head.add(vanilla);
        head.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        method.instructions.insert(head);
        Patches.LOGGER.info("Patch LeafDecay applied to BlockLeaves.updateTick");
        return Patches.write(node);
    }

    private static byte[] log(byte[] bytes) {
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "breakBlock", "func_180663_b", "(" + WORLD + POS + STATE + ")V");
        if (method == null || callsHook(method, "logBroken")) {
            Patches.LOGGER.warn("Patch LeafDecay skipped: no BlockLog.breakBlock to patch; a broken log marks leaves 4 blocks around as in 1.12.2");
            return bytes;
        }
        InsnList head = new InsnList();
        head.add(new VarInsnNode(Opcodes.ALOAD, 1));
        head.add(new VarInsnNode(Opcodes.ALOAD, 2));
        head.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "logBroken", "(" + WORLD + POS + ")V", false));
        method.instructions.insert(head);
        Patches.LOGGER.info("Patch LeafDecay applied to BlockLog.breakBlock");
        return Patches.write(node);
    }

    static boolean callsHook(MethodNode method, String hook) {
        for (org.objectweb.asm.tree.AbstractInsnNode insn : method.instructions.toArray()) {
            if (insn instanceof MethodInsnNode && HOOKS.equals(((MethodInsnNode) insn).owner) && hook.equals(((MethodInsnNode) insn).name)) {
                return true;
            }
        }
        return false;
    }
}
