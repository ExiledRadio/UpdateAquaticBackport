"""Trident stream assets: textures, item models, sounds, the drowned's loot table, two advancements and
the language lines, from the 1.18.2 jar and the 1.18 asset index.

    python tools/trident/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

# --- textures ----------------------------------------------------------------------------------------
for t in ["entity/trident", "entity/trident_riptide", "entity/zombie/drowned", "entity/zombie/drowned_outer_layer"]:
    assets.texture(t)
item_texture = assets.texture("item/trident")

# --- item models ---------------------------------------------------------------------------------------
# trident: the flat icon (inventories, ground, item frames). trident_in_hand / trident_throwing: the 3D
# model drawn by the item renderer, with 1.18's display transforms. The client's baked model picks
# between them; 1.18's override list is replaced by that, so it is left out here.
assets.item_generated("trident", item_texture)
for name in ["trident_in_hand", "trident_throwing"]:
    data = assets.jar_json("assets/minecraft/models/item/%s.json" % name)
    data.pop("overrides", None)
    data.pop("gui_light", None)
    data["textures"] = {"particle": item_texture}
    assets.write_json("models/item/%s.json" % name, data)

# --- sounds --------------------------------------------------------------------------------------------
EVENTS = [
    "item.trident.hit", "item.trident.hit_ground", "item.trident.return", "item.trident.riptide_1",
    "item.trident.riptide_2", "item.trident.riptide_3", "item.trident.throw", "item.trident.thunder",
    "entity.drowned.ambient", "entity.drowned.ambient_water", "entity.drowned.death", "entity.drowned.death_water",
    "entity.drowned.hurt", "entity.drowned.hurt_water", "entity.drowned.shoot", "entity.drowned.step",
    "entity.drowned.swim", "entity.zombie.converted_to_drowned", "entity.husk.converted_to_zombie",
]
events = {e: assets.copy_sound(e) for e in EVENTS}
assets.merge_sounds("trident", events)

# --- loot: current Java's drowned without the copper ingot (a later update's) ---------------------------
assets.write_json("loot_tables/entities/drowned.json", {"pools": [{
    "name": "rotten_flesh",
    "rolls": 1,
    "entries": [{
        "type": "item", "name": "minecraft:rotten_flesh", "weight": 1,
        "functions": [
            {"function": "set_count", "count": {"min": 0, "max": 2}},
            {"function": "looting_enchant", "count": {"min": 0, "max": 1}},
        ],
    }],
}]})


# --- advancements --------------------------------------------------------------------------------------
def display(name):
    return {
        "icon": {"item": "%s:trident" % M},
        "title": {"translate": "advancements.%s.%s.title" % (M, name)},
        "description": {"translate": "advancements.%s.%s.description" % (M, name)},
        "frame": "task",
    }


assets.write_json("advancements/adventure/throw_trident.json", {
    "parent": "minecraft:adventure/kill_a_mob",
    "display": display("throw_trident"),
    "criteria": {"shot_trident": {
        "trigger": "minecraft:player_hurt_entity",
        "conditions": {"damage": {"type": {"is_projectile": True, "direct_entity": {"type": "%s:trident" % M}}}},
    }},
})
# 1.12.2 has no channeled_lightning trigger: trident/Channeling grants this criterion itself.
assets.write_json("advancements/adventure/very_very_frightening.json", {
    "parent": "%s:adventure/throw_trident" % M,
    "display": display("very_very_frightening"),
    "criteria": {"struck_villager": {"trigger": "minecraft:impossible"}},
})

# --- language ------------------------------------------------------------------------------------------
names = {
    "item.%s.trident.name" % M: assets.name("item.minecraft.trident"),
    "entity.%s.trident.name" % M: assets.name("entity.minecraft.trident"),
    "entity.%s.drowned.name" % M: assets.name("entity.minecraft.drowned"),
    "death.attack.trident": assets.name("death.attack.trident"),
    "death.attack.trident.item": assets.name("death.attack.trident.item"),
}
for e in ["loyalty", "riptide", "impaling", "channeling"]:
    names["enchantment.%s.%s" % (M, e)] = assets.name("enchantment.minecraft.%s" % e)
for a in ["throw_trident", "very_very_frightening"]:
    for part in ["title", "description"]:
        names["advancements.%s.%s.%s" % (M, a, part)] = assets.name("advancements.adventure.%s.%s" % (a, part))

# Our own words: the stream's new config fields (name and tooltip, as the @Config comments).
config = {
    "drownedSpawn": ("Drowned Spawning",
                     "Drowned spawn in oceans and rivers. Off, they come only from zombies that drown and from spawn eggs."),
    "zombiesConvert": ("Zombies Turn into Drowned",
                       "A zombie underwater for 30 seconds turns into a drowned, and a husk into a zombie."),
    "impalingMobs": ("Impaling Mobs",
                     "Mobs Impaling counts as aquatic, by entity id, besides squid, guardians and every other water mob. "
                     "Ids of mods that are not installed are ignored."),
}
for field, (title, tooltip) in config.items():
    names["config.%s.trident.%s" % (M, field)] = title
    names["config.%s.trident.%s.tooltip" % (M, field)] = tooltip

# A lang value is one line: a vanilla line break becomes a space, as the family's language tool does.
lines = sorted("%s=%s" % (k, " ".join(v.split("\n"))) for k, v in names.items()) + assets.subtitle_lines(events)
assets.merge_lang("trident", lines)
print("trident: %d sound events, %d lang lines" % (len(events), len(lines)))
