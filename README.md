# Cobblemon: Legendary Armory

Eight full legendary armour sets for **Cobblemon 1.8.1** on **NeoForge / Minecraft 1.21.1**.

Each set is forged from a mix of vanilla Minecraft materials, Cobblemon items (evolution stones,
type gems, held items, Poke Balls) and - when the mod is installed - **Cobblemon: Mega Showdown**
key items such as the Red Orb, Blue Orb, Adamant Orb and Legend Plate.

Wearing all four pieces of a set grants a themed set bonus **and** multiplies the Cobblemon spawn
weight of the types that legendary presides over.

## Requirements

| | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.214 or newer |
| Cobblemon | 1.8.0 or newer (built against `1.8.1+1.21.1`) |
| Cobblemon: Mega Showdown | optional |

Mega Showdown is a soft dependency. Every recipe that uses one of its key items ships a paired
fallback recipe, gated on `neoforge:mod_loaded`, that swaps the key item for this mod's own
**Dominion Core**. Install Mega Showdown and the canonical recipes take over automatically.

## Building

```bash
./gradlew build          # jar lands in build/libs/
./gradlew runClient      # dev client with Cobblemon on the classpath
```

If the wrapper is not present yet, run `gradle wrapper` once first.

The build pulls Cobblemon from the ImpactDev maven and Kotlin for Forge from its GitHub pages
maven; both are declared in `build.gradle`.

## The sets

### Rayquaza - Sky Sovereign

**Set bonus - Delta Stream:** Slow falling, sprint gliding, jump boost, and immunity to falls, lightning and freezing.

**Spawning:** Dragon and Flying Pokemon spawn 3x more often. Rayquaza 4x.

**Materials:** Netherite Ingot, Dragon Gem, Dragon Fang, Dragon Scale, Emerald Block, Master Ball

### Groudon - Continent Crusher

**Set bonus - Drought:** Fire resistance, lava swimming, Haste II, Strength, halved explosion damage, and clear skies.

**Spawning:** Ground, Fire and Rock Pokemon spawn 3x more often. Groudon 4x.

**Materials:** Netherite Ingot, Ground Gem, Fire Stone, Hard Stone, Magma Block, Red Orb *(or a Dominion Core without Mega Showdown)*

### Kyogre - Abyssal Tide

**Set bonus - Drizzle:** Water breathing, Conduit Power, Dolphin's Grace, no drowning or freezing, and endless rain.

**Spawning:** Water and Ice Pokemon spawn 3x more often. Kyogre 4x.

**Materials:** Netherite Ingot, Water Gem, Water Stone, Deep Sea Scale, Prismarine Crystals, Blue Orb *(or a Dominion Core without Mega Showdown)*

### Dialga - Temporal Aegis

**Set bonus - Roar of Time:** Haste III, Resistance, Speed, halved projectile damage, and attackers are slowed.

**Spawning:** Steel and Dragon Pokemon spawn 3x more often. Dialga 4x.

**Materials:** Netherite Ingot, Steel Gem, Dragon Gem, Netherite Scrap, Diamond Block, Adamant Orb *(or a Dominion Core without Mega Showdown)*

### Palkia - Spatial Rift

**Set bonus - Spacial Rend:** Speed II, Jump Boost, +3 block and +2 entity reach, and no fall or ender pearl damage.

**Spawning:** Water and Dragon Pokemon spawn 3x more often. Palkia 4x.

**Materials:** Netherite Ingot, Water Gem, Dragon Gem, Ender Pearl, Amethyst Block, Lustrous Orb *(or a Dominion Core without Mega Showdown)*

### Giratina - Renegade Shroud

**Set bonus - Shadow Force:** Night vision, wither and magic immunity, invisibility while sneaking, and attackers are blinded and burned.

**Spawning:** Ghost, Dragon and Dark Pokemon spawn 3x more often. Giratina 4x.

**Materials:** Netherite Ingot, Ghost Gem, Dragon Gem, Dusk Stone, Crying Obsidian, Griseous Orb *(or a Dominion Core without Mega Showdown)*

### Necrozma - Prismatic Dawn

**Set bonus - Prismatic Laser:** Night vision, fire and magic immunity, Strength and Regeneration in sunlight, and attackers are weakened.

**Spawning:** Psychic and Dragon Pokemon spawn 3x more often. Necrozma 4x.

**Materials:** Netherite Ingot, Psychic Gem, Dragon Gem, Shiny Stone, Glowstone, Ultranecrozium Z *(or a Dominion Core without Mega Showdown)*

### Arceus - Judgment Plate

**Set bonus - Multitype:** Regeneration, Resistance, Saturation, +4 hearts, wither/magic/fire/starvation immunity, and your party slowly heals.

**Spawning:** Every legendary, mythical and Ultra Beast spawns 4x more often.

**Materials:** Netherite Ingot, any Type Gem, Ability Patch, Netherite Block, Gold Block, Legend Plate *(or a Dominion Core without Mega Showdown)*

### Dominion Core

A crafting core made from a Master Ball, a Nether Star, a Netherite Ingot and two Type Gems. It
stands in for a Mega Showdown key item in any fallback recipe, and it is the signature ingredient
of the Arceus set when Mega Showdown is absent.

## Crafting patterns

Every set uses the same six-ingredient layout, where `N` netherite, `G` primary type gem,
`H` secondary Cobblemon item, `D` tertiary Cobblemon item, `E` vanilla flavour block and
`S` the signature key item:

```
helmet        chestplate     leggings       boots
G E G         H D H          N S N          G D G
H N H         G S G          G E G          H N H
              N E N          H D H
```

## Configuration

`config/legendary_armory-common.toml`:

| Option | Default | Effect |
|---|---|---|
| `abilities.enableSetBonuses` | `true` | Master switch for every wearer ability |
| `abilities.enableWeatherControl` | `true` | Lets Groudon clear the sky and Kyogre summon rain |
| `abilities.enablePartyHealing` | `true` | Lets the Arceus set top up your party's HP |
| `spawning.enableSpawnBoosts` | `true` | Master switch for the spawn weight multipliers |
| `spawning.spawnBoostScale` | `1.0` | Scales every bonus; `0.5` halves the bonus above 1x |
| `spawning.globalLegendaryBoost` | `1.25` | Extra multiplier on legendaries while wearing any full set |

## How the spawn boost works

Cobblemon builds one `PlayerSpawner` per player and runs every builder registered on
`PlayerSpawnerFactory.influenceBuilders` against it. On common setup the mod adds a builder that
attaches a `LegendaryArmorSpawnInfluence` to each player's spawner
(`src/main/java/com/legendaryarmory/compat/`).

That influence overrides `affectWeight`: for each candidate `PokemonSpawnDetail` it resolves the
species and form, then multiplies the weight if the form's elemental types or labels match the set
the player is currently wearing. Because it reads live equipment, swapping armour takes effect on
the very next spawn attempt - no relog needed.

## Project layout

```
src/main/java/com/legendaryarmory/
  LegendaryArmory.java          mod entrypoint, bus + config registration
  Config.java                   NeoForge config spec
  sets/LegendarySet.java        the eight sets as data (stats, palettes, boosted types)
  registry/                     armour materials, items, creative tab
  item/LegendaryArmorItem.java  ArmorItem subclass carrying its set + tooltips
  ability/SetBonusHandler.java  per-tick set bonuses (effects, attributes, weather, party heal)
  ability/CombatHandler.java    damage immunities, mitigation and retaliation
  compat/                       all Cobblemon-facing code, kept in one place
  util/ArmorSets.java           "what set is this entity wearing?"

tools/
  sets.py                       single source of truth for set data
  generate_assets.py            emits models, textures, recipes, tags and en_us.json
  png.py                        dependency-free PNG writer
```

## Regenerating assets

Item models, item icons, armour layer textures, recipes, the type gem tag and the language file
are all generated:

```bash
python3 tools/generate_assets.py
```

Edit `tools/sets.py` (and the matching entry in `LegendarySet.java`) to change a set's palette,
materials or description, then re-run the script.

## A note on the textures

The 16x16 item icons and the 64x32 armour layers are **procedurally generated placeholders**.
They are palette-correct, readable and consistent across the eight sets, but they are drawn by a
script, not by an artist. Replace the PNGs under
`src/main/resources/assets/legendary_armory/textures/` with hand-drawn art when you have it - the
file names and model JSON will not need to change.
