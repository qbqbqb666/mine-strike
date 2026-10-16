import dev.minestrike.skins.SkinCatalog;
import dev.minestrike.skins.SkinStore;
import java.nio.file.*;
import java.util.*;

public class SkinDataTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        SkinCatalog catalog = SkinCatalog.JSON.fromJson(Files.readString(Path.of(args[0])), SkinCatalog.class);
        catalog.validate();
        check(catalog.groups.size() == 5, "five weapon groups");
        check(catalog.groups.stream().mapToInt(g -> g.skins.size()).sum() == 23, "all skin.txt entries");
        check(catalog.groupFor("GunId", "mcs2:cs_ak").key.equals("ak"), "base AK match");
        check(catalog.groupFor("GunId", "mcs2:cs_ak_asiimov").key.equals("ak"), "skin AK match");
        check(catalog.groupFor("MeleeWeaponId", "mcs2:t_knife").key.equals("knife"), "T default knife match");
        check(catalog.groupFor("MeleeWeaponId", "mcs2:ct_knife").key.equals("knife"), "CT default knife match");
        check(catalog.groupFor("GunId", "fallout:x26") == null, "unrelated weapon unchanged");
        check(catalog.checked(Map.of("ak", "mcs2:cs_awp_fade")).isEmpty(), "reject another weapon skin");
        check(catalog.checked(Map.of("ak", "missing:gun")).isEmpty(), "reject forged gun ID");
        Path directory = Files.createTempDirectory("mine-strike-skin-test-");
        try {
            UUID a = UUID.randomUUID(), b = UUID.randomUUID();
            SkinStore store = new SkinStore(directory, catalog);
            Map<String, String> first = Map.of("ak", "mcs2:cs_ak_asiimov", "knife", "cs2_wt:karambit");
            Map<String, String> second = Map.of("ak", "mcs2:cs_ak_fire_serpent");
            store.save(a, first); store.save(b, second);
            check(store.load(a).equals(first) && store.load(b).equals(second), "UUID isolation");
            store.load(a).clear();
            check(store.load(a).equals(first), "defensive copies");
            SkinStore reopened = new SkinStore(directory, catalog);
            check(reopened.load(a).equals(first), "choices survive restart");
            Map<String, String> locked = Map.copyOf(store.load(a));
            store.save(a, second);
            check(locked.equals(first), "match snapshot remains unchanged");
            boolean rejected = false;
            try { store.save(a, Map.of("ak", "mcs2:cs_awp_fade")); } catch (IllegalArgumentException e) { rejected = true; }
            check(rejected && store.load(a).equals(second), "invalid save cannot overwrite choices");
            store.save(a, Map.of());
            check(store.load(a).isEmpty(), "default selection reset");
            check(store.load(b).equals(second), "reset does not affect other player");
            try (var files = Files.list(directory)) {
                check(files.noneMatch(p -> p.toString().endsWith(".tmp")), "atomic write cleanup");
            }
        } finally {
            try (var files = Files.walk(directory)) {
                for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
            }
        }
        System.out.println("PASS: catalog, UUID isolation, persistence, snapshots, validation, reset and atomic writes");
    }
}
