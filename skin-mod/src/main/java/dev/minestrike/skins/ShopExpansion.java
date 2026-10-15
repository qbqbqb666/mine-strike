package dev.minestrike.skins;

import com.google.gson.*;
import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.screen.*;
import com.phasetranscrystal.fpsmatch.common.item.ShopEditTool;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.ShopMap;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import com.phasetranscrystal.fpsmatch.util.FPSMCodec;
import dev.minestrike.skins.mixin.ShopEditorAccess;
import dev.minestrike.skins.mixin.ShopSlotMenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkHooks;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

public final class ShopExpansion {
    private static final String EDIT_DATA = "MineStrikeEditorSlots", COUNTS = "MineStrikeShopCounts";
    private static final Map<FPSMShop<?>, Integer> revisions = new WeakHashMap<>();
    private static final Map<AbstractContainerMenu, Integer> menuRevisions = new WeakHashMap<>();
    public static boolean isCS(Map<?, ?> data) { return data.containsKey(ItemType.EQUIPMENT); }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void normalize(Map<?, ? extends List<ShopSlot>> data) {
        if (!isCS(data)) return;
        Map raw = data;
        raw.computeIfAbsent(ItemType.valueOf("MYSTERY"), key -> new ArrayList<ShopSlot>());
        validate(data, ShopRows.COLUMNS);
    }
    public static void validate(Map<?, ? extends List<ShopSlot>> data, int count) {
        if (count != ShopRows.COLUMNS || data.size() != count || !data.keySet().containsAll(List.of(ItemType.values())))
            throw new IllegalArgumentException("商店必须包含六个分类。");
        ShopRows.rows(data.values());
    }
    public static FPSMShop<ItemType> shop(ItemStack stack) {
        if (!(stack.m_41720_() instanceof ShopEditTool tool) || !FPSMCore.initialized()) return null;
        var found = FPSMCore.getInstance().getMapByName(tool.getTag(stack, "SelectedMap"));
        if (found.isEmpty() || !(found.get() instanceof ShopMap map)) return null;
        FPSMShop<?> shop = (FPSMShop<?>) map.getShop(tool.getTag(stack, "SelectedShop")).orElse(null);
        if (shop == null || shop.enumClass != ItemType.class) return null;
        return (FPSMShop<ItemType>) shop;
    }
    public static int[] counts(ItemStack stack) { return stack.m_41784_().m_128465_(COUNTS); }
    public static void remember(AbstractContainerMenu menu, ItemStack tool) {
        FPSMShop<?> shop = shop(tool);
        if (shop != null) menuRevisions.put(menu, revisions.getOrDefault(shop, 0));
    }
    public static boolean editable(ServerPlayer player, AbstractContainerMenu menu, ItemStack stack) {
        if (!player.m_20310_(2)) { message(player, "需要管理员权限等级 2。"); return false; }
        if (player.f_36096_ != menu) return false;
        FPSMShop<?> shop = shop(stack);
        if (shop == null) return false;
        var tool = (ShopEditTool) stack.m_41720_();
        var map = FPSMCore.getInstance().getMapByName(tool.getTag(stack, "SelectedMap")).orElse(null);
        if (map == null || map.isStart || map instanceof com.phasetranscrystal.blockoffensive.map.CSGameMap cs && MapBaseline.busy(cs)) {
            message(player, "请在比赛未开始、地图空闲时编辑商店。"); return false;
        }
        if (!Objects.equals(menuRevisions.get(menu), revisions.getOrDefault(shop, 0))) {
            message(player, "商店已被其他操作更新，请重新打开后编辑。"); return false;
        }
        return true;
    }
    public static List<ShopSlot> editorData(EditorShopContainer menu, ItemStack tool, boolean client) {
        if (!client) return flatten(Objects.requireNonNull(shop(tool)));
        String packed = tool.m_41784_().m_128461_(EDIT_DATA);
        if (packed.isEmpty()) throw new IllegalStateException("缺少商店编辑数据，请更新双方自制模组。");
        List<ShopSlot> slots = new ArrayList<>();
        for (JsonElement value : JsonParser.parseString(unpack(packed)).getAsJsonArray())
            slots.add(FPSMCodec.decodeFromJson(ShopSlot.CODEC, value));
        int[] counts = counts(tool);
        if (counts.length != ShopRows.COLUMNS || Arrays.stream(counts).anyMatch(n -> n < 0 || n > ShopRows.MAX_ROWS)
                || slots.size() != (Arrays.stream(counts).max().orElse(0) + 1) * ShopRows.COLUMNS)
            throw new IllegalArgumentException("商店编辑数据尺寸无效。");
        return slots;
    }
    private static List<ShopSlot> flatten(FPSMShop<ItemType> shop) {
        List<ShopSlot> slots = new ArrayList<>();
        for (int row = 0; row <= ShopRows.rows(shop.getDefaultShopDataMap().values()); row++)
            for (ItemType type : ItemType.values()) {
                var column = shop.getDefaultShopDataMap().get(type);
                slots.add(row < column.size() ? copyForEdit(column.get(row)) : empty());
            }
        return slots;
    }
    private static ShopSlot copyForEdit(ShopSlot original) {
        ShopSlot slot = new ShopSlot(original.process().m_41777_(), original.getDefaultCost(), original.getMaxBuyCount(), original.getGroupId());
        slot.setIndex(original.getIndex());
        for (String name : original.getListenerNames()) slot.addListener(FPSMCore.getInstance().getListenerModuleManager().getListenerModule(name));
        return slot;
    }
    private static ShopSlot empty() { return new ShopSlot(ItemStack.f_41583_, 0); }
    // Opening data must be ready before NetworkHooks serializes the tool.
    public static void prepareTool(ItemStack stack) {
        FPSMShop<ItemType> shop = shop(stack);
        if (shop == null) return;
        stack.m_41784_().m_128473_("ShopItems");
        JsonArray json = new JsonArray();
        flatten(shop).forEach(slot -> json.add(FPSMCodec.encodeToJson(ShopSlot.CODEC, slot)));
        stack.m_41784_().m_128385_(COUNTS, Arrays.stream(ItemType.values())
                .mapToInt(type -> shop.getDefaultShopDataMap().get(type).size()).toArray());
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) { gzip.write(json.toString().getBytes(StandardCharsets.UTF_8)); }
            stack.m_41784_().m_128359_(EDIT_DATA, Base64.getEncoder().encodeToString(bytes.toByteArray()));
        } catch (IOException e) { throw new IllegalStateException("无法同步商店编辑数据", e); }
    }
    private static String unpack(String data) {
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(data)))) {
            return new String(gzip.readNBytes(2 * 1024 * 1024), StandardCharsets.UTF_8);
        } catch (IOException e) { throw new IllegalArgumentException("商店编辑数据损坏", e); }
    }
    public static void sendShape(FPSMShop<?> shop, ServerPlayer player) {
        if (shop.enumClass != ItemType.class) return;
        var map = MineStrikeSkins.map(player);
        if (map == null || map.getShop(player).orElse(null) != shop) return;
        int[] counts = Arrays.stream(ItemType.values()).mapToInt(type -> {
            List<?> list = (List<?>) shop.getDefaultShopDataMap().get(type); return list == null ? 0 : list.size();
        }).toArray();
        SkinNetwork.sendShopShape(player, counts);
        ShopBundles.sendView(shop, player);
    }
    private static void changed(ItemStack stack) {
        FPSMShop<?> shop = Objects.requireNonNull(shop(stack));
        revisions.merge(shop, 1, Integer::sum);
        shop.resetPlayerData();
        var tool = (ShopEditTool) stack.m_41720_();
        var map = FPSMCore.getInstance().getMapByName(tool.getTag(stack, "SelectedMap")).orElseThrow();
        FPSMCore.getInstance().getFPSMDataManager().saveData(map, map.getMapName());
    }
    public static void openEditor(ServerPlayer player, ItemStack stack) {
        prepareTool(stack);
        NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inv, p) -> new EditorShopContainer(id, inv, stack),
                Component.m_237113_("商店管理")), buffer -> buffer.m_130055_(stack));
    }
    public static void action(ServerPlayer player, SkinNetwork.ShopEditAction action) {
        if (!(player.f_36096_ instanceof EditorShopContainer menu) || menu.f_38840_ != action.menu()) return;
        ItemStack stack = ((ShopEditorAccess) menu).mineStrikeTool();
        if (!editable(player, menu, stack) || action.column() < 0 || action.column() >= ShopRows.COLUMNS) return;
        FPSMShop<ItemType> shop = shop(stack);
        var list = shop.getDefaultShopDataMap().get(ItemType.values()[action.column()]);
        int row = action.row();
        if (action.operation().equals("add") && row == list.size() && row < ShopRows.MAX_ROWS) {
            ShopSlot slot = empty();
            ShopRows.add(list, slot);
            changed(stack);
            prepareTool(stack);
            NetworkHooks.openScreen(player, new SimpleMenuProvider((id, inv, p) -> new EditShopSlotMenu(id, inv, copyForEdit(slot), stack,
                    ShopRows.index(row, action.column())), Component.m_237113_("编辑商品")), buffer -> {
                buffer.m_130070_(FPSMCodec.encodeToJson(ShopSlot.CODEC, slot).toString());
                buffer.m_130055_(stack); buffer.writeInt(ShopRows.index(row, action.column()));
            });
        } else if (action.operation().equals("delete") && row >= 0 && row < list.size()) {
            ShopRows.remove(list, row); changed(stack); openEditor(player, stack);
        } else if (action.operation().equals("edit") && row >= 0 && row < list.size()) {
            ((ShopEditorAccess) menu).mineStrikeOpenSlot(player, copyForEdit(list.get(row)), ShopRows.index(row, action.column()));
        }
    }
    public static void save(ServerPlayer player, SkinNetwork.ShopSave packet) {
        if (!(player.f_36096_ instanceof EditShopSlotMenu menu) || menu.f_38840_ != packet.menu()) return;
        ItemStack tool = menu.getGuiItemStack();
        if (!editable(player, menu, tool)) return;
        if (packet.price() < 0 || packet.price() > 16000 || packet.ammo() < 0 || packet.ammo() > 1000000
                || packet.maxBuy() < 1 || packet.maxBuy() > 64 || packet.group() < -1 || packet.name().length() > 80) return;
        var access = (ShopSlotMenuAccess) menu;
        List<ItemStack> draft = ShopBundles.items(menu).stream().map(ItemStack::m_41777_).collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        var stack = draft.isEmpty() ? ItemStack.f_41583_ : draft.get(0);
        if (stack.m_41619_()) { message(player, "请先从下方背包放入商品。"); return; }
        if (packet.name().isBlank()) stack.m_41787_(); else if (!packet.name().strip().equals(stack.m_41611_().getString())) stack.m_41714_(Component.m_237113_(packet.name().strip()));
        access.mineStrikeHandler().setStackInSlot(0, stack);
        ShopSlot slot = copyForEdit(access.mineStrikeSlot());
        slot.setMaxBuyCount(packet.maxBuy());
        if (packet.ammoChanged() && stack.m_41720_() instanceof com.tacz.guns.api.item.IGun gun)
            com.phasetranscrystal.fpsmatch.util.FPSMUtil.setTotalDummyAmmo(stack, gun, packet.ammo());
        draft.set(0, stack);
        ItemStack bundle = ShopBundles.pack(draft);
        slot.setItemSupplier(() -> bundle.m_41777_());
        slot.setDefaultCost(packet.price()); slot.setGroupId(packet.group());
        int index = access.mineStrikeIndex();
        shop(tool).replaceDefaultShopData(ItemType.values()[ShopRows.column(index)].name(), ShopRows.row(index), slot);
        changed(tool);
        message(player, "商品已保存。");
        openEditor(player, tool);
    }
    public static void pick(ServerPlayer player, int menuId, int inventoryIndex) {
        if (!(player.f_36096_ instanceof EditShopSlotMenu menu) || menu.f_38840_ != menuId
                || inventoryIndex < 0 || inventoryIndex >= 36 || !editable(player, menu, menu.getGuiItemStack())) return;
        ItemStack item = ShopBundles.clean(player.m_150109_().m_8020_(inventoryIndex));
        if (item.m_41619_()) { message(player, "该背包格已为空，请重新选择。"); return; }
        List<ItemStack> items = ShopBundles.items(menu);
        if (items.size() >= ShopBundles.MAX_ITEMS) { message(player, "一个商品最多包含 " + ShopBundles.MAX_ITEMS + " 项物品。"); return; }
        items.add(item);
        ShopBundles.syncEditor(player, menu);
    }
    public static void removeItem(ServerPlayer player, int menuId, int index) {
        if (!(player.f_36096_ instanceof EditShopSlotMenu menu) || menu.f_38840_ != menuId
                || !editable(player, menu, menu.getGuiItemStack())) return;
        List<ItemStack> items = ShopBundles.items(menu);
        if (index < 0 || index >= items.size()) return;
        items.remove(index);
        ShopBundles.syncEditor(player, menu);
    }
    public static void roundOnly(ServerPlayer player, int menuId, int index, boolean enabled) {
        if (!(player.f_36096_ instanceof EditShopSlotMenu menu) || menu.f_38840_ != menuId
                || !editable(player, menu, menu.getGuiItemStack())) return;
        List<ItemStack> items = ShopBundles.items(menu);
        if (index < 0 || index >= items.size()) return;
        RoundItems.configure(items.get(index).m_41784_(), enabled);
        ShopBundles.syncEditor(player, menu);
    }
    public static void deployable(ServerPlayer player, int menuId, int index, boolean enabled) {
        if (!(player.f_36096_ instanceof EditShopSlotMenu menu) || menu.f_38840_ != menuId
                || !editable(player, menu, menu.getGuiItemStack())) return;
        List<ItemStack> items = ShopBundles.items(menu);
        if (index < 0 || index >= items.size() || !(items.get(index).m_41720_() instanceof net.minecraft.world.item.BlockItem)) return;
        ShopDeployments.configure(items.get(index).m_41784_(), enabled);
        ShopBundles.syncEditor(player, menu);
    }
    public static void back(ServerPlayer player, int menuId) {
        if (player.f_36096_ instanceof EditShopSlotMenu menu && menu.f_38840_ == menuId && player.m_20310_(2))
            openEditor(player, menu.getGuiItemStack());
    }
    private static void message(ServerPlayer player, String text) {
        player.m_5661_(Component.m_237113_(text), false);
        SkinNetwork.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                new SkinNetwork.ShopFeedback(player.f_36096_.f_38840_, text));
    }
}
