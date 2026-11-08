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

## Look and feel (stream Lf, with Backport Core 1.2.0)

Self-tests: `gradlew runServer -Pselftest=lookfeel` (registration, assets, splash pool, the Allow Snooper
default, lang) and `gradlew runClient -Pselftest=client` (title screen, F3 key map, fog step, the copied commands;
log only).

- [ ] **Title screen**: 1.13's ocean panorama (coral, kelp, a shipwreck) turns all the way round, sharp, with the
      gradient over it. With a newer update backport installed, the newer panorama shows instead.
- [ ] **Splashes**: over several launches "Thank you for the fish!", "All rumors are true!", "Truly gone fishing!".
- [ ] **World loading**: "Preparing spawn area: N%".
- [ ] **F3 + C**: "[Debug]: Copied location to clipboard"; pasting gives `/tp @s x y z yaw pitch`, which works as a
      command. Holding F3 + C still crashes the game after 6 seconds, as before.
- [ ] **F3 + I** on a chest with cheats on: "Copied server-side block data" in singleplayer, and the pasted
      `/setblock ... replace {Items:[...]}` recreates it; Shift + F3 + I says client-side. On a mob: `/summon ...`.
      On a server (no server query in 1.12.2): client-side data.
- [ ] **F3 + Q** lists the F3 + C and F3 + I lines.
- [ ] **Underwater fog** looks as before this change (it now runs as a step of Backport Core's fog pipeline).
- [ ] **Options**: a fresh instance has Allow Snooper off (Options > Snooper Settings).
- [ ] **Controls**: mouse buttons read "Left Button", "Right Button", "Middle Button".

## 1.13's pieces in Backport Core's finished presentation module (Core 1.2.0)

- [ ] **Title screen**: on the first start the panorama shows at once, then the logo, splash, buttons and text fade
      in over a second (1.13). No 3D Anaglyph on Video Settings; GUI Scale reads Auto, 1, 2, 3 (4 on a 1080p window).
- [ ] **Music**: under water in an ocean or river, after a while, Axolotl, Dragon Fish or Shuniji (1.13); it keeps
      playing while you stay, and stops for the game music when another track is asked for.
- [ ] **F3**: "Integrated server @ N ms ticks, N tx, N rx" on the third line; "minecraft:overworld FC: 0" above
      XYZ; "Looking at block" and, over water, "Looking at liquid"; on the right "Targeted Block", "Targeted Fluid"
      (minecraft:water, level) and "Targeted Entity".
- [ ] **Edit World**: Make Backup (a toast "Backed up: <world>", a zip in .minecraft/backups) and Open Backups Folder.
- [ ] **Prismarine stairs and slabs**: a bucket of water waterlogs them; lava does not; flowing water does not run in;
      a half slab between two water sources fills by itself.
- [ ] **Text**: "Left Button", "Loading terrain...", "Open World Folder", "Raw Cod" (1.13.2's wording through Core).

## Creative tabs (with Backport Core 1.3.0)

The places are in `docs/CREATIVE_TABS.md`; a dev client already checked the lists, the search tab and JEI.

- [ ] There is no tab for this mod; every vanilla tab keeps its icon, name, background and scroll bar.
- [ ] A few places: carved pumpkin after the pumpkin, the undyed shulker box just before the white one, coral blocks after the black concrete powder, the turtle shell first in Combat, the trident after the totem with its four enchanted books, fish buckets after the milk bucket.
- [ ] The search tab finds the mod's items, and JEI lists them.
- [ ] Other mods' items are still in their tabs, in their own order.
- [ ] With another update backport installed too, its items and this mod's sit together by kind (stripped logs, slabs).
