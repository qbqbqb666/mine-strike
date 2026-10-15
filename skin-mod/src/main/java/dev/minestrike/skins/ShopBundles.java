package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import dev.minestrike.skins.mixin.ShopSlotMenuAccess;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

public final class ShopBundles {
    public static final int MAX_ITEMS = 36;
    private static final String ITEMS = "MineStrikeBundleItems", RECEIPT = "MineStrikeBundleReceipt", PART = "MineStrikeBundlePart";
    public interface EditorState { List<ItemStack> mineStrikeItems(); }
    public interface SlotState { List<Receipt> mineStrikeReceipts(); }
    public record Receipt(List<InventoryPlan.Stack> items) {}
    public record View(int column, int row, int count, boolean refundable) {}

    public static boolean isBundle(ShopSlot slot) {
        CompoundTag tag = slot.process().m_41783_();
        return tag != null && !tag.m_128437_(ITEMS, 10).isEmpty();
    }
    public static ItemStack clean(ItemStack source) {
        ItemStack item = source.m_41777_();
        CompoundTag tag = item.m_41783_();
        if (tag != null) {
            tag.m_128473_(ITEMS); tag.m_128473_(RECEIPT); tag.m_128473_(PART); tag.m_128473_(RoundItems.EPOCH); tag.m_128473_(ShopDeployments.ISSUED);
            if (tag.m_128456_()) item.m_41751_(null);
        }
        return item;
    }
    public static List<ItemStack> unpack(ItemStack source) {
        List<ItemStack> items = new ArrayList<>();
        if (source.m_41619_()) return items;
        items.add(clean(source));
        CompoundTag tag = source.m_41783_();
        if (tag != null) {
            ListTag extras = tag.m_128437_(ITEMS, 10);
            if (extras.size() >= MAX_ITEMS) throw new IllegalArgumentException("一个商品最多包含 " + MAX_ITEMS + " 项物品。");
            for (int i = 0; i < extras.size(); i++) {
                ItemStack item = clean(ItemStack.m_41712_(extras.m_128728_(i)));
                if (item.m_41619_()) throw new IllegalArgumentException("套餐副物品不存在，请检查模组和配置。");
                items.add(item);
            }
        }
        return items;
    }
    public static ItemStack pack(List<ItemStack> items) {
        if (items.isEmpty() || items.size() > MAX_ITEMS) throw new IllegalArgumentException("商品物品数量无效。");
        ItemStack main = clean(items.get(0));
        if (main.m_41619_()) throw new IllegalArgumentException("主物品不能为空。");
        if (items.size() > 1) {
            ListTag extras = new ListTag();
            for (int i = 1; i < items.size(); i++) {
                ItemStack item = clean(items.get(i));
                if (item.m_41619_()) throw new IllegalArgumentException("副物品不能为空。");
                extras.add(item.m_41739_(new CompoundTag()));
            }
            main.m_41784_().m_128365_(ITEMS, extras);
        }
        return main;
    }
    public static void applyExtraSkins(ItemStack item, Map<String, String> choices) {
        CompoundTag tag = item.m_41783_();
        if (tag == null || tag.m_128437_(ITEMS, 10).isEmpty()) return;
        List<ItemStack> items = unpack(item);
        for (int i = 1; i < items.size(); i++) items.set(i, MineStrikeSkins.apply(items.get(i), choices));
        tag.m_128365_(ITEMS, pack(items).m_41784_().m_128437_(ITEMS, 10));
    }
    public static List<ItemStack> items(EditShopSlotMenu menu) { return ((EditorState) menu).mineStrikeItems(); }
    public static void updateEditor(EditShopSlotMenu menu) {
        List<ItemStack> items = items(menu);
        ((ShopSlotMenuAccess) menu).mineStrikeHandler().setStackInSlot(0, items.isEmpty() ? ItemStack.f_41583_ : items.get(0).m_41777_());
    }
    public static void syncEditor(ServerPlayer player, EditShopSlotMenu menu) {
        updateEditor(menu);
        menu.m_38946_();
        SkinNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SkinNetwork.ShopBundleState(menu.f_38840_, items(menu).stream().map(ItemStack::m_41777_).toList()));
    }
    private static InventoryPlan.Stack state(ItemStack item) {
        if (item.m_41619_()) return InventoryPlan.EMPTY;
        CompoundTag key = item.m_41739_(new CompoundTag());
        key.m_128473_("Count");
        return new InventoryPlan.Stack(key, item.m_41613_(), item.m_41741_());
    }
    private static ItemStack stack(InventoryPlan.Stack state) {
        if (state.count() == 0) return ItemStack.f_41583_;
        CompoundTag tag = state.key().m_6426_();
        tag.m_128344_("Count", (byte) state.count());
        return ItemStack.m_41712_(tag);
    }
    private static List<InventoryPlan.Stack> inventory(Player player, int size) {
        List<InventoryPlan.Stack> result = new ArrayList<>();
        for (int i = 0; i < size; i++) result.add(state(player.m_150109_().m_8020_(i)));
        return result;
    }
    private static void commit(Player player, List<InventoryPlan.Stack> plan) {
        List<ItemStack> stacks = plan.stream().map(ShopBundles::stack).toList();
        for (int i = 0; i < stacks.size(); i++) player.m_150109_().m_6836_(i, stacks.get(i));
        player.m_150109_().m_6596_();
        player.f_36095_.m_38946_();
    }
    private static List<InventoryPlan.Stack> grant(ShopSlot slot, ServerPlayer player) {
        List<ItemStack> items = unpack(slot.process());
        String receipt = UUID.randomUUID().toString();
        List<InventoryPlan.Stack> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemStack item = RoundItems.issue(items.get(i), player);
            item.m_41784_().m_128359_(RECEIPT, receipt);
            item.m_41784_().m_128405_(PART, i);
            result.add(state(item));
        }
        return result;
    }
    public static boolean canFit(ShopSlot slot, ServerPlayer player) {
        return InventoryPlan.add(inventory(player, 36), grant(slot, player)).isPresent();
    }
    public static boolean buy(ShopSlot slot, ServerPlayer player) {
        List<InventoryPlan.Stack> additions = grant(slot, player);
        var plan = InventoryPlan.add(inventory(player, 36), additions);
        if (plan.isEmpty()) { full(player); return false; }
        commit(player, plan.get());
        ((SlotState) slot).mineStrikeReceipts().add(new Receipt(additions));
        return true;
    }
    public static void full(ServerPlayer player) { player.m_5661_(Component.m_237113_("背包空间不足，未购买套餐。"), false); }
    public static boolean canReturn(ShopSlot slot, Player player) {
        if (slot.getBoughtCount() <= 0 || slot.isLocked()) return false;
        var inventory = inventory(player, player.m_150109_().m_6643_());
        return ((SlotState) slot).mineStrikeReceipts().stream().anyMatch(receipt -> InventoryPlan.remove(inventory, receipt.items()).isPresent());
    }
    public static int returnItems(ShopSlot slot, Player player, int count) {
        int needed = Math.min(count, slot.getBoughtCount());
        if (needed <= 0 || slot.isLocked()) return 0;
        var remaining = inventory(player, player.m_150109_().m_6643_());
        List<Receipt> receipts = ((SlotState) slot).mineStrikeReceipts();
        List<Receipt> selected = new ArrayList<>();
        for (Receipt receipt : receipts) {
            var plan = InventoryPlan.remove(remaining, receipt.items());
            if (plan.isEmpty()) continue;
            selected.add(receipt); remaining = plan.get();
            if (selected.size() == needed) break;
        }
        if (selected.size() != needed) return 0;
        commit(player, remaining);
        receipts.removeAll(selected);
        return needed;
    }
    public static void sendView(FPSMShop<?> shop, ServerPlayer player) {
        List<View> views = new ArrayList<>();
        var data = shop.getPlayerShopData(player.m_20148_()).getData();
        for (var entry : data.entrySet()) {
            if (!(entry.getKey() instanceof ItemType type)) continue;
            var slots = entry.getValue();
            for (int i = 0; i < slots.size(); i++) {
                ShopSlot slot = slots.get(i);
                if (isBundle(slot)) views.add(new View(type.ordinal(), i, unpack(slot.process()).size(), canReturn(slot, player)));
            }
        }
        SkinNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SkinNetwork.ShopBundleView(SkinCatalog.JSON.toJson(views)));
    }
}
