import dev.minestrike.skins.PreviewGeometry;
import java.util.Arrays;

public class PreviewGeometryTest {
    private static void check(int[] actual, int[] expected) {
        if (!Arrays.equals(actual, expected)) throw new AssertionError(Arrays.toString(actual));
    }
    public static void main(String[] args) {
        check(PreviewGeometry.intersection(20, 100, 200, 100, 10, 80, 300, 260), new int[]{20, 100, 220, 200});
        check(PreviewGeometry.intersection(20, 320, 200, 100, 10, 80, 300, 260), new int[]{20, 320, 220, 340});
        check(PreviewGeometry.intersection(20, 10, 200, 100, 10, 80, 300, 260), new int[]{20, 80, 220, 110});
        check(PreviewGeometry.intersection(20, 400, 200, 100, 10, 80, 300, 260), null);
        check(PreviewGeometry.intersection(-80, 100, 200, 100, 10, 80, 300, 260), new int[]{10, 100, 120, 200});
        check(PreviewGeometry.intersection(20, 340, 200, 100, 10, 80, 300, 260), null);
        System.out.println("PASS: preview clipping at top/bottom/sides, fully hidden and viewport boundary");
    }
}
