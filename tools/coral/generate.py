"""Coral stream assets: 40 blockstates, their models, textures, items, names and the coral block sounds.

    python tools/coral/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
TYPES = ["tube", "brain", "bubble", "fire", "horn"]


def bs(model):
    """Blockstate model names drop the block/ folder; 1.12.2 adds it back."""
    return model.replace(":block/", ":")


lang = []
for t in TYPES:
    for dead in ("", "dead_"):
        block = "%s%s_coral_block" % (dead, t)
        plant = "%s%s_coral" % (dead, t)
        fan = "%s%s_coral_fan" % (dead, t)
        wall = "%s%s_coral_wall_fan" % (dead, t)

        m = assets.port_model("block/" + block)
        assets.write_json("blockstates/%s.json" % block, {"variants": {"normal": {"model": bs(m)}}})
        assets.item_from_block(block)

        m = assets.port_model("block/" + plant)
        assets.multipart_blockstate(plant, [(None, bs(m))])
        assets.item_generated(plant, assets.texture("block/" + plant))

        m = assets.port_model("block/" + fan)
        assets.multipart_blockstate(fan, [(None, bs(m))])
        assets.item_generated(fan, assets.texture("block/" + fan))

        m = assets.port_model("block/" + wall)
        rotations = {"north": 0, "east": 90, "south": 180, "west": 270}
        parts = []
        for facing, y in rotations.items():
            parts.append(({"facing": facing}, bs(m)))
        assets.multipart_blockstate(wall, parts)
        # y rotation per facing: patch the parts written above.
        import json
        path = os.path.join(assets.ASSETS, "blockstates", wall + ".json")
        data = json.load(open(path, encoding="utf-8"))
        for part in data["multipart"]:
            facing = part.get("when", {}).get("facing")
            if facing and rotations[facing]:
                part["apply"]["y"] = rotations[facing]
        assets.write_json("blockstates/%s.json" % wall, data)

        for name in (block, plant, fan, wall):
            lang.append("tile.%s.%s.name=%s" % (M, name, assets.name("block.minecraft." + name)))

events = {}
for e in ["block.coral_block.break", "block.coral_block.step", "block.coral_block.place", "block.coral_block.hit",
          "block.coral_block.fall"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("coral", events)
assets.merge_lang("coral", sorted(lang) + assets.subtitle_lines(events))
print("coral: %d names" % len(lang))
