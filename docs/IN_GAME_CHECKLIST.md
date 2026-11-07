# Update Aquatic Backport 1.0.0: what needs a look in game

Everything server-side is covered by the headless self-test (about 16,100 checks alone, and passing next
to Village & Pillage 2.4.0 and Buzzy Bees 1.0.0). These need a real client, which only the user can look at:

## Swimming and crawling (patched: Backport Core P1-P5)
*   Sprint with your head under water: you lie flat, the body turns along the swim, with 1.13's stroke,
    armor following the body. Third person and first person.
*   The camera easing down to the swim height and back up, without a jump.
*   Swimming up, down and level by looking; it keeps going while you hold sprint, even in water one block
    deep, and stops when you stop sprinting or leave the water.
*   Swimming into a one-block gap, coming out crawling, and standing up when there's room. Crawling under a
    closed trapdoor.
*   Another player seeing you swim and crawl (a second client, or a LAN friend).
*   A trident raised over the head to throw; the Riptide spin pose.
*   With Fluidlogged API installed, sprinting in water works at all (it disables it on its own; P2 fixes it).

## Water
*   1.13's gray water texture tinted per biome, and the new ocean colors (warm turquoise to frozen purple).
    Other mods' biomes with a white tint get the default blue.
*   Underwater fog: the biome's fog color, blending over 5 seconds, darker deeper down, clearing the longer
    the head stays under.
*   Underwater ambience: the enter and exit sounds, the loop fading in and out, the random additions.
*   The air bar staying up above water until it's full again (air comes back gradually).
*   Items and experience orbs floating up in water.
*   Light dimming by one per block of water (look at a lit area under deep water).

## Waterlogging (Fluidlogged API through Backport Core)
*   Water drawn inside kelp, seagrass, coral, coral fans, conduits, bubble columns, sea pickles and
    prismarine stairs and slabs; no water faces drawn between them and the water around them.
*   The plants' cut-out layers, the empty-model bubble column and conduit with water inside.

## Mobs
*   Models, textures and animations: cod, salmon, pufferfish (three puff stages), tropical fish (2 shapes,
    6 patterns, 15 x 15 colors, the 22 named ones), turtles and babies, dolphins (with a carried item), the
    phantom (size scaling, glowing eyes, wing beat), the drowned (with its trident and the outer layer).
*   Fish buckets' item models and the tropical fish bucket's tooltip.
*   Dolphins leaping, playing with items and racing a boat; Dolphin's Grace on yourself.
*   Squid ink clouds; skeleton horses' underwater sounds.
*   Every new mob sound and its subtitle.

## Blocks and items
*   Kelp, seagrass, coral and fans (living and dead), sea pickles glowing only in water, blue ice, the
    conduit (shell, cage, wind, the eye opening at a full frame, the nautilus particles) and its item model.
*   Bubble columns: the bubbles, the whirl and rise sounds, a boat rocking and sinking or popping up.
*   The trident's flat and 3D item models, its glint, the Loyalty return line, the Channeling lightning.
*   Carved pumpkins (the pumpkin blur when worn), the faceless plain pumpkin, the undyed shulker box texture,
    the debug stick's messages, wood and stripped logs, prismarine stairs and slabs.
*   Middle-clicking bark wood picks the wood item.

## Structures and maps
*   Shipwrecks, ocean ruins and buried treasure as they look in a new world.
*   A buried treasure map: the red X and the map's preview.

## Effects
*   The Conduit Power, Dolphin's Grace and Slow Falling icons; Conduit Power's underwater brightness.

## Languages
*   A look at a few languages in game. The Spanish text read by a native speaker; the review points are in
    the languages hand-over note (spawn wording, "Insomnio", structure names, es_mx neutrality).

## Not tested anywhere yet
*   Terrain mods (Open Terrain Generator, Realistic Terrain Generation, Biomes O' Plenty) with the new
    oceans and structures.
*   A world saved by an earlier dev build loading with this release (only synthetic old chunks were tested).
*   Sponges next to kelp (in 1.14.4 they drop the kelp; here it is removed without a drop).
