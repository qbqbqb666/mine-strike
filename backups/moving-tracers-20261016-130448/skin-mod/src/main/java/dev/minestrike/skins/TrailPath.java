package dev.minestrike.skins;

import java.util.*;
import dev.minestrike.skins.TrailGeometry.*;

public final class TrailPath {
    public record Part(Segment actual, Segment firstPerson) {}
    private final List<Part> parts = new ArrayList<>();
    private boolean preview;
    private double length, updated;

    public boolean append(Segment actual, Segment firstPerson, boolean initial, double limit) {
        double distance = actual.to().sub(actual.from()).length();
        if (!actual.from().finite() || !actual.to().finite() || !firstPerson.from().finite()
                || !firstPerson.to().finite() || distance < 1e-5 || !Double.isFinite(distance)) return false;
        if (initial && !parts.isEmpty()) return false;
        if (!initial && preview && parts.get(parts.size() - 1).actual().to().sub(actual.from()).length() >= 1e-4) {
            parts.clear(); length = 0;
        }
        if (parts.size() >= 128 || length >= limit) return false;
        double fraction = Math.min(1, (limit - length) / distance);
        Point visualFrom = firstPerson.from();
        if (!parts.isEmpty()) {
            Part previous = parts.get(parts.size() - 1);
            if (previous.actual().to().sub(actual.from()).length() < 1e-4)
                visualFrom = previous.firstPerson().to();
        }
        Segment clipped = new Segment(actual.from(), actual.from().add(actual.to().sub(actual.from()).scale(fraction)), actual.born(), actual.own());
        Segment visual = new Segment(visualFrom, firstPerson.from().add(firstPerson.to().sub(firstPerson.from()).scale(fraction)), actual.born(), actual.own());
        parts.add(new Part(clipped, visual));
        length += distance * fraction;
        updated = actual.born(); preview = initial;
        return true;
    }

    public List<Segment> segments(boolean firstPerson) {
        List<Segment> result = new ArrayList<>(parts.size());
        double skip = 2;
        for (Part part : parts) {
            double distance = part.actual().to().sub(part.actual().from()).length();
            if (skip >= distance) { skip -= distance; continue; }
            Segment s = firstPerson ? part.firstPerson() : part.actual();
            Point from = s.from().add(s.to().sub(s.from()).scale(skip / distance));
            skip = 0;
            result.add(new Segment(from, s.to(), updated, s.own()));
        }
        return result;
    }

    public boolean expired(double now, double life) { return now < updated || now - updated >= life; }
}
