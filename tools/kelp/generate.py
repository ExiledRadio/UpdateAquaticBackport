"""Kelp stream assets: blockstates, models, textures and names, from the 1.18.2 jar.

    python tools/kelp/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

kelp = assets.port_model("block/kelp")
kelp_plant = assets.port_model("block/kelp_plant")
seagrass = assets.port_model("block/seagrass")
tall_top = assets.port_model("block/tall_seagrass_top")
tall_bottom = assets.port_model("block/tall_seagrass_bottom")
dried_block = assets.port_model("block/dried_kelp_block")

# Blockstate model names drop the block/ folder; 1.12.2 adds it back.
def bs(model):
    return model.replace(":block/", ":")


assets.pseudo_water_blockstate("kelp", [(None, bs(kelp))])
assets.pseudo_water_blockstate("kelp_plant", [(None, bs(kelp_plant))])
assets.pseudo_water_blockstate("seagrass", [(None, bs(seagrass))])
assets.pseudo_water_blockstate("tall_seagrass", [({"half": "lower"}, bs(tall_bottom)), ({"half": "upper"}, bs(tall_top))])
assets.write_json("blockstates/dried_kelp_block.json", {"variants": {"normal": {"model": bs(dried_block)}}})

assets.item_generated("kelp", assets.texture("item/kelp"))
assets.item_generated("seagrass", assets.texture("item/seagrass"))
assets.item_generated("dried_kelp", assets.texture("item/dried_kelp"))
assets.item_from_block("dried_kelp_block")

names = {
    "tile.%s.kelp.name" % M: assets.name("block.minecraft.kelp"),
    "tile.%s.kelp_plant.name" % M: assets.name("block.minecraft.kelp_plant"),
    "tile.%s.seagrass.name" % M: assets.name("block.minecraft.seagrass"),
    "tile.%s.tall_seagrass.name" % M: assets.name("block.minecraft.tall_seagrass"),
    "tile.%s.dried_kelp_block.name" % M: assets.name("block.minecraft.dried_kelp_block"),
    "item.%s.dried_kelp.name" % M: assets.name("item.minecraft.dried_kelp"),
}
assets.merge_lang("kelp", sorted("%s=%s" % kv for kv in names.items()))
print("kelp: done")
