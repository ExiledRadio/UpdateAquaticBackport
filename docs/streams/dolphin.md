# Stream D: dolphins

Package `dolphin` (client side in `dolphin/client`). Config `dolphins` (`enabled`, `spawnWeight`). Entity id 15
(16-19 unused). No packets (25-29 unused): everything the client needs travels in the entity's data watcher,
entity events and vanilla's potion sync.

## Changelog section

*   **Dolphins** swim in oceans in pods of one or two, except in cold and frozen oceans (fewer in lukewarm and
    warm ones). They breathe air: four minutes under water, then they drown, so they come up to breathe.
    Stranded on land they flop about and dry out after two minutes, unless it rains.
*   Dolphins chase items floating in water, pick them up and toss them about. Whatever one is carrying when it
    dies, it drops.
*   They follow boats being rowed, leap out of the water, and swim alongside players who sprint in water,
    giving them **Dolphin's Grace**: water slows you far less while it lasts.
*   Feed a dolphin raw cod, raw salmon, tropical fish or pufferfish and it leads you to the nearest
    shipwreck or ocean ruin.
*   Dolphins are neutral: hit one (with anything but a guardian) and the whole pod fights back. They keep
    away from guardians.
*   They drop up to one raw cod (cooked if they die burning; Looting adds more) and 1-3 experience. Leads work
    on them, and a dolphin on a lead never despawns.
*   A dolphin spawn egg; dolphin sounds and subtitles; dolphin particles behind a swimming dolphin.

## How it works

*   **Base class**: `EntityDolphin extends EntityCreature` (1.12.2's `EntityWaterMob` is not a creature, and the
    AI tasks need one) and counts against the water creature cap through Forge's `isCreatureType`.
*   **Numbers** from 1.14.4's `DolphinEntity`: health 10, speed 1.2, attack 3 (difficulty-scaled as any mob's:
    nothing on Peaceful), size 0.9 x 0.6, eyes 0.3, air 4800, moistness 2400, every goal and its priority,
    the swim movement and look controllers, the swimmer path finder that may break the surface, the jump
    checks, the toss motion, the boat-following phases, the 50-chunk structure search. Spawn egg colors
    2243405 / 16382457. Dolphin's Grace: beneficial, color 8954814, 0.96 water slowdown.
*   **Today's rules (minecraft.wiki)**: spawning from 13 below sea level up to it, with water above and below;
    no dolphins in cold oceans (1.13 had them); the dolphin leads only to shipwrecks and ocean ruins (current
    `dolphin_located` tag; buried treasure is not in it); a leashed dolphin never despawns (26.1).
*   **Air without a patch**: 1.12.2 resets every mob's air to 300 when its eyes leave water. The dolphin's
    `onEntityUpdate` sets it to 4800 straight after (1.14.4 refills it at once too). A bubble column of this
    mod at the eyes counts as air, looked up by name (`bubble_column`).
*   **Dolphin's Grace without a patch** (`DolphinsGrace`): 1.12.2's travel multiplies horizontal motion by 0.8
    (or Depth Strider's value) after moving. At the next `LivingUpdateEvent`, before the next move, the motion
    is scaled by 0.96 / that value, which gives 1.14.4's result exactly; vertical motion is 0.8 in both. It
    runs only where travel would (`isServerWorld() || canPassengerSteer()`, never for a NoAI mob) and for a
    player only on its own client, where players are moved.
*   **Swimming players**: 1.13's swimming is sprinting in water, so that is what draws a dolphin
    (`AISwimWithPlayer.isSwimming`, the one place to change when the swimming stream lands). Range 10 with
    line of sight, shortened by sneaking and by invisibility as 1.14.4's visibility rule does.
*   **Weights**: natural spawning is added in postInit to every non-snowy ocean biome in the biome dictionary,
    so other mods' oceans and the oceans stream's new biomes get it too, at `spawnWeight` (default 10, the
    1.12.2 squid's own weight: in today's ocean dolphins and squid are about equally common). Temperature is
    decided per position with `common/OceanKind`: cold and frozen refuse, lukewarm and warm pass one try in
    five (today's lukewarm and warm weights are a fifth of squid's).
*   **Guardians**: 1.14.4 passes the guardian class to its revenge goal as "never retaliate against"; 1.12.2's
    constructor reads that argument as "never call for help", so `AIDefendPod` checks the attacker itself.
*   **Small fixes over 1.14.4**: the jump sound plays on every leap (vanilla never resets its flag, so only a
    dolphin's first leap sounds).
*   **Look helper**: 1.12.2 keeps `EntityLiving.lookHelper` private; the dolphin sets its own by SRG name
    (`field_70749_g`) in its constructor.

## For the structure stream: the locator interface

The plan asks for a small `common/Structures` locator. A stream does not change `common`, so it lives in
`dolphin/StructureLocator` for now, with the exact API the integrator should move into `common/Structures`:

```java
public final class Structures {
    public static final String SHIPWRECK = "shipwreck", OCEAN_RUIN = "ocean_ruin", BURIED_TREASURE = "buried_treasure";

    public interface Locator {
        /** @return a position in the nearest structure of this kind within radiusChunks, or null */
        @Nullable BlockPos nearest(WorldServer world, BlockPos from, int radiusChunks);
    }

    public static void register(String kind, Locator locator);   // the structure stream, once per kind, in init
    public static void unregister(String kind);                  // tests
    public static boolean has(String kind);
    @Nullable public static BlockPos nearest(WorldServer world, String kind, BlockPos from, int radiusChunks);
    @Nullable public static BlockPos nearestOf(WorldServer world, BlockPos from, int radiusChunks, String... kinds);
}
```

*   Kinds are 1.13's structure names in lower case. A locator runs on the server thread, must not generate
    anything, and should not load chunks it does not need (it is called each time a dolphin is fed).
*   Dolphins call `nearestOf(world, pos, 50, OCEAN_RUIN, SHIPWRECK)` from `AISwimToTreasure`, the only use.
*   With nothing registered every lookup is null: a fed dolphin forgets the fish and just swims (tested).
*   The same locator can serve treasure maps (`BURIED_TREASURE`) later.

## For the integrator

1.  **Move `dolphin/StructureLocator` to `common/Structures`** (same API, class renamed), change the one
    import in `dolphin/AISwimToTreasure` and the selftest, and delete the dolphin copy.
2.  **`tools/lang/vanilla_keys.py`** lists only `tile.`, `item.` and `subtitles.` keys as vanilla. This stream
    adds `entity.updateaquaticbackport.dolphin.name` (Mojang's `entity.minecraft.dolphin`),
    `effect.updateaquaticbackport.dolphins_grace` (`effect.minecraft.dolphins_grace`) and Mojang's own
    `death.attack.dryout` / `death.attack.dryout.player` keys, all with Mojang's English; the script reports
    them as ours. It needs rules for `entity.<modid>.X.name -> entity.minecraft.X`, `effect.<modid>.X ->
    effect.minecraft.X` and "the same key exists in Mojang's file" (`build_langs.py` on `ua-languages` already
    has all three). `vanilla_keys.json` is not committed here; regenerate it after merging.
3.  **Other languages**: not written here; run the family's language tool after the merge.
4.  The Mojang text for `death.attack.dryout.player` says "whilst"; it is vanilla's line, kept as is.
5.  `UpdateAquaticBackport.postInit` gains one line (`DolphinContent.postInit()`), `ModConfig.Dolphins` one field,
    `SelfTestRunner.TESTS` one line.

## Tested headlessly

`./gradlew runServer -Pselftest=dolphin` on port 25736: **`SELFTEST DONE pass=131 fail=0`**; with every test
(`-Pselftest=all`): **`pass=999 fail=0`**; then `python tools/check_assets.py`: **74 blocks, 5210 states, 63 item
models, 104 sound files, 0 errors**. A boot of a fresh world with `dolphins.enabled=false` starts cleanly and
registers nothing.

*   Registration: entity `updateaquaticbackport:dolphin`, mod entity id 15, translation name, spawn egg and its
    colors, made from its id by `EntityList`; Dolphin's Grace registered, beneficial, color, name key; 10 sound
    events; every goal present; the dry-out death message key.
*   Attributes and size, 4800 air and 2400 moistness at spawn, not pushed by water, 1-3 experience.
*   Spawning: in-water placement, spawn entries in ocean and deep ocean (weight 10, groups 1-2) and not in
    frozen ocean, river or plains; counted as a water creature only, and not for the cap when persistent; the
    height and water rule at sea level -14 / -13 / -1 / 0; cold, frozen and non-ocean refused; warm passing
    about a fifth of tries; and in a **generated ocean near spawn, 286 spots in water, 91 passing**, none of
    them in a cold or frozen ocean and none refused in a normal one. A leashed dolphin does not despawn.
*   Breathing: air falls one a tick under water; at 150 air it swims up from the floor of a 6-deep pool and
    refills to 4800; at -20 it takes 2 drowning damage; Water Breathing keeps its air.
*   Drying out: on land air refills at once, moistness falls one a tick, it flops, at 0 it takes 1 damage
    from `dryout`; water restores moistness.
*   Saving: treasure position, got fish, moistness, air and the carried item survive a write and a load
    through `EntityList.createEntityFromNBT`.
*   Feeding by a real server player: all four raw fish eaten (one used each), a stick and cooked fish are
    not, creative uses none.
*   Treasure: with no locator a fed dolphin forgets the fish; with a shipwreck locator 35 blocks away and an
    ocean ruin 2000 away it picks the shipwreck, **swims from 35 to 5 blocks and forgets the fish on arrival**;
    under 100 air it does not set off.
*   Dolphin's Grace: a mob in still water keeps 0.8 of its speed a tick, with the effect 0.96, vertical
    unchanged.
*   Swimming with players (a real server player in the world's player list): nothing while not sprinting; a
    sprinting player 4 blocks away gets Dolphin's Grace for at most 5 seconds; invisible with no armor, not
    noticed at 4 blocks; visible again, noticed.
*   Items: it picks up a stick floating in water and tosses it (a new item with a pickup delay); an item
    picked up is always dropped on death.
*   Boats: a still boat is ignored; a steered boat within 5 blocks starts the follow goal.
*   Jumping: at the surface with open water ahead it may leap (0.7 up, 0.6 forward); deep under water with no
    air above, never.
*   Defense: a dolphin hit by a zombie and its podmate both target the zombie and bite it; hit by a guardian,
    it does not fight back.
*   Drops: 200 deaths dropped 99 raw cod, never more than one each, no cooked; burning, cooked cod only.
*   The server booting proves no client class is reached from server code.

## Not tested

*   Anything visual (no client): the model, its animation, the texture, the carried item at the snout, the
    swimming particles, the happy particles of a dolphin that knows the way, the Dolphin's Grace icon in the
    inventory and the HUD.
*   Dolphin's Grace on a real player: the scaling runs on the player's own client, and the headless test runs
    it on a server-moved mob instead (the same code and the same rule).
*   Sounds playing and their subtitles showing.
*   Natural spawning by the world's own spawner over time (the spawn list, placement and spawn checks are
    tested; a spawning pass needs players near loaded chunks).
*   Rain keeping a stranded dolphin moist (the rule is vanilla's `isWet`).
*   The boat chase itself (only that it starts), the leap's flight and the avoid-guardians goal running.
*   The real structure stream's locators (it does not exist yet); the locator here was a test double.
*   Not included, on purpose: baby dolphins and golden dandelions (26.1 additions with models and items
    1.18's assets do not have).
