package dev.minestrike.skins;

import com.phasetranscrystal.fpsmatch.common.client.screen.EditorShopContainer;
import dev.minestrike.skins.mixin.ShopEditorAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import java.util.Arrays;

public final class ExpandedShopEditorScreen extends ShopPanelScreen implements MenuAccess<EditorShopContainer> {
    private final EditorShopContainer menu;
    private int offset;
    private int[] counts;
    private boolean pending;
    private String feedback="";
    private static boolean editing;
    private static final int CELL=112, ROW=70, VISIBLE=4;
    public ExpandedShopEditorScreen(EditorShopContainer menu,Inventory inv,Component title) { super("商店管理",708,400);this.menu=menu; }
    @Override public EditorShopContainer m_6262_() { return menu; }
    @Override protected void m_7856_() {
        super.m_7856_();
        ItemStack tool=((ShopEditorAccess)menu).mineStrikeTool();
        counts=ShopExpansion.counts(tool);
        ct=tool.m_41784_().m_128461_("SelectedShop").equals("ct");
        offset=Math.min(offset,maxOffset());
        m_142416_(ShopVisuals.button(left+panelWidth-214,top+17,106,24,editing?"完成编辑":"编辑模式",ct,b->{editing=!editing;offset=Math.min(offset,maxOffset());b.m_93666_(Component.m_237113_(editing?"完成编辑":"编辑模式"));}));
        m_142416_(ShopVisuals.button(left+panelWidth-98,top+17,80,24,"返回游戏",ct,b->m_7379_()));
    }
    @Override protected void drawPanel(GuiGraphics g, int mx, int my, float tick) {
        ShopVisuals.panel(g, left, top, panelWidth, panelHeight, ct);
        ShopVisuals.heading(g, (ct ? "CT / " : "T / ") + "商店管理", left + 18, top + 21, ShopVisuals.accent(ct));
        if (!feedback.isEmpty()) ShopVisuals.fitText(g, feedback, left + 18, top + 48, 470, ShopVisuals.MUTED);
        for (int col = 0; col < 6; col++) {
            int x = left + 18 + col * CELL;
            ShopVisuals.heading(g, ShopVisuals.NAMES[col], x + 5, top + 74, ShopVisuals.TEXT);
            for (int visible = 0; visible < VISIBLE; visible++) {
                int row = offset + visible, y = top + 96 + visible * ROW;
                if (row > counts[col] || row == counts[col] && !editing) continue;
                boolean hover = mx >= x && mx < x + CELL - 6 && my >= y && my < y + ROW - 6;
                ShopVisuals.card(g, x, y, CELL - 6, ROW - 6, ct, hover);
                if (row == counts[col]) {
                    ShopVisuals.centerText(g, counts[col] >= 32 ? "已满" : "+ 添加", x + (CELL - 6) / 2, y + 27, ShopVisuals.accent(ct), 1.15f);
                    continue;
                }
                var slot = menu.getAllSlots().get(ShopRows.index(row, col));
                var item = slot.process();
                if (item.m_41619_()) {
                    ShopVisuals.centerText(g, "+", x + (CELL - 6) / 2, y + 24, ShopVisuals.MUTED, 1.8f);
                } else {
                    ShopVisuals.icon(g, item, x + 9, y + 9, CELL - 24, 29);
                    ShopVisuals.fitText(g, item.m_41611_().getString(), x + 7, y + 41, CELL - 20, ShopVisuals.TEXT);
                    ShopVisuals.text(g, "$ " + slot.getDefaultCost(), x + 7, y + 53, ShopVisuals.accent(ct));
                }
                if (editing) {
                    int dx = x + CELL - 24, dy = y + 3;
                    ShopVisuals.deleteButton(g, dx, dy, mx >= dx && mx < dx + 16 && my >= dy && my < dy + 17);
                }
            }
        }
        int total = Math.max(1, Arrays.stream(counts).max().orElse(0) + (editing ? 1 : 0));
        if (total > VISIBLE) {
            int x = left + panelWidth - 9, y = top + 96, height = VISIBLE * ROW - 6;
            int thumb = Math.max(18, height * VISIBLE / total);
            g.m_280509_(x, y, x + 2, y + height, 0x483A4650);
            int position = y + (height - thumb) * offset / (total - VISIBLE);
            g.m_280509_(x, position, x + 2, position + thumb, ShopVisuals.accent(ct));
        }
    }
    private int maxOffset() { return Math.max(0,Arrays.stream(counts).max().orElse(0)+(editing?1:0)-VISIBLE); }
    public void feedback(String message) { pending=false;feedback=message; }
    @Override protected boolean panelClick(double x,double y,int button) {
        if(button!=0||pending||x<left+18||y<top+96) return false;
        int col=(int)(x-left-18)/CELL,visible=(int)(y-top-96)/ROW,row=offset+visible;
        if(col<0||col>=6||visible>=VISIBLE||x>=left+18+(col+1)*CELL-6||y>=top+96+(visible+1)*ROW-6) return false;
        String op;
        if(row<counts[col]) op=editing&&x>=left+18+col*CELL+CELL-24&&y<top+96+visible*ROW+18?"delete":"edit";
        else if(row==counts[col]&&editing&&row<32) op="add"; else return false;
        pending=true;
        Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_,1));
        SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopEditAction(menu.f_38840_,op,col,row));return true;
    }
    @Override public boolean m_6050_(double x,double y,double delta) {
        if(!pending) offset=Math.max(0,Math.min(maxOffset(),offset+(delta<0?1:-1)));return true;
    }
    @Override public void m_7379_() { Minecraft.m_91087_().f_91074_.m_6915_(); }
}
