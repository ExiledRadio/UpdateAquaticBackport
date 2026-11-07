# Follow-ups to streams X and O: pumpkins, endermen, wood, old ocean chunks

Branch `ua-followups`, from `ua-integration` (b5c4090). Packages `blocks`, `asm` (new) and `ocean`, plus
`common/OceanKind`. No new blocks, items, entities, packets, config options or lang lines.

## Changelog section

*   **Endermen** are calmed only by a **carved pumpkin** on your head; a plain pumpkin no longer protects
    you. As in 1.14, an enderman riding something does not teleport at you, and one that is already angry
    stays angry if you put a carved pumpkin on afterwards. Endermen can pick up carved pumpkins.
*   **The plain pumpkin can no longer be worn**: not in the helmet slot, not by shift-clicking, not by
    armor stands, dispensers or mobs. Wear a carved pumpkin instead.
*   **A plain pumpkin never builds a golem**, however it is placed (by a player, a machine from another
    mod, a structure or a command). A carved pumpkin or a jack o'lantern still does.
*   **Middle-clicking wood** (the bark block) picks the wood item, not the log.
*   **Oceans in chunks from before the update**: in a world that already existed before the new ocean
    biomes were switched on, the old ocean chunks still get warm and cold water (coral, fish, kelp) from
    the temperature noise, as they did before; newly generated chunks use the real warm, lukewarm, cold
    and frozen ocean biomes. Worlds and world types that do not get the new biomes (flat worlds, other
    dimensions, world types that build their own biome map) keep the noise everywhere.

## How it works

*   **Endermen** (`blocks/EndermanStare`): 1.12.2's stare check is private, names `Blocks.PUMPKIN`, and is
    called only by the enderman's private target task `EntityEnderman$AIFindPlayer`. That task is replaced
    on `EntityJoinWorldEvent` (server side, any `EntityEnderman`, subclasses included) by `FindPlayer`, a
    port of 1.14.4's `FindPlayerGoal` with 1.14.4's `shouldAttackPlayer` (carved pumpkin item on the head
    calms; otherwise the look-vector dot product `> 1 - 0.025 / distance` and line of sight). An entity
    joining a world is not running its tasks, so its list is safe to edit there (never from a goal); the
    target list is rebuilt in its own order with the one task swapped, so priorities 1/2/3 and the
    iteration order stay vanilla's. Teleporting calls the enderman's own protected `teleportRandomly` /
    `teleportToEntity` (reflection, SRG `func_70820_n` / `func_70816_c`), so a subclass's overrides are
    still used. A task another mod put in place of vanilla's is left alone. No patch: the replacement is
    exact, because the private check has no other caller.
*   **Carried carved pumpkins**: `EntityEnderman.setCarriable(carvedPumpkin, true)`, from 1.14.4's
    `enderman_holdable` tag.
*   **Three patches** (`asm`, documented in `docs/PATCHES.md`): P1 `EntityLiving.getSlotForItemStack` asks
    `BlocksHooks.wornPumpkin()` instead of reading `Blocks.PUMPKIN`; P2 the two pattern matches in
    `BlockPumpkin.trySpawnGolem` go through `BlocksHooks.golemMatch`, which never matches for the plain
    pumpkin; P3 `BlockLog` gains a `getPickBlock` override that passes vanilla's stack through
    `BlocksHooks.pickBlock`. With P2, `PlacementRules` no longer takes over a supported plain pumpkin's
    placement just to skip `onBlockAdded` (vanilla's item places it now), and `Placement.place` lost its
    `runOnAdded` parameter.
*   **Old ocean chunks** (`ocean/OceanChunks`), as vanilla tells old chunks from new by their own saved
    data (its data version): a chunk saved while its biomes have 1.13's temperatures gets
    `updateaquaticbackport:OceanTemperatures: 1b` at the root of its NBT, beside `DataVersion`.
    `OceanKind.of(world, biome, x, z)` / `OceanKind.at` call vanilla's ocean and deep ocean `NORMAL` only
    when that column's chunk has real temperatures:
    *   loaded from disk: only if its data had the flag (a chunk made before the stream was on never gets
        it, however often it is saved again);
    *   generated now, or not loaded at all: if the world's biome provider has `OceanLayers.Mix` in its
        layer chain (`BiomeProvider.genBiomes`, SRG `field_76944_d`, walked down `GenLayer.parent`,
        `field_75909_a`, in case another mod wrapped it); flat and debug worlds, single-biome providers
        and world types that build their own map have none;
    *   on a client (which never sees saved data), only the provider counts.
    Everywhere else vanilla oceans take the noise, as without the stream. `OceanKind.of(biome, seed, x, z)`
    (no world) now always gives the noise for vanilla oceans; `FishSpawning` uses the world form. The
    stream no longer registers vanilla's two oceans as `NORMAL`; it installs the rule through
    `OceanKind.useRealTemperatures`. Chunk lookups are `getLoadedChunk` (what `World.getBiome` already
    does) and a weak map from loaded chunks to their flag.

## Tested headlessly

**`./gradlew runServer -Pselftest=all`, fresh world (`followups-all`, port 25749): `SELFTEST DONE
pass=14417 fail=0`**, then `python tools/check_assets.py`: **88 blocks, 5489 states, 90 item models, 380
sound files, 0 errors**. All three patches log `applied` once each. `./gradlew build` afterwards: the
release jar carries `FMLCorePlugin` and `FMLCorePluginContainsFMLMod` in its manifest, is reobfuscated
(SRG names in the new classes) and has no self-test classes.

*   **Endermen** (`PumpkinFollowupsTest`, in `blocks`), with real server players added to the world's
    player list, each placed 8 blocks away looking straight into the enderman's eyes: bare head, plain
    pumpkin, jack o'lantern, skeleton skull and iron helmet anger it, a carved pumpkin does not, both by
    the stare check and through the task itself (`shouldExecute`, then five `updateTask` ticks make the
    player its attack target); a player looking away does not anger it; a player who angered it and then
    puts on a carved pumpkin stays its target. The enderman's target list is the replacement at priority
    1, hurt-by-target at 2, the endermite target at 3, and no vanilla `AIFindPlayer`; an enderman read
    back from its NBT (as on chunk load) gets the replacement too. Endermen can carry a carved pumpkin and
    still a pumpkin.
*   **Wearing** (P1): see `docs/PATCHES.md` (slots, helmet slot, shift-click, armor stand).
*   **Golems** (P2): plain pumpkins set by `setBlockState` on snow and on the iron T build nothing and
    leave every block; a jack o'lantern and a carved pumpkin set the same way still build; stream X's
    golem tests (player placement, dispenser, carved pumpkin, jack o'lantern) all still pass.
*   **Pick block** (P3): all six bark logs pick their wood item, upright logs their log with the right
    metadata, stone picks stone.
*   **Ocean kinds in one world** (`ocean`): the overworld's provider is layered; a new flat world's and a
    single-biome provider are not; a new default provider is. A vanilla-ocean chunk generated now is real
    and its vanilla ocean `NORMAL`; its save writes the flag; handed a load without the flag it becomes
    old, every vanilla ocean column takes the noise, and saving it writes no flag; handed a load with the
    flag it is real again. An unloaded column of vanilla ocean is `NORMAL`. 400 seed-only lookups of
    vanilla ocean and deep ocean equal the noise.
*   **A world made without the stream, then with it** (`-Pselftest=oceanlegacy`, three runs of world
    `legacy`, never part of `all`):
    1.  `oceans.enabled=false`, fresh world: a 5 x 5-chunk stretch of vanilla ocean the noise calls warm and
        one it calls cold, 50 chunks, every vanilla ocean column given the noise; positions written down.
    2.  `oceans.enabled=true`, same world: **pass=251 fail=0** (with `blocks`). All 50 old chunks loaded from
        disk without the flag, still only 1.12 biomes; **12800 vanilla ocean columns, every one the noise:
        WARM 5250, LUKEWARM 1128, NORMAL 704, COLD 5718**. 75 new chunks generated 20000+ blocks east,
        all real: **6400 vanilla ocean columns all `NORMAL`, 6400 `warm_ocean` columns all WARM, 6400
        `cold_ocean`/`deep_cold_ocean` columns all COLD**.
    3.  Same again: **pass=10 fail=0**. The old chunks are still old after being saved by run 2 (same 12800
        columns, same kinds); all 75 new chunks were read back with the flag and their vanilla ocean is
        still `NORMAL`.
*   The dedicated server booting with the patches and the new classes shows no client type is reached
    from server code.

## Not tested

*   **A real (reobfuscated) instance**: the patches' SRG names (`func_184640_d`, `field_150423_aK`,
    `func_180673_e`, `func_177681_a`) and the reflection SRG names are taken from the MCP stable_39
    mappings and the release jar was inspected, but no production client or server was started.
*   **The client**: the actual middle click (`ForgeHooks.onPickBlock`, creative and survival), the helmet
    slot refusing a plain pumpkin in the GUI, how a plain pumpkin held in the hand looks.
*   **Endermen over time**: the replaced task under real ticking (teleporting away when stared at from
    close, teleporting to a far target, the riding rule), and against other mods that edit enderman AI.
*   Old-chunk behavior in other dimensions and world types in a real game (only checked through their
    biome providers), and a world that switches the oceans stream off and on again (its chunks saved
    while off lose the flag and count as old afterwards).
*   Mobs picking up a plain pumpkin item (they take it in the hand through the same patched method; not
    run).

## For the integrator

*   This is the mod's first coremod: `build.gradle` names `asm.UpdateAquaticPlugin` in the jar's and the
    dev jar's manifest and passes `fml.coreMods.load` to the run tasks. `docs/PATCHES.md` is new.
*   `common/OceanKind`: new `of(World, Biome, int, int)` and `useRealTemperatures`; `of(Biome, long, int,
    int)` no longer gives `NORMAL` for vanilla oceans. Other streams calling `OceanKind.at` need no change.
*   `SelfTest` got a default `inAll()`; `SelfTestRunner` skips tests that return false for `all`
    (`oceanlegacy` only).
*   `docs/streams/blocks.md` ("Not done" replaced) and `docs/streams/ocean.md` ("For the integrator")
    point here.
