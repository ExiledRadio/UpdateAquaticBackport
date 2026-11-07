# Stream O: ocean biomes and terrain

Package `ocean`. Config `oceans` (`enabled`, `underwaterCaves`). No blocks, items, entities or packets.

## Changelog section

*   **Ocean temperatures**: newly generated oceans are **warm**, **lukewarm**, **cold** or **frozen**, with
    **deep lukewarm**, **deep cold** and **deep frozen** forms, in regions a couple of thousand blocks
    across. Warm and frozen water never touches a coast. Ordinary oceans and deep oceans remain where the
    water is neither warm nor cold.
*   **Ocean floors** by temperature: sand in warm and lukewarm oceans, gravel in cold and frozen ones, with
    patches of clay, sand and gravel.
*   **Frozen oceans** freeze over in patches, with open water between, and carry **icebergs**: great
    masses of packed ice with snow on top, smaller icebergs, and rarely one of blue ice. Blue ice also
    grows on packed ice under the water. Deep frozen oceans have icebergs but no sea ice.
*   **Underwater caves and ravines** cut through the ocean floor, filled with water, with obsidian at the
    bottom (and magma blocks when bubble columns are on). They can be switched off.
*   **Ocean monuments** now generate in every kind of deep ocean, not only the plain one.
*   Squid in every ocean, as many as 1.13 has; polar bears on frozen oceans.

## How it works

*   **Biome map** (`OceanLayers`), through Forge's `WorldTypeEvent.InitBiomeGens`: 1.14.4's `OceanLayer`
    (improved noise, one sample per 256 blocks, seeded by the world seed) zoomed six times, and its
    `MixOceansLayer` appended to the finished map. 1.12.2's finished map is the same river-mixed map 1.13
    mixes oceans into, so no layer in the chain is rebuilt and other mods' changes survive. Only vanilla's
    ocean, deep ocean and frozen ocean cells are replaced; other mods' ocean biomes stay and count as
    ocean (not land) for the coast rule. The block-scale map is vanilla's Voronoi zoom over the result.
    Deep warm becomes a warm ocean, as in 1.13 (which never generated its deep warm ocean) and today's game.
*   **Seven biomes**, `updateaquaticbackport:` `warm_ocean`, `lukewarm_ocean`, `deep_lukewarm_ocean`,
    `cold_ocean`, `deep_cold_ocean`, `frozen_ocean`, `deep_frozen_ocean`. Our own frozen ocean rather than
    vanilla's (which 1.12.2 never generates): vanilla's terrain and temperature methods are final or
    shared. 1.14.4's depth, scale, temperature, downfall, squid and polar bear numbers, Forge 1.14's
    dictionary types, Forge's `BiomeManager.oceanBiomes`. Each is registered with `OceanKind.register`;
    vanilla's ocean and deep ocean are `NORMAL` where this map made the chunk (see "For the integrator").
*   **Floors**: vanilla's `generateBiomeTerrain` with the deep-floor block as a parameter, as 1.13's surface
    builder configurations (warm: sand, sand, sand; lukewarm: grass, dirt, sand; cold and frozen: vanilla).
    Written at vanilla's transposed `(z & 15, y, x & 15)` so the columns line up with vanilla's neighbors.
*   **Frozen oceans** (`BiomeFrozenOcean`): 1.14.4's `FrozenOceanSurfaceBuilder` ported (the big packed-ice
    icebergs and their snow caps), and its patchy temperature (0.2 where two noises agree). 1.12.2's
    `Biome.getTemperature` is final, so the population freezing pass (`PopulateChunkEvent.Populate` `ICE`)
    is replaced, only in chunks with a frozen ocean column, by the same pass using `temperatureAt`.
*   **Icebergs** (`Icebergs`): 1.14.4's `IcebergFeature` (packed ice 1 chunk in 16, blue ice 1 in 200) and
    `BlueIceFeature` (0-19 tries at y 30-61), centered 12-19 blocks into the decorated area so they stay
    in the four loaded chunks. The rare cave carved into an iceberg can reach 16 blocks out; it only
    carves loaded blocks. Writes use flags `2 | 16`: 1.12.2's flag 2 alone reads all six neighbors for
    observers and loads the next chunk for an edge block. Blue ice is
    `Owners.block(VILLAGE_AND_PILLAGE, "blue_ice")`; with neither mod's copy, no blue ice.
*   **Underwater carvers** (`UnderwaterCarvers`): wraps the ravine generator (`InitMapGenEvent` `RAVINE`),
    so it runs after the world's caves and ravines, whoever made them. 1.14.4's `UnderwaterCanyonWorldCarver`
    (1 in 50 per start chunk) and `UnderwaterCaveWorldCarver` (1 in 15), with their seeds, shapes and block
    lists, in chunks whose biome (at the chunk corner, as 1.14.4) is any ocean by `OceanKind`. Carved
    blocks below sea level become water; y 10 obsidian, or magma one time in four when
    `Owners.ourBlock("bubble_column")` exists; below that lava. 1.12.2's magma deletes the water above it
    on a random tick, which is why it waits for bubble columns.
*   **Flowing**: water a carver places next to air is noted per chunk and set flowing (with a tick) at
    `PopulateChunkEvent.Post`, as 1.13 schedules a fluid tick there. Notes are dropped at world unload and
    capped at 4096 chunks per world.
*   **Monuments** (`OceanMonuments`, `InitMapGenEvent` `OCEAN_MONUMENT`, only when it is still vanilla's
    class): vanilla's spacing, seed, structure and saved name, with 1.14.4's rule: the center any deep
    ocean, the 29-block surroundings any ocean or river.
*   **Biome names** are plain strings in 1.12.2 (vanilla's are English; there is no translation key), so
    they are 1.18's English names in `OceanContent`. `tools/ocean/generate.py` fails if one differs from
    1.18's `en_us.json`.

## Tested headlessly

`./gradlew runServer -Pselftest=ocean` in a fresh world (seed 1013, port 25741), then
`python tools/check_assets.py`: **`SELFTEST DONE pass=11540 fail=0`**, **74 blocks, 5210 states, 63 item
models, 60 sound files, 0 errors**.

*   Registration: seven biomes, names, ids, `OceanKind`, depth, scale, temperature, snow, dictionary types,
    Forge's ocean list, squid in all, polar bears only in the frozen two; vanilla ocean and deep ocean are
    `NORMAL`; the patchy temperature gives only 0.0/0.2 (frozen) and 0.5/0.2 (deep frozen) at sea level.
*   The biome map over 8192 x 8192 blocks around spawn (4-block cells): ocean 327885, deep ocean 413875,
    warm 126009, lukewarm 232168, deep lukewarm 169025, cold 51997, deep cold 101267, frozen 1913 and deep
    frozen 1848 (both found by a sparse search out to 16000 blocks); vanilla's frozen ocean never; **no warm
    or frozen cell within 32 blocks of land**; block-scale biomes agree with the 4-block map.
*   The overworld generator's ravine generator is the carver wrapper and its monument is ours.
*   Icebergs in a pool at sea level: packed ice above and below the surface, snow caps; a blue iceberg is
    blue ice; blue ice grows only under water next to packed ice, and spreads.
*   Carvers on a synthetic chunk (stone to y 50, water to the surface): 17 of 40 chunks carved, water only
    above y 10, obsidian only at y 10, lava only below, nothing at or above sea level, no magma without
    bubble columns. Noted water beside air flows once; water walled in stays still.
*   **Real generated chunks** (7 x 7 loaded, 5 x 5 checked, per biome): warm floors 73% sand, lukewarm and
    deep lukewarm 100% and 94% sand, cold and deep cold 100% and 94% gravel, frozen 99% gravel; no sea ice
    or packed ice outside the frozen two; the frozen ocean 1889 ice and 3040 open water at the surface; the
    deep frozen ocean no sea ice; **15200 packed ice and 362 blue ice** in the two frozen areas; **1401
    obsidian at y 10** from underwater caves in the 44800 ocean columns checked.
*   Monuments from eight starting points: all eight on a deep ocean, two on 1.13's new deep oceans.
*   **Every stream's test** (`-Pselftest=all`, fresh world): **`SELFTEST DONE pass=12408 fail=0`**, and no
    cascading-worldgen warning (an earlier version set carved water flowing with flag 2 and had one). Kelp:
    1501 kelp and 1236 seagrass in a vanilla ocean near spawn; coral: 2865 coral blocks in a `warm_ocean`,
    none in a `cold_ocean`.
*   With `oceans.enabled=false` (`-Pselftest=scaffold,coral,kelp`, fresh world): **`pass=274 fail=0`**; the
    server boots without the biomes and the coral test takes its old path (4513 coral in a warm stretch of a
    vanilla ocean by the noise, none in a cold one).
*   The server booting proves no client type is reached from server code.

## Not tested

*   Anything visual: how the new oceans, icebergs and caves look. Water color is unchanged here (1.12.2's
    water color is a multiplier on a blue texture, which cannot brighten it); stream A's water colors do it.
*   Gameplay over time: frozen-ocean patches freezing over during play (expected; vanilla's weather uses the
    biome's own temperature), polar bear and squid spawning in practice.
*   Other world types and biome mods (Biomes O' Plenty and others that build their own layers), and the
    block-scale mixing path for a mod that replaces vanilla's Voronoi layer.
*   Large biomes and customized worlds (the temperature map does not scale with biome size, as in 1.13).
*   Magma at the bottom of underwater caves (needs the bubble stream).

## Languages

Two `en_us.lang` lines in the `# --- ocean ---` block (the `underwaterCaves` config name and tooltip), from
`tools/ocean/generate.py`. `tools/lang/vanilla_keys.py` rerun: 78 vanilla names, 69 of our own (the new
lines are ours). The 17 other language files are left for the family's language tool after merge.

## For the integrator

*   **`common/OceanKind`**: vanilla's `Biomes.OCEAN` and `Biomes.DEEP_OCEAN` are `NORMAL` only in chunks
    this stream's biome map made; chunks generated before the stream was on, and worlds or world types
    its layers do not reach, keep the noise (`ocean/OceanChunks`, a flag in each chunk's saved data; see
    `docs/streams/followups.md`). Before the follow-ups this stream registered them as `NORMAL` everywhere.
*   **Older tests updated on this branch** (test code only), since they exercised the noise through
    vanilla's ocean, which this stream registers as `NORMAL`: `ScaffoldSelfTest` now samples
    `OceanKind.fromNoise`; `CoralSelfTest.worldGen` looks up `warm_ocean` and `cold_ocean` by name and uses
    them when present, else keeps its old vanilla-ocean check. `KelpSelfTest` needed no change.
*   **Stream A** (water colors): our biomes keep the default multiplier. 1.13's water / fog colors: warm
    4445678 / 270131, lukewarm and deep lukewarm 4566514 / 267827, cold and deep cold 4020182 / 329011,
    frozen and deep frozen 3750089 / 329011 (vanilla ocean 4159204 / 329011).
*   **Stream B** (bubble columns): magma is generated at y 10 of underwater caves only when a block named
    `bubble_column` is registered, with plain water above it. B must turn water over magma into a column
    (or keep 1.12.2's magma from deleting it on a random tick).
*   **Streams F, D, T, R, S**: fish, dolphins, turtles, drowned and ocean structures find these biomes through
    `OceanKind` and the dictionary; nothing here adds their spawns.
