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
 * P11: chests pair by a stored pairing, as 1.13's do (hooks: {@code mechanics/ChestPairing}). Each method that 1.12.2
 * pairs chests in starts with {@code if (ChestPairing.handles(this)) return ChestPairing.<hook>(...);}, so with the
 * setting off, or for another mod's chest subclass, 1.12.2's own code runs:
 *
 * <ul>
 *   <li>{@code BlockChest}: {@code getStateForPlacement}, {@code onBlockPlacedBy}, {@code canPlaceBlockAt},
 *       {@code getContainer} (inventories, hoppers, comparators), {@code getBoundingBox}; {@code onBlockAdded} returns
 *       at once (1.12.2 turned neighbor chests to line up); {@code breakBlock} first calls {@code ChestPairing.broken}.</li>
 *   <li>{@code TileEntityChest}: {@code getAdjacentChest} (which fills the {@code adjacentChest*} fields the renderer, the
 *       lid and other mods read); and two methods are added, {@code getUpdatePacket} and {@code onDataPacket}, which send
 *       the pairing to clients when it changes.</li>
 *   <li>Forge's {@code VanillaDoubleChestItemHandler.get} (the chest's item handler capability).</li>
 * </ul>
 * Each head adds one branch, with its frame (the method's starting frame).
 */
public final class ChestPairingPatch implements IClassTransformer {

    private static final String HOOKS = "com/exiledradio/updateaquaticbackport/mechanics/ChestPairing";
    private static final String WORLD = "Lnet/minecraft/world/World;";
    private static final String POS = "Lnet/minecraft/util/math/BlockPos;";
    private static final String STATE = "Lnet/minecraft/block/state/IBlockState;";
    private static final String CHEST = "Lnet/minecraft/block/BlockChest;";
    private static final String TILE = "Lnet/minecraft/tileentity/TileEntityChest;";
    private static final String FACING = "Lnet/minecraft/util/EnumFacing;";
    private static final String LIVING = "Lnet/minecraft/entity/EntityLivingBase;";
    private static final String PACKET = "Lnet/minecraft/network/play/server/SPacketUpdateTileEntity;";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        if ("net.minecraft.block.BlockChest".equals(transformedName)) {
            return block(bytes);
        }
        if ("net.minecraft.tileentity.TileEntityChest".equals(transformedName)) {
            return tile(bytes);
        }
        if ("net.minecraftforge.items.VanillaDoubleChestItemHandler".equals(transformedName)) {
            return items(bytes);
        }
        return bytes;
    }

    /** {@code if (ChestPairing.handles(arg0)) return hook(args);} at the head of a method. */
    private static void head(MethodNode m, String gateDesc, int[] loads, int[] loadOps, String hook, String hookDesc, int ret) {
        LabelNode vanilla = new LabelNode();
        InsnList list = new InsnList();
        list.add(new VarInsnNode(Opcodes.ALOAD, 0));
        list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "handles", gateDesc, false));
        list.add(new JumpInsnNode(Opcodes.IFEQ, vanilla));
        for (int i = 0; i < loads.length; i++) {
            list.add(new VarInsnNode(loadOps[i], loads[i]));
        }
        if (hook != null) {
            list.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, hook, hookDesc, false));
        }
        list.add(new InsnNode(ret));
        list.add(vanilla);
        list.add(new FrameNode(Opcodes.F_SAME, 0, null, 0, null));
        m.instructions.insert(list);
    }

    private static int[] a(int... v) {
        return v;
    }

    private static int[] objects(int n) {
        int[] ops = new int[n];
        java.util.Arrays.fill(ops, Opcodes.ALOAD);
        return ops;
    }

    private static boolean patched(ClassNode node) {
        for (MethodNode m : node.methods) {
            if (containsOwner(m)) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsOwner(MethodNode m) {
        for (org.objectweb.asm.tree.AbstractInsnNode insn : m.instructions.toArray()) {
            if (insn instanceof MethodInsnNode && HOOKS.equals(((MethodInsnNode) insn).owner)) {
                return true;
            }
        }
        return false;
    }

    private static byte[] block(byte[] bytes) {
        ClassNode node = Patches.read(bytes);
        String gate = "(Lnet/minecraft/block/Block;)Z";
        MethodNode box = Patches.method(node, "getBoundingBox", "func_185496_a", "(" + STATE + "Lnet/minecraft/world/IBlockAccess;" + POS + ")Lnet/minecraft/util/math/AxisAlignedBB;");
        MethodNode added = Patches.method(node, "onBlockAdded", "func_176213_c", "(" + WORLD + POS + STATE + ")V");
        MethodNode placement = Patches.method(node, "getStateForPlacement", "func_180642_a", "(" + WORLD + POS + FACING + "FFFI" + LIVING + ")" + STATE);
        MethodNode placed = Patches.method(node, "onBlockPlacedBy", "func_180633_a", "(" + WORLD + POS + STATE + LIVING + "Lnet/minecraft/item/ItemStack;)V");
        MethodNode can = Patches.method(node, "canPlaceBlockAt", "func_176196_c", "(" + WORLD + POS + ")Z");
        MethodNode container = Patches.method(node, "getContainer", "func_189418_a", "(" + WORLD + POS + "Z)Lnet/minecraft/world/ILockableContainer;");
        MethodNode broken = Patches.method(node, "breakBlock", "func_180663_b", "(" + WORLD + POS + STATE + ")V");
        if (box == null || added == null || placement == null || placed == null || can == null || container == null || broken == null || patched(node)) {
            Patches.LOGGER.warn("Patch ChestPairing skipped: BlockChest is not as expected; chests pair as in 1.12.2");
            return bytes;
        }
        head(box, gate, a(2, 3), objects(2), "box", "(Lnet/minecraft/world/IBlockAccess;" + POS + ")Lnet/minecraft/util/math/AxisAlignedBB;", Opcodes.ARETURN);
        head(added, gate, a(), a(), null, null, Opcodes.RETURN);
        head(placement, gate, a(0, 1, 2, 3, 8), objects(5), "placement", "(" + CHEST + WORLD + POS + FACING + LIVING + ")" + STATE, Opcodes.ARETURN);
        head(placed, gate, a(0, 1, 2, 3, 4, 5), objects(6), "placed", "(" + CHEST + WORLD + POS + STATE + LIVING + "Lnet/minecraft/item/ItemStack;)V", Opcodes.RETURN);
        head(can, gate, a(1, 2), objects(2), "canPlace", "(" + WORLD + POS + ")Z", Opcodes.IRETURN);
        head(container, gate, a(0, 1, 2, 3), new int[] {Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ILOAD}, "container",
                "(" + CHEST + WORLD + POS + "Z)Lnet/minecraft/world/ILockableContainer;", Opcodes.ARETURN);
        InsnList first = new InsnList();
        first.add(new VarInsnNode(Opcodes.ALOAD, 0));
        first.add(new VarInsnNode(Opcodes.ALOAD, 1));
        first.add(new VarInsnNode(Opcodes.ALOAD, 2));
        first.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "broken", "(" + CHEST + WORLD + POS + ")V", false));
        broken.instructions.insert(first);
        Patches.LOGGER.info("Patch ChestPairing applied to BlockChest");
        return Patches.write(node);
    }

    private static byte[] tile(byte[] bytes) {
        ClassNode node = Patches.read(bytes);
        MethodNode adjacent = Patches.method(node, "getAdjacentChest", "func_174911_a", "(" + FACING + ")" + TILE);
        MethodNode check = Patches.method(node, "checkForAdjacentChests", "func_145979_i", "()V");
        if (adjacent == null || check == null || patched(node)) {
            Patches.LOGGER.warn("Patch ChestPairing skipped: TileEntityChest is not as expected; chests pair as in 1.12.2");
            return bytes;
        }
        head(adjacent, "(" + TILE + ")Z", a(0, 1), objects(2), "adjacent", "(" + TILE + FACING + ")" + TILE, Opcodes.ARETURN);
        boolean srg = check.name.startsWith("func_");
        String updatePacket = srg ? "func_189518_D_" : "getUpdatePacket";
        if (Patches.method(node, updatePacket, updatePacket, "()" + PACKET) == null) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, updatePacket, "()" + PACKET, null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "updatePacket", "(" + TILE + ")" + PACKET, false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            node.methods.add(m);
        }
        String dataDesc = "(Lnet/minecraft/network/NetworkManager;" + PACKET + ")V";
        if (Patches.method(node, "onDataPacket", "onDataPacket", dataDesc) == null) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "onDataPacket", dataDesc, null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS, "dataPacket", "(" + TILE + PACKET + ")V", false));
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            node.methods.add(m);
        }
        Patches.LOGGER.info("Patch ChestPairing applied to TileEntityChest");
        return Patches.write(node);
    }

    private static byte[] items(byte[] bytes) {
        ClassNode node = Patches.read(bytes);
        MethodNode get = Patches.method(node, "get", "get", "(" + TILE + ")Lnet/minecraftforge/items/VanillaDoubleChestItemHandler;");
        if (get == null || patched(node)) {
            Patches.LOGGER.warn("Patch ChestPairing skipped: Forge's VanillaDoubleChestItemHandler.get is not as expected");
            return bytes;
        }
        head(get, "(" + TILE + ")Z", a(0), objects(1), "itemHandler", "(" + TILE + ")Lnet/minecraftforge/items/VanillaDoubleChestItemHandler;", Opcodes.ARETURN);
        Patches.LOGGER.info("Patch ChestPairing applied to VanillaDoubleChestItemHandler.get");
        return Patches.write(node);
    }
}
