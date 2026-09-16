package com.legendaryarmory.sets;

import net.minecraft.world.item.ArmorItem;

import java.util.List;
import java.util.Locale;

/**
 * Every legendary armour set in the mod.
 *
 * <p>A set is purely descriptive data: the registry classes turn each entry into an
 * {@link net.minecraft.world.item.ArmorMaterial}-backed set of four items, the ability
 * handlers read the set bonus from here, and the Cobblemon spawn influence reads the
 * {@link #boostedTypes()} / {@link #boostedLabels()} to decide which spawns to weight up.</p>
 */
public enum LegendarySet {

    /** Rayquaza - emerald, netherite, dragon fang/scale, a Master Ball and Dragon Gems. */
    RAYQUAZA(
            "rayquaza", "Sky Sovereign", "Delta Stream",
            List.of("dragon", "flying"), List.of(), 3.0F, 4.0F,
            new Stats(4, 9, 7, 4, 3.5F, 0.10F, 20, 45)),

    /** Groudon - Fire Stones, netherite, Ground Gems and the Red Orb. */
    GROUDON(
            "groudon", "Continent Crusher", "Drought",
            List.of("ground", "fire", "rock"), List.of(), 3.0F, 4.0F,
            new Stats(4, 9, 7, 4, 4.0F, 0.15F, 15, 50)),

    /** Kyogre - Water Stones, netherite, Water Gems and the Blue Orb. */
    KYOGRE(
            "kyogre", "Abyssal Tide", "Drizzle",
            List.of("water", "ice"), List.of(), 3.0F, 4.0F,
            new Stats(4, 9, 7, 4, 3.5F, 0.10F, 18, 48)),

    /** Dialga - the Adamant Orb, netherite, Steel Gems and Dragon Gems. */
    DIALGA(
            "dialga", "Temporal Aegis", "Roar of Time",
            List.of("steel", "dragon"), List.of(), 3.0F, 4.0F,
            new Stats(5, 9, 7, 4, 4.0F, 0.20F, 16, 52)),

    /** Palkia - the Lustrous Orb, netherite, Water Gems and Dragon Gems. */
    PALKIA(
            "palkia", "Spatial Rift", "Spacial Rend",
            List.of("water", "dragon"), List.of(), 3.0F, 4.0F,
            new Stats(4, 9, 7, 4, 3.5F, 0.10F, 22, 48)),

    /** Giratina - the Griseous Orb, netherite, Ghost Gems and Dragon Gems. */
    GIRATINA(
            "giratina", "Renegade Shroud", "Shadow Force",
            List.of("ghost", "dragon", "dark"), List.of(), 3.0F, 4.0F,
            new Stats(4, 9, 8, 4, 3.8F, 0.15F, 20, 50)),

    /** Necrozma - Ultranecrozium Z, netherite, Psychic Gems and a Nether Star. */
    NECROZMA(
            "necrozma", "Prismatic Dawn", "Prismatic Laser",
            List.of("psychic", "dragon"), List.of(), 3.0F, 4.0F,
            new Stats(5, 10, 8, 5, 4.0F, 0.15F, 25, 55)),

    /** Arceus - the capstone set. Boosts every legendary and mythical instead of a type. */
    ARCEUS(
            "arceus", "Judgment Plate", "Multitype",
            List.of(), List.of("legendary", "mythical", "ultra_beast"), 4.0F, 4.0F,
            new Stats(5, 10, 8, 5, 5.0F, 0.25F, 30, 66));

    /**
     * Defensive profile of a set.
     *
     * @param helmet          armour points on the helmet
     * @param chestplate      armour points on the chestplate
     * @param leggings        armour points on the leggings
     * @param boots           armour points on the boots
     * @param toughness       armour toughness (netherite is 3.0)
     * @param knockback       knockback resistance per piece (netherite is 0.1)
     * @param enchantability  enchantability (netherite is 15, gold is 25)
     * @param durabilityBase  multiplied by the vanilla per-slot constants for final durability
     */
    public record Stats(int helmet, int chestplate, int leggings, int boots,
                        float toughness, float knockback, int enchantability, int durabilityBase) {

        public int defenseFor(ArmorItem.Type type) {
            return switch (type) {
                case HELMET -> helmet;
                case CHESTPLATE, BODY -> chestplate;
                case LEGGINGS -> leggings;
                case BOOTS -> boots;
            };
        }
    }

    private final String id;
    private final String title;
    private final String bonusName;
    private final List<String> boostedTypes;
    private final List<String> boostedLabels;
    private final float spawnMultiplier;
    private final float signatureMultiplier;
    private final Stats stats;

    LegendarySet(String id, String title, String bonusName,
                 List<String> boostedTypes, List<String> boostedLabels,
                 float spawnMultiplier, float signatureMultiplier, Stats stats) {
        this.id = id;
        this.title = title;
        this.bonusName = bonusName;
        this.boostedTypes = boostedTypes;
        this.boostedLabels = boostedLabels;
        this.spawnMultiplier = spawnMultiplier;
        this.signatureMultiplier = signatureMultiplier;
        this.stats = stats;
    }

    /** Registry path prefix, e.g. {@code rayquaza} in {@code legendary_armory:rayquaza_helmet}. */
    public String id() {
        return id;
    }

    /** Human readable set name used in tooltips, e.g. "Sky Sovereign". */
    public String title() {
        return title;
    }

    /** Name of the set bonus, e.g. "Delta Stream". */
    public String bonusName() {
        return bonusName;
    }

    /** Cobblemon elemental type names (lower case) whose spawn weight this set multiplies. */
    public List<String> boostedTypes() {
        return boostedTypes;
    }

    /** Cobblemon species labels (e.g. {@code legendary}) whose spawn weight this set multiplies. */
    public List<String> boostedLabels() {
        return boostedLabels;
    }

    /** Weight multiplier applied to matching spawns. */
    public float spawnMultiplier() {
        return spawnMultiplier;
    }

    /** Weight multiplier applied to the legendary the set is named after. */
    public float signatureMultiplier() {
        return signatureMultiplier;
    }

    /** The Pokemon this set is named after, as a Cobblemon species name. */
    public String signatureSpecies() {
        return id;
    }

    public Stats stats() {
        return stats;
    }

    /** {@code legendary_armory:rayquaza_helmet} style registry path for a piece. */
    public String itemPath(ArmorItem.Type type) {
        return id + "_" + suffixOf(type);
    }

    /** Lower case slot suffix used in registry names and asset paths. */
    public static String suffixOf(ArmorItem.Type type) {
        return type.name().toLowerCase(Locale.ROOT);
    }

    /** Translation key root for the set, e.g. {@code legendary_armory.set.rayquaza}. */
    public String translationRoot() {
        return "legendary_armory.set." + id;
    }
}
