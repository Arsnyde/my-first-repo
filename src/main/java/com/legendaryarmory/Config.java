package com.legendaryarmory;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server-side configuration. Every value is read defensively so an unloaded config never crashes a tick. */
public final class Config {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue ENABLE_SET_BONUSES = BUILDER
            .comment("Master switch for the wearer abilities granted by a complete armour set.")
            .define("abilities.enableSetBonuses", true);

    public static final ModConfigSpec.BooleanValue ENABLE_WEATHER_CONTROL = BUILDER
            .comment("Allow the Groudon (Drought) and Kyogre (Drizzle) sets to change the weather.")
            .define("abilities.enableWeatherControl", true);

    public static final ModConfigSpec.BooleanValue ENABLE_PARTY_HEALING = BUILDER
            .comment("Allow the Arceus set to slowly restore the HP of the Pokemon in your party.")
            .define("abilities.enablePartyHealing", true);

    public static final ModConfigSpec.BooleanValue ENABLE_SPAWN_BOOSTS = BUILDER
            .comment("Master switch for the Cobblemon spawn weight multipliers.")
            .define("spawning.enableSpawnBoosts", true);

    public static final ModConfigSpec.DoubleValue SPAWN_BOOST_SCALE = BUILDER
            .comment("Scales every spawn multiplier. 1.0 uses the per-set values, 0.5 halves the bonus above 1x.")
            .defineInRange("spawning.spawnBoostScale", 1.0D, 0.0D, 10.0D);

    public static final ModConfigSpec.DoubleValue GLOBAL_LEGENDARY_BOOST = BUILDER
            .comment("Extra weight multiplier applied to legendary/mythical spawns while wearing any full set.")
            .defineInRange("spawning.globalLegendaryBoost", 1.25D, 1.0D, 10.0D);

    public static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }

    public static boolean flag(ModConfigSpec.BooleanValue value, boolean fallback) {
        try {
            return value.get();
        } catch (IllegalStateException | NullPointerException e) {
            return fallback;
        }
    }

    public static double number(ModConfigSpec.DoubleValue value, double fallback) {
        try {
            return value.get();
        } catch (IllegalStateException | NullPointerException e) {
            return fallback;
        }
    }
}
