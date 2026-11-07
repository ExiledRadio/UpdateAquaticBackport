# Stream T: turtles

Package `turtle` (client classes in `turtle/client`). Config `turtles` (`enabled`, `spawnWeight`). Entity id 10
(11-14 unused). No packets (20-24 unused). Assets from `tools/turtle/generate.py`.

## Changelog section

*   **Turtles** live on sandy beaches, in groups of two to five, and remember the beach they were born on.
    They are slow on land and swim fast, heading far out to sea and coming home now and then. Lightning kills
    them, leaving a bowl. They drop seagrass.
*   **Breeding**: feed two turtles seagrass. One of them swims home, digs a nest in the sand for a while and
    lays one to four **turtle eggs**. Both wait five minutes before breeding again.
*   **Turtle eggs** crack three times and hatch into baby turtles, faster at night, but only on sand. Up to four
    fit in a block. Walking on them (unless sneaking) or landing on them breaks them; zombies, husks, zombie
    villagers, zombie pigmen and drowned seek out eggs and stomp them. Collect them with Silk Touch.
*   **Baby turtles** drop a **scute** when they grow up. Seagrass makes them grow faster. Zombies, skeletons,
    wild wolves and ocelots (and stray cats and foxes, with Village & Pillage Backport's) hunt babies on land.
*   **Turtle Shell**: a helmet made from five scutes. Worn with your head out of water it keeps 10 seconds of
    Water Breathing topped up, so you can stay under that much longer. Repair it with scutes.
*   **Potion of the Turtle Master** (Slowness IV and Resistance III for 20 seconds): brew a turtle shell into an
    awkward potion. Redstone makes it last 40 seconds, glowstone makes it stronger (Slowness VI, Resistance
    IV). Splash, lingering and tipped arrow forms as for every potion.

## How it works

*   **The turtle** (`EntityTurtle`) is 1.14.4's `TurtleEntity` line for line where 1.12.2 allows: the same
    goals by the same names and priorities, its movement controller and in-water `travel`, the same NBT keys
    (`HomePosX`... `HasEgg`, `TravelPos*`), 30 health, speed 0.25, 1.2 x 0.4 and 0.3 of that as a baby.
*   **Walk-and-swim pathfinding**: 1.12.2 has walkers that float on the surface and swimmers that never leave
    water. `TurtleNodeProcessor` is 1.13's `WalkAndSwimNodeProcessor` (water cost 0, land 6, water against a
    solid block 4 - 1.12.2 has no `WATER_BORDER` type, so that cost is added in the processor), and
    `TurtleNavigator` is 1.13's swimmer navigator with the turtle's "stand anywhere solid, or only water while
    travelling" rule; a path point counts as reached within one block vertically, as today's navigation, where
    1.14's swimmer allowed 1.2 and so ended shore paths one block above the water. `RandomPositions` and `MoveToBlockGoal` are 1.14.4's, because 1.12.2's lack the "towards,
    within an angle" search and the vertical search the turtle goals rely on.
*   **Today's changes over 1.13**: a 5-minute breeding cooldown after mating; the second and later turtles of a
    spawned group are babies 5% of the time; trampling: never turtles, bats or non-living things, sneaking is
    safe, landing is not (1/3), standing is 1/100 a tick, mobs only with mob griefing.
*   **Eggs** (`BlockTurtleEgg`, metadata `eggs-1 | hatch<<2`): random ticks between celestial angle 0.65 and 0.69
    (about 3 to 4 am) always crack, otherwise 1 in 500; only on sand (1.12.2's sand block includes red sand).
    Hatchlings are babies for 24000 ticks with home at the nest, facing south. Breaking one egg leaves the rest
    (`harvestBlock` puts back all but one); Silk Touch drops one egg. The egg item adds to a cluster it is aimed
    at or beside (and tells the client so, or the click would never be sent).
*   **Other mobs' goals** are added in `EntityJoinWorldEvent`, once per mob, never from inside a goal
    (`TurtleEvents`): 1.14.4's `AttackTurtleEggGoal` (`BreakEggGoal`, priority 4, 24 around and 3 up or down) for
    every `EntityZombie` and for this mod's `drowned` by name if it is not one; baby-turtle targets for zombies
    except pigmen (5), every `AbstractSkeleton` (3), wolves (4, untamed only), ocelots (1, untamed only: a tamed
    1.12.2 ocelot is the cat), and Village & Pillage Backport's `cat` (1) and `fox` (4) by registry name.
*   **The shell**: an `EnumHelper` armor material (`25` durability factor, `{2,5,6,2}`, enchantability 9, 1.13's
    equip sound, repair item scute). A player tick handler adds Water Breathing 200 ticks, no particles, every
    tick the head is out of water (today's `turtleHelmetTick`). Its texture is `turtle_layer_1`.
*   **Potions**: three `PotionType`s sharing the base name `turtle_master` (`potion.effect.turtle_master` and so on).
    Brewing is three `IBrewingRecipe`s that check the potion type; Forge's own `BrewingRecipe` ignores NBT and would
    turn any potion into Turtle Master.
*   **Spawning**: `EntityRegistry.addSpawn` in postInit (so every mod's biomes exist) for every biome that the biome
    dictionary calls a beach, is not snowy and has sand on top. Spawn check: sand below, light above 8, below sea
    level + 4.
*   **Seagrass** is the kelp stream's, looked up as `Owners.ours("seagrass")`; with the kelp stream off, turtles
    cannot be bred or tempted and drop nothing.
*   **Steps**: 1.12.2 has no `determineNextStepDistance`; the shamble sound is played from the turtle's tick every
    0.15 blocks walked, and the regular step sound is silenced.

## For the integrator

*   `tools/lang/vanilla_keys.py` maps only `tile.`, `item.` and `subtitles.` keys. This stream's other vanilla
    names need two more candidate rules (the English already equals 1.18's):
    *   `entity.updateaquaticbackport.<name>.name` -> `entity.minecraft.<name>` (`turtle`);
    *   `potion.effect.<x>`, `splash_potion.effect.<x>`, `lingering_potion.effect.<x>`, `tipped_arrow.effect.<x>`
        -> `item.minecraft.<same>` (`turtle_master`). Phantoms' Slow Falling will need the same.
*   The 17 other language files are not written (Rule 4: the family tool, after merge).
*   One line each in `UpdateAquaticBackport.postInit` (`TurtleContent.postInit()`) and `SelfTestRunner.TESTS`.
*   The renderer is registered from `turtle/client/TurtleClient` (a `Side.CLIENT` event subscriber on
    `ModelRegistryEvent`), not through `client/ClientSetup`. If the integrator prefers one place for renderers,
    move that one call there.
*   Drowned (stream R): if `EntityDrowned` extends `EntityZombie` it gets the egg goal and turtle target through the
    zombie rule; otherwise by its registry name `updateaquaticbackport:drowned`.

## Tested headlessly

`./gradlew runServer -Pselftest=turtle` on port 25735: **`SELFTEST DONE pass=160 fail=0`** (four runs in a row
after the last fix); `-Pselftest=all`: **`pass=1028 fail=0`**; `python tools/check_assets.py`: **75 blocks, 5222
states, 66 item models, 108 sound files, 0 errors**; `python tools/lang/vanilla_keys.py`: the egg, scute, shell and
all 14 subtitles are vanilla names, the entity and potion names wait for the two rules above.

*   Registration: egg block and item, scute, shell, the entity (network id 10, spawn egg), 14 sound events, three
    potion types; all 12 egg states survive a metadata round trip.
*   The turtle: 30 health, speed 0.25, 1.2 x 0.4, a baby 0.36 x 0.12; breathes underwater, not pushed by water,
    no leash. Saved through `EntityList` and read back with id, home and eggs (1.13's NBT keys). A spawned group's
    first is grown and later ones babies 5% of the time (2000 tries).
*   Spawning: the biome list has the beach and not the snowy beach or stone shore; the beach's creature list has
    turtles at weight 5 in 2-5; the spawn check passes on lit sand at sea level and fails on grass, high up, in the
    dark; the world generator's own chunk spawning on a sand flat with the beach biome placed a group of 2-5
    turtles, each with home at its spawn.
*   Egg block: placing; adding with the item on the egg and on the block under it, up to four, one item each, a
    fifth going on top; shapes; hardness and blast resistance 0.5; breaking with a real player takes one egg at a
    time, nothing dropped without Silk Touch, one egg each with it, the last one taking the block.
*   Hatching: at 3:30 am every random tick cracks (50 of 50); off sand nothing; three cracks then three babies
    (one per egg), each a baby for 24000 ticks, home at the nest, facing south; by day 1 in 500 (50000 tries).
*   Trampling: turtles, bats and items never; armor stands, zombies and players do; without mob griefing only
    players; sneaking over 2000 times breaks nothing; walking breaks one now and then; landing breaks one about a
    third of the time (300 landings); zombies landing never.
*   Breeding (with the kelp stream's seagrass): fed seagrass both are in love, they mate in 60 ticks, one carries
    eggs, both wait 6000 ticks, experience but no baby; a turtle carrying eggs is not fed. At home on sand it digs,
    and in about 210-285 ticks lays 1-4 eggs on the sand near home, then stops digging.
*   Growing up drops one scute; 200 deaths drop 0-2 seagrass each, about one on average; lightning kills and drops
    a bowl; a baby drops nothing.
*   Other mobs: zombies seek eggs and hunt babies, zombie pigmen only seek eggs, skeletons, wolves and ocelots hunt
    babies, cows neither; goals are never added twice. The prey rule (a baby, not in water) for a dry baby, an adult
    and a baby in a pool. A zombie and a wild wolf each pick a baby turtle on land as their target. A zombie on a
    four-egg cluster stomps it all in 62 ticks; with mob griefing off it leaves it alone for 200.
*   Getting about: a turtle 4 blocks from the water on a beach whose water is level with the sand walks in (400
    ticks); in water it finds a path to a point 10.7 blocks away and swims to within 0.8 in 200 ticks.
*   The shell: 2 armor, 275 durability, enchantability 9, head slot, repaired with scutes and not leather, five
    scutes make it; a real player wearing it gets 200 ticks of Water Breathing without particles with the head out
    of water, none with it under water or with another helmet.
*   Turtle Master: the three forms' effects and durations, their shared name; the shell is a brewing ingredient;
    awkward (drinkable and splash) + shell, + redstone, + glowstone each give the right type; water + shell gives
    nothing; gunpowder makes it splash; eight arrows around a lingering one craft eight tipped arrows of it.
*   The server booting proves no client type is reached from server code; the release jar's classes use `func_`
    names and carry no test classes.

Found and fixed while testing: 1.12.2's path finder adds half a block to a target block's height, which 1.14's
target lookup also does, so every path aimed one block above its target (over the water, never into it); and
1.14's swimmer path following, with a turtle's reach, ended a path on the shore one block above the water. Both
now behave as today's turtles.

## Not tested

*   Anything visual (no client): the turtle model and its animations (flippers, digging, the egg bulge), its
    texture, the renderer and shadow, the worn shell texture, egg models and their rotations, item icons.
*   Sounds playing (the events are registered and every file exists).
*   Natural spawning by the world spawner (the biome list and the spawn check are tested, not a spawn cycle), and
    long trips: travelling out to sea and the home-beach return over hundreds of blocks.
*   Village & Pillage Backport's cats and foxes (needs that mod; the code path is a registry-name match).
*   Players' Water Breathing in real play (the tick handler is called directly with a real server player).
