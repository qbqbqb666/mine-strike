package dev.minestrike.skins;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class SkinSelectionFragment extends Fragment {
    private final SkinCatalog catalog;
    private final Map<String, String> selected;
    private boolean locked;
    private boolean saving;
    private String message;
    private SkinCatalog.Group current;
    private LinearLayout content;
    private TextView status;
    private Button confirm;

    public SkinSelectionFragment(SkinCatalog catalog, Map<String, String> choices, boolean locked, String message) {
        this.catalog = catalog;
        this.selected = new LinkedHashMap<>(choices);
        this.locked = locked;
        this.message = message;
        current = catalog.groups.get(0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, DataSet state) {
        Context context = requireContext();
        LinearLayout panel = column(context);
        panel.setPadding(panel.dp(24), panel.dp(20), panel.dp(24), panel.dp(20));
        panel.setBackground(background(0xF51A2024));
        panel.addView(text(context, "MINE-STRIKE  /  赛前皮肤选择", 22, 0xFFEAF1ED));
        panel.addView(text(context, "选择外观后确认。比赛开始后锁定，购买和回合发放沿用选择。", 13, 0xFFB5C3BC));
        LinearLayout tabs = new LinearLayout(context);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        for (SkinCatalog.Group group : catalog.groups) {
            Button tab = button(context, group.name);
            tab.setOnClickListener(v -> { current = group; showChoices(); });
            tabs.addView(tab, new LinearLayout.LayoutParams(0, tabs.dp(44), 1));
        }
        panel.addView(tabs, new LinearLayout.LayoutParams(-1, panel.dp(52)));
        ScrollView scroll = new ScrollView(context);
        content = column(context);
        scroll.addView(content, new ViewGroup.LayoutParams(-1, -2));
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        status = text(context, message, 13, 0xFFB5C3BC);
        panel.addView(status);
        LinearLayout actions = new LinearLayout(context);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button close = button(context, "返回游戏");
        close.setOnClickListener(v -> Minecraft.m_91087_().execute(() -> Minecraft.m_91087_().m_91152_(null)));
        confirm = button(context, "确认并保存本局装备");
        confirm.setOnClickListener(v -> {
            saving = true;
            confirm.setEnabled(false);
            status.setText("正在保存…");
            SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.Request(true, SkinCatalog.JSON.toJson(selected)));
        });
        actions.addView(close, new LinearLayout.LayoutParams(0, panel.dp(48), 1));
        actions.addView(confirm, new LinearLayout.LayoutParams(0, panel.dp(48), 2));
        panel.addView(actions);
        FrameLayout outer = new FrameLayout(context);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        params.setMargins(outer.dp(12), outer.dp(12), outer.dp(12), outer.dp(12));
        outer.addView(panel, params);
        showChoices();
        return outer;
    }

    public void receive(Map<String, String> choices, boolean locked, String message) {
        selected.clear(); selected.putAll(choices);
        this.locked = locked;
        this.message = message;
        saving = false;
        if (status != null) { status.setText(message); showChoices(); }
    }

    private void showChoices() {
        if (content == null) return;
        content.removeAllViews();
        Context context = requireContext();
        content.addView(text(context, current.name + " · " + selectionName(current), 18, 0xFFEAF1ED));
        List<String> ids = new ArrayList<>();
        ids.add(null);
        for (SkinCatalog.Skin skin : current.skins) ids.add(skin.id);
        for (int first = 0; first < ids.size(); first += 3) {
            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            for (int i = first; i < Math.min(first + 3, ids.size()); i++) {
                String id = ids.get(i);
                boolean chosen = Objects.equals(selected.get(current.key), id);
                LinearLayout card = column(context);
                card.setPadding(card.dp(8), card.dp(8), card.dp(8), card.dp(8));
                card.setBackground(background(chosen ? 0xFF37564B : 0xFF252E33));
                String displayId = id == null ? current.baseIds.get(0) : id;
                boolean available = id == null || MineStrikeSkins.available(current, displayId);
                MinecraftSurfaceView icon = new MinecraftSurfaceView(context);
                icon.setRenderer(new ItemRenderer(MineStrikeSkins.available(current, displayId)
                        ? MineStrikeSkins.preview(current, displayId) : ItemStack.f_41583_));
                card.addView(icon, new LinearLayout.LayoutParams(-1, card.dp(76)));
                Button choose = button(context, (!available ? "资源未加载 · " : chosen ? "已选 · " : "") + skinName(current, id));
                choose.setEnabled(!locked && !saving && available);
                choose.setOnClickListener(v -> {
                    if (id == null) selected.remove(current.key); else selected.put(current.key, id);
                    status.setText("尚未保存，请点击确认。");
                    showChoices();
                });
                card.addView(choose, new LinearLayout.LayoutParams(-1, card.dp(44)));
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
                params.setMargins(row.dp(4), row.dp(4), row.dp(4), row.dp(4));
                row.addView(card, params);
            }
            content.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
        content.addView(text(context, "本局选择", 16, 0xFFEAF1ED));
        for (SkinCatalog.Group group : catalog.groups) {
            content.addView(text(context, group.name + "：" + selectionName(group), 13, 0xFFB5C3BC));
        }
        if (confirm != null) { confirm.setEnabled(!locked && !saving); confirm.setText(locked ? "本局外观已锁定" : "确认并保存本局装备"); }
    }

    private String selectionName(SkinCatalog.Group group) { return skinName(group, selected.get(group.key)); }
    private static String skinName(SkinCatalog.Group group, String id) {
        return group.skins.stream().filter(s -> s.id.equals(id)).map(s -> s.name).findFirst().orElse("默认外观");
    }
    private static LinearLayout column(Context context) {
        LinearLayout layout = new LinearLayout(context); layout.setOrientation(LinearLayout.VERTICAL); return layout;
    }
    private static ShapeDrawable background(int color) {
        ShapeDrawable drawable = new ShapeDrawable(); drawable.setColor(color); drawable.setCornerRadius(8); return drawable;
    }
    private static TextView text(Context context, String value, int size, int color) {
        TextView view = new TextView(context); view.setText(value); view.setTextSize(size); view.setTextColor(color);
        view.setPadding(0, view.dp(4), 0, view.dp(8)); return view;
    }
    private static Button button(Context context, String value) {
        Button button = new Button(context); button.setText(value); button.setTextSize(13); button.setTextColor(0xFFEAF1ED);
        button.setGravity(Gravity.CENTER); return button;
    }

    private static final class ItemRenderer implements MinecraftSurfaceView.Renderer {
        private final ItemStack item;
        private int width;
        private int height;
        ItemRenderer(ItemStack item) { this.item = item; }
        public void onSurfaceChanged(int width, int height) { this.width = width; this.height = height; }
        public void onDraw(GuiGraphics graphics, int mouseX, int mouseY, float tick, double alpha, float scale) {
            var pose = graphics.m_280168_();
            pose.m_85836_();
            float zoom = Math.max(1, Math.min(width, height) / 22f);
            pose.m_252880_(width / 2f - 8 * zoom, height / 2f - 8 * zoom, 0);
            pose.m_85841_(zoom, zoom, 1);
            graphics.m_280480_(item, 0, 0);
            graphics.m_280262_();
            pose.m_85849_();
        }
    }
}
