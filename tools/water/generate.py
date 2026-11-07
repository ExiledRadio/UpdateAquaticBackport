"""Water stream assets: 1.13's water textures, the underwater ambience sounds and the config names.

    python tools/water/generate.py

The textures go to our own textures/water/ folder, not over vanilla's: the client serves them in place of
1.12.2's blocks/water_* only while biome water colors are on (water/client/WaterTexturePack).
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
ROOT = assets.ROOT

for name in ["water_still", "water_flow", "water_overlay"]:
    assets.texture("block/" + name, "water/" + name)
for name in ["water_still", "water_flow"]:
    if not os.path.exists(os.path.join(assets.ASSETS, "textures", "water", name + ".png.mcmeta")):
        raise SystemExit("water: %s should be animated" % name)

events = {}
for e in ["ambient.underwater.enter", "ambient.underwater.exit", "ambient.underwater.loop",
          "ambient.underwater.loop.additions", "ambient.underwater.loop.additions.rare",
          "ambient.underwater.loop.additions.ultra_rare"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("water", events)

# Config names are ours; each tooltip is the field's @Config.Comment, read from ModConfig so the two agree.
NAMES = {
    "biomeColors": "Biome Water Colors",
    "underwaterFog": "Underwater Fog",
    "ambience": "Underwater Ambience",
    "lightThroughWater": "Light Through Water",
    "gradualAir": "Gradual Air Refill",
    "floatingItems": "Floating Items and Orbs",
    "shallowJump": "Jump in Shallow Water",
}
config = open(os.path.join(ROOT, "src", "main", "java", "com", "exiledradio", M, "ModConfig.java"),
              encoding="utf-8").read()
water = config[config.index("public static final class Water"):]
water = water[:water.index("\n    }\n")]
comments = {}
for m in re.finditer(r'@Config\.Comment\((\{.*?\}|".*?")\)\s*(?:@[^\n]*\s*)*public boolean (\w+)', water, re.S):
    comments[m.group(2)] = " ".join(re.findall(r'"((?:[^"\\]|\\.)*)"', m.group(1)))
lang = []
for field, label in NAMES.items():
    if field not in comments:
        raise SystemExit("water: no @Config.Comment for %s" % field)
    lang.append("config.%s.water.%s=%s" % (M, field, label))
    lang.append("config.%s.water.%s.tooltip=%s" % (M, field, comments[field]))
missing = set(comments) - set(NAMES) - {"enabled"}
if missing:
    raise SystemExit("water: no name for %s" % sorted(missing))

assets.merge_lang("water", lang + assets.subtitle_lines(events))
print("water: 3 textures, %d sound events, %d lang lines" % (len(events), len(lang)))
