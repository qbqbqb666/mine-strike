package dev.minestrike.skins;

import java.util.*;

public final class ShopRows {
    public static final int COLUMNS = 6, MIN_ROWS = 0, MAX_ROWS = 32;
    public static int row(int slot) { return slot / COLUMNS; }
    public static int column(int slot) { return slot % COLUMNS; }
    public static int index(int row, int column) { return row * COLUMNS + column; }
    public static int rows(Collection<? extends List<?>> lists) {
        int rows = lists.stream().mapToInt(List::size).max().orElse(0);
        if (rows > MAX_ROWS) throw new IllegalArgumentException("每个分类最多支持 " + MAX_ROWS + " 个商品。");
        return rows;
    }
    public static <T> void add(List<T> list, T empty) {
        if (list.size() >= MAX_ROWS) throw new IllegalArgumentException("该分类已达到商品上限。");
        list.add(empty);
    }
    public static <T> void remove(List<T> list, int index) { list.remove(index); }
    public static int bodyHeight(int rows) { return 98 * Math.max(1, rows); }
    public static float scale(int width, int height, int rows) {
        return Math.min(width / 1080f, height / (float) (190 + bodyHeight(rows)));
    }
}
