package dev.minestrike.skins;

import java.util.Set;
import java.util.UUID;

public record RadarPolicy(boolean restricted, Set<UUID> teammates) {
    public RadarPolicy { teammates = restricted ? Set.copyOf(teammates) : Set.of(); }
    public boolean visible(UUID self, UUID target) {
        return !restricted || target != null && (target.equals(self) || teammates.contains(target));
    }
}
