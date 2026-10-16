package dev.minestrike.skins;

import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public final class SkinStore {
    private final Path directory;
    private final SkinCatalog catalog;
    private final Map<UUID, Map<String, String>> cache = new HashMap<>();

    public SkinStore(Path directory, SkinCatalog catalog) {
        this.directory = directory;
        this.catalog = catalog;
    }

    public Map<String, String> load(UUID uuid) {
        return new LinkedHashMap<>(cache.computeIfAbsent(uuid, key -> {
            Path file = directory.resolve(key + ".json");
            if (!Files.exists(file)) return new LinkedHashMap<>();
            try {
                Map<String, String> data = SkinCatalog.JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8),
                        new TypeToken<Map<String, String>>() {}.getType());
                return catalog.checked(data);
            } catch (Exception e) {
                throw new IllegalStateException("Cannot read player skin choices: " + file, e);
            }
        }));
    }

    public void save(UUID uuid, Map<String, String> choices) throws IOException {
        Map<String, String> checked = catalog.checked(choices);
        if (choices == null || !checked.equals(choices)) throw new IllegalArgumentException("Unknown skin selection");
        Files.createDirectories(directory);
        Path target = directory.resolve(uuid + ".json");
        Path temp = Files.createTempFile(directory, uuid + "-", ".tmp");
        try {
            Files.writeString(temp, SkinCatalog.JSON.toJson(checked), StandardCharsets.UTF_8);
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
            cache.put(uuid, new LinkedHashMap<>(checked));
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
