package dev.minestrike.skins;

public final class BombDamage {
    public static final double RANGE = 56;

    public static float amount(double distance, float maxHealth) {
        if (!Double.isFinite(distance) || distance < 0 || distance >= RANGE
                || !Float.isFinite(maxHealth) || maxHealth <= 0) return 0;
        double falloff = 1 - distance / RANGE;
        return (float) (maxHealth * 10 * falloff * falloff);
    }

    private BombDamage() {}
}
