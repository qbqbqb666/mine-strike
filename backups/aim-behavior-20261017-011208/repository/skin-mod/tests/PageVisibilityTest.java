import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewParent;
import icyllis.modernui.widget.LinearLayout;
import net.minecraft.client.gui.GuiGraphics;
import sun.misc.Unsafe;
import java.lang.reflect.*;

public class PageVisibilityTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        Field parent = View.class.getDeclaredField("mParent"); parent.setAccessible(true);
        Field flags = View.class.getDeclaredField("mViewFlags"); flags.setAccessible(true);
        ViewParent root = (ViewParent) Proxy.newProxyInstance(ViewParent.class.getClassLoader(),
                new Class<?>[]{ViewParent.class}, (proxy, method, values) -> null);
        View skin = (View) unsafe.allocateInstance(LinearLayout.class);
        View match = (View) unsafe.allocateInstance(LinearLayout.class);
        parent.set(skin, root); parent.set(match, root);
        Class<?> tileType = Class.forName("dev.minestrike.skins.SkinSelectionFragment$PixelTile");
        View skinButton = (View) unsafe.allocateInstance(tileType);
        View matchButton = (View) unsafe.allocateInstance(tileType);
        parent.set(skinButton, skin); parent.set(matchButton, match);
        for (String name : new String[]{"width", "height"}) {
            Field size = tileType.getDeclaredField(name); size.setAccessible(true);
            size.setInt(skinButton, 64); size.setInt(matchButton, 64);
        }
        Method draw = tileType.getDeclaredMethod("onDraw", GuiGraphics.class, int.class, int.class, float.class, double.class, float.class);
        draw.setAccessible(true);
        for (int i = 0; i < 20; i++) {
            boolean matchPage = i % 2 != 0;
            flags.setInt(skin, matchPage ? View.GONE : View.VISIBLE);
            flags.setInt(match, matchPage ? View.VISIBLE : View.GONE);
            check(skinButton.isShown() != matchButton.isShown(), "Exactly one page visible");
            View hidden = matchPage ? skinButton : matchButton;
            draw.invoke(hidden, null, 0, 0, 0f, 1d, 1f);
        }
        parent.set(skinButton, null);
        draw.invoke(skinButton, null, 0, 0, 0f, 1d, 1f);
        flags.setInt(match, View.INVISIBLE);
        draw.invoke(matchButton, null, 0, 0, 0f, 1d, 1f);
        System.out.println("PASS: hidden/invisible/detached page buttons skip Minecraft drawing; 20 alternating page switches");
    }
}
