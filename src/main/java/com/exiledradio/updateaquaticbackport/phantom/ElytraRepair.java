package com.exiledradio.updateaquaticbackport.phantom;

import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.StringUtils;

/**
 * 1.13: phantom membrane repairs the elytra in an anvil, and leather no longer does.
 *
 * <p>1.12.2's elytra names leather in its own {@code getIsRepairable}; without patching it, the anvil's Forge
 * event answers first. Membrane gets vanilla's repair exactly ({@code ContainerRepair.updateRepairOutput}): each
 * membrane mends a quarter of the durability and costs a level, renaming costs one more, the prior-work penalty
 * of both items is added, and the result's penalty doubles plus one. Leather on an elytra gives nothing.
 *
 * <p>One difference: the event does not say who is at the anvil, so a repair costing 40 levels or more is refused
 * for everyone, where vanilla still lets a creative player take it.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class ElytraRepair {

    private ElytraRepair() {
    }

    @SubscribeEvent
    public static void onAnvil(AnvilUpdateEvent event) {
        Item membrane = PhantomContent.membrane();
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if (membrane == null || left.isEmpty() || right.isEmpty() || left.getItem() != Items.ELYTRA) {
            return;
        }
        if (right.getItem() == Items.LEATHER) {
            refuse(event);
            return;
        }
        if (right.getItem() != membrane) {
            return;
        }
        Repair repair = repair(left, right, event.getName());
        if (repair == null) {
            refuse(event);
            return;
        }
        event.setOutput(repair.output);
        event.setCost(repair.levels);
        event.setMaterialCost(repair.materials);
    }

    /**
     * Cancels the event, and clears the anvil's output too: a cancelled event leaves the last result in place,
     * so leather swapped straight in for membrane would otherwise take the membrane repair.
     */
    private static void refuse(AnvilUpdateEvent event) {
        event.setCanceled(true);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.CLIENT) {
            com.exiledradio.updateaquaticbackport.phantom.client.PhantomClient.clearAnvilOutput(event.getLeft(), event.getRight());
            return;
        }
        MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server != null) {
            for (EntityPlayerMP player : server.getPlayerList().getPlayers()) {
                clearOutput(player.openContainer, event.getLeft(), event.getRight());
            }
        }
    }

    /** Clears an anvil's output if it is the anvil holding exactly these two stacks. */
    public static void clearOutput(Container container, ItemStack left, ItemStack right) {
        if (container instanceof ContainerRepair && container.getSlot(0).getStack() == left && container.getSlot(1).getStack() == right) {
            ContainerRepair anvil = (ContainerRepair) container;
            anvil.getSlot(2).putStack(ItemStack.EMPTY);
            anvil.maximumCost = 0;
            anvil.materialCost = 0;
        }
    }

    /** An anvil result: the item, its cost in levels and how many material items it uses. */
    static final class Repair {
        final ItemStack output;
        final int levels;
        final int materials;

        Repair(ItemStack output, int levels, int materials) {
            this.output = output;
            this.levels = levels;
            this.materials = materials;
        }
    }

    /**
     * Vanilla's anvil repair of {@code left} with {@code right} as its material, renamed to {@code name}.
     *
     * @return the result, or null when nothing would change or it is too expensive
     */
    static Repair repair(ItemStack left, ItemStack right, String name) {
        ItemStack out = left.copy();
        int step = Math.min(out.getItemDamage(), out.getMaxDamage() / 4);
        if (step <= 0) {
            return null;
        }
        int used;
        int cost = 0;
        for (used = 0; step > 0 && used < right.getCount(); ++used) {
            out.setItemDamage(out.getItemDamage() - step);
            ++cost;
            step = Math.min(out.getItemDamage(), out.getMaxDamage() / 4);
        }
        int rename = 0;
        if (StringUtils.isBlank(name)) {
            if (left.hasDisplayName()) {
                rename = 1;
                out.clearCustomName();
            }
        } else if (!name.equals(left.getDisplayName())) {
            rename = 1;
            out.setStackDisplayName(name);
        }
        cost += rename;
        int total = left.getRepairCost() + right.getRepairCost() + cost;
        if (total >= 40) {
            return null;
        }
        int penalty = Math.max(left.getRepairCost(), right.getRepairCost());
        // A repair is never a rename alone here, so the penalty always grows.
        penalty = penalty * 2 + 1;
        out.setRepairCost(penalty);
        return new Repair(out, total, used);
    }
}
