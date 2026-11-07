# Briefs for the remaining streams

One session per stream, all at once. Each brief is complete on its own; `docs/PLAN.md` has the full
content list and the shared-file rules, `docs/streams/kelp.md` and `coral.md` show what "done" looks like.

## Common to every brief

- **Where**: a git worktree of `E:\MincraftModding\Workspaces\UpdateAquaticBackport`, made with
  `git worktree add ..\UpdateAquaticBackport-<stream> -b ua-<stream> main`. Commit on that branch; never on
  `main`, never push, never merge.
- **Read first**: `docs/PLAN.md` (your stream's section and *Shared files and rules*), `docs/streams/kelp.md`,
  and the memory notes on FG2, reobf, `@Config`, sided classes, lang `pack.mcmeta` and the dev-env coremod
  crash in `C:\Users\Radio\.claude\projects\E--MincraftModding\memory\`.
- **Behavior**: current Java Edition per minecraft.wiki; 1.14.4's decompiled code for numbers
  (`MC1144Reference\build\mcsources\forge-1.14.4-28.2.26_mapped_snapshot_20190719-1.14.3-sources.jar`).
  Assets from the 1.18.2 jar and 1.18 asset index through `tools/assets.py`; write a `tools/<stream>/generate.py`.
- **Code**: your package only; register through `common/Content`; your `ModConfig` category already exists;
  your entity and packet ids are in the plan's table; other streams' items by name with `Owners.ours(...)`.
  A change to `common/` is the integrator's: describe it in your stream doc instead.
- **Test**: a `selftest/<Stream>SelfTest.java` (one line in `SelfTestRunner.TESTS`), run with
  `./gradlew runServer -Pselftest=<stream>` on port `25730 + row number` (set it in your worktree's
  `eclipse/server.properties`; copy `eula.txt` and that file from the main worktree's `eclipse/`), then
  `python tools/check_assets.py` with 0 errors. Never `cleanRunServer`. Kill only your own server, by PID.
  Entities: spawn them, tick them, check AI, drops, spawning rules and saving (write to NBT and read back).
  Client renderers and models cannot be seen headlessly: keep them in `client/` classes, make sure the
  server boots, and list them under "Not tested".
- **Done**: content working in the headless test, `./gradlew build` clean as the last Gradle command, the
  jar's classes using `func_` names, `docs/streams/<stream>.md` (changelog section, how it works, tested,
  not tested), a commit on your branch. No mixins, no coremod.
- **Rules**: never mention other backport mods in user-facing text; American spelling; never send
  keystrokes or clicks to the user's screen.

## F. Fish (row 4, port 25734)

Cod, salmon, pufferfish and tropical fish as water mobs (entity ids 1-9, packets 15-19), package `fish`,
config `fish`. Swimming AI, schooling, flopping and suffocating on land, fleeing players; pufferfish
puffing in three stages and stinging; tropical fish's 2 shapes x 6 patterns x 15 x 15 colors and the 22
named variants; buckets of each that keep variant and name; spawning by `common/OceanKind`; drops use
vanilla's raw fish items (1.12.2's fish metadata 0-3 already are cod, salmon, tropical fish, pufferfish)
renamed as 1.13 did; *Fishy Business* and *Tactical Fishing*. Renderers and the six models ported from
1.14.4's model classes.

## T. Turtles (row 5, port 25735)

Turtles (entity ids 10-14, packets 20-24), package `turtle`, config `turtles`: beach spawning, home beach,
breeding with seagrass (`Owners.ours("seagrass")`, null-safe), laying 1-4 eggs, babies dropping a scute on
growing up; turtle egg block (hatching stages, trampling, zombies seeking them); scute; turtle shell helmet
(Water Breathing rule as current Java) repaired with scutes; Turtle Master potion with long and strong forms
and tipped arrows, brewed from the shell; undead and predators attacking baby turtles (AI added on spawn,
never from inside a goal).

## R. Trident and drowned (row 7, port 25737)

Package `trident`, config `trident`, entity ids 25-29, packets 35-39: the trident item and thrown entity,
Loyalty, Riptide, Impaling (aquatic mobs only, as Java), Channeling; the drowned (spawning in oceans and
rivers, trident throwing, nautilus shell in the off hand via `Owners.ours("nautilus_shell")`), zombies
converting after 30 s underwater and husks to zombies; *A Throwaway Joke* and *Very Very Frightening*.
Riptide's spin needs only a client render hook, no mixin.

## D. Dolphins (row 6, port 25736)

Package `dolphin`, config `dolphins`, entity ids 15-19, packets 25-29: dolphins (air supply, playing with
items, following boats, jumping, neutral group defense), Dolphin's Grace, feeding raw cod leading to the
nearest structure through a small locator the structure stream fills in (write the interface up in your doc;
with nothing registered they just swim).

## P. Phantoms (row 8, port 25738)

Package `phantom`, config `phantoms`, entity ids 20-24, packets 30-34: phantoms and their insomnia spawning
(track time since rest per player; reset on sleep and death), swooping, burning, sizes; phantom membrane and
elytra repair with it; Slow Falling with its potions and tipped arrows. Name the entity `phantom` and the item
`phantom_membrane`: other mods find them by those names.

## N. Conduit (row 9, port 25739)

Package `conduit`, config `conduit`, packets 40-44: the conduit block and tile entity with its frame check
(prismarine, prismarine bricks, dark prismarine, sea lanterns), range, Conduit Power, attacking hostile mobs at
full power, its renderer; the nautilus shell (fishing treasure) and heart of the sea items.

## O. Ocean biomes and terrain (row 11, port 25741)

Package `ocean`, config `oceans`, packets 50-54: warm, lukewarm, cold and frozen oceans and the deep forms
current Java has (no deep warm), through Forge's biome generation event; each registered with
`OceanKind.register` so every stream's generation follows it; floors by temperature; icebergs (blue ice via
`Owners.block(Owners.VILLAGE_AND_PILLAGE, "blue_ice")`); underwater caves and ravines. Test with a fresh
world and real generated chunks.

## B. Bubble columns (row 12, port 25742)

Package `bubble`, config `bubbles`, entity ids 45-49, packets 55-59: the bubble column block over soul sand and
magma, built on `common/BlockPseudoWater` like kelp; pushing, pulling, boats sinking, air refilling, particles
and sounds.

## X. Other blocks and items (row 13, port 25743)

Package `blocks`, config `blocks`, packets 60-64: prismarine stairs and slabs (three each), carved pumpkin and
shears carving, the wood (bark) recipe, packed ice from ice, the debug stick, the undyed shulker box and
cauldron washing, and the smaller placement rules in the plan.

## M. Mob changes (row 14, port 25744)

Package `mobs`, config `mobs`, entity ids 40-44, packets 65-69: undead sinking in water, squid ink and fleeing,
polar bears on ice, parrots imitating phantoms and drowned, skeleton horses underwater. AI added on spawn.

## A. Water look and feel (row 15, port 25745)

Package `water`, config `water`, packets 70-74: biome water colors (Forge's water color event), underwater fog
by depth, underwater ambience, water's light reduction, gradual air refill. Floating items and the jump out of
flowing water without patches; floating items and orbs follow the user's answer to open point 2 of
`docs/MIXIN_DECISION.md`.

## Waiting

- **S. Ocean structures**: needs Backport Core's modern structure template loader and terrain adapters.

## Sw. Swimming and crawling (start only after the user approves `docs/MIXIN_DECISION.md`; port 25746)

Our own code, written from scratch: no MixinBooter, no mixins, no ASM, and nothing taken from, adapted from
or credited to another mod. The pose engine (pose state, size, eye height, sync, drawing) is shared code for
the family, so build it in Backport Core (`E:\MincraftModding\Workspaces\BackportCore`, in its own worktree
and branch, coordinated with whoever owns that repo) and the swimming and crawling rules in Update Aquatic's
`swim` package (packets 75-79, a new `swimming` config category with `enabled` and `crawling`). Follow the
design table in `docs/MIXIN_DECISION.md`: size and eye height at `PlayerTickEvent` END on both sides, swim
motion on the client after vanilla's water movement, crawl speed through `InputUpdateEvent`, the pose sent to
tracking clients, the body rotated in `RenderPlayerEvent`, and limbs from our own `ModelPlayer` subclass set into
both player renderers. Stand down when a known player-resizing or player-model mod is installed (by mod id,
never named in game). Headless tests: a server player in water sprinting becomes 0.6 x 0.6 with eyes at 0.4;
one in a one-block gap stays crawling for 200 ticks with no suffocation damage and stands up when the ceiling
goes; the server's movement check accepts a crawl through the gap (no "moved wrongly"); the pose packet
round-trips. Visual parts are listed as not tested. If any part proves impossible without patching, stop and
report exactly which part and why; do not write ASM.

## Wl. Waterlogging (start only after the user approves `docs/MIXIN_DECISION.md`; port 25747)

Our own system, framework in Backport Core (its own worktree and branch, coordinated with its owner), no
patches and no dependency on Fluidlogged API. Build `IWaterloggable` and the `Waterlogging` helper (the
`waterlogged` property plus a fixed water `level`, material and light by state, buckets, placing into water,
breaking leaving water, spreading flowing water from a newly waterlogged block, one model-name mapping), and the
client baked-model wrapper that draws water inside the block on faces bordering non-water and the surface at 8/9
height. Then move Update Aquatic's `common/BlockPseudoWater` blocks (kelp, seagrass, coral) onto it with their
states and metadata unchanged, so no world changes. Fluidlogged API: a listener class loaded only when
`fluidlogged_api` is present answers its `FluidloggableEvent` so our blocks are never fluidlogged by it, compiled
against a stand-in kept out of the jar. Its coremod cannot load in the FG2 dev environment, so that part is tested
in E2Eu-Cleanroom (it has v3.3.3): our blocks beside its fluidlogged ones, flow, buckets, rendering. Vanilla blocks
are not waterlogged by us (see open point 1 of the design). Headless tests as for kelp and coral, plus spreading and
the meta layout of an existing block gaining the property.
