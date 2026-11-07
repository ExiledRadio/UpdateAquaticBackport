package com.exiledradio.updateaquaticbackport.bubble;

import com.exiledradio.updateaquaticbackport.ModConfig;
import com.exiledradio.updateaquaticbackport.UpdateAquaticBackport;
import com.exiledradio.updateaquaticbackport.common.Content;
import com.exiledradio.updateaquaticbackport.common.LegacyWater;

import net.minecraft.block.Block;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import javax.annotation.Nullable;

/**
 * Bubble columns: the block, its four sounds and the generator that starts columns in new chunks.
 *
 * <p>The column has no item, as in 1.13. Off ({@code bubbles.enabled}), nothing is registered and every
 * handler in this package does nothing, so soul sand and magma under water behave as in 1.12.2.
 */
@Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
public final class BubbleContent {

    /** Generators run lightest first; this one looks for bases after every other has placed its blocks. */
    static final int GENERATOR_WEIGHT = 1000;

    // Made eagerly so the block and client classes can name them; registered only when the stream is on.
    public static final SoundEvent UPWARDS_AMBIENT = sound("block.bubble_column.upwards_ambient");
    public static final SoundEvent UPWARDS_INSIDE = sound("block.bubble_column.upwards_inside");
    public static final SoundEvent WHIRLPOOL_AMBIENT = sound("block.bubble_column.whirlpool_ambient");
    public static final SoundEvent WHIRLPOOL_INSIDE = sound("block.bubble_column.whirlpool_inside");

    private static BlockBubbleColumn column;

    private BubbleContent() {
    }

    public static boolean enabled() {
        return ModConfig.bubbles.enabled;
    }

    /** @return the bubble column block, or null when the stream is off */
    @Nullable
    public static BlockBubbleColumn column() {
        return column;
    }

    private static SoundEvent sound(String name) {
        SoundEvent event = new SoundEvent(Content.id(name));
        event.setRegistryName(Content.id(name));
        return event;
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        if (!enabled()) {
            return;
        }
        column = Content.block(event.getRegistry(), "bubble_column", new BlockBubbleColumn(), null);
        column.setCreativeTab(null);
        Content.multipart(column);
        LegacyWater.always(column);
        GameRegistry.registerWorldGenerator(new BubbleGenerator(), GENERATOR_WEIGHT);
    }

    @SubscribeEvent
    public static void registerSounds(RegistryEvent.Register<SoundEvent> event) {
        if (!enabled()) {
            return;
        }
        event.getRegistry().registerAll(UPWARDS_AMBIENT, UPWARDS_INSIDE, WHIRLPOOL_AMBIENT, WHIRLPOOL_INSIDE);
    }
}
