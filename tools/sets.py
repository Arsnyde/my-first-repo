"""Single source of truth for the eight legendary sets.

Mirrors com.legendaryarmory.sets.LegendarySet on the Java side; used by generate_assets.py
to emit item models, textures, recipes, tags and the language file.
"""

MOD_ID = "legendary_armory"
MS = "mega_showdown"

# Every Cobblemon type gem, used for the legendary_armory:type_gems tag.
TYPE_GEMS = [
    "bug", "dark", "dragon", "electric", "fairy", "fighting", "fire", "flying", "ghost",
    "grass", "ground", "ice", "normal", "poison", "psychic", "rock", "steel", "water",
]

# palette: (base, light, dark, accent) as #RRGGBB
SETS = {
    "rayquaza": {
        "title": "Sky Sovereign",
        "bonus": "Delta Stream",
        "pokemon": "Rayquaza",
        "palette": ("#1E7A3C", "#43C36B", "#0C4520", "#FFD34D"),
        "description": "Slow falling, sprint gliding, jump boost, and immunity to falls, lightning and freezing.",
        "spawns": "Dragon and Flying Pokemon spawn 3x more often. Rayquaza 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:dragon_gem"},
            "H": {"item": "cobblemon:dragon_fang"},
            "D": {"item": "cobblemon:dragon_scale"},
            "E": {"item": "minecraft:emerald_block"},
            "S": {"item": "cobblemon:master_ball"},
        },
        "signature_mod": None,
    },
    "groudon": {
        "title": "Continent Crusher",
        "bonus": "Drought",
        "pokemon": "Groudon",
        "palette": ("#B03024", "#E2593F", "#5A1210", "#FF9A2E"),
        "description": "Fire resistance, lava swimming, Haste II, Strength, halved explosion damage, and clear skies.",
        "spawns": "Ground, Fire and Rock Pokemon spawn 3x more often. Groudon 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:ground_gem"},
            "H": {"item": "cobblemon:fire_stone"},
            "D": {"item": "cobblemon:hard_stone"},
            "E": {"item": "minecraft:magma_block"},
            "S": {"item": f"{MS}:red_orb"},
        },
        "signature_mod": MS,
    },
    "kyogre": {
        "title": "Abyssal Tide",
        "bonus": "Drizzle",
        "pokemon": "Kyogre",
        "palette": ("#14417F", "#2D77C9", "#081F3C", "#E0453B"),
        "description": "Water breathing, Conduit Power, Dolphin's Grace, no drowning or freezing, and endless rain.",
        "spawns": "Water and Ice Pokemon spawn 3x more often. Kyogre 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:water_gem"},
            "H": {"item": "cobblemon:water_stone"},
            "D": {"item": "cobblemon:deep_sea_scale"},
            "E": {"item": "minecraft:prismarine_crystals"},
            "S": {"item": f"{MS}:blue_orb"},
        },
        "signature_mod": MS,
    },
    "dialga": {
        "title": "Temporal Aegis",
        "bonus": "Roar of Time",
        "pokemon": "Dialga",
        "palette": ("#3E5C86", "#7A9CC6", "#1C2B41", "#9BE3FF"),
        "description": "Haste III, Resistance, Speed, halved projectile damage, and attackers are slowed.",
        "spawns": "Steel and Dragon Pokemon spawn 3x more often. Dialga 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:steel_gem"},
            "H": {"item": "cobblemon:dragon_gem"},
            "D": {"item": "minecraft:netherite_scrap"},
            "E": {"item": "minecraft:diamond_block"},
            "S": {"item": f"{MS}:adamant_orb"},
        },
        "signature_mod": MS,
    },
    "palkia": {
        "title": "Spatial Rift",
        "bonus": "Spacial Rend",
        "pokemon": "Palkia",
        "palette": ("#C9C3E8", "#F2EEFF", "#6B6494", "#E0559B"),
        "description": "Speed II, Jump Boost, +3 block and +2 entity reach, and no fall or ender pearl damage.",
        "spawns": "Water and Dragon Pokemon spawn 3x more often. Palkia 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:water_gem"},
            "H": {"item": "cobblemon:dragon_gem"},
            "D": {"item": "minecraft:ender_pearl"},
            "E": {"item": "minecraft:amethyst_block"},
            "S": {"item": f"{MS}:lustrous_orb"},
        },
        "signature_mod": MS,
    },
    "giratina": {
        "title": "Renegade Shroud",
        "bonus": "Shadow Force",
        "pokemon": "Giratina",
        "palette": ("#2B2F3A", "#4D5566", "#121319", "#C9482E"),
        "description": "Night vision, wither and magic immunity, invisibility while sneaking, and attackers are blinded and burned.",
        "spawns": "Ghost, Dragon and Dark Pokemon spawn 3x more often. Giratina 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:ghost_gem"},
            "H": {"item": "cobblemon:dragon_gem"},
            "D": {"item": "cobblemon:dusk_stone"},
            "E": {"item": "minecraft:crying_obsidian"},
            "S": {"item": f"{MS}:griseous_orb"},
        },
        "signature_mod": MS,
    },
    "necrozma": {
        "title": "Prismatic Dawn",
        "bonus": "Prismatic Laser",
        "pokemon": "Necrozma",
        "palette": ("#1B1330", "#3E2E6B", "#0A0716", "#56E0E0"),
        "description": "Night vision, fire and magic immunity, Strength and Regeneration in sunlight, and attackers are weakened.",
        "spawns": "Psychic and Dragon Pokemon spawn 3x more often. Necrozma 4x.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"item": "cobblemon:psychic_gem"},
            "H": {"item": "cobblemon:dragon_gem"},
            "D": {"item": "cobblemon:shiny_stone"},
            "E": {"item": "minecraft:glowstone"},
            "S": {"item": f"{MS}:ultranecrozium_z"},
        },
        "signature_mod": MS,
    },
    "arceus": {
        "title": "Judgment Plate",
        "bonus": "Multitype",
        "pokemon": "Arceus",
        "palette": ("#EDE6D2", "#FFFDF2", "#A2946F", "#E3C04A"),
        "description": "Regeneration, Resistance, Saturation, +4 hearts, wither/magic/fire/starvation immunity, and your party slowly heals.",
        "spawns": "Every legendary, mythical and Ultra Beast spawns 4x more often.",
        "ingredients": {
            "N": {"item": "minecraft:netherite_ingot"},
            "G": {"tag": f"{MOD_ID}:type_gems"},
            "H": {"item": "cobblemon:ability_patch"},
            "D": {"item": "minecraft:netherite_block"},
            "E": {"item": "minecraft:gold_block"},
            "S": {"item": f"{MS}:legend_plate"},
        },
        "signature_mod": MS,
    },
}

# The signature slot falls back to the mod's own crafting core when Mega Showdown is absent.
FALLBACK_SIGNATURE = {"item": f"{MOD_ID}:dominion_core"}

PIECES = ["helmet", "chestplate", "leggings", "boots"]

PATTERNS = {
    "helmet": ["GEG", "HNH"],
    "chestplate": ["HDH", "GSG", "NEN"],
    "leggings": ["NSN", "GEG", "HDH"],
    "boots": ["GDG", "HNH"],
}
