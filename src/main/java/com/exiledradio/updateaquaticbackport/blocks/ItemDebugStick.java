package com.exiledradio.updateaquaticbackport.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

/**
 * 1.13's debug stick, from 1.14.4's {@code DebugStickItem}.
 *
 * <p>For a creative player allowed to use command blocks: attacking a block selects its next property
 * (sneaking, the previous one), using it on a block sets the selected property to its next value
 * (sneaking, the previous one) without updating neighbors. Each says what it did above the hotbar. The
 * selection is kept per block type in the stick's {@code DebugProperty} tag. Nobody else can do anything
 * with it, and no one breaks blocks while holding it. Only from commands, as in vanilla: it is in no
 * creative tab.
 *
 * <h2>Stored properties only</h2>
 * 1.12.2 blocks keep their state in 4 bits of metadata, and many of their properties (fence connections,
 * a door's hinge in its lower half, ...) are worked out from the neighbors when drawn and never stored. Such
 * a property would snap back the moment it was set, so the stick offers only the properties of the clicked
 * state that survive the metadata round trip.
 */
public final class ItemDebugStick extends Item {

    ItemDebugStick() {
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return true;
    }

    @Override
    public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing,
                                      float hitX, float hitY, float hitZ) {
        if (!world.isRemote) {
            handleClick(player, world.getBlockState(pos), world, pos, true, player.getHeldItem(hand));
        }
        return EnumActionResult.SUCCESS;
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, EntityPlayer player) {
        return true;
    }

    @Override
    public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
        return false;
    }

    /** Attacking ({@code use} false, from {@link BlocksEvents}) or using the stick on a block, server side. */
    void handleClick(EntityPlayer player, IBlockState state, World world, BlockPos pos, boolean use, ItemStack stack) {
        if (!player.canUseCommandBlock()) {
            return;
        }
        Block block = state.getBlock();
        String id = String.valueOf(block.getRegistryName());
        List<IProperty<?>> properties = storedProperties(state);
        if (properties.isEmpty()) {
            message(player, ".empty", id);
            return;
        }
        NBTTagCompound tag = stack.getOrCreateSubCompound("DebugProperty");
        IProperty<?> selected = byName(properties, tag.getString(id));
        if (use) {
            if (selected == null) {
                selected = properties.get(0);
            }
            IBlockState next = cycle(state, selected, player.isSneaking());
            world.setBlockState(pos, next, 2 | 16);
            message(player, ".update", selected.getName(), valueName(next, selected));
        } else {
            selected = adjacent(properties, selected, player.isSneaking());
            tag.setString(id, selected.getName());
            message(player, ".select", selected.getName(), valueName(state, selected));
        }
    }

    private void message(EntityPlayer player, String suffix, Object... args) {
        player.sendStatusMessage(new TextComponentTranslation(getTranslationKey() + suffix, args), true);
    }

    /** @return the state's properties, in vanilla's order (by name), that its metadata can hold every step of */
    public static List<IProperty<?>> storedProperties(IBlockState state) {
        List<IProperty<?>> out = new ArrayList<IProperty<?>>();
        Block block = state.getBlock();
        for (IProperty<?> property : state.getPropertyKeys()) {
            try {
                IBlockState next = cycle(state, property, false);
                @SuppressWarnings("deprecation")
                IBlockState stored = block.getStateFromMeta(block.getMetaFromState(next));
                if (stored.getValue(property).equals(next.getValue(property))) {
                    out.add(property);
                }
            } catch (RuntimeException ignored) {
                // A block whose metadata methods cannot take a state it lists: not storable, then.
            }
        }
        return out;
    }

    private static IProperty<?> byName(List<IProperty<?>> properties, String name) {
        for (IProperty<?> property : properties) {
            if (property.getName().equals(name)) {
                return property;
            }
        }
        return null;
    }

    static <T extends Comparable<T>> IBlockState cycle(IBlockState state, IProperty<T> property, boolean backwards) {
        return state.withProperty(property, adjacent(new ArrayList<T>(property.getAllowedValues()), state.getValue(property), backwards));
    }

    /** 1.14's {@code Util.getElementAfter} / {@code getElementBefore}: wraps around; null starts at an end. */
    static <T> T adjacent(List<T> values, T current, boolean backwards) {
        int i = current == null ? -1 : values.indexOf(current);
        if (backwards) {
            return values.get(i <= 0 ? values.size() - 1 : i - 1);
        }
        return values.get((i + 1) % values.size());
    }

    private static <T extends Comparable<T>> String valueName(IBlockState state, IProperty<T> property) {
        return property.getName(state.getValue(property));
    }
}
