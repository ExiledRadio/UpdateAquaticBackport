"""Stream X assets: prismarine stairs and slabs, the carved pumpkin and the faceless pumpkin, wood, the debug
stick, the undyed shulker box, the carving sound, and every name.

    python tools/blocks/generate.py

Textures: the prismarines, pumpkins, bark and stick are 1.12.2's own (the blocks they sit beside are drawn
with them); only the undyed shulker box's two textures are new, from the 1.18.2 jar.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# 1.13 name -> 1.12.2 texture of the block it is made of, and 1.12.2's full-block model (the double slab).
PRISMARINES = {
    "prismarine": ("blocks/prismarine_rough", "prismarine_rough"),
    "prismarine_brick": ("blocks/prismarine_bricks", "prismarine_bricks"),
    "dark_prismarine": ("blocks/prismarine_dark", "prismarine_dark"),
}
WOODS = ["oak", "spruce", "birch", "jungle", "acacia", "dark_oak"]
# 1.12.2 calls dark oak "big_oak" in its log keys.
OLD_LOG_KEYS = {"oak": "oak", "spruce": "spruce", "birch": "birch", "jungle": "jungle", "acacia": "acacia",
                "dark_oak": "big_oak"}


def stairs_blockstate(name):
    """1.12.2's own stairs blockstate (purpur_stairs.json: rotations, uvlock), pointed at our models."""
    import json
    data = json.loads(assets.OLD_JAR.read("assets/minecraft/blockstates/purpur_stairs.json").decode("utf-8"))
    names = {"purpur_stairs": name, "purpur_inner_stairs": name + "_inner", "purpur_outer_stairs": name + "_outer"}
    for entry in data["variants"].values():
        entry["model"] = "%s:%s" % (M, names[entry["model"]])
    return data


lang = []

for name, (texture, full) in PRISMARINES.items():
    tex = {"bottom": texture, "top": texture, "side": texture}
    assets.write_json("models/block/%s_stairs.json" % name, {"parent": "block/stairs", "textures": tex})
    assets.write_json("models/block/%s_stairs_inner.json" % name, {"parent": "block/inner_stairs", "textures": tex})
    assets.write_json("models/block/%s_stairs_outer.json" % name, {"parent": "block/outer_stairs", "textures": tex})
    assets.write_json("blockstates/%s_stairs.json" % name, stairs_blockstate(name + "_stairs"))
    assets.item_from_block(name + "_stairs")

    assets.write_json("models/block/%s_slab.json" % name, {"parent": "block/half_slab", "textures": tex})
    assets.write_json("models/block/%s_slab_top.json" % name, {"parent": "block/upper_slab", "textures": tex})
    assets.write_json("blockstates/%s_slab.json" % name, {"variants": {
        "half=bottom,variant=default": {"model": "%s:%s_slab" % (M, name)},
        "half=top,variant=default": {"model": "%s:%s_slab_top" % (M, name)},
    }})
    assets.write_json("blockstates/%s_double_slab.json" % name, {"variants": {"variant=default": {"model": full}}})
    assets.item_from_block(name + "_slab")

    lang.append("tile.%s.%s_stairs.name=%s" % (M, name, assets.name("block.minecraft.%s_stairs" % name)))
    lang.append("tile.%s.%s_slab.name=%s" % (M, name, assets.name("block.minecraft.%s_slab" % name)))
    # The double slab has no 1.13 name of its own: two slabs are still the slab.
    lang.append("tile.%s.%s_double_slab.name=%s" % (M, name, assets.name("block.minecraft.%s_slab" % name)))

# Carved pumpkin: 1.12.2's pumpkin, face and all. The plain pumpkin: 1.13's faceless column, in 1.12.2's textures.
assets.write_json("models/block/carved_pumpkin.json", {"parent": "block/orientable", "textures": {
    "top": "blocks/pumpkin_top", "front": "blocks/pumpkin_face_off", "side": "blocks/pumpkin_side"}})
assets.write_json("blockstates/carved_pumpkin.json", {"variants": {
    "facing=north": {"model": "%s:carved_pumpkin" % M},
    "facing=south": {"model": "%s:carved_pumpkin" % M, "y": 180},
    "facing=west": {"model": "%s:carved_pumpkin" % M, "y": 270},
    "facing=east": {"model": "%s:carved_pumpkin" % M, "y": 90},
}})
assets.item_from_block("carved_pumpkin")
lang.append("tile.%s.carved_pumpkin.name=%s" % (M, assets.name("block.minecraft.carved_pumpkin")))

# Vanilla's pumpkin, mapped here by blocks/client/BlocksClient (not one of our blocks, so check_assets.py
# never sees these two; they are checked below instead).
assets.write_json("models/block/pumpkin.json", {"parent": "block/cube_column", "textures": {
    "end": "blocks/pumpkin_top", "side": "blocks/pumpkin_side"}})
assets.write_json("blockstates/pumpkin.json", {"variants": {"normal": {"model": "%s:pumpkin" % M}}})
assets.item_from_block("pumpkin")

# Undyed shulker box: drawn by its tile entity renderer; the block model is only the breaking particle.
top = assets.texture("block/shulker_box")
assets.texture("entity/shulker/shulker")
assets.write_json("models/block/shulker_box.json", {"textures": {"particle": top}})
assets.write_json("blockstates/shulker_box.json", {"variants": {
    "facing=%s" % f: {"model": "%s:shulker_box" % M} for f in ("down", "up", "north", "south", "west", "east")}})
assets.write_json("models/item/shulker_box.json", {"parent": "item/shulker_box", "textures": {"particle": top}})
lang.append("tile.%s.shulker_box.name=%s" % (M, assets.name("block.minecraft.shulker_box")))

# Wood: vanilla's own bark models, which its axis=none logs use.
for wood in WOODS:
    assets.write_json("models/item/%s_wood.json" % wood, {"parent": "block/%s_bark" % wood})
    lang.append("item.%s.%s_wood.name=%s" % (M, wood, assets.name("block.minecraft.%s_wood" % wood)))
    # 1.13 renamed the logs ("Oak Wood" is now the bark block): vanilla's keys, 1.13's names.
    lang.append("tile.log.%s.name=%s" % (OLD_LOG_KEYS[wood], assets.name("block.minecraft.%s_log" % wood)))

assets.write_json("models/item/debug_stick.json", {"parent": "item/handheld", "textures": {"layer0": "items/stick"}})
lang.append("item.%s.debug_stick.name=%s" % (M, assets.name("item.minecraft.debug_stick")))
for suffix in ("empty", "select", "update"):
    lang.append("item.%s.debug_stick.%s=%s" % (M, suffix, assets.name("item.minecraft.debug_stick." + suffix)))

# Config: our own words.
lang.append("config.%s.blocks.placementRules=Looser Placement" % M)
lang.append("config.%s.blocks.placementRules.tooltip=1.13's looser placement: pumpkins, jack o'lanterns and fence gates "
            "need no block under them, and doors can be placed against the side of a block." % M)

events = {"block.pumpkin.carve": assets.copy_sound("block.pumpkin.carve")}
assets.merge_sounds("blocks", events)
assets.merge_lang("blocks", sorted(lang) + assets.subtitle_lines(events))

# The two vanilla-pumpkin files check_assets.py cannot reach: every texture they name must exist in 1.12.2.
for path in ("assets/minecraft/textures/blocks/pumpkin_top.png", "assets/minecraft/textures/blocks/pumpkin_side.png",
             "assets/minecraft/models/block/cube_column.json"):
    assert path in assets.OLD_NAMES, path
print("blocks: %d lang lines" % len(lang))
