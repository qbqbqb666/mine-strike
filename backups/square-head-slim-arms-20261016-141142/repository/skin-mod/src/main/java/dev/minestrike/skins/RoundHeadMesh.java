package dev.minestrike.skins;

import java.util.ArrayList;
import java.util.List;

public final class RoundHeadMesh {
    public record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {}
    private static final int STEPS = 12;
    private static final int[][] CORNERS = {
            {-1,-1,-1}, {1,-1,-1}, {1,1,-1}, {-1,1,-1},
            {-1,-1,1}, {1,-1,1}, {1,1,1}, {-1,1,1}
    };
    // The vanilla cube face order and UV rectangles, including the reversed bottom face.
    private static final int[][] FACES = {
            {5,4,0,1, 8,0,16,8}, {2,3,7,6, 16,8,24,0},
            {0,4,7,3, 0,8,8,16}, {1,0,3,2, 8,8,16,16},
            {5,1,2,6, 16,8,24,16}, {4,5,6,7, 24,8,32,16}
    };
    public static final List<Vertex> HEAD = build(false), HAT = build(true);

    private static List<Vertex> build(boolean hat) {
        List<Vertex> vertices = new ArrayList<>(6 * STEPS * STEPS * 4);
        for (int[] face : FACES) for (int y = 0; y < STEPS; y++) for (int x = 0; x < STEPS; x++) {
            vertices.add(vertex(face, x, y, hat));
            vertices.add(vertex(face, x + 1, y, hat));
            vertices.add(vertex(face, x + 1, y + 1, hat));
            vertices.add(vertex(face, x, y + 1, hat));
        }
        return List.copyOf(vertices);
    }

    private static Vertex vertex(int[] face, int column, int row, boolean hat) {
        double a = column / (double)STEPS, b = row / (double)STEPS;
        double[] p = new double[3];
        for (int axis = 0; axis < 3; axis++)
            p[axis] = (1-b) * ((1-a) * CORNERS[face[0]][axis] + a * CORNERS[face[1]][axis])
                    + b * ((1-a) * CORNERS[face[3]][axis] + a * CORNERS[face[2]][axis]);
        double x = p[0], y = p[1], z = p[2];
        float nx = (float)(x * Math.sqrt(1 - y*y/2 - z*z/2 + y*y*z*z/3));
        float ny = (float)(y * Math.sqrt(1 - z*z/2 - x*x/2 + z*z*x*x/3));
        float nz = (float)(z * Math.sqrt(1 - x*x/2 - y*y/2 + x*x*y*y/3));
        float radius = hat ? 4.5f : 4;
        return new Vertex(nx * radius, ny * radius - 4, nz * radius,
                (float)((face[6] + (face[4] - face[6]) * a + (hat ? 32 : 0)) / 64),
                (float)((face[5] + (face[7] - face[5]) * b) / 64), nx, ny, nz);
    }

    private RoundHeadMesh() {}
}
