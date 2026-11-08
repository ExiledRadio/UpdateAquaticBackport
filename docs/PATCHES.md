# Bytecode patches

The family's standing rule (2026-11-07): closest to vanilla, and bytecode patching is allowed where vanilla
parity needs it. This mod has its own small coremod, `asm/UpdateAquaticPlugin` (no MixinBooter), with one
transformer per patch. Each patch below says what it changes, why no event or API could do it, how it fails
safe, and how it is tested.

The coremod is named in the jar's manifest (`FMLCorePlugin`, `FMLCorePluginContainsFMLMod: true`; the dev
jar too, so a dependent's dev run loads it) and, for `runServer`/`runClient`, by `-Dfml.coreMods.load`
(`build.gradle`; GradleStart merges the two, so it loads once). Sorting index 1001: after FML's
deobfuscation, so a release sees SRG member names and the dev environment MCP names; every transformer
matches both. A transformer that does not find exactly the shape it expects changes nothing and logs
`Patch ... skipped`; it never half-patches a class. No patch adds a branch, so classes are written with
`COMPUTE_MAXS` only and no class is loaded while another is transformed. The patched code calls
`blocks/BlocksHooks`, which answers as vanilla 1.12.2 does when `blocks.enabled` is off.

Fluidlogged API (3.3.3) transforms none of these five classes (its vanilla plugins are listed in its jar
under `mod/asm/plugins/vanilla`: no `EntityLiving`, `BlockPumpkin`, `BlockLog`, `MapGenVillage` or `BiomeProvider`).

## P1. The plain pumpkin is not worn

*   **Class**: `net.minecraft.entity.EntityLiving`. **Transformer**: `asm/PumpkinHeadPatch`. **Hook**:
    `BlocksHooks.wornPumpkin()`. **Since**: 0.1.0.
*   **Vanilla 1.13+**: `MobEntity.getSlotForItemStack` sends the carved pumpkin (and skulls) to the head; the
    plain pumpkin is an ordinary item that goes in the hand.
*   **Why a patch**: 1.12.2's `getSlotForItemStack` names `Blocks.PUMPKIN`, and vanilla's pumpkin item is a
    plain `ItemBlock` with no `getEquipmentSlot` to override (replacing vanilla's item in the registry would
    be far more invasive). Everything that puts an item on a head asks this method: the helmet slot's
    `isItemValid`, shift-clicking in the inventory, dispensers, armor stands, mobs picking up items.
*   **What changes**: the one `GETSTATIC Blocks.PUMPKIN` in `getSlotForItemStack(ItemStack)` becomes
    `INVOKESTATIC BlocksHooks.wornPumpkin()`, which returns the carved pumpkin block (whose item already says
    `HEAD` through Forge's `getEquipmentSlot`, so it is never compared) or `Blocks.PUMPKIN` with the stream
    off.
*   **Fails safe**: if the method is missing or has other than one such read, nothing is patched; the plain
    pumpkin can then still be worn, as before this patch.
*   **Tested** (`-Pselftest=blocks`): plain pumpkin `MAINHAND`, carved pumpkin, skull and helmet `HEAD`,
    jack o'lantern `MAINHAND`; a real server player's helmet slot refuses a plain pumpkin and takes a carved
    one; shift-clicking puts a plain pumpkin in the hotbar and a carved one on the head; an armor stand does
    not wear a plain pumpkin and wears a carved one.

## P2. The plain pumpkin builds no golem

*   **Class**: `net.minecraft.block.BlockPumpkin`. **Transformer**: `asm/PumpkinGolemPatch`. **Hook**:
    `BlocksHooks.golemMatch(pattern, world, pos, block)`. **Since**: 0.1.0.
*   **Vanilla 1.13+**: only `CarvedPumpkinBlock` (the carved pumpkin and the jack o'lantern) builds snow
    golems and iron golems; `PumpkinBlock` has no golem code at all.
*   **Why a patch**: vanilla's pumpkin builds them in its private `trySpawnGolem`, run by `onBlockAdded`,
    which runs whoever sets the block: a player, a dispenser-like block from another mod, a structure, a
    command. Stream X kept a player's placement out of it; nothing but a patch reaches the rest.
*   **What changes**: the two `BlockPattern.match(World, BlockPos)` calls in `trySpawnGolem(World, BlockPos)`
    (snow golem, iron golem) become `BlocksHooks.golemMatch(pattern, world, pos, this)` (an `ALOAD 0` is
    inserted before each), which returns no match for `Blocks.PUMPKIN` while the stream is on and
    vanilla's match for everything else, so the carved pumpkin (a `BlockPumpkin` subclass with its own
    patterns), the jack o'lantern and other mods' subclasses are unchanged.
*   **Fails safe**: if the method is missing or has other than two such calls, nothing is patched, and a
    plain pumpkin builds golems as in 1.12.2.
*   **Also**: with the patch, `PlacementRules` no longer takes over a supported plain pumpkin's placement
    to skip `onBlockAdded`; vanilla's item places it.
*   **Tested** (`-Pselftest=blocks`): a plain pumpkin set by `setBlockState` on two snow blocks or on the
    iron T builds nothing and every block stays; a jack o'lantern set the same way builds a snow golem and
    a carved pumpkin an iron golem; a player's plain pumpkin on snow builds nothing (stream X's test, now
    through vanilla's item).

## P3. Middle-clicking wood picks the wood item

*   **Class**: `net.minecraft.block.BlockLog`. **Transformer**: `asm/WoodPickBlockPatch`. **Hook**:
    `BlocksHooks.pickBlock(block, state, vanillaStack)`. **Since**: 0.1.0.
*   **Vanilla 1.13+**: wood is its own block (`oak_wood`) and picks its own item.
*   **Why a patch**: this mod's wood is vanilla's bark log state (`axis=none`) with an item of its own, so
    vanilla's pick gives the log. Picking is `Block.getPickBlock` (Forge's, client side), with no event in
    1.12.2, and `BlockLog` does not override it.
*   **What changes**: `BlockLog` gets one new method, an override of `getPickBlock(IBlockState,
    RayTraceResult, World, BlockPos, EntityPlayer)` that calls `super.getPickBlock(...)` and returns
    `BlocksHooks.pickBlock(this, state, thatStack)`. The hook swaps in the wood item only for
    `Blocks.LOG`/`Blocks.LOG2` in the bark state, else returns vanilla's stack. `getPickBlock` is Forge's
    method, so it has one name everywhere; subclasses that override it are unaffected.
*   **Fails safe**: if `BlockLog` already has such a method (another patch), nothing is added and wood picks
    the log as before.
*   **Tested** (`-Pselftest=blocks`): `BlockLog` declares `getPickBlock`; all six bark logs pick their wood
    item; an upright log of each type still picks its log with the right metadata; stone picks stone. The
    client's middle click itself (`ForgeHooks.onPickBlock`) is not run headlessly.

## P4. Villages start by 1.13's biome point

*   **Class**: `net.minecraft.world.gen.structure.MapGenVillage`. **Transformer**: `asm/VillageBiomePatch`.
    **Hook**: `structure/Placement113.villageBiomes(provider, x, z, radius, biomes)`. **Since**: 1.0.0.
*   **Vanilla 1.13+**: `VillageStructure.hasStartAt` reads the block-scale biome at block (9, 9) of the
    candidate chunk; 1.12.2's `canSpawnStructureAtCoords` reads the 1:4 cell at (8, 8)
    (`areBiomesViable(x * 16 + 8, z * 16 + 8, 0, VILLAGE_SPAWN_BIOMES)`). The grid (32/8, salt 10387312) and
    the biome list are the same, so only villages near a biome edge move; that is enough to differ from
    1.13's seed maps.
*   **Why a patch**: replacing the village generator through `InitMapGenEvent` would take the slot Village &
    Pillage's 1.14 villages need (they replace exactly vanilla's class), and its released versions would then
    build no villages at all. The patch keeps vanilla's class, and V&P's subclass, which calls this method,
    gets 1.13's point too.
*   **What changes**: the one `INVOKEVIRTUAL BiomeProvider.areBiomesViable(IIILjava/util/List;)Z` in
    `canSpawnStructureAtCoords(II)Z` becomes `INVOKESTATIC Placement113.villageBiomes`, same arguments with the
    provider first. For radius 0 the hook asks `provider.getBiome(x + 1, z + 1)` (block scale) and the list;
    any other radius goes to `areBiomesViable` unchanged.
*   **Fails safe**: if the method is missing or has other than one such call, nothing is patched and villages
    keep 1.12.2's point.
*   **Tested**: Backport Core's vanilla oracle (`tools/vanilla-oracle`, `Oracle1132` against
    `OverworldDump` on a real server) gives the same village start chunks as 1.13.2's own code over 6 seeds,
    blocks -4096..4095.

## P5. Biome searches pick as 1.13's does

*   **Class**: `net.minecraft.world.biome.BiomeProvider`. **Transformer**: `asm/BiomeSearchPatch`. **Hook**:
    `structure/Placement113.findBiomePosition(provider, x, z, range, biomes, random)`. **Since**: 1.0.0.
*   **Vanilla 1.13+**: `findBiomePosition` keeps one matching 1:4 cell of the square, replacing it with chance
    `1 / (n + 1)`, and counts `n` for every matching cell. 1.12.2 counts only the cells it keeps, so it draws
    different random numbers. The search places all 128 strongholds (one shared random, so one difference
    moves every later stronghold) and starts the spawn point.
*   **Why a patch**: strongholds and `WorldServer.createSpawnPosition` call the method directly; only the
    method itself reaches both, and the other world types' providers that inherit it.
*   **What changes**: the method's whole body becomes `return Placement113.findBiomePosition(this, x, z, range,
    biomes, random)`, 1.13's loop over `getBiomesForGeneration` of the same square. No branch is added (the
    old body and its frames are removed).
*   **Fails safe**: if the method is missing, nothing is patched.
*   **Tested**: Backport Core's vanilla oracle gives the same 128 strongholds and spawn biome search as
    1.13.2's code over 6 seeds.

## Not in this coremod: swimming and crawling

Swimming and crawling (stream Sw, `docs/streams/swim.md`) add no transformer here. The pose engine they run
on is shared family code in Backport Core 1.1.0, which has its own coremod in its own jar
(`com.exiledradio.backportcore.asm.BackportCorePlugin`) with the five patches swimming needs: water travel
(`EntityLivingBase.travel`), the client sprint rules (`EntityPlayerSP.onLivingUpdate`), the biped swim
stroke (`ModelBiped.setRotationAngles`), the body turn (`RenderPlayer.applyRotations`) and the camera's eye
height (`EntityRenderer.orientCamera`). They are documented, with their tests, in Backport Core's
`docs/PATCHES.md`. This mod only registers its rules with that engine.

## Not a patch: endermen

Endermen calmed by a carved pumpkin rather than a plain one (`blocks/EndermanStare`) replace the
enderman's private target task on `EntityJoinWorldEvent` instead; that is exact, because the private stare
check has no other caller. See `docs/streams/followups.md`.
