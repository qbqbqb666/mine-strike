package dev.minestrike.skins;

public final class AimTiming {
    public static final float SECONDS = .25f;
    public static float opening(float original, boolean aiming) { return aiming ? SECONDS : original; }
    public static double advance(double current, double deltaTicks) {
        return Math.max(0, Math.min(1, current + Math.max(0, deltaTicks) / (20 * (double)SECONDS)));
    }
    private AimTiming() {}
}
