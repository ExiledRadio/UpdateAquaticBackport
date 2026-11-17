package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Players in Creative can sleep with monsters nearby (1.13, 18w20a).
 *
 * <p>In {@code EntityPlayer.trySleep}, the one {@code List.isEmpty()} call (the monsters near the bed) becomes
 * {@code MechanicsHooks.noMonstersNear(list, this)}: true for an empty list, or for a player in Creative while the
 * setting is on. Nothing else in the method changes.
 */
public final class CreativeSleepPatch implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.entity.player.EntityPlayer".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "trySleep", "func_180469_a",
                "(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/entity/player/EntityPlayer$SleepResult;");
        MethodInsnNode call = null;
        int found = 0;
        if (method != null) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEINTERFACE && "java/util/List".equals(((MethodInsnNode) insn).owner)
                        && "isEmpty".equals(((MethodInsnNode) insn).name)) {
                    call = (MethodInsnNode) insn;
                    found++;
                }
            }
        }
        if (found != 1) {
            Patches.LOGGER.warn("Patch CreativeSleep skipped: expected one monster check in EntityPlayer.trySleep, found {}", found);
            return bytes;
        }
        method.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, LeafDecayPatch.HOOKS, "noMonstersNear",
                "(Ljava/util/List;Lnet/minecraft/entity/player/EntityPlayer;)Z", false));
        Patches.LOGGER.info("Patch CreativeSleep applied to EntityPlayer.trySleep");
        return Patches.write(node);
    }
}
