"""Assets shared by several streams: 1.13's wet grass sounds.

    python tools/common/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

events = {}
for e in ["block.wet_grass.break", "block.wet_grass.step", "block.wet_grass.place", "block.wet_grass.hit",
          "block.wet_grass.fall"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("common", events)
assets.merge_lang("common", assets.subtitle_lines(events))
print("common: %d sound events" % len(events))
