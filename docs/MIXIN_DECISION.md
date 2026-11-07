# Swimming, crawling and waterlogging: the chosen design

**Decided by the user (2026-11-06):** all three are our own code, written from scratch. No MixinBooter, no
mixins, and no code taken from, adapted from or credited to any other mod. Waterlogging is our own system,
with its framework in Backport Core, and it must keep working when Fluidlogged API is installed, without
depending on it.

Nothing below is built yet. The user reviews this design first. If any part turns out to need bytecode
patching, that part stops and goes back to the user before any ASM is written. The parts that already look
that way are listed at the end.

## Swimming and crawling

### What 1.12.2 already gives us

Forge calls the player post-tick event (`PlayerTickEvent`, phase END) as the **last line of
`EntityPlayer.updateSize()`**, which runs once per tick on both the client and the server. Whatever size we
set there is the player's size until the next tick's movement is done. Two more vanilla rules help:

- `updateSize()` only grows a player back when the bigger box fits. A player who is 0.6 tall in a
  one-block gap stays 0.6 tall on their own, until there is room to stand.
- `getEyeHeight()` already returns 0.4 for a non-sneaking player whose height is 0.6 (vanilla's elytra
  rule). The public Forge field `eyeHeight` covers the sneaking case.

So the pose system needs no patch.

### Design (package `swim`, pose engine shared through Backport Core)

| Part | How |
| --- | --- |
| **Pose state** | One per player: `STANDING`, `SWIMMING` or `CRAWLING`. It is worked out the same way on both sides every tick: swimming is sprinting with eyes in water (1.13's rule); crawling is being unable to fit the standing box when the swimming box fits (1.14's rule). The server sends each player's pose to the clients tracking them, using one packet in our id block, so other players are drawn right. |
| **Size and eyes** | At `PlayerTickEvent` END: `setSize(0.6F, 0.6F)` (protected, called through reflection, resolved once) for SWIMMING and CRAWLING. `eyeHeight` is set so the eyes sit at 0.4 even while sneaking, and is restored on STANDING. Both sides apply it, so the server's own movement check (`NetHandlerPlayServer.processPlayer`) uses the same box as the client and never rubber-bands a crawling player. |
| **Swim motion** | Client side, where 1.12.2 moves players: at `PlayerTickEvent` END (after vanilla's water movement) we apply 1.13's swimming changes to the motion: less water drag while sprinting, vertical motion following the look direction, sinking faster while sneaking, and Dolphin's Grace when that stream exists. The server needs nothing: it trusts the client's movement within its normal limits, and swimming stays inside them. |
| **Crawl motion** | `InputUpdateEvent` (client) scales forward and strafe input to the sneaking speed while crawling. |
| **Starting and stopping** | Swimming stops when the player stops sprinting, leaves the water or runs out of hunger to sprint. Crawling stops when there is room to stand. Both are re-checked every tick, the way 1.14 decides the pose. |
| **Third-person look** | `RenderPlayerEvent.Pre/Post` rotates the whole body to lie along the look direction (pitch-following when swimming, flat when crawling) around the player's position. The swimming and crawling arm and leg animation come from our own `ModelPlayer` subclass. It is put into both player renderers' `mainModel` field (default and slim skins) once, at client setup. It draws exactly as vanilla's unless the player is swimming or crawling. |
| **First person** | Nothing extra: the camera follows the eye height. |
| **Config** | A new `swimming` category: `enabled`, `crawling`. |

### Risks

- **Mods that also resize the player or replace the player model** (morph mods, animation mods, and any
  other mod that adds swimming). We detect the known ones by mod id and stand down quietly, never naming
  them in game. When our model is not in the renderer at draw time, we fall back to rotating the body only.
- **Hitbox changes in the middle of a tick.** Vanilla's `updateSize()` may grow the box back for a moment
  just before our END handler shrinks it. No movement or collision runs between the two, so this is safe.
  The self-test must still prove it: a player crawling in a one-block gap for 200 ticks, never taking
  suffocation damage.

## Waterlogging

### What "our own waterlogging" can mean on 1.12.2 without patches

A 1.12.2 chunk holds one state per position. Without patching, a block and water can share a space only
if **the block's own state says it is waterlogged**, and the block then *is* water to the rest of the game.
This is the technique Update Aquatic already uses for kelp, seagrass and coral (`common/BlockPseudoWater`).
It works well:

- the block's material is water while waterlogged;
- it carries vanilla water's `level` property, fixed at 0;
- so swimming, drowning, fog, boats, fish, flow, source forming and buckets all treat it as still water.

It only works for **blocks we write**. It cannot reach vanilla's blocks.

### Design (framework in Backport Core; Update Aquatic supplies the water mechanics)

| Part | How |
| --- | --- |
| **`IWaterloggable` + `Waterlogging` helper** in Backport Core | A block adds 1.13's `waterlogged` property and the fixed `level` property. It keeps one metadata bit for `waterlogged`, chosen so existing metadata keeps its meaning (required for blocks already in worlds, such as the family's lanterns and sea pickles). The helper supplies: material and light opacity by state; the bucket rules (fill, drain, refuse); placing into a water source waterlogs; breaking or exploding leaves water; and one shared model-name mapping. |
| **Spreading** | When a block becomes waterlogged it schedules a tick. That tick starts vanilla flowing water in the open spaces beside and below it, as a 1.13 source would. From then on vanilla's own water keeps flowing, because it reads our block as a source (`level` 0). |
| **Drawing the water** | A client-only baked-model wrapper, registered through Forge's model bake event for our blocks only, by exact name. It adds water faces (still-water texture, biome tint) on every side that borders something not water, and the surface at 8/9 height when nothing water-like is above. This removes the one gap the current kelp and coral blocks have: water not drawn when seen from the side through glass or air. |
| **Who uses it** | Update Aquatic's kelp, seagrass, coral, bubble columns and sea pickles. Later backports' own lanterns, campfires, chains, scaffolding and other blocks that 1.13+ made waterloggable. Each of those mods calls the framework; Backport Core adds no blocks. |
| **Config** | A new `waterlogging` category in Backport Core: `enabled`. Off, blocks still register the property, so worlds stay loadable, but they never take water. |

### Fluidlogged API installed

- **No double-patching** is possible, because we patch nothing.
- **Our blocks opt out of its fluidlogging.** It publishes an `IFluidloggable` interface and a
  `FluidloggableEvent`. A listener class loaded only when `fluidlogged_api` is present tells it our blocks
  are not fluidloggable: they already manage their own water, and its water on top of ours would be drawn
  and simulated twice. We compile against a small stand-in of its event, kept out of our jar, so there is
  no dependency. (Village & Pillage does the same for its JEI and Mouse Tweaks hooks.)
- **Vanilla blocks stay its business.** When it is installed, players get its waterlogged stairs, slabs and
  fences, and ours never touches them.
- **Must be tested in a real instance.** Its coremod cannot load in the FG2 dev environment. E2Eu-Cleanroom
  already has v3.3.3. Things to check there: our pseudo-water blocks beside its fluidlogged blocks (flow,
  rendering, buckets), and what its fluid-collision and renderer changes do with a block whose material is
  water.

## Looks infeasible without patching (for the user to decide)

1. **Waterlogging vanilla blocks** (stairs, slabs, fences, glass panes, chests, ladders, signs, rails, ...).
   Their state containers are vanilla's, and 1.12.2 keeps one state per position. The only patch-free
   route is replacing the vanilla blocks in the registry with our own subclasses that have the extra
   property. Stairs have a free metadata bit; stone slabs have none. Replacing vanilla blocks breaks other
   mods that compare against `Blocks.OAK_STAIRS` and the like, and changes every world's block mapping.
   **Not recommended.** The real fix is a second state layer per chunk (what Fluidlogged API does), which
   needs bytecode patches to `World`, `Chunk`, rendering and physics. **Proposal:** our waterlogging covers
   our blocks and the family's. Vanilla blocks are waterlogged only when the player installs Fluidlogged
   API.
2. **Items and experience orbs floating in water** (stream A). `EntityItem` and `EntityXPOrb` have no
   update event. The only patch-free route is moving every loaded item from a world tick event, which costs
   performance and is a little out of step. **Proposal:** do it that way behind a config switch, default
   on, limited to entities in water, or skip it.

Nothing in swimming or crawling looks like it needs patching.
