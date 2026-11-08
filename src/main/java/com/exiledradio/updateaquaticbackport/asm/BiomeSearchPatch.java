package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * The biome search behind strongholds and the spawn point picks the way 1.13's does.
 *
 * <p>{@code BiomeProvider.findBiomePosition} walks the 1:4 cells of a square and keeps one matching cell,
 * replacing the kept one with chance {@code 1 / (n + 1)}. 1.12.2 counts {@code n} only when it replaces, 1.13
 * ({@code OverworldBiomeProvider.findBiomePosition}) counts every matching cell, so the two draw different
 * random numbers and pick different cells; every stronghold after the first difference moves, since all 128
 * share one random. The method's body becomes a call to {@code Placement113.findBiomePosition}, 1.13's loop
 * over the same cells (read through {@code getBiomesForGeneration}).
 */
public final class BiomeSearchPatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.world.biome.BiomeProvider";
    private static final String HOOK = "com/exiledradio/updateaquaticbackport/structure/Placement113";
    private static final String DESC = "(IIILjava/util/List;Ljava/util/Random;)Lnet/minecraft/util/math/BlockPos;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "findBiomePosition", "func_180630_a", DESC);
        if (method == null) {
            Patches.LOGGER.warn("Patch BiomeSearch skipped: no BiomeProvider.findBiomePosition; strongholds and spawn keep 1.12.2's pick");
            return bytes;
        }
        // The new body has no branch, so it needs no stack map frames.
        method.instructions.clear();
        method.tryCatchBlocks.clear();
        if (method.localVariables != null) {
            method.localVariables.clear();
        }
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOK, "findBiomePosition",
                "(Lnet/minecraft/world/biome/BiomeProvider;IIILjava/util/List;Ljava/util/Random;)Lnet/minecraft/util/math/BlockPos;", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        Patches.LOGGER.info("Patch BiomeSearch applied to BiomeProvider.findBiomePosition");
        return Patches.write(node);
    }
}
