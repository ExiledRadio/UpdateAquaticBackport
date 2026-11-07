package com.exiledradio.updateaquaticbackport.coral;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.SharedSounds;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.item.Item;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Coral: for each of the five kinds, living and dead, a coral block, a coral, a coral fan and a wall fan -
 * 40 blocks, 30 items (wall fans place from the fan item). Reefs and their sea pickles are
 * {@link CoralGenerator}; pickles spreading over coral with bone meal is {@link CoralPickles}.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class CoralContent {

    public static final SoundEvent CORAL_BREAK = event("block.coral_block.break");
    public static final SoundEvent CORAL_STEP = event("block.coral_block.step");
    public static final SoundEvent CORAL_PLACE = event("block.coral_block.place");
    public static final SoundEvent CORAL_HIT = event("block.coral_block.hit");
    public static final SoundEvent CORAL_FALL = event("block.coral_block.fall");

    /** 1.13's {@code SoundType.CORAL}, the living coral blocks'. */
    public static final SoundType CORAL = new SoundType(1.0F, 1.0F, CORAL_BREAK, CORAL_STEP, CORAL_PLACE, CORAL_HIT, CORAL_FALL);

    public static final Map<CoralType, BlockCoralBlock> BLOCKS = new EnumMap<CoralType, BlockCoralBlock>(CoralType.class);
    public static final Map<CoralType, BlockCoralBlock> DEAD_BLOCKS = new EnumMap<CoralType, BlockCoralBlock>(CoralType.class);
    public static final Map<CoralType, BlockCoral> CORALS = new EnumMap<CoralType, BlockCoral>(CoralType.class);
    public static final Map<CoralType, BlockCoral> DEAD_CORALS = new EnumMap<CoralType, BlockCoral>(CoralType.class);
    public static final Map<CoralType, BlockCoral> FANS = new EnumMap<CoralType, BlockCoral>(CoralType.class);
    public static final Map<CoralType, BlockCoral> DEAD_FANS = new EnumMap<CoralType, BlockCoral>(CoralType.class);
    public static final Map<CoralType, BlockCoralWallFan> WALL_FANS = new EnumMap<CoralType, BlockCoralWallFan>(CoralType.class);
    public static final Map<CoralType, BlockCoralWallFan> DEAD_WALL_FANS = new EnumMap<CoralType, BlockCoralWallFan>(CoralType.class);

    private static final List<Block> ALL = new ArrayList<Block>();

    private CoralContent() {
    }

    public static boolean enabled() {
        return ModConfig.coral.enabled;
    }

    /** @return every coral block of this stream, for tests */
    public static List<Block> all() {
        return Collections.unmodifiableList(ALL);
    }

    private static SoundEvent event(String name) {
        SoundEvent e = new SoundEvent(Content.id(name));
        e.setRegistryName(Content.id(name));
        return e;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (enabled()) {
            event.getRegistry().registerAll(CORAL_BREAK, CORAL_STEP, CORAL_PLACE, CORAL_HIT, CORAL_FALL);
        }
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Block> r = event.getRegistry();
        for (CoralType t : CoralType.values()) {
            BlockCoralBlock dead = add(r, "dead_" + t.name + "_coral_block",
                    new BlockCoralBlock(net.minecraft.block.material.MapColor.GRAY, null, SoundType.STONE), "pickaxe");
            DEAD_BLOCKS.put(t, dead);
            BLOCKS.put(t, add(r, t.name + "_coral_block", new BlockCoralBlock(t.color, dead, CORAL), "pickaxe"));
        }
        for (CoralType t : CoralType.values()) {
            BlockCoral dead = add(r, "dead_" + t.name + "_coral", new BlockCoral(t, false, false, SoundType.STONE), null);
            BlockCoral living = add(r, t.name + "_coral", new BlockCoral(t, false, true, SharedSounds.WET_GRASS), null);
            living.dead = dead;
            DEAD_CORALS.put(t, dead);
            CORALS.put(t, living);
        }
        for (CoralType t : CoralType.values()) {
            BlockCoral dead = add(r, "dead_" + t.name + "_coral_fan", new BlockCoral(t, true, false, SoundType.STONE), null);
            BlockCoral living = add(r, t.name + "_coral_fan", new BlockCoral(t, true, true, SharedSounds.WET_GRASS), null);
            living.dead = dead;
            DEAD_FANS.put(t, dead);
            FANS.put(t, living);
        }
        for (CoralType t : CoralType.values()) {
            BlockCoralWallFan dead = add(r, "dead_" + t.name + "_coral_wall_fan", new BlockCoralWallFan(t, false, SoundType.STONE), null);
            BlockCoralWallFan living = add(r, t.name + "_coral_wall_fan", new BlockCoralWallFan(t, true, SharedSounds.WET_GRASS), null);
            living.dead = dead;
            dead.itemBlock = DEAD_FANS.get(t);
            living.itemBlock = FANS.get(t);
            dead.setCreativeTab(null);
            living.setCreativeTab(null);
            DEAD_WALL_FANS.put(t, dead);
            WALL_FANS.put(t, living);
        }
        for (Block b : ALL) {
            if (b instanceof BlockCoral) {
                Content.multipart(b);
            }
        }
        GameRegistry.registerWorldGenerator(new CoralGenerator(), 6);
        UpdateAquaticBackport.LOGGER.info("Registered {} coral blocks.", ALL.size());
    }

    private static <B extends Block> B add(IForgeRegistry<Block> r, String name, B block, String tool) {
        Content.block(r, name, block, tool);
        ALL.add(block);
        return block;
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        if (!enabled()) {
            return;
        }
        IForgeRegistry<Item> r = event.getRegistry();
        for (CoralType t : CoralType.values()) {
            Content.registerBlockItem(r, BLOCKS.get(t));
            Content.registerBlockItem(r, DEAD_BLOCKS.get(t));
            Content.registerBlockItem(r, CORALS.get(t));
            Content.registerBlockItem(r, DEAD_CORALS.get(t));
            Content.blockItem(r, FANS.get(t), new ItemCoralFan(FANS.get(t), WALL_FANS.get(t)));
            Content.blockItem(r, DEAD_FANS.get(t), new ItemCoralFan(DEAD_FANS.get(t), DEAD_WALL_FANS.get(t)));
        }
    }
}
