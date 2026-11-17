# Stream Me: 1.13's behavior changes to existing things

Package `mechanics` (client part `mechanics/client`). Config `mechanics`: `leafDistance`, `squidBiomes`,
`floorItemFrames`, `shapedOutlines` (client), `cactusBreaksSigns`, `placementSounds`, `witchHutMushroom` (new
chunks), `creativeSleep`, `chestPairing`, all on by default. Entity id 30 (block 30-34): `updateaquaticbackport:item_frame`. No
packets, blocks or items. Patches P7 to P11 (`docs/PATCHES.md`). From the family audit of 2026-11-16
(`family-notes/MECHANICS_AUDIT_2026-11-16.md`), its 1.13 rows.

## Changelog section

*   **Leaves stay up to 6 blocks from a log** (1.12.2: 4), so large trees keep their canopy. Every leaves block
    that uses vanilla's decay, modded ones included.
*   **Squid spawn only in oceans and rivers**, not in swamps, ponds or other water.
*   **Item frames can be placed on floors and ceilings.**
*   **The block outline follows the block's shape**: stairs, fences, panes, iron bars, hoppers, cauldrons,
    anvils, brewing stands and others are no longer outlined as a box.
*   **Signs and banners can be placed beside a cactus**, which breaks it.
*   **Signs, banners, heads and redstone dust make a sound when placed.**
*   **Witch huts have a red mushroom in their flower pot** (new chunks).
*   **Players in Creative can sleep with monsters nearby.**
*   **Chests join as in 1.13**: a chest placed while sneaking stays single, so two chests can stand side by side; a
    chest can be placed beside a double chest or between two chests; a chest joins only a single chest of its kind
    beside it that faces the same way (or, sneaking, the single chest whose side was clicked).

## How it works

*   **Leaves**: patch P7. 1.12.2's marking (`check_decay`, set when a log or leaf nearby goes) still decides when a
    leaf is checked; the check reaches 6 steps, and a broken log marks the leaves 6 around. Leaves 5 and 6 from a
    log stay, as in 1.13. Not done: 1.13 also decays, on a random tick, unmarked leaves already more than 6 from a
    log; in an existing world that would strip leaves 1.12.2 kept (and modded trees built to 1.12.2's rules).
*   **Squid** (`SquidSpawning`): 1.13.1 (18w31a) kept squid to rivers, oceans and beaches; 1.14 dropped the
    beaches, and current Java (1.14.4's biome classes and since) lists squid only in oceans and rivers. Current
    Java wins (`docs/PLAN.md`): at load complete, `EntitySquid`'s entry (that exact class) leaves the water creature
    list of every biome that is neither `OCEAN` nor `RIVER` in the biome dictionary (modded biomes included), and a
    natural squid spawn anywhere else (a later entry, `PotentialSpawns`) is refused. Spawners, eggs and commands
    are left alone. The ocean and river entries keep 1.12.2's weight (the dolphins are weighted against it).
    Needs a restart to change.
*   **Item frames on floors and ceilings** (`EntityFloorItemFrame`, `MechanicsContent.placeFrame`,
    `client/RenderFloorItemFrame`): 1.12.2's `EntityHanging` takes only the four sides, so a frame facing up or
    down is its own entity, a subclass of the vanilla frame (maps, comparators, rotating, breaking and dropping
    all work), with 1.14.4's `ItemFrameEntity` rules: pitch -90 or 90, a box 12 by 12 pixels and 1 thick against
    the block, held up by a solid block, and `Facing` saved as 1.13's index. Using an item frame on the top or
    bottom of a block fails in 1.12.2's item; that click then goes on as a plain item use (after the block had its
    chance to open or flip), where this places the frame along the player's own look and reach, plays its sound
    and uses up one frame (none in Creative). The renderer is 1.12.2's turned by the pitch first, as 1.14.4's.
    With the setting off, existing floor frames stay; no new ones are placed.
*   **Outlines** (`BlockShape`, `client/MechanicsClient`): 17w47a, "blocks with a collision box now have matching
    bounding boxes". 1.13 outlines a block by its shape and draws the shape's edges. Here a hopper, anvil and
    cauldron get 1.14.4's shapes; any other block whose collision is two or more boxes is outlined by them, cut at
    the block's top (fences and walls collide 1.5 high); a one-box block keeps 1.12.2's box. Edges are found as
    `VoxelShape.forEachEdge` finds them and drawn as vanilla's box (black at 40%, 2 pixels, no depth writes). Not
    done: what the crosshair can target is 1.12.2's (1.12.2's stairs already target by their parts).
*   **Cactus**: patch P8. **Witch hut**: patch P9. **Creative sleep**: patch P10.
*   **Chests** (`ChestPairing`, patch P11): the pairing is stored per chest (tile entity Forge data) and every 1.12.2
    reader of the neighbors asks it instead. First skipped (2026-11-17): 1.12.2 has no stored chest type and pairs by
    looking at neighbors in five places; doing it means replacing all of them, which looked too invasive for a
    low-impact item. Done after a player report, gated to vanilla's two chest blocks so other mods' chest subclasses
    (Quark's chests, say) keep their own pairing; Iron Chests and other mods with their own chest blocks are
    untouched. A mod that reads `adjacentChest*` on a vanilla chest sees the stored pairing.
*   **Placement sounds** (`MechanicsContent.placeSound`, 17w47a): 1.12.2's sign, head and redstone items are not
    `ItemBlock`s, and its banner item places on its own; none plays a sound. Forge's place event (server) gives the
    block placed; for a sign, banner, head or redstone dust placed by anything but another mod's `ItemBlock`
    (which plays its own), the block's place sound plays for everyone around at `ItemBlock`'s volume and pitch.

## Not done

*   The 1.13 anvil's detailed collision (its outline is 1.13's; walking against it is still 1.12.2's box).

## Tests (`-Pselftest=chests`)

Chests placed by a real player's item use: two placed side by side while sneaking stay single (pairing, container
size, `adjacentChest*` fields), through a block update and saved and read back; a normal placement joins a single
chest facing the same way, saved; a third chest beside a double is allowed and single, the double unchanged; a chest
placed between two singles joins the one on its left only; chests facing different ways do not join; sneaking against a
single chest's side joins it with its facing; a trapped chest and a chest never join, two trapped chests do; a hopper
sees 54 or 27 slots, a comparator reads the other half's items only for a double, the item handler has 54 or 27 slots;
the update packet and tag carry the pairing; breaking a half leaves the other single; two chests without a stored
pairing pair as 1.12.2 and get it stored; with the setting off the two singles are one double and a chest beside them is
refused again, and with it on again they are single. Client (`clientmech`): a pair placed while sneaking opens as 27
slots, a normal pair as 54, and `ua_mech_chests.png` shows the double and the two singles.

## Tests (`-Pselftest=mechanics`)

Leaves (distances, decay at 7, edges do not connect, setting off at 5, a broken log marks 6), squid (the lists of
oceans, rivers, this mod's oceans and seven other biomes; a natural spawn in a swamp refused, a spawner's not),
floor frames (valid on a floor and under a ceiling, not in the air, pitch, box, `Facing` saved as 1, read back
with its item, a real player looking down places one and uses one up, looking at nothing or with the setting off
does not), cactus (signs and banners allowed, setting off refused, the cactus breaks), sounds (sign, banner, head,
redstone dust through Forge's item placement; setting off silent), witch hut (red mushroom; setting off empty),
sleep (survival `NOT_SAFE`, Creative `OK`, setting off `NOT_SAFE`), outlines (stone none, a stair 18 edges, a corner
fence 18 edges 1 high, a lone post none, hopper, cauldron and anvil shapes, a fence gate none).

Client (`runClient -Pselftest=clientmech`, `ClientMechanicsTest`): in a flat Creative world the client has the
floor and ceiling frames, the crosshair lands on stairs, a corner fence and a hopper, and screenshots
(`eclipse/screenshots/ua_mech_*.png`) show three floor frames lying flat with their items, a ceiling frame lit under
stone (its eye height is 0, as 1.14.4's; 1.12.2's default read the light inside the block above and drew it black),
and the stairs, L-shaped fence (vanilla's square box with the setting off) and hopper outlined by their shapes.
