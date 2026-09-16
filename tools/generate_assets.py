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


def draw_icon(path, palette, piece):
    base, light, dark, accent = (hex_to_rgb(c) for c in palette)
    outline = shade(dark, 0.55)
    cells, accent_rows = piece_mask(piece)

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

def draw_armor_layer(path, palette, layer):
    """A 64x32 armour layer. Every pixel is opaque, so all model faces get covered."""
    base, light, dark, accent = (hex_to_rgb(c) for c in palette)
    trim_rows = {5, 6} if layer == 1 else {20, 21}
    pixels = []
    for y in range(32):
        row = []
        for x in range(64):
            if y in trim_rows:
                colour = mix(accent, light, 0.25 * noise(x, y, 11))
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

        draw_armor_layer(os.path.join(ASSETS, "textures", "models", "armor", f"{set_id}_layer_1.png"), palette, 1)
        draw_armor_layer(os.path.join(ASSETS, "textures", "models", "armor", f"{set_id}_layer_2.png"), palette, 2)

        for piece in PIECES:
            item_id = f"{set_id}_{piece}"
            draw_icon(os.path.join(ASSETS, "textures", "item", f"{item_id}.png"), palette, piece)
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
