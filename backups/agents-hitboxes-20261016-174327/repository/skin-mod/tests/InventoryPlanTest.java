import dev.minestrike.skins.InventoryPlan;
import dev.minestrike.skins.InventoryPlan.Stack;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

public class InventoryPlanTest {
    private static Stack item(String gun, String receipt, int count, int limit) {
        CompoundTag tag = new CompoundTag();
        tag.m_128359_("id", "tacz:modern_kinetic_gun");
        tag.m_128359_("GunId", gun);
        tag.m_128359_("Receipt", receipt);
        return new Stack(tag, count, limit);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static Map<CompoundTag,Integer> totals(List<Stack> stacks) {
        Map<CompoundTag,Integer> result = new HashMap<>();
        for (Stack stack : stacks) if (stack.count() > 0) result.merge(stack.key(), stack.count(), Integer::sum);
        return result;
    }
    public static void main(String[] args) {
        Stack a = item("tacz:ak47", "one", 1, 1), b = item("tacz:glock_17", "one", 1, 1);
        var empty = List.of(InventoryPlan.EMPTY, InventoryPlan.EMPTY);
        var bundle = List.of(a, b);
        var filled = InventoryPlan.add(empty, bundle).orElseThrow();
        check(totals(filled).equals(totals(bundle)), "Every main and secondary item must be granted");
        check(empty.stream().allMatch(s -> s.count() == 0), "Purchase planning must not mutate inventory");
        check(InventoryPlan.add(List.of(InventoryPlan.EMPTY), bundle).isEmpty(), "Reject entire bundle when only main fits");
        check(InventoryPlan.remove(List.of(a, InventoryPlan.EMPTY), bundle).isEmpty(), "Missing secondary denies whole refund");
        check(InventoryPlan.remove(filled, bundle).orElseThrow().stream().allMatch(s -> s.count() == 0), "Whole refund removes all items");
        check(InventoryPlan.remove(filled, List.of(a, a)).isEmpty(), "One item cannot satisfy two entries");
        check(InventoryPlan.remove(filled, List.of(item("tacz:ak47", "other-purchase", 1, 1), b)).isEmpty(), "Other purchase cannot replace missing item");
        check(InventoryPlan.remove(filled, List.of(item("tacz:m4a1", "one", 1, 1), b)).isEmpty(), "GunId NBT distinguishes same registry item");
        Stack ammo = item("ammo", "one", 60, 64);
        var partial = List.of(ammo, InventoryPlan.EMPTY);
        var merged = InventoryPlan.add(partial, List.of(ammo.withCount(10))).orElseThrow();
        check(merged.get(0).count() == 64 && merged.get(1).count() == 6, "Respect stack size and split across slots");
        check(partial.get(0).count() == 60, "Merge is planned atomically");
        check(InventoryPlan.remove(List.of(ammo.withCount(30), ammo.withCount(30)), List.of(ammo)).isPresent(), "Refund accepts split stack with same identity");
        check(InventoryPlan.remove(List.of(ammo.withCount(59)), List.of(ammo)).isEmpty(), "Consumed item denies refund");
        Random random = new Random(27);
        for (int trial = 0; trial < 1000; trial++) {
            List<Stack> inventory = new ArrayList<>();
            for (int i = 0; i < 36; i++) inventory.add(random.nextBoolean() ? InventoryPlan.EMPTY : item("old", "old", 1 + random.nextInt(64), 64));
            List<Stack> additions = new ArrayList<>();
            for (int i = 0, n = 1 + random.nextInt(6); i < n; i++) additions.add(item("gun" + i, "purchase" + trial, 1 + random.nextInt(64), 64));
            var before = totals(inventory);
            var plan = InventoryPlan.add(inventory, additions);
            check(before.equals(totals(inventory)), "Input remains unchanged on success or failure");
            if (plan.isPresent()) {
                check(plan.get().stream().allMatch(s -> s.count() >= 0 && s.count() <= s.limit()), "No overfull slots");
                check(totals(InventoryPlan.remove(plan.get(), additions).orElseThrow()).equals(before), "Purchase/refund conserves original inventory");
            }
        }
        System.out.println("PASS: atomic bundle capacity, full refunds, NBT/receipt identity, quantities, split stacks and 1000 conservation trials");
    }
}
