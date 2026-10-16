package dev.minestrike.skins;

import com.tacz.guns.api.TimelessAPI;
import me.xjqsh.lrtactical.api.LrTacticalAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public final class ShopVisuals {
    public static final String[] NAMES = {"装备", "手枪", "中型武器", "步枪", "投掷物", "神秘武器"};
    private static final ResourceLocation CT_BACKGROUND = new ResourceLocation(MineStrikeSkins.ID, "textures/gui/shop-ct.png");
    private static final ResourceLocation T_BACKGROUND = new ResourceLocation(MineStrikeSkins.ID, "textures/gui/shop-t.png");
    private static final ResourceLocation PIXEL_FONT = new ResourceLocation(MineStrikeSkins.ID, "pixel");
    public static final int TEXT = 0xFFF4F1E9, MUTED = 0xFFB6B7B0;
    public static int accent(boolean ct) { return ct ? 0xFFA0D5F0 : 0xFFF1D29B; }
    private static Component label(String text) {
        return Component.m_237113_(text).m_6270_(Style.f_131099_.m_131150_(PIXEL_FONT));
    }
    public static void background(GuiGraphics g, int width, int height, boolean ct) {
        float fit = Math.max(width / 1672f, height / 941f) * 1.06f;
        int w = (int) Math.ceil(1672 * fit), h = (int) Math.ceil(941 * fit);
        int x = Math.min(0, Math.round((width-w)/2f + width * 0.06f));
        g.m_280163_(ct ? CT_BACKGROUND : T_BACKGROUND, x, (height-h)/2, 0, 0, w, h, w, h);
        g.m_280509_(0, 0, width, height, ct ? 0x190B1827 : 0x191E1409);
    }
    public static void panel(GuiGraphics g, int x, int y, int width, int height, boolean ct) {
        g.m_280509_(x + 4, y + 6, x + width + 4, y + height + 6, 0x46000000);
        g.m_280509_(x, y, x + width, y + height, ct ? 0xA3122534 : 0xA32C2115);
        g.m_280509_(x, y, x + width, y + 2, ct ? 0xBA98C8DF : 0xBACFB180);
        g.m_280509_(x, y + 2, x + 1, y + height, ct ? 0x7088B4CE : 0x70CBB28D);
        g.m_280509_(x + width - 2, y + 2, x + width, y + height, 0x75070C10);
        g.m_280509_(x, y + height - 2, x + width, y + height, 0x97070B0E);
    }
    public static void card(GuiGraphics g, int x, int y, int width, int height, boolean ct, boolean hover) {
        g.m_280509_(x + 2, y + 4, x + width + 2, y + height + 4, 0x55000000);
        int fill = ct ? hover ? 0x9A496A80 : 0x63334C5E : hover ? 0x9A8A6B43 : 0x63654C30;
        int light = ct ? hover ? 0xD7BCDDEA : 0xA786A9BB : hover ? 0xD7F1DCB5 : 0xA7B99D74;
        g.m_280509_(x, y, x + width, y + height, fill);
        g.m_280509_(x, y, x + width - 1, y + 2, light);
        g.m_280509_(x, y + 2, x + 2, y + height - 1, light);
        g.m_280509_(x + width - 2, y + 2, x + width, y + height, 0x97091118);
        g.m_280509_(x + 2, y + height - 3, x + width - 2, y + height, 0xB5070C12);
        if (hover) g.m_280509_(x + 3, y + 3, x + width - 3, y + 4, accent(ct));
    }
    public static void deleteButton(GuiGraphics g, int x, int y, boolean hover) {
        g.m_280509_(x + 1, y + 2, x + 17, y + 18, 0x70000000);
        g.m_280509_(x, y, x + 16, y + 16, hover ? 0xFFB84F43 : 0xDA793D36);
        g.m_280509_(x, y, x + 16, y + 1, 0xFFD09A84);
        g.m_280509_(x, y + 15, x + 16, y + 17, 0xFF3D201E);
        centerText(g, "×", x + 8, y + 3, TEXT, 1.1f);
    }
    public static void text(GuiGraphics g, String text, int x, int y, int color) {
        g.m_280614_(Minecraft.m_91087_().f_91062_, label(text), x, y, color, true);
    }
    public static void heading(GuiGraphics g, String text, int x, int y, int color) {
        var pose = g.m_280168_();
        pose.m_85836_(); pose.m_252880_(x, y, 0); pose.m_85841_(1.2f, 1.2f, 1);
        text(g, text, 0, 0, color); pose.m_85849_();
    }
    public static void centerText(GuiGraphics g, String text, int x, int y, int color, float scale) {
        var font = Minecraft.m_91087_().f_91062_;
        var component = label(text);
        var pose = g.m_280168_();
        pose.m_85836_(); pose.m_252880_(x, y, 0); pose.m_85841_(scale, scale, 1);
        g.m_280614_(font, component, -font.m_92852_(component) / 2, 0, color, true);
        pose.m_85849_();
    }
    public static void fitText(GuiGraphics g, String text, int x, int y, int width, int color) {
        var font = Minecraft.m_91087_().f_91062_;
        var lines = font.m_92923_(label(text), Math.max(1, width));
        if (!lines.isEmpty()) g.m_280649_(font, lines.get(0), x, y, color, true);
    }
    public static void icon(GuiGraphics g, ItemStack stack, int x, int y, int width, int height) {
        if (stack.m_41619_()) return;
        ResourceLocation texture = TimelessAPI.getGunDisplay(stack).map(d -> d.getSlotTexture())
                .orElseGet(() -> LrTacticalAPI.getMeleeDisplay(stack).map(d -> d.getSlotTexture()).orElse(null));
        if (texture != null) {
            int size = Math.min(width,height * 2);
            var matrix = g.m_280168_().m_85850_().m_252922_();
            g.m_280588_((int) Math.floor(x * matrix.m00() + matrix.m30()),
                    (int) Math.floor(y * matrix.m11() + matrix.m31()),
                    (int) Math.ceil((x + width) * matrix.m00() + matrix.m30()),
                    (int) Math.ceil((y + height) * matrix.m11() + matrix.m31()));
            g.m_280163_(texture, x + (width-size)/2, y + (height-size)/2, 0,0,size,size,size,size);
            g.m_280618_();
        } else {
            float size=Math.min(32,Math.min(width,height));
            var pose=g.m_280168_(); pose.m_85836_(); pose.m_252880_(x+(width-size)/2f,y+(height-size)/2f,0);
            pose.m_85841_(size/16f,size/16f,1); g.m_280480_(stack,0,0); pose.m_85849_();
        }
    }
    public static Button button(int x,int y,int width,int height,String text,boolean ct,Button.OnPress press) {
        return new Button(Button.m_253074_(label(text),press).m_252987_(x,y,width,height)) {
            @Override public void m_87963_(GuiGraphics g,int mx,int my,float tick) {
                boolean hover = this.f_93623_ && mx >= x && mx < x + width && my >= y && my < y + height;
                card(g, x, y, width, height, ct, hover);
                centerText(g, this.m_6035_().getString(), x + width / 2, y + (height - 10) / 2,
                        this.f_93623_ ? TEXT : MUTED, height >= 22 ? 1.1f : 1f);
            }
        };
    }
}
