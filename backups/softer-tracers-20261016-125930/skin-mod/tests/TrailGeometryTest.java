import dev.minestrike.skins.TrailGeometry;
import dev.minestrike.skins.TrailPath;
import dev.minestrike.skins.TrailGeometry.*;

public class TrailGeometryTest {
    private static void close(double actual, double expected) {
        if (Math.abs(actual - expected) > 1e-6) throw new AssertionError(actual + " != " + expected);
    }
    private static Ribbon frame(Segment s, Point eye, double now) {
        return TrailGeometry.frame(s, eye, now, 3, .025, 3, 1.8);
    }
    public static void main(String[] args) {
        Point origin = new Point(0, 0, 0), end = new Point(100, 0, 0);
        Segment shot = new Segment(origin, end, 10);
        close(TrailGeometry.closest(origin, end, new Point(-5, 4, 0)).x(), 0);
        close(TrailGeometry.closest(origin, end, new Point(105, 4, 0)).x(), 100);
        close(TrailGeometry.closest(origin, end, new Point(50, 4, 0)).x(), 50);
        close(TrailGeometry.distance(shot, new Point(50, 4, 0)), 4);
        assert frame(shot, new Point(50, 10, 0), 9) == null;
        assert frame(shot, new Point(50, 10, 0), 10) != null;
        assert frame(shot, new Point(50, 10, 0), 13) == null;
        Ribbon normal = frame(shot, new Point(50, 10, 0), 10.5);
        close(normal.head().x(), 100);
        close(normal.head().sub(normal.tail()).length(), 100);
        close(normal.tail().x(), 0);
        assert normal.head().x() > normal.tail().x();
        assert !normal.near();
        Ribbon near = frame(shot, new Point(50, 1, 0), 11);
        assert near.near();
        close(near.tail().x(), 0);
        close(near.head().x(), 100);
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
        Segment own = new Segment(origin, new Point(4, 0, 0), 10, true);
        Ribbon ownFirst = frame(own, origin, 10);
        assert ownFirst != null && !ownFirst.near();
        close(ownFirst.tail().x(), 1);
        close(ownFirst.head().x(), 4);
        assert ownFirst.tailWidth() > 0 && ownFirst.tailWidth() <= .0014 + 1e-8;
        Point direction = ownFirst.head().sub(ownFirst.tail());
        direction = direction.scale(1 / direction.length());
        close(ownFirst.up().length(), 1);
        close(ownFirst.up().dot(direction), 0);
        close(ownFirst.up().dot(ownFirst.side()), 0);
        Point radial = ownFirst.side().scale(.5).add(ownFirst.up().scale(Math.sqrt(3) / 2));
        assert Math.abs(ownFirst.side().cross(radial).dot(direction)) > .8;
        assert ownFirst.width() < frame(new Segment(origin, new Point(4, 0, 0), 10), origin, 10).width();
        for (double tick = 10; tick < 13; tick += .01) {
            Ribbon r = frame(own, origin, tick);
            assert r != null && !r.near() && r.tail().length() >= 1 - 1e-6;
            assert r.head().sub(r.tail()).length() > 0;
            close(r.head().x(), 4);
        }
        assert frame(new Segment(origin, new Point(.5, 0, 0), 10, true), origin, 10) == null;
        Ribbon crossesEye = frame(new Segment(new Point(-4, 0, 0), new Point(4, 0, 0), 10, true), origin, 10);
        close(crossesEye.tail().x(), 1);
        assert frame(own, origin, 13) == null;
        Segment eyeShot = new Segment(origin, new Point(0, 0, 15), 10, true);
        Ribbon eyeRibbon = frame(eyeShot, origin, 10);
        double focal = 1080 / (2 * Math.tan(Math.toRadians(70) / 2));
        close(projectedLength(eyeRibbon, focal), 0);
        Segment muzzleShot = TrailGeometry.firstPerson(eyeShot, origin, new Point(.3, -.2, -.5));
        Ribbon muzzleRibbon = frame(muzzleShot, origin, 10);
        assert projectedLength(muzzleRibbon, focal) > 10 : "Hip-fire streak must extend visibly away from the reticle";
        assert !muzzleRibbon.near() && muzzleRibbon.tail().length() >= 1 - 1e-6;
        close(eyeShot.from().length(), 0);
        close(eyeShot.to().z(), 15);
        Segment distant = new Segment(new Point(0, 0, 50), new Point(0, 0, 65), 10, true);
        assert TrailGeometry.firstPerson(distant, origin, new Point(.3, -.2, -.5)).equals(distant);
        for (int range : new int[]{15, 50, 100, 128}) {
            Ribbon aimed = frame(new Segment(origin, new Point(0, 0, range), 10, true), origin, 10);
            double corePixels = 2 * focal * aimed.width() / aimed.head().z();
            assert corePixels >= 1 : "Aligned ADS streak must retain at least one pixel of core width";
            assert corePixels * 2.8 < 9 : "No oversized near-camera glow";
        }
        assert TrailGeometry.firstPerson(eyeShot, origin, origin).equals(eyeShot);
        for (int shotIndex = 0; shotIndex < 30; shotIndex++) {
            TrailPath path = new TrailPath();
            Segment physical = new Segment(origin, new Point(0, 0, 100), 10, true);
            Segment visual = TrailGeometry.firstPerson(physical, origin, new Point(.3, -.2, -.5));
            assert path.append(physical, visual, true, 128);
            assert path.segments(true).size() == 1;
            Ribbon complete = frame(path.segments(true).get(0), origin, 10);
            assert complete != null && complete.head().sub(complete.tail()).length() > 98;
            assert projectedLength(complete, focal) > 100 : "Long shots must retain the muzzle-side path";
            assert !path.expired(12.99, 3) && path.expired(13, 3);
        }
        TrailPath curve = new TrailPath();
        Segment preview = new Segment(origin, new Point(12, 0, 0), 10, false);
        assert curve.append(preview, preview, true, 128);
        assert !curve.append(preview, preview, true, 128);
        Segment first = new Segment(origin, new Point(10, -1, 0), 10, false);
        assert curve.append(first, first, false, 128);
        assert curve.segments(false).size() == 1 && curve.segments(false).get(0).to().equals(first.to());
        Segment second = new Segment(first.to(), new Point(20, -3, 0), 12, false);
        assert curve.append(second, second, false, 128);
        assert curve.segments(false).size() == 2;
        assert curve.segments(false).get(0).from().equals(origin);
        assert curve.segments(false).get(0).to().equals(curve.segments(false).get(1).from());
        close(curve.segments(false).get(0).born(), 12);
        assert !curve.expired(14.99, 3) && curve.expired(15, 3);
        TrailPath advanced = new TrailPath();
        assert advanced.append(first, first, true, 128);
        assert advanced.append(second, second, false, 128);
        assert advanced.segments(false).size() == 2 : "A first tick starting at the preview endpoint must preserve the confirmed prefix";
        TrailPath bounded = new TrailPath();
        Segment longShot = new Segment(origin, new Point(200, 0, 0), 10, false);
        assert bounded.append(longShot, longShot, true, 128);
        close(bounded.segments(false).get(0).to().x(), 128);
        assert !bounded.append(new Segment(new Point(200, 0, 0), new Point(210, 0, 0), 11), longShot, true, 128);
        System.out.println("PASS: all 30 no-tick shots retain full 100-block paths; curved joints, preview replacement, confirmed prefix, shared fade and 128-block bound");
        System.out.println("PASS: first-person eye-ray projection reproduces invisible streak; native muzzle offset restores >10px hip-fire length, ADS core >=1px");
        System.out.println("PASS: nonzero birth-frame trails, no replay from zero, own-shot camera exclusion without near-pass amplification");
        System.out.println("PASS: full path direction, fade, near-pass emphasis without cropping, collision-end bounds and degenerate views");
    }
    private static double projectedLength(Ribbon ribbon, double focal) {
        Point a = ribbon.tail(), b = ribbon.head();
        return focal * Math.hypot(a.x() / a.z() - b.x() / b.z(), a.y() / a.z() - b.y() / b.z());
    }
}
