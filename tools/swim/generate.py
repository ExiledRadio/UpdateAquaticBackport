"""Swimming stream: the config names and tooltips (the stream adds no blocks, items, textures or sounds).

    python tools/swim/generate.py

Each tooltip is the field's @Config.Comment, read from ModConfig so the two always agree; the category's
own name and tooltip come from the @Config.Comment on the `swimming` field.
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID
ROOT = assets.ROOT

CATEGORY = "Swimming and Crawling"
NAMES = {
    "enabled": "Swimming",
    "crawling": "Crawling",
}


def joined(comment):
    return " ".join(re.findall(r'"((?:[^"\\]|\\.)*)"', comment)).replace('\\"', '"')


config = open(os.path.join(ROOT, "src", "main", "java", "com", "exiledradio", M, "ModConfig.java"),
              encoding="utf-8").read()
field = re.search(r'@Config\.Comment\((\{[^}]*\}|"(?:[^"\\]|\\.)*")\)\s*public static final Swimming swimming', config)
if not field:
    raise SystemExit("swim: no @Config.Comment on ModConfig.swimming")
body = config[config.index("public static final class Swimming"):]
body = body[:body.index("\n    }\n")]
comments = {}
for m in re.finditer(r'@Config\.Comment\((\{.*?\}|".*?")\)\s*(?:@[^\n]*\s*)*public boolean (\w+)', body, re.S):
    comments[m.group(2)] = joined(m.group(1))

lang = ["config.%s.swimming=%s" % (M, CATEGORY),
        "config.%s.swimming.tooltip=%s" % (M, joined(field.group(1)))]
for key, label in NAMES.items():
    if key not in comments:
        raise SystemExit("swim: no @Config.Comment for %s" % key)
    lang.append("config.%s.swimming.%s=%s" % (M, key, label))
    lang.append("config.%s.swimming.%s.tooltip=%s" % (M, key, comments[key]))
missing = set(comments) - set(NAMES)
if missing:
    raise SystemExit("swim: no name for %s" % sorted(missing))

assets.merge_lang("swim", lang)
print("swim: %d lang lines" % len(lang))
