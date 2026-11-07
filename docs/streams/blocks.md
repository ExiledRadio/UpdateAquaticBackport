# Stream X: other blocks and items

Package `blocks` (client parts in `blocks/client`). Config `blocks` (`enabled`, new `placementRules`). No
entities, no packets (ids 60-64 unused).

## Changelog section

*   **Prismarine, prismarine brick and dark prismarine stairs and slabs.** Six blocks make four stairs, three
    make six slabs. A stonecutter that cuts by crafting recipes cuts them too.
*   **Carved pumpkins**: use shears on a pumpkin to carve a face into it (the face goes on the side you
    clicked) and get 4 pumpkin seeds. The plain pumpkin no longer has a face. Snow golems and iron golems are
    built with a carved pumpkin or a jack o'lantern, not a plain pumpkin; jack o'lanterns are crafted from a
    carved pumpkin and a torch. Wear a carved pumpkin on your head; dispensers put one on a mob's head or on
    a golem's body. Snow golems wear a carved pumpkin, and so do zombies and skeletons at Halloween.
*   **Wood**: four logs make three wood, a block with bark on every side. It makes four planks, smelts into
    charcoal and burns like a log. Logs are now named "Oak Log" and so on; "Oak Wood" is the bark block.
*   **Packed ice** from nine ice.
*   **Debug stick** (`/give @p updateaquaticbackport:debug_stick`): in creative, for operators, attack a block
    to pick one of its properties and use the stick on it to change that property.
*   **Shulker box**: two shulker shells and a chest now make an undyed shulker box. Dye it any color as
    before, or use a dyed shulker box on a cauldron with water to wash the color out (contents kept).
*   **Looser placement**: pumpkins, jack o'lanterns and fence gates can be placed with nothing under them,
    and doors can be placed by clicking the side of a block (`placementRules`, on by default).

## How it works

*   **Slabs** follow the family's two-block slab (as Village & Pillage's building slabs): `<name>_slab` and
    `<name>_double_slab`, a one-value `variant` that `ItemSlab` needs. Blockstate files list
    `variant=default`, so no state mapper is needed. Strength 1.5 / 6 and map colors from 1.14.4 (cyan for
    prismarine, diamond for the other two). Models use 1.12.2's own (animated) prismarine textures, and the
    double slab is vanilla's full block model.
*   **Carved pumpkin** extends vanilla's `BlockPumpkin`, which looks its golem patterns up through protected
    methods: overriding those with a carved-pumpkin-or-jack-o'lantern head is all it takes, the spawning is
    vanilla's. It needs no support and does not tick. Its item is worn on the head (`getEquipmentSlot`)
    and draws vanilla's pumpkin blur (`renderHelmetOverlay`, client class). Endermen can carry it.
*   **The plain pumpkin**: drawn faceless by mapping vanilla's pumpkin states and item to this mod's
    `pumpkin` models (client, `BlocksClient`), made from 1.12.2's pumpkin textures. It builds no golems and
    is not worn (patches P1 and P2, `docs/PATCHES.md`). Its dispenser behavior becomes plain dropping; the
    carved pumpkin gets the old behavior.
*   **Carving** is 1.14.4's `PumpkinBlock.onBlockActivated`: clicked face, or the player's facing when
    clicked on top or bottom; 4 seeds thrown out of the face; `block.pumpkin.carve` (new sound, copied from
    1.18); shears wear by 1. Any `ItemShears`.
*   **Jack o'lantern recipe**: vanilla's `minecraft:lit_pumpkin` edited in place (pumpkin slot to carved
    pumpkin), as the family narrows recipes, so it keeps its name and recipe-book advancement.
*   **Wood** is vanilla's hidden `axis=none` log state, which is 1.13's wood in all but name and is drawn by
    vanilla's `oak_bark` models. The items (`oak_wood` ... `dark_oak_wood`) are plain items, not `ItemBlock`s
    (an `ItemBlock` for vanilla's log would take over the log's item mapping), placing that state like
    `ItemBlock.onItemUse`. Breaking a bark log gives the wood item back (`HarvestDropsEvent`, Silk Touch too);
    axe stripping it already gives stripped wood (stream W). Charcoal 0.15 xp, 300 ticks of fuel, ore name
    `logWood` (1.13's logs tag holds wood). Vanilla's six log names are renamed to 1.13's in `en_us.lang`.
*   **Debug stick** is 1.14.4's `DebugStickItem`: `canUseCommandBlock` (creative and op level 2), attack
    selects (sneak: previous), use cycles (sneak: previous), set with flags 2|16, messages above the hotbar
    with 1.18's text, selection per block id in the `DebugProperty` tag. 1.12.2 has no
    `canPlayerBreakBlockWhileHolding`, so attacking is `PlayerInteractEvent.LeftClickBlock`, cancelled on
    both sides. 1.12.2 keeps a state in 4 bits of metadata and works many properties out from neighbors
    (fence connections), so the stick offers only properties whose every step survives the metadata round
    trip; a property that would snap back is never offered.
*   **Undyed shulker box** is vanilla's `BlockShulkerBox` reporting purple (1.12.2 needs a color; vanilla code
    indexes arrays by it) with its own tile entity class, `updateaquaticbackport:shulker_box`. That class is
    what lets the client bind a renderer: vanilla's shulker renderer with `bindTexture` overridden to swap in
    1.13's undyed `entity/shulker/shulker.png`, and an item renderer drawing the same tile entity.
    Everything else (inventory, opening, keeping contents, tooltip, comparators, dyeing, dispensers) is
    vanilla's. The shells-and-chest recipe is registered under vanilla's name `minecraft:purple_shulker_box`
    (a registry override), so the recipe book and its unlocking advancement now give the undyed box.
*   **Cauldron washing** is current Java's `CauldronInteraction.SHULKER_BOX`: only the sixteen dyed boxes,
    water level above 0, one level used, item tag kept. Not while sneaking (the box is placed then, as for
    any item).
*   **Placement rules** (`PlacementRules`, `EventPriority.LOW`): a click that vanilla would refuse for want of
    support is taken over for pumpkins, jack o'lanterns and fence gates (any `BlockFenceGate`); a door item
    (any `ItemDoor`) clicked on a side face is handed to vanilla's door code as the click on the top of the
    block under the door's place. Every taken-over click is first offered to the clicked block exactly as
    `processRightClickBlock` does (a trapdoor or chest still takes it), and the client cancels the same
    clicks, so no ghost blocks.

## Tested headlessly

`./gradlew runServer -Pselftest=all` (port 25743), then `python tools/check_assets.py`: **`SELFTEST DONE pass=1042
fail=0`** across all five tests (174 of them this stream's); **85 blocks, 5349 states, 78 item models, 62 sound
files, 0 errors**. `python tools/lang/vanilla_keys.py`: every new name that has a matching key is listed as
vanilla except the false flags under *For the integrator*.

*   Registration: 3 stairs, 3 slabs, 3 doubles (no item), the carved pumpkin, the undyed box, 6 wood items, the
    debug stick in no tab; every stored state survives a metadata round trip (stairs' shape is never stored);
    stairs and slabs hardness 1.5, blast resistance 6, pickaxe; map colors; carved pumpkin hardness 1, axe,
    worn on the head; the box stacks to 1.
*   Recipes: 3 of each prismarine to 6 slabs, 6 to 4 stairs; 9 ice to packed ice; 4 logs to 3 wood and wood to
    4 planks for all six woods (acacia and dark oak from `log2`); wood smelts to charcoal, burns 300 ticks, is
    `logWood`; carved pumpkin over a torch makes a jack o'lantern and a plain pumpkin no longer does; shells
    and a chest make the undyed box, which dyes red with vanilla's coloring recipe.
*   Through the server's own right-click path (`processRightClickBlock`, a real player): a slab on the floor
    is a bottom slab, a second makes the double, which drops two; stairs place and drop themselves.
*   Carving: shears on the east face carve it facing east, 4 seeds, shears wear by 1; on the top it faces the
    player; a carved pumpkin is not carved again.
*   Golems: a carved pumpkin on two snow blocks makes a snow golem and uses up the blocks; on the iron T an
    iron golem; a plain pumpkin placed by a player on snow builds nothing, stays, and is used up; a jack
    o'lantern and a player's carved pumpkin still build golems; a dispenser puts a carved pumpkin on a snow
    golem's body and builds it; the plain pumpkin's dispenser behavior is plain dropping.
*   Placement: a pumpkin, a jack o'lantern and two fence gates placed against a floating block's sides; a
    trapdoor clicked with a pumpkin opens and nothing is placed; with `placementRules` off a floating fence
    gate and a side-clicked door are refused; with it on, a door clicked on a wall's side stands in front of
    it (both halves) and is used up; a door over air is still refused.
*   Wood places vanilla's bark log (oak in `log`, dark oak in `log2`), breaking it gives the wood back, Silk
    Touch too; an ordinary log still drops a log.
*   Debug stick: a creative player who is not an operator changes nothing; as an operator, attacking breaks
    nothing and selects `facing`, using it cycles north to south and sneaking back; on a log two attacks
    select `variant` and using it makes oak spruce; each block keeps its own selection; a fence offers no
    property, a lever both of its.
*   Undyed box: places with its own tile entity, saved as `updateaquaticbackport:shulker_box` and read back
    with its contents; broken, it drops itself with its contents; a named red box with a tag, washed in a
    cauldron, comes out undyed with name and tag, the cauldron one level lower, nothing placed; an undyed box
    takes no water; an empty cauldron washes nothing; dispensers use vanilla's shulker box behavior for it.
*   A zombie joining with a pumpkin on its head wears a carved one.
*   The server booting at all shows no client type is reached from server code (the `@SideOnly` helmet
    overlay is stripped; `blocks/client` is a client-only subscriber).
*   Forge logs one expected warning: the `minecraft:purple_shulker_box` override.

## Not tested

*   Anything visual (no client): the faceless pumpkin, carved pumpkin, stairs and slab models, the undyed
    shulker box's renderer and item renderer and its texture, the pumpkin blur, the snow golem head layer
    swap (`BlocksClient`, reflection on `RenderLivingBase.layerRenderers`, SRG `field_177097_h`), the debug
    stick's glint and its messages arriving above the hotbar.
*   The client side of the click handlers (the client is expected to cancel the same clicks; only the server
    side runs here).
*   The carve sound playing.
*   A Halloween-spawned zombie or skeleton (the head swap is tested on a zombie given a pumpkin and spawned).

## Done later (follow-ups)

The four gaps this stream left are closed in `docs/streams/followups.md`: endermen are calmed only by a
carved pumpkin (their target task replaced), and three patches (`docs/PATCHES.md`) stop the plain pumpkin
being worn or building golems however it is placed, and make middle-clicking wood pick the wood item.

## For the integrator

*   Nothing in `common/` needs to change.
*   `tools/lang/vanilla_keys.py` (on `main`) only knows `tile.` / `item.` / `subtitles.` keys of this mod, so it
    lists these vanilla texts as our own (false flags for the language tool to map, not translate):
    *   `tile.log.{oak,spruce,birch,jungle,acacia,big_oak}.name` - vanilla keys renamed to 1.18's
        `block.minecraft.<wood>_log` ("Oak Log"); they want `SPECIAL` entries as woodset's trapdoor has;
    *   `item.updateaquaticbackport.debug_stick.{empty,select,update}` - 1.18's
        `item.minecraft.debug_stick.{empty,select,update}`;
    *   `tile.updateaquaticbackport.<prismarine>_double_slab.name` - the slab's own name,
        `block.minecraft.<prismarine>_slab` (no 1.13 key exists for a double slab).
    `vanilla_keys.json` was not regenerated in this commit.
*   The other 17 language files are not written here; `en_us.lang` has every new string in the `blocks`
    section, written by `tools/blocks/generate.py`.
*   Two vanilla recipes are touched: `minecraft:lit_pumpkin` (edited in place) and
    `minecraft:purple_shulker_box` (overridden to give the undyed box; Forge logs the override).
*   The generator also writes `blockstates/pumpkin.json` and `models/{block,item}/pumpkin.json` for vanilla's
    pumpkin; `check_assets.py` never sees them (not our block), so the generator checks their textures
    itself.
