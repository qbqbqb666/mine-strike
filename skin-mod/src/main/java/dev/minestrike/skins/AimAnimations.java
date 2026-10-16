package dev.minestrike.skins;

import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import java.util.Locale;

public final class AimAnimations {
    public static boolean name(String name, boolean boltOrDrawOnly) { return name(name, boltOrDrawOnly, false); }
    public static boolean name(String name, boolean boltOrDrawOnly, boolean cyclingShoot) {
        if (name == null) return false;
        String value = name.toLowerCase(Locale.ROOT);
        return value.contains("bolt") || value.contains("blot") || value.contains("draw")
                || value.contains("equip") || (cyclingShoot && value.contains("shoot"))
                || (!boltOrDrawOnly && value.contains("reload"));
    }
    public static boolean active(ObjectAnimationRunner runner, boolean boltOrDrawOnly) { return active(runner, boltOrDrawOnly, false); }
    public static boolean active(ObjectAnimationRunner runner, boolean boltOrDrawOnly, boolean cyclingShoot) {
        if (runner == null) return false;
        if ((runner.isRunning() || runner.isHolding() || runner.isPausing() || runner.isTransitioning())
                && name(runner.getAnimation().name, boltOrDrawOnly, cyclingShoot)) return true;
        var next = runner.getTransitionTo();
        return runner.isTransitioning() && next != null && name(next.getAnimation().name, boltOrDrawOnly, cyclingShoot);
    }
    public static double cycleSeconds(com.tacz.guns.resource.index.CommonGunIndex index, com.tacz.guns.api.item.gun.FireMode mode) {
        // The native SPAS script feeds the chamber immediately in its BURST (semi-auto) mode.
        if (mode == com.tacz.guns.api.item.gun.FireMode.BURST
                && "tacz:spas_12_gun_logic".equals(String.valueOf(index.getGunData().getScript()))) return 0;
        return cycleSeconds(index.getGunData().getBoltActionTime(), index.getGunData().getScriptParam());
    }
    public static double cycleSeconds(float nativeTime, java.util.Map<String, Object> params) {
        Object scriptTime = params == null ? null : params.get("bolt_time");
        return Math.max(nativeTime, scriptTime instanceof Number number ? number.doubleValue() : 0);
    }
    private AimAnimations() {}
}
