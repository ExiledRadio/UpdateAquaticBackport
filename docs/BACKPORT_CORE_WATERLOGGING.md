# What Backport Core needs for waterlogging

Written by Update Aquatic Backport for Backport Core's owner. Backport Core's repo is not edited from here.
Decision: waterlogging comes from Fluidlogged API (3.3.3, CurseForge project 485654), a required dependency
of Backport Core. See `MIXIN_DECISION.md` for what was checked: license, dependencies, Cleanroom.

## 1. Dependency

### `build.gradle` (ForgeGradle 2.3, `stable_39` mappings: Fluidlogged API's README requires both)

```groovy
repositories {
    // CurseMaven serves CurseForge files by project and file id, so the version is pinned exactly.
    maven { url 'https://cursemaven.com' }
}

dependencies {
    // Fluidlogged API 3.3.3 (CurseForge file 8445863). deobfCompile maps it to dev names, and FG2's
    // GradleStart finds its coremod on the classpath, so runServer loads it.
    deobfCompile 'curse.maven:fluidlogged-api-485654:8445863'
}
```

The author's documented alternative is JitPack: `maven { url 'https://jitpack.io' }` with
`deobfCompile 'com.github.jbredwards:fluidlogged-api:1.12.2-Latest-SNAPSHOT'`. It is a moving snapshot (the
repo has no tags), so CurseMaven's pinned file is preferred.

Every family mod that implements the adapter compiles against Fluidlogged API's API classes as well, so it
adds the same two blocks. A family mod's dependency on Backport Core's dev jar (plain `compile`, per the
flatDir note) does not carry Fluidlogged API along with it.

### `@Mod`

```java
dependencies = "required-after:fluidlogged_api@[3.3.3,)"
```

Family mods keep `required-after:backportcore`, which brings it.

### Release

- **CurseForge**: mark Fluidlogged API as a *Required Dependency* of Backport Core.
- **Never bundle, shade or jar-in-jar it.** Its license forbids redistribution.
- It must be installed on both client and server.

### First check for Core's session

Boot a dev `runServer` with the dependency and confirm in the log that Fluidlogged API loads. Expect a line
like `Calling tweak Fluidlogged API Plugin` and the mod in the loaded list. This has not been tried in our
workspaces yet. If it does not load in dev, stop and report: the headless tests depend on it.

## 2. The adapter API

Package suggestion: `com.exiledradio.backportcore.waterlog`. No mixins and no ASM, only Fluidlogged API's
published API (`git.jbredwards.fluidlogged_api.api.*`).

```java
/**
 * Implemented by every family block that 1.13 and later make waterloggable. Extends Fluidlogged API's
 * IFluidloggable, so Fluidlogged API treats the block as its own; the defaults below bridge its methods to
 * three simple choices, so family blocks never deal with FluidState directly.
 */
public interface Waterloggable extends IFluidloggable {

    /** How a block lives with water. */
    enum Need {
        /** May hold water or not: lanterns, chains, slabs, stairs, walls, scaffolding, campfires, pickles, coral. */
        OPTIONAL,
        /** Only exists in water: kelp, kelp plant, seagrass, tall seagrass, bubble columns. Drained, it breaks. */
        REQUIRED
    }

    /** Default OPTIONAL. */
    default Need waterNeed(IBlockState state) { return Need.OPTIONAL; }

    /** Whether this state can hold water at all, e.g. false for a double slab. Default true. */
    default boolean canHoldWater(IBlockState state, IBlockAccess world, BlockPos pos) { return true; }

    /**
     * Called after this position's water came or went (bucket, flow, command, generation). Coral uses it to
     * start its death timer; sea pickles to relight; campfires to go out.
     */
    default void onWaterChanged(World world, BlockPos pos, IBlockState state, boolean hasWater) { }

    // ---- bridges to IFluidloggable (all default; blocks do not override these) ----

    // isFluidValid(state, world, pos, fluid): fluid == FluidRegistry.WATER && canHoldWater(...)
    // isFluidloggable(state, world, pos, fluidState): the same, for the FluidState overload
    // onFluidFill / onFluidChange: return PASS so Fluidlogged API applies it, then onWaterChanged(..., true)
    // onFluidDrain: REQUIRED -> remove the block (no drop) and return SUCCESS; else PASS, then
    //               onWaterChanged(..., false)
    // overrideApplyDefaultsSetting(): true, so these rules win over the player's "allowDefaults" config
    // canFluidFlow / canFluidConnect / shouldFluidRender: Fluidlogged API's defaults
}

/** Reading and setting a position's water, for generation, tests and the blocks themselves. */
public final class Waterlogging {
    /** True if the position holds water: a vanilla water block, or a block with Fluidlogged API water. */
    public static boolean hasWater(IBlockAccess world, BlockPos pos);

    /** True if the position's water is a still source, such as ocean water or a waterlogged block. */
    public static boolean hasWaterSource(IBlockAccess world, BlockPos pos);

    /** Adds or removes water at a Waterloggable block. @return false if the block cannot hold it */
    public static boolean setWater(World world, BlockPos pos, boolean water, int flags);

    /** Places a block keeping the water that was there (kelp columns, reefs, sea pickles in generation). */
    public static boolean placeKeepingWater(World world, BlockPos pos, IBlockState state, int flags);

    /** One-time move of a released block's stored `waterlogged` property into Fluidlogged API water. */
    public static void migrateStoredProperty(Block block, IProperty<Boolean> waterlogged);
}
```

The helper is built on `FluidloggedUtils.getFluidState / setFluidState / setFluidToAir` and
`FluidState.of(FluidRegistry.WATER)`. Exact precedence between a block's `IFluidloggable` answers, the
`FluidloggableEvent` and Fluidlogged API's config files is not documented, so Core's tests pin it down.

## 3. Optional, for the user to decide

**Vanilla stairs** are not on Fluidlogged API's default fluidloggable list; 1.13 made them waterloggable.
Core could add `minecraft:*_stairs` through Fluidlogged API's whitelist or its config event. That needs no
patch.

## 4. Tests for Core's session (headless, Fluidlogged API in dev)

1. A `Waterloggable` test block placed into a water source keeps the water.
2. A bucket drains it and fills it.
3. Its water flows out into open air beside it, and back in.
4. Swimming and drowning work inside it.
5. A `REQUIRED` block breaks when drained, with nothing left behind.
6. `onWaterChanged` fires on fill, on drain and from flow.
7. `canHoldWater` false refuses water.
8. `placeKeepingWater` in a pool.
9. `migrateStoredProperty` on a state saved with `waterlogged=true`.
10. A vanilla slab still fluidlogs by Fluidlogged API's own defaults.
11. A dedicated server boots with no client class reached.
