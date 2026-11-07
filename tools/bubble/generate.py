"""Bubble column stream assets: the blockstate, an empty model, four sounds and the names.

    python tools/bubble/generate.py

The column draws nothing of its own (1.13's is invisible; its water is drawn by the fluid renderer, here
Fluidlogged API's): its blockstate is one empty model.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# No elements: nothing drawn. The particle is 1.12.2's water.
assets.write_json("models/block/bubble_column.json", {"textures": {"particle": "blocks/water_still"}})
assets.multipart_blockstate("bubble_column", [(None, "%s:bubble_column" % M)])

events = {}
for e in ["block.bubble_column.upwards_ambient", "block.bubble_column.upwards_inside",
          "block.bubble_column.whirlpool_ambient", "block.bubble_column.whirlpool_inside"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("bubble", events)

names = {
    "tile.%s.bubble_column.name" % M: assets.name("block.minecraft.bubble_column"),
}
config = [
    "config.%s.bubbles.generate=Form in New Chunks" % M,
    "config.%s.bubbles.generate.tooltip=Bubble columns start over soul sand and magma blocks that world generation "
    "leaves under water, in newly generated chunks." % M,
]
lines = sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events) + config
assets.merge_lang("bubble", lines)
print("bubble: %d sound events, %d lang lines" % (len(events), len(lines)))
