package dev.minestrike.skins;

import net.minecraftforge.common.ForgeConfigSpec;

public final class TracerConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, TACZ, SUPERB, OWN;
    public static final ForgeConfigSpec.DoubleValue LENGTH, LIFETIME, WIDTH, NEAR_RADIUS, NEAR_BOOST, DISTANCE;
    public static final ForgeConfigSpec.IntValue BUDGET;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.define("enabled", true);
        TACZ = b.define("tacz", true);
        SUPERB = b.define("superbWarfare", true);
        OWN = b.comment("Also enhance the local player's bullets. Off avoids duplicating native first-person tracers.").define("showOwnShots", false);
        LENGTH = b.comment("Maximum visible streak length in blocks.").defineInRange("length", 6.0, 0.5, 12.0);
        LIFETIME = b.comment("Fade duration in game seconds; 0.15 is three ticks.").defineInRange("lifetime", 0.15, 0.05, 0.4);
        WIDTH = b.comment("Core streak half-width in world units.").defineInRange("width", 0.025, 0.005, 0.08);
        NEAR_RADIUS = b.comment("Passing trajectories within this distance of the camera receive a short local emphasis.").defineInRange("nearRadius", 3.0, 0.5, 6.0);
        NEAR_BOOST = b.defineInRange("nearBoost", 1.8, 1.0, 3.0);
        DISTANCE = b.defineInRange("maxDistance", 128.0, 16.0, 256.0);
        BUDGET = b.comment("Maximum rendered streaks per frame; closest trajectories have priority.").defineInRange("maxStreaks", 256, 16, 1024);
        SPEC = b.build();
    }
    private TracerConfig() {}
}
