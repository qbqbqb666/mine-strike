package dev.minestrike.skins;

import java.util.Objects;

public final class ReloadViewState {
    private Object context;
    private long last;
    private float value;
    public float update(Object key, boolean reloading, float aim, long now) {
        if (key == null) { context = null; last = 0; value = 0; return 0; }
        if (!Objects.equals(key, context)) { context = key; last = now; value = 0; }
        double elapsed = Math.max(0, (now - last) / 1_000_000_000.0);
        last = now;
        float step = (float)(elapsed / .12);
        value = reloading ? Math.min(1, value + step) : Math.max(0, value - step);
        return value * Math.max(0, Math.min(1, aim));
    }
}
