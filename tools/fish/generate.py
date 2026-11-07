"""Fish stream assets: entity textures, bucket items, sounds, loot tables, advancements and names.

    python tools/fish/generate.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
FISH = ["cod", "salmon", "pufferfish", "tropical_fish"]
# 1.12.2's raw fish metadata: 0 cod, 1 salmon, 2 clownfish (tropical fish), 3 pufferfish.
META = {"cod": 0, "salmon": 1, "tropical_fish": 2, "pufferfish": 3}
TYPES = ["kob", "sunstreak", "snooper", "dasher", "brinely", "spotty",
         "flopper", "stripey", "glitter", "blockfish", "betty", "clayfish"]
COLORS = ["white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
          "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"]

lang = []

# --- entity textures ---------------------------------------------------------------------------
textures = ["cod", "salmon", "pufferfish", "tropical_a", "tropical_b"]
textures += ["tropical_%s_pattern_%d" % (s, p) for s in "ab" for p in range(1, 7)]
for t in textures:
    assets.texture("entity/fish/" + t)

# --- buckets -----------------------------------------------------------------------------------
for f in FISH:
    name = f + "_bucket"
    assets.item_generated(name, assets.texture("item/" + name))
    lang.append("item.%s.%s.name=%s" % (M, name, assets.name("item.minecraft." + name)))
# 1.13 joins a tropical fish's two color names with a comma.
lang.append("item.%s.tropical_fish_bucket.colors=%%s, %%s" % M)

# --- entity names, tropical fish types, predefined fish, colors --------------------------------
for f in FISH:
    lang.append("entity.%s.%s.name=%s" % (M, f, assets.name("entity.minecraft." + f)))
for t in TYPES:
    key = "entity.minecraft.tropical_fish.type." + t
    lang.append("entity.%s.tropical_fish.type.%s=%s" % (M, t, assets.name(key)))
for i in range(22):
    key = "entity.minecraft.tropical_fish.predefined.%d" % i
    lang.append("entity.%s.tropical_fish.predefined.%d=%s" % (M, i, assets.name(key)))
for c in COLORS:
    lang.append("color.%s.%s=%s" % (M, c, assets.name("color.minecraft." + c)))

# --- vanilla's raw fish, renamed as 1.13 did ---------------------------------------------------
# Our language file loads after Minecraft's (resource domains keep their load order), so these
# lines replace 1.12.2's names: Raw Fish -> Raw Cod, Cooked Fish -> Cooked Cod, Clownfish -> Tropical Fish.
RENAMES = {
    "item.fish.cod.raw.name": "item.minecraft.cod",
    "item.fish.cod.cooked.name": "item.minecraft.cooked_cod",
    "item.fish.salmon.raw.name": "item.minecraft.salmon",
    "item.fish.salmon.cooked.name": "item.minecraft.cooked_salmon",
    "item.fish.clownfish.raw.name": "item.minecraft.tropical_fish",
    "item.fish.pufferfish.raw.name": "item.minecraft.pufferfish",
}
for old, new in RENAMES.items():
    lang.append("%s=%s" % (old, assets.name(new)))

# --- loot tables (1.14.4's, with 1.12.2's raw fish metadata) -----------------------------------
for f in FISH:
    fish_entry = {"type": "item", "name": "minecraft:fish", "entryName": f, "weight": 1,
                  "functions": [{"function": "set_data", "data": META[f]}]}
    if f in ("cod", "salmon"):
        fish_entry["functions"].append({"function": "furnace_smelt", "conditions": [
            {"condition": "entity_properties", "entity": "this", "properties": {"on_fire": True}}]})
    assets.write_json("loot_tables/entities/%s.json" % f, {"pools": [
        {"name": f, "rolls": 1, "entries": [fish_entry]},
        {"name": "bone_meal", "rolls": 1, "conditions": [{"condition": "random_chance", "chance": 0.05}],
         "entries": [{"type": "item", "name": "minecraft:dye", "entryName": "bone_meal", "weight": 1,
                      "functions": [{"function": "set_data", "data": 15}]}]},
    ]})

# --- advancements: Fishy Business and Tactical Fishing, on 1.12.2's husbandry tab --------------
def adv_display(icon, key):
    lang.append("advancements.%s.husbandry.%s.title=%s" % (M, key, assets.name("advancements.husbandry.%s.title" % key)))
    lang.append("advancements.%s.husbandry.%s.description=%s"
                % (M, key, assets.name("advancements.husbandry.%s.description" % key)))
    return {"icon": icon,
            "title": {"translate": "advancements.%s.husbandry.%s.title" % (M, key)},
            "description": {"translate": "advancements.%s.husbandry.%s.description" % (M, key)},
            "frame": "task", "show_toast": True, "announce_to_chat": True, "hidden": False}


criteria = {}
for f in FISH:
    criteria[f] = {"trigger": "%s:fishing_rod_hooked" % M,
                   "conditions": {"item": {"item": "minecraft:fish", "data": META[f]}}}
assets.write_json("advancements/husbandry/fishy_business.json", {
    "parent": "minecraft:husbandry/root",
    "display": adv_display({"item": "minecraft:fishing_rod"}, "fishy_business"),
    "criteria": criteria,
    "requirements": [FISH],
})
criteria = {}
for f in FISH:
    criteria[f + "_bucket"] = {"trigger": "%s:filled_bucket" % M,
                               "conditions": {"item": {"item": "%s:%s_bucket" % (M, f)}}}
assets.write_json("advancements/husbandry/tactical_fishing.json", {
    "parent": "%s:husbandry/fishy_business" % M,
    "display": adv_display({"item": "%s:pufferfish_bucket" % M}, "tactical_fishing"),
    "criteria": criteria,
    "requirements": [[f + "_bucket" for f in FISH]],
})

# --- sounds ------------------------------------------------------------------------------------
events = {}
for e in ["entity.cod.ambient", "entity.cod.death", "entity.cod.flop", "entity.cod.hurt",
          "entity.salmon.ambient", "entity.salmon.death", "entity.salmon.flop", "entity.salmon.hurt",
          "entity.puffer_fish.ambient", "entity.puffer_fish.blow_out", "entity.puffer_fish.blow_up",
          "entity.puffer_fish.death", "entity.puffer_fish.flop", "entity.puffer_fish.hurt", "entity.puffer_fish.sting",
          "entity.tropical_fish.ambient", "entity.tropical_fish.death", "entity.tropical_fish.flop",
          "entity.tropical_fish.hurt", "entity.fish.swim", "item.bucket.fill_fish", "item.bucket.empty_fish"]:
    events[e] = assets.copy_sound(e)
assets.merge_sounds("fish", events)
assets.merge_lang("fish", sorted(lang) + assets.subtitle_lines(events))
print("fish: %d names, %d sound events, %d textures" % (len(lang), len(events), len(textures)))
