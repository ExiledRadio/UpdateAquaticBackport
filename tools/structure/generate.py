"""Structure stream assets: today's shipwreck and ocean ruin files, the six chest loot tables, the red X map
icon and the buried treasure map's name.

    python tools/structure/generate.py

Sources:
  * structure files and loot tables: the 26.3 client jar (G:/MojangLang/26.3/client.jar), Mojang's own
    files unchanged (Backport Core's ModernTemplate reads them as they are);
  * the red X: 26.3's map decoration sprite, drawn into a copy of 1.12.2's map_icons.png at slot 10, the
    slot 1.12.2's renderer uses for the eleventh decoration type (the one this stream adds);
  * the map's name: 1.18's en_us.json through assets.name().

Loot tables are today's, written in 1.12.2's format under updateaquaticbackport:chests/. Items 1.12.2 and
this mod do not have keep their weight as an empty entry, so every other item comes up exactly as often as
today; pools that hold nothing 1.12.2 has are left out. The heart of the sea (the conduit stream's item)
is added to buried treasure in code, when that stream is on: a loot table naming a missing item fails to
load as a whole.
"""
import io
import json
import os
import sys
import zipfile

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
NEW = zipfile.ZipFile("G:/MojangLang/26.3/client.jar")

# ---- structure files

count = 0
for name in sorted(NEW.namelist()):
    for folder in ("shipwreck", "underwater_ruin"):
        prefix = "data/minecraft/structure/%s/" % folder
        if name.startswith(prefix) and name.endswith(".nbt"):
            assets.write("structures/%s/%s" % (folder, name[len(prefix):]), NEW.read(name))
            count += 1
assert count == 68, count

# ---- loot tables

# Today's item -> (1.12.2 item, metadata or None). Anything not listed and not in 1.12.2 is absent.
ITEMS = {
    "minecraft:cooked_cod": ("minecraft:cooked_fish", 0),
    "minecraft:cooked_salmon": ("minecraft:cooked_fish", 1),
    "minecraft:lapis_lazuli": ("minecraft:dye", 4),
    "minecraft:buried_treasure_map": ("minecraft:map", None),   # given its map by our function
}
SAME = {"paper", "potato", "poisonous_potato", "carrot", "wheat", "coal", "rotten_flesh", "pumpkin",
        "gunpowder", "tnt", "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
        "iron_ingot", "gold_ingot", "emerald", "diamond", "experience_bottle", "iron_nugget", "gold_nugget",
        "prismarine_crystals", "iron_sword", "potion", "compass", "map", "clock", "feather", "book",
        "golden_apple", "golden_helmet", "fishing_rod", "stone_axe"}
# Added in code (ours, from another stream): left out of the file.
IN_CODE = {"minecraft:heart_of_the_sea"}


def count_fn(count):
    if isinstance(count, dict):
        assert count["type"] == "minecraft:uniform", count
        return {"function": "set_count", "count": {"min": count["min"], "max": count["max"]}}
    return {"function": "set_count", "count": count}


def port_modifiers(mods, out_name):
    """Today's item modifiers -> 1.12.2 functions."""
    if mods is None:
        return []
    if isinstance(mods, dict):
        mods = [mods]
    out = []
    for m in mods:
        t = m["type"]
        if t == "minecraft:set_count":
            if m["count"] != 1:
                out.append(count_fn(m["count"]))
        elif t == "minecraft:enchant_randomly":
            # #minecraft:on_random_loot is every enchantment that applies, treasure ones included: 1.12.2's
            # enchant_randomly with no list.
            assert m.get("options") == "#minecraft:on_random_loot", m
            out.append({"function": "enchant_randomly"})
        elif t == "minecraft:set_potion":
            out.append({"function": "set_nbt", "tag": "{Potion:\"%s\"}" % m["id"]})
        elif t == "minecraft:exploration_map":
            assert m["destination"] == "#minecraft:on_treasure_maps", m
            out.append({"function": "%s:exploration_map" % M, "destination": "buried_treasure",
                        "decoration": m["decoration"].split(":")[1], "zoom": m.get("zoom", 2),
                        "search_radius": m.get("search_radius", 50),
                        "skip_existing_chunks": m.get("skip_existing_chunks", True)})
        elif t == "minecraft:filtered":
            # "Discard the map if no structure was found": our function gives nothing then.
            pass
        else:
            raise ValueError("unported modifier %s in %s" % (t, out_name))
    return out


absent_seen = set()


def port_table(name):
    src = json.loads(NEW.read("data/minecraft/loot_table/chests/%s.json" % name).decode("utf-8"))
    pools = []
    for i, pool in enumerate(src["pools"]):
        pool_mods = pool.get("modifier", pool.get("functions"))
        entries = []
        real = 0
        for e in pool["entries"]:
            weight = e.get("weight", 1)
            if e["type"] == "minecraft:empty":
                entries.append({"type": "empty", "entryName": "empty", "weight": weight})
                continue
            assert e["type"] == "minecraft:item", e
            today = e["name"]
            if today in IN_CODE:
                continue
            path = today.split(":")[1]
            if today in ITEMS:
                item, meta = ITEMS[today]
            elif path in SAME:
                item, meta = today, None
            else:
                absent_seen.add(today)
                entries.append({"type": "empty", "entryName": today, "weight": weight})
                continue
            fns = []
            if meta:
                fns.append({"function": "set_data", "data": meta})
            fns += port_modifiers(e.get("modifier", e.get("functions")), name)
            fns += port_modifiers(pool_mods, name)
            entry = {"type": "item", "name": item, "entryName": today, "weight": weight}
            if fns:
                entry["functions"] = fns
            entries.append(entry)
            real += 1
        if real == 0:
            continue
        # Forge reads entry names per pool; two empty entries need different names.
        empties = [x for x in entries if x["entryName"] == "empty"]
        if len(empties) > 1:
            for k, x in enumerate(empties):
                x["entryName"] = "empty_%d" % k
        rolls = pool["rolls"]
        if isinstance(rolls, dict):
            assert rolls["type"] == "minecraft:uniform"
            rolls = {"min": rolls["min"], "max": rolls["max"]}
        pools.append({"name": "pool%d" % i, "rolls": rolls, "entries": entries})
    assets.write_json("loot_tables/chests/%s.json" % name, {"pools": pools})


TABLES = ["shipwreck_map", "shipwreck_supply", "shipwreck_treasure", "underwater_ruin_big",
          "underwater_ruin_small", "buried_treasure"]
for t in TABLES:
    port_table(t)

# ---- the red X map icon

from PIL import Image  # noqa: E402

icons = Image.open(io.BytesIO(assets.OLD_JAR.read("assets/minecraft/textures/map/map_icons.png"))).convert("RGBA")
cell = icons.size[0] // 4
red_x = Image.open(io.BytesIO(NEW.read("assets/minecraft/textures/map/decorations/red_x.png"))).convert("RGBA")
if red_x.size[0] != cell:
    red_x = red_x.resize((cell, cell), Image.NEAREST)
slot = 10
icons.paste(red_x, ((slot % 4) * cell, (slot // 4) * cell))
out = io.BytesIO()
icons.save(out, "PNG")
full = os.path.join(assets.ROOT, "src", "main", "resources", "assets", "minecraft", "textures", "map", "map_icons.png")
os.makedirs(os.path.dirname(full), exist_ok=True)
with open(full, "wb") as f:
    f.write(out.getvalue())

# ---- names

lang = ["filled_map.buried_treasure=%s" % assets.name("filled_map.buried_treasure")]
assets.merge_lang("structure", lang)
print("structure: %d structure files, %d loot tables, absent items kept as empty weight: %s"
      % (count, len(TABLES), ", ".join(sorted(absent_seen))))
