package dev.minestrike.skins;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AudioConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLED, HRTF, POSITION, SPATIALIZE, ATTENUATION, TAILS, DRY, DEBUG;
    public static final ForgeConfigSpec.DoubleValue STEP_RANGE, GUN_EXPONENT, STEP_VOLUME, GUN_VOLUME, VEHICLE_VOLUME;
    public static final ForgeConfigSpec.IntValue MAX_TAILS;
    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        ENABLED = b.comment("Master switch. Restart Minecraft after changing audio settings.").define("enabled", true);
        HRTF = b.comment("Request HRTF from Minecraft's OpenAL device without editing options.txt.").define("preferHrtf", true);
        POSITION = b.comment("Keep combat sounds at their actual world positions instead of reflected positions.").define("directPosition", true);
        SPATIALIZE = b.comment("Spatialize remote combat sources, including stereo buffers, using AL_SOFT_source_spatialize.").define("spatializeStereo", true);
        ATTENUATION = b.define("adjustAttenuation", true);
        STEP_RANGE = b.comment("Minimum client-side footstep attenuation range. Cannot recover packets not received.").defineInRange("footstepRange", 32.0, 16.0, 64.0);
        GUN_EXPONENT = b.comment("Remote TaCZ shot falloff exponent; original is 2. Does not extend the server's sound range.").defineInRange("taczFalloffExponent", 1.0, 0.5, 2.0);
        STEP_VOLUME = b.defineInRange("footstepVolume", 1.0, 0.0, 2.0);
        GUN_VOLUME = b.defineInRange("gunVolume", 1.0, 0.0, 2.0);
        VEHICLE_VOLUME = b.comment("Remote engine/track volume. Occupied local vehicle is excluded.").defineInRange("vehicleVolume", 0.7, 0.0, 2.0);
        TAILS = b.comment("Limit old shot tails per identified shooter and sound; never reject a new shot.").define("limitShotTails", true);
        DRY = b.comment("Remove auxiliary reverb sends from selected footsteps and remote shots, keeping direct occlusion filters.").define("dryCombatSounds", true);
        MAX_TAILS = b.defineInRange("maxShotTails", 3, 1, 16);
        DEBUG = b.comment("Log selected sound requests and source starts. Can be noisy; disabled by default.").define("diagnostics", false);
        SPEC = b.build();
    }
    private AudioConfig() {}
}
