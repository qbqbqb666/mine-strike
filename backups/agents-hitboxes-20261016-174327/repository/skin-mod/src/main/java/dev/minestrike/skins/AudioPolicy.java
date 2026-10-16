package dev.minestrike.skins;

import java.util.*;
import java.util.function.Predicate;

public final class AudioPolicy {
    public enum Kind { OTHER, STEP, GUN, VEHICLE }
    public interface Source { UUID minestrike$audioEmitter(); }
    public record Emitter(UUID entity, String sound) {}

    public static boolean local(UUID emitter, UUID player, UUID vehicle) {
        return emitter != null && (emitter.equals(player) || emitter.equals(vehicle));
    }

    public static Kind kind(String id) {
        int colon = id.indexOf(':');
        if (colon < 0) return Kind.OTHER;
        String namespace = id.substring(0, colon), path = id.substring(colon + 1);
        if (namespace.equals("minecraft") && path.startsWith("block.") && path.endsWith(".step")) return Kind.STEP;
        if (namespace.equals("tacz") && path.equals("gun")) return Kind.GUN;
        if (namespace.equals("superbwarfare")) {
            if (path.contains("_1p") || path.contains("reload")) return Kind.OTHER;
            if (path.contains("engine") || path.equals("track_move") || path.equals("wheel_step")
                    || path.equals("track_vehicle_step") || path.equals("wheel_vehicle_step")
                    || path.equals("track_vehicle_skip") || path.equals("wheel_vehicle_skip")) return Kind.VEHICLE;
            if (path.contains("_fire_3p") || path.endsWith("_far") || path.endsWith("_veryfar")
                    || path.equals("lav_ad_fire")) return Kind.GUN;
        }
        return Kind.OTHER;
    }

    public static boolean taczShot(String path) {
        String name = path.substring(path.lastIndexOf('/') + 1);
        if (name.contains("reload") || name.contains("inspect") || name.contains("draw") || name.contains("bolt")) return false;
        return name.contains("shoot") || name.contains("silence");
    }

    public static float shotGain(float volume, double distance, double range, double exponent) {
        if (range <= 0 || distance >= range) return 0;
        return (float)(volume * Math.pow(Math.max(0, 1 - Math.max(0, distance) / range), exponent));
    }

    public static float mixGain(float original, double multiplier) {
        return (float)Math.max(0, Math.min(1, original * multiplier));
    }

    public static final class TailLimiter<T> {
        private record Voice<T>(T sound, long started) {}
        private final Map<Emitter, ArrayDeque<Voice<T>>> voices = new HashMap<>();

        public List<T> add(Emitter emitter, T sound, long now, int max) {
            ArrayDeque<Voice<T>> queue = voices.computeIfAbsent(emitter, key -> new ArrayDeque<>());
            List<T> retired = new ArrayList<>();
            while (queue.size() >= max && now - queue.peekFirst().started() >= 150_000_000L)
                retired.add(queue.removeFirst().sound());
            queue.addLast(new Voice<>(sound, now));
            return retired;
        }

        public void prune(Predicate<T> active) {
            voices.values().forEach(queue -> queue.removeIf(voice -> !active.test(voice.sound())));
            voices.values().removeIf(ArrayDeque::isEmpty);
        }
        public void clear() { voices.clear(); }
    }
    private AudioPolicy() {}
}
