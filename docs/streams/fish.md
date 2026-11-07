# Stream F: fish

Package `fish` (client code in `fish/client`). Config `fish` (`enabled`, `spawn`). Entity ids 1-4 of the
stream's 1-9; no packets (packet ids 15-19 stay free).

## Changelog section

*   **Cod**, **salmon**, **pufferfish** and **tropical fish** swim in the oceans. They wander, flee from
    players, panic when hurt, and flop about and suffocate out of water. Cod, salmon and tropical fish
    swim in schools that follow a leader.
*   Where they live: cod in oceans, cold and lukewarm oceans; salmon in cold and frozen oceans and in
    rivers; pufferfish and tropical fish in warm and lukewarm oceans. They spawn in groups, close under
    the sea surface, with a mob cap of their own, so squid are not crowded out.
*   **Pufferfish** puff up in two steps when a player or most other mobs come close, sting whatever
    touches them with Poison, and deflate when left alone.
*   **Tropical fish** come in 2,700 looks: 2 shapes, 6 patterns and 15 colors for body and pattern. Most
    are one of 22 named common fish such as the Clownfish or the Moorish Idol; a school shares one look.
*   **Buckets of fish**: use a water bucket on a fish to catch it. Emptying the bucket places water and
    lets the fish out with its name, health and look; it never despawns. A bucket of tropical fish names
    its fish. Dispensers empty fish buckets.
*   Killed fish drop raw fish (cooked if burning) and sometimes bone meal. Raw Fish is now **Raw Cod**,
    Cooked Fish **Cooked Cod**, and Clownfish **Tropical Fish**.
*   Eating a pufferfish gives Poison II for a minute, Hunger III and Nausea I for 15 seconds.
*   New advancements: **Fishy Business** (catch a fish with a rod) and **Tactical Fishing** (catch one with a
    bucket).
*   Spawn eggs for all four fish. With **Natural Spawning** off, fish come only from eggs, spawners,
    commands and buckets.

## How it works

*   **Water mobs that are creatures**: 1.12.2's `EntityWaterMob` is not an `EntityCreature`, and every AI
    task fish need (wander, avoid, panic) takes one; 1.13 moved water mobs under `CreatureEntity` for the
    same reason. `EntityAbstractFish` extends `EntityCreature` and copies in what `EntityWaterMob` does
    (air out of water, `isNotColliding`, talk interval, experience, not pushed by water), with
    `PathNavigateSwimmer`, 1.13's movement controller and 1.13's `travel`.
*   **Water ambient**: current Java's fish category, which 1.12.2 lacks, is a creature type added with
    Forge's `EnumHelper.addCreatureType` (`UPDATEAQUATICBACKPORT_WATER_AMBIENT`, cap 20, peaceful, every
    tick). 1.12.2's spawner walks `EnumCreatureType.values()`, so it spawns it like any other. Fish count
    only toward it; a bucket fish or a named fish does not count (current Java leaves persistent mobs out).
    Despawning is current Java's water ambient one: at once beyond 64 blocks, at random beyond 32.
*   **Spawn lists by `OceanKind`**: the water ambient list is filled per position in
    `WorldEvent.PotentialSpawns` from `common/OceanKind` (plus the biome dictionary's `RIVER` for salmon),
    so every ocean, ours, vanilla's or another mod's, gets current Java's weights. Static entry objects:
    the spawner re-checks a picked entry by identity. Spawn height is current Java's: 13 blocks under sea
    level up to sea level, water below and above.
*   **Groups**: 1.12.2's spawner ignores group sizes and gives each pack of 1-4 fresh spawn data (mixed
    tropical schools). So the spawner spawns one fish per round (`getMaxSpawnedInChunk() = 1`); a fish the
    natural spawner checks (`LivingSpawnEvent.CheckSpawn`, not from a spawner) brings the rest of its group
    in `onInitialSpawn`: current Java's group size, each member a random step of up to 5 blocks across from
    the last, every one checked as a natural spawn, all sharing the first one's spawn data.
    `onInitialSpawn` is final in the base class so the group is made only after a tropical fish has picked
    its look; kinds override `prepareSpawn`. A tropical fish of a random look spawns alone (1.13's
    `isMaxGroupSizeReached`). Natural spawns more than 64 blocks from every player are refused: 1.12.2
    spawns up to 128 blocks out, where a fish would despawn at once, and current Java never spawns a mob
    beyond its despawn distance.
*   **Schooling**: 1.13's `FollowSchoolLeaderGoal`, its 200-219 tick search wait counted in world ticks
    (1.13 checks goals every tick, 1.12.2 every third). Its bookkeeping in `shouldExecute` is safe: the task
    claims no movement bits, so it is asked at every check.
*   **Pufferfish**: 1.13's timers and sizes (0.35 / 0.49 / 0.7). It fears survival and adventure players and
    every mob not in current Java's `not_scary_for_pufferfish` tag; 1.12.2 has no aquatic mob type, so the
    exceptions are matched by class (`EntityWaterMob`, guardians, our fish) and name (turtles). The
    player's sting sound, which 1.13 sends as a game-state event 1.12.2 lacks, is a sound packet to that
    player.
*   **Pufferfish food**: vanilla's item adds 1.12.2's effects; while a pufferfish is being eaten,
    `PotionApplicableEvent` swaps Poison IV for II and Nausea II for I as they are applied. Poison IV from
    anything else is untouched.
*   **Buckets**: four items (`cod_bucket`, ...), plain `Item`s rather than `ItemBucket`s so nothing mistakes
    them for a fluid container. Placing copies `ItemBucket`'s, including Forge's `FillBucketEvent` and the
    Nether boiling the water away; kelp, seagrass and wet coral (`common/BlockPseudoWater`) already are water
    and just take the fish, a dry coral is filled. The bucket keeps current Java's data: name, `Health`,
    `NoAI`, `Silent`, `NoGravity`, `Glowing`, `Invulnerable`, `BucketVariantTag`; an `EntityTag` is applied as
    a spawn egg's is.
*   **Advancement triggers**: 1.12.2 has neither `filled_bucket` nor `fishing_rod_hooked`, so both are ours
    (`updateaquaticbackport:filled_bucket`, `updateaquaticbackport:fishing_rod_hooked`, 1.12.2 item
    predicates). The second fires from Forge's `ItemFishedEvent` for every item reeled in. Fishy Business
    hangs off vanilla's husbandry root; Tactical Fishing off Fishy Business.
*   **Raw fish names** are vanilla keys (`item.fish.cod.raw.name` ...) in our `en_us.lang`. Resource domains
    load in insertion order (a `LinkedHashSet`), Minecraft's first, so our lines win.
*   **Renderers** are 1.14.4's, with its six models (`CodModel`, `SalmonModel`, the three pufferfish models,
    `TropicalFishAModel`/`BModel`), registered from `ModelRegistryEvent` by a client-only subscriber, so
    `common/ClientSetup` and the mod class are untouched.
*   **Numbers** from 1.14.4: health 3, sizes, speeds (flee 1.6/1.4 within 8 blocks, panic 1.25), swim
    chance 1 in 40, schools of 8 (salmon 5), puff timers, sting 1 + stage damage and 3 s Poison per stage,
    spawn egg colors, loot. From current Java: spawn weights and groups, spawn heights, the water ambient
    cap and despawn distances, the pufferfish's fears and food effects, the bucket data.

## Tested headlessly

`./gradlew runServer -Pselftest=fish`, then `python tools/check_assets.py`: **`SELFTEST DONE pass=215 fail=0`** for
`fish` alone (the count varies a little with random group sizes; stable over six runs), **`pass=1083 fail=0`**
with every stream's test (`-Pselftest=all`); **74 blocks, 5210 states, 67 item models, 123 sound files, 0
errors**. `python tools/lang/vanilla_keys.py`: 162 vanilla names, 70 of our own; every fish name, entity,
tropical fish type, predefined fish, color, advancement and subtitle is a vanilla name; ours are only the
config lines and the tooltip's color separator.

*   Registration: four entities with ids 1-4, translation names and spawn eggs; four buckets stacking to 1,
    each with a dispenser behavior; four loot tables load; the water ambient type (cap 20, peaceful, every
    tick); both advancements load with their triggers.
*   Fear rules: survival player yes, creative and spectator no; zombie yes; squid, guardian, cod and
    pufferfish no.
*   Spawn lists for every ocean kind, deep lukewarm and rivers, with their total weights; a real vanilla
    ocean near spawn offers fish through Forge's event; no fish among water creatures.
*   In a 40x40 pool from y=48 to 62, each fish: may spawn at y=54, not on the floor; alive in water after 800
    ticks with full air and having swum more than a block in 800 ticks; counts as water ambient only, one per spawner
    round; saved and loaded as itself with FromBucket, health, variant and puff state (and a loaded puffed
    pufferfish has the puffed hitbox); a bucket fish is left out of the cap.
*   Five cod form a school within 300 ticks (a cod that wandered out of reach before the first search may
    stay alone); every follower's leader leads and no leader follows.
*   Natural spawns: refused with no player within 64 blocks, allowed with one 40 blocks away; six per kind
    through Forge's check-spawn event: groups within current Java's sizes, every member following the first,
    predefined tropical schools all one look, a random-look tropical fish alone.
*   A salmon on land flops and first suffocates at tick 320 (16 s), losing 2 health.
*   Pufferfish: deflated 0.35 wide and stays so alone; a pig within 2 blocks: half puffed within 5 ticks,
    fully puffed 2 s later at 0.7 wide; the touching pig is stung and poisoned 6 s; a touching survival
    player is stung, Poison I for 6 s; alone again it half deflates after 3 s and deflates after 5.
*   Buckets: a water bucket on a named tropical fish gives a tropical fish bucket with its look, name and
    health; emptying it on a dry floor through the real right-click path leaves an empty bucket, water,
    and the same fish (look, name, health, never despawning); tooltip keys for a non-predefined fish; a cod
    bucket into kelp keeps the kelp; not into stone; a dispenser empties a cod bucket into water with a
    persistent cod; in creative the water bucket stays and the fish bucket is added.
*   Drops: each fish drops its raw fish metadata; cod and salmon on fire drop the cooked ones.
*   Eating a pufferfish: Poison II 60 s, Nausea I 15 s, Hunger III 15 s; Poison IV from elsewhere stays IV.
*   Advancements: a stick reeled in does nothing, a salmon grants Fishy Business; the bucket catch above
    granted Tactical Fishing.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything visual (no client): the renderers and six models, the swim wiggle and lying on the side out of
    water, the pufferfish's model swap, the tropical fish's tinted body and pattern layer, the bucket item
    models and the tropical bucket's tooltip text.
*   Sounds playing, the sting sound packet arriving, subtitles.
*   The real natural spawner in a world with a player (it needs a connected player's chunks); the spawn
    path was driven by hand through the same Forge events.
*   Despawning at 64/32 blocks (needs a player in the world).
*   The Nether boiling the water out of a fish bucket.

## Not done (later updates, or not 1.13)

*   Salmon sizes (1.21.2), lush caves and mangrove swamps as homes (1.17, 1.19), axolotls hunting fish.
*   New textures for vanilla's raw fish items: only their names change.
*   Sneaking does not stop a pufferfish puffing: current Java has no such rule (the brief mentioned one).

## For the integrator

*   `tools/lang/vanilla_keys.py`: `candidates()` gains generic rules for `entity.<modid>.*`,
    `advancements.<modid>.*` and `color.<modid>.*`, and `SPECIAL` the six raw fish keys. Other streams with
    entities (turtles, dolphins, phantoms, drowned) need the same entity rule; take one copy.
*   `en_us.lang`'s config block gains `fish.spawn` and its tooltip, after `fish.enabled`.
*   The water ambient creature type lives in `fish` (`FishContent.waterAmbient()`); squid and dolphins stay
    water creatures, so no other stream needs it. If one ever does, it moves to `common`.
*   `SelfTestRunner.TESTS` gains `new FishSelfTest()`.
*   Packet ids 15-19 are unused.
*   The 18 language files are not generated here; the family tool is on `ua-languages`.
