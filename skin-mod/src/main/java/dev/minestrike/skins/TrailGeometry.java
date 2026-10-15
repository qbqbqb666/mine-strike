package dev.minestrike.skins;

public final class TrailGeometry {
    public record Point(double x, double y, double z) {
        public Point add(Point p) { return new Point(x + p.x, y + p.y, z + p.z); }
        public Point sub(Point p) { return new Point(x - p.x, y - p.y, z - p.z); }
        public Point scale(double value) { return new Point(x * value, y * value, z * value); }
        public double dot(Point p) { return x * p.x + y * p.y + z * p.z; }
        public Point cross(Point p) { return new Point(y * p.z - z * p.y, z * p.x - x * p.z, x * p.y - y * p.x); }
        public double length() { return Math.sqrt(dot(this)); }
        public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    }
    public record Segment(Point from, Point to, double born) {}
    public record Ribbon(Point tail, Point head, Point side, double alpha, double width, boolean near) {}

    public static Point closest(Point from, Point to, Point eye) {
        Point delta = to.sub(from);
        double squared = delta.dot(delta);
        double t = squared < 1e-12 ? 0 : clamp(eye.sub(from).dot(delta) / squared, 0, 1);
        return from.add(delta.scale(t));
    }

    public static double distance(Segment segment, Point eye) {
        return closest(segment.from(), segment.to(), eye).sub(eye).length();
    }

    public static Ribbon frame(Segment s, Point eye, double now, double lifeTicks,
                               double maxLength, double width, double nearRadius, double nearBoost) {
        double age = now - s.born();
        if (age < 0 || age >= lifeTicks || !s.from().finite() || !s.to().finite() || !eye.finite()) return null;
        Point delta = s.to().sub(s.from());
        double length = delta.length();
        if (length < 1e-5 || !Double.isFinite(length)) return null;
        Point direction = delta.scale(1 / length);
        boolean near = distance(s, eye) <= nearRadius;
        Point from = s.from(), to = s.to();
        if (near) {
            double center = closest(from, to, eye).sub(from).dot(direction);
            from = s.from().add(direction.scale(Math.max(0, center - maxLength * .5)));
            to = s.from().add(direction.scale(Math.min(length, center + maxLength * .5)));
        }
        Point head = from.add(to.sub(from).scale(clamp(age, 0, 1)));
        Point tail = head.sub(direction.scale(Math.min(maxLength, head.sub(from).length())));
        if (head.sub(tail).length() < 1e-5) return null;
        Point side = direction.cross(eye.sub(head));
        if (side.length() < 1e-6) side = direction.cross(new Point(0, 1, 0));
        if (side.length() < 1e-6) side = direction.cross(new Point(1, 0, 0));
        side = side.scale(1 / side.length());
        double distanceScale = 1 + Math.min(distance(s, eye), 128) / 128;
        return new Ribbon(tail, head, side, (1 - age / lifeTicks) * (near ? 1 : .75),
                width * distanceScale * (near ? nearBoost : 1), near);
    }
    private static double clamp(double value, double low, double high) { return Math.max(low, Math.min(high, value)); }
    private TrailGeometry() {}
}
