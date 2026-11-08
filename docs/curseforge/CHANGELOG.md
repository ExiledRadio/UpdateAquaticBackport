# 1.0.0

First release: all of 1.13's Update Aquatic on 1.12.2. The new oceans and their floors, kelp, seagrass and coral reefs, icebergs and underwater caves; cod, salmon, pufferfish, tropical fish, turtles, dolphins, the drowned and phantoms; the trident and its four enchantments, the conduit, bubble columns, Slow Falling and the Turtle Master; carved pumpkins, prismarine stairs and slabs, wood blocks and 1.13's water. Where current Java has changed something since 1.13, this mod does it the current way.

**Requires Backport Core 1.1.0 or newer and Fluidlogged API 3.3.3 or newer**, on the client and the server, with Forge 14.23.5.2847 or newer.

## New: ocean biomes and terrain

*   **Ocean temperatures**: newly generated oceans are **warm**, **lukewarm**, **cold** or **frozen**, with **deep lukewarm**, **deep cold** and **deep frozen** forms, in regions a couple of thousand blocks across. Warm and frozen water never touches a coast. Plain oceans and deep oceans remain where the water is neither warm nor cold. As in current Java, there is no deep warm ocean.
*   **Ocean floors** by temperature: sand in warm and lukewarm oceans, gravel in cold and frozen ones, with patches of clay, sand and gravel.
*   **Frozen oceans** freeze over in patches, with open water between, and carry **icebergs**: great masses of packed ice with snow on top, smaller icebergs, and rarely one of blue ice. Blue ice also grows on packed ice under the water. Deep frozen oceans have icebergs but no sea ice.
*   **Underwater caves and ravines** cut through the ocean floor, filled with water, with obsidian at the bottom (and magma blocks when bubble columns are on). `oceans.underwaterCaves` switches them off.
*   **Ocean monuments** now generate in every kind of deep ocean, not only the plain one, by 1.13's rule.
*   Squid in every ocean, as many as in 1.13; polar bears on frozen oceans.

## Seed maps: worlds match 1.13

A new world generated with this mod matches what 1.13 generates for the same seed, so seed map sites set to 1.13 show its biomes and structures where they really are. This was checked against Minecraft 1.13.2's own code over six seeds: every biome in a 2,048-block square and every structure start in an 8,192-block square came out the same.

*   **Biomes**: the ocean temperatures as above, and the rare biome variants (sunflower plains, flower forests, tall birch forests, gravelly mountains and the like) in 1.13's places. 1.12.2 placed those from a noise it never seeded with the world seed.
*   **Temples**: desert pyramids, jungle temples, witch huts and igloos each have their own grid, as since 1.13, instead of sharing one.
*   **Shipwrecks and ocean ruins** are as far apart as in 1.13 (one per 16 chunks, at least 8 apart).
*   **Villages, temples and ocean structures** check the biome at the same block 1.13 does, and **ocean monuments** measure their surroundings from it.
*   **Strongholds and the world spawn** search for their biome the way 1.13 does, and strongholds can be in mushroom field shores. The spawn point's final spot is still chosen 1.12.2's way.
*   Mineshafts, mansions and slime chunks were already the same.
*   With a backport of a later update installed as well, the world matches that update instead: its numbers win.

## New: kelp and seagrass

*   **Kelp**: place it on the sea floor; it grows up toward the surface, a little at a time, and stops at the top of the water. Bone meal grows it a block, and shears on the top stop it growing, as in current Java. Breaking any piece breaks everything above it. Every piece drops one kelp.
*   **Dried kelp**: smelt kelp. Restores 1 hunger and is eaten twice as fast as other food. Nine make a **dried kelp block**, which smelts 20 items as fuel.
*   **Seagrass** and **tall seagrass** grow on sea, river and swamp floors. Collect seagrass with shears. Bone meal on seagrass makes it tall; bone meal on the sea floor spreads seagrass around it (and coral, in warm oceans).
*   Kelp forests and seagrass meadows generate in newly explored oceans, rivers and swamps: kelp in every ocean but warm and frozen ones.

## New: coral

*   **Coral blocks** in tube, brain, bubble, fire and horn, and their **dead** forms. A coral block with no water touching it dies a few seconds later. Mined with a pickaxe it drops its dead form; with Silk Touch, itself.
*   **Coral** and **coral fans** in the same five, living and dead. Fans go on the floor or on the side of a block. Placed in water they are waterlogged; out of water, and not touching any, they die. A bucket takes their water or gives it back. They drop only with Silk Touch.
*   **Coral reefs** in warm oceans: coral trees, claws and mushrooms, with coral, fans and sea pickles growing on them.
*   **Bone meal on a sea pickle on a coral block** makes it four pickles and spreads more over the coral around.
*   Bone meal on the floor of a warm ocean grows coral and fans among the seagrass.

## New: fish

*   **Cod**, **salmon**, **pufferfish** and **tropical fish** swim in the oceans. They wander, flee from players, panic when hurt, and flop about and suffocate out of water. Cod, salmon and tropical fish swim in schools that follow a leader, as in current Java.
*   Where they live: cod in oceans and in cold and lukewarm oceans; salmon in cold and frozen oceans and in rivers; pufferfish and tropical fish in warm and lukewarm oceans. They spawn in groups, close under the surface, with a mob cap of their own, so squid aren't crowded out.
*   **Pufferfish** puff up in two steps when a player or most other mobs come close, sting whatever touches them with Poison, and deflate when left alone.
*   **Tropical fish** come in 2,700 looks: 2 shapes, 6 patterns and 15 colors each for body and pattern. Most are one of the 22 named common fish, such as the Clownfish or the Moorish Idol; a school shares one look.
*   **Buckets of fish**: use a water bucket on a fish to catch it. Emptying the bucket places water and lets the fish out with its name, health and look; it never despawns. A bucket of tropical fish names its fish. Dispensers empty fish buckets.
*   Killed fish drop raw fish (cooked if burning) and sometimes bone meal. Raw Fish is now **Raw Cod**, Cooked Fish **Cooked Cod**, and Clownfish **Tropical Fish**.
*   Eating a pufferfish gives Poison II for a minute, and Hunger III and Nausea for 15 seconds.
*   New advancements: **Fishy Business** (catch a fish with a rod) and **Tactical Fishing** (catch one with a bucket).
*   Spawn eggs for all four fish. With `fish.spawn` off, fish come only from eggs, spawners, commands and buckets.

## New: turtles

*   **Turtles** live on sandy beaches, in groups of two to five, and remember the beach they hatched on. They are slow on land and swim fast, heading far out to sea and coming home now and then. Lightning kills them, leaving a bowl. They drop seagrass.
*   **Breeding**: feed two turtles seagrass. One of them swims home, digs a nest in the sand for a while and lays one to four **turtle eggs**. Both wait five minutes before breeding again.
*   **Turtle eggs** crack three times and hatch into baby turtles, faster at night, but only on sand. Up to four fit in a block. Walking on them (unless sneaking) or landing on them breaks them; zombies, husks, zombie villagers, zombie pigmen and drowned seek out eggs and stomp on them. Collect them with Silk Touch.
*   **Baby turtles** drop a **scute** when they grow up. Seagrass makes them grow faster. Zombies, skeletons, wild wolves and ocelots hunt babies on land.
*   **Turtle shell**: a helmet made from five scutes. Worn with your head out of the water it keeps 10 seconds of Water Breathing topped up, so you can stay under that much longer. Repair it with scutes.
*   **Potion of the Turtle Master** (Slowness IV and Resistance III for 20 seconds, as in current Java): brew a turtle shell into an awkward potion. Redstone makes it last 40 seconds, and glowstone makes it stronger (Slowness VI, Resistance IV). Splash, lingering and tipped arrow forms, as for every potion.

## New: dolphins

*   **Dolphins** swim in oceans in pods of one or two, except in cold and frozen oceans, with fewer in lukewarm and warm ones. They breathe air: four minutes under water and they drown, so they come up to breathe. Stranded on land they flop about and dry out after two minutes, unless it rains.
*   Dolphins chase items floating in water, pick them up and toss them about. Whatever one is carrying when it dies, it drops.
*   They follow boats being rowed, leap out of the water, and swim alongside players who sprint in water, giving them **Dolphin's Grace**: water slows you far less while it lasts.
*   Feed a dolphin raw cod, raw salmon, tropical fish or pufferfish and it leads you to the nearest shipwreck or ocean ruin.
*   Dolphins are neutral: hit one and the whole pod fights back. They keep away from guardians.
*   They drop up to one raw cod (cooked if they die burning; Looting adds more) and 1 to 3 experience. Leads work on them, and a dolphin on a lead never despawns.
*   A dolphin spawn egg, dolphin sounds and subtitles, and the trail a swimming dolphin leaves behind it.

## New: the trident and the drowned

*   **Trident**: 9 attack damage at 1.1 attack speed, 250 durability. Hold use and let go to throw it for 8 damage; it sticks in blocks and only its thrower can pick it back up. A trident one use from breaking can't be used. Found only on drowned.
*   **Loyalty** (I to III): the trident flies back to its thrower after it hits, through blocks, faster at each level; it even comes back from the void. A Loyalty trident never despawns.
*   **Riptide** (I to III): in water or rain, the trident launches you instead of leaving your hand, and you spin for a second, hitting the first mob in your way. Can't go with Loyalty or Channeling.
*   **Impaling** (I to V): 2.5 more damage per level against aquatic mobs (squid, guardians, fish, dolphins, turtles), thrown or in melee, as in current Java. `trident.impalingMobs` adds other mods' mobs by id.
*   **Channeling** (I): in a thunderstorm, a trident that hits a mob under open sky calls down lightning.
*   **Drowned**: underwater zombies in oceans and rivers. They swim after you when you're in the water, come ashore at night, and some throw tridents. 3% hold a nautilus shell, which they always drop. They drop rotten flesh, and sometimes their trident.
*   **Zombies drown**: a zombie under water for 30 seconds shakes for 15 more and becomes a drowned, keeping its equipment and name. A husk becomes a zombie the same way.
*   Advancements **A Throwaway Joke** (hit something with a thrown trident) and **Very Very Frightening** (strike a villager with lightning).

## New: phantoms

*   **Phantoms**: undead flying mobs that come for players who haven't slept. Go three in-game days without getting into a bed and, at night or in a thunderstorm, phantoms may appear high above you whenever you stand under open sky at or above sea level: more of them on harder difficulties, more often the longer you stay awake. They circle overhead and swoop down to bite every 8 to 12 seconds, burn in daylight, and keep away from cats.
*   Getting into a bed (you need not sleep through the night) or dying resets the count. It shows as **Time Since Last Rest** under General statistics and works as a scoreboard criterion (`stat.timeSinceRest`).
*   The **doInsomnia** game rule (added to every world, on by default) stops them spawning in one world, as in current Java; the `phantoms.insomnia` config option stops them everywhere.
*   **Phantom membrane**: a phantom killed by a player drops up to one, more with Looting. It repairs the **elytra** in an anvil, which leather no longer does.
*   **Slow Falling**: fall gently and take no fall damage. Brew an awkward potion with phantom membrane (1:30), add redstone to make it last 4:00, and gunpowder and dragon's breath for splash and lingering potions; lingering potions make **arrows of Slow Falling**.
*   A phantom spawn egg. `/summon updateaquaticbackport:phantom ~ ~ ~ {Size:10}` makes larger ones (sizes 0 to 64, each step wider and harder-hitting).

## New: the conduit

*   **Conduit**: craft it from 8 nautilus shells around a heart of the sea. Place it in water and build a frame of prismarine, prismarine bricks, dark prismarine or sea lanterns around it: three 5x5 rings, one in each direction, with the conduit in the middle and water in the 3x3x3 around it. 16 frame blocks wake it; every 7 blocks add 16 blocks of range, up to 96 with the full 42.
*   Players in water or rain within its range get **Conduit Power**: they don't run out of air under water (and get it back there, as in current Java), see under water as with Night Vision, and mine as with Haste.
*   With a full frame its eye opens and it attacks one hostile mob in water or rain within 8 blocks, 4 damage every 2 seconds.
*   It gives off light level 15, can be waterlogged (a bucket fills or empties it), drops itself (in explosions too) and can't be pushed by pistons.
*   **Nautilus shells** can be fished up as treasure. The **heart of the sea** comes from buried treasure.

## New: bubble columns

*   **Bubble columns**: soul sand under water sends a column of bubbles up to the surface; a magma block under water makes a whirlpool that pulls down. The column appears a second after the block is placed (or after water covers it) and fills the whole depth at once; it goes when its block does.
*   Swim into an upward column to be carried up and thrown out of the water at the top; a whirlpool drags you to the bottom. Falling into a column never hurts. Your air comes back while your head is in a column, just as above water.
*   A boat on a whirlpool shakes for three seconds, then sinks and throws its passengers off. On an upward column it is thrown into the air.
*   Bubbles rise and spiral in the columns, with their own flowing, whirling, "woosh" and "zoom" sounds.
*   An empty bucket takes the water from a column. A block placed into a column replaces it.
*   Magma blocks that world generation leaves under water (in newly generated chunks) start their columns straight away.

## New: blocks and items

*   **Prismarine, prismarine brick and dark prismarine stairs and slabs.** Six blocks make four stairs, three make six slabs.
*   **Carved pumpkins**: use shears on a pumpkin to carve a face into it (the face goes on the side you clicked) and get 4 pumpkin seeds. The plain pumpkin no longer has a face. Snow golems and iron golems are built with a carved pumpkin or a jack o'lantern, not a plain pumpkin; jack o'lanterns are crafted from a carved pumpkin and a torch. Wear a carved pumpkin on your head; dispensers put one on a mob's head or on a golem's body. Snow golems wear a carved pumpkin, and so do zombies and skeletons at Halloween.
*   **Endermen** are calmed only by a **carved pumpkin** on your head. As in 1.14, an enderman riding something doesn't teleport at you, one that is already angry stays angry if you put a carved pumpkin on afterwards, and endermen can pick up carved pumpkins.
*   **The plain pumpkin can no longer be worn**, and it never builds a golem, however it is placed.
*   **Middle-clicking wood** (the bark block) picks the wood item.
*   **Wood**: four logs make three wood, a block with bark on every side. It makes four planks, smelts into charcoal and burns like a log. Logs are now named "Oak Log" and so on; "Oak Wood" is the bark block.
*   **Stripped logs and stripped wood** in all six woods, **spruce, birch, jungle, acacia and dark oak trapdoors, buttons and pressure plates**, **blue ice** and **sea pickles**, with axe stripping. Vanilla's trapdoor, wooden button and wooden pressure plate take oak planks only, as in 1.13, and are named Oak Trapdoor and Oak Pressure Plate.
*   **Packed ice** from nine ice.
*   **Debug stick** (`/give @p updateaquaticbackport:debug_stick`): in creative, for operators, attack a block to pick one of its properties and use the stick on it to change that property.
*   **Shulker box**: two shulker shells and a chest now make an undyed shulker box. Dye it any color as before, or use a dyed shulker box on a cauldron with water to wash the color out, contents kept.
*   **Looser placement**: pumpkins, jack o'lanterns and fence gates can be placed with nothing under them, and doors can be placed by clicking the side of a block (`blocks.placementRules`, on by default).

## Changed: water

*   **Water colors by biome**: water has 1.13's texture and its colors: blue in most places, darker blue in snowy taigas and snowy beaches, purple in frozen oceans and rivers, a light green-gray in swamps, and, with the new ocean biomes, teal in warm oceans and light blue in lukewarm ones. Other mods' biomes keep their own tint, laid over the new blue.
*   **Seeing under water**: the fog under water takes the biome's water color, blends as you swim into another biome, gets darker the deeper you go, and clears the longer you stay under (about 30 seconds to see furthest). Swamp water is murkier. Respiration and Water Breathing no longer help you see.
*   **Underwater ambience**: a soft sound as your head goes under and comes up, a quiet loop while under, and now and then bubbles, a creak or a distant whale.
*   **Light through water**: water dims light by 1 level per block instead of 3, so shallow seas are no longer dark at the bottom. Light already worked out in existing chunks stays until something there changes, as in 1.13.
*   **Air comes back gradually** after you surface (a full bar in under 4 seconds), and the bubbles stay on screen until it is full.
*   **Items and experience orbs float** up in water instead of sinking (`water.floatingItems`).
*   **Jumping in shallow flowing water** (the thin layers at the edge of a flow) is a normal jump instead of swimming up slowly, so you can step out of it onto the next block.

## Changed: mobs

*   **Undead mobs sink in water**: zombies, husks, zombie villagers, zombie pigmen, skeletons, strays, wither skeletons, skeleton horses and zombie horses no longer swim to the surface; they sink and walk along the bottom. Undead mobs no longer drown.
*   **Baby zombies burn in daylight**, like adult ones. Baby husks still don't.
*   **Squid squirt ink**: a hurt squid shoots a cloud of ink and swims away from whatever hurt it.
*   **Polar bears spawn on the ice of frozen oceans.**
*   **Parrots imitate phantoms and drowned.**
*   **Skeleton horses can be ridden under water**: the rider stays on, the horse is fast under water, and it has its own underwater sounds.

## New: ocean structures

*   **Shipwrecks** in every ocean, sunk to the sea floor, and **beached shipwrecks** half buried on beaches: Minecraft's own 20 ship designs, upright, sideways or upside down, whole or broken, in any of the six woods. Each has supply, map and treasure chests with current Java's loot.
*   **Ocean ruins**: small and large stone brick ruins, cracked and mossy, in ordinary, cold and frozen oceans, and sandstone ruins in warm and lukewarm ones. Large ruins often stand among 4 to 8 small ones. Their chests hold current Java's ruin loot, and **drowned** wait in them.
*   **Buried treasure** under beaches: one chest, buried in sand on the stone below, with a **heart of the sea**, iron, gold, emeralds, diamonds, prismarine crystals, TNT, cooked fish and potions of Water Breathing.
*   **Buried treasure maps** in shipwreck map chests and ocean ruin chests: they show the land around the nearest buried treasure, marked with a **red X**.

## New: waterlogging

*   **Waterlogging, as in 1.13**: kelp, seagrass, coral, coral fans, conduits, bubble columns and prismarine stairs and slabs hold the water they are in. The water is drawn around them, flows out of them into open space, and buckets fill and empty the ones that may be dry.
*   **Kelp, seagrass and bubble columns always hold water**: drain them and they go with it. An empty bucket aimed at kelp or seagrass does nothing, as in 1.13; one aimed at a bubble column takes its water and leaves air. Kelp and seagrass can be placed in falling water, which becomes a source.
*   **Coral, coral fans and conduits** may be dry or in water: a coral drained of its water dies a few seconds later.
*   Light passes through all of them exactly as through the water around them, and squid, fish, dolphins and drowned spawn in the water of a kelp forest or a reef.

## New: swimming and crawling

*   **Swimming**: sprint with your head under water to swim. You lie flat, only 0.6 blocks tall, with your eyes low, and you move much faster, following where you look: up, down or level. Swimming carries on while you keep sprinting anywhere in the water, even in water one block deep, and through bumping into walls; it stops when you stop sprinting, leave the water, or get too hungry to sprint away from the ground. You can start the sprint under water with a double tap of forward or the sprint key, as on land.
*   **Through one-block gaps**: a swimmer fits through holes one block tall, and comes out of them crawling.
*   **Crawling**: with no room to stand, you crawl flat on the ground at sneaking speed and stay down until there is room, such as after swimming into a low tunnel, or with a trapdoor closed over you. With room to sneak but not to stand, you get up to a crouch first.
*   **Water feels like 1.13**: water slows a sprinting player less, you sink more slowly when you stop swimming, sneaking in water takes you down, and looking up at the surface does not lift you out unless you jump. Slow Falling slows sinking in water too.
*   **What everyone sees**: other players see you swim and crawl. Swimmers and crawlers are drawn lying along the swim with 1.13's stroke, armor and all, and the camera eases down to your new eye height and back up instead of jumping.
*   **Riptide**: a spinning player is the same small size as a swimmer while the spin lasts.
*   Players raise a trident over their head to throw it.
*   On a server, the server's own swimming and crawling settings count for everyone on it.

## New: 1.13's look and feel

With Backport Core 1.1.0 the game looks the way 1.13 did, not only plays like it. When another update backport is installed too, the newest update's look wins, as in vanilla.

*   **The title screen** shows 1.13's ocean panorama, sharp and turning all the way round, with 1.13's gradient over it.
*   **Splashes**: "Thank you for the fish!", "All rumors are true!" and "Truly gone fishing!".
*   **The world-loading screen** shows "Preparing spawn area" with a percentage.
*   **F3 + C** copies your location as a `/tp` command, and **F3 + I** copies the block or entity you look at as a `/setblock` or `/summon` command (with its data when you may use commands). F3 + Q lists both.
*   **Allow Snooper** is off by default, as from 1.13 (set once; your own choice stays yours afterwards).
*   1.13's wording for vanilla text: "Left Button", "Loading terrain...", "Open World Folder", "Load Anyway" and more, in every language.

## Works with Village & Pillage Backport

*   **Shared blocks**: stripped logs and wood, the wood trapdoors, buttons and pressure plates, blue ice and sea pickles belong to Village & Pillage Backport, which added them first. Whichever installed mod owns a shared block provides it: with Village & Pillage installed, this mod registers none of its own and uses Village & Pillage's everywhere (axes strip logs into its stripped logs, and icebergs carry its blue ice). Without it, this mod registers identical copies of its own.
*   **The composter** takes kelp, dried kelp and seagrass at 30% and the dried kelp block at 50%.
*   **Cats** scare phantoms away and hiss at them, and bring phantom membrane among their morning gifts. Stray cats and foxes hunt baby turtles on land.
*   **The stonecutter** cuts the prismarine stairs and slabs.

## Config

`config/updateaquaticbackport.cfg`, or the in-game config screen: one category for each part of the update (`woodset`, `kelp`, `coral`, `fish`, `turtles`, `dolphins`, `phantoms`, `trident`, `conduit`, `structures`, `oceans`, `bubbles`, `blocks`, `mobs` and `water`), each with an `enabled` switch. Off, a category registers nothing. Generation, natural spawning and each smaller change have switches of their own.

## Languages

*   **18 languages**: English, Spanish (Spain and Latin America, including Argentina, Uruguay and Venezuela), Brazilian Portuguese, French, German, Russian, Simplified and Traditional Chinese, Japanese, Korean, Italian, Polish, Turkish, Ukrainian and Dutch. Block, item, mob, effect, enchantment, potion, advancement, statistic and subtitle names use Minecraft's own translations, so they match the names you already know.
