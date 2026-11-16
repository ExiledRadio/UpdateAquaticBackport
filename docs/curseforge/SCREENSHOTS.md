# Images

`DESCRIPTION.md` names each image by its file name. Once a shot is on the project's Images tab, put its URL in the
table and swap the file name in `DESCRIPTION.md` for it, as Village & Pillage's page does.

Scenes are in the `Backport Showcase` instance. Teleports, times and setup are in
`Releases/screenshot-scenes/SCENES.md` under the scene number given here.

| File | Where it sits | Scene | Taken | URL |
| --- | --- | --- | --- | --- |
| `ua-coral-reef-dolphins.jpg` | header | 7 | yes | |
| `ua-frozen-ocean-icebergs.jpg` | Oceans | 23 | no | |
| `ua-shipwreck-kelp-forest.jpg` | Oceans | 8 | yes | |
| `ua-turtle-beach-sunset.jpg` | Sea life | 9 | yes | |
| `ua-drowned-trident-phantoms.jpg` | The drowned and phantoms | 11 | yes | |
| `ua-ocean-ruins-sea-lanterns.jpg` | Shipwrecks, ruins and treasure | 10 | yes | |
| `ua-swimming.jpg` | Swimming | none, see below | no | |
| `ua-bubble-columns.jpg` or `.gif` | Swimming | 24 | no | |
| `ua-blocks.jpg` | Everything else | 25 | no | |
| `ua-settings.png` | Everything else | none, see below | no | |

All URLs are prefixed with `https://media.forgecdn.net/attachments/`.

## Shots without a scene

*   **Swimming.** Go to scene 7 (the reef), press F5 twice for the front view or once for the back view, hold sprint
    and swim along the reef. F2 while moving. A second account is not needed.
*   **Settings.** Main menu, Mods, Mod Options, Update Aquatic's page in Backport Core's settings screen, the same
    framing as `vp-settings-new.png`.

## Bubble columns as a GIF

CurseForge shows animated GIFs in a description. Record 4 to 6 seconds at scene 24 without moving, then convert:

```
ffmpeg -i ua-bubble-columns.mp4 -vf "fps=15,scale=960:-1:flags=lanczos,split[a][b];[a]palettegen=max_colors=128[p];[b][p]paletteuse=dither=bayer" -loop 0 ua-bubble-columns.gif
```

Keep it under about 8 MB; drop to `fps=12` or `scale=800:-1` if it comes out larger.

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
