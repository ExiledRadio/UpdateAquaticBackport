# Stream Sw: swimming and crawling

Package `swim` (client side in `swim.client`). Config `swimming`: `enabled` and `crawling`, both on by
default and both live (no restart). Packet 75 (the server's switches, at login); 76-79 unused. No entity
ids, blocks, items, textures or sounds. One line in `UpdateAquaticBackport.preInit` and one packet line in
`NetworkHandler` (see *For the integrator*).

The pose engine - the pose itself, the hitbox, the eyes, sending poses to other players and drawing them -
is shared family code in **Backport Core 1.2.0** (branch `pose`, package `pose`, documented in its
`docs/API.md`). It needs five small bytecode patches, in Backport Core's own coremod, each documented in its
`docs/PATCHES.md`. This stream is the rules: when a player swims, how a swimmer moves, and the client's
sprint and input rules.

## Changelog section

*   **Swimming**: sprint with your head under water to swim. You lie flat, only 0.6 blocks tall, with your
    eyes low, and you move much faster, following where you look: up, down or level. Swimming carries on
    while you keep sprinting anywhere in the water, even in water one block deep, and through bumping into
    walls; it stops when you stop sprinting, leave the water, or get too hungry to sprint away from the
    ground. You can start the sprint under water with a double tap of forward or the sprint key, as on land.
*   **Through one-block gaps**: a swimmer fits through holes one block tall, and comes out of them crawling.
*   **Crawling**: with no room to stand, you crawl flat on the ground at sneaking speed and stay down until
    there is room, such as after swimming into a low tunnel, or with a trapdoor closed over you. With room to
    sneak but not to stand, you get up to a crouch first.
*   **Water feels like 1.13**: water slows a sprinting player less, you sink more slowly when you stop
    swimming, sneaking in water takes you down, and looking up at the surface does not lift you out unless
    you jump. Slow Falling slows sinking in water too.
*   **What everyone sees**: other players see you swim and crawl. Swimmers and crawlers are drawn lying along
    the swim with 1.13's stroke, armor and all, and the camera eases down to your new eye height and back up
    instead of jumping.
*   **Riptide**: a spinning player is the same small size as a swimmer while the spin lasts.
*   Players raise a trident over their head to throw it.
*   On a server, the server's own swimming and crawling settings count for everyone on it.

## How it works

*   **When a player swims** (`SwimRules`, Backport Core's `PoseRules`): 1.14.4's `Entity.updateSwimming` and
    `PlayerEntity.isSwimming`. A player who is sprinting with their eyes under water starts swimming (eyes
    under: 1.12.2's `isInsideOfMaterial(WATER)`, which Fluidlogged API extends to water held in other
    blocks); a swimmer keeps swimming while sprinting anywhere in water; never flying, riding or spectating.
    Kept per player object on each side, updated by the engine at the end of each player tick, just before it
    picks the pose (1.14 updates it at the start of the next tick, from the same position). `crawls()` is the
    `crawling` switch; `isSpinAttacking` is the trident stream's `Riptide.isSpinning`.
*   **The pose** (Backport Core): 1.14.4's `PlayerEntity.updatePose` at `PlayerTickEvent` END on the server
    and on the player's own client: swimming, then the Riptide spin, then sneaking; if that pose does not fit
    and the player is not riding or spectating, sneaking if it fits, else the swimming pose (crawling); and
    nothing changes while even the swimming box is blocked. The swimming and spin poses are 0.6 x 0.6 with
    the eyes at 0.4 (sneaking too), set right there, so the next tick's movement, collisions, suffocation
    check and the server's movement check all use that box. Back to standing or sneaking only when that box
    fits, as 1.12.2's own `updateSize` grows a player.
*   **Swim motion** (`SwimMovement`, Backport Core's `PlayerMovement`, called from inside
    `EntityLivingBase.travel` by its patch P1), 1.14.4's numbers: before moving, a swimmer's vertical motion
    eases toward the look's (`motionY += (lookY - motionY) * k`, k 0.085 looking more than a little down,
    else 0.06) unless they look up at nothing but air above their head without jumping; a sprinter in water
    is slowed by 0.9 a tick instead of 0.8 (Depth Strider still applies; Dolphin's Grace is left to the
    dolphin stream, which sets 0.96, as 1.14 does); the pull in water is 0.005 instead of 0.02 and none
    while sprinting; with Slow Falling, while sinking, 0.01 / 16, and a slow sinker settles at -0.003 as in
    1.14.
*   **Sprint rules** (`client/SwimClient.beforeMove`, through patch P2, after vanilla's sprint checks and
    before the player moves): the sprint is decided again by 1.14.4's `ClientPlayerEntity.livingTick` from
    the state the tick began with (captured at `InputUpdateEvent`, plus the last tick's input): a sprint can
    start with `onGround || canSwim`; under water any forward input counts (`> 1e-5`, on land `>= 0.8`); the
    sprint key works in water only with the eyes under; a swim ends only by `!onGround && !sneak &&
    (no forward or no food)` or leaving the water, so walls do not end it; otherwise a sprint in water with
    the eyes out ends. Creative double-jump flight is not toggled mid-swim (the toggle is undone and the
    abilities sent again). Sneaking in water adds 1.14.4's `-0.04 x swim speed`. Fluidlogged API stops every
    sprint in water at the start of the flight check (unless one other swimming mod is installed); this
    runs after it, so the 1.14 rules win.
*   **Input** (`client/SwimClient`, `InputUpdateEvent`): 1.14 slows by pose, 1.12.2 by the sneak key. So
    crawling without sneak is slowed x 0.3, and swimming with sneak held is not (the 0.3 is undone).
*   **The server's switches** (`SwimContent`, packet 75): sent at login and after a config screen change in a
    game this client hosts; a client uses them while connected and its own after it leaves. The server
    checks every move against the box these rules give, so the two must agree.
*   **Trident** (`client/SwimClient`): the trident stream's `TridentPoses.apply` is a Backport Core player
    model hook (patch P3), so it reaches the player model and its armor, before the swim stroke;
    `TridentPoses.playersRaiseArm` turns on once that patch is known to be in, which also gives players the
    point-up `trident_throwing` item model.
*   **Standing down**: with a mod installed that resizes players or adds its own swimming, Backport Core's
    engine stands down and so does all of this (`Poses.active()` is false); vanilla 1.12.2 swimming stays.

## Tested headlessly

`./gradlew runServer -Pselftest=swim` (port 25746): **`SELFTEST DONE pass=50 fail=0`**; `-Pselftest=all` in
a fresh world, rebased on main 1378df4, with Backport Core 1.2.0, Fluidlogged API and this mod's own coremod
all loaded: **`SELFTEST DONE pass=16030 fail=0`** (main's 15980 plus these 50); `python
tools/check_assets.py`: **88 blocks, 5489 states, 90 item models, 380 sound files, 0 errors**. Real server players, each with a connection that keeps what it is sent.

*   Wiring: both switches on by default, the engine active, Backport Core's patch P1 applied in this dev
    environment (beside Fluidlogged API), its coremod found through the dev jar's manifest.
*   Sprint-swimming: under water and not sprinting, no swim (0.6 x 1.8, eyes 1.62); sprinting with the eyes
    under: swimming, pose SWIMMING, 0.6 x 0.6, eyes 0.4, a box 0.6 tall, not crawling; sneaking too: eyes
    still 0.4; sprint stops: stands, 1.8 and 1.62. Eyes above water: no swim; a swim carries on in water one
    block deep; never while flying or riding.
*   Stepping down off the swim: swimming out onto open land stands the player up; a one-block water tunnel
    that drains ends the swim and leaves the player crawling at 0.6 x 0.6; the roof goes and they stand.
*   Crawling: in a one-block gap the player crawls at 0.6 x 0.6, stays down for 200 ticks with their eyes
    never in the roof and no suffocation damage (the first tick, put there standing, is before the pose
    changes, as in 1.14), and stands up when the roof goes. Control: crawling off, the same gap leaves
    vanilla's 1.8 box with the eyes in the roof.
*   The server's movement check: 24 position packets crawling 6 blocks along the gap through
    `NetHandlerPlayServer.processPlayer`, every one accepted with no "moved wrongly"; control: a standing
    player (crawling off) walking into the gap is pulled back ("moved wrongly").
*   Swim motion through vanilla's `travel` and patch P1: slowdown 0.9; vertical eased to a level look by
    0.06 with no pull; to a 45-degree-down look by 0.085; up under water; no lift at the surface looking up
    without jumping, a lift with jump; not sprinting 0.8 and a 0.005 pull; Slow Falling 0.01 / 16 while
    sinking and -0.003 at rest; sprinting in water without swimming still 0.9 and no pull; Dolphin's Grace
    leaves vanilla's slowdown for the dolphin stream.
*   Riptide: a spin makes the player SPIN_ATTACK, 0.6 x 0.6, eyes 0.4; standing again after it.
*   Switches: swimming off mid-swim gives vanilla's 1.8 at once; packet 75 round trip (1 byte); the server's
    switches win over the client's own, and the client's count again after leaving.
*   Backport Core's own tests (its `-Pselftest`, port 25761): the pose engine with test rules, the pose packet
    round trip, the movement hooks reached from vanilla's `travel` for players and not pigs, and all five
    patches through the dev transformer chain beside Fluidlogged API and on SRG names (its `docs/PATCHES.md`).

## Not tested

*   Everything drawn and everything only a client runs (no client headlessly): the body turn, the stroke and
    head, armor following it, the camera easing, the trident raised, the crawl and swim input speeds, the
    1.14 sprint rules and creative-flight guard on a real client, sneaking sinking. Their patches are proven
    to apply and verify (dev chain with Fluidlogged API, and SRG names); the code is in `swim.client` and
    Backport Core's `pose.client`, and the dedicated server boots without reaching it.
*   Poses reaching other players' clients over a real connection (the packet is only encoded and decoded),
    and remote players' animation from it.
*   A swim through real client movement over time (the motion is tested one tick at a time on the server's
    copy of a player).

## Limits

*   1.14's shorter crouch (1.5 blocks, with a forced crouch under low ceilings at sneaking speed) is a 1.14
    change this stream does not make: sneaking keeps 1.12.2's 1.65 box, and a player with 1.65 to 1.8 blocks
    of room is stuck at the sneaking size (as 1.12.2 already does) without being slowed.
*   Mobs keep 1.12.2's water pull (0.02); only players get 1.13's. Swimming statistics and hunger keep
    1.12.2's diving numbers.
*   A swimmer's cape still bends as if sneaking when sneak is held.

## For the integrator

*   `UpdateAquaticBackport.preInit`: one line, `SwimContent.preInit(event.getSide().isClient())`; the
    dependency is now `required-after:backportcore@[1.2.0,)`; `gradle.properties` `core_version=1.2.0` (copy
    Backport Core 1.2.0's dev jar from branch `pose` into `deps/`).
*   `NetworkHandler.init`: packet 75 registered.
*   `ModConfig`: the new `swimming` category (`enabled`, `crawling`).
*   `selftest/SelfTestRunner.TESTS`: one line, `new SwimSelfTest()`.
*   `en_us.lang`: a `# --- swim ---` section from `tools/swim/generate.py` (the category's name and tooltip
    are there too, not in the file's header). English only: the 18 languages are the language tool's.
    `tools/lang/vanilla_keys.json` regenerated (it was behind the merged lang file); all six swim keys are
    ours.
*   No transformer in this mod's `asm/UpdateAquaticPlugin`: all five patches are Backport Core's (its own
    plugin, its own jar). `docs/PATCHES.md` here has a section pointing to them.
*   `docs/MIXIN_DECISION.md` still says swimming needs no patching; the standing rules of 2026-11-07 replaced
    its "no ASM" line, and Backport Core's `docs/PATCHES.md` now has the patches.
*   With Backport Core's P1 in place, the dolphin stream's `DolphinsGrace` could move into a
    `PlayerMovement.waterSlowDown` and be exact on the same tick; it is left as it is.
