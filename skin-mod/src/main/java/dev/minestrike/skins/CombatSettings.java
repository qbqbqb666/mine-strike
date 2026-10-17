package dev.minestrike.skins;

import com.google.gson.Gson;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public record CombatSettings(double health, List<Double> armor, List<Double> parts) {
    public static final Gson JSON = new Gson();
    public static CombatSettings defaults() {
        return new CombatSettings(20, Arrays.asList(null,null,null,null), List.of(4.0,1.0,1.0,1.0));
    }
    public CombatSettings {
        if (!Double.isFinite(health) || health < 1 || health > 1024) throw new IllegalArgumentException("生命值范围为 1–1024");
        if (armor == null || armor.size()!=4 || parts == null || parts.size()!=4) throw new IllegalArgumentException("需要四个护甲槽和四个部位");
        armor = Collections.unmodifiableList(new ArrayList<>(armor)); parts = List.copyOf(parts);
        for (Double n : armor) if (n!=null && (!Double.isFinite(n) || n<0 || n>30)) throw new IllegalArgumentException("每槽护甲范围为 0–30，留空沿用装备");
        for (Double n : parts) if (n==null || !Double.isFinite(n) || n<0 || n>100 || Math.abs(n*10-Math.rint(n*10))>1e-7) throw new IllegalArgumentException("倍率范围为 0.0–100.0，仅保留一位小数");
    }
    public double multiplier(int region) { return parts.get(region==1?0:region==3?2:region==2?3:1); }
    public static Path file(Path world, String dimension, String map) {
        String key=Base64.getUrlEncoder().withoutPadding().encodeToString((dimension+"\n"+map).getBytes(StandardCharsets.UTF_8));
        return world.resolve("mine-strike-data/combat-settings").resolve(key+".json");
    }
    public static CombatSettings load(Path file) throws java.io.IOException {
        return Files.exists(file) ? JSON.fromJson(Files.readString(file),CombatSettings.class) : null;
    }
    public void save(Path file) throws java.io.IOException {
        Files.createDirectories(file.getParent());
        Path temporary=Files.createTempFile(file.getParent(),"combat-",".tmp");
        try {
            Files.writeString(temporary,JSON.toJson(this),StandardCharsets.UTF_8);
            try { Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
}
