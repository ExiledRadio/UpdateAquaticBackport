"""Ocean stream: config names, and a check that the biome names in code are 1.18's.

    python tools/ocean/generate.py

1.12.2 biome names are plain strings (vanilla's are English too, and there is no translation key for them),
so they live in OceanContent.java. This checks each one against 1.18's en_us.json and fails if one differs.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
SOURCE = os.path.join(assets.ROOT, "src", "main", "java", "com", "exiledradio", M, "ocean", "OceanContent.java")

BIOMES = ["warm_ocean", "lukewarm_ocean", "deep_lukewarm_ocean", "cold_ocean", "deep_cold_ocean", "frozen_ocean",
          "deep_frozen_ocean"]

code = open(SOURCE, encoding="utf-8").read()
names = re.findall(r'properties\("([^"]+)"', code)
expected = [assets.name("biome.minecraft." + b) for b in BIOMES]
if names != expected:
    sys.exit("ocean: biome names in OceanContent.java %s differ from 1.18's %s" % (names, expected))

lines = [
    "config.%s.oceans.underwaterCaves=Underwater Caves and Ravines" % M,
    "config.%s.oceans.underwaterCaves.tooltip=Underwater caves and ravines cut through ocean floors in newly "
    "generated chunks." % M,
]
assets.merge_lang("ocean", lines)
print("ocean: %d biome names match 1.18, %d lang lines" % (len(names), len(lines)))
