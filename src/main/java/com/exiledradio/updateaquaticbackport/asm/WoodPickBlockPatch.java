package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Middle-clicking 1.13's wood picks the wood item, not the log.
 *
 * <p>This mod's wood is vanilla's bark log ({@code axis=none}) with an item of its own; vanilla's pick
 * ({@code Block.getPickBlock}, Forge's) gives the log's item. {@code BlockLog} gets an override of
 * {@code getPickBlock} that asks vanilla first and passes the answer through
 * {@code BlocksHooks.pickBlock(this, state, answer)}:
 * <pre>
 *     public ItemStack getPickBlock(IBlockState state, RayTraceResult target, World world, BlockPos pos, EntityPlayer player) {
 *         return BlocksHooks.pickBlock(this, state, super.getPickBlock(state, target, world, pos, player));
 *     }
 * </pre>
 * The hook changes the answer only for vanilla's two log blocks in the bark state. {@code getPickBlock} is
 * Forge's own method, so it has one name everywhere. If {@code BlockLog} already has one (another patch),
 * nothing is added.
 */
public final class WoodPickBlockPatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.block.BlockLog";
    private static final String DESC = "(Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/util/math/RayTraceResult;"
            + "Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/EntityPlayer;)"
            + "Lnet/minecraft/item/ItemStack;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        if (Patches.method(node, "getPickBlock", "getPickBlock", DESC) != null) {
            Patches.LOGGER.warn("Patch WoodPickBlock: BlockLog already has getPickBlock; wood still picks the log");
            return bytes;
        }
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "getPickBlock", DESC, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        for (int i = 0; i <= 5; i++) {
            method.instructions.add(new VarInsnNode(Opcodes.ALOAD, i));
        }
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, node.superName, "getPickBlock", DESC, false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Patches.HOOKS, "pickBlock",
                "(Lnet/minecraft/block/Block;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/item/ItemStack;)"
                        + "Lnet/minecraft/item/ItemStack;", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        node.methods.add(method);
        Patches.LOGGER.info("Patch WoodPickBlock applied: BlockLog.getPickBlock added");
        return Patches.write(node);
    }
}
