package com.arsnyde.battleclarity;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-side config. Everything here is cosmetic and local to the player. */
public final class ClarityConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue MODEL_PADDING;
    public static final ModConfigSpec.DoubleValue MODEL_PADDING_SCALE;
    public static final ModConfigSpec.BooleanValue HIDE_NON_SOLID;
    public static final ModConfigSpec.DoubleValue SIGHT_MARGIN;
    public static final ModConfigSpec.IntValue SAMPLE_DENSITY;
    public static final ModConfigSpec.IntValue DILATION;
    public static final ModConfigSpec.DoubleValue MAX_DISTANCE;
    public static final ModConfigSpec.IntValue STICKY_TICKS;
    public static final ModConfigSpec.IntValue RECOMPUTE_INTERVAL;
    public static final ModConfigSpec.IntValue MAX_HIDDEN_BLOCKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Cobblemon Battle Clarity - hides only the blocks that stand between your camera and the",
                  "Pokemon taking part in a battle. Everything returns to normal when the battle ends.");
        b.push("general");

        ENABLED = b
                .comment("Master switch. Turn off to leave the world rendering completely untouched.")
                .define("enabled", true);

        HIDE_NON_SOLID = b
                .comment("Hide non-solid view-obstructing blocks too, such as leaves, glass and doors.",
                         "When false only fully solid blocks are hidden.")
                .define("hideNonSolidBlocks", true);

        b.pop();
        b.push("model");

        MODEL_PADDING = b
                .comment("Rendered Pokemon models are usually bigger than their hitbox (wings, tails, horns).",
                         "This many blocks are added around every battler's hitbox to approximate the model,",
                         "and any block touching that box is always hidden.")
                .defineInRange("modelPadding", 0.25D, 0.0D, 3.0D);

        MODEL_PADDING_SCALE = b
                .comment("Extra padding proportional to the Pokemon's size, so large Pokemon get a larger",
                         "allowance. Added as: modelPaddingScale * max(width, height).")
                .defineInRange("modelPaddingScale", 0.15D, 0.0D, 1.0D);

        b.pop();
        b.push("shape");

        SIGHT_MARGIN = b
                .comment("How far (in blocks) to inflate each Pokemon's hitbox before working out what blocks",
                         "cover it. Bigger values carve a more generous window around the Pokemon.")
                .defineInRange("sightMargin", 0.6D, 0.0D, 4.0D);

        SAMPLE_DENSITY = b
                .comment("How many sample points per axis are traced across each Pokemon's hitbox.",
                         "Higher is more accurate but costs more CPU. 5 means a 5x5x5 lattice (surface only).")
                .defineInRange("sampleDensity", 5, 2, 12);

        DILATION = b
                .comment("Extra blocks of feathering around each hole, so the opening looks like a window",
                         "instead of a pinhole. 0 hides the strictly-necessary blocks only.")
                .defineInRange("dilation", 1, 0, 3);

        MAX_DISTANCE = b
                .comment("Never hide a block further than this many blocks from the camera.")
                .defineInRange("maxDistance", 24.0D, 4.0D, 64.0D);

        b.pop();
        b.push("performance");

        STICKY_TICKS = b
                .comment("Once hidden, a block stays hidden for this many ticks after it stops blocking your",
                         "view. Stops the hole flickering as you and the Pokemon move around. 20 ticks = 1s.")
                .defineInRange("stickyTicks", 30, 0, 200);

        RECOMPUTE_INTERVAL = b
                .comment("Minimum number of client ticks between two recalculations.")
                .defineInRange("recomputeIntervalTicks", 4, 1, 40);

        MAX_HIDDEN_BLOCKS = b
                .comment("Hard ceiling on how many blocks may be hidden at once, to bound chunk rebuild cost.")
                .defineInRange("maxHiddenBlocks", 4000, 100, 20000);

        b.pop();

        SPEC = b.build();
    }

    private ClarityConfig() {
    }
}
