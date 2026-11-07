# Stream R: trident and drowned

Package `trident` (client code in `trident.client`). Config `trident` (`enabled`, `drownedSpawn`,
`zombiesConvert`, `impalingMobs`). Entity ids 25 (thrown trident) and 26 (drowned), packets 35 (a zombie
is converting, for the shake) and 36 (Riptide spin started/ended).

## Changelog section

*   **Trident**: 9 attack damage at 1.1 attack speed, 250 durability. Hold use and let go to throw it for 8
    damage; it sticks in blocks and only its thrower can pick it back up. A trident one use from breaking
    cannot be used. Found only on drowned.
*   **Loyalty** (I-III): the trident flies back to its thrower after it hits, through blocks, faster at each
    level; it even comes back from the void. A Loyalty trident never despawns.
*   **Riptide** (I-III): in water or rain, the trident launches you instead of leaving your hand, and you spin
    for a second, hitting the first mob in your way. Cannot go with Loyalty or Channeling.
*   **Impaling** (I-V): 2.5 more damage per level against aquatic mobs (squid, guardians, fish, dolphins,
    turtles), thrown or in melee.
*   **Channeling** (I): in a thunderstorm, a trident that hits a mob under open sky calls down lightning.
*   **Drowned**: underwater zombies in oceans and rivers. They swim after you when you are in the water, come
    ashore at night, and some throw tridents. 3% hold a nautilus shell, which they always drop. They drop
    rotten flesh, and their trident sometimes.
*   **Zombies drown**: a zombie underwater for 30 seconds shakes for 15 more and becomes a drowned, keeping
    its equipment and name. A husk becomes a zombie the same way.
*   Advancements *A Throwaway Joke* (hit something with a thrown trident) and *Very Very Frightening* (strike
    a villager with lightning).

## How it works

*   **Thrown trident** (`EntityTrident`) is 1.12.2's arrow with 1.14.4's `TridentEntity` on top. The arrow's
    in-ground timer is private and its water drag fixed at 0.6, so the class corrects the drag to 0.99 after
    the arrow's tick, resets the despawn timer by reflection (SRG `field_70252_j`) for Loyalty tridents, and
    flies the Loyalty return itself (1.14's no-clip flight). The thrower is saved by UUID (1.12.2's arrow
    forgets it on reload) and sent to clients as spawn data so they can draw the return.
*   **Item**: `EnumAction.NONE` to vanilla code (no new enum value, so no switch ever meets one); the
    raised-spear first-person view is a `RenderSpecificHandEvent` hook. The item model is a baked wrapper:
    the flat `trident` model in GUI, ground and frames, the builtin `trident_in_hand` (3D, drawn by the item
    renderer) elsewhere, `trident_throwing` while a mob throws.
*   **Enchantments** are on a trident-only `EnumEnchantmentType` (`EnumHelper.addEnchantmentType`), so
    tables and anvils offer them only for tridents; the mod's tab lists their books. 1.12.2 has no aquatic
    creature attribute, so **Impaling** adds its bonus in `LivingHurtEvent` for melee (scaled by the attack
    cooldown read at `AttackEntityEvent`, with the enchanted-hit particles) and in the trident's hit for
    throws. "Aquatic" is: vanilla's water mobs, guardians, a creature attribute named `WATER`, and the ids in
    `trident.impalingMobs`.
*   **Channeling** lightning remembers its caster (1.12.2's bolt has no caster field);
    `EntityStruckByLightningEvent` on a villager grants *Very Very Frightening*, whose only criterion is
    `minecraft:impossible` because 1.12.2 has no `channeled_lightning` trigger. *A Throwaway Joke* uses
    1.12.2's own `player_hurt_entity` trigger on our entity id.
*   **Riptide** spin (`Riptide`): kept per entity object on each side. The server checks the path box each
    tick, attacks the first living entity with the held item and bounces the player; clients are told when a
    spin starts and stops. The spin pose (1.14.4's rotations) and the riptide rings are drawn through
    `RenderLivingEvent` and a layer added to the player renderers. No mixin.
*   **Drowned** (`EntityDrowned`) is 1.12.2's zombie with 1.14.4's drowned goals, move helper, water and
    ground navigators and travel; it drops 1.12.2's swim goal, so it sinks. Equipment pickup follows 1.14.4's
    preference (a trident is kept unless a less damaged one comes along). Spawning (`DrownedSpawning`): spawn
    entries on every biome-dictionary ocean (5), river (100) and frozen river (1), added at init so every
    mod's biomes are typed; placement `IN_WATER`; 1.14.4's water rule in `LivingSpawnEvent.CheckSpawn`
    (spawners skip it), and current Java's monster light rule (block light 0) in `getCanSpawnHere`.
*   **Conversion** (`DrownedConversion`): 1.12.2's zombie has no drowning clock, so it lives in the mob's
    persistent Forge data and advances in `LivingUpdateEvent`; the mob's air is held full while it runs.
    Only the exact classes `EntityZombie` and `EntityHusk` convert. The new mob is made as current Java's
    `convertTo` (equipment and drop chances via the saved NBT, name, persistence, baby, AI switch, new
    attribute bonuses). The shake is a client render hook fed by packet 35.
*   **Numbers** from 1.14.4: throw needs 10 ticks of use, speed 2.5; Loyalty pull 0.05 x level; Riptide push
    3 x (1 + level) / 4, spin 20 ticks; enchantment costs and rarities; drowned 6.25% trident, 3.75% rod, 3%
    shell; trident throw interval 40, range 10, speed 1.6, spread 14 - 4 x difficulty; ocean 1 in 40 below
    sea level - 5, river 1 in 15; conversion 600 + 300 ticks. Current Java: no copper ingot drop (later
    update), the too-damaged rule, Loyalty from the void, enchanted tridents glinting when thrown.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25737), then `python tools/check_assets.py`:
**`SELFTEST DONE pass=1077 fail=0`** (209 of them this stream's), **74 blocks, 5210 states, 64 item models,
119 sound files, 0 errors**. `./gradlew build` last; the jar's classes carry `func_` names (the only
readable Minecraft names left are Forge's own additions) and no self-test classes.

*   Registration: item, 4 enchantments (names, max levels, rarities, costs, trident-only, Riptide's
    exclusions, Unbreaking and Mending apply, Sharpness does not), enchanting-table rolls, both entity ids
    and network ids, spawn egg, the drowned's name key, all 19 sounds, the trident damage source, loot table,
    in-water placement, spawn weights (ocean, deep ocean, frozen ocean 5; river 100; frozen river 1; none on
    plains or beaches), what Impaling counts as aquatic.
*   Item: 9 damage and 1.1 speed in the main hand only, 250 durability, stack of 1, enchantability 1.
    Through a real server player: use starts, 9 ticks throws nothing, 10 ticks throws (1 durability, owner,
    pickup allowed, along the look), creative keeps the item and throws a creative-only copy, a trident one
    use from breaking refuses to be used.
*   Thrown hits: 8 to a pig, no second hit after the bounce, *A Throwaway Joke* granted; Impaling V kills a
    squid and adds nothing to a pig.
*   Channeling: no lightning in clear weather, without the enchantment, or under a roof; in a thunderstorm
    a bolt on the target, cast by the thrower; ticked, it turns the villager into a witch and grants *Very
    Very Frightening* to the thrower only.
*   Sticking and pickup: sticks in a wall, a stranger cannot pick it up, the thrower can; a plain one
    despawns after a minute in the ground, a Loyalty one does not; thrown stack, Loyalty, in-ground state and
    owner survive saving.
*   Loyalty: returns 4 ticks after landing, reaches its thrower and goes back into the inventory; with the
    thrower gone it drops as an item; comes back from below the world while a plain one is lost; water drag
    0.99.
*   Riptide: refused on dry land; in a pool it launches at 3 blocks per tick (Riptide III), keeps the item,
    costs 1 durability, throws nothing, spins 20 ticks; the first mob touched takes 9, which ends the spin
    and bounces the player.
*   Melee Impaling: Impaling V kills a squid, the same trident hits a pig for 9, 1 durability per hit.
*   Drowned: breathes underwater, has the trident attack and no float goal, 40 ticks underwater unhurt;
    targets by day only in water, at night anything; swims with the water navigator after a target in water,
    walks without; 1000 spawns: about 6.25% trident, 3.75% fishing rod, never armor, no nautilus shell while
    that stream is absent; its trident throw cannot be picked up and hits for 8; keeps its trident over a
    sword and swaps for a less damaged trident; survives saving; loot is 0-2 rotten flesh only.
*   Spawning: 1 in 40 in deep ocean water, 1 in 15 in rivers at any depth, never out of water or in ocean
    water less than 6 below sea level; dark water passes the light rule, any block light fails it; a natural
    spawn out of water is denied, a spawner's is not.
*   Conversion, through each mob's real tick: a zombie and a husk underwater count 600 ticks without drowning,
    then convert; the clock survives saving; zombie villagers, drowned and dry zombies never start; after 300
    more ticks the zombie is a drowned with its sword, drop chance, name, persistence and full health, and
    the husk a zombie; config off, nothing happens.
*   Both packets encode and decode.

## Not tested

*   Everything drawn (no client): the trident and drowned renderers and models, the item's flat/3D switch
    and glint, the first-person spear and spin views, the spin pose and riptide rings, the converting shake,
    the Loyalty return line. These are all in `trident.client`; the dedicated server booting proves no
    client type is reached from server code.
*   Sounds playing, and the packets actually reaching a client (only their encoding is tested).
*   Natural spawning over time in a real ocean (the spawn lists, placement, water and light rules are tested
    one by one).
*   The drowned's pathfinding and goals over time (heading for water, swimming up, going ashore), and
    trident-holding drowned throwing at a real player.
*   Nautilus shells in the off hand: the conduit stream owns the item; without it nothing is held (tested).

## Limits

*   Players hold a trident being thrown in the normal held pose: 1.12.2's player model cannot raise the arm
    without replacing it. `client/TridentPoses` has the pose; the swimming stream's own player model should
    call `TridentPoses.apply` and set `TridentPoses.playersRaiseArm`, which also switches players to the
    point-up `trident_throwing` model.
*   The spinning player's hitbox and eye height do not shrink to 0.6 (that is the swimming stream's pose
    engine).
*   Drowned seeking turtle eggs and baby turtles is the turtle stream's (it adds that AI to undead on spawn);
    drowned in ocean ruins are the structure stream's (by entity id `updateaquaticbackport:drowned`).

## For the integrator

*   `UpdateAquaticBackport.init`: one line, `TridentContent.init()` (adds the drowned to the spawn lists).
*   `NetworkHandler.init`: packets 35 and 36 registered.
*   `selftest/SelfTestRunner.TESTS`: one line, `new TridentSelfTest()`.
*   `ModConfig.Trident`: three new fields (`drownedSpawn`, `zombiesConvert`, `impalingMobs`).
*   `tools/lang/vanilla_keys.py`: its candidates now also match entity, effect, enchantment and advancement
    keys (as `build_langs.py` does) and same-named keys (death messages), and treat a vanilla line break as a
    space. Other streams with entities will want the same change; keep one copy.
*   `en_us.lang`: the `# --- trident ---` block has the stream's config names and tooltips; if
    `build_langs.py config` regenerates tooltips into its own block, drop the tooltip lines from this one.
    The other 17 language files are not written here (the family's language tool is on `ua-languages`).
*   No `common/` change needed.
