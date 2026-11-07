"""Mob changes stream assets: sound events, subtitles and config names.

    python tools/mobs/generate.py

No textures or models: the stream changes vanilla mobs. The squid ink particle uses 1.12.2's own
generic smoke sprites.
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
import assets  # noqa: E402

M = assets.MODID

events = {}
for e in ["entity.squid.squirt", "entity.skeleton_horse.ambient_water", "entity.skeleton_horse.gallop_water",
          "entity.skeleton_horse.step_water", "entity.skeleton_horse.swim"]:
    events[e] = assets.copy_sound(e)


def imitation(event, imitated):
    """1.18's parrot imitation: another event, at its own pitch and volume, pointed at our copy of that event.

    1.12.2 multiplies an event reference's pitch and volume in as 1.18 does. The imitated event belongs to
    another stream (phantoms, drowned); without it the reference is simply never played.
    """
    spec = assets.vanilla_sound(event)
    sounds = []
    for sound in spec["sounds"]:
        entry = dict(sound)
        assert entry.get("type") == "event" and entry["name"] == imitated, (event, entry)
        entry["name"] = "%s:%s" % (M, imitated)
        sounds.append(entry)
    return {"sounds": sounds, "subtitle": spec["subtitle"].replace("subtitles.", "subtitles.%s." % M, 1)}


events["entity.parrot.imitate.phantom"] = imitation("entity.parrot.imitate.phantom", "entity.phantom.ambient")
events["entity.parrot.imitate.drowned"] = imitation("entity.parrot.imitate.drowned", "entity.drowned.ambient")
assets.merge_sounds("mobs", events)

# Config names and tooltips (the tooltips repeat the @Config.Comment text in ModConfig.Mobs).
config = [
    ("undeadSink", "Undead Sink",
     "Zombies, skeletons and their kinds, and skeleton and zombie horses, sink in water instead of swimming "
     "to the surface, and no undead mob drowns."),
    ("babyZombiesBurn", "Baby Zombies Burn", "Baby zombies burn in daylight like adult ones."),
    ("squidInk", "Squid Ink", "A squid that is hurt squirts a cloud of ink and swims away from whatever hurt it."),
    ("polarBearsOnIce", "Polar Bears on Ice", "Polar bears spawn on the ice of frozen oceans."),
    ("parrotsImitate", "Parrots Imitate", "Parrots imitate phantoms and drowned when those mobs are in the game."),
    ("skeletonHorsesUnderwater", "Skeleton Horses Underwater",
     "Skeleton horses keep their rider under water, move faster there than other horses, and make their own "
     "sounds under water."),
]
lines = assets.subtitle_lines(events)
for field, name, tooltip in config:
    lines.append("config.%s.mobs.%s=%s" % (M, field, name))
    lines.append("config.%s.mobs.%s.tooltip=%s" % (M, field, tooltip))
assets.merge_lang("mobs", lines)
print("mobs: %d sound events, %d lang lines" % (len(events), len(lines)))
