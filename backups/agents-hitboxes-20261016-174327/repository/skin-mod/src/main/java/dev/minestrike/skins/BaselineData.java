package dev.minestrike.skins;

import net.minecraft.nbt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public record BaselineData(String map, String dimension, Bounds bounds, ListTag palette,
                           int[] blocks, ListTag blockEntities, ListTag entities) {
    public record Bounds(int x, int y, int z, int sx, int sy, int sz) {
        public Bounds {
            if (sx <= 0 || sy <= 0 || sz <= 0 || (long) sx * sy * sz > 8_000_000)
                throw new IllegalArgumentException("地图区域为空或超过 800 万个方块。");
            Math.addExact(x, sx - 1); Math.addExact(y, sy - 1); Math.addExact(z, sz - 1);
        }
        public int volume() { return sx * sy * sz; }
        public int px(int i) { return x + i % sx; }
        public int py(int i) { return y + i / (sx * sz); }
        public int pz(int i) { return z + i / sx % sz; }
        public boolean contains(int a, int b, int c) {
            return a >= x && a < (long) x + sx && b >= y && b < (long) y + sy && c >= z && c < (long) z + sz;
        }
    }
    public void validate() {
        if (map.isBlank() || dimension.isBlank() || blocks.length != bounds.volume() || palette.isEmpty())
            throw new IllegalArgumentException("基准元数据不完整。");
        for (int id : blocks) if (id < 0 || id >= palette.size()) throw new IllegalArgumentException("基准方块索引无效。");
        for (int i = 0; i < blockEntities.size(); i++) {
            CompoundTag tag = blockEntities.m_128728_(i);
            if (!bounds.contains(tag.m_128451_("x"), tag.m_128451_("y"), tag.m_128451_("z")))
                throw new IllegalArgumentException("基准方块实体越界。");
        }
    }
    public CompoundTag tag() {
        validate();
        CompoundTag tag = new CompoundTag();
        tag.m_128405_("Version", 1); tag.m_128359_("Map", map); tag.m_128359_("Dimension", dimension);
        tag.m_128385_("Bounds", new int[]{bounds.x, bounds.y, bounds.z, bounds.sx, bounds.sy, bounds.sz});
        tag.m_128365_("Palette", palette); tag.m_128385_("Blocks", blocks);
        tag.m_128365_("BlockEntities", blockEntities); tag.m_128365_("Entities", entities);
        return tag;
    }
    public static BaselineData read(Path file) throws IOException {
        CompoundTag tag = NbtIo.m_128937_(file.toFile());
        if (tag == null || tag.m_128451_("Version") != 1) throw new IOException("基准版本不支持。");
        int[] b = tag.m_128465_("Bounds");
        if (b.length != 6) throw new IOException("基准区域无效。");
        try {
            BaselineData data = new BaselineData(tag.m_128461_("Map"), tag.m_128461_("Dimension"),
                    new Bounds(b[0], b[1], b[2], b[3], b[4], b[5]), tag.m_128437_("Palette", 10),
                    tag.m_128465_("Blocks"), tag.m_128437_("BlockEntities", 10), tag.m_128437_("Entities", 10));
            data.validate(); return data;
        } catch (RuntimeException e) { throw new IOException("基准数据损坏。", e); }
    }
    public void write(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        Path temp = Files.createTempFile(file.getParent(), "baseline-", ".tmp");
        try {
            NbtIo.m_128944_(tag(), temp.toFile());
            if (Files.exists(file)) Files.copy(file, file.resolveSibling(file.getFileName() + ".previous"), StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temp); }
    }
}
