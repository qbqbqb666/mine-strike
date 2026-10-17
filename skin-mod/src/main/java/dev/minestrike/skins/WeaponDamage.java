package dev.minestrike.skins;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class WeaponDamage {
    public record Value(double original, double target) {}
    private static final Map<String,Value> VALUES = values();
    private static Map<String,Value> values() {
        Map<String,Value> result = new LinkedHashMap<>();
        result.put("mcs2:cs_glock", new Value(4.5,7));
        result.put("suffuse:qbu191", new Value(5,8));
        result.put("superbwarfare:taser", new Value(5,20));
        result.put("mcs2:cs_ak", new Value(5.5,6));
        result.put("ronmc:p90", new Value(5.5,5));
        result.put("suffuse:pp19", new Value(6.5,6.5));
        result.put("ronmc:g36c", new Value(7,7));
        result.put("daffas_arsenal:makten_single", new Value(7,5));
        result.put("mcs2:cs_deagle", new Value(7.2,10));
        result.put("suffuse:tec9", new Value(8,9));
        result.put("tacz:minigun", new Value(8,4));
        result.put("tacz:p320", new Value(10,6));
        result.put("daffas_arsenal:ssg69", new Value(18,19));
        result.put("suffuse:python", new Value(20,20));
        result.put("ronmc:m1014", new Value(36,36));
        result.put("tacz:m870", new Value(36,36));
        result.put("mcs2:cs_awp", new Value(40,40));
        result.put("superbwarfare:bocek", new Value(48,10));
        result.put("superbwarfare:rpg", new Value(300,300));
        result.put("mcs2:cs_m4a4", new Value(4.6,6));
        result.put("mcs2:cs_usp", new Value(5.2,5.2));
        result.put("ronmc:57usg", new Value(5.5,6));
        result.put("daffas_arsenal:harpa9", new Value(6,6));
        result.put("rainforest:famas", new Value(6,6));
        result.put("maxstuff:hk_mp5sd", new Value(6,6));
        result.put("mcs2:cs_m4a1s", new Value(6,6));
        result.put("maxstuff:aug_a2", new Value(6.5,6.5));
        // Superb stores ItemStack.getDescriptionId(), rather than the registry ID, on its projectiles.
        for (String name : List.of("bocek","taser","rpg"))
            result.put("item.superbwarfare."+name,result.get("superbwarfare:"+name));
        try (var in = WeaponDamage.class.getResourceAsStream("/default-skins.json")) {
            if (in == null) throw new IllegalStateException("Missing bundled skin catalog");
            var catalog = SkinCatalog.JSON.fromJson(new InputStreamReader(in,StandardCharsets.UTF_8),SkinCatalog.class);
            for (var group : catalog.groups) if ("GunId".equals(group.tag)) {
                Value value = result.get(group.baseIds.get(0));
                if (value != null) for (var skin : group.skins) result.put(skin.id,value);
            }
        } catch (java.io.IOException e) { throw new IllegalStateException("Cannot read bundled skin damage aliases",e); }
        return Map.copyOf(result);
    }
    public static Value value(String id) { return id == null ? null : VALUES.get(id); }
    public static float scale(String id, float original) {
        Value v = value(id);
        return v == null ? original : (float)(original * (v.target()/v.original()));
    }
    private WeaponDamage() {}
}
