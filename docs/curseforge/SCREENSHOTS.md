# Images

None uploaded yet. Once a shot is on the project's Images tab, put its URL in the table and swap the
matching `<!-- IMAGE: ... -->` line in `DESCRIPTION.md` (or the section named here) for the image, as
Village & Pillage's page does, so the page can be rebuilt without hunting for them.

| Shot | Where it sits | URL |
| --- | --- | --- |
| A coral reef in a warm ocean, tropical fish schooling over it, in clear daytime water | header | |
| A warm, a cold and a frozen ocean side by side from above, showing the three water colors | Oceans | |
| An iceberg in a frozen ocean, with patchy sea ice around it | Oceans | |
| A kelp forest from the sea floor, looking up toward the surface | Kelp, seagrass and coral | |
| All ten coral blocks, coral and fans, living and dead, laid out in rows | Kelp, seagrass and coral | |
| A school of cod, and a puffed-up pufferfish | Fish | |
| A turtle on its nest beside its eggs, at night | Turtles | |
| Dolphins leaping beside a rowed boat | Dolphins | |
| A drowned holding a trident and a nautilus shell, under water | The drowned and the trident | |
| A player spinning up out of the water with Riptide, in the rain | The drowned and the trident | |
| Phantoms circling over a player at night | Phantoms | |
| A conduit in a full prismarine frame, its eye open | The conduit | |
| An upward bubble column beside a whirlpool | Bubble columns | |
| The underwater fog in a swamp beside that of a warm ocean | Water | |
| A carved pumpkin, prismarine stairs and slabs and the six wood blocks | Blocks and items | |
| The config screen, top level | Config | |

All prefixed with `https://media.forgecdn.net/attachments/`.

Still worth capturing once the last three parts are in: a shipwreck and an ocean ruin, a player
sprint-swimming and crawling, and a waterlogged sea pickle or lantern.

## Icon

`docs/curseforge/icon.png` is the 512x512 CurseForge avatar: the family's Update Aquatic icon,
`docs/curseforge/family-icons/update-aquatic-backport.png` in Village & Pillage's repository, copied
unchanged, in the same frame as the rest of the family. `icon-400.png` is CurseForge's stated 400x400.

`src/main/resources/pack.png` is the same icon at 128x128, the logo in the in-game mod list
(`"logoFile": "pack.png"` in `mcmod.info`).

Both smaller sizes are Lanczos downscales of the 512 file. To regenerate them after the family icon
changes, from the repository root:

```
python -c "from PIL import Image; im = Image.open('docs/curseforge/icon.png').convert('RGBA'); im.resize((400, 400), Image.LANCZOS).save('docs/curseforge/icon-400.png'); im.resize((128, 128), Image.LANCZOS).save('src/main/resources/pack.png')"
```
