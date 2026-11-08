# Requests for Backport Core

Things this mod needs from Backport Core (`E:\MincraftModding\Workspaces\BackportCore`). This repo never edits
Core; each request says what is wanted and what this mod does until it exists.

## 1. Look and feel: 1.13 pieces the `presentation` module does not cover yet (Core 1.1.0)

Stream Lf registers 1.13 with `Presentation.version` (panorama, overlay, splashes, the Allow Snooper default,
F3+C, F3+I) and runs the underwater fog as a `FogPipeline` step. These 1.13 items need an owner Core does not
have yet:

*   **Video Settings: vanilla's own entries.** 1.13 removed 3D Anaglyph, renamed GUI Scale's Small, Normal, Large
    to 1, 2, 3 and allows more steps on large screens (4 at 1080p), and added Fullscreen Resolution and Biome
    Blend. Wanted: removing and relabeling vanilla options (`removeVanillaOption("ANAGLYPH")`, a GUI Scale range
    up to the window's maximum), and Fullscreen Resolution/Biome Blend as owned options (Biome Blend also needs a
    patch of `BiomeColorHelper`, a render-wide change that wants one owner).
*   **Title screen**: the 1.13 fade-in on first start, and "Copyright Mojang AB. Do not distribute!" (1.13 changed
    the punctuation). Wanted with the title-screen pieces Core already draws.
*   **Debug screen text (18w22c to pre7)**: "Targeted Block", "Targeted Fluid", "Targeted Entity" rows,
    integrated-server tick time and packets, the dimension above XYZ (1.13.1). Later versions change the same
    lines, so they need one owner (a debug-text pipeline, also asked for by the 1.14 and 1.16 backports).
*   **In-game music**: 1.13's underwater music (`music.under_water`: Axolotl, Dragon Fish, Shuniji) needs the
    situational music choice, which only one mod can own; the 1.16 backport already runs its own ticker for the
    nether. Wanted: Core owning the in-game music ticker with versioned situations (under water from 1.13, nether
    biomes from 1.16).
*   **World list (17w47a, 1.13.1)**: Edit World's "Make Backup" and "Open Backups Folder", and the world list's
    search bar. One owner for the world screens.

Out of scope for 1.12.2 (no counterpart): Command Suggestions (1.12.2 has no Brigadier suggestions), Optimize World
(the Flattening's converter), data packs, the LWJGL 3 key names (letter keys in lowercase).

## Done in Backport Core 1.1.0 (core-finish, 2026-11-08)

Used here on branch `ua-lookfeel2`: Video Settings by version (no 3D Anaglyph, numbered GUI Scale: Core's 1.13
table), the title-screen fade-in, the F3 text pipeline (1.13's lines, registered here), the in-game music owner
(`music.under_water`, registered here, with 1.13's three tracks), Edit World's Make Backup and Open Backups Folder,
and vanilla text by version (`vanilla_text/`). Core's waterlogging rules now make the prismarine stairs and slabs
`Waterloggable` here. Still open: Fullscreen Resolution and Biome Blend, the world list's search bar.
