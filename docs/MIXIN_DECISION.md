# Decision needed: swimming, crawling and waterlogging

These three 1.13 features change the player and the world at a level no Forge event reaches, so they need
mixins (through MixinBooter) or a library that already has them. Nothing below is built yet. Everything
else in `PLAN.md` is built without either.

## What each one needs

| Feature | What has to change | Size |
| --- | --- | --- |
| **Swimming** (sprint underwater, 0.6-block hitbox, swim animation) | the player's pose, size and eye height every tick, its movement in water, the third-person model and first-person camera | medium: about 15 mixin targets, half of them client |
| **Crawling** (under one-block gaps) | the same pose system, plus the collision check that forces it | small once swimming exists |
| **Waterlogging** (a block and water in one space) | where a chunk stores a second state per position, every `getBlockState` / `setBlockState` caller that must see the water, fluid flow and ticking, fluid rendering inside non-liquid blocks, entity physics, buckets, saving and loading | large: tens of targets across world, chunk, rendering and physics |

## What exists on 1.12.2

- **Aqua Acrobatics** (Fuzs, maintained by embeddedt): 1.13 swimming, crawling and 1.14's sneak height.
  Mixins, needs a Mixin provider (MixinBooter or MixinBootstrap). **Public domain** (the Unlicense), so its
  code can be copied, changed and shipped by anyone. Already in your DregoraCleanroom-Celeritas (1.15.4)
  and E2Eu-Cleanroom (1.15.2) instances. Known conflicts: Better Sprinting and Idō; a few open reports with
  specific mods (Spartan Weaponry).
- **Fluidlogged API** (jbredwards): full fluidlogging for any block and any Forge fluid, with rendering,
  physics and flowing fluids, plus many fluid fixes. ASM coremod. **Mod Permissions License**: other mods
  may depend on it and add compatibility, but may not copy or redistribute its code. Already in five of
  your instances (v3.3.2 / v3.3.3), including E2Eu.

## Options

### 1. Write all three ourselves

- **Cost**: the largest. Swimming and crawling are about a week of careful work. Waterlogging is a library
  in its own right; Fluidlogged API is the proof of how much it takes to get right.
- **Risk**: every big pack that already has Aqua Acrobatics or Fluidlogged API would have two mods
  patching the same methods. We would have to detect both and stand down, so the code we wrote would sit
  unused exactly where packs are largest.
- **Gain**: no outside dependency, full control.

### 2. Build on a library for one part (waterlogging through Fluidlogged API)

- **How**: Backport Core gets a small `Waterlogging` interface with two backends. With Fluidlogged API
  installed, the family's blocks (kelp, coral, sea pickles, lanterns, campfires, chains, scaffolding)
  register as fluidloggable and get real waterlogging, drawn and simulated by the library. Without it,
  the backend is what this mod already does (`common/BlockPseudoWater`: the plant *is* the water), which
  needs no patches and only misses the water being drawn inside a block seen through glass.
- **Cost**: small: an adapter and tests with the library installed (it cannot load in the FG2 dev
  environment, so those tests run in a real instance).
- **Risk**: low. The license allows exactly this. A soft dependency, so nothing breaks without it.
- **Limit**: waterlogging vanilla's own blocks (stairs, slabs, fences, chests) is something the library
  already does for itself; we would not duplicate it.

### 3. Something else: swimming and crawling from Aqua Acrobatics' code, inside this mod

- **How**: Aqua Acrobatics is public domain, so its swimming and crawling mixins can be taken into the
  `swim` stream with credit, trimmed to 1.13's behavior and updated to today's (swimming up waterfalls,
  current Java hitboxes), and loaded through MixinBooter's late loader. When Aqua Acrobatics itself is
  installed, ours do not load at all and its version runs.
- **Cost**: medium: adapting and testing rather than writing from nothing.
- **Risk**: medium. Same conflict list as Aqua Acrobatics (Better Sprinting, Idō). A hard dependency on
  MixinBooter (already in every Cleanroom instance, and required by RLMixins in your Dregora packs).

### 4. Defer

Keep the pseudo-water blocks, ship everything else, and add none of the three. Players who want them
install Aqua Acrobatics and Fluidlogged API themselves; we just make sure we work with both.

## Recommendation

**Options 2 and 3 together**: waterlogging through a Backport Core interface backed by Fluidlogged API when
it is installed and by our pseudo-water otherwise; swimming and crawling adapted from Aqua Acrobatics' public-domain code,
standing down when Aqua Acrobatics is present. No chunk-storage patching of our own, ever.

The cost is one required library (MixinBooter) for the swim stream only; every other stream stays
mixin-free.
