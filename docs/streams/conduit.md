# Stream N: conduit

Package `conduit` (client side in `conduit.client`). Config `conduit` (`enabled`; no new fields). No packets
or entity ids used: the conduit's target reaches the client in its tile entity's update packet.

## Changelog section

*   **Conduit**: craft it from 8 nautilus shells around a heart of the sea. Place it in water and build a
    frame of prismarine, prismarine bricks, dark prismarine or sea lanterns around it - three 5x5 rings, one
    in each direction, with the conduit in the middle and water in the 3x3x3 around it. 16 frame blocks
    wake it; every 7 blocks add 16 blocks of range, up to 96 with the full 42.
*   Players in water or rain within its range get **Conduit Power**: they do not run out of air under
    water (and get it back there, as today's Java), see under water as with Night Vision, and mine as with
    Haste.
*   With a full frame its eye opens and it attacks one hostile mob in water or rain within 8 blocks, 4
    damage every 2 seconds.
*   It gives light 15, can be waterlogged (a bucket fills or empties it), drops itself (explosions too)
    and cannot be pushed by pistons.
*   **Nautilus shells** can be fished up as treasure. The **heart of the sea** comes from buried treasure
    (once ocean structures are in).

## How it works

*   `BlockConduit` is a `common/BlockPseudoWater` with `waterlogged` (default dry, as today's): waterlogged,
    it is the water in its own space, which the conduit needs, since its own block is the middle of the
    3x3x3 water check. Dry, it is glass, so flowing water does not enter it. The block model is 1.18's empty
    one (a particle texture) plus the shared water surface at the top of the water; the tile entity's
    renderer draws it.
*   `TileEntityConduit` is 1.14.4's `ConduitTileEntity`: every 40 ticks of world time (both sides) the
    water and frame check; on the server Conduit Power (260 ticks, ambient) to players within
    `frame / 7 * 16` who are wet (`isWet`: in water or rain), and with 42 frame blocks the hunt (`IMob`
    and wet, inside a 17-block box, kept until dead or 8 blocks away, 4 magic damage). Sounds come from
    the server; the client draws, turns and spawns nautilus particles from its own copy of the check.
*   Frame blocks are 1.12.2's prismarine block (all three variants) and the sea lantern, fixed as in
    vanilla (Forge 1.14's `supports_conduit` tag has no 1.12 counterpart).
*   **Conduit Power** (`PotionConduitPower`, color `#1DC2D1`):
    *   *breath*: 1.12.2 lowers air after the living update event, so `ConduitPowerEvents` adds 4 air plus
        the one vanilla is about to take (today's Java refills under water since 1.21.4; 4 a tick is
        1.13's refill rate);
    *   *mining*: `BreakSpeed` swaps vanilla's Haste factor for the stronger of Haste and Conduit Power,
        20% a level, never both (1.14.4's `EffectUtils.getMiningSpeedup`);
    *   *seeing under water*: 1.12.2's light map and fog color ask only "is Night Vision active". While
        the eyes are in water and no real Night Vision is active, `ConduitClient` puts a Night Vision entry
        in the client player's effect map at the start of each frame and takes it out when the HUD or a
        screen starts to draw (or the frame ends). Only the world render sees it; no tick, packet or effect
        list does. The brightness is full at once (1.14 ramps it up over a player's first 30 s in water).
    *   its icon is 1.18's `mob_effect/conduit_power`, drawn through Forge's `renderInventoryEffect` /
        `renderHUDEffect` (client-only methods, stripped on a server).
*   **Renderer** (`RenderConduit`): 1.14.4's renderer with 1.18's textures - the closed shell, or the
    bobbing cage, the 22-frame wind in three orientations and the camera-facing eye, open with a full
    frame. 1.18's wind strips are 64x704, the same layout 1.14's 64x1024 sheet used. The item uses 1.18's
    `builtin/entity` model and an item renderer that draws the closed shell.
*   **Nautilus particle** (`ParticleNautilus`): 1.12.2's enchanting-table motion (the same class 1.14 uses
    for both) with 1.18's `particle/nautilus` sprite stitched into the block atlas; respects the particle
    setting and the 32-block limit.
*   **Items**: nautilus shell, heart of the sea and the conduit are Uncommon (today's rarity: 1.21.2 made the
    conduit and the shell Uncommon). The shell joins the fishing treasure pool with weight 1, beside the six
    1.12.2 has, as 1.13 added it (today's "open water only" rule needs 1.16's open-water check and is left
    out; today's Java also moved lily pads to junk, which is not an Update Aquatic change and is untouched).

## For the integrator and other streams

*   **Nothing in `common/` needs to change.**
*   `tools/lang/vanilla_keys.py` on `main` has no rule for `effect.<modid>.<name>`, so it lists
    `effect.updateaquaticbackport.conduit_power` ("Conduit Power", 1.18's `effect.minecraft.conduit_power`)
    as one of ours. `build_langs.py` on `ua-languages` already maps `effect.` keys, so the family tool
    translates it from Mojang's files; `vanilla_keys.py` needs the same rule (the turtle, dolphin and
    phantom streams' effects will hit it too). `tools/lang/vanilla_keys.json` was regenerated for the check
    and then left as on `main`; rerun `python tools/lang/vanilla_keys.py` after merging.
*   The 18 language files were not generated: `build_langs.py` is on `ua-languages`, not on `main`.
    Every new key is in `en_us.lang` (the `# --- conduit ---` block from `tools/conduit/generate.py`).
*   By name, for other streams: the drowned's off-hand shell (stream R) is `Owners.ours("nautilus_shell")`;
    buried treasure's heart (stream S) is `Owners.ours("heart_of_the_sea")`; until S exists the heart, and so
    the conduit, cannot be had in survival. For the Village & Pillage maintainer: the wandering trader's
    nautilus shell can list `updateaquaticbackport:nautilus_shell`.
*   Another mod that looks up Conduit Power can find it as `updateaquaticbackport:conduit_power`.

## Tested headlessly

`./gradlew runServer -Pselftest=conduit` (port 25739), then `python tools/check_assets.py`: **`SELFTEST DONE pass=167 fail=0`**;
with every stream's test in one run (`-Pselftest=all`) **`pass=1035 fail=0`**; **75 blocks, 5274 states, 66 item
models, 75 sound files, 0 errors**.

*   Registration: block, item, both items, Conduit Power (color, name key, not harmful), tile entity id,
    the five sound events; all three items Uncommon and stacking to 64; every stored state survives a
    metadata round trip; default state dry; waterlogged is water, dry is glass; hardness 3, blast resistance
    3, light 15, pickaxe, pistons blocked, not passable, the 6x6x6 pixel box, a tile entity.
*   Recipe: 8 shells around a heart make a conduit, 9 shells nothing. The fishing treasure table, rolled
    7000 times, gives a nautilus shell about 1 in 7 times.
*   Placing by a real player's item use: under water waterlogged with its tile entity, on land dry.
    Breaking by a real player: under water leaves water, on land air, both drop the conduit; four
    explosions break it and every one drops it.
*   The frame: exactly 42 positions, all on the 5x5 rings; prismarine, bricks, dark prismarine and sea
    lanterns count, glass does not. In a pool: 15 blocks inactive, then 16, 20, 21, 28, 35, 41 and 42 with
    ranges 32, 32, 48, 64, 80, 80, 96, the eye open only at 42; a mixed full frame is full; dry (bucket
    state) it stops and keeps its tile entity; a stone block in the 3x3x3 stops it, flowing water there does
    not. Its own tick: across 40 ticks of world time it checks exactly once.
*   Conduit Power: real players spawned in the world - in the pool gets it (260 ticks, level I, ambient
    with particles); 31 blocks away in range 32 gets it, 33 away does not, a dry one in range does not. A
    pig ticked under water: loses air without it; with it gains exactly 4 a tick, refills to full, and at
    -15 air does not drown. Digging: Conduit Power I is 1.2x; with Haste II it is Haste II's 1.4x, not both;
    Conduit Power II with Haste I is 1.4x.
*   Hunting: a full frame targets a zombie in water, 4 damage, leaves a pig alone, hits the same target
    again; the update tag carries the target; a fresh tile entity read from the saved NBT finds the same
    zombie by its id and hits it; a target 8 blocks away is dropped; a dry zombie in range is not
    targeted; 41 frame blocks: active but no hunting; back to 42 it hunts; a dead target is dropped.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything visual (no client): the renderer (shell, cage, wind, eye, their rotations and animation),
    the item renderer and its display transforms, the water surface on a waterlogged conduit, the nautilus
    particles, Conduit Power's icon in the inventory and on the HUD, and seeing under water (the borrowed
    Night Vision in the light map and fog).
*   Sounds playing (the events are registered and their files checked by `check_assets.py`).
*   The client predicting breath and mining speed for its own player (the same event code runs there).
*   Fishing with a real rod (the loot table itself is tested).

> **Since waterlogging (stream Wl, 2026-11-07):** these blocks no longer use `common/BlockPseudoWater`; they hold real water through Backport Core's `Waterloggable` adapter over Fluidlogged API. See `docs/streams/waterlog.md`.
