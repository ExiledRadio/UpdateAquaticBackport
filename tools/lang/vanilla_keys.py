"""Maps each en_us.lang key of this mod to the modern Minecraft key it takes its name from, if any.

Rule 4 of the family plan: vanilla names come from Mojang's language files and are never translated by us.
This writes tools/lang/vanilla_keys.json:

    {"vanilla": {"tile.updateaquaticbackport.kelp.name": "block.minecraft.kelp", ...},
     "ours":    ["itemGroup.updateaquaticbackport", "config.updateaquaticbackport.kelp", ...]}

"vanilla" keys get their text in every language from the language files of the Minecraft version that
has that key; "ours" are translated by us. A key is "vanilla" only when its English equals the modern
English exactly, so a name we changed is never silently replaced. Rerun after any generator:

    python tools/lang/vanilla_keys.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
SPECIAL = {
    # 1.13 renamed vanilla's own wooden trapdoor and pressure plate once the other woods had theirs.
    "tile.trapdoor.name": "block.minecraft.oak_trapdoor",
    "tile.pressurePlateWood.name": "block.minecraft.oak_pressure_plate",
    # 1.13 renamed 1.12.2's raw fish (fish stream).
    "item.fish.cod.raw.name": "item.minecraft.cod",
    "item.fish.cod.cooked.name": "item.minecraft.cooked_cod",
    "item.fish.salmon.raw.name": "item.minecraft.salmon",
    "item.fish.salmon.cooked.name": "item.minecraft.cooked_salmon",
    "item.fish.clownfish.raw.name": "item.minecraft.tropical_fish",
    "item.fish.pufferfish.raw.name": "item.minecraft.pufferfish",
}


def candidates(key):
    if key in SPECIAL:
        return [SPECIAL[key]]
    parts = key.split(".")
    if key.startswith("tile.%s." % M) and key.endswith(".name"):
        name = ".".join(parts[2:-1])
        return ["block.minecraft." + name, "item.minecraft." + name]
    if key.startswith("item.%s." % M) and key.endswith(".name"):
        name = ".".join(parts[2:-1])
        return ["item.minecraft." + name, "block.minecraft." + name]
    if key.startswith("subtitles.%s." % M):
        return ["subtitles." + key[len("subtitles.%s." % M):]]
    # The kinds build_langs.py also matches by structure (fish, trident and phantom streams merged here).
    # Entities (entity.<modid>.cod.name) and their sub-keys (entity.<modid>.tropical_fish.type.kob).
    if key.startswith("entity.%s." % M):
        rest = key[len("entity.%s." % M):]
        return ["entity.minecraft." + (rest[:-len(".name")] if rest.endswith(".name") else rest)]
    if key.startswith("effect.%s." % M):
        return ["effect.minecraft." + ".".join(parts[2:])]
    if key.startswith("enchantment.%s." % M):
        return ["enchantment.minecraft." + ".".join(parts[2:])]
    if key.startswith("advancements.%s." % M):
        rest = key[len("advancements.%s." % M):]
        out = ["advancements." + rest]
        if len(parts) == 4:
            out += ["advancements.%s.%s.%s" % (tab, parts[2], parts[3])
                    for tab in ("adventure", "story", "husbandry", "nether", "end")]
        return out
    if key.startswith("color.%s." % M):
        return ["color.minecraft." + key[len("color.%s." % M):]]
    # Potions: potion.effect.<modid>.slow_falling and the splash, lingering and tipped arrow forms.
    for form in ("potion", "splash_potion", "lingering_potion", "tipped_arrow"):
        if key.startswith("%s.effect.%s." % (form, M)):
            return ["item.minecraft.%s.effect.%s" % (form, ".".join(parts[3:]))]
    # Statistics: stat.timeSinceRest -> stat.minecraft.time_since_rest
    if key.startswith("stat.") and len(parts) == 2:
        import re
        return ["stat.minecraft." + re.sub(r"(?<!^)(?=[A-Z])", "_", parts[1]).lower()]
    # A key 1.12.2 lacks but modern Minecraft has under the same name (death messages).
    return [key]


def main():
    lang = os.path.join(assets.ASSETS, "lang", "en_us.lang")
    vanilla, ours, mismatched = {}, [], []
    for line in open(lang, encoding="utf-8").read().splitlines():
        if not line or line.startswith("#") or "=" not in line:
            continue
        key, text = line.split("=", 1)
        found = None
        for cand in candidates(key):
            if cand in assets.VANILLA_LANG:
                found = cand
                break
        # A lang value is one line; a vanilla line break is a space here (as build_langs.py writes it).
        if found and " ".join(assets.VANILLA_LANG[found].split("\n")) == text:
            vanilla[key] = found
        else:
            if found:
                mismatched.append("%s: ours %r, vanilla %s %r" % (key, text, found, assets.VANILLA_LANG[found]))
            ours.append(key)
    out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "vanilla_keys.json")
    with open(out, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps({"vanilla": vanilla, "ours": ours}, indent=2, sort_keys=True) + "\n")
    for m in mismatched:
        print("DIFFERS", m)
    print("%d vanilla names, %d of our own" % (len(vanilla), len(ours)))


if __name__ == "__main__":
    main()
