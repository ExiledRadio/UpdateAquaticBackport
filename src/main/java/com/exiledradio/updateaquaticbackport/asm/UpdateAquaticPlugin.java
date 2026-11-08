package com.exiledradio.updateaquaticbackport.asm;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * This mod's own small bytecode patches, each one class here and one section of {@code docs/PATCHES.md}.
 * Used only where vanilla parity needs a change no event or override can make.
 *
 * <ul>
 *   <li>{@link PumpkinHeadPatch}: the plain pumpkin is no longer worn on the head.</li>
 *   <li>{@link PumpkinGolemPatch}: the plain pumpkin no longer builds golems, however it is placed.</li>
 *   <li>{@link WoodPickBlockPatch}: middle-clicking 1.13's wood (a bark log) picks the wood item.</li>
 * The village biome point and the strongholds' and spawn point's biome search (1.0.0's P4 and P5) are
 * Backport Core's patches P9 and P8 since Core 1.2.0, shared by every backport of 1.13 and later.
 * </ul>
 * Sorted after FML's deobfuscation (index 1001), so a release sees SRG member names and the development
 * environment MCP names; every patch matches both. Each patch changes only the instructions it names, finds
 * them by owner, name and descriptor (not by position), does nothing if they are not there (so running
 * twice changes nothing more), adds no branch, and is written with {@code COMPUTE_MAXS} only, so it loads
 * no class while transforming one. That keeps it out of the way of other transformers (Fluidlogged API's
 * patch none of these methods).
 */
@IFMLLoadingPlugin.Name("UpdateAquaticBackportPatches")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.SortingIndex(1001)
@IFMLLoadingPlugin.TransformerExclusions("com.exiledradio.updateaquaticbackport.asm.")
public final class UpdateAquaticPlugin implements IFMLLoadingPlugin {

    @Override
    public String[] getASMTransformerClass() {
        return new String[] {
                PumpkinHeadPatch.class.getName(),
                PumpkinGolemPatch.class.getName(),
                WoodPickBlockPatch.class.getName(),
        };
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Nullable
    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
