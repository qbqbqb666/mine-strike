package dev.minestrike.skins;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.graphics.Matrix;
import icyllis.modernui.mc.MinecraftSurfaceView;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.*;
import icyllis.modernui.widget.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.api.TimelessAPI;
import me.xjqsh.lrtactical.api.LrTacticalAPI;
import java.util.*;

public final class SkinSelectionFragment extends Fragment {
    private boolean matchPage;
    private MatchLobby.State lobby;
    private LinearLayout skinPane, matchPane;
    private PixelTile skinTab, matchTab, roomInfo, teamT, teamCT, readyInfo, readyButton, endInfo, endButton, lobbyMessage;
    private ScrollView matchViewport;
    private ImageView background;
    private View leftSpace, rightSpace;
    private final SkinCatalog catalog;
    private final Map<String, String> selected;
    private boolean locked;
    private boolean saving;
    private String message;
    private SkinCatalog.Group current;
    private LinearLayout cards;
    private LinearLayout summary;
    private ScrollView cardsViewport;
    private ScrollView summaryViewport;
    private PixelTile preview;
    private PixelTile status;
    private PixelTile confirm;
    private final Map<String, PixelTile> tabs = new LinkedHashMap<>();

    public SkinSelectionFragment(SkinCatalog catalog, Map<String, String> choices, boolean locked, String message, MatchLobby.State lobby) {
        this.lobby = lobby;
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
        panel.setPadding(panel.dp(14), panel.dp(10), panel.dp(14), panel.dp(10));
        ShapeDrawable stone = new ShapeDrawable();
        stone.setColor(0x66D9D4C8);
        stone.setStroke(panel.dp(2), 0xA6514C42);
        panel.setBackground(stone);
        LinearLayout navigation = row(context);
        skinTab = tile(context, "皮肤选择", null, true, null);
        matchTab = tile(context, "比赛阶段", null, true, null);
        skinTab.setOnClickListener(v -> showPage(false));
        matchTab.setOnClickListener(v -> showPage(true));
        navigation.addView(skinTab, cell(navigation, 38, 1));
        navigation.addView(matchTab, cell(navigation, 38, 1));
        panel.addView(navigation, new LinearLayout.LayoutParams(-1, panel.dp(46)));
        skinPane = column(context);
        LinearLayout categories = row(context);
        for (SkinCatalog.Group group : catalog.groups) {
            PixelTile tab = tile(context, group.name, null, true, null);
            tab.setOnClickListener(v -> { current = group; cardsViewport.scrollTo(0, 0); refresh(); });
            tabs.put(group.key, tab);
            categories.addView(tab, cell(categories, 40, 1));
        }
        skinPane.addView(categories, new LinearLayout.LayoutParams(-1, panel.dp(48)));
        LinearLayout body = row(context);
        LinearLayout left = column(context);
        preview = tile(context, "", null, false, null);
        preview.framed = true;
        left.addView(preview, new LinearLayout.LayoutParams(-1, left.dp(150)));
        cardsViewport = new ScrollView(context);
        cards = column(context);
        cardsViewport.addView(cards, new ViewGroup.LayoutParams(-1, -2));
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.setMargins(0, left.dp(8), 0, 0);
        left.addView(cardsViewport, scrollParams);
        body.addView(left, new LinearLayout.LayoutParams(0, -1, 3));
        LinearLayout right = column(context);
        right.addView(tile(context, "已选装备", null, false, null), new LinearLayout.LayoutParams(-1, right.dp(30)));
        summaryViewport = new ScrollView(context);
        summary = column(context);
        summaryViewport.addView(summary, new ViewGroup.LayoutParams(-1, -2));
        right.addView(summaryViewport, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout.LayoutParams sideParams = new LinearLayout.LayoutParams(0, -1, 1);
        sideParams.setMargins(body.dp(10), 0, 0, 0);
        body.addView(right, sideParams);
        skinPane.addView(body, new LinearLayout.LayoutParams(-1, 0, 1));
        status = tile(context, "", null, false, null);
        skinPane.addView(status, new LinearLayout.LayoutParams(-1, panel.dp(22)));
        LinearLayout actions = row(context);
        PixelTile close = tile(context, "返回游戏", null, true, null);
        close.setOnClickListener(v -> Minecraft.m_91087_().execute(() -> Minecraft.m_91087_().m_91152_(null)));
        confirm = tile(context, "保存选择", null, true, null);
        confirm.chosen = true;
        confirm.setOnClickListener(v -> {
            saving = true;
            message = "保存中";
            refresh();
            SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.Request(true, SkinCatalog.JSON.toJson(selected)));
        });
        actions.addView(close, cell(actions, 44, 1));
        actions.addView(confirm, cell(actions, 44, 1));
        skinPane.addView(actions, new LinearLayout.LayoutParams(-1, panel.dp(48)));
        panel.addView(skinPane, new LinearLayout.LayoutParams(-1, 0, 1));
        matchViewport = new ScrollView(context);
        matchPane = column(context);
        roomInfo = tile(context, "", null, false, matchViewport);
        matchPane.addView(roomInfo, new LinearLayout.LayoutParams(-1, panel.dp(30)));
        matchPane.addView(tile(context, "选择阵营", null, false, matchViewport), new LinearLayout.LayoutParams(-1, panel.dp(26)));
        LinearLayout sides = row(context);
        teamT = tile(context, "T 土匪", null, true, matchViewport);
        teamCT = tile(context, "CT 反恐", null, true, matchViewport);
        teamT.setOnClickListener(v -> lobbyAction("t"));
        teamCT.setOnClickListener(v -> lobbyAction("ct"));
        sides.addView(teamT, cell(sides, 42, 1)); sides.addView(teamCT, cell(sides, 42, 1));
        matchPane.addView(sides, new LinearLayout.LayoutParams(-1, panel.dp(50)));
        matchPane.addView(tile(context, "游戏准备", null, false, matchViewport), new LinearLayout.LayoutParams(-1, panel.dp(26)));
        readyInfo = tile(context, "", null, false, matchViewport);
        matchPane.addView(readyInfo, new LinearLayout.LayoutParams(-1, panel.dp(32)));
        readyButton = tile(context, "准备游戏", null, true, matchViewport);
        readyButton.setOnClickListener(v -> lobbyAction("ready"));
        matchPane.addView(readyButton, new LinearLayout.LayoutParams(-1, panel.dp(42)));
        matchPane.addView(tile(context, "发起结束游戏", null, false, matchViewport), new LinearLayout.LayoutParams(-1, panel.dp(26)));
        endInfo = tile(context, "", null, false, matchViewport);
        matchPane.addView(endInfo, new LinearLayout.LayoutParams(-1, panel.dp(32)));
        endButton = tile(context, "发起结束游戏", null, true, matchViewport);
        endButton.setOnClickListener(v -> lobbyAction("end"));
        matchPane.addView(endButton, new LinearLayout.LayoutParams(-1, panel.dp(42)));
        lobbyMessage = tile(context, "", null, false, matchViewport);
        matchPane.addView(lobbyMessage, new LinearLayout.LayoutParams(-1, panel.dp(36)));
        PixelTile back = tile(context, "返回游戏", null, true, matchViewport);
        back.setOnClickListener(v -> Minecraft.m_91087_().execute(() -> Minecraft.m_91087_().m_91152_(null)));
        matchPane.addView(back, new LinearLayout.LayoutParams(-1, panel.dp(42)));
        matchViewport.addView(matchPane, new ViewGroup.LayoutParams(-1, -2));
        matchViewport.setVisibility(View.GONE);
        panel.addView(matchViewport, new LinearLayout.LayoutParams(-1, 0, 1));
        FrameLayout outer = new FrameLayout(context);
        background = new ImageView(context) {
            @Override
            protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
                super.onSizeChanged(width, height, oldWidth, oldHeight);
                fitBackground(width, height);
            }
        };
        ImageDrawable backdrop = new ImageDrawable("minestrike_skins", "gui/skin-background.png");
        backdrop.setFilter(false);
        background.setImageDrawable(backdrop);
        background.setScaleType(ImageView.ScaleType.MATRIX);
        outer.addView(background, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout foreground = row(context);
        leftSpace = new View(context);
        rightSpace = new View(context);
        foreground.addView(leftSpace, new LinearLayout.LayoutParams(0, -1, 26));
        foreground.addView(panel, new LinearLayout.LayoutParams(0, -1, 74));
        foreground.addView(rightSpace, new LinearLayout.LayoutParams(0, -1, 0));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER);
        params.setMargins(outer.dp(12), outer.dp(12), outer.dp(12), outer.dp(12));
        outer.addView(foreground, params);
        refresh();
        refreshLobby();
        skinTab.chosen = true;
        return outer;
    }

    private void fitBackground(int width, int height) {
        var drawable = background.getDrawable();
        if (drawable == null || width <= 0 || height <= 0) return;
        float scale = Math.max((float) width / drawable.getIntrinsicWidth(), (float) height / drawable.getIntrinsicHeight());
        Matrix matrix = new Matrix();
        matrix.setScaleTranslate(scale, scale, matchPage ? width - drawable.getIntrinsicWidth() * scale : 0,
                (height - drawable.getIntrinsicHeight() * scale) / 2);
        background.setImageMatrix(matrix);
    }
    private void showPage(boolean match) {
        matchPage = match;
        skinPane.setVisibility(match ? View.GONE : View.VISIBLE);
        matchViewport.setVisibility(match ? View.VISIBLE : View.GONE);
        skinTab.chosen = !match; matchTab.chosen = match;
        leftSpace.setLayoutParams(new LinearLayout.LayoutParams(0, -1, match ? 0 : 26));
        rightSpace.setLayoutParams(new LinearLayout.LayoutParams(0, -1, match ? 26 : 0));
        ImageDrawable backdrop = new ImageDrawable("minestrike_skins", match ? "gui/match-background.png" : "gui/skin-background.png");
        backdrop.setFilter(false); background.setImageDrawable(backdrop);
        fitBackground(background.getWidth(), background.getHeight());
        if (match) lobbyAction("status");
    }
    private void lobbyAction(String action) {
        Minecraft.m_91087_().execute(() -> SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.LobbyAction(action)));
    }
    public void receiveLobby(MatchLobby.State state) { lobby = state; refreshLobby(); }
    private void refreshLobby() {
        if (roomInfo == null) return;
        boolean available = lobby != null && lobby.available();
        boolean started = available && lobby.started();
        String team = lobby == null ? "" : lobby.team();
        roomInfo.label = available ? "当前比赛：" + lobby.map() + (started ? "  游戏中" : "  等待准备") : "当前存档没有 CS 比赛配置";
        teamT.label = "T 土匪" + (available ? "  " + lobby.tCount() + " 人" : "");
        teamCT.label = "CT 反恐" + (available ? "  " + lobby.ctCount() + " 人" : "");
        teamT.chosen = team.equals("t"); teamCT.chosen = team.equals("ct");
        teamT.setEnabled(available && !started); teamCT.setEnabled(available && !started);
        readyInfo.label = lobby == null ? "同步中" : "服务器人数：" + lobby.total() + (started ? "  比赛进行中" : "  已准备：" + lobby.readyCount());
        readyButton.label = started ? "比赛进行中" : lobby != null && lobby.ready() ? "取消准备" : "准备游戏";
        readyButton.chosen = lobby != null && lobby.ready();
        readyButton.setEnabled(available && !started && (team.equals("t") || team.equals("ct")));
        endInfo.label = !started ? "比赛开始后可发起结束" : "结束同意：" + lobby.endCount() + " / " + lobby.total();
        endButton.label = lobby != null && lobby.ending() ? "撤回结束同意" : lobby != null && lobby.endCount() > 0 ? "同意结束游戏" : "发起结束游戏";
        endButton.chosen = lobby != null && lobby.ending(); endButton.setEnabled(started);
        if (lobby != null && !lobby.message().isEmpty()) lobbyMessage.label = lobby.message();
    }

    public void receive(Map<String, String> choices, boolean locked, String message) {
        selected.clear(); selected.putAll(choices);
        this.locked = locked;
        this.message = message;
        saving = false;
        refresh();
    }

    private void refresh() {
        if (cards == null) return;
        Context context = requireContext();
        tabs.forEach((key, tile) -> tile.chosen = key.equals(current.key));
        preview.label = selectionName(current);
        preview.item = item(current, selected.get(current.key));
        cards.removeAllViews();
        List<String> ids = new ArrayList<>();
        ids.add(null);
        for (SkinCatalog.Skin skin : current.skins) ids.add(skin.id);
        for (int first = 0; first < ids.size(); first += 3) {
            LinearLayout row = row(context);
            for (int i = first; i < Math.min(first + 3, ids.size()); i++) {
                String id = ids.get(i);
                String displayId = id == null ? current.baseIds.get(0) : id;
                boolean available = id == null || MineStrikeSkins.available(current, displayId);
                PixelTile card = tile(context, skinName(current, id), item(current, id), true, cardsViewport);
                card.chosen = Objects.equals(selected.get(current.key), id);
                card.setEnabled(!locked && !saving && available);
                if (!available) card.label = "资源未加载";
                card.setOnClickListener(v -> {
                    if (id == null) selected.remove(current.key); else selected.put(current.key, id);
                    message = "未保存";
                    refresh();
                });
                row.addView(card, cell(row, 106, 1));
            }
            for (int i = Math.min(first + 3, ids.size()) - first; i < 3; i++) {
                row.addView(new View(context), cell(row, 106, 1));
            }
            cards.addView(row, new LinearLayout.LayoutParams(-1, cards.dp(114)));
        }
        summary.removeAllViews();
        for (SkinCatalog.Group group : catalog.groups) {
            PixelTile entry = tile(context, group.name + "\n" + selectionName(group), null, true, summaryViewport);
            entry.chosen = group == current;
            entry.setOnClickListener(v -> { current = group; cardsViewport.scrollTo(0, 0); refresh(); });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, summary.dp(62));
            params.setMargins(0, 0, 0, summary.dp(8));
            summary.addView(entry, params);
        }
        status.label = locked ? "已锁定" : saving ? "保存中" : message != null && message.contains("已保存") ? "已保存"
                : "未保存".equals(message) ? "未保存" : message != null && message.contains("失败") ? "保存失败" : "";
        confirm.setEnabled(!locked && !saving);
        confirm.label = locked ? "已锁定" : "保存选择";
    }

    private String selectionName(SkinCatalog.Group group) { return skinName(group, selected.get(group.key)); }
    private static String skinName(SkinCatalog.Group group, String id) {
        return group.skins.stream().filter(s -> s.id.equals(id)).map(s -> s.name).findFirst().orElse("默认");
    }
    private static ItemStack item(SkinCatalog.Group group, String selectedId) {
        String id = selectedId == null ? group.baseIds.get(0) : selectedId;
        return MineStrikeSkins.available(group, id) ? MineStrikeSkins.preview(group, id) : ItemStack.f_41583_;
    }
    private static LinearLayout column(Context context) {
        LinearLayout view = new LinearLayout(context); view.setOrientation(LinearLayout.VERTICAL); return view;
    }
    private static LinearLayout row(Context context) {
        LinearLayout view = new LinearLayout(context); view.setOrientation(LinearLayout.HORIZONTAL); return view;
    }
    private static LinearLayout.LayoutParams cell(View view, int height, int weight) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, view.dp(height), weight);
        params.setMargins(view.dp(3), view.dp(3), view.dp(3), view.dp(3));
        return params;
    }
    private static PixelTile tile(Context context, String label, ItemStack item, boolean button, ScrollView clip) {
        return new PixelTile(context, label, item, button, clip);
    }

    private static final class PixelTile extends MinecraftSurfaceView implements MinecraftSurfaceView.Renderer {
        private static final ResourceLocation PIXEL_FONT = new ResourceLocation("minestrike_skins", "pixel");
        private volatile String label;
        private volatile ItemStack item;
        private volatile boolean chosen;
        private boolean largeText;
        private boolean framed;
        private final boolean button;
        private final ScrollView clip;
        private int width;
        private int height;

        PixelTile(Context context, String label, ItemStack item, boolean button, ScrollView clip) {
            super(context);
            this.label = label;
            this.item = item;
            this.button = button;
            this.clip = clip;
            setSoundEffectsEnabled(button);
            setRenderer(this);
        }
        @Override
        public void playSoundEffect(int effect) {
            if (!button || !isEnabled() || !isSoundEffectsEnabled()) return;
            Minecraft minecraft = Minecraft.m_91087_();
            minecraft.execute(() -> minecraft.m_91106_().m_120367_(
                    SimpleSoundInstance.m_263171_(SoundEvents.f_12490_, 1.0F)));
        }
        public void onSurfaceChanged(int width, int height) { this.width = width; this.height = height; }
        public void onDraw(GuiGraphics graphics, int mouseX, int mouseY, float tick, double guiScale, float alpha) {
            if (!isShown() || width <= 0 || height <= 0) return;
            if (clip != null) {
                int[] own = new int[2], viewport = new int[2];
                getLocationInWindow(own);
                clip.getLocationInWindow(viewport);
                int[] bounds = PreviewGeometry.intersection(own[0], own[1], width, height,
                        viewport[0], viewport[1], clip.getWidth(), clip.getHeight());
                if (bounds == null) return;
                RenderSystem.enableScissor(bounds[0], Minecraft.m_91087_().m_91268_().m_85442_() - bounds[3],
                        bounds[2] - bounds[0], bounds[3] - bounds[1]);
            }
            float w = (float) (width / guiScale), h = (float) (height / guiScale);
            var pose = graphics.m_280168_();
            pose.m_85836_();
            try {
                graphics.m_280246_(1, 1, 1, alpha);
                boolean pressed = button && isPressed() && isEnabled();
                int edge = Math.max(1, Math.round(dp(2) / (float) guiScale));
                if (button || framed) drawBlock(graphics, Math.round(w), Math.round(h), edge, pressed);
                if (pressed) pose.m_252880_(0, edge, 0);
                ItemStack stack = item;
                if (stack != null && !stack.m_41619_()) {
                    float textSpace = dp(26) / (float) guiScale;
                    float size = Math.max(1, Math.min(w * 0.82f, h - textSpace - edge * 4));
                    pose.m_85836_();
                    pose.m_252880_((w - size) / 2, edge * 3, 0);
                    ResourceLocation texture = TimelessAPI.getGunDisplay(stack).map(display -> display.getSlotTexture())
                            .orElseGet(() -> LrTacticalAPI.getMeleeDisplay(stack).map(display -> display.getSlotTexture()).orElse(null));
                    if (texture != null) {
                        int pixels = Math.max(1, Math.round(size));
                        graphics.m_280163_(texture, 0, 0, 0, 0, pixels, pixels, pixels, pixels);
                    } else {
                        pose.m_85841_(size / 16, size / 16, 1);
                        graphics.m_280480_(stack, 0, 0);
                    }
                    graphics.m_280262_();
                    pose.m_85849_();
                }
                float textScale = dp(largeText ? 18 : 12) / (float) guiScale / 9;
                pose.m_85836_();
                pose.m_85841_(textScale, textScale, 1);
                var font = Minecraft.m_91087_().f_91062_;
                var text = Component.m_237113_(label).m_6270_(Style.f_131099_.m_131150_(PIXEL_FONT));
                var lines = font.m_92923_(text, Math.max(16, (int) ((w - edge * 6) / textScale)));
                int count = Math.min(2, lines.size());
                float y = stack != null ? (h - dp(22) / (float) guiScale) / textScale : h / textScale / 2 - count * 5;
                int color = !button && !framed ? 0xFF38352F : !isEnabled() && button ? 0xFFCCCCCC
                        : chosen || button && isHovered() ? 0xFFFFFFA0 : 0xFFF0F0F0;
                for (int i = 0; i < count; i++) {
                    var line = lines.get(i);
                    graphics.m_280649_(font, line, Math.round((w / textScale - font.m_92724_(line)) / 2), Math.round(y + i * 10), color, true);
                }
                graphics.m_280262_();
                pose.m_85849_();
                if (chosen && button && stack != null) {
                    float badgeScale = dp(10) / (float) guiScale / 9;
                    pose.m_85836_();
                    pose.m_85841_(badgeScale, badgeScale, 1);
                    var badge = Component.m_237113_("已选").m_6270_(Style.f_131099_.m_131150_(PIXEL_FONT));
                    int badgeX = Math.round(edge * 4 / badgeScale);
                    int badgeY = Math.round(edge * 4 / badgeScale);
                    graphics.m_280509_(badgeX - 2, badgeY - 2, badgeX + font.m_92852_(badge) + 2, badgeY + 11, 0xFF353D2B);
                    graphics.m_280614_(font, badge, badgeX, badgeY, 0xFFFFFFA0, true);
                    graphics.m_280262_();
                    pose.m_85849_();
                }
            } finally {
                graphics.m_280246_(1, 1, 1, 1);
                pose.m_85849_();
            }
        }

        private void drawBlock(GuiGraphics g, int w, int h, int edge, boolean pressed) {
            int fill = chosen ? 0xCC829363 : framed ? 0x99B6B1A6 : isHovered() && isEnabled() ? 0xCCAAA79D : 0xB0969389;
            int light = pressed ? 0xFF57534B : chosen ? 0xFFC0CF9B : 0xFFD9D5CA;
            int shadow = pressed ? 0xFFAAAAAA : 0xFF303030;
            g.m_280509_(0, 0, w, edge, 0xCC111111);
            g.m_280509_(0, h - edge, w, h, 0xCC111111);
            g.m_280509_(0, edge, edge, h - edge, 0xCC111111);
            g.m_280509_(w - edge, edge, w, h - edge, 0xCC111111);
            g.m_280509_(edge, edge, w - edge, h - edge, fill);
            g.m_280509_(edge, edge, w - edge, edge * 2, light);
            g.m_280509_(edge, edge, edge * 2, h - edge, light);
            g.m_280509_(edge, h - edge * 2, w - edge, h - edge, shadow);
            g.m_280509_(w - edge * 2, edge, w - edge, h - edge, shadow);
            for (int y = edge * 3; y < h - edge * 3; y += edge * 5) {
                for (int x = edge * 3; x < w - edge * 3; x += edge * 7) {
                    int tint = ((x / edge + y / edge) % 3 == 0) ? 0x0DFFFFFF : 0x0C000000;
                    g.m_280509_(x, y, x + edge, y + edge, tint);
                }
            }
            if (chosen) {
                g.m_280509_(edge * 2, edge * 2, w - edge * 2, edge * 3, 0xFFD4C46E);
                g.m_280509_(edge * 2, h - edge * 3, w - edge * 2, h - edge * 2, 0xFFD4C46E);
                g.m_280509_(edge * 2, edge * 2, edge * 3, h - edge * 2, 0xFFD4C46E);
                g.m_280509_(w - edge * 3, edge * 2, w - edge * 2, h - edge * 2, 0xFFD4C46E);
            }
        }
    }
}
