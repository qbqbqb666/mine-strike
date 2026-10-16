package dev.minestrike.skins;

/** Shared model dimensions; safe to load on a dedicated server. */
public final class PlayerMetrics {
    public static final float WIDTH = .75f, HEAD = .65f;
    public static final float LENGTH = (32 - 8 * HEAD) / 24;
    public static final double UNIT = .9375 / 16;
    private PlayerMetrics() {}
}
