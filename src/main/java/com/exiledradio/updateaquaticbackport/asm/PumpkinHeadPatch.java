package com.exiledradio.updateaquaticbackport.asm;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The plain pumpkin is no longer worn (1.13 moved wearing to the carved pumpkin).
 *
 * <p>{@code EntityLiving.getSlotForItemStack} sends the pumpkin's item to the head slot by naming
 * {@code Blocks.PUMPKIN}, and everything that puts an item on a head asks it: the inventory's helmet slot,
 * shift-clicking into it, dispensers, armor stands, mobs picking items up. The one read of
 * {@code Blocks.PUMPKIN} there becomes a call to {@code BlocksHooks.wornPumpkin()}, which answers the carved
 * pumpkin while this mod's blocks are on (and {@code Blocks.PUMPKIN} when they are off). The carved
 * pumpkin's own item already names the head slot, so it never gets that far; the plain pumpkin falls
 * through to vanilla's "main hand".
 */
public final class PumpkinHeadPatch implements IClassTransformer {

    private static final String TARGET = "net.minecraft.entity.EntityLiving";

    @Override
    public byte[] transform(String name, String transformedName, byte[] bytes) {
        if (bytes == null || !TARGET.equals(transformedName)) {
            return bytes;
        }
        ClassNode node = Patches.read(bytes);
        MethodNode method = Patches.method(node, "getSlotForItemStack", "func_184640_d",
                "(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/inventory/EntityEquipmentSlot;");
        List<FieldInsnNode> reads = new ArrayList<FieldInsnNode>();
        if (method != null) {
            for (AbstractInsnNode insn : method.instructions.toArray()) {
                if (insn.getOpcode() == Opcodes.GETSTATIC) {
                    FieldInsnNode field = (FieldInsnNode) insn;
                    if (field.owner.equals("net/minecraft/init/Blocks") && Patches.named(field.name, "PUMPKIN", "field_150423_aK")) {
                        reads.add(field);
                    }
                }
            }
        }
        if (reads.size() != 1) {
            Patches.LOGGER.warn("Patch PumpkinHead skipped: expected one Blocks.PUMPKIN in EntityLiving.getSlotForItemStack, found {}; "
                    + "the plain pumpkin can still be worn", reads.size());
            return bytes;
        }
        method.instructions.set(reads.get(0), new MethodInsnNode(Opcodes.INVOKESTATIC, Patches.HOOKS, "wornPumpkin",
                "()Lnet/minecraft/block/Block;", false));
        Patches.LOGGER.info("Patch PumpkinHead applied to EntityLiving.getSlotForItemStack");
        return Patches.write(node);
    }
}
