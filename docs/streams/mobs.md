# Stream M: mob changes

Package `mobs`. Config `mobs` (`enabled`, `undeadSink`, `babyZombiesBurn`, `squidInk`, `polarBearsOnIce`,
`parrotsImitate`, `skeletonHorsesUnderwater`). No entity ids used; packet 65 (squid ink). Seven sound events,
no blocks or items. Everything is events, plus AI tasks added to or removed from a mob when it joins a world.

## Changelog section

*   **Undead mobs sink in water**: zombies, husks, zombie villagers, zombie pigmen, skeletons, strays, wither
    skeletons, skeleton horses and zombie horses no longer swim to the surface; they sink and walk along the
    bottom. Undead mobs no longer drown.
*   **Baby zombies burn in daylight**, like adult ones (baby husks still do not).
*   **Squid squirt ink**: a hurt squid shoots a cloud of ink and swims away from whatever hurt it.
*   **Polar bears spawn on the ice of frozen oceans.**
*   **Parrots imitate phantoms and drowned.**
*   **Skeleton horses can be ridden under water**: the rider stays on, the horse is fast under water, and it
    has its own underwater sounds.

## How it works

*   **Sinking** (`UndeadInWater`): 1.12.2's zombies, skeletons and horses float because of a swim goal; current
    Java's zombie and skeleton families and its skeleton and zombie horses have none. When one of these (or a
    subclass from another mod) joins a world, its `EntityAISwimming` goals are removed with `removeTask` and
    its ground navigator stops planning along the surface (`setCanSwim(false)`). Only if a swim goal was
    there, so the trident stream's drowned (which has its own swimming) is untouched. Never from inside a goal.
*   **Breathing**: current Java's `canBreatheUnderwater` is true for every undead mob. At the start of the tick
    (`LivingUpdateEvent`, before vanilla takes a breath) an undead mob with its eyes under water has its air
    set full, and drowning damage to undead is cancelled. By creature attribute, so other mods' undead too.
*   **Baby zombies**: 1.12.2's `EntityZombie.onLivingUpdate` skips babies. For a baby whose kind burns
    (`shouldBurnInDay`, read by reflection: false for husks), the adult's exact check runs at the start of its
    tick: daytime, brightness above 0.5, chance `(brightness - 0.4) * 2 / 30`, sky above the eyes, helmet worn
    down instead of burning.
*   **Squid ink** (`SquidInk`, `EntityAISquidFlee`, `PacketSquidInk`, `client/ParticleSquidInk`): 1.12.2 has
    no hook after a hit lands, so `LivingDamageEvent` marks the squid and it squirts at the start of its next
    tick if it then has a revenge target (current Java's condition). The squirt sound is 1.13's
    `entity.squid.squirt` at the squid's volume 0.4 and living pitch; the 30 particles are made by every client
    within 32 blocks from one packet (position, body pitch and yaw), aimed as current Java's `spawnInk`, drawn as
    1.14.4's `SquidInkParticle` on 1.12.2's generic smoke sprites (which 1.14's `generic_0..7` are). The flee
    goal is 1.14.4's `FleeGoal` at priority 1, no mutex, added when the squid joins a world; it keeps 1.14.4's
    discarded `normalize()` so the flee speed matches the game (3 x the offset / 20 when the attacker is within
    5 blocks).
*   **Polar bears** (`PolarBearSpawning`): 1.12.2 spawns nothing on ice (ice is not solid enough for its
    placement check) and its animals need grass. Current Java's rule: in a frozen or deep frozen ocean, only on
    ice, in light above 8; elsewhere as any animal. Here "frozen ocean" is `OceanKind.FROZEN` (1.12.2's frozen
    ocean, the oceans stream's frozen oceans, any snowy ocean). The polar bear gets its own Forge spawn
    placement type (vanilla's on-ground check, or standing on ice); `LivingSpawnEvent.CheckSpawn` answers
    ALLOW/DENY in frozen oceans (spawners and other biomes are left to vanilla), which covers the regular
    spawning cycle; chunk generation, whose on-ground check is written into 1.12.2's
    `performWorldGenSpawning`, gets its polar bear part again at `PopulateChunkEvent.Post` (after the ice has
    frozen; same chance, weighted pick, group walk and populated area as vanilla). At post-init every
    frozen-ocean biome without a polar bear entry gets 1.14.4's (weight 1, groups of 1-2); the oceans stream's
    own biomes already have one, so nothing doubles.
*   **Parrots** (`ParrotImitation`): Forge's `EntityParrot.registerMimicSound`, with the phantom and drowned
    classes found by name (`updateaquaticbackport:phantom`, `updateaquaticbackport:drowned`); a mob that is
    missing or switched off is not added. The sounds are 1.13's own events, `entity.parrot.imitate.phantom` and
    `.drowned`, written exactly as 1.18's sounds.json has them: a reference to
    `updateaquaticbackport:entity.phantom.ambient` at pitch 1.7, volume 0.6, and to
    `updateaquaticbackport:entity.drowned.ambient` at pitch 1.8, volume 0.6. 1.12.2's `SoundHandler`
    multiplies an event reference's pitch and volume in (checked in its source), so this is the phantom's own
    sound at 1.7 x and 0.6 x, with the parrot's subtitles ("Parrot screeches", "Parrot gurgles"). No sound
    files are copied for them; without those streams the references are never played.
*   **Skeleton horses** (`SkeletonHorses`, `client/MobsClient`):
    *   Keeping the rider: 1.12.2 dismounts a rider whose eyes are under water from inside the rider's
        `EntityLivingBase.onEntityUpdate`, by asking the mount, which a vanilla horse cannot answer differently
        without a patch. `EntityMountEvent` is cancelled when the mount is a skeleton horse, the rider is under
        water and the call came straight from that method (read from the stack, MCP and SRG names, only in
        that already rare case), so sneaking, dying and teleporting still get a rider off.
    *   Speed: current Java's skeleton horse keeps 96% of its speed per tick in water, others 80%. At the start
        of the next tick its horizontal motion is scaled by 0.96 / 0.8 (= 1.2), the same as a 0.96 slow-down,
        on the side that works out its movement (the server, or the client of the player steering it).
    *   Sounds: an entity's sounds reach the client as plain positioned sounds, and the server cannot change
        what a vanilla horse plays. The client swaps them on arrival (`PlaySoundEvent`): a skeleton horse
        ambient sound, or the generic swim sound, played where a skeleton horse is and no other non-riding mob
        is closer, becomes `ambient_water` (eyes under water) or 1.14.4's `getSwimSound` choice (step or
        gallop on the bottom, swim off it) at 1.14.4's `playSwimSound` volumes.

## Tested headlessly

`./gradlew runServer -Pselftest=mobs`: **`SELFTEST DONE pass=139 fail=0`**; with every stream on main,
`-Pselftest=all`: **`pass=1007 fail=0`**; then `python tools/check_assets.py`: **74 blocks, 5210 states,
63 item models, 86 sound files, 0 errors**.

*   The seven sound events registered.
*   Sinking: all nine kinds have 1.12.2's swim goal before joining and none after, with a non-swimming
    navigator; in a 9-deep pool all nine are on the floor after 200 ticks (y 0.0 above it) with full health,
    while a pig swims at the surface. A zombie and a skeleton horse saved and loaded again have no swim goal;
    with `undeadSink` off a new zombie keeps it.
*   Breathing: a zombie and a skeleton under water for 400 ticks keep their air and health; a pig held under
    water drowns (control); drowning damage to a zombie is refused.
*   Baby zombies at noon under open sky: a baby catches fire through its update event; a baby husk never; a
    baby in a leather helmet does not and the helmet wears down; an adult is left to vanilla; with the switch
    off, or at night, a baby does not burn.
*   Squid: one flee goal (priority 1, mutex 0) on joining, again after saving and loading. Hurt by nothing it
    can blame: marked, cleared next tick, no sound. Hurt by a player: it blames the player and plays the
    squirt sound exactly once on its next tick; a hit blocked by invulnerability makes none. The flee goal
    with the attacker 2 blocks west sets the swim vector to exactly (0.3, 0, 0); it does not start at 12
    blocks; ticked for real the squid swims away.
*   Polar bears: their own placement type; the frozen ocean lists them once (1, 1-2), ice plains still once,
    plain ocean not at all. In frozen-ocean chunks: placement on ice allowed (and still refused for other
    animals), on grass allowed; spawn check ALLOW on ice, DENY on grass, DENY inside a block, DEFAULT from a
    spawner and outside frozen oceans. Posting the populated-chunk event over an ice field spawned **6 polar
    bears in 38 chunks**, all on ice inside the populated area, none on the grass strip; over open water none.
*   Parrots: phantoms and drowned are not in this branch, so nothing is added and nothing imitates them
    (the test checks Forge's mimic map either way, so after the merge it checks that both are added with
    the right events); vanilla's imitations untouched; an unknown name or a vanilla name finds nothing.
*   Skeleton horses: a skeleton rider stays on a skeleton horse for 40 ticks with its eyes under water, and
    is thrown off a normal horse (control); a real server player stays on through its own entity tick, gets
    off by sneaking, and is thrown off a normal horse (control); getting off on purpose works under water.
    Horizontal motion in water is scaled by exactly 1.2 for a skeleton horse, not for a zombie horse, not out
    of water; an unridden skeleton horse sinks to the floor.
*   The dedicated server boots, so no client class is reached from server code.

## Not tested

*   Anything on a client: the ink particles (look, fade, drift), the squirt and parrot sounds playing, the
    skeleton horse sound swap (`client/MobsClient`), and a player steering a skeleton horse under water (the
    client-side 1.2 scale) and how fast that feels.
*   The polar bears' regular spawning cycle end to end (the placement and the spawn check it uses are tested
    one by one), and chunk generation in a real frozen ocean: vanilla 1.12.2 generates none, so the populated
    event was posted over a prepared chunk.
*   Parrots imitating, with phantoms and drowned present (needs `ua-phantom` and `ua-trident` merged; the test
    then checks the mimic map).
*   Another mod's undead or zombie subclass that adds its own swim goal later than joining.

## Limits

*   The skeleton horse sound swap matches by position. For a horse someone else rides fast, the drawn horse
    can lag more than 1.5 blocks behind the server's, and that sound is then the plain one. The gallop counter
    is the client's own (reset when the horse has no rider), so the step-or-gallop rhythm can differ slightly
    from 1.14.4's, which shares it with land steps.
*   The jump sound under water (`jump_water`) is not swapped: 1.12.2 plays the horse jump sound only on the
    steering player's client, where it is never heard.
*   1.14.4 checks a spawner's spawn against the polar bear rule too; 1.12.2's spawners keep their own check.

## For the integrator

*   `UpdateAquaticBackport.postInit`: one line, `MobsContent.postInit()` (parrot imitations, the polar bear
    placement type and spawn entries; after every mod's entities and biomes).
*   `NetworkHandler.init`: packet 65 (`PacketSquidInk`, to clients).
*   `selftest/SelfTestRunner.TESTS`: one line, `new MobsSelfTest()`.
*   `ModConfig.Mobs`: six new fields, each with `@Config.LangKey`, its name and `.tooltip` line in the
    `# --- mobs ---` block of `en_us.lang` (from `tools/mobs/generate.py`).
*   `sounds.json`: seven events from the generator. `entity.parrot.imitate.phantom` and `.drowned` point at
    `updateaquaticbackport:entity.phantom.ambient` (on `ua-phantom`) and
    `updateaquaticbackport:entity.drowned.ambient` (on `ua-trident`); both names were checked on those branches.
    The skeleton horse files (`sounds/mob/horse/skeleton/water/*`, `sounds/liquid/swim9-17`) and
    `sounds/entity/squid/*` are copied by `assets.copy_sound`; another stream copying the same vanilla file
    writes the same bytes to the same path, so such a merge is clean.
*   Languages: `python tools/lang/vanilla_keys.py` lists all four new subtitles as vanilla names and the twelve
    config lines as ours. This stream names no entity, effect or potion, so main's script flags nothing falsely.
    The regenerated `vanilla_keys.json` is not committed; rerun it after the merge. The other 17 language
    files are not written here (the family's language tool is on `ua-languages`).
*   No `common/` change needed.
