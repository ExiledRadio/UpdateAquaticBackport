package com.exiledradio.updateaquaticbackport.asm;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.annotation.Nullable;

/** What the patches share: reading and writing a class, and finding a method by either of its names. */
final class Patches {

    static final Logger LOGGER = LogManager.getLogger("Update Aquatic Backport");

    /** Where the patched code calls back into the mod. Only named here, never loaded. */
    static final String HOOKS = "com/exiledradio/updateaquaticbackport/blocks/BlocksHooks";

    private Patches() {
    }

    static ClassNode read(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        return node;
    }

    /** Max stack and locals recomputed; frames kept as they are (no patch adds a branch). */
    static byte[] write(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        return writer.toByteArray();
    }

    /** @return true if a name is the member's development (MCP) or release (SRG) name */
    static boolean named(String name, String mcp, String srg) {
        return name.equals(mcp) || name.equals(srg);
    }

    @Nullable
    static MethodNode method(ClassNode node, String mcp, String srg, String desc) {
        for (MethodNode method : node.methods) {
            if (named(method.name, mcp, srg) && method.desc.equals(desc)) {
                return method;
            }
        }
        return null;
    }
}
