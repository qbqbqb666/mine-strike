package dev.minestrike.skins;

import java.util.Objects;

public final class AimReturnState {
    private Object context;
    private boolean cycling, resume, reload, latch;
    private long readyAt;
    public void bind(Object key) {
        if (!Objects.equals(context, key)) { context = key; cycling = resume = reload = latch = false; readyAt = 0; }
    }
    public void cycle(Object key, boolean aimed, long now, long duration) {
        bind(key); if (key == null || reload || cycling) return;
        cycling = true; resume = aimed; readyAt = now + Math.max(0, duration);
    }
    public void reload(Object key) { bind(key); if (key != null) { reload = latch = true; cycling = resume = false; } }
    public boolean input(boolean requested) {
        if (!requested) { resume = latch = false; return false; }
        if (reload || latch) return false;
        if (cycling) { resume = true; return false; }
        return true;
    }
    public boolean inputView(boolean actual) { return cycling ? resume : latch || actual; }
    public Boolean update(Object key, boolean reloading, boolean cyclingNow, long now) {
        bind(key); if (key == null) return null;
        reload = reloading;
        if (reload) { cycling = resume = false; latch = true; return false; }
        if (cycling) {
            if (cyclingNow || now < readyAt) return false;
            cycling = false; boolean restore = resume; resume = false;
            return restore ? Boolean.TRUE : null;
        }
        return latch ? Boolean.FALSE : null;
    }
    public void release() { latch = false; }
}
