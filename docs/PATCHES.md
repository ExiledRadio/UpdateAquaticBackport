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
`Patch ... skipped`; it never half-patches a class. Classes are written with `COMPUTE_MAXS` only, so no class
is loaded while another is transformed; the patches that add a branch (P7 and P11, each at the head of a method) add
their frame themselves. P1 to P3 call `blocks/BlocksHooks`, which answers as vanilla 1.12.2 does when `blocks.enabled`
is off; P7 to P10 call `mechanics/MechanicsHooks` and P11 `mechanics/ChestPairing`, which answers as 1.12.2 does when the patch's own
`mechanics` setting is off.

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

## P7. Leaves stay up to 6 blocks from a log

*   **Classes**: `net.minecraft.block.BlockLeaves`, `net.minecraft.block.BlockLog`. **Transformer**:
    `asm/LeafDecayPatch`. **Hooks**: `MechanicsHooks.leavesTick`, `logBroken`. **Setting**: `mechanics.leafDistance`.
    **Since**: after 1.1.1 (unreleased).
*   **Vanilla 1.13+**: `LeavesBlock` keeps a `distance` (1 to 7) to the nearest log through leaves, and a leaf at 7
    that is not persistent decays; so leaves up to 6 steps from a log stay. 1.12.2's `BlockLeaves.updateTick`
    searches only 4 steps (a 9x9x9 cube), so large trees (big oaks, dark oaks, jungle and spruce giants, modded
    trees) lose the outer canopy once something near them changes.
*   **Why a patch**: the search is inside `updateTick`, with its range as constants and a fixed array; every
    leaves block that extends `BlockLeaves` (vanilla's, most mods') runs it, and no event reaches it.
*   **What changes**: `updateTick` starts with `if (MechanicsHooks.leavesTick(this, world, pos, state)) return;`.
    With the setting on, the hook runs 1.12.2's check with 1.13's range: a leaf marked to check (`check_decay`)
    that can decay stays (and is unmarked) if a block that sustains leaves is at most 6 steps away through leaves
    (faces only, as both versions count), else it drops as vanilla's does. Surroundings not loaded leave the mark,
    so a random tick checks again (1.12.2 decided on stale data). `BlockLog.breakBlock` starts with
    `MechanicsHooks.logBroken(world, pos)`, which marks the leaves up to 6 around a broken log (vanilla's
    marking, 4 around, follows), so leaves 5 and 6 from a log that goes are checked too.
*   **Kept from 1.12.2**: leaves are still checked only when marked (a log or leaf near them went). 1.13 also
    decays, on a random tick, any unmarked leaf already more than 6 from a log; that would strip leaves from
    trees in existing worlds (and modded trees built with 1.12.2's rules) that 1.12.2 leaves alone, so it is
    not done. Leaves 5 and 6 from a log persist, as in 1.13.
*   **Fails safe**: a method not found leaves that class as 1.12.2 (logged). A leaves block without vanilla's
    two properties, or one that overrides `updateTick` without calling it, keeps its own decay.
*   **Tested** (`-Pselftest=mechanics`): distances 1 to 8 along a row; after the checks, leaves 1 to 6 stay
    unmarked and 7 and 8 are gone; a leaf touching a log only by an edge is not held; with the setting off,
    leaf 5 decays as in 1.12.2; breaking a log marks the leaf 6 away (not 7) and leaves 5 and 6 then decay.

## P8. Signs and banners beside a cactus

*   **Classes**: `net.minecraft.block.BlockSign`, `net.minecraft.block.BlockBanner`. **Transformer**:
    `asm/CactusSignPatch`. **Hook**: `MechanicsHooks.cactusBeside`. **Setting**: `mechanics.cactusBreaksSigns`.
    **Since**: after 1.1.1 (unreleased).
*   **Vanilla 1.13+** (17w47a): signs and banners can be placed beside a cactus, and the cactus then breaks.
    1.12.2's `canPlaceBlockAt` of both refuses a spot with a cactus beside it (`BlockContainer.hasInvalidNeighbor`).
*   **What changes**: the boolean that `hasInvalidNeighbor` returns in each `canPlaceBlockAt` goes through
    `cactusBeside(boolean)`, false with the setting on. The rest is 1.12.2's own: the sign's neighbor update
    reaches the cactus, which breaks beside a solid block (wood is solid); a wall sign on the cactus then loses
    its support and drops too, as in 1.13.
*   **Fails safe**: other than one such call, nothing is patched in that class.
*   **Tested**: a sign and a banner can be placed beside a cactus (not with the setting off); a sign placed there
    breaks the cactus.

## P9. The witch hut's flower pot holds a red mushroom

*   **Class**: `ComponentScatteredFeaturePieces$SwampHut`. **Transformer**: `asm/SwampHutPotPatch`. **Hook**:
    `MechanicsHooks.swampHutPot`. **Setting**: `mechanics.witchHutMushroom` (new chunks). **Since**: after 1.1.1 (unreleased).
*   **Vanilla 1.13+** (17w47a): the pot in a witch hut holds a red mushroom. 1.12.2 asks for one through the pot's
    `contents` property, but that property is only drawn from the tile entity, so the pot is empty.
*   **Why a patch**: the hut is built inside chunk population with no event of its own; finding huts afterwards
    would mean reading the structure generator's private map.
*   **What changes**: right after the `setBlockState` that follows `Blocks.FLOWER_POT` in `addComponentParts`,
    `swampHutPot(this, world, box)` puts a red mushroom in the empty pots of the hut's part of the chunk.
*   **Tested**: a hut built in a test area has a red mushroom in its pot; with the setting off, an empty pot.

## P10. Creative players sleep with monsters nearby

*   **Class**: `net.minecraft.entity.player.EntityPlayer`. **Transformer**: `asm/CreativeSleepPatch`. **Hook**:
    `MechanicsHooks.noMonstersNear`. **Setting**: `mechanics.creativeSleep`. **Since**: after 1.1.1 (unreleased).
*   **Vanilla 1.13+** (18w20a): a player in Creative can sleep while monsters are near. 1.12.2's `trySleep`
    answers `NOT_SAFE` for anyone. Forge's sleep event can only refuse or answer for vanilla, not let the rest of
    `trySleep` run.
*   **What changes**: the method's one `List.isEmpty()` (the monsters near the bed) becomes
    `noMonstersNear(list, this)`: empty, or the player is in Creative and the setting is on.
*   **Tested**: with a zombie near, a survival player gets `NOT_SAFE`, a Creative one `OK`, and with the setting
    off `NOT_SAFE` again.

## P11. Chests pair by a stored pairing

*   **Classes**: `net.minecraft.block.BlockChest`, `net.minecraft.tileentity.TileEntityChest`, Forge's
    `net.minecraftforge.items.VanillaDoubleChestItemHandler`. **Transformer**: `asm/ChestPairingPatch`. **Hooks**:
    `mechanics/ChestPairing`. **Setting**: `mechanics.chestPairing`. **Since**: after 1.1.1 (unreleased).
*   **Vanilla 1.13+** (17w47a): a chest's `type` (single, left, right) is part of its block state.
    `ChestBlock.getStateForPlacement`: the chest faces the player; sneaking against the side of a single chest joins
    that chest (with its facing); sneaking otherwise leaves it single; placed normally it joins a single chest of the
    same kind on its left or right that faces the same way. A chest can be placed anywhere, beside a double chest or
    between two chests (it stays single, or joins one). Breaking a half leaves the other single. 1.12.2 pairs any two
    chests of a kind that touch, every time it looks (`getContainer`, `getBoundingBox`, `TileEntityChest.getAdjacentChest`,
    Forge's item handler), so it refuses a third chest beside a double and can never keep two side by side apart.
*   **Why a patch**: every reader recomputes the pairing from the neighbors inside vanilla's methods; there is no
    event, and the block's metadata (its facing, values 2 to 5) has no room for a type.
*   **What changes**: the pairing is stored in the chest tile entity's Forge data (`UpdateAquaticChestPair`: -1 single,
    else the direction's index), saved with it and sent with the chunk. These methods start with
    `if (ChestPairing.handles(this)) return ChestPairing.<hook>(...)`: `BlockChest.getStateForPlacement`,
    `onBlockPlacedBy`, `canPlaceBlockAt`, `getContainer` (inventories, hoppers, comparators), `getBoundingBox`,
    `onBlockAdded` (returns: 1.12.2 turned neighbors to line up), `TileEntityChest.getAdjacentChest` (which fills the
    public `adjacentChestXNeg/XPos/ZNeg/ZPos` the renderer, the lid and other mods read) and Forge's
    `VanillaDoubleChestItemHandler.get`. `breakBlock` first calls `ChestPairing.broken` (the other half becomes single).
    `TileEntityChest` gets `getUpdatePacket` (its update tag, when the chest is handled) and `onDataPacket` (takes the
    pairing), so a change reaches clients after the block update that `ChestPairing` sends. `handles` is true only for
    vanilla's chest and trapped chest with vanilla's tile entity class, with the setting on; another mod's chest
    subclass keeps 1.12.2's code.
*   **Old chests**: a chest without a stored pairing pairs as 1.12.2 would, with a neighbor of its kind that has none
    either and pairs back (west, east, north, south), and the server then stores it (only with the area around loaded,
    so a pair across an unloaded chunk border is not split). A stored pairing counts only while the other half stores
    the way back.
*   **Fails safe**: a class not as expected is left whole (each logs); with `BlockChest` unpatched, placement and
    containers stay 1.12.2's.
*   **Tested** (`-Pselftest=chests`, and `runClient -Pselftest=clientmech`): see `docs/streams/mechanics.md`.

## P4 and P5: moved to Backport Core

The villages' 1.13 biome point (P4) and the 1.13 pick of the strongholds' and spawn point's biome search (P5)
are Backport Core 1.1.0's patches P9 and P8 now (its `docs/PATCHES.md`), shared by every backport of 1.13 and
later, so each of them alone gets them. This mod only registers 1.13's structure numbers with Core's placement
table (`structure/Placement113`). The numbers P4 and P5 had are not reused.

## P6: moved to Backport Core

Biome Blend (1.0.1's P6, `asm/BiomeBlendPatch` and `water/BiomeBlend`) is Backport Core's patch P68 since Core 1.4.7
and this mod's 1.2.3, with its Video Settings option (still `updateaquaticbackport:biome_blend_radius`), so every
backport of 1.13 and later has it.

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
