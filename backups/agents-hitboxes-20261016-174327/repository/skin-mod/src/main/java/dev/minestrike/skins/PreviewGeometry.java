package dev.minestrike.skins;

public final class PreviewGeometry {
    private PreviewGeometry() {}

    public static int[] intersection(int x, int y, int width, int height,
                                     int clipX, int clipY, int clipWidth, int clipHeight) {
        int left = Math.max(x, clipX), top = Math.max(y, clipY);
        int right = Math.min(x + width, clipX + clipWidth), bottom = Math.min(y + height, clipY + clipHeight);
        return right > left && bottom > top ? new int[]{left, top, right, bottom} : null;
    }
}
