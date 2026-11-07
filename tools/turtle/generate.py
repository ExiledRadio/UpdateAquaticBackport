"""Turtle stream assets: turtle egg blockstate and models, items, armor and entity textures, sounds, names.

    python tools/turtle/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# Turtle egg: 1.18's blockstate (four random rotations per state), with its models ported to our namespace.
state = assets.jar_json("assets/minecraft/blockstates/turtle_egg.json")
ported = {}
for variant, options in state["variants"].items():
    for option in options if isinstance(options, list) else [options]:
        path = option["model"].split(":", 1)[-1]
        if path not in ported:
            ported[path] = assets.port_model(path)
        # Blockstate model names drop the block/ folder; 1.12.2 adds it back.
        option["model"] = ported[path].replace(":block/", ":")
assets.write_json("blockstates/turtle_egg.json", state)

assets.item_generated("turtle_egg", assets.texture("item/turtle_egg"))
assets.item_generated("scute", assets.texture("item/scute"))
assets.item_generated("turtle_helmet", assets.texture("item/turtle_helmet"))
# The worn shell (ArmorMaterial texture "updateaquaticbackport:turtle") and the turtle itself.
assets.texture("models/armor/turtle_layer_1")
assets.texture("entity/turtle/big_sea_turtle")

events = {}
for e in ["entity.turtle.ambient_land", "entity.turtle.death", "entity.turtle.death_baby", "entity.turtle.egg_break",
          "entity.turtle.egg_crack", "entity.turtle.egg_hatch", "entity.turtle.hurt", "entity.turtle.hurt_baby",
          "entity.turtle.lay_egg", "entity.turtle.shamble", "entity.turtle.shamble_baby", "entity.turtle.swim",
          "entity.zombie.destroy_egg", "item.armor.equip_turtle"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("turtle", events)

names = {
    "tile.%s.turtle_egg.name" % M: assets.name("block.minecraft.turtle_egg"),
    "item.%s.scute.name" % M: assets.name("item.minecraft.scute"),
    "item.%s.turtle_helmet.name" % M: assets.name("item.minecraft.turtle_helmet"),
    "entity.%s.turtle.name" % M: assets.name("entity.minecraft.turtle"),
}
# 1.12.2 names potions by prefix + the potion's base name; all three Turtle Master forms share "turtle_master".
for prefix in ["potion", "splash_potion", "lingering_potion", "tipped_arrow"]:
    names["%s.effect.turtle_master" % prefix] = assets.name("item.minecraft.%s.effect.turtle_master" % prefix)

config = {
    # Our own words (family Rule 4): config names and tooltips for the fields this stream added.
    "config.%s.turtles.spawnWeight" % M: "Beach Spawn Weight",
    "config.%s.turtles.spawnWeight.tooltip" % M:
        "How often turtles spawn on sandy beaches, against the other animals there (1.13: 5). 0 stops them "
        "spawning naturally.",
}

lines = sorted("%s=%s" % kv for kv in names.items())
lines += sorted("%s=%s" % kv for kv in config.items())
lines += assets.subtitle_lines(events)
assets.merge_lang("turtle", lines)
print("turtle: %d models, %d sound events, %d lang lines" % (len(ported), len(events), len(lines)))
