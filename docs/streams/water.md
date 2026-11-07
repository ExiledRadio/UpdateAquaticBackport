# Stream A: water look and feel

Package `water` (client side in `water.client`). Config `water`: `enabled` plus one switch per part
(`biomeColors`, `underwaterFog`, `ambience`, `lightThroughWater`, `gradualAir`, `floatingItems`,
`shallowJump`), all on by default. No packets (70-74 unused), no entity ids, no blocks or items. No mixins, no
coremod, no change to `UpdateAquaticBackport.java`: everything hangs off events from the stream's own
`@Mod.EventBusSubscriber` classes.

## Changelog section

*   **Water colors by biome**: water has 1.13's texture and its colors: blue in most places, darker blue in
    snowy taigas and beaches, purple in frozen oceans and rivers, a light green-gray in swamps, and (with the
    new ocean biomes) teal in warm oceans and light blue in lukewarm ones. Other mods' biomes keep their own
    tint, laid over the new blue.
*   **Seeing under water**: the fog under water takes the biome's water color, blends as you swim into
    another biome, gets darker the deeper you go, and clears the longer you stay under (about 30 seconds to
    see furthest). Swamp water is murkier. Respiration and Water Breathing no longer help you see.
*   **Underwater ambience**: a soft sound as your head goes under and comes up, a quiet loop while under, and
    now and then bubbles, a creak or a distant whale.
*   **Light through water**: water dims light by 1 level per block instead of 3, so shallow seas are no
    longer dark at the bottom. Light already worked out in existing chunks stays until something there
    changes, as in 1.13.
*   **Air comes back gradually** after you surface (4 a tick, a full bar in under 4 seconds), and the
    bubbles stay on screen until it is full.
*   **Items and experience orbs float** up in water instead of sinking.
*   **Jumping in shallow flowing water** (the thin layers at the edge of a flow) is a normal jump instead of
    swimming up slowly, so you can step out of it onto the next block.

## How it works

*   **Colors** (`WaterColors`): Forge's `BiomeEvent.GetWaterColor`, which every water tint in 1.12.2 asks
    (vanilla's water, Forge's fluid renderer, our own water surfaces). Vanilla biomes by identity with
    1.14.4's `waterColor` / `waterFogColor`; our seven ocean biomes by registry name (`warm_ocean`,
    `lukewarm_ocean`, `deep_lukewarm_ocean`, `cold_ocean`, `deep_cold_ocean`, `frozen_ocean`,
    `deep_frozen_ocean`), so the oceans stream needs to set no colors; any other mod's biome: white becomes
    1.13's default `#3F76E4`, any other tint is multiplied channel by channel with `#3F76E4` (it was a filter
    over 1.12.2's blue texture, so it stays a filter over the new blue). Fog for unknown biomes is the
    default `#050533`.
*   **Texture** (`client/WaterTexturePack`): 1.13's colors only suit 1.13's gray texture. The 1.18 textures
    (`water_still`, `water_flow` with their animations, `water_overlay`) are stored in our own
    `textures/water/`, and a small resource pack serves them as `minecraft:textures/blocks/water_*`. It is
    added at `ModelRegistryEvent` (pre-initialization, after the first resource load and before the block
    atlas is stitched): to `Minecraft.defaultResourcePacks` (reflection, `field_110449_ao`) so every later
    reload keeps it, and to the live resource manager. It sits above the game's and the mods' packs and
    below any pack the player picks, so a player's own water texture still wins. Forge's water `Fluid` gets
    `#3F76E4` as its color, so tanks and pipes drawing it with the fluid color are not gray. If the files
    cannot be served, colors switch themselves off and 1.12.2's water stays.
*   **Fog** (`client/WaterClient`), 1.14.4's `FogRenderer`, applied whenever the camera is in water:
    `FogColors` recomputes the color from scratch: the biome's fog color at the camera, blended over 5000 ms
    as 1.14.4; the void darkening (`camera y * getVoidFogYFactor`, squared, below 1 - "darker at lower
    depths"); then the water-vision brightening. `FogDensity` sets `EXP2` and 0.05 minus
    `waterBrightness^2 * 0.03` (plus 0.005 in swamps) for the player, 0.05 for other living view entities,
    0.1 otherwise, and is cancelled (Forge then skips vanilla's whole setup, mode included, hence the explicit
    `setFog`). Blindness is left to vanilla, as 1.14.4. `waterBrightness` is 1.14.4's: a per-player counter,
    +1 a tick with the eyes in water (+10 spectating), -10 out, 0-600.
*   **Ambience**: 1.14.4's `UnderwaterAmbientSoundHandler` and `UnderwaterAmbientSounds` on the client
    player's tick: enter/exit sounds, the loop (fades in over 40 ticks, out at twice the speed), additions at
    1% a tick (0.1% rare, 0.01% ultra rare). Category `ambient`, no position. Sound files and `sounds.json`
    entries are 1.18's, via `tools/water/generate.py` (2.7 MB).
*   **Light** (`WaterContent`): at the block registry event, lowest priority (vanilla's blocks exist, no world
    yet), `setLightOpacity(1)` on still and flowing water, on both sides.
*   **Gradual air** (`GradualAir`): 1.12.2 refills to 300 at every tick the eyes are dry; a jump to exactly 300
    from below 296 is that refill and becomes the value before plus 4. Players: remembered at the pre-tick,
    corrected at the post-tick, both sides (the client refills its own copy, and the HUD reads it). Other
    mobs: corrected at the start of their next tick (`LivingUpdateEvent`), so their next tick under water
    carries on from the right value; between ticks a surfaced mob reads 300. `EntityWaterMob` keeps its
    instant refill (as 1.14.4's fish, squid and dolphins), and so does an entity whose eyes are in
    `updateaquaticbackport:bubble_column` (looked up by name; that column refills air itself). The air bar is
    drawn at `RenderGameOverlayEvent.Post(AIR)` when the player is dry and air is below 300 (vanilla draws it
    only under water), in Forge's slot (`right_height`).
*   **Floating items and orbs** (`FloatingItems`), per the proposal in `MIXIN_DECISION.md`'s open point
    (still not answered by the user; see below): neither entity has an update event, so a pass over
    `loadedEntityList` runs at each server world's tick end and the client's tick end. For one whose eyes are
    in water it adds 1.14.4's lift (item: `+0.0005` while below `0.06`; orb: `min(y + 0.0005, 0.06)`;
    horizontal x 0.99) and gives back the gravity 1.12.2 takes at the start of the next tick (0.04 / 0.03), so
    the next move is exactly 1.14.4's. Orbs never set 1.12.2's in-water flag, so they are tested by the eyes
    only.
*   **Shallow-water jump** (`ShallowWater`, client in `WaterClient.input`): 1.14.4 jumps normally when on the
    ground and submerged no more than 0.4 (`submergedHeight`, worked out as 1.14.4's
    `handleFluidAcceleration`: highest water surface touching the box minus its bottom, a block counting as
    full when water is above it). That is 1.12.2's flowing levels 5-7, the wiki's "level 1, 2 and 3" in
    1.13's fluid levels. At `InputUpdateEvent` (keys read, jump not yet handled) the player jumps, with 1.14.4's
    10-tick cooldown; vanilla then adds its swim-up 0.04 for the same tick, which the jump subtracts first.

## Tested headlessly

`./gradlew runServer -Pselftest=water` on port 25745, then `python tools/check_assets.py`:
**`SELFTEST DONE pass=181 fail=0`** for `water` alone, **`pass=1049 fail=0`** with `all` (scaffold,
woodset, kelp, coral, water); **74 blocks, 5210 states, 63 item models, 88 sound files, 0 errors**.

*   Six sound events registered; every part on by default.
*   Colors through the real event (`Biome.getWaterColorMultiplier`): plains, ocean, deep ocean, river, desert,
    Nether, End, snowy tundra default; swamp and swamp hills; frozen ocean and river; snowy taiga, its hills,
    mountains and the snowy beach; no vanilla biome left white (all of them checked). Our seven ocean names
    (stand-in biomes when the oceans stream is not in the build); a mod biome without a tint gets the
    default; a mod biome's tint over the new blue; fog colors for each.
*   Light: still and flowing water opacity 1, ice unchanged; sky light in a 12-deep walled pool drops by
    exactly 1 per block from 14 at the top to 3 at the bottom.
*   Gradual air: the rule itself; a real server player out of water gains 4 a tick (100, 104, ... 124), goes
    under and carries on from 124, and fills to exactly 300; a pig gets 10 ticks of +4 above water then -1
    under (139); a squid back in water is full at once. (The bubble-column exemption runs only when that
    stream is in the build: not exercised on this branch.)
*   Floating: one tick in water rises 0.0005 and keeps 0.98 of it (1.14.4's numbers); after 800 ticks an item
    has floated from near the floor to the surface and stays there; without the float the item sinks to the
    floor; an orb rises 0.0005 in a tick and never faster than 0.06; no float out of water; a no-gravity item
    gets only the lift; the world-tick pass reaches a spawned item.
*   Shallow water: submerged height of each flowing level 0-7 is (8 - level)/9 and 1.12.2 calls every one of
    them "in water"; normal jumps exactly at levels 5-7; water above makes the lower block full and no jump;
    off the ground, flying or dry: no jump.
*   The dedicated server booting at all proves no client type is reached from server code.

## Not tested

Everything a player sees or hears, since there is no headless client:

*   The texture replacement (that the pack is picked up, the animation plays, and a player's resource pack
    still overrides it), the colors on screen, Forge's water fluid color in tanks.
*   The fog: color, blending between biomes, darkening, density, the water-vision clearing.
*   The ambience sounds playing, fading and stopping.
*   The air bar above water.
*   The client half of floating (items bobbing smoothly between server updates) and the shallow-water jump
    itself (only the rule deciding it was tested).

## Deliberately left out

*   **Underwater music** (1.13's "Axolotl", "Dragon Fish", "Shuniji"): 31 MB of streams, and picking music
    needs the music ticker's selection, which has no hook. Left for the user to decide.
*   **Mobs jumping out of shallow water**: 1.14.4's rule is in `LivingEntity` for every mob; here only the
    player, where 1.12.2 visibly fails (mob movement is the server's and would need its own pass).
*   **The boss fog tint** (wither/dragon fight darkening) is not applied to the water fog: its value is
    private to `EntityRenderer`.

## For the user: floating items

`MIXIN_DECISION.md`'s open point on floating items had no answer when this stream was built. It is built as
proposed there: a world-tick pass, behind its own switch (`water.floatingItems`, on by default), limited to
items and orbs whose eyes are in water. Turning the default off, or removing the part, is one line.

## For the integrator (`common/` and other streams)

*   **`common/BlockPseudoWater.WATER_OPACITY`** is still 3, so kelp, seagrass, waterlogged coral, conduits and
    bubble columns still dim light by 3 while the water around them dims by 1. Change `getLightOpacity` to
    return `Blocks.WATER.getLightOpacity(Blocks.WATER.getDefaultState())` (it follows the
    `water.lightThroughWater` switch that way), or `WaterContent.WATER_OPACITY` when that switch is on.
*   **Bubble columns** (stream B refills +4 a tick with the eyes in a column): `GradualAir` skips any entity
    whose eye block is `updateaquaticbackport:bubble_column`, so the two never add up. After merging B, run
    `-Pselftest=water` once to exercise the exemption.
*   **Conduit Power** (stream N) brightens underwater fog by borrowing Night Vision for the frame. With
    `water.underwaterFog` on, the water fog color is recomputed from scratch and its Night Vision brightening
    is dropped, which is 1.14.4's order (in water, Night Vision does not brighten the fog); its light-map
    brightening is unaffected. The water-vision brightening replaces it.
*   **Oceans** (stream O): nothing to do; colors come by biome name. A biome renamed there needs the name
    here too (`WaterColors.OURS`).
*   **`client/ClientSetup`**: unchanged; its pseudo-water surface tint goes through the color event and
    gets 1.13's colors, and the shared surface model uses `blocks/water_still`, which the pack replaces.
*   **Languages**: the stream's only strings are its 7 config names and tooltips, all ours, in the
    `# --- water ---` block of `en_us.lang`. There are no vanilla names (the underwater sounds have no
    subtitles), so `tools/lang/vanilla_keys.py` has nothing of ours to flag. The other 17 languages are
    left to the family's language tool after merge.
