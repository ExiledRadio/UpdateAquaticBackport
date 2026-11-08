# Creative tabs

Update Aquatic Backport has no creative tab of its own. Each block and item is in the vanilla tab Minecraft 1.19.2 gives it, next to its vanilla neighbor in 1.19.2's order, through Backport Core's creative placement (`creative.CreativePlacement`; the rule and the table are in Backport Core's `docs/CREATIVE_TABS.md`). Items are placed by their vanilla name: `common/Content` places every block with an item and every item, and `UpdateAquaticBackport.preInit` the spawn eggs. Trident enchantment books are in Combat.

"After" and "Before" are the stacks either side of it with this mod and Backport Core alone, from a dev client's `-Dbackportcore.creativeDump` (an item is put after its nearest 1.19.2 neighbor before it, or before the one after it, whichever is of its kind). With other backports installed an item moves to stay by its 1.19.2 neighbor: stripped logs follow the warped stem once the Nether Update adds it, and so on.

## Building Blocks

| Item | After | Before |
|---|---|---|
| `carved_pumpkin` | `carved_pumpkin` | `netherrack` |
| `stripped_oak_log` | `dark_oak_log` | `stripped_spruce_log` (this mod) |
| `stripped_spruce_log` | `stripped_oak_log` (this mod) | `stripped_birch_log` (this mod) |
| `stripped_birch_log` | `stripped_spruce_log` (this mod) | `stripped_jungle_log` (this mod) |
| `stripped_jungle_log` | `stripped_birch_log` (this mod) | `stripped_acacia_log` (this mod) |
| `stripped_acacia_log` | `stripped_jungle_log` (this mod) | `stripped_dark_oak_log` (this mod) |
| `stripped_dark_oak_log` | `stripped_acacia_log` (this mod) | `stripped_oak_wood` (this mod) |
| `stripped_oak_wood` | `stripped_dark_oak_log` (this mod) | `stripped_spruce_wood` (this mod) |
| `stripped_spruce_wood` | `stripped_oak_wood` (this mod) | `stripped_birch_wood` (this mod) |
| `stripped_birch_wood` | `stripped_spruce_wood` (this mod) | `stripped_jungle_wood` (this mod) |
| `stripped_jungle_wood` | `stripped_birch_wood` (this mod) | `stripped_acacia_wood` (this mod) |
| `stripped_acacia_wood` | `stripped_jungle_wood` (this mod) | `stripped_dark_oak_wood` (this mod) |
| `stripped_dark_oak_wood` | `stripped_acacia_wood` (this mod) | `oak_wood` (this mod) |
| `oak_wood` | `stripped_dark_oak_wood` (this mod) | `spruce_wood` (this mod) |
| `spruce_wood` | `oak_wood` (this mod) | `birch_wood` (this mod) |
| `birch_wood` | `spruce_wood` (this mod) | `jungle_wood` (this mod) |
| `jungle_wood` | `birch_wood` (this mod) | `acacia_wood` (this mod) |
| `acacia_wood` | `jungle_wood` (this mod) | `dark_oak_wood` (this mod) |
| `dark_oak_wood` | `acacia_wood` (this mod) | `acacia_stairs` |
| `prismarine_stairs` | `dark_prismarine` | `prismarine_brick_stairs` (this mod) |
| `prismarine_brick_stairs` | `prismarine_stairs` (this mod) | `dark_prismarine_stairs` (this mod) |
| `dark_prismarine_stairs` | `prismarine_brick_stairs` (this mod) | `sea_lantern` |
| `prismarine_slab` | `purpur_slab` | `prismarine_brick_slab` (this mod) |
| `prismarine_brick_slab` | `prismarine_slab` (this mod) | `dark_prismarine_slab` (this mod) |
| `dark_prismarine_slab` | `prismarine_brick_slab` (this mod) | `end_stone_bricks` |
| `dead_tube_coral_block` | `black_concrete_powder` | `dead_brain_coral_block` (this mod) |
| `dead_brain_coral_block` | `dead_tube_coral_block` (this mod) | `dead_bubble_coral_block` (this mod) |
| `dead_bubble_coral_block` | `dead_brain_coral_block` (this mod) | `dead_fire_coral_block` (this mod) |
| `dead_fire_coral_block` | `dead_bubble_coral_block` (this mod) | `dead_horn_coral_block` (this mod) |
| `dead_horn_coral_block` | `dead_fire_coral_block` (this mod) | `tube_coral_block` (this mod) |
| `tube_coral_block` | `dead_horn_coral_block` (this mod) | `brain_coral_block` (this mod) |
| `brain_coral_block` | `tube_coral_block` (this mod) | `bubble_coral_block` (this mod) |
| `bubble_coral_block` | `brain_coral_block` (this mod) | `fire_coral_block` (this mod) |
| `fire_coral_block` | `bubble_coral_block` (this mod) | `horn_coral_block` (this mod) |
| `horn_coral_block` | `fire_coral_block` (this mod) | `blue_ice` (this mod) |
| `blue_ice` | `horn_coral_block` (this mod) | `dried_kelp_block` (this mod) |
| `dried_kelp_block` | `blue_ice` (this mod) | (end of the tab) |

## Decoration Blocks

| Item | After | Before |
|---|---|---|
| `seagrass` | `dead_bush` | `sea_pickle` (this mod) |
| `sea_pickle` | `seagrass` (this mod) | `dandelion` |
| `kelp` | `red_mushroom` | `torch` |
| `shulker_box` | `chorus_flower` | `white_shulker_box` |
| `tube_coral` | `black_glazed_terracotta` | `brain_coral` (this mod) |
| `brain_coral` | `tube_coral` (this mod) | `bubble_coral` (this mod) |
| `bubble_coral` | `brain_coral` (this mod) | `fire_coral` (this mod) |
| `fire_coral` | `bubble_coral` (this mod) | `horn_coral` (this mod) |
| `horn_coral` | `fire_coral` (this mod) | `dead_brain_coral` (this mod) |
| `dead_brain_coral` | `horn_coral` (this mod) | `dead_bubble_coral` (this mod) |
| `dead_bubble_coral` | `dead_brain_coral` (this mod) | `dead_fire_coral` (this mod) |
| `dead_fire_coral` | `dead_bubble_coral` (this mod) | `dead_horn_coral` (this mod) |
| `dead_horn_coral` | `dead_fire_coral` (this mod) | `dead_tube_coral` (this mod) |
| `dead_tube_coral` | `dead_horn_coral` (this mod) | `tube_coral_fan` (this mod) |
| `tube_coral_fan` | `dead_tube_coral` (this mod) | `brain_coral_fan` (this mod) |
| `brain_coral_fan` | `tube_coral_fan` (this mod) | `bubble_coral_fan` (this mod) |
| `bubble_coral_fan` | `brain_coral_fan` (this mod) | `fire_coral_fan` (this mod) |
| `fire_coral_fan` | `bubble_coral_fan` (this mod) | `horn_coral_fan` (this mod) |
| `horn_coral_fan` | `fire_coral_fan` (this mod) | `dead_tube_coral_fan` (this mod) |
| `dead_tube_coral_fan` | `horn_coral_fan` (this mod) | `dead_brain_coral_fan` (this mod) |
| `dead_brain_coral_fan` | `dead_tube_coral_fan` (this mod) | `dead_bubble_coral_fan` (this mod) |
| `dead_bubble_coral_fan` | `dead_brain_coral_fan` (this mod) | `dead_fire_coral_fan` (this mod) |
| `dead_fire_coral_fan` | `dead_bubble_coral_fan` (this mod) | `dead_horn_coral_fan` (this mod) |
| `dead_horn_coral_fan` | `dead_fire_coral_fan` (this mod) | `painting` |

## Redstone

| Item | After | Before |
|---|---|---|
| `spruce_pressure_plate` | `oak_pressure_plate` | `birch_pressure_plate` (this mod) |
| `birch_pressure_plate` | `spruce_pressure_plate` (this mod) | `jungle_pressure_plate` (this mod) |
| `jungle_pressure_plate` | `birch_pressure_plate` (this mod) | `acacia_pressure_plate` (this mod) |
| `acacia_pressure_plate` | `jungle_pressure_plate` (this mod) | `dark_oak_pressure_plate` (this mod) |
| `dark_oak_pressure_plate` | `acacia_pressure_plate` (this mod) | `redstone_torch` |
| `spruce_trapdoor` | `oak_trapdoor` | `birch_trapdoor` (this mod) |
| `birch_trapdoor` | `spruce_trapdoor` (this mod) | `jungle_trapdoor` (this mod) |
| `jungle_trapdoor` | `birch_trapdoor` (this mod) | `acacia_trapdoor` (this mod) |
| `acacia_trapdoor` | `jungle_trapdoor` (this mod) | `dark_oak_trapdoor` (this mod) |
| `dark_oak_trapdoor` | `acacia_trapdoor` (this mod) | `oak_fence_gate` |
| `spruce_button` | `oak_button` | `birch_button` (this mod) |
| `birch_button` | `spruce_button` (this mod) | `jungle_button` (this mod) |
| `jungle_button` | `birch_button` (this mod) | `acacia_button` (this mod) |
| `acacia_button` | `jungle_button` (this mod) | `dark_oak_button` (this mod) |
| `dark_oak_button` | `acacia_button` (this mod) | `trapped_chest` |

## Miscellaneous

| Item | After | Before |
|---|---|---|
| `turtle_egg` | `beacon` | `conduit` (this mod) |
| `conduit` | `turtle_egg` (this mod) | `scute` (this mod) |
| `scute` | `conduit` (this mod) | `coal` |
| `pufferfish_bucket` | `milk_bucket` | `salmon_bucket` (this mod) |
| `salmon_bucket` | `pufferfish_bucket` (this mod) | `cod_bucket` (this mod) |
| `cod_bucket` | `salmon_bucket` (this mod) | `tropical_fish_bucket` (this mod) |
| `tropical_fish_bucket` | `cod_bucket` (this mod) | `brick` |
| `cod spawn egg` | `chicken spawn egg` | `cow spawn egg` |
| `dolphin spawn egg` | `creeper spawn egg` | `donkey spawn egg` |
| `drowned spawn egg` | `donkey spawn egg` | `elder_guardian spawn egg` |
| `phantom spawn egg` | `parrot spawn egg` | `pig spawn egg` |
| `pufferfish spawn egg` | `polar_bear spawn egg` | `rabbit spawn egg` |
| `salmon spawn egg` | `rabbit spawn egg` | `sheep spawn egg` |
| `tropical_fish spawn egg` | `stray spawn egg` | `turtle spawn egg` (this mod) |
| `turtle spawn egg` | `tropical_fish spawn egg` (this mod) | `vex spawn egg` |
| `nautilus_shell` | `music_disc_wait` | `heart_of_the_sea` (this mod) |
| `heart_of_the_sea` | `nautilus_shell` (this mod) | (end of the tab) |

## Foodstuffs

| Item | After | Before |
|---|---|---|
| `dried_kelp` | `melon_slice` | `beef` |

## Combat

| Item | After | Before |
|---|---|---|
| `turtle_helmet` | (start of the tab) | `bow` |
| `trident` | `totem_of_undying` | (end of the tab) |

## Brewing

| Item | After | Before |
|---|---|---|
| `phantom_membrane` | `lingering_potion` | (end of the tab) |

## Not in the creative menu

As in vanilla 1.19.2, which keeps them out of every tab (they are still in the game): `debug_stick`.
