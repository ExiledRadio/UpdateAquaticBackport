package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain pumpkin no longer builds snow golems or iron golems (1.13: only a carved pumpkin or a jack
 * o'lantern does), whoever places it: a player, another mod, a command.
 *
 * <p>{@code BlockPumpkin.trySpawnGolem} (private, run by {@code onBlockAdded}) builds a golem when one of
 * its two patterns matches. Its two {@code BlockPattern.match(World, BlockPos)} calls become calls to
 * {@code BlocksHooks.golemMatch(pattern, world, pos, this)}, which gives no match for the plain pumpkin
 * and vanilla's match for every other {@code BlockPumpkin} (the carved pumpkin, the jack o'lantern,
 * subclasses). Nothing else in the method changes.
 */
public final class PumpkinGolemPatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.block.BlockPumpkin";
    private static final String PATTERN = "net/minecraft/block/state/pattern/BlockPattern";
    private static final String MATCH_DESC = "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)L" + PATTERN + "$PatternHelper;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "trySpawnGolem", "func_180673_e",
                "(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;)V");
        List<MethodInsnNode> calls = new ArrayList<MethodInsnNode>();
        if (method != null) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    MethodInsnNode call = (MethodInsnNode) insn;
                    if (call.owner.equals(PATTERN) && Patches.named(call.name, "match", "func_177681_a") && call.desc.equals(MATCH_DESC)) {
                        calls.add(call);
                    }
                }
            }
        }
        if (calls.size() != 2) {
            Patches.LOGGER.warn("Patch PumpkinGolem skipped: expected two pattern matches in BlockPumpkin.trySpawnGolem, found {}; "
                    + "a plain pumpkin placed by code can still build a golem", calls.size());
            return bytes;
        }
        for (MethodInsnNode call : calls) {
            method.instructions.insertBefore(call, new VarInsnNode(Opcodes.ALOAD, 0));
            method.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, Patches.HOOKS, "golemMatch",
                    "(L" + PATTERN + ";Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;"
                            + "Lnet/minecraft/block/BlockPumpkin;)L" + PATTERN + "$PatternHelper;", false));
        }
        Patches.LOGGER.info("Patch PumpkinGolem applied to BlockPumpkin.trySpawnGolem");
        return Patches.write(node);
    }
}
