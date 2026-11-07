"""Phantom stream assets: the phantom's textures and sounds, the membrane, Slow Falling's icon, the loot table
and every name, from the 1.18.2 jar and the 1.18 asset index.

    python tools/phantom/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# Textures: the entity and its eyes (1.18 keeps 1.14's 64x64 layout), the effect icon, the item.
assets.texture("entity/phantom")
assets.texture("entity/phantom_eyes")
assets.texture("mob_effect/slow_falling")
assets.item_generated("phantom_membrane", assets.texture("item/phantom_membrane"))

# Sounds.
events = {}
for e in ["entity.phantom.ambient", "entity.phantom.bite", "entity.phantom.death", "entity.phantom.flap",
          "entity.phantom.hurt", "entity.phantom.swoop"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("phantom", events)

# 1.18's loot table in 1.12.2's format: 0-1 membrane, Looting adds 0-1 per level, only when a player kills it.
# Forge 1.12 wants every pool of a mod's table named.
assets.write_json("loot_tables/entities/phantom.json", {
    "pools": [{
        "name": "phantom_membrane",
        "rolls": 1,
        "entries": [{
            "type": "item",
            "name": "%s:phantom_membrane" % M,
            "weight": 1,
            "functions": [
                {"function": "set_count", "count": {"min": 0, "max": 1}},
                {"function": "looting_enchant", "count": {"min": 0, "max": 1}},
            ],
        }],
        "conditions": [{"condition": "killed_by_player"}],
    }]
})

names = {
    "entity.%s.phantom.name" % M: assets.name("entity.minecraft.phantom"),
    "item.%s.phantom_membrane.name" % M: assets.name("item.minecraft.phantom_membrane"),
    "effect.%s.slow_falling" % M: assets.name("effect.minecraft.slow_falling"),
    "potion.effect.%s.slow_falling" % M: assets.name("item.minecraft.potion.effect.slow_falling"),
    "splash_potion.effect.%s.slow_falling" % M: assets.name("item.minecraft.splash_potion.effect.slow_falling"),
    "lingering_potion.effect.%s.slow_falling" % M: assets.name("item.minecraft.lingering_potion.effect.slow_falling"),
    "tipped_arrow.effect.%s.slow_falling" % M: assets.name("item.minecraft.tipped_arrow.effect.slow_falling"),
    "stat.timeSinceRest": assets.name("stat.minecraft.time_since_rest"),
    # Our own: the config option.
    "config.%s.phantoms.insomnia" % M: "Insomnia",
    "config.%s.phantoms.insomnia.tooltip" % M: "Phantoms spawn at night over players who have not slept for three "
                                               "days. Off, they never spawn on their own in any world; the "
                                               "doInsomnia game rule turns them off in one world.",
}
lines = sorted("%s=%s" % kv for kv in names.items()) + assets.subtitle_lines(events)
assets.merge_lang("phantom", lines)
print("phantom: %d sound events, %d names" % (len(events), len(lines)))
