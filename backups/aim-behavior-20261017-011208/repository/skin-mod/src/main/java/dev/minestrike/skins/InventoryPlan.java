package dev.minestrike.skins;

import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Plans a complete inventory change before any live slot is modified. */
public final class InventoryPlan {
    public record Stack(CompoundTag key, int count, int limit) {
        public Stack withCount(int value) { return new Stack(key, value, limit); }
    }
    public static final Stack EMPTY = new Stack(new CompoundTag(), 0, 64);

    public static Optional<List<Stack>> add(List<Stack> inventory, List<Stack> additions) {
        List<Stack> result = new ArrayList<>(inventory);
        for (Stack item : additions) {
            int left = item.count();
            for (int i = 0; i < result.size() && left > 0; i++) {
                Stack current = result.get(i);
                if (current.count() == 0 || !current.key().equals(item.key())) continue;
                int amount = Math.min(left, Math.max(0, Math.min(current.limit(), item.limit()) - current.count()));
                result.set(i, current.withCount(current.count() + amount));
                left -= amount;
            }
            for (int i = 0; i < result.size() && left > 0; i++) {
                if (result.get(i).count() != 0) continue;
                int amount = Math.min(left, item.limit());
                result.set(i, item.withCount(amount));
                left -= amount;
            }
            if (left > 0) return Optional.empty();
        }
        return Optional.of(result);
    }
    public static Optional<List<Stack>> remove(List<Stack> inventory, List<Stack> removals) {
        List<Stack> result = new ArrayList<>(inventory);
        for (Stack item : removals) {
            int left = item.count();
            for (int i = 0; i < result.size() && left > 0; i++) {
                Stack current = result.get(i);
                if (current.count() == 0 || !current.key().equals(item.key())) continue;
                int amount = Math.min(left, current.count());
                result.set(i, current.withCount(current.count() - amount));
                left -= amount;
            }
            if (left > 0) return Optional.empty();
        }
        return Optional.of(result);
    }
}
