# Stream B: bubble columns

Package `bubble` (client parts in `bubble/client`). Config `bubbles` (`enabled`, `generate`). No entity ids
or packets used (45-49 and 55-59 stay free).

## Changelog section

*   **Bubble columns**: soul sand under water sends a column of bubbles up to the surface; a magma block
    under water makes a whirlpool that pulls down. The column appears a second after the block is placed (or
    after water covers it) and fills the whole depth at once; it goes when its block does.
*   Swim into an upward column to be carried up and thrown out of the water at the top; a whirlpool drags
    you to the bottom. Falling into a column never hurts. Your air comes back while your head is in a
    column, just as above water.
*   A boat on a whirlpool shakes for three seconds, then sinks and throws its passengers off. On an upward
    column it is thrown into the air.
*   Bubbles rise and spiral in the columns, with their own flowing, whirling, "woosh" and "zoom" sounds.
*   An empty bucket takes the water from a column. A block placed into a column replaces it.
*   Magma blocks that world generation leaves under water (in newly generated chunks) start their columns
    straight away.

## How it works

*   **The block is the water**: the column is a `common/BlockPseudoWater` like kelp, always water, level 0.
    It has 1.13's `drag` property (metadata 0 whirlpool, 1 upward). It draws only the shared water surface
    when it is the top block, over an empty model; its own ray trace is a liquid's (an ordinary click goes
    through, a bucket stops on it), it is replaceable, and a bucket empties it to air (`drained`).
*   **Today's whole-column update** (`BubbleColumns.updateColumn`): a change below a column block, or a new
    water source above the top, schedules that block 5 ticks out; its tick sets it and every column or water
    source above it to what the block below asks for (column with the same drag, upward over soul sand,
    whirlpool over magma, still water over anything else). Flag 2, so observers see it and nothing else is
    updated; blocks that go back to water get a neighbor update so water flows on from them. Columns stop at
    flowing water, kelp, coral and anything else that is not a plain source.
*   **Soul sand and magma stay vanilla's**: `BubbleEvents` sees them placed, or water placed over them
    (`NeighborNotifyEvent`), and puts them on a list of its own, worked through by the world tick 20 ticks
    later (1.14.4's tick rate). Vanilla's scheduled tick is not used for them: it would run 1.12.2's magma
    tick, which boils the water above away.
*   **Pushing** (1.14.4's `Entity.onEnterBubbleColumn` numbers, unchanged today), from the block's
    `onEntityCollision`, which every entity's move calls on both sides: inside, +0.06 up to 0.7 or -0.03 down
    to -0.3, fall distance reset; in the top block under air, +0.1 up to 1.8 or -0.03 down to -0.9, with
    splash and bubble particles from the server.
*   **Air**: at the start of a living entity's tick, if its eyes are in a column, its air goes up by 4 (plus
    the one point vanilla's air rule is about to take). Full air shows as 299, ten full bubbles.
*   **Boats** (`BoatRocking`, 1.14.4's `BoatEntity.updateRocking`): kept in a weak table per side, since a
    1.12.2 boat has no field for it. A boat touching the top of a column rocks for 60 ticks; then a whirlpool
    adds -0.7 and throws off its passengers, an upward column sets 2.7 (player aboard) or 0.6. Leaving the
    column stops the count. The client counts the same from its own collisions to draw the tilt
    (`client/RenderBoatRocking`, wrapped around whatever boat renderer is installed on the first client tick).
*   **Particles and sounds** (`client/BubbleEffects`, 1.14.4's `animateTick`): two rising bubbles per display
    tick in an upward column, one spiralling bubble in a whirlpool (1.12.2's bubble sprite), the ambient sound
    1 in 200, following the particle setting as 1.14.4's optional particles. The enter sounds are 1.14.4's
    `BubbleColumnAmbientSoundHandler`, on the client tick.
*   **Generation** (`BubbleGenerator`, weight 1000 so it runs after the other generators): scans the area a
    chunk's population wrote for soul sand and magma with a water source above and starts their columns.
    Off in dimensions where water boils away, and with `bubbles.generate` off.

## Tested headlessly

`./gradlew runServer -Pselftest=bubble,kelp,coral,woodset`, then `python tools/check_assets.py`:
**`SELFTEST DONE pass=932 fail=0`** (with the woodset, kelp and coral tests in the same run), **75 blocks, 5274 states, 63 item models, 72 sound files, 0 errors**. `python tools/lang/vanilla_keys.py` lists the block name and the four subtitles as vanilla names.

*   Registration: the block, no item, multipart, water material, level 0 in every state, both stored states
    round-trip through metadata, four sounds registered, replaceable, destroyed by pistons, no drops.
*   Soul sand and magma placed in a 12-deep pool wait exactly 20 ticks (not due at 19), then fill all 12
    blocks at once with an upward column and a whirlpool, stopping at the surface; the top block shows the
    water surface, the one below does not. Soul sand placed without updates waits for nothing; water placed
    over it starts the wait. No column through flowing water; a column stops under kelp and kelp stays.
*   Pushing, each number and cap above, called on an item directly; fall distance reset inside, kept on top.
    For real: an item rises more than a block in 40 ticks in an upward column; a pig is pulled down a
    whirlpool while a pig beside it in plain water swims up.
*   Air: a pig with its eyes in a column goes from 100 to 140 air in 10 ticks and stops at full; in plain
    water it loses 10.
*   Boats: a boat with a pig aboard starts rocking on a whirlpool, sinks on the 60th tick (motion -0.7 or
    lower) and loses its passenger; an empty boat on an upward column is thrown up at 0.6; a boat moved off a
    whirlpool after 30 ticks stops rocking and does not sink.
*   Changing the base: soul sand swapped for magma changes nothing at once, the whole column 5 ticks later;
    the base replaced by stone turns the whole column back to water; the base dug out lets the water flow down
    into it and leaves no column; a new water source over the top joins the column.
*   An ordinary ray trace from above goes through the column to its base; an empty bucket used by a real
    player fills from the column and leaves air, the column below stays.
*   Generation: magma and soul sand set under water without updates get their columns from the generator's
    scan; a second scan starts nothing.
*   The server booting at all proves no client type is reached from server code.

## Not tested

*   Anything drawn (no client): the water surface on the top block, the rising and spiralling particles, the
    boat's tilt (`RenderBoatRocking`) and the splash particles.
*   Sounds playing: ambient flow and whirl, the "woosh" and "zoom" on entering, the boat splash.
*   A real player being carried up and thrown out (the client moves its own player; the same collision code
    is tested on items and pigs), and a player-steered boat thrown up at 2.7 (needs a client to steer).
*   Generation in real terrain: nothing in vanilla 1.12.2's overworld puts magma or soul sand under water, so
    this waits for the ocean stream's underwater ravines.

## Known limits

*   The list of bases waiting their 20 ticks is not saved: a base waiting when the server stops starts its
    column at its next change.
*   Water over soul sand that becomes a source without a block update (vanilla 1.12.2's infinite-source rule
    sets it quietly) is not noticed until something next to it changes.
*   Water does not flow out of the side of a column into a hole dug next to it (the column is not a vanilla
    liquid; the same limit as kelp). Once the column goes, its water flows normally.
*   Vanilla 1.12.2's magma block still boils away plain water above it on a random tick when there is no
    column (for example flowing water over it). A column above it is safe.
*   Boats from other mods with their own renderer do not tilt (they still sink and are thrown).

## For the integrator

*   No change to `common/` is needed. The column extends `common/BlockPseudoWater` and overrides `drained`
    to return air, which `common/PseudoWaterEvents` already uses for the bucket.
*   Stream Wl (waterlogging): the column moves onto the `Waterloggable` adapter with kelp; `BubbleColumns.
    canExistIn` should then also accept Fluidlogged API's source water if columns are to pass through it
    (1.13's stop at waterlogged blocks, so no change is needed for parity).
*   Stream O (ocean terrain): magma at the bottom of underwater ravines gets its columns from
    `BubbleGenerator` if it is placed during generation or by an `IWorldGenerator` lighter than weight 1000.
*   Stream A (water look and feel): if its gradual air refill also runs with the eyes in a column, the two
    add up; A should skip entities whose eye block is `updateaquaticbackport:bubble_column`, or this stream's
    `BubbleEvents.onLivingUpdate` can be dropped in favor of A's rule.
*   Languages: `en_us.lang` lines come from `tools/bubble/generate.py` (block name, four subtitles, the
    `generate` config name and tooltip). The other 17 languages were not written here; run the family's
    language tool (`tools/lang/build_langs.py`, on `ua-languages`) after merge.
