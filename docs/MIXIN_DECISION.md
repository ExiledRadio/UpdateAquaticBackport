# Swimming, crawling and waterlogging: the chosen design

> **Superseded on patching (2026-11-07):** the user's standing rule is now "closest to vanilla, always", with our own small
> ASM patches allowed wherever parity needs them (no MixinBooter). Swimming and crawling use five patches in Backport
> Core's coremod, and Update Aquatic has three of its own; see `docs/PATCHES.md`. Waterlogging still comes from
> Fluidlogged API as below.

**Decided by the user (2026-11-06):**

- **Swimming and crawling** are our own code, written from scratch. No MixinBooter, no mixins, and no code
  taken from, adapted from or credited to any other mod.
- **Waterlogging** uses Fluidlogged API for everything, as a required dependency of Backport Core. This
  replaced the earlier choice of our own system ("why reinvent the wheel").

Nothing below is built yet. If any part of our own code turns out to need bytecode patching, that part
stops and goes back to the user before any ASM is written. Fluidlogged API's own patches are its business,
not our code.

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

## Waterlogging: Fluidlogged API

### What it is (checked 2026-11-06)

| | |
| --- | --- |
| **Current version** | **3.3.3** for 1.12.2, released 2026-07-16 (CurseForge project 485654, file 8445863). It is also on Modrinth and GitHub (`jbredwards/Fluidlogged-API`, branch `1.12.2-Latest`). There are no git tags. |
| **License** | The author's *Mod Permissions License* v1. It allows running it and depending on it, and copying parts of it into other software only "for the purpose of improving compatibility". It does **not** allow redistributing it: it must stay a separate download (a CurseForge *required dependency*), never bundled or shaded into our jars. Depending on it the way we plan is allowed. |
| **What it needs** | Nothing. Its `@Mod` declares no dependencies, and it contains no mixins and no mixin configs. It is an ASM coremod of its own (`FMLCorePlugin: ...asm.ASMHandler`, about 370 transformer classes, with an access transformer). It is needed on both client and server. |
| **Cleanroom** | Runs. v3.3.2 loads its plugin in all four DregoraCleanroom instances on Cleanroom 0.6.13 (the Celeritas instance booted today), and v3.3.3 in E2Eu-Cleanroom. Every one of those instances also has Fugue; it is not proven whether Fluidlogged API needs Fugue on Cleanroom. |
| **Dev environment** | Its README supports ForgeGradle 2.3 with `stable_39` mappings (ours) through `deobfCompile`. So unlike Future MC it should load in our `runServer`, and the adapter can be tested headlessly. Not yet tried; it is Backport Core's first check. |
| **API for our blocks** | `api.block.IFluidloggable`: a block that implements it decides its own fluidlogging. It answers `isFluidValid` / `isFluidloggable`, `canFluidFlow` / `canFluidConnect` per side, `shouldFluidRender`, and gets the callbacks `onFluidFill` / `onFluidDrain` / `onFluidChange`. `overrideApplyDefaultsSetting()` returning true makes its own rules win over the player's config. `api.block.BlockWaterloggedPlant` is its base for plants that only live in water. `api.util.FluidloggedUtils` holds `getFluidState`, `setFluidState`, `setFluidToAir`, `getFluidOrReal` and `canFluidFlow`. `api.util.FluidState` is a (fluid, block state) pair, `FluidState.of(...)`, with `EMPTY`. `api.event.FluidloggableEvent` (has a result) lets outside code allow or deny a block. `FluidloggedEvent` and a config event round it out. |
| **Defaults** | Out of the box it fluidlogs a long list of vanilla blocks and their "properly coded" modded subclasses: slabs, walls, fences, panes, trapdoors, signs, chests, rails, buttons, leaves and more. Stairs are **not** on that default list. It can download community configs, which may add more. |

### Design

- **Backport Core requires Fluidlogged API** (`required-after:fluidlogged_api@[3.3.3,)`), so every family mod
  does too, through Core. Our own waterlogging system is dropped: no `waterlogged` property of ours, no
  water-material trick, no water-drawing model.
- **One adapter in Backport Core**, `Waterloggable` (see `docs/BACKPORT_CORE_WATERLOGGING.md` for the exact
  API). It extends Fluidlogged API's `IFluidloggable`, so Fluidlogged API sees our blocks as its own. It
  bridges to three simple choices per block: whether it can hold water; whether it needs water (kelp,
  seagrass) or merely may have it (lanterns, slabs); and what happens when its water comes or goes (coral
  dying, sea pickles lighting). A small `Waterlogging` helper reads and sets a position's water for
  generation and tests.
- **Every family block that 1.13+ makes waterloggable** implements `Waterloggable`:
  - Update Aquatic: kelp, kelp plant, seagrass, tall seagrass, coral and coral fans (all four forms), sea
    pickles (our copy), bubble columns;
  - Village & Pillage: sea pickles, lanterns, campfires, scaffolding, the stone slab, stair and wall sets;
  - later backports: chains and the rest.
- **Vanilla blocks** get waterlogging from Fluidlogged API itself, by its defaults and configs. Stairs are not
  among its defaults. To match 1.13 for vanilla stairs, Backport Core would add them through Fluidlogged API's
  config (its whitelist, or its config event). That needs no patch; it is a choice for the user.
- **No mixin and no ASM in our code.** Everything above is plain Java against Fluidlogged API's published
  API.

### What changes in Update Aquatic

The mod is unreleased, so its blocks can change state freely; no world needs converting.

- `common/BlockPseudoWater`, `PseudoWaterEvents`, the `level` and `surface` properties, the water-surface
  model and the water tint all go.
- Kelp, seagrass and coral become ordinary plant and coral blocks implementing `Waterloggable`, with their
  water held by Fluidlogged API:
  - kelp and seagrass *need* water: drained, they break;
  - coral and fans *may* have it, and dry ones die as now (the six-face rule stays);
  - their own `waterlogged` property goes, since the water's presence is the state.
- Generation places blocks with their water through the helper. The tests run with Fluidlogged API in the dev
  environment.
- This is one follow-up stream ("Wl-UA"), after Core ships the adapter.

### For Village & Pillage (owner of released blocks)

Its sea pickle has a stored `waterlogged` property in worlds that already exist. When it moves onto the
adapter, it keeps the property for metadata compatibility and moves the water across once. A pickle that says
waterlogged but has no Fluidlogged API water gets that water as its chunk loads, and from then on its light
follows the real water. Backport Core can offer that conversion as part of its migration code.

## Still open (for the user to decide)

1. **Items and experience orbs floating in water** (stream A). `EntityItem` and `EntityXPOrb` have no update
   event. The only patch-free route is moving every loaded item from a world tick event, which costs a little
   performance and runs slightly out of step. **Proposal:** do it that way behind a config switch, limited to
   entities in water, or skip it.
2. **Vanilla stairs** waterlogged as in 1.13: add them to Fluidlogged API's config from Backport Core, or leave
   it to the player's own config.

Nothing in swimming or crawling looks like it needs patching.
