package dev.minestrike.skins;

import java.util.*;
import dev.minestrike.skins.TrailGeometry.*;

public final class TrailPath {
    public record Part(Segment actual, Segment firstPerson, double start, double duration) {}
    private final List<Part> parts = new ArrayList<>();
    private boolean preview;
    private double length, updated;
    private double displayDelay = Double.NaN;

    public boolean append(Segment actual, Segment firstPerson, boolean initial, double limit) {
        return append(actual, firstPerson, initial, limit, actual.to().sub(actual.from()).length());
    }

    public boolean append(Segment actual, Segment firstPerson, boolean initial, double limit, double blocksPerTick) {
        double distance = actual.to().sub(actual.from()).length();
        if (!actual.from().finite() || !actual.to().finite() || !firstPerson.from().finite()
                || !firstPerson.to().finite() || distance < 1e-5 || !Double.isFinite(distance)
                || !Double.isFinite(blocksPerTick) || blocksPerTick <= 0) return false;
        if (initial && !parts.isEmpty()) return false;
        double start = actual.born();
        if (!initial && preview && parts.get(parts.size() - 1).actual().to().sub(actual.from()).length() >= 1e-4) {
            start = parts.get(0).start();
            parts.clear(); length = 0;
        }
        if (parts.size() >= 128 || length >= limit) return false;
        double fraction = Math.min(1, (limit - length) / distance);
        Point visualFrom = firstPerson.from();
        if (!parts.isEmpty()) {
            Part previous = parts.get(parts.size() - 1);
            start = Math.max(start, previous.start() + previous.duration());
            if (previous.actual().to().sub(actual.from()).length() < 1e-4)
                visualFrom = previous.firstPerson().to();
        }
        Segment clipped = new Segment(actual.from(), actual.from().add(actual.to().sub(actual.from()).scale(fraction)), actual.born(), actual.own());
        Segment visual = new Segment(visualFrom, firstPerson.from().add(firstPerson.to().sub(firstPerson.from()).scale(fraction)), actual.born(), actual.own());
        double duration = distance * fraction / blocksPerTick;
        if (parts.isEmpty()) duration = Math.max(duration, 2.4 + 1.2 * Math.min(1, distance * fraction / 128));
        parts.add(new Part(clipped, visual, start, duration));
        length += distance * fraction;
        updated = actual.born(); preview = initial;
        return true;
    }

    public List<Segment> segments(boolean firstPerson, double now) {
        List<Segment> result = new ArrayList<>(parts.size());
        if (parts.isEmpty()) return result;
        if (Double.isNaN(displayDelay)) displayDelay = Math.max(0, now - parts.get(0).start());
        double animationTime = now - displayDelay;
        double head = 0;
        for (Part part : parts) {
            double distance = part.actual().to().sub(part.actual().from()).length();
            double progress = Math.max(0, Math.min(1, (animationTime - part.start()) / part.duration()));
            double initial = part == parts.get(0) ? Math.min(2.1, distance) : 0;
            head += initial + (distance - initial) * progress;
            if (progress < 1) break;
        }
        double tail = Math.max(2, head - 6), offset = 0;
        double born = Math.min(now, endTime() + displayDelay);
        for (Part part : parts) {
            double distance = part.actual().to().sub(part.actual().from()).length();
            double fromDistance = Math.max(tail, offset), toDistance = Math.min(head, offset + distance);
            if (toDistance <= fromDistance + 1e-5) { offset += distance; continue; }
            Segment s = firstPerson ? part.firstPerson() : part.actual();
            Point delta = s.to().sub(s.from());
            Point from = s.from().add(delta.scale((fromDistance - offset) / distance));
            Point to = s.from().add(delta.scale((toDistance - offset) / distance));
            result.add(new Segment(from, to, born, s.own()));
            offset += distance;
        }
        return result;
    }

    private double endTime() {
        if (parts.isEmpty()) return updated;
        Part last = parts.get(parts.size() - 1);
        return last.start() + last.duration();
    }

    public boolean expired(double now, double life) {
        if (parts.isEmpty() || now < parts.get(0).start()) return true;
        return now - endTime() - (Double.isNaN(displayDelay) ? 0 : displayDelay) >= life;
    }
}
