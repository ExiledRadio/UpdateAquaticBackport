# Update Aquatic Backport: plan

Everything Java Edition 1.13 (Update Aquatic, with 1.13.1 and 1.13.2) added or changed, grouped into
streams that separate sessions can build at once, each in its own package and worktree.

Source: minecraft.wiki's *Java Edition 1.13* page and the pages it links. Where current Java behaves
differently from 1.13, current Java wins (marked **now:**). 1.14.4's decompiled code
(`MC1144Reference`, Forge 28.2.26 sources) gives the numbers the wiki leaves out. Assets come from the
1.18.2 client jar and the 1.18 asset index (`tools/assets.py`).

## Scope at a glance

| In | Out (and why) |
| --- | --- |
| Every new block, item, mob, effect, enchantment, potion, structure and ocean biome; 1.13's changes to existing mobs, blocks and water | The Flattening, the command rewrite, data packs, functions, tags and the other purely technical changes: 1.12.2 cannot take them and players never see them |
| Swimming, crawling, waterlogging (decision pending, see `MIXIN_DECISION.md`) | The four End biomes: the End's terrain did not change; they only split one biome id into four |
| | Debug-screen, options-menu and launcher changes |

## Content owned by another mod of the family

Village & Pillage Backport (`villagerbackport`) shipped these 1.13 blocks first, so it owns them:

- stripped logs and stripped wood, all six woods (`stripped_<wood>_log`, `stripped_<wood>_wood`);
- spruce, birch, jungle, acacia and dark oak trapdoors, buttons and pressure plates;
- blue ice;
- sea pickles;
- axe stripping.

Following Rule 1 of the family plan (`BACKPORT_FAMILY_PLAN.md` in Village & Pillage), this mod registers
its own copies **only when `villagerbackport` is not installed**: the same code, the same properties,
the same metadata, the same registry paths under `updateaquaticbackport:`. When the owner is present
nothing is registered and every lookup goes through `common/Owners`, which returns the owner's block.
The owner later renames our copies in place; because states match exactly, nothing moves.

Also owned elsewhere, not copied: smooth stone, smooth sandstone and smooth quartz (1.13 exposed them as
blocks; Village & Pillage adds them with 1.14's slabs and stairs).

## Streams

Each stream owns one package under `com.exiledradio.updateaquaticbackport`, one config category with an
`enabled` switch (already present in `ModConfig`), one block of packet ids and one block of entity ids.
No stream calls another's classes; when one needs another's item it looks it up by registry name with
`common/Owners.ours(name)`, which returns null when that stream is switched off.

| # | Stream | Package | Config | Entity ids | Packet ids | Needs |
| --- | --- | --- | --- | --- | --- | --- |
| W | Shared 1.13 blocks (copies) | `woodset` | `woodset` | none | 85-89 | nothing |
| K | Kelp and seagrass | `kelp` | `kelp` | none | 5-9 | nothing |
| C | Coral | `coral` | `coral` | none | 10-14 | nothing |
| F | Fish | `fish` | `fish` | 1-9 | 15-19 | nothing |
| T | Turtles | `turtle` | `turtles` | 10-14 | 20-24 | nothing |
| D | Dolphins | `dolphin` | `dolphins` | 15-19 | 25-29 | nothing |
| P | Phantoms | `phantom` | `phantoms` | 20-24 | 30-34 | nothing |
| R | Trident and drowned | `trident` | `trident` | 25-29 | 35-39 | nothing |
| N | Conduit | `conduit` | `conduit` | 30-34 | 40-44 | nothing |
| S | Ocean structures | `structure` | `structures` | 35-39 | 45-49 | Backport Core (structure templates, terrain adapters) |
| O | Ocean biomes and terrain | `ocean` | `oceans` | none | 50-54 | nothing |
| B | Bubble columns | `bubble` | `bubbles` | 45-49 | 55-59 | nothing |
| X | Other blocks and items | `blocks` | `blocks` | none | 60-64 | nothing |
| M | Mob changes | `mobs` | `mobs` | 40-44 | 65-69 | nothing |
| A | Water look and feel | `water` | `water` | none | 70-74 | nothing (parts need the mixin decision) |
| Sw | Swimming and crawling | `swim` | (new) `swimming` | none | 75-79 | the mixin decision |
| Wl | Waterlogging | `waterlog` + Backport Core | (new) `waterlogging` | none | 80-84 | the mixin decision; Backport Core |

Entity ids are this mod's own `EntityRegistry.registerModEntity` numbers. Potions, enchantments and
sounds have no numbers to collide on: each stream names them under its own words.

### W. Shared 1.13 blocks (copies)

The blocks listed under *Content owned by another mod*, as exact copies (same classes, states, metadata,
recipes, smelting, fuel, flammability, ore dictionary, axe stripping and narrowed oak recipes), registered
only when `villagerbackport` is absent. Also:

- remap `villagerbackport:<name>` to ours in Forge's missing-mappings event, for a world that had the owner
  and lost it;
- the sea pickle keeps its own water rule (it has a `waterlogged` property and a material that blocks
  water), unchanged from the owner's, until real waterlogging exists; pickles spread on coral with bone
  meal when the coral stream is on.

### K. Kelp and seagrass

- **Kelp** (`kelp`, top, age 0-25 in 1.13; `kelp_plant`, stem). Grows only with water above, 14% per
  random tick, a new top one block up. Placed only in water sources, on a block with a solid top or on
  kelp. Breaking any piece breaks everything above. **now:** bone meal grows it. Shears on the top stop
  its growth for good (**now:**, 1.18).
- **Dried kelp** (food 1, saturation 0.6, eaten in 16 ticks), smelted or campfire-cooked from kelp.
- **Dried kelp block**: 9 dried kelp, burns 4000 ticks (20 items), compostable 50% with a 1.14 composter.
- **Seagrass** (`seagrass`, `tall_seagrass`). Replaceable, drops only with shears. Bone meal on seagrass
  makes tall seagrass; bone meal on the floor under water spreads seagrass (**now:**, as 1.16+).
- Generation in oceans (kelp not in warm or frozen oceans; seagrass everywhere underwater including rivers
  and swamps), per `common/OceanKind`.
- Compost values (kelp 30%, dried kelp 30%, seagrass 30%, dried kelp block 50%) when a composter exists.

### C. Coral

- 5 coral blocks (tube, brain, bubble, fire, horn) and 5 dead ones. Die after a random 60-100 ticks
  without water touching any face. Silk Touch drops the living block, otherwise the dead block.
- 5 coral and 5 dead coral (dead coral plants came in 1.13.1); 5 coral fans and 5 dead fans; their wall
  forms. Plants and fans need water in their own space and die without it; Silk Touch only.
- Coral reefs in warm oceans: claw, mushroom and tree shapes, with coral, fans and sea pickles on top.
- Bone meal on a sea pickle on a living coral block spreads pickles (owner's or our sea pickle).

### F. Fish

- **Cod, salmon, pufferfish, tropical fish** as mobs: swimming AI, schooling (**now:**), flopping and
  suffocating on land, avoiding players. Cod in normal/cold/lukewarm oceans (groups of 3-6), salmon in
  cold and frozen oceans and rivers, pufferfish in lukewarm and warm, tropical fish in warm and lukewarm
  (lush caves belong to a later update).
- **Pufferfish**: puffs up in three stages near non-sneaking players and axolotl-less mobs; stings with
  Poison on touch and Poison and Nausea on eating, with the current Java wiki's durations.
- **Tropical fish**: 2 shapes x 6 patterns x 15 x 15 colors; 22 named common variants; tooltip on the
  bucket.
- **Buckets** of each: right-click a fish with a water bucket; empty to release with its variant and name.
- The fish items stay vanilla's: 1.12.2's raw fish metadata 0-3 are cod, salmon, tropical fish and
  pufferfish already. Their names change to 1.13's ("Raw Cod", "Tropical Fish").
- Advancements *Fishy Business* and *Tactical Fishing*.

### T. Turtles

- Turtles on warm beaches (groups of 2-5), home beach, breeding with seagrass (looked up by name), laying
  1-4 eggs at home, slow on land, fast in water, babies dropping a scute when grown.
- **Turtle eggs**: 1-4 per block, hatch in 3 stages (faster at night), trampled by mobs and players, broken
  by zombies, husks, zombie pigmen and drowned that seek them out.
- **Scute**, **turtle shell** helmet (2 armor; Water Breathing 10 s while not underwater, as 1.13), repair
  with scutes.
- **Potion of the Turtle Master** (Slowness IV + Resistance III 20 s; **now:** these values), long and
  strong forms, splash, lingering and tipped arrows; brewed from turtle shell.
- Undead, wolves, foxes, ocelots and cats attack baby turtles.

### D. Dolphins

- Dolphins in non-frozen oceans (small groups), breathe air (4800 ticks),
  play with dropped items, follow boats, jump, are neutral and attack in groups.
- **Dolphin's Grace** effect for players swimming nearby.
- Feeding raw cod or salmon makes it lead to the nearest shipwreck, ocean ruin or buried treasure, through
  a lookup the structure stream fills in (`common` gets a small `Structures` locator interface; with the
  structure stream off they swim about).

### P. Phantoms

- Phantoms spawn over players who have not slept for 3 days (72000 ticks of "time since rest", tracked
  per player since 1.12.2 lacks that statistic; reset by sleeping and dying), at night or in a
  thunderstorm, with sky access, in groups by difficulty, by the current Java wiki's spawning rules. Circle and swoop,
  burn in daylight, size from spawn.
- **Phantom membrane** (drop 0-1, Looting adds); repairs the elytra (instead of leather).
- **Slow Falling** effect, potions (1:30, long 4:00), splash, lingering, tipped arrows.
- Cats scare phantoms (other mods that look up `phantom` by name pick this one up).

### R. Trident and drowned

- **Trident**: 9 melee damage, 1.1 speed, thrown 8 damage, durability 250, sticks and can be picked up.
- **Loyalty** I-III, **Riptide** I-III, **Impaling** I-V (**now:** Java still limits Impaling to aquatic
  mobs), **Channeling** I. Riptide's spin attack and pose need a client hook; it works without the
  swimming stream.
- **Drowned**: spawn in oceans and rivers (more at night), in ocean ruins; a zombie converts after 30 s
  underwater, plus 15 s; husks convert to zombies the same way. Trident throwers; nautilus shell in the
  off hand; current Java's drops except the copper ingot (a later update's).
- Advancements *A Throwaway Joke* and *Very Very Frightening*.

### N. Conduit

- **Conduit** block with its tile entity and animated renderer, activated by a 5x5x5 frame of prismarine,
  prismarine bricks, dark prismarine and sea lanterns (16 blocks minimum, 42 for full power), range 16 per
  7 frame blocks, kills hostile mobs within 8 blocks at full power (4 damage every 2 s).
- **Conduit Power** effect: water breathing, night vision underwater, haste.
- **Nautilus shell** (fishing treasure, drowned) and **heart of the sea** (buried treasure).

### S. Ocean structures (needs Backport Core)

- **Shipwrecks** (20 templates; beached and sunken), **ocean ruins** (warm and cold, big and small,
  clusters), **buried treasure** (one chest, at the beach), with 1.13's loot tables.
- **Buried treasure map** from shipwreck and ocean ruin chests; 1.13's red X marker.
- Dolphin targets for stream D.
- Needs Backport Core's modern structure template loader (1.13+ NBT palettes to 1.12.2 states) and its
  terrain adapters. Starts once Core is out.

### O. Ocean biomes and terrain

- Warm, lukewarm, cold and frozen oceans, deep lukewarm, deep cold, deep frozen (**now:** no deep warm
  ocean; it was removed in 1.18), in place of vanilla's ocean by the same temperature rule as
  `common/OceanKind`, through Forge's biome generation event (no mixin).
- Ocean floors of sand, gravel and clay by temperature; icebergs (packed ice and blue ice, looked up via
  `Owners`) in frozen oceans; underwater caves and ravines (magma at the bottom when the bubble stream is
  on).
- Each biome is registered with `OceanKind.register`, so every stream's generation follows it.

### B. Bubble columns

- Bubble column block over soul sand (up) and magma blocks (down), filling the water column; pushes and
  pulls entities, sinks boats, refills air, particles and sounds. Uses the same water-block technique as
  kelp (`common/BlockPseudoWater`).

### X. Other blocks and items

- Prismarine, prismarine brick and dark prismarine stairs and slabs (stonecutter recipes appear on their
  own where a 1.14 stonecutter reads crafting recipes).
- **Carved pumpkin**: shears on a pumpkin carve it (4 seeds); the plain pumpkin loses its face; golems and
  jack o'lanterns take the carved one.
- **Wood** (bark) blocks: 4 logs make 3, as 1.12.2's hidden all-bark log.
- Packed ice from 9 ice; **debug stick**; undyed (purple) shulker box and washing a dyed one in a cauldron.
- Smaller rules where they are visible and need no patches: fence gates and pumpkins need no support,
  doors place on a side face.

### M. Mob changes

- Undead mobs sink in water; squid squirt ink and flee; polar bears spawn on ice; parrots imitate phantoms
  and drowned; skeleton horses can be ridden underwater; zombies and baby zombies as 1.13 (babies burn).
- Done through events and AI tasks added on spawn, never by editing a goal list from inside a goal.

### A. Water look and feel

- Biome water colors (1.13's per-biome colors via Forge's water color event), underwater fog by depth and
  biome, underwater ambience sounds, water reducing light by 1 instead of 3 (new water only), air
  refilling gradually, items and experience orbs floating.
- Some of these (floating items, the jump out of flowing water) are easiest as mixins; they wait for the
  decision.

### Sw. Swimming and crawling (decision pending)

Sprinting underwater swims (0.6 x 0.6 hitbox), crawling under one-block gaps, 1.13's swim animations.
Needs player mixins. See `MIXIN_DECISION.md`.

### Wl. Waterlogging (decision pending)

Real waterlogging for this mod's blocks (and the family's: lanterns, campfires, sea pickles, chains,
scaffolding) through Backport Core. When it exists, `common/BlockPseudoWater` switches its blocks over and
the known limit (water inside the block is not drawn from the side) goes away.

## Shared files and rules

Every stream touches a few shared files. To keep merges mechanical:

- **`ModConfig.java`**: every category already exists. A stream edits only inside its own class. Category
  classes extend `Object`.
- **`NetworkHandler.java`**: register packets only with ids from your block, in id order.
- **`UpdateAquaticBackport.java`**: at most one line per lifecycle hook, calling your package.
- **`selftest/SelfTestRunner.java`**: one line in `TESTS`.
- **`en_us.lang`, `sounds.json`**: written only by your generator through `tools/assets.py`
  (`merge_lang` / `merge_sounds`), which replaces your own `# --- <stream> ---` block and nothing else.
- **`common/`**: read freely. Changing a `common` class is the integrator's job; a stream that needs a
  change writes it up in its stream doc instead.
- **Registration**: from your own `@Mod.EventBusSubscriber` class, through `common/Content` (names,
  creative tab, item models). Blocks drawn from multipart files call `Content.multipart`.
- **Other streams' items**: by name with `Owners.ours("name")`, null-safe. Owned blocks with
  `Owners.block(Owners.VILLAGE_AND_PILLAGE, "name")`.
- **Docs**: your changelog section, what was tested and what was not, in `docs/streams/<stream>.md`.

## Build and test

- `./gradlew build` never deploys anywhere unless `uabDeployDir` is set in `~/.gradle/gradle.properties`
  (deliberately not the workspace-wide `deployDir`).
- Test on a dedicated dev server: `./gradlew runServer -Pselftest=<stream>` compiles in
  `src/selftest/java`, runs the stream's test after the server starts, writes the state and item dumps,
  logs `SELFTEST DONE pass=N fail=M` and stops the server. Then `python tools/check_assets.py` must report
  0 errors. Port 25730 here; a stream worktree uses `25730 + its row number`.
- Never `cleanRunServer` (it deletes `eclipse/`); runs always rerun. Kill only servers you started, by
  PID.
- Build last: any run task rewrites `build/libs` with an unobfuscated jar. Before a jar leaves the
  workspace, check its classes carry `func_` names.
- Client-only types never appear in a class a server loads; put them in a `client/` class.
- No mixins and no coremod until the decision is made, and then only in the streams that need them.

## Integrations with other mods of the family (Rule 2)

All soft: `Loader.isModLoaded` and names, never a hard dependency.

- **Composter** (Village & Pillage): kelp, dried kelp, seagrass, dried kelp block, sea pickles
  (`common/Compat.compost`).
- **Owner's blocks**: axe stripping and every lookup use the owner's stripped logs, blue ice and sea pickles
  when it is installed (`Owners`).
- **Things Village & Pillage looks up by name** and finds here without changes: phantoms (`phantom`, cats
  scare them), phantom membrane (cat gifts), raw fish (vanilla items).
- **For the Village & Pillage maintainer** (not done here): its trades and wandering trader list other mods'
  scutes, dried kelp blocks, coral blocks, fish buckets and nautilus shells by name; adding
  `updateaquaticbackport:` names there makes them sell ours.
- **Waterlogging** for the family's lanterns, campfires and sea pickles comes with stream Wl.

## Order

1. Scaffold, this plan, the mixin decision write-up: **done** (first session).
2. Streams W, K, C: **done** (first session; see `docs/streams/`). Then F, T, D, P, R, N, O, B, X, M, A in
   parallel.
3. S once Backport Core has the template loader.
4. Sw, Wl, and the mixin parts of A after the decision.
