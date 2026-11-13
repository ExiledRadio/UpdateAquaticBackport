package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.13's Biome Blend: {@code BiomeColorHelper} averages the biome colors over 1.13's square instead of 1.12.2's
 * fixed 3x3.
 *
 * <p>{@code BiomeColorHelper.getColorAtPos} (private, client only) walks
 * {@code getAllInBoxMutable(pos.add(-1, 0, -1), pos.add(1, 0, 1))} and divides each channel's sum by 9. The four
 * {@code ICONST_M1}/{@code ICONST_1} arguments of the two {@code BlockPos.add(int, int, int)} calls become
 * {@code BiomeBlend.low()} and {@code BiomeBlend.high()}, and the three {@code BIPUSH 9} before an {@code IDIV}
 * become {@code BiomeBlend.count()}. The two {@code ICONST_0} (y) and everything else stay.
 */
public final class BiomeBlendPatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.world.biome.BiomeColorHelper";
    private static final String HOOKS = "com/exiledradio/updateaquaticbackport/water/BiomeBlend";
    private static final String BLOCK_POS = "net/minecraft/util/math/BlockPos";
    private static final String ADD_DESC = "(III)L" + BLOCK_POS + ";";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        org.objectweb.asm.tree.ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "getColorAtPos", "func_180285_a",
                "(Lnet/minecraft/world/IBlockAccess;Lnet/minecraft/util/math/BlockPos;"
                        + "Lnet/minecraft/world/biome/BiomeColorHelper$ColorResolver;)I");
        List<AbstractInsnNode> lows = new ArrayList<AbstractInsnNode>();
        List<AbstractInsnNode> highs = new ArrayList<AbstractInsnNode>();
        List<AbstractInsnNode> nines = new ArrayList<AbstractInsnNode>();
        int adds = 0;
        if (method != null) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.owner.equals(BLOCK_POS) && Patches.named(call.name, "add", "func_177982_a") && call.desc.equals(ADD_DESC)) {
                        adds++;
                        // x, y, z: the three instructions before the call.
                        AbstractInsnNode z = call.getPrevious();
                        AbstractInsnNode y = z == null ? null : z.getPrevious();
                        AbstractInsnNode x = y == null ? null : y.getPrevious();
                        if (x != null && y.getOpcode() == Opcodes.ICONST_0 && x.getOpcode() == z.getOpcode()) {
                            if (x.getOpcode() == Opcodes.ICONST_M1) {
                                lows.add(x);
                                lows.add(z);
                            } else if (x.getOpcode() == Opcodes.ICONST_1) {
                                highs.add(x);
                                highs.add(z);
                            }
                        }
                    }
                } else if (insn.getOpcode() == Opcodes.BIPUSH && ((IntInsnNode) insn).operand == 9
                        && insn.getNext() != null && insn.getNext().getOpcode() == Opcodes.IDIV) {
                    nines.add(insn);
                }
            }
        }
        if (adds != 2 || lows.size() != 2 || highs.size() != 2 || nines.size() != 3) {
            Patches.LOGGER.warn("Patch BiomeBlend skipped: BiomeColorHelper.getColorAtPos is not 1.12.2's 3x3 average "
                    + "(adds {}, corners {}/{}, divisors {}); biome colors blend 3x3 whatever Biome Blend says",
                    adds, lows.size(), highs.size(), nines.size());
            return bytes;
        }
        for (AbstractInsnNode insn : lows) {
            method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "low", "()I", false));
        }
        for (AbstractInsnNode insn : highs) {
            method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "high", "()I", false));
        }
        for (AbstractInsnNode insn : nines) {
            method.instructions.set(insn, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "count", "()I", false));
        }
        Patches.LOGGER.info("Patch BiomeBlend applied to BiomeColorHelper.getColorAtPos");
        return Patches.write(node);
    }
}
