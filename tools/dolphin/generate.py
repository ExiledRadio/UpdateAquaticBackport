"""Dolphin stream assets: the dolphin's texture, sounds and loot table, the Dolphin's Grace icon, and names.

    python -I tools/dolphin/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

assets.texture("entity/dolphin")
assets.texture("mob_effect/dolphins_grace")

events = {}
for e in ["ambient", "ambient_water", "attack", "death", "eat", "hurt", "jump", "play", "splash", "swim"]:
    events["entity.dolphin." + e] = assets.copy_sound("entity.dolphin." + e)
assets.merge_sounds("dolphin", events)

# 1.18's loot table in 1.12.2's format: 0-1 raw cod, +0-1 per Looting level, cooked when the dolphin burns.
# 1.12.2's raw cod is minecraft:fish with data 0.
assets.write_json("loot_tables/entities/dolphin.json", {
    "pools": [{
        "name": "dolphin",
        "rolls": 1,
        "entries": [{
            "type": "item",
            "name": "minecraft:fish",
            "weight": 1,
            "functions": [
                {"function": "set_data", "data": 0},
                {"function": "set_count", "count": {"min": 0, "max": 1}},
                {"function": "looting_enchant", "count": {"min": 0, "max": 1}},
                {"function": "furnace_smelt",
                 "conditions": [{"condition": "entity_properties", "entity": "this", "properties": {"on_fire": True}}]}
            ]
        }]
    }]
})

lang = [
    "entity.%s.dolphin.name=%s" % (M, assets.name("entity.minecraft.dolphin")),
    "effect.%s.dolphins_grace=%s" % (M, assets.name("effect.minecraft.dolphins_grace")),
    "death.attack.dryout=%s" % assets.name("death.attack.dryout"),
    # Current Java's wording (1.18's said "whilst"; the family writes American English and the newest names).
    "death.attack.dryout.player=%1$s died from dehydration while trying to escape %2$s",
    # Our own: the config option this stream adds (its comment, as the config screen's tooltip).
    "config.%s.dolphins.spawnWeight=Spawn Weight" % M,
    "config.%s.dolphins.spawnWeight.tooltip=How often dolphins spawn in oceans, against the squid's 10. 0 stops "
    "natural spawning. Lukewarm and warm oceans get a fifth of this; cold and frozen oceans none." % M,
]
assets.merge_lang("dolphin", sorted(lang) + assets.subtitle_lines(events))
print("dolphin: %d names, %d sound events" % (len(lang), len(events)))
