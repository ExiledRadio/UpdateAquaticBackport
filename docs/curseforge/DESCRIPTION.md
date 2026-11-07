# Update Aquatic Backport

**All of 1.13 Update Aquatic on 1.12.2: the new oceans, everything that lives in them, and the trident.**

The oceans are 1.13's oceans: warm, lukewarm, cold and frozen, with coral reefs in the warm ones, kelp forests and seagrass meadows almost everywhere, and icebergs drifting in the frozen ones. Cod, salmon, pufferfish and tropical fish swim in schools, turtles nest on the beaches, dolphins race your boat and lead you to treasure, and the drowned walk the sea floor with tridents. Above it all, the phantoms come for anyone who hasn't slept. And the rest of the update: the conduit, bubble columns, Slow Falling and the Turtle Master, carved pumpkins, prismarine stairs and slabs, and 1.13's water, with its colors, its fog and the light that reaches the sea floor.

It follows Minecraft as it plays today. Where current Java has changed something since 1.13 (bone meal grows kelp, shears stop it, fish swim in schools), this mod does it the current way. The numbers come from the decompiled game, so spawn rates, mob health, drop chances, growth rates and potion lengths are the ones the real game uses.

**Everything 1.13 added is in this one mod.** It needs two libraries: Backport Core, which holds the code and textures the update backports share, and Fluidlogged API, for waterlogging. All three are needed on the client and the server.

> 💬 **[Join the Discord](https://discord.gg/kxQvMDJBTN)** for bug reports, questions and release pings.

<!-- IMAGE: header, a coral reef in a warm ocean (see SCREENSHOTS.md) -->

***

## Oceans

Newly generated oceans come in four temperatures, as in 1.13: **warm**, **lukewarm**, **cold** and **frozen**, with **deep lukewarm**, **deep cold** and **deep frozen** forms, in regions a couple of thousand blocks across. Warm and frozen water never touches a coast. Plain oceans and deep oceans remain where the water is neither warm nor cold. As in current Java, there is no deep warm ocean.

*   **The sea floor** follows the temperature: sand in warm and lukewarm oceans, gravel in cold and frozen ones, with patches of clay, sand and gravel.
*   **Frozen oceans** freeze over in patches, with open water between, and carry **icebergs**: great masses of packed ice with snow on top, smaller ones, and now and then one of blue ice. Blue ice also grows on packed ice under the water. Deep frozen oceans have icebergs but no sea ice.
*   **Underwater caves and ravines** cut through the ocean floor, filled with water, with obsidian at the bottom and magma blocks there too.
*   **Ocean monuments** generate in every kind of deep ocean, not only the plain one.
*   **Squid** live in every ocean, and **polar bears** on the ice of frozen oceans.

***

## Kelp, seagrass and coral

*   **Kelp** grows up from the sea floor toward the surface, a little at a time, and stops at the top of the water. Bone meal grows it a block, as in current Java, and shears on the top stop it growing for good. Breaking any piece breaks everything above it.
*   **Dried kelp**, smelted from kelp, restores 1 hunger and is eaten twice as fast as other food. Nine make a **dried kelp block**, which smelts 20 items as fuel.
*   **Seagrass** and **tall seagrass** grow on sea, river and swamp floors. Shears collect it. Bone meal on seagrass makes it tall, and bone meal on the sea floor spreads seagrass around it.
*   **Coral blocks**, **coral** and **coral fans** in tube, brain, bubble, fire and horn, living and dead. Coral needs water: a coral block with none touching it dies a few seconds later, and coral and fans out of water die too. Fans go on the floor or on the side of a block. A pickaxe on a coral block gives its dead form; Silk Touch gives the living one, and coral and fans drop only with Silk Touch.
*   **Coral reefs** grow in warm oceans: coral trees, claws and mushrooms, with coral, fans and sea pickles on them. Bone meal on the floor of a warm ocean grows coral and fans among the seagrass.
*   **Sea pickles** on a coral block spread with bone meal: four pickles, and more on the coral around.

Kelp forests grow in every ocean except warm and frozen ones; seagrass grows everywhere underwater, rivers and swamps included.

***

## Fish

*   **Cod**, **salmon**, **pufferfish** and **tropical fish** swim in the oceans. They wander, flee from players, panic when hurt, and flop about and suffocate out of water. Cod, salmon and tropical fish swim in schools that follow a leader, as in current Java.
*   **Where they live**: cod in oceans and in cold and lukewarm oceans; salmon in cold and frozen oceans and in rivers; pufferfish and tropical fish in warm and lukewarm oceans. They spawn in groups close under the surface, with a mob cap of their own, so squid aren't crowded out.
*   **Pufferfish** puff up in two steps when a player or most other mobs come close, sting whatever touches them with Poison, and deflate when left alone. Eating one gives Poison II for a minute, and Hunger III and Nausea for 15 seconds.
*   **Tropical fish** come in 2,700 looks: 2 shapes, 6 patterns and 15 colors each for body and pattern. Most are one of the 22 named common fish, such as the Clownfish or the Moorish Idol, and a school shares one look.
*   **Buckets of fish**: use a water bucket on a fish to catch it. Emptying the bucket places the water and lets the fish out with its name, health and look, and a fish from a bucket never despawns. A bucket of tropical fish names its fish. Dispensers empty fish buckets too.
*   Fish drop raw fish (cooked if they die burning) and sometimes bone meal. Raw Fish is now **Raw Cod**, Cooked Fish is **Cooked Cod**, and Clownfish is **Tropical Fish**.
*   The **Fishy Business** and **Tactical Fishing** advancements.

***

## Turtles

*   **Turtles** live on sandy beaches in groups of two to five, and remember the beach they hatched on. They are slow on land and fast in the water, swimming far out to sea and coming home now and then.
*   **Breeding**: feed two turtles seagrass. One swims home, digs in the sand for a while and lays one to four **turtle eggs**.
*   **Turtle eggs** crack three times and hatch, faster at night, but only on sand. Up to four fit in a block. Walking on them breaks them unless you sneak, and zombies, husks, zombie villagers, zombie pigmen and drowned seek them out and stomp on them. Silk Touch collects them.
*   **Baby turtles** drop a **scute** when they grow up, and seagrass makes them grow faster. Zombies, skeletons, wild wolves and ocelots hunt them on land.
*   **The turtle shell**, a helmet made from five scutes. Worn with your head out of the water, it keeps 10 seconds of Water Breathing topped up, so you can stay under that much longer. Scutes repair it.
*   **The Potion of the Turtle Master**: Slowness IV and Resistance III for 20 seconds, as in current Java. Brew a turtle shell into an awkward potion; redstone makes it last 40 seconds, and glowstone makes it stronger. Splash, lingering and tipped arrows too.

***

## Dolphins

*   **Dolphins** swim in pods in every ocean except cold and frozen ones. They breathe air: four minutes under water and they drown, so they come up to breathe. Stranded on land, they dry out after two minutes unless it's raining.
*   They chase items floating in the water, pick them up and toss them about, follow boats being rowed, and leap out of the water.
*   **Dolphin's Grace**: swim alongside a dolphin while sprinting in the water and it lends you its speed, so the water slows you far less while it lasts.
*   **Feed one** raw cod, raw salmon, tropical fish or pufferfish and it leads you to the nearest shipwreck or ocean ruin.
*   Dolphins are neutral: hit one and the whole pod fights back. They keep away from guardians.
*   Leads work on them, and a dolphin on a lead never despawns.

***

## The drowned and the trident

*   **The drowned** are zombies of the deep, in oceans and rivers. They swim after you when you're in the water, come ashore at night, and some throw tridents. A few carry a nautilus shell, which they always drop.
*   **Zombies drown**: a zombie under water for 30 seconds shakes for 15 more and becomes a drowned, keeping its equipment and name. A husk becomes a zombie the same way.
*   **The trident**: 9 attack damage at 1.1 attack speed, and 250 durability. Hold use and let go to throw it for 8 damage. It sticks where it lands, and only its thrower can pick it back up. It comes only from the drowned, as in the real game.
*   **Loyalty** brings a thrown trident back to its thrower, through blocks and even out of the void. **Riptide** launches you instead, in water or in the rain, spinning into the first mob in your way. **Impaling** adds 2.5 damage per level against aquatic mobs, as in current Java. **Channeling** calls down lightning on a mob hit in a thunderstorm.
*   The **A Throwaway Joke** and **Very Very Frightening** advancements.

***

## Phantoms

*   **Phantoms** come for players who haven't slept. Go three in-game days without getting into a bed and, at night or in a thunderstorm, phantoms may appear high above you whenever you stand under open sky: more on harder difficulties, and more often the longer you stay awake. They circle overhead, swoop down to bite, burn in daylight, and keep away from cats.
*   Getting into a bed resets the count, even if you don't sleep through the night, and so does dying. It shows as **Time Since Last Rest** in your statistics and works as a scoreboard criterion.
*   The **doInsomnia** game rule switches phantoms off in one world, as in current Java.
*   **Phantom membrane** drops from phantoms killed by a player. It repairs the **elytra** in an anvil, which leather no longer does.
*   **Slow Falling**: fall gently and take no fall damage. Brew phantom membrane into an awkward potion; redstone makes it last longer, and there are splash and lingering potions and arrows of Slow Falling.

***

## The conduit

*   **The conduit**, crafted from eight nautilus shells around a heart of the sea. Place it in water inside a frame of prismarine, prismarine bricks, dark prismarine or sea lanterns, and it wakes. The bigger the frame, the further it reaches, up to 96 blocks with the full 42 blocks.
*   **Conduit Power** for players in the water or the rain within range: you don't run out of air, you see under water as with Night Vision, and you mine faster.
*   With the full frame, its eye opens and it attacks hostile mobs in the water nearby.
*   **Nautilus shells** can be fished up as treasure, and the **heart of the sea** is found in buried treasure.

***

## Bubble columns

*   Soul sand under water sends a **column of bubbles** up to the surface; a magma block makes a **whirlpool** that drags you down.
*   Swim into an upward column to be carried up and thrown out of the water at the top. Your air comes back while your head is in a column, and falling into one never hurts.
*   A boat over a whirlpool shakes for three seconds, then sinks and throws its passengers off. Over an upward column, it's thrown into the air.
*   Bubbles rise and spiral, with their own sounds.

***

## Water

*   **Water colors by biome**, with 1.13's water texture: blue in most places, darker in snowy taigas and snowy beaches, purple in frozen oceans and rivers, green-gray in swamps, teal in warm oceans and light blue in lukewarm ones. Other mods' biomes keep their own tint.
*   **Seeing under water**: the fog takes the biome's water color, darkens the deeper you go and clears the longer you stay under. Swamp water is murkier.
*   **Underwater ambience**: a soft sound as your head goes under and comes up, a quiet loop while you're under, and now and then bubbles, a creak or a distant whale.
*   **Light through water**: water dims light by 1 level per block instead of 3, so shallow seas are no longer dark at the bottom.
*   **Air comes back gradually** after you surface.
*   **Items and experience orbs float** up in water instead of sinking.
*   **Jumping out of shallow flowing water** is a normal jump, so you can step out of it onto the next block.

***

## Mobs

*   **Undead mobs sink**: zombies, skeletons and their kinds, and skeleton and zombie horses, walk along the bottom instead of swimming to the surface. Undead mobs don't drown.
*   **Baby zombies burn in daylight**, like adult ones.
*   **Squid squirt ink** when hurt, and swim away from whatever hurt them.
*   **Parrots imitate** phantoms and drowned.
*   **Skeleton horses can be ridden under water**, fast, with their own underwater sounds.

***

## Blocks and items

*   **Prismarine, prismarine brick and dark prismarine stairs and slabs.**
*   **Carved pumpkins**: shears carve a face into a pumpkin and give 4 pumpkin seeds. The plain pumpkin has no face now. Golems and jack o'lanterns take a carved pumpkin, and you can wear one on your head.
*   **Wood**, the log with bark on every side, made from four logs. Logs are now named Oak Log and so on, and Oak Wood is the bark block.
*   **Stripped logs and stripped wood** in every wood, and an axe to strip them. **Trapdoors, buttons and pressure plates in every wood**, **blue ice** and **sea pickles**.
*   **Packed ice** from nine ice.
*   **The undyed shulker box**, and washing the color out of a dyed one in a cauldron, contents and all.
*   **The debug stick**, for operators in creative.
*   **Looser placement**: pumpkins, jack o'lanterns and fence gates stand on nothing, and doors place on the side of a block.

## Ocean structures

*   **Shipwrecks** in every ocean, sunk to the sea floor, and **beached shipwrecks** half buried on beaches: Minecraft's own 20 ship designs, upright, sideways or upside down, whole or broken, in any of the six woods. Each has supply, map and treasure chests with current Java's loot.
*   **Ocean ruins**: small and large stone brick ruins, cracked and mossy, in ordinary, cold and frozen oceans, and sandstone ruins in warm and lukewarm ones. Large ruins often stand among 4 to 8 small ones. Their chests hold current Java's ruin loot, and **drowned** wait in them.
*   **Buried treasure** under beaches: one chest, buried in sand on the stone below, with a **heart of the sea**, iron, gold, emeralds, diamonds, prismarine crystals, TNT, cooked fish and potions of Water Breathing.
*   **Buried treasure maps** in shipwreck map chests and ocean ruin chests: they show the land around the nearest buried treasure, marked with a **red X**.

<!-- PENDING: Wl -->
## Waterlogging

*Placeholder: which blocks hold water, and how buckets and flowing water work with them.*

<!-- PENDING: Sw -->
## Swimming and crawling

*Placeholder: sprint-swimming, crawling through one-block gaps and their animations.*

***

## Works with other mods

Nothing here needs another mod, apart from the two libraries.

**Village & Pillage Backport** adds the same stripped logs and wood, wood trapdoors, buttons and pressure plates, blue ice and sea pickles, because 1.14's villages are built from them. Whichever installed mod owns a shared block provides it: with Village & Pillage installed, this mod adds none of its own and uses Village & Pillage's everywhere, so axes strip logs into its stripped logs and icebergs carry its blue ice. Without it, this mod adds identical copies of its own. With both installed, there is also:

*   **The composter** takes kelp, dried kelp, seagrass and the dried kelp block.
*   **Cats** scare phantoms off, as in 1.14, and bring phantom membrane among their morning gifts. Stray cats and foxes hunt baby turtles.
*   **The stonecutter** cuts the prismarine stairs and slabs.

Mobs from other mods count as aquatic for Impaling when they are water mobs, and more can be added by id with `trident.impalingMobs`.

***

## Config

Everything lives in `config/updateaquaticbackport.cfg`, or the in-game config screen, with every setting documented in the file.

*   `woodset`: stripped logs and wood, the wood trapdoors, buttons and pressure plates, blue ice and sea pickles, and axe stripping
*   `kelp`, `coral`: the blocks, and whether they generate
*   `fish`, `turtles`, `dolphins`: the mobs, and how often they spawn
*   `phantoms`: phantoms, membrane and Slow Falling, and the `insomnia` switch that stops phantoms everywhere
*   `trident`: the trident, its four enchantments and the drowned, zombies drowning, and the mobs Impaling counts as aquatic
*   `conduit`, `bubbles`: the conduit, and bubble columns
*   `structures`: shipwrecks, ocean ruins and buried treasure
*   `oceans`: the new ocean biomes, icebergs, and underwater caves and ravines
*   `blocks`: prismarine stairs and slabs, carved pumpkins, wood and the other small changes, and the looser placement rules
*   `mobs`: each of 1.13's changes to existing mobs, one switch apiece
*   `water`: water colors, the underwater fog and ambience, light through water, gradual air, floating items and the shallow-water jump

Every part of it switches off. Each category has an `enabled` switch, and turning one off registers nothing from it.

***

## Languages

In 18 languages: English, Spanish (Spain and Latin America, including Argentina, Uruguay and Venezuela), Brazilian Portuguese, French, German, Russian, Simplified and Traditional Chinese, Japanese, Korean, Italian, Polish, Turkish, Ukrainian and Dutch. Block, item, mob, effect, enchantment, advancement and subtitle names use Minecraft's own translations, so they match the names you already know.

***

## What is not here

*   What 1.13 changed under the hood: the Flattening, the new commands, data packs and tags. 1.12.2 can't take them, and nothing you see in the game depends on them.
*   The underwater music.
*   What later updates added to the oceans and to fish: axolotls, lush caves, mangrove swamps and salmon sizes.

***

## Things to know

Existing worlds work. The new oceans, coral reefs, kelp, seagrass, icebergs and underwater caves only appear in chunks you haven't explored yet, so where old ocean meets new you'll see the change at a chunk border. The mobs spawn in old chunks too, each in its own biomes, so tropical fish and pufferfish need the new warm and lukewarm oceans.

Light already worked out under water in existing chunks stays as it was until something there changes, as in 1.13.

Endermen are still calmed by a plain pumpkin worn on your head, as well as a carved one.

If floating items cost you performance in a world with a lot of dropped items in water, turn off `water.floatingItems`.

***

## Requirements

*   Minecraft 1.12.2, Forge 14.23.5.2847 or newer
*   [Backport Core](https://www.curseforge.com/minecraft/mc-mods/backport-core) 1.1.0 or newer
*   [Fluidlogged API](https://www.curseforge.com/minecraft/mc-mods/fluidlogged-api) 3.3.3 or newer
*   Required on the client and the server

## Install

Drop this jar, Backport Core's and Fluidlogged API's in `mods` on both sides. Delete any older version first, or the two copies will crash on startup with a duplicate mod id.

***

Also by me: [Enchantment Recipes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-enchantment-recipes), which lets you craft enchanted books from mod materials, and [Villager Tomes](https://www.curseforge.com/minecraft/mc-mods/rlcraft-villager-tomes), which lets you teach a librarian the books you find.

Not affiliated with Mojang. Licensed MIT.

Source: [https://github.com/ExiledRadio/UpdateAquaticBackport](https://github.com/ExiledRadio/UpdateAquaticBackport)
