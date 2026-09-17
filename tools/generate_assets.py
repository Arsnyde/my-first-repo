#!/usr/bin/env python3
"""Generates every asset and data file that is pure boilerplate.

Run from the repository root:

    python3 tools/generate_assets.py

Textures are procedurally drawn placeholders: readable, palette-correct and consistent, but
they are meant to be replaced by hand-drawn art. Everything else (models, recipes, tags, lang)
is the real deal.
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from png import write_png
from sets import (FALLBACK_SIGNATURE, MOD_ID, MS, PATTERNS, PIECES, SETS, TYPE_GEMS)

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", MOD_ID)
DATA = os.path.join(ROOT, "src", "main", "resources", "data", MOD_ID)

TRANSPARENT = (0, 0, 0, 0)


# --------------------------------------------------------------------------- colour helpers

def hex_to_rgb(value):
    value = value.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def shade(rgb, factor):
    return tuple(max(0, min(255, int(c * factor))) for c in rgb)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def noise(x, y, seed):
    h = (x * 73856093) ^ (y * 19349663) ^ (seed * 83492791)
    h &= 0xFFFFFFFF
    h = (h ^ (h >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((h >> 16) & 0xFF) / 255.0


# --------------------------------------------------------------------------- item icons

# Per-set silhouette additions: horns, crests and fins that make each helmet read as its Pokemon.
HELMET_EXTRAS = {
    "rings":   [(2, 3), (1, 4), (13, 3), (14, 4)],                  # Rayquaza head fins
    "plates":  [(2, 2), (2, 3), (13, 2), (13, 3)],                  # Groudon shoulder spikes
    "slashes": [(1, 5), (2, 4), (14, 5), (13, 4)],                  # Kyogre head fins
    "crest":   [(7, 0), (8, 0), (7, 1), (8, 1)],                    # Dialga crown fin
    "pearls":  [(2, 5), (13, 5), (2, 6), (13, 6)],                  # Palkia neck pearls
    "ribs":    [(4, 1), (6, 0), (9, 0), (11, 1)],                   # Giratina six point crown
    "prism":   [(5, 0), (7, 1), (8, 1), (10, 0)],                   # Necrozma light spikes
    "wheel":   [(1, 6), (14, 6), (1, 7), (14, 7)],                  # Arceus wheel nubs
}


def motif_pixels(motif, cells, piece):
    """Which pixels of a silhouette get painted in the set's secondary accent."""
    if not cells:
        return set()
    xs = [x for x, _ in cells]
    ys = [y for _, y in cells]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    marks = set()

    if motif == "rings":
        # Rayquaza: yellow ring segments down the length of the body.
        for y in range(y0 + 2, y1, 3):
            row = sorted(x for x, yy in cells if yy == y)
            if len(row) >= 2:
                marks.update({(row[0], y), (row[1], y), (row[-2], y), (row[-1], y)})
    elif motif == "plates":
        # Groudon: pale spikes along every upward facing edge.
        for x, y in cells:
            if (x, y - 1) not in cells and y > y0:
                marks.add((x, y))
    elif motif == "slashes":
        # Kyogre: red slashes cutting across the body.
        marks.update((x, y) for x, y in cells if (x + y) % 5 == 0)
    elif motif == "crest":
        # Dialga: a diamond core in the middle of the piece.
        cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
        marks.update((x, y) for x, y in cells if abs(x - cx) + abs(y - cy) in (1, 2))
    elif motif == "pearls":
        # Palkia: pink pearl clusters on the outer edges.
        for y in range(y0 + 1, y1, 4):
            row = sorted(x for x, yy in cells if yy == y)
            if row:
                marks.update({(row[0], y), (row[-1], y), (row[0], y + 1), (row[-1], y + 1)})
    elif motif == "ribs":
        # Giratina: golden ribs running vertically.
        marks.update((x, y) for x, y in cells if x % 3 == x0 % 3)
    elif motif == "prism":
        # Necrozma: cyan light shards on the diagonal.
        marks.update((x, y) for x, y in cells if (x - y) % 4 == 0)
    elif motif == "wheel":
        # Arceus: the ring, plus the cross of its plates.
        cx, cy = (x0 + x1) // 2, (y0 + y1) // 2
        marks.update((x, y) for x, y in cells
                     if abs(x - cx) + abs(y - cy) == 3 or x == cx or y == cy)

    return {p for p in marks if p in cells}


def piece_mask(piece):
    """Returns a set of (x, y) pixels covered by the silhouette, plus the accent rows."""
    cells = set()
    accent_rows = set()

    def fill(y, x0, x1):
        for x in range(x0, x1 + 1):
            cells.add((x, y))

    if piece == "helmet":
        fill(2, 5, 10)
        fill(3, 4, 11)
        for y in range(4, 8):
            fill(y, 3, 12)
        for y in range(8, 11):
            fill(y, 3, 5)
            fill(y, 10, 12)
        fill(11, 4, 5)
        fill(11, 10, 11)
        accent_rows.add(7)
    elif piece == "chestplate":
        fill(3, 5, 6)
        fill(3, 9, 10)
        for y in range(4, 13):
            fill(y, 5, 10)
        # A one pixel gap keeps the pauldrons readable against the torso.
        for y in range(4, 10):
            fill(y, 1, 3)
            fill(y, 12, 14)
        accent_rows.add(6)
    elif piece == "leggings":
        for y in range(3, 6):
            fill(y, 3, 12)
        for y in range(6, 13):
            fill(y, 3, 6)
            fill(y, 9, 12)
        accent_rows.add(4)
    elif piece == "boots":
        for y in range(6, 10):
            fill(y, 3, 6)
            fill(y, 9, 12)
        for y in range(10, 13):
            fill(y, 2, 6)
            fill(y, 9, 13)
        accent_rows.add(10)
    return cells, accent_rows


def draw_icon(path, palette, accent2, motif, piece):
    base, light, dark, accent = (hex_to_rgb(c) for c in palette)
    accent2 = hex_to_rgb(accent2)
    outline = shade(dark, 0.55)
    cells, accent_rows = piece_mask(piece)
    if piece == "helmet":
        cells.update(HELMET_EXTRAS.get(motif, []))
    marks = motif_pixels(motif, cells, piece)

    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            if (x, y) not in cells:
                row.append(TRANSPARENT)
                continue
            edge = any((x + dx, y + dy) not in cells
                       for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            if edge:
                row.append(outline + (255,))
            elif (x, y) in marks:
                row.append(mix(accent2, light, 0.15 * noise(x, y, 5)) + (255,))
            elif y in accent_rows:
                row.append(accent + (255,))
            else:
                # Soft top-lit gradient with a little grain so large flats do not look dead.
                t = 1.0 - (y / 15.0)
                colour = mix(base, light, t * 0.7)
                colour = mix(colour, dark, 0.18 * noise(x, y, 7))
                row.append(colour + (255,))
        pixels.append(row)
    write_png(path, 16, 16, pixels)


def draw_core_icon(path):
    """The Dominion Core: a prismatic orb, deliberately unlike any single set."""
    base, light, dark, accent = (hex_to_rgb(c) for c in ("#2E2350", "#9C7BE8", "#120C24", "#F5D76E"))
    pixels = []
    for y in range(16):
        row = []
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            dist = (dx * dx + dy * dy) ** 0.5
            if dist > 6.4:
                row.append(TRANSPARENT)
            elif dist > 5.4:
                row.append(shade(dark, 0.7) + (255,))
            elif dist > 4.2:
                row.append(accent + (255,))
            else:
                glow = max(0.0, 1.0 - dist / 4.6)
                colour = mix(base, light, glow)
                colour = mix(colour, accent, 0.25 * noise(x, y, 3))
                row.append(colour + (255,))
        pixels.append(row)
    write_png(path, 16, 16, pixels)


# --------------------------------------------------------------------------- armour layers

def layer_mark(motif, x, y, layer):
    """The same per-set motif, tiled across an armour layer so the worn set matches the icons."""
    if motif == "rings":
        return y % 8 == 3 and x % 6 < 3
    if motif == "plates":
        return y % 16 in (2, 3) and x % 4 < 2
    if motif == "slashes":
        return (x + y) % 9 in (0, 1)
    if motif == "crest":
        return (abs((x % 12) - 6) + abs((y % 12) - 6)) == 3
    if motif == "pearls":
        return (x % 9 in (3, 4)) and (y % 9 in (3, 4))
    if motif == "ribs":
        return x % 5 == 2
    if motif == "prism":
        return (x - y) % 7 in (0, 1)
    if motif == "wheel":
        return (abs((x % 14) - 7) + abs((y % 14) - 7)) in (4, 5)
    return False


def draw_armor_layer(path, palette, accent2, motif, layer):
    """A 64x32 armour layer. Every pixel is opaque, so all model faces get covered."""
    base, light, dark, accent = (hex_to_rgb(c) for c in palette)
    accent2 = hex_to_rgb(accent2)
    trim_rows = {5, 6} if layer == 1 else {20, 21}
    pixels = []
    for y in range(32):
        row = []
        for x in range(64):
            if y in trim_rows:
                colour = mix(accent, light, 0.25 * noise(x, y, 11))
            elif layer_mark(motif, x, y, layer):
                colour = mix(accent2, dark, 0.22 * noise(x, y, 13))
            else:
                # Vertical shading inside each 16 pixel model band keeps plates looking plated.
                band = (y % 16) / 15.0
                colour = mix(light, base, band)
                colour = mix(colour, dark, 0.30 * (y / 31.0))
                colour = mix(colour, dark, 0.14 * noise(x, y, layer))
                if x % 16 in (0, 15):
                    colour = mix(colour, dark, 0.45)
            row.append(colour + (255,))
        pixels.append(row)
    write_png(path, 64, 32, pixels)


# --------------------------------------------------------------------------- json output

def write_json(path, payload):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as fh:
        json.dump(payload, fh, indent=2)
        fh.write("\n")


def item_model(item_id):
    return {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD_ID}:item/{item_id}"}}


def recipe(set_id, piece, signature, condition=None):
    data = SETS[set_id]
    keys = dict(data["ingredients"])
    keys["S"] = signature
    pattern = PATTERNS[piece]
    used = {ch for line in pattern for ch in line if ch != " "}

    out = {}
    if condition is not None:
        out["neoforge:conditions"] = [condition]
    out["type"] = "minecraft:crafting_shaped"
    out["category"] = "equipment"
    out["pattern"] = pattern
    out["key"] = {ch: keys[ch] for ch in sorted(used)}
    out["result"] = {"count": 1, "id": f"{MOD_ID}:{set_id}_{piece}"}
    return out


def title_case(value):
    return " ".join(part.capitalize() for part in value.split("_"))


def main():
    lang = {
        "itemGroup.legendary_armory": "Legendary Armory",
        "item.legendary_armory.dominion_core": "Dominion Core",
        "legendary_armory.tooltip.full_set": "Full set: %s",
        "legendary_armory.tooltip.core": "A seed of legendary power. Used to forge armour when Mega Showdown is absent.",
    }

    # Dominion Core
    os.makedirs(os.path.join(ASSETS, "textures", "item"), exist_ok=True)
    draw_core_icon(os.path.join(ASSETS, "textures", "item", "dominion_core.png"))
    write_json(os.path.join(ASSETS, "models", "item", "dominion_core.json"), item_model("dominion_core"))
    write_json(os.path.join(DATA, "recipe", "dominion_core.json"), {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": [" M ", "GNG", " I "],
        "key": {
            "M": {"item": "cobblemon:master_ball"},
            "N": {"item": "minecraft:nether_star"},
            "G": {"tag": f"{MOD_ID}:type_gems"},
            "I": {"item": "minecraft:netherite_ingot"},
        },
        "result": {"count": 1, "id": f"{MOD_ID}:dominion_core"},
    })

    # Type gem tag
    write_json(os.path.join(DATA, "tags", "item", "type_gems.json"),
               {"values": [f"cobblemon:{gem}_gem" for gem in TYPE_GEMS]})

    for set_id, data in SETS.items():
        palette = data["palette"]

        accent2, motif = data["accent2"], data["motif"]
        draw_armor_layer(os.path.join(ASSETS, "textures", "models", "armor", f"{set_id}_layer_1.png"),
                         palette, accent2, motif, 1)
        draw_armor_layer(os.path.join(ASSETS, "textures", "models", "armor", f"{set_id}_layer_2.png"),
                         palette, accent2, motif, 2)

        for piece in PIECES:
            item_id = f"{set_id}_{piece}"
            draw_icon(os.path.join(ASSETS, "textures", "item", f"{item_id}.png"), palette, accent2, motif, piece)
            write_json(os.path.join(ASSETS, "models", "item", f"{item_id}.json"), item_model(item_id))

            if data["signature_mod"] is None:
                write_json(os.path.join(DATA, "recipe", f"{item_id}.json"),
                           recipe(set_id, piece, data["ingredients"]["S"]))
            else:
                loaded = {"type": "neoforge:mod_loaded", "modid": data["signature_mod"]}
                write_json(os.path.join(DATA, "recipe", f"{item_id}.json"),
                           recipe(set_id, piece, data["ingredients"]["S"], loaded))
                write_json(os.path.join(DATA, "recipe", f"{item_id}_fallback.json"),
                           recipe(set_id, piece, FALLBACK_SIGNATURE,
                                  {"type": "neoforge:not", "value": loaded}))

            lang[f"item.{MOD_ID}.{item_id}"] = f"{data['pokemon']} {title_case(piece)}"

        root = f"{MOD_ID}.set.{set_id}"
        lang[f"{root}.title"] = f"{data['title']} Set"
        lang[f"{root}.bonus"] = data["bonus"]
        lang[f"{root}.description"] = data["description"]
        lang[f"{root}.spawns"] = data["spawns"]

    write_json(os.path.join(ASSETS, "lang", "en_us.json"), dict(sorted(lang.items())))

    print(f"Generated assets for {len(SETS)} sets ({len(SETS) * len(PIECES)} armour pieces).")


if __name__ == "__main__":
    main()
