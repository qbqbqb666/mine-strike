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
            assert path.append(physical, visual, true, 128, 100);
            double firstFrame = 10.95, duration = 2.4 + 1.2 * 100 / 128;
            close(path.segments(false, firstFrame).get(0).to().z(), 2.1);
            for (int fps : new int[]{30, 60}) {
                double previous = 2.1; int movingFrames = 0;
                for (double now = firstFrame; now < firstFrame + duration; now += 20.0 / fps) {
                    var moving = path.segments(false, now);
                    assert !moving.isEmpty();
                    Segment light = moving.get(moving.size() - 1);
                    close(light.to().z(), 2.1 + 97.9 * (now - firstFrame) / duration);
                    assert light.to().z() >= previous && light.to().z() < 100;
                    assert light.from().z() >= 2 - 1e-6;
                    assert light.to().sub(light.from()).length() <= 6 + 1e-6;
                    previous = light.to().z(); movingFrames++;
                }
                assert movingFrames >= 4 : "Flight must advance across multiple rendered frames";
            }
            double flightEnd = firstFrame + duration;
            close(path.segments(false, flightEnd).get(0).to().z(), 100);
            close(path.segments(false, flightEnd).get(0).from().z(), 94);
            assert !path.expired(flightEnd + 2.99, 3) && path.expired(flightEnd + 3.01, 3);
            assert frame(path.segments(true, flightEnd + 1).get(0), origin, flightEnd + 1).alpha()
                    < frame(path.segments(true, flightEnd).get(0), origin, flightEnd).alpha();
        }
        TrailPath impactPath = new TrailPath();
        Segment clippedShot = new Segment(origin, new Point(9, 0, 0), 10, false);
        assert impactPath.append(clippedShot, clippedShot, true, 128, 12.5);
        close(impactPath.segments(false, 10.95).get(0).to().x(), 2.1);
        double impactDuration = 2.4 + 1.2 * 9 / 128;
        close(impactPath.segments(false, 10.95 + impactDuration / 2).get(0).to().x(), 5.55);
        double impactTime = 10.95 + impactDuration;
        close(impactPath.segments(false, impactTime).get(0).to().x(), 9);
        close(impactPath.segments(false, impactTime + .1).get(0).born(), impactTime);
        assert impactPath.expired(impactTime + 3.01, 3);

        TrailPath curve = new TrailPath();
        Segment preview = new Segment(origin, new Point(12, 0, 0), 10, false);
        assert curve.append(preview, preview, true, 128, 12);
        assert !curve.append(preview, preview, true, 128, 12);
        close(curve.segments(false, 10.95).get(0).to().x(), 2.1);
        Segment first = new Segment(origin, new Point(10, -1, 0), 11, false);
        assert curve.append(first, first, false, 128, first.to().length());
        assert curve.segments(false, 11.1).get(0).to().length() > 2.1 : "Preview replacement must preserve the first-frame clock";
        Segment second = new Segment(first.to(), new Point(20, -3, 0), 11, false);
        assert curve.append(second, second, false, 128, second.to().sub(second.from()).length());
        double jointTime = 10.95 + 2.4 + 1.2 * first.to().length() / 128;
        var bent = curve.segments(false, jointTime + .2);
        assert bent.size() == 2 && bent.get(0).to().equals(bent.get(1).from());
        close(bent.get(1).to().x(), 12); close(bent.get(1).to().y(), -1.4);
        close(bent.stream().mapToDouble(piece -> piece.to().sub(piece.from()).length()).sum(), 6);
        assert !curve.expired(jointTime + 3.99, 3) && curve.expired(jointTime + 4.01, 3);
        TrailPath bounded = new TrailPath();
        Segment longShot = new Segment(origin, new Point(200, 0, 0), 10, false);
        assert bounded.append(longShot, longShot, true, 128, 200);
        bounded.segments(false, 10.95);
        close(bounded.segments(false, 14.55).get(0).to().x(), 128);
        assert !bounded.append(longShot, longShot, true, 128);
        TrailPath closeImpact = new TrailPath();
        Segment shortShot = new Segment(origin, new Point(1.5, 0, 0), 10);
        assert closeImpact.append(shortShot, shortShot, true, 128);
        assert closeImpact.segments(false, 11).isEmpty();

        for (int height : new int[]{720, 1080, 2160}) for (int fov : new int[]{45, 70, 100}) {
            double lens = height / (2 * Math.tan(Math.toRadians(fov) / 2));
            for (int range : new int[]{4, 32, 64, 128}) {
                Ribbon r = frame(new Segment(new Point(0, 0, Math.max(2, range - 6)), new Point(0, 0, range), 10, true), origin, 10);
                var mesh = TrailGeometry.head(r, origin, lens);
                assert mesh.size() == 12;
                double area = 0, radiusPixels = 0;
                for (TrailGeometry.Triangle triangle : mesh) {
                    Point x = triangle.a(), y = triangle.b(), z = triangle.c();
                    area += Math.abs((y.x()/y.z()-x.x()/x.z())*(z.y()/z.z()-x.y()/x.z())
                            - (y.y()/y.z()-x.y()/x.z())*(z.x()/z.z()-x.x()/x.z())) * lens * lens / 2;
                    for (Point vertex : new Point[]{x, y, z}) {
                        assert vertex.z() <= range + 1e-8 : "Head must not extend past the clipped endpoint";
                        radiusPixels = Math.max(radiusPixels, lens * Math.hypot(vertex.x()/vertex.z(), vertex.y()/vertex.z()));
                    }
                }
                assert area > 4 : "Closed head must remain visible directly along the shot axis";
                assert radiusPixels >= 1 && radiusPixels <= 1.6 : "Keep head near two to three pixels, without a large patch";
            }
        }
        System.out.println("PASS: 30 no-tick shots begin on the first fractional render frame, advance across 30/60fps frames, keep six-block tails and fade after arrival");
        System.out.println("PASS: preview clock, curved joints, muzzle gap, wall endpoints and 128-block bound; closed head has positive axial area at 720/1080/2160p and 45/70/100 FOV");
        System.out.println("PASS: first-person eye-ray projection reproduces invisible streak; native muzzle offset restores >10px hip-fire length, ADS core >=1px");
        System.out.println("PASS: nonzero birth-frame trails, no replay from zero, own-shot camera exclusion without near-pass amplification");
        System.out.println("PASS: full path direction, fade, near-pass emphasis without cropping, collision-end bounds and degenerate views");
    }
    private static double projectedLength(Ribbon ribbon, double focal) {
        Point a = ribbon.tail(), b = ribbon.head();
        return focal * Math.hypot(a.x() / a.z() - b.x() / b.z(), a.y() / a.z() - b.y() / b.z());
    }
}
