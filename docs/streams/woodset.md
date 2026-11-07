# Stream W: shared 1.13 blocks (copies)

Package `woodset`. Config `woodset`. Built in the first session.

## Changelog section

*   **Stripped logs and stripped wood** in all six woods, **spruce, birch, jungle, acacia and dark oak
    trapdoors, buttons and pressure plates**, **blue ice** and **sea pickles**, with axe stripping.
    Vanilla's trapdoor, wooden button and wooden pressure plate take oak planks only, as in 1.13, and are
    named Oak Trapdoor and Oak Pressure Plate.
*   If another installed mod already provides these blocks, this mod adds none of its own and uses that
    mod's everywhere.

## How it works

*   The block classes (`Wood`, `BlockStrippedLog`, `BlockWoodRedstone`, `BlockBlueIce`, `BlockSeaPickle`)
    and every blockstate, model, texture, sound and name are copied verbatim from the owner by
    `tools/woodset/copy_from_owner.py`. Only `WoodsetContent` and `AxeStripping` are this mod's own.
*   `WoodsetContent.enabled()` is `woodset.enabled` and the owner absent. With the owner installed nothing
    registers, axes are left to the owner, and `Owners.block(...)` returns the owner's blocks.
*   A world saved with the owner and opened without it: `remapOwner` renames `villagerbackport:<name>` to
    ours in Forge's missing-mappings event.

## Tested headlessly

`./gradlew runServer -Pselftest=woodset`, then `python tools/check_assets.py`:

*   **Owner absent: `SELFTEST DONE pass=605 fail=0`.** 29 blocks with items in the creative tab; every state
    survives a metadata round trip; crafting (planks from stripped logs and wood, 4 stripped logs to 3
    stripped wood, each wood's trapdoor, button and plate from its own planks, oak planks still making
    vanilla's three, vanilla's recipes kept under their names, blue ice); charcoal, fuel times, flammability,
    `logWood`; stripping all 24 vanilla log states through the server's real right-click path (axis kept,
    bark to stripped wood, axe damage 1, a second click does nothing), a stick does nothing,
    `axeStripping=false` does nothing; logs placed with an axis, wood, sustain leaves, rotate; trapdoors open
    by hand and redstone, buttons power and schedule release, plates pressed by an item, all beside vanilla's
    oak; blue ice slipperiness, no drops, Silk Touch, no melting, pickaxe; sea pickles placed dry and dark,
    stacking to 4, lit by water beside, light by count, popping 4 items without support; the owner's
    missing mapping remapped onto ours and the owner's other blocks left alone.
*   **Owner installed** (its 2.3.0 dev jar in `eclipse/mods`, fresh world): **`pass=14 fail=0`**. Nothing
    registered, lookups give the owner's blocks, an axe strips an oak log into the owner's stripped log,
    and the axe takes 1 damage (not 2).
*   **Assets: 29 blocks, 207 states, 29 item models, 8 sound files, 0 errors.**

## Found, not fixed here

*   **The owner does not remap our copies yet.** A world with this mod's copies, opened with the owner
    added, stops at FML's missing-blocks prompt (seen on the dev server). Rule 1 says the owner renames
    `updateaquaticbackport:<name>` onto its own blocks in its missing-mappings handler; Village & Pillage
    2.3.0 has no such handler. Its maintainer (or Backport Core's migration code) needs to add it for the 29
    names in `WoodsetContent`.

## Not tested

*   Anything visual (no client): models, textures, the strip sound.
*   Future MC duplicate hiding for these copies: not wired; it belongs in Backport Core's Future MC table
    (`WoodsetContent.futureMcPairs()` is ready for it).
