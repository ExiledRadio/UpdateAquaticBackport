"""Blockstates for playing without Fluidlogged API: the same parts plus a water surface.

    python tools/common/pseudo_water.py

Without Fluidlogged API, kelp, seagrass, coral, coral fans, the conduit and bubble columns are the water in their
own space (common/PseudoWater.java), and the client reads their blockstates from blockstates/pseudo_water/. Each
one is the block's own blockstate (written by its stream's generator; run this after those) with one more part:
the shared water surface, drawn when the state's drawn-only "surface" property is true.
"""
import json
import os

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
MODID = "updateaquaticbackport"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MODID)

CORALS = ["tube", "brain", "bubble", "fire", "horn"]
BLOCKS = ["kelp", "kelp_plant", "seagrass", "tall_seagrass", "bubble_column", "conduit"]
for dead in ["", "dead_"]:
    for t in CORALS:
        BLOCKS += [dead + t + "_coral", dead + t + "_coral_fan", dead + t + "_coral_wall_fan"]


def write_json(rel, obj):
    full = os.path.join(ASSETS, *rel.split("/"))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(obj, indent=2) + "\n")


# One up face at vanilla source water's height (8/9 of a block), 1.12.2's still water texture, tint index 1
# (the biome water color; tint 0 is the plants' own), culled by a block above.
write_json("models/block/water_surface.json", {
    "ambientocclusion": False,
    "textures": {"particle": "blocks/water_still", "water": "blocks/water_still"},
    "elements": [{
        "from": [0, 0, 0], "to": [16, 14.222, 16],
        "shade": False,
        "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#water", "tintindex": 1, "cullface": "up"}}
    }]
})

for name in BLOCKS:
    with open(os.path.join(ASSETS, "blockstates", name + ".json"), encoding="utf-8") as f:
        state = json.load(f)
    state["multipart"].append({"when": {"surface": "true"}, "apply": {"model": "%s:water_surface" % MODID}})
    write_json("blockstates/pseudo_water/%s.json" % name, state)
print("pseudo_water: %d blockstates" % len(BLOCKS))
