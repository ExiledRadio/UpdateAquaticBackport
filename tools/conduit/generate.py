"""Conduit stream assets: the conduit's blockstate, models and renderer textures, the nautilus particle,
Conduit Power's icon, the nautilus shell and heart of the sea items, the conduit sounds and the names.

    python tools/conduit/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID


def bs(model):
    """Blockstate model names drop the block/ folder; 1.12.2 adds it back."""
    return model.replace(":block/", ":")


# The conduit: drawn by its tile entity renderer. The block model is 1.18's (a particle texture and no
# elements); the water of a waterlogged conduit is Fluidlogged API's to draw.
block_model = assets.port_model("block/conduit")
assets.multipart_blockstate("conduit", [(None, bs(block_model))])
# 1.18's item model: builtin/entity, drawn by the item renderer, with its display transforms.
assets.port_model("item/conduit")

# Renderer textures, written as plain images: 1.18's wind strips carry an animation section for its sprite
# atlas, but 1.12.2 binds them whole and the renderer picks the frame, as 1.14's did.
for name in ["base", "cage", "wind", "wind_vertical", "open_eye", "closed_eye"]:
    path = "textures/entity/conduit/%s.png" % name
    assets.write(path, assets.jar_bytes("assets/minecraft/" + path))

assets.texture("particle/nautilus")
assets.texture("mob_effect/conduit_power")

for item in ["nautilus_shell", "heart_of_the_sea"]:
    assets.item_generated(item, assets.texture("item/" + item))

lang = [
    "tile.%s.conduit.name=%s" % (M, assets.name("block.minecraft.conduit")),
    "item.%s.nautilus_shell.name=%s" % (M, assets.name("item.minecraft.nautilus_shell")),
    "item.%s.heart_of_the_sea.name=%s" % (M, assets.name("item.minecraft.heart_of_the_sea")),
    "effect.%s.conduit_power=%s" % (M, assets.name("effect.minecraft.conduit_power")),
]

events = {}
for e in ["block.conduit.activate", "block.conduit.ambient", "block.conduit.ambient.short",
          "block.conduit.attack.target", "block.conduit.deactivate"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("conduit", events)
assets.merge_lang("conduit", sorted(lang) + assets.subtitle_lines(events))
print("conduit: %d names, %d sound events" % (len(lang), len(events)))
