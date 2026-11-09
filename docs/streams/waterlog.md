# Stream Wl: waterlogging

Kelp, seagrass, coral, coral fans, the conduit and bubble columns keep their water the way 1.13 does: as
waterlogging, through Backport Core's `Waterloggable` adapter over Fluidlogged API (3.3.3). The pseudo-water
technique (`common/BlockPseudoWater`, `common/PseudoWaterEvents`, the fixed `level` property, the drawn-only
`surface` property, the `waterlogged` property, the water-surface model and the water tint) is gone. No
config, no packets, no entity ids, no mixins, no ASM. One Forge field is set by reflection
(`common/WaterSpawning`, below).

## Changelog section

*   **Real waterlogging**: kelp, seagrass, coral, coral fans, conduits and bubble columns hold water the
    1.13 way. Their water is drawn by the water renderer, flows out
    of them into open space, and goes with them only when they need it.
*   Kelp, seagrass and bubble columns always hold water: a block that drains them (a sponge, a command)
    takes them with it. An empty bucket aimed at kelp or seagrass does nothing, as in 1.13; one aimed at a
    bubble column takes its water and leaves air.
*   Coral, coral fans and the conduit may hold water or not: a bucket empties or fills them, water flowing in
    fills them, and a coral drained of its only water dies 3-5 seconds later.
*   Light goes through all of them exactly as through the water around them.
*   Kelp and seagrass can be placed in falling water, which becomes a source, as in 1.13.
*   Squid, fish, dolphins and drowned spawn in the water of waterlogged blocks (a kelp forest's kelp, a
    reef's coral), as in 1.13.
*   Prismarine stairs and slabs hold water (a double slab does not).
*   Worlds from earlier builds: kelp, seagrass, bubble columns and waterlogged coral, fans and conduits get
    their water back the first time their chunk loads.

## How it works

*   **`common/BlockUnderwater`** (replaces `BlockPseudoWater`): `Block implements Waterloggable`, a plant's
    shape (no collision, not opaque, `UNDEFINED` faces, pistons destroy it, cut-out layer). Materials:
    `OCEAN_PLANT` (kelp, living coral) and `SEA_GRASS` (seagrass, bubble columns; replaceable), both
    `MaterialLogic` - not liquid, not solid, not blocking movement, so 1.12.2 code that reads a water
    material's `LEVEL` never sees them. Fluidlogged API does not wash a waterloggable block away: water flows
    into it instead. Dead coral keeps the coral stream's stone-like `DRY` material; the conduit stays glass.
    `fullWater` is 1.14.4's placement test (`isTagged(WATER) && getLevel() == 8`: a source, plain or kept, or
    falling water). Breaking, explosions and loss of support go through `setBlockToAir` / `destroyBlock`,
    which Fluidlogged API makes leave the block's water.
*   **Needs** (`waterNeed`): kelp, kelp plant, seagrass, tall seagrass and the bubble column are `REQUIRED`
    (drained, Core removes them with no drop); coral, fans, wall fans (living and dead) and the conduit are
    `OPTIONAL` (Core's default). Coral's death timer runs from `onWaterChanged(..., false)` as well as from
    neighbor changes; `isInWater` is `Waterlogging.hasWater` here or on any face.
*   **`common/RequiredWater`**: today's kelp, seagrass and bubble column carry their water in their state, so
    `/setblock kelp` in the air makes a water source, and kelp in falling water makes it a source. Here a
    `REQUIRED` block queues its position in `onBlockAdded`; at the end of the world tick, one still without a
    source gets one (`setWater`, flag 3, live). Waiting matters: inside `onBlockAdded` the water being
    replaced has not been moved into the block yet. Every placement by hand or generation is into a source
    already, so the check finds nothing to do.
*   **Buckets** (`KelpContent.onBucket`): `FillBucketEvent` with an empty bucket aimed at kelp or seagrass is
    cancelled (1.14.4's kelp and seagrass are not `IBucketPickupHandler`s; Core's default would drain them and
    so remove them). The bubble column is one (it gives water and leaves air), and Core's `REQUIRED` drain does
    exactly that, so it is left to Fluidlogged API. Water buckets: Fluidlogged API's own handling; kelp and
    seagrass stay as they are (it reports SUCCESS and empties the bucket, as 1.14.4's does when it places the
    water beside them).
*   **Generation**: kelp columns, seagrass, coral plants, fans, wall fans and sea pickles are placed with
    `Waterlogging.placeKeepingWater(world, pos, state, 2)` (still water, as generated water is). Coral blocks
    and bubble columns use `setBlockState` as before (a full block cannot hold water; a column is set into a
    source, which Fluidlogged API keeps).
*   **`common/LegacyWater`**: no property names were ever saved (1.12.2 saves ids and metadata), so old
    chunks load the same blocks, only without their water. A chunk read from disk without the
    `updateaquaticbackport:waterMoved` mark is searched in its saved data (`Sections`: `Blocks`, `Add`,
    `Data`) for kelp, kelp plant, seagrass, tall seagrass and bubble columns (every state), coral plants and
    fans (metadata bit 1) and wall fans (bit 4), and the conduit (bit 1); the end of the world tick gives each
    one still there a still water source. Chunks are marked when saved (not while they still wait), so this
    runs once per chunk. The coral and conduit metadata bits are now ignored when read.
*   **`common/WaterSpawning`**: 1.12.2's `IN_WATER` spawn test asks for water *material* at the spot and below
    it, which a waterlogged block's water is not, so no water mob would spawn in kelp (the dolphin test's
    spawn spots dropped from 154 to 144 without it). Forge lets a `SpawnPlacementType` carry a predicate
    (`spawnPredicate`, Forge's own field, so the same name in dev and release); init sets `IN_WATER`'s, if
    nothing else has, to 1.14.4's: `Waterlogging.hasWater` here and below, no normal cube above.
*   **Other streams' water checks** that read `getMaterial() == Material.WATER` at a position now read
    `Waterlogging.hasWater` (1.14.4's `getFluidState(pos).isTagged(WATER)`): coral blocks' six faces, the
    conduit's 3x3x3, dolphin navigation, jumping, finding water and breathing (`canBreatheAt` treats kept
    water as liquid, except a bubble column's), dolphin and fish spawn checks, fish swimming targets, squid
    fleeing, drowned spawning and finding water, turtle path weights and travel, shallow-water depth
    (`ShallowWater.water` reads the kept water's level through `FluidloggedUtils`), and the bubble column
    particles. Entity checks (`isInsideOfMaterial`, `isInWater`), swimming, drowning, boats, fog and the
    camera are Fluidlogged API's own patches and were left alone. Plain-water checks that 1.14.4 makes with
    `== Blocks.WATER` (kelp growth, bonemeal underwater, feature placement, turtles going to water, bubble
    columns forming) still ask for a plain water block.
*   **The fish bucket** fills any block that can hold water (`Waterlogging.canHoldWater`) and lets the fish
    out; kelp, which already holds water, just takes the fish.
*   **Prismarine stairs and slabs** extend vanilla's `BlockStairs` / `BlockSlab`, which Fluidlogged API's
    default whitelist covers (slabs only with `half` bottom or top), so no code: the double slab has no `half`
    property and refuses water.
*   **Assets**: `tools/assets.py` has `multipart_blockstate` instead of `pseudo_water_blockstate` and
    `water_surface`; the kelp, coral, bubble and conduit generators were rerun (only their blockstates
    changed); `models/block/water_surface.json` is deleted; `client/ClientSetup` lost the water tint handler.
*   **Light**: our blocks have no opacity of their own (0); Fluidlogged API's chunk patch takes the larger of
    the block's and its water's, so they dim exactly as water does: 1 a block with the water stream's
    `lightThroughWater`, vanilla's 3 without it.

## Tested (headless, `./gradlew runServer -Pselftest=all`, Fluidlogged API 3.3.3 in dev)

Two runs, each in a fresh world: 14005 and 14004 checks passed, 0 failed (the baseline on `main` c06917b
was 13941 / 0; the difference of one between runs is a loop whose length depends on the world). Every
generation and biome count logged by the kelp, coral, dolphin and ocean tests is identical to the baseline's
(1501 kelp and 1236 seagrass in 25 chunks; 2865 coral at the warm ocean; the ocean biome map; icebergs; the
carver; dolphin spawn spots 154 / 125). `python tools/check_assets.py`: 88 blocks, 468 states, 0 errors.

New checks (`KelpSelfTest`, `CoralSelfTest`, `BubbleSelfTest`, `ConduitSelfTest`, new `WaterlogSelfTest`):

*   kelp placed in water keeps it, as a source; every piece kelp grows into keeps its water; breaking kelp
    leaves water; drained kelp is removed with no drop; generated kelp and seagrass all hold water (0 dry in
    25 chunks);
*   a real empty bucket, used by a player looking down at kelp and at seagrass, stays empty and leaves them;
    a water bucket leaves them as they are;
*   sky light down a 9-high kelp column equals the water beside it at every depth; seagrass and a bubble
    column likewise; kelp itself has opacity 0;
*   kelp and seagrass set in the air get a source at the end of the tick; kelp can be placed in falling water
    (not in thin flowing water) and turns it into a source;
*   an old chunk's kelp is found by its saved id (with `Add`), and an old world's kelp gets its water back
    once;
*   coral placed in water keeps it; a fan placed on land is dry and can hold water; a real water bucket
    waterlogs a dry coral in a stone cup and comes back empty, putting no water above it; a coral with only
    its own water lives; an empty bucket takes the water, the coral is scheduled, and **a drained coral
    dies**; a fan drained by `setWater` dies too; reef coral and fans hold water; generated living coral in
    the warm ocean has none dry;
*   every bubble column block keeps its source; the bucket on a column still takes the water and leaves air;
    the push, air and boat tests pass unchanged;
*   the conduit has one state; placed into water it keeps it, on land it is dry; drained it deactivates;
    broken under water it leaves a source;
*   water mobs can spawn in kelp (`IN_WATER`), not in the air, still in plain water; a pig in kelp is in water
    and loses air; a boat over kelp floats;
*   prismarine stairs and both slab halves keep water, double slabs refuse it;
*   a fish bucket on kelp lets the fish out and leaves the kelp; on a dry coral it waterlogs it;
*   no block of ours is a liquid material.

## Not tested

*   Anything drawn: Fluidlogged API's water inside our blocks (side faces, the surface, through glass), the
    plants' cut-out layer, the empty bubble column and conduit models. The client was not started.
*   Fluidlogged API's water rendering in a bubble column whose block model is empty (render type `MODEL`):
    assumed to draw the water as for any waterlogged block.
*   `LegacyWater` on a real world saved by an earlier build (the scan and the queue were tested on a
    synthetic chunk tag and a placed block, not through a chunk load from disk).
*   A sponge on kelp (Fluidlogged API drains it; Core's `REQUIRED` rule removes it with no drop, where 1.14.4
    drops the kelp).
*   Water flowing into a dry coral or conduit by itself (Fluidlogged API's flow; Core's tests cover it).
*   Other mods' fluidlogging configs or `FluidloggableEvent` handlers overriding ours.

## For the integrator

*   **Files outside this stream's own package**: `common/` (removed `BlockPseudoWater`, `PseudoWaterEvents`;
    added `BlockUnderwater`, `RequiredWater`, `LegacyWater`, `WaterSpawning`; a comment in `Content`),
    `client/ClientSetup` (tint removed), one line in `UpdateAquaticBackport.init` (`WaterSpawning.init()`),
    and the kelp, coral, bubble, conduit, dolphin, fish, mobs, trident, turtle and water packages (the water
    checks above), their self-tests, `tools/assets.py` and four generators, `docs/PLAN.md` (B and Wl).
*   **The sea pickle copy is unchanged.** It is the owner's (Village & Pillage Backport) class copied
    verbatim, with its own `waterlogged` property and meta bit 4 and its own "water beside or above" rule;
    Fluidlogged API does not keep water in it (logged by the waterlog test). Moving it needs the owner to
    move first (`Waterloggable` + `Waterlogging.migrateStoredProperty(block, WATERLOGGED)` for its released
    worlds), then `tools/woodset/copy_from_owner.py`; `CoralGenerator.seaPickle` and `CoralPickles.isLit`
    read the property by name and would then read `Waterlogging.hasWater` instead. Its `waterNextTo` also
    reads water *material*, so a pickle whose only neighboring water is kept in kelp goes dark.
*   **Stale stream docs** (other streams' files, not edited here): `kelp.md`, `coral.md`, `bubble.md`,
    `conduit.md` describe the pseudo-water technique; `water.md`'s note on `BlockPseudoWater.WATER_OPACITY`
    is resolved (Fluidlogged API takes the water's opacity).
*   **For Backport Core's owner** (not edited here): (1) the `IN_WATER` spawn predicate is a gap of
    Fluidlogged API that every family mod with water mobs shares; `WaterSpawning` only sets it when nothing
    has, so Core can take it over. (2) A `REQUIRED` block drained by a sponge drops nothing; 1.14.4's sponge
    drops kelp and seagrass. (3) `RequiredWater` (give a `REQUIRED` block a source when it was placed without
    one) may belong in Core too.
*   `tools/lang/vanilla_keys.json` on `main` is older than the merged streams: running
    `tools/lang/vanilla_keys.py` rewrites it (269 vanilla names, 130 ours). Left as it was here. No new lang
    keys in this stream; nothing for `build_langs.py`.

## Without Fluidlogged API (optional since 2026-11-09)

Fluidlogged API is Backport Core's optional dependency, not this mod's. Without it (`Waterlogging.isAvailable()`
false) the blocks above go back to the model before this stream, in `common/PseudoWater`: kelp, seagrass, tall
seagrass and bubble columns are water material with `LEVEL` 0 (always water); coral, fans and the conduit have
`waterlogged` in metadata bit 1 (wall fans: bit 4) and are water material while it is true. The model is picked
per block at construction (`PseudoWater.active()`), and each block reads it back from its own state container
(`isPseudoWater`). The client maps those blocks to `blockstates/pseudo_water/<name>.json` (the same parts plus
the water surface, `tools/common/pseudo_water.py`) and tints the surface. Buckets: `common/PseudoWaterEvents`.
Sea pickles keep their stored `waterlogged` flag as "water beside or above". `LegacyWater` neither converts nor
marks chunks then, so the blocks get Fluidlogged API water once it is installed. With Fluidlogged API every
path is unchanged. Only `common/fluidlogged/FluidloggedAccess` names a Fluidlogged API class.
