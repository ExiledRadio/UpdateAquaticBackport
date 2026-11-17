package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Signs and banners can be placed beside a cactus, which then breaks (1.13, 17w47a).
 *
 * <p>1.12.2's {@code BlockSign.canPlaceBlockAt} and {@code BlockBanner.canPlaceBlockAt} refuse a spot with a cactus
 * beside it ({@code hasInvalidNeighbor}). The boolean that call returns goes through
 * {@code MechanicsHooks.cactusBeside(boolean)}, which answers false while the setting is on. Nothing else changes:
 * the sign or banner is placed, its neighbor update reaches the cactus, and 1.12.2's cactus breaks beside a solid
 * block (a sign's and a banner's material, wood, is solid), as 1.13's does.
 */
public final class CactusSignPatch implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !"net.minecraft.block.BlockSign".equals(transformedName) && !"net.minecraft.block.BlockBanner".equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "canPlaceBlockAt", "func_176196_c",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z");
        MethodInsnNode call = null;
        int found = 0;
        if (method != null && !LeafDecayPatch.callsHook(method, "cactusBeside")) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL && Patches.named(((MethodInsnNode) insn).name, "hasInvalidNeighbor", "func_181087_e")
                        && ((MethodInsnNode) insn).desc.equals("(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)Z")) {
                    call = (MethodInsnNode) insn;
                    found++;
                }
            }
        }
        if (found != 1) {
            Patches.LOGGER.warn("Patch CactusSign skipped in {}: expected one cactus check, found {}", transformedName, found);
            return bytes;
        }
        method.instructions.insert(call, new MethodInsnNode(Opcodes.INVOKESTATIC, LeafDecayPatch.HOOKS, "cactusBeside", "(Z)Z", false));
        Patches.LOGGER.info("Patch CactusSign applied to {}.canPlaceBlockAt", transformedName);
        return Patches.write(node);
    }
}
