# Stream S: ocean structures

Package `structure`. Config `structures` (`enabled`; no new fields). No blocks, items, entities or packets
of its own (entity ids 35-39 and packet ids 45-49 stay unused). Depends on Backport Core 1.1.0
(`ModernTemplate`, `TemplatePiece`, `ScatteredStructure`, `ScatteredPlacement`, `Structures`,
`TerrainHeights`, `Waterlogging`).

## Changelog section

*   **Shipwrecks** in every ocean, sunk to the sea floor, and **beached shipwrecks** half buried on beaches:
    today's 20 ship files, upright, sideways or upside down, whole or broken, in any of the six woods.
    Each has supply, map and treasure chests with today's loot.
*   **Ocean ruins**: small and large stone-brick ruins (cracked and mossy) in ordinary, cold and frozen
    oceans, sandstone ruins in warm and lukewarm ones. Large ruins often stand in a cluster of 4 to 8 small
    ones. Their chests hold today's ruin loot, and **drowned** wait in them.
*   **Buried treasure** under beaches: one chest, buried in sand on the stone below, with a **heart of the
    sea**, iron, gold, emeralds, diamonds, prismarine crystals, TNT, cooked fish and potions of Water
    Breathing.
*   **Buried treasure maps** in shipwreck map chests and ocean ruin chests: they show the land around the
    nearest buried treasure, marked with today's **red X**.
*   Dolphins fed raw fish lead players to the nearest shipwreck or ocean ruin.

## How it works

*   **Placement** (`MapGenShipwreck`, `MapGenOceanRuin`, `MapGenBuriedTreasure`): Core's
    `ScatteredStructure`, driven from the populate step, with today's structure sets (from 26.3's data and
    `StructureSets` in the 1.19.2 sources): shipwrecks spacing 24 / separation 4 / salt 165745295, ocean
    ruins 20 / 8 / 14357621, buried treasure every chunk (1 / 0 / 0) kept at 0.01 by today's
    `legacy_type_2` reducer (`setLargeFeatureWithSalt(seed, x, z, 10387320).nextFloat() < 0.01`), its
    chest at block (9, 9) of the chunk. Overworld only, and only with "generate structures" on.
*   **Biomes** (`StructureBiomes`), read at the chunk's middle (block 8, 8, today's `onTopOfChunkCenter`)
    through the biome provider, never loading a chunk: sunken shipwrecks `#is_ocean` (any ocean by
    `OceanKind`, so the ocean stream's seven biomes, vanilla's and other mods' oceans count), beached ones
    and buried treasure `#is_beach` (the dictionary's beaches without the stone beach and the mushroom
    island shore, which today's tag leaves out), warm ruins in warm and lukewarm oceans (deep lukewarm
    included), cold ruins in every other ocean. Each pair shares one structure set and never overlaps in
    biome, so the biome decides which one a candidate gets, as today.
*   **Layout** follows today's `generatePieces`/`addPieces`: the chunk's corner at y 90, a random turn, a
    random file of the kind. Shipwrecks turn around (4, 0, 15); ruins around their corner; a cold ruin is
    the brick file (integrity 0.9 large, 0.8 small) with its cracked (0.7) and mossy (0.5) twins over it; a
    large ruin (0.3) gets a cluster (0.9) of 4-8 small ones in today's eight spots, never over it.
*   **Heights at layout, not at placement.** Today's pieces measure the heightmap as they are placed; in
    1.12.2 the chunks a ship covers do not exist yet. So the start works today's numbers out from Core's
    `TerrainHeights` prediction: a sunken ship on the average `OCEAN_FLOOR_WG` of its (unturned) footprint,
    a beached one half its height (plus 0-2) below the lowest `WORLD_SURFACE_WG`, a ruin on the floor at
    its corner and then today's `getHeight` drop (the scan down through air, water and ice lands on
    `min(y - 1, floor - 1)` per column). On terrain Core cannot predict (`forWorld` null) the start is left
    empty and nothing is saved: no shipwrecks or ruins there (buried treasure still generates).
*   **Pieces**: `ShipwreckPiece` and `OceanRuinPiece` are Core `TemplatePiece`s placing Mojang's own files
    (`assets/updateaquaticbackport/structures/shipwreck/*.nbt`, `underwater_ruin/*.nbt`, copied unchanged from
    the 26.3 jar) with air and structure blocks left out and integrity as today's `BlockRotProcessor`.
    Markers: `supply_chest`, `map_chest`, `treasure_chest` give the chest below them its table; a ruin's
    `chest` becomes a chest (holding water when there is water there) with the big or small ruin table; a
    ruin's `drowned` spawns the trident stream's drowned (by name, persistent, `onInitialSpawn`) and becomes
    water below sea level, air above. `BuriedTreasurePiece` is one column inside its own chunk's populated
    area, so it reads the real world at populate time, exactly as today's piece: down from the sea floor to
    the first block on sandstone, stone, andesite, granite or diorite, the open sides walled in (the stone
    below, else the block there or sand), the chest facing away from what is beside it, never waterlogged.
*   **Save names** (permanent): starts `UAB_Shipwreck`, `UAB_OceanRuin`, `UAB_BuriedTreasure`; pieces
    `UAB_ShipwreckPiece`, `UAB_OceanRuinPiece`, `UAB_BuriedTreasurePiece`. Pieces save today's
    `isBeached`, `BiomeType`, `IsLarge` beside Core's template fields.
*   **Loot tables** under `updateaquaticbackport:chests/`: `shipwreck_map`, `shipwreck_supply`,
    `shipwreck_treasure`, `underwater_ruin_big`, `underwater_ruin_small`, `buried_treasure`, converted from
    26.3's by `tools/structure/generate.py` (cooked cod and salmon are cooked fish 0 and 1, lapis is dye 4,
    the potion is `set_nbt` Water Breathing, `#on_random_loot` enchanting is 1.12.2's `enchant_randomly`).
    Today's items 1.12.2 has no equivalent for (moss block, suspicious stew, bamboo, stone and iron spears)
    keep their weight as empty entries, so everything else comes up exactly as often as today; the coast
    armor trim and nautilus armor pools hold nothing 1.12.2 has and are left out. The heart of the sea joins
    buried treasure in `LootTableLoadEvent` when the conduit stream registers it (a table naming a missing
    item fails to load whole).
*   **Treasure maps** (`LootExplorationMap`, `updateaquaticbackport:exploration_map`): today's
    `exploration_map` for Core's `Structures` kinds: `Structures.nearest(buried_treasure, 50 chunks)`, a
    zoom-1 map centered there with tracking on, the biome preview, the decoration and the name key
    `filled_map.buried_treasure`; no map when none is found (today's filter discards it). The locator
    answers block (9, 9) of the chunk, today's `locate_offset`.
*   **Red X**: 1.12.2's `MapDecoration.Type` has no red X; one more type (`RED_X`, icon 10) is added with
    `EnumHelper` in pre-init on both sides, and the mod ships `assets/minecraft/textures/map/map_icons.png`:
    1.12.2's own sheet with 26.3's `red_x` sprite in slot 10 (1.12.2 fills slots 10-15 with a blue
    placeholder). A resource pack with its own `map_icons.png` hides the red X.
*   **Dolphins** find shipwrecks and ruins through Core's `Structures` (the dolphin stream's call is
    unchanged); `ScatteredStructure.register` registers the locators.
*   `ModernPalette.source("updateaquaticbackport")` is called in pre-init (here, as the first stream that
    places templates), so files naming kelp, seagrass and the like get this mod's blocks.

## Tested headlessly

`./gradlew runServer -Pselftest=structure` (port 25740, fresh world `eclipse/structure`, seed 1013):
**`SELFTEST DONE pass=1545 fail=0`**, no cascading-worldgen warning ("loaded a new chunk") in the log;
then `python tools/check_assets.py`: **88 blocks, 5489 states, 90 item models, 380 sound files, 0 errors**.

*   Registration: three locators, three overworld instances, today's spacing/separation/salt for each; the
    0.01 reducer keeps 1% of 200 000 chunks and matches today's formula; `RED_X` is icon 10 with no map
    tint; the mod's `map_icons.png` has a red X in slot 10.
*   Biome rules on vanilla's beaches, stone beach, mushroom shore, plains, ocean, river and the ocean
    stream's seven biomes.
*   **Every file** (20 shipwrecks, 48 ruins): loads, every block resolves, places in a test area with
    99%+ of its blocks in the world, no structure block left; markers (47 chest, 51 drowned, 16 map, 14
    supply, 14 treasure) all filled: chests have their tables, every drowned marker spawns a persistent
    drowned and becomes air above sea level.
*   **Loot**: every table loads; 300 rolls each (12 for the map tables) give only expected items, every
    buried treasure exactly one heart of the sea, Water Breathing potions; every buried treasure map has
    the red X on block (9, 9) of a buried treasure chunk, zoom 1, tracking, the name key, within 50 chunks.
*   **Real generation** in fresh chunks (11 x 11 loaded around each start): the nearest sunken shipwreck
    (deep ocean), beached shipwreck (beach), nine cold ruins (two in vanilla oceans, one in
    `deep_cold_ocean`) and a warm ruin (`deep_lukewarm_ocean`), and two buried treasures. Each: laid out
    and saved, every covered chunk populated, within 8 chunks of its start, each saved piece reads back
    the same, the locator finds it, every block of every piece compared with its file as turned (ships
    412/412 and 379/379; ruins as their integrity allows, a large cold ruin with its cluster 2089/2176 in
    18 pieces), every chest with its table (shipwreck map/treasure chests, ruin big/small chests), drowned
    present and persistent (10 in the clustered ruin), the predicted ground agreeing with the built world
    around it (Core's prediction against the real ground in a ring 3 blocks outside each box: all columns
    for most, 38 of 46 and 43 of 46 at the worst two); buried treasure chests on sandstone and stone, walled
    in, under the beach.
*   **`-Pselftest=all`** in a fresh world: **`SELFTEST DONE pass=15485 fail=0`**, no cascading-worldgen
    warning. An earlier `all` run here had 4 bubble-stream failures (a test pool's water frozen to ice when a
    neighboring chunk populated in a cold biome): the population flake the integration worktree is fixing in
    `Check.area()` (uncommitted there); the rerun passed.

## Not tested

*   Anything client-side: the red X drawn on a map (the texture is checked, its drawing is not), map
    colors of the biome preview.
*   Restarting on a world with saved starts and building the rest of them after the restart (pieces read
    back from their saves in the same run).
*   Other terrain mods (OTG, RTG, BOP, Caves Not Cliffs, Future Delight's world type): Core predicts their
    heights, but only vanilla terrain with the ocean stream was generated here; where Core cannot predict
    the terrain, no shipwrecks or ruins generate.
*   A chest emptied by a hopper before any player opens it gives no treasure map (1.12.2's loot context has
    no position; the map is searched from the opening player).
*   With the trident stream off (no drowned) or the conduit stream off (no heart): the code paths are
    null-checked, not run.

## Languages

One line in the `# --- structure ---` block of `en_us.lang` from `tools/structure/generate.py`:
`filled_map.buried_treasure`, 1.18's own key and English (`assets.name`). `tools/lang/vanilla_keys.py`
rerun: 270 vanilla names, 130 of our own; the new key is a vanilla name. `vanilla_keys.json` was regenerated
(it had not been rerun since earlier merges). The 17 other language files are left to the family's language
tool.

## For the integrator

*   **`UpdateAquaticBackport.java`**: one line each in `preInit` and `init` (`StructureContent`).
*   **`DolphinSelfTest`** (test code only): its treasure test unregistered the shipwreck and ruin locators at
    the end, which broke every later test that locates structures in `-Pselftest=all`. It now puts back the
    locator `ScatteredStructure.register` gives (Core has no getter to save the old one).
*   **`assets/minecraft/textures/map/map_icons.png`** overrides vanilla's. If another family mod also adds a
    map decoration, the two must share the ordinal and the sheet: `StructureContent.addRedX` reuses a type
    already named `RED_X`, but the sheet should then move to Backport Core with the decoration.
*   **`common/OceanKind`**: nothing to change. Warm ruins follow `isWarmish()`.
*   `ModernPalette.source(MOD_ID)` now lives in `StructureContent.preInit`; if Wl or another stream adds it
    too, keep one.
