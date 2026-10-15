import dev.minestrike.skins.TrailGeometry;
import dev.minestrike.skins.TrailGeometry.*;

public class TrailGeometryTest {
    private static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-6) throw new AssertionError(actual + " != " + expected);
    }
    private static Ribbon frame(Segment s, Point eye, double now) {
        return TrailGeometry.frame(s, eye, now, 3, 6, .025, 3, 1.8);
    }
    public static void main(String[] args) {
        Point origin = new Point(0, 0, 0), end = new Point(100, 0, 0);
        Segment shot = new Segment(origin, end, 10);
        close(TrailGeometry.closest(origin, end, new Point(-5, 4, 0)).x(), 0);
        close(TrailGeometry.closest(origin, end, new Point(105, 4, 0)).x(), 100);
        close(TrailGeometry.closest(origin, end, new Point(50, 4, 0)).x(), 50);
        close(TrailGeometry.distance(shot, new Point(50, 4, 0)), 4);
        assert frame(shot, new Point(50, 10, 0), 9) == null;
        assert frame(shot, new Point(50, 10, 0), 10) == null;
        assert frame(shot, new Point(50, 10, 0), 13) == null;
        Ribbon normal = frame(shot, new Point(50, 10, 0), 10.5);
        close(normal.head().x(), 50);
        close(normal.head().sub(normal.tail()).length(), 6);
        assert normal.head().x() > normal.tail().x();
        assert !normal.near();
        Ribbon near = frame(shot, new Point(50, 1, 0), 11);
        assert near.near();
        close(near.tail().x(), 47);
        close(near.head().x(), 53);
        assert near.width() > normal.width();
        assert near.alpha() > frame(shot, new Point(50, 10, 0), 11).alpha();
        assert near.alpha() > frame(shot, new Point(50, 1, 0), 12).alpha();
        Ribbon reversed = frame(new Segment(end, origin, 10), new Point(50, 1, 0), 11);
        assert reversed.head().x() < reversed.tail().x();
        Ribbon incoming = frame(shot, new Point(50, 0, 0), 11);
        assert incoming.side().finite();
        close(incoming.side().length(), 1);
        Ribbon vertical = frame(new Segment(origin, new Point(0, 100, 0), 10), new Point(0, 50, 0), 11);
        assert vertical.side().finite();
        close(vertical.side().length(), 1);
        assert frame(new Segment(origin, origin, 10), origin, 11) == null;
        assert frame(new Segment(origin, new Point(Double.NaN, 0, 0), 10), origin, 11) == null;
        Segment clipped = new Segment(origin, new Point(4, 0, 0), 10);
        Ribbon impact = frame(clipped, new Point(3, 1, 0), 11);
        assert impact.head().x() <= 4 && impact.tail().x() >= 0;
        for (double tick = 10.1; tick < 13; tick += .05) {
            Ribbon r = frame(clipped, new Point(3, 1, 0), tick);
            assert r.head().x() <= 4 && r.tail().x() >= -1e-6;
            assert r.alpha() >= 0 && r.alpha() <= 1;
            assert r.head().sub(r.tail()).length() <= 6;
        }
        System.out.println("PASS: tracer direction, short length, animation/fade, near-pass emphasis, collision-end bounds and degenerate views");
    }
}
