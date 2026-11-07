package com.exiledradio.updateaquaticbackport;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Configuration: one category per stream, each with an {@code enabled} switch.
 *
 * <h2>Rules for streams</h2>
 * <ul>
 *   <li>A stream edits only inside its own category class.</li>
 *   <li>Category classes extend {@code Object} directly. Forge 1.12 crashes at startup ("Unknown type")
 *       on a category class that extends another one.</li>
 *   <li>{@code enabled} is read once, during registration, and needs a restart: switching a stream off
 *       means its blocks and items are never registered, so a world that has them loses them.</li>
 *   <li>A changed default never reaches an installed game (Forge keeps the value in the file), so a
 *       default that must change gets a new key.</li>
 * </ul>
 */
@Config(modid = UpdateAquaticBackport.MOD_ID, name = UpdateAquaticBackport.MOD_ID)
public final class ModConfig {

    @Config.LangKey("config.updateaquaticbackport.woodset")
    @Config.Comment("Stripped logs and wood, the five wood trapdoors, buttons and pressure plates, blue ice and sea pickles.")
    public static final Woodset woodset = new Woodset();

    @Config.LangKey("config.updateaquaticbackport.kelp")
    @Config.Comment("Kelp, dried kelp and seagrass.")
    public static final Kelp kelp = new Kelp();

    @Config.LangKey("config.updateaquaticbackport.coral")
    @Config.Comment("Coral, coral blocks, coral fans and coral reefs.")
    public static final Coral coral = new Coral();

    @Config.LangKey("config.updateaquaticbackport.fish")
    @Config.Comment("Cod, salmon, pufferfish and tropical fish, and their buckets.")
    public static final Fish fish = new Fish();

    @Config.LangKey("config.updateaquaticbackport.turtles")
    @Config.Comment("Turtles, turtle eggs, scutes, the turtle shell and the Turtle Master potion.")
    public static final Turtles turtles = new Turtles();

    @Config.LangKey("config.updateaquaticbackport.dolphins")
    @Config.Comment("Dolphins and Dolphin's Grace.")
    public static final Dolphins dolphins = new Dolphins();

    @Config.LangKey("config.updateaquaticbackport.phantoms")
    @Config.Comment("Phantoms, phantom membrane and Slow Falling.")
    public static final Phantoms phantoms = new Phantoms();

    @Config.LangKey("config.updateaquaticbackport.trident")
    @Config.Comment("The trident, its four enchantments, and the drowned.")
    public static final Trident trident = new Trident();

    @Config.LangKey("config.updateaquaticbackport.conduit")
    @Config.Comment("The conduit, Conduit Power, the nautilus shell and the heart of the sea.")
    public static final Conduit conduit = new Conduit();

    @Config.LangKey("config.updateaquaticbackport.structures")
    @Config.Comment("Shipwrecks, ocean ruins, buried treasure and treasure maps.")
    public static final Structures structures = new Structures();

    @Config.LangKey("config.updateaquaticbackport.oceans")
    @Config.Comment("1.13's ocean biomes, icebergs, and underwater caves and ravines.")
    public static final Oceans oceans = new Oceans();

    @Config.LangKey("config.updateaquaticbackport.bubbles")
    @Config.Comment("Bubble columns over soul sand and magma blocks.")
    public static final Bubbles bubbles = new Bubbles();

    @Config.LangKey("config.updateaquaticbackport.blocks")
    @Config.Comment("Smaller 1.13 block and item changes: prismarine stairs and slabs, carved pumpkins, wood blocks and more.")
    public static final Blocks blocks = new Blocks();

    @Config.LangKey("config.updateaquaticbackport.mobs")
    @Config.Comment("1.13's changes to existing mobs.")
    public static final Mobs mobs = new Mobs();

    @Config.LangKey("config.updateaquaticbackport.water")
    @Config.Comment("How water looks and feels: biome water colors, underwater fog, air refilling, floating items.")
    public static final Water water = new Water();

    // ------------------------------------------------------------------------------------------
    // Categories. Each stream owns exactly one.
    // ------------------------------------------------------------------------------------------

    public static final class Woodset {
        @Config.RequiresMcRestart
        @Config.Comment({"Add these blocks when no other installed mod provides them. Off, nothing is registered,",
                "and a world that has them loses them."})
        @Config.LangKey("config.updateaquaticbackport.woodset.enabled")
        public boolean enabled = true;

        @Config.Comment("Right-clicking a log with any axe strips it.")
        @Config.LangKey("config.updateaquaticbackport.woodset.axeStripping")
        public boolean axeStripping = true;
    }

    public static final class Kelp {
        @Config.RequiresMcRestart
        @Config.Comment("Kelp, dried kelp, the dried kelp block and seagrass. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.kelp.enabled")
        public boolean enabled = true;

        @Config.Comment("Kelp and seagrass generate in oceans and rivers in newly generated chunks.")
        @Config.LangKey("config.updateaquaticbackport.kelp.generate")
        public boolean generate = true;
    }

    public static final class Coral {
        @Config.RequiresMcRestart
        @Config.Comment("Coral blocks, coral, coral fans and their dead forms. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.coral.enabled")
        public boolean enabled = true;

        @Config.Comment("Coral reefs generate in warm oceans in newly generated chunks.")
        @Config.LangKey("config.updateaquaticbackport.coral.generate")
        public boolean generate = true;
    }

    public static final class Fish {
        @Config.RequiresMcRestart
        @Config.Comment("Cod, salmon, pufferfish and tropical fish as mobs, and their buckets. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.fish.enabled")
        public boolean enabled = true;
    }

    public static final class Turtles {
        @Config.RequiresMcRestart
        @Config.Comment("Turtles, turtle eggs, scutes, the turtle shell and Turtle Master. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.turtles.enabled")
        public boolean enabled = true;

        @Config.RequiresMcRestart
        @Config.RangeInt(min = 0, max = 100)
        @Config.Comment({"How often turtles spawn on sandy beaches, against the other animals there (1.13: 5). 0 stops them",
                "spawning naturally."})
        @Config.LangKey("config.updateaquaticbackport.turtles.spawnWeight")
        public int spawnWeight = 5;
    }

    public static final class Dolphins {
        @Config.RequiresMcRestart
        @Config.Comment("Dolphins and Dolphin's Grace. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.dolphins.enabled")
        public boolean enabled = true;
    }

    public static final class Phantoms {
        @Config.RequiresMcRestart
        @Config.Comment("Phantoms, phantom membrane and Slow Falling. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.phantoms.enabled")
        public boolean enabled = true;
    }

    public static final class Trident {
        @Config.RequiresMcRestart
        @Config.Comment("The trident, Loyalty, Riptide, Impaling, Channeling and the drowned. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.trident.enabled")
        public boolean enabled = true;
    }

    public static final class Conduit {
        @Config.RequiresMcRestart
        @Config.Comment("The conduit, Conduit Power, the nautilus shell and the heart of the sea. Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.conduit.enabled")
        public boolean enabled = true;
    }

    public static final class Structures {
        @Config.RequiresMcRestart
        @Config.Comment("Shipwrecks, ocean ruins, buried treasure and buried treasure maps. Off, none generate.")
        @Config.LangKey("config.updateaquaticbackport.structures.enabled")
        public boolean enabled = true;
    }

    public static final class Oceans {
        @Config.RequiresMcRestart
        @Config.Comment("1.13's warm, lukewarm, cold and frozen oceans, icebergs, and underwater caves and ravines.")
        @Config.LangKey("config.updateaquaticbackport.oceans.enabled")
        public boolean enabled = true;
    }

    public static final class Bubbles {
        @Config.RequiresMcRestart
        @Config.Comment("Bubble columns over soul sand (upward) and magma blocks (downward). Off, nothing is registered.")
        @Config.LangKey("config.updateaquaticbackport.bubbles.enabled")
        public boolean enabled = true;
    }

    public static final class Blocks {
        @Config.RequiresMcRestart
        @Config.Comment("Prismarine stairs and slabs, carved pumpkins, wood blocks from logs and the other small changes.")
        @Config.LangKey("config.updateaquaticbackport.blocks.enabled")
        public boolean enabled = true;
    }

    public static final class Mobs {
        @Config.RequiresMcRestart
        @Config.Comment("1.13's changes to existing mobs: undead sinking, squid ink, polar bears on ice and the rest.")
        @Config.LangKey("config.updateaquaticbackport.mobs.enabled")
        public boolean enabled = true;
    }

    public static final class Water {
        @Config.RequiresMcRestart
        @Config.Comment("Biome water colors, underwater fog, gradual air refill and the rest of 1.13's water changes.")
        @Config.LangKey("config.updateaquaticbackport.water.enabled")
        public boolean enabled = true;
    }

    // ------------------------------------------------------------------------------------------

    /** Saves edits made in the in-game config screen. */
    @Mod.EventBusSubscriber(modid = UpdateAquaticBackport.MOD_ID)
    public static final class EventHandler {
        private EventHandler() {
        }

        @SubscribeEvent
        public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
            if (UpdateAquaticBackport.MOD_ID.equals(event.getModID())) {
                ConfigManager.sync(UpdateAquaticBackport.MOD_ID, Config.Type.INSTANCE);
            }
        }
    }

    private ModConfig() {
    }
}
