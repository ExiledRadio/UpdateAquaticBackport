# Stream P: phantoms

Package `phantom` (client side in `phantom/client`). Config `phantoms` (`enabled`, `insomnia`). Entity id 20 of
20-24; no packets (30-34 unused).

## Changelog section

*   **Phantoms**: undead flying mobs that come for players who have not slept. Go three in-game days without
    getting into a bed and, at night or in a thunderstorm, phantoms may appear high above you whenever you stand
    under open sky at or above sea level: more of them on harder difficulties, more often the longer you stay
    awake. They circle overhead and swoop down to bite every 8 to 12 seconds, burn in daylight, and keep away
    from cats.
*   Getting into a bed (you need not sleep through the night) or dying resets the count. It shows as **Time
    Since Last Rest** under General statistics and works as a scoreboard criterion (`stat.timeSinceRest`).
*   The **doInsomnia** game rule (added to every world, on by default) stops them spawning in one world; the
    `insomnia` config option stops them everywhere.
*   **Phantom membrane**: a phantom killed by a player drops up to one, more with Looting. It repairs the
    **elytra** in an anvil, which leather no longer does.
*   **Slow Falling**: fall gently and take no fall damage. Brew an awkward potion with phantom membrane
    (1:30), add redstone to make it last 4:00, and gunpowder and dragon's breath for splash and lingering
    potions; lingering potions make **arrows of Slow Falling**.
*   A phantom spawn egg; `/summon updateaquaticbackport:phantom ~ ~ ~ {Size:10}` makes larger ones (sizes 0-64,
    each step wider and harder-hitting).

## How it works

*   **The mob** is 1.14.4's `PhantomEntity` goal for goal: a circle goal around an anchor, an attack picker that
    lifts the anchor 20-40 blocks over the target and switches to a swoop every 8-12 s, a swoop that bites on
    contact and breaks off after a bite, a wall, being hurt or a cat, and a target goal that every 3 s takes
    the highest suitable player within 64 blocks. Its move helper steers straight at one point, turning at
    most 4 degrees a tick. Current Java's guard against a zero-length steer is added. Numbers: 20 health, 6 +
    size attack damage, 0.9 x 0.5 hitbox widened by 0.2 a step, 5 experience, tracked 80 blocks every 3 ticks.
*   **1.12.2 plumbing**: the look helper zeroes the pitch after the move helper sets it and is a private final
    field, so the move helper's pitch is put back in `travel` (server side only) instead of replacing the
    helper by reflection. 1.12.2 asks a goal that is not running only every third tick, so the target goal
    counts its 20/60-tick delay in game ticks. Level event 1039 (the bite) does not exist here, so the bite
    sound is played directly at the same volume and pitch.
*   **Cats**: without Village & Pillage, tamed ocelots are 1.12.2's cats; they hiss and the swoop ends. With it
    installed, its `villagerbackport:cat` entities count instead (it makes them hiss itself, and finds this
    phantom by its name `phantom`). It also finds `phantom_membrane` by name for cat gifts.
*   **Time since rest** is a real statistic (`StatBasic`, time-formatted), so the statistics file keeps it per
    player. It counts up every tick out of bed on the player tick, is held at zero in bed, and is taken away on
    death (`LivingDeathEvent`, lowest priority, so a cancelled death keeps it). If another mod has already
    registered `stat.timeSinceRest`, that statistic is used.
*   **The spawner** is 1.14.4's `PhantomSpawner` on the overworld's world tick, with current Java's game rule
    check first; Forge's `CheckSpawn` and special-spawn events fire for every phantom, so spawn-control mods see
    them.
*   **Slow Falling** without patching `travel`: in the air vanilla moves and then pulls down, so the pull is
    corrected at the start of the next living tick (a fall is recognised by the motion vanilla's own pull
    leaves); an elytra glide pulls before moving, so its difference is added before. The result is 1.14's motion
    exactly (the test checks the height against 1.14's formula to 0.001). Water, lava, levitation, creative
    flight and no-gravity entities are untouched, as they never used the constant 1.14 changes. The icon is
    drawn from 1.18's texture by `phantom/client/PhantomClient`, through Potion methods stripped on a server.
*   **Elytra repair** answers Forge's anvil event with vanilla's own repair arithmetic for membrane, and cancels
    leather. A cancelled anvil event leaves the previous result in the output slot, so leather swapped straight
    in for membrane could have taken the membrane repair; a refusal also clears the output of the anvil holding
    those exact stacks (the server's, through the player list; the client's, through `PhantomClient`). One
    difference: the event does not say who is at the anvil, so a repair of 40 levels or more is refused for
    creative players too.
*   **Assets** (`tools/phantom/generate.py`): textures for the phantom, its eyes, the membrane and the effect
    icon from the 1.18.2 jar; six sound events from the 1.18 index; the loot table in 1.12.2's format; all
    names through `assets.name(...)`.

## Tested headlessly

`./gradlew runServer -Pselftest=phantom`, then `python tools/check_assets.py`: **`SELFTEST DONE pass=141 fail=0`**
(and **`pass=1009 fail=0`** with `-Pselftest=all`), **74 blocks, 5210 states, 64 item models, 83 sound files, 0
errors**.

*   Registration: entity `updateaquaticbackport:phantom` with its egg, the loot table, the membrane, the effect
    (beneficial, 1.14's color), both potion types with 1800 and 4800 ticks, six sounds, the statistic and its
    scoreboard criterion; every translation key the content asks for is in `en_us.lang`.
*   The mob: size 0 on spawn, 0.9 x 0.5, eye height, 6 damage, 20 health, undead (Instant Health hurts it),
    counts as a monster, anchor 5 above its spawn; size 5 gives a 1.9-wide box and 11 damage; size is clamped
    to 0-64; removed in Peaceful.
*   Saving: written with its id, `Size` and anchor, read back as a phantom with the same size, box, damage and
    anchor; created by name as `/summon` does.
*   Flight, 400 ticks at night: stays airborne, circles within its radius, turns through every heading, pitches
    (so the look-helper fix works), no target with no one near.
*   Burns in daylight under open sky, not at night.
*   A real server player: targeted, swooped at and bitten; the swoop ends after the bite; a creative or
    spectating player is ignored.
*   A tamed ocelot spawned beside a swooping phantom ends the swoop; a wild ocelot does not.
*   Drops over 200 kills each: about 0.5 membrane per player kill, about 2 with Looting III, none without a
    player.
*   Time since rest: one per tick awake; getting into a real bed at night clears it and holds it at zero;
    counts again after waking; dying clears it; the `doInsomnia` rule exists and is on.
*   The spawner: never for a rested player; often for one awake a week, in groups of at most difficulty + 1,
    20-34 above and at most 10 to the side, size 0; none under a roof, below sea level, by day, over a
    spectator, or with `doInsomnia` false; the world tick spawns at night.
*   Slow Falling: a pig's fall matches 1.14's formula, a plain pig falls much faster, no fall distance builds
    up (at most the current tick's drop, as in 1.14), and a 50-block landing does no damage where it hurts a
    plain pig.
*   Brewing: awkward + membrane gives slow falling, redstone the long form, for potion, splash and lingering;
    gunpowder and dragon's breath convert; water + membrane does nothing; 8 tipped arrows of Slow Falling.
*   Elytra: one membrane mends a quarter for one level, ten mend it fully using three, a rename adds a level,
    the prior-work penalty grows, leather is refused, an undamaged elytra gives nothing, other items are left
    to vanilla; the same through a real anvil container, where leather swapped straight in for the membrane
    leaves the output empty and costs nothing.
*   The server booting proves no client type is reached from server code. The test ticks entities itself
    (age, then `onUpdate`), because the world's own update insists on 32 loaded blocks round the entity.

## Not tested

*   Anything visual (no client): the model, its wing beat, the size scaling and pitch in the renderer, the
    glowing eyes, the mycelium trail and flap sounds (client-side), the Slow Falling icon in the inventory and
    HUD, the statistic's line in the statistics screen.
*   Sounds playing.
*   Slow Falling while gliding with an elytra (only players glide; the headless player is not moved by a
    client), and on the client player itself.
*   Village & Pillage's cats (its jar is not in the dev environment).
*   Natural spawns over many real nights with a real connected player.

## Languages

Every string is in `en_us.lang`, written by the generator. Not generated for the other 17 languages here: the
family's language tool is on the `ua-languages` branch, to be run after merge. Its structural rules already map
`entity.updateaquaticbackport.phantom.name`, `effect.updateaquaticbackport.slow_falling`, the subtitles and
`item.updateaquaticbackport.phantom_membrane.name`. These need lines in `tools/lang/vanilla_map.txt` (or a
rule) to take Mojang's names:

    potion.effect.updateaquaticbackport.slow_falling=item.minecraft.potion.effect.slow_falling
    splash_potion.effect.updateaquaticbackport.slow_falling=item.minecraft.splash_potion.effect.slow_falling
    lingering_potion.effect.updateaquaticbackport.slow_falling=item.minecraft.lingering_potion.effect.slow_falling
    tipped_arrow.effect.updateaquaticbackport.slow_falling=item.minecraft.tipped_arrow.effect.slow_falling
    stat.timeSinceRest=stat.minecraft.time_since_rest

The same pattern (`potion.effect.updateaquaticbackport.<base>` and friends) will come from the turtle stream's
Turtle Master, so a rule may be better than lines.

## For the integrator

*   Nothing in `common/` needs changing.
*   Shared files touched: `ModConfig.Phantoms` (one new field, `insomnia`), `SelfTestRunner.TESTS` (one line),
    `en_us.lang` and `sounds.json` (the generator's `phantom` blocks only).
*   `UpdateAquaticBackport.java` is untouched: registration is from `@Mod.EventBusSubscriber` classes, and the
    renderer is registered from a client-only subscriber in `ModelRegistryEvent`.
*   Stream M (parrots imitating phantoms) can play `updateaquaticbackport:entity.phantom.ambient` at pitch 1.7,
    volume 0.6 (1.18's `entity.parrot.imitate.phantom`), looked up by name.
*   `tools/lang/vanilla_keys.py` only knows `tile.`, `item.` and `subtitles.` keys, so it lists the entity,
    effect, potion and statistic names as ours; the language tool's own rules (and the lines above) are what
    count. `tools/lang/vanilla_keys.json` was not regenerated in this branch, to keep the merge clean.
