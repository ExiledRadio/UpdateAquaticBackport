"""Shared helpers for the streams' asset generators: textures, sounds and names from modern Minecraft.

Sources (the newest local copies):
  * textures and models: the 1.18.2 client jar;
  * sounds, sounds.json and en_us.json: the 1.18 asset index and its object store.

Every stream's generator (tools/<stream>/generate.py) imports this module, copies what it needs into
src/main/resources/assets/updateaquaticbackport/, and merges its sound events and language lines into the
shared sounds.json and en_us.lang between markers of its own, so generators never overwrite each other.
"""
import hashlib
import json
import os
import shutil
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODID = "updateaquaticbackport"
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MODID)
INSTALL = "G:/Sort this shit/Minecraft/Install"
JAR = zipfile.ZipFile(INSTALL + "/versions/1.18.2/1.18.2.jar")
INDEX = json.load(open(INSTALL + "/assets/indexes/1.18.json", encoding="utf-8"))["objects"]


def _object(path):
    """@return the bytes of an asset-index object such as 'minecraft/sounds.json'."""
    h = INDEX[path]["hash"]
    with open("%s/assets/objects/%s/%s" % (INSTALL, h[:2], h), "rb") as f:
        return f.read()


VANILLA_SOUNDS = json.loads(_object("minecraft/sounds.json").decode("utf-8"))
# English is the one language shipped inside the client jar rather than the asset index.
VANILLA_LANG = json.loads(JAR.read("assets/minecraft/lang/en_us.json").decode("utf-8"))


def write(rel, data):
    """Writes bytes or text to a path under our assets folder, creating directories."""
    full = os.path.join(ASSETS, *rel.split("/"))
    os.makedirs(os.path.dirname(full), exist_ok=True)
    mode = "wb" if isinstance(data, bytes) else "w"
    with open(full, mode, **({} if mode == "wb" else {"encoding": "utf-8", "newline": "\n"})) as f:
        f.write(data)


def write_json(rel, obj):
    write(rel, json.dumps(obj, indent=2) + "\n")


def jar_bytes(path):
    return JAR.read(path)


def jar_json(path):
    return json.loads(JAR.read(path).decode("utf-8"))


def texture(src, dst=None):
    """Copies textures/<src>.png (and its .mcmeta, for animated ones) from the 1.18.2 jar to ours.

    @param src like 'block/kelp'; @param dst our path, default the same
    @return our texture name, 'updateaquaticbackport:<dst>'
    """
    dst = dst or src
    write("textures/%s.png" % dst, jar_bytes("assets/minecraft/textures/%s.png" % src))
    meta = "assets/minecraft/textures/%s.png.mcmeta" % src
    if meta in JAR.namelist():
        write("textures/%s.png.mcmeta" % dst, jar_bytes(meta))
    return "%s:%s" % (MODID, dst)


def vanilla_sound(event):
    """@return 1.18's sounds.json entry for an event such as 'block.wet_grass.break'."""
    return VANILLA_SOUNDS[event]


def copy_sound(event, our_event=None):
    """Copies an event's .ogg files into our sounds/ and returns our sounds.json entry for it.

    Files keep their vanilla paths under our namespace; event references stay as they are.
    """
    spec = json.loads(json.dumps(VANILLA_SOUNDS[event]))
    out = []
    for sound in spec["sounds"]:
        entry = dict(sound) if isinstance(sound, dict) else {"name": sound}
        if entry.get("type") == "event":
            out.append(entry)
            continue
        name = entry["name"]
        ns, _, path = name.rpartition(":")
        data = _object("minecraft/sounds/%s.ogg" % path)
        write("sounds/%s.ogg" % path, data)
        entry["name"] = "%s:%s" % (MODID, path)
        out.append(entry if len(entry) > 1 else entry["name"])
    spec["sounds"] = out
    if "subtitle" in spec:
        spec["subtitle"] = spec["subtitle"].replace("subtitles.", "subtitles.%s." % MODID, 1)
    return spec


def name(key):
    """@return 1.18's English name for a translation key such as 'block.minecraft.kelp'."""
    return VANILLA_LANG[key]


def merge_sounds(section, events):
    """Replaces this generator's events in sounds.json (keys it wrote before are dropped first)."""
    path = os.path.join(ASSETS, "sounds.json")
    data = json.load(open(path, encoding="utf-8")) if os.path.exists(path) else {}
    marker = os.path.join(ROOT, "tools", section, ".sounds")
    if os.path.exists(marker):
        for old in open(marker, encoding="utf-8").read().split():
            data.pop(old, None)
    data.update(events)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(json.dumps(data, indent=2) + "\n")
    with open(marker, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(sorted(events)) + "\n")


def merge_lang(section, lines):
    """Replaces the block between '# --- <section> ---' and the next '# ---' (or the end) in en_us.lang."""
    path = os.path.join(ASSETS, "lang", "en_us.lang")
    text = open(path, encoding="utf-8").read().rstrip("\n").split("\n")
    head = "# --- %s ---" % section
    out = []
    skipping = False
    for line in text:
        if line == head:
            skipping = True
            continue
        if skipping and line.startswith("# --- "):
            skipping = False
        if not skipping:
            out.append(line)
    while out and out[-1] == "":
        out.pop()
    out += ["", head] + lines
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write("\n".join(out) + "\n")


def item_generated(name, texture_name):
    """Writes models/item/<name>.json as a flat item with one texture."""
    write_json("models/item/%s.json" % name, {"parent": "item/generated", "textures": {"layer0": texture_name}})


def item_from_block(name, model=None):
    """Writes models/item/<name>.json that shows the block model."""
    write_json("models/item/%s.json" % name, {"parent": "%s:block/%s" % (MODID, model or name)})
