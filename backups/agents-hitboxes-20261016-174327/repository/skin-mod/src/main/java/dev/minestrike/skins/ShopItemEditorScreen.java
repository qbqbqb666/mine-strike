package dev.minestrike.skins;

import com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu;
import dev.minestrike.skins.mixin.ShopSlotMenuAccess;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.api.item.IGun;
import java.util.List;

public final class ShopItemEditorScreen extends ShopPanelScreen implements MenuAccess<EditShopSlotMenu> {
    private static final int ITEM_COLUMNS = 4, ITEM_CELL = 58, ITEM_ROW = 82, VISIBLE_ROWS = 2;
    private final EditShopSlotMenu menu;
    private final Inventory inventory;
    private EditBox name, price, ammo, group, maxBuy;
    private Button save;
    private String error = "", initialAmmo = "";
    private boolean pending, mainChanged, adding;
    private int offset;

    public ShopItemEditorScreen(EditShopSlotMenu menu, Inventory inventory, Component title) {
        super("编辑商品", 540, 350); this.menu = menu; this.inventory = inventory;
    }
    @Override public EditShopSlotMenu m_6262_() { return menu; }
    private List<ItemStack> items() { return ShopBundles.items(menu); }
    private ItemStack item() { return items().isEmpty() ? ItemStack.f_41583_ : items().get(0); }
    private EditBox field(String label, int x, int y, int width, String value, boolean numeric) {
        EditBox field = new EditBox(f_96547_, left + x, top + y, width, 20, Component.m_237113_(label));
        field.m_94199_(numeric ? 8 : 80);
        if (numeric) field.m_94153_(s -> s.matches("-?\\d*"));
        field.m_94144_(value); m_142416_(field); return field;
    }
    @Override protected void m_7856_() {
        super.m_7856_();
        ct = menu.getGuiItemStack().m_41784_().m_128461_("SelectedShop").equals("ct");
        var access = (ShopSlotMenuAccess) menu;
        String[] old = name == null ? null : new String[]{name.m_94155_(), price.m_94155_(), ammo.m_94155_(), group.m_94155_(), maxBuy.m_94155_()};
        name = field("商品名称", 300, 60, 220, old == null ? (item().m_41619_() ? "" : item().m_41611_().getString()) : old[0], false);
        price = field("价格", 300, 108, 104, old == null ? "" + menu.getPrice() : old[1], true);
        ammo = field("虚拟弹药", 416, 108, 104, old == null ? "" + menu.getAmmo() : old[2], true);
        if (old == null) initialAmmo = ammo.m_94155_();
        group = field("组 ID", 300, 156, 104, old == null ? "" + menu.getGroupId() : old[3], true);
        maxBuy = field("购买上限", 416, 156, 104, old == null ? "" + access.mineStrikeSlot().getMaxBuyCount() : old[4], true);
        m_142416_(ShopVisuals.button(left + 434, top + 42, 86, 14, "恢复默认名称", ct, b -> name.m_94144_("")));
        m_142416_(ShopVisuals.button(left + 300, top + 286, 104, 26, "返回商店", ct, b -> back()));
        save = m_142416_(ShopVisuals.button(left + 416, top + 286, 104, 26, "保存商品", ct, b -> save()));
    }
    public void feedback(String message) { pending = false; mainChanged = false; adding = false; save.f_93623_ = true; error = message; }
    public void receiveItems(List<ItemStack> received) {
        List<ItemStack> items = items();
        items.clear(); received.forEach(stack -> items.add(stack.m_41777_()));
        ShopBundles.updateEditor(menu);
        if (mainChanged) {
            ItemStack main = item();
            name.m_94144_(main.m_41619_() ? "" : main.m_41611_().getString());
            ammo.m_94144_("" + (main.m_41720_() instanceof IGun gun ? gun.getMaxDummyAmmoAmount(main) : 0));
            initialAmmo = ammo.m_94155_();
        }
        int max = Math.max(0, (items.size() + ITEM_COLUMNS - 1) / ITEM_COLUMNS - VISIBLE_ROWS);
        offset = mainChanged ? 0 : adding ? max : Math.min(offset, max);
        pending = false; mainChanged = false; adding = false; error = "";
    }
    private void save() {
        if (pending) return;
        try {
            int p = Integer.parseInt(price.m_94155_()), a = Integer.parseInt(ammo.m_94155_()), g = Integer.parseInt(group.m_94155_()), m = Integer.parseInt(maxBuy.m_94155_());
            if (p < 0 || p > 16000 || a < 0 || a > 1000000 || g < -1 || m < 1 || m > 64) { error = "检查数值：价格 0–16000，上限 1–64"; return; }
            if (items().isEmpty()) { error = "请先点击背包中的物品"; return; }
            pending = true; save.f_93623_ = false;
            SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopSave(menu.f_38840_, name.m_94155_(), p, a, g, m, !ammo.m_94155_().equals(initialAmmo)));
        } catch (NumberFormatException e) { error = "请完整填写数字"; }
    }
    private void back() { if (!pending) { pending = true; SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopBack(menu.f_38840_)); } }
    @Override public void m_7379_() { back(); }
    @Override protected void drawPanel(GuiGraphics g, int mx, int my, float tick) {
        ShopVisuals.panel(g, left, top, panelWidth, panelHeight, ct);
        ShopVisuals.heading(g, (ct ? "CT" : "T") + " / 编辑商品", left + 20, top + 18, ShopVisuals.accent(ct));
        List<ItemStack> items = items();
        if (items.isEmpty()) ShopVisuals.centerText(g, "+ 从背包添加", left + 132, top + 105, ShopVisuals.MUTED, 1.1f);
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) for (int col = 0; col < ITEM_COLUMNS; col++) {
            int index = (offset + visible) * ITEM_COLUMNS + col;
            if (index >= items.size()) continue;
            int x = left + 20 + col * ITEM_CELL, y = top + 44 + visible * ITEM_ROW;
            boolean hover = mx >= x && mx < x + 54 && my >= y && my < y + 78;
            ShopVisuals.card(g, x, y, 54, 78, ct, hover || index == 0);
            ShopVisuals.text(g, index == 0 ? "主" : "副", x + 4, y + 4, index == 0 ? ShopVisuals.accent(ct) : ShopVisuals.MUTED);
            ShopVisuals.icon(g, items.get(index), x + 7, y + 14, 39, 24);
            if (items.get(index).m_41613_() > 1) ShopVisuals.text(g, "×" + items.get(index).m_41613_(), x + 4, y + 33, ShopVisuals.TEXT);
            ShopVisuals.deleteButton(g, x + 35, y + 3, mx >= x + 35 && mx < x + 51 && my >= y + 3 && my < y + 20);
            boolean only = RoundItems.enabled(items.get(index));
            g.m_280509_(x + 3, y + 44, x + 51, y + 57, only ? 0x805D7751 : 0x603C4147);
            ShopVisuals.text(g, only ? "仅本小局" : "原有规则", x + 5, y + 46, only ? ShopVisuals.accent(ct) : ShopVisuals.MUTED);
            if (items.get(index).m_41720_() instanceof net.minecraft.world.item.BlockItem) {
                boolean deploy = ShopDeployments.enabled(items.get(index));
                g.m_280509_(x + 3, y + 60, x + 51, y + 73, deploy ? 0x805D7751 : 0x603C4147);
                ShopVisuals.text(g, deploy ? "允许部署" : "禁止部署", x + 5, y + 62, deploy ? ShopVisuals.accent(ct) : ShopVisuals.MUTED);
            }
            if (hover) ShopVisuals.fitText(g, items.get(index).m_41611_().getString(), left + 20, top + 313, 248, ShopVisuals.MUTED);
        }
        if ((items.size() + ITEM_COLUMNS - 1) / ITEM_COLUMNS > VISIBLE_ROWS)
            ShopVisuals.text(g, "↕", left + 263, top + 111, ShopVisuals.accent(ct));
        ShopVisuals.text(g, "背包", left + 20, top + 208, ShopVisuals.MUTED);
        for (int r = 0; r < 4; r++) for (int c = 0; c < 9; c++) {
            int x = left + 20 + c * 24, y = top + 224 + r * 20 + (r == 3 ? 5 : 0), index = r == 3 ? c : 9 + r * 9 + c;
            boolean hover = mx >= x && mx < x + 18 && my >= y && my < y + 18;
            ShopVisuals.card(g, x - 1, y - 1, 20, 19, ct, hover);
            g.m_280480_(inventory.m_8020_(index), x, y);
            if (inventory.m_8020_(index).m_41613_() > 1) g.m_280370_(f_96547_, inventory.m_8020_(index), x, y);
            if (hover && !inventory.m_8020_(index).m_41619_()) ShopVisuals.fitText(g, inventory.m_8020_(index).m_41611_().getString(), left + 20, top + 313, 248, ShopVisuals.MUTED);
        }
        String[] labels = {"商品名称", "套餐价格", "主物品弹药", "组 ID", "购买上限"};
        int[] xs = {300, 300, 416, 300, 416}, ys = {46, 94, 94, 142, 142};
        for (int i = 0; i < labels.length; i++) ShopVisuals.text(g, labels[i], left + xs[i], top + ys[i], ShopVisuals.MUTED);
        ShopVisuals.text(g, "监听模块", left + 300, top + 192, ShopVisuals.MUTED);
        ShopVisuals.fitText(g, menu.getListeners(), left + 300, top + 208, 220, ShopVisuals.TEXT);
        if (!error.isEmpty()) ShopVisuals.fitText(g, error, left + 20, top + 334, 500, 0xFFFF9B8C);
    }
    @Override protected boolean panelClick(double x, double y, int button) {
        if (button != 0 || pending) return false;
        List<ItemStack> items = items();
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) for (int col = 0; col < ITEM_COLUMNS; col++) {
            int index = (offset + visible) * ITEM_COLUMNS + col;
            int sx = left + 20 + col * ITEM_CELL, sy = top + 44 + visible * ITEM_ROW;
            if (index < items.size() && x >= sx + 35 && x < sx + 51 && y >= sy + 3 && y < sy + 20) {
                mainChanged = index == 0; pending = true;
                SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopRemoveItem(menu.f_38840_, index)); return true;
            }
        }
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) for (int col = 0; col < ITEM_COLUMNS; col++) {
            int index = (offset + visible) * ITEM_COLUMNS + col;
            int sx = left + 20 + col * ITEM_CELL, sy = top + 44 + visible * ITEM_ROW;
            if (index < items.size() && x >= sx + 3 && x < sx + 51 && y >= sy + 44 && y < sy + 57) {
                pending = true;
                SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopRoundOnly(menu.f_38840_, index, !RoundItems.enabled(items.get(index)))); return true;
            }
        }
        for (int visible = 0; visible < VISIBLE_ROWS; visible++) for (int col = 0; col < ITEM_COLUMNS; col++) {
            int index = (offset + visible) * ITEM_COLUMNS + col;
            int sx = left + 20 + col * ITEM_CELL, sy = top + 44 + visible * ITEM_ROW;
            if (index < items.size() && items.get(index).m_41720_() instanceof net.minecraft.world.item.BlockItem
                    && x >= sx + 3 && x < sx + 51 && y >= sy + 60 && y < sy + 73) {
                pending = true;
                SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopDeployable(menu.f_38840_, index, !ShopDeployments.enabled(items.get(index)))); return true;
            }
        }
        for (int r = 0; r < 4; r++) for (int c = 0; c < 9; c++) {
            int sx = left + 20 + c * 24, sy = top + 224 + r * 20 + (r == 3 ? 5 : 0), index = r == 3 ? c : 9 + r * 9 + c;
            if (x >= sx && x < sx + 18 && y >= sy && y < sy + 18) {
                if (inventory.m_8020_(index).m_41619_()) return true;
                if (items.size() >= ShopBundles.MAX_ITEMS) { error = "一个商品最多包含 " + ShopBundles.MAX_ITEMS + " 项物品"; return true; }
                mainChanged = items.isEmpty(); adding = true; pending = true;
                SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopPick(menu.f_38840_, index)); return true;
            }
        }
        return false;
    }
    @Override public boolean m_6050_(double x, double y, double delta) {
        if (x / scale < left + 20 || x / scale >= left + 275 || y / scale < top + 44 || y / scale >= top + 208) return false;
        int max = Math.max(0, (items().size() + ITEM_COLUMNS - 1) / ITEM_COLUMNS - VISIBLE_ROWS);
        if (!pending) offset = Math.max(0, Math.min(max, offset + (delta < 0 ? 1 : -1)));
        return true;
    }
    @Override public boolean m_7933_(int key, int scan, int mods) {
        if (key == 256) { back(); return true; }
        for (EditBox field : List.of(name, price, ammo, group, maxBuy)) if (field.m_7933_(key, scan, mods)) return true;
        return super.m_7933_(key, scan, mods);
    }
}
