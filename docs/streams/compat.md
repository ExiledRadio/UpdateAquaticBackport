# Compat: Update Aquatic with Village & Pillage 2.4.0 and Buzzy Bees 1.0.0

Branch `ua-compat`, from `main` (1378df4), on Backport Core main 315467c (1.1.0, with its coremod). `./gradlew runServer -Pselftest=all`, fresh world each time, with the
other mod's `-dev` jar in `eclipse/mods/`. Requests for the other two mods are in the shared notes folder,
`update-aquatic-compat-requests.md`.

## Results

| Run | Before | After |
|---|---|---|
| Alone | 15981 / 0 | 16023 / 0 |
| With V&P 2.4.0 | 15390 / 3 | 15434 / 0 |
| With Buzzy Bees 1.0.0 | 15979 / 1 | 16021 / 0 |
| With Buzzy Bees 1.0.0, its coremod loaded | - | 16020 / 0 |

Buzzy Bees' `-dev` jar has no `FMLCorePlugin` manifest, so its patches P1-P5 do not load from `eclipse/mods`.
The last run used a copy of that jar with the manifest added by hand, so its patches ran beside ours.

## Game code

*   **Phantoms and Village & Pillage's cats** (`phantom/FamilyCats`, `phantom/EntityPhantom`). The swoop's cat
    check (1.14.4: while swooping, every 20 ticks of age, any cat within 16 blocks ends the swoop and each cat
    hisses) now counts tamed ocelots *and* `villagerbackport:cat` together. Before, with V&P installed it
    looked only for V&P's cats, so a tamed ocelot V&P had not yet turned into a cat (that happens at the end of
    the tick, or never with `convertTamedOcelots` off) was ignored. Every cat now hisses as 1.14.4's does: V&P's
    through its own `hiss()` (found by name), a tamed ocelot with `entity.cat.hiss` at the ocelot's volume 0.4
    and the usual pitch (it was volume 1, pitch 1).
*   **V&P's generic phantom fear kept off our phantom.** V&P's `CatFear` acts on any mob registered as
    `phantom`: every 10 ticks, with a target and a cat near, it drops the target, pushes the phantom up and makes
    the cats hiss. On ours that meant breaking off while circling too, rising, and two hisses. On exactly the
    ticks it would act (V&P loaded, its `cats.phantomsFearCats` on, age a multiple of 10, a target, a V&P cat
    within 16 blocks) our phantom's `getAttackTarget()` reads null from the highest-priority to the lowest-
    priority `LivingUpdateEvent` handler; its own goals run after the event and see the target. Other mods'
    update handlers see no target on those ticks only. V&P has been asked to skip `updateaquaticbackport:phantom`
    itself; then the shield never triggers.
*   **Sea pickles on coral with V&P: no change needed.** `coral/CoralPickles` and `coral/CoralGenerator` already
    use whichever pickle the game has (`Owners.block`, properties by name), and spread V&P 2.4.0's pickle
    correctly in a run before the ownership move below. The coral NPE was the test's.

## Test code

*   **Coral** (`CoralSelfTest`): section 7 took `WoodsetContent.seaPickle()`, null with V&P installed (the
    woodset stream gives way to V&P 2.4.0, which registers a pickle too): the NPE, and the rest of the coral
    test (reef shapes, generation) never ran. Sea pickles and blue ice now belong to Update Aquatic (Backport
    Core 315467c); turning our copies into owned blocks is separate work. So the test uses our own pickle
    (properties by name, every spread pickle checked lit) and, only when ours is missing because V&P ships one,
    skips the pickle checks instead of crashing; reefs and generation still run.
*   **Phantom** (`PhantomSelfTest.cats`): the tamed ocelot it spawned was queued by V&P for conversion at the
    end of the tick, which never came inside the test, and the old phantom ignored ocelots with V&P installed,
    so it bit. "Ended the swoop" also passed after a bite (the swoop ends after any bite). Now runs once with a
    tamed ocelot and, with V&P, once with `villagerbackport:cat`, and checks the swoop ends while swooping on a
    20th tick of age, before any bite, with exactly one hiss, back to circling. A wild ocelot still never stops it.
*   **Parrots** (`MobsSelfTest.parrots`): checked `size() >= 27`. Buzzy Bees removes the enderman, polar bear,
    wolf and zombie pigman imitations, as 1.15 did: legitimate, and it touches neither of ours (phantom,
    drowned). The test now checks each of vanilla's 28 entries with its own sound, accepting only those four
    missing, and only when Buzzy Bees is loaded.

## Collisions looked for

*   **Blocks and items** shared with V&P (29: stripped logs and wood, blue ice, sea pickle, five woods'
    trapdoors, buttons and plates): ours not registered with V&P 2.4.0 installed, as this branch's code still
    has it; recipe narrowing and axe stripping are left to V&P (log line "come from their owner"). Blue ice and
    sea pickles have since moved to Update Aquatic in Core's table; V&P 2.4.0 still registers them, and with
    Core 315467c that mix caused no failure (structure palettes fall through to V&P's blocks). Nothing shared
    with Buzzy Bees.
*   **Recipes**: none of ours outputs a vanilla item; V&P's (stick from bamboo) and Buzzy Bees' (sugar from honey)
    do not overlap ours.
*   **Sounds**: all namespaced, no clash. Buzzy Bees registers its own `block.coral_block.*` (honeycomb block)
    alongside ours: a duplicate, not a conflict; asked to use ours when we are installed.
*   **Dispenser behaviors**: Buzzy Bees wraps glass bottles, armor stands and fire charges; ours are carved
    pumpkins, pumpkins, shulker boxes and fish buckets. No overlap.
*   **Logs**: no warnings or errors in any run beyond the ones the alone run has, other than Buzzy Bees' own
    "Patch P4 is not applied" when its coremod is not loaded.
