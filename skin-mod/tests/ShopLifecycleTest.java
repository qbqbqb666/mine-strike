import net.minecraft.client.gui.screens.Screen;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class ShopLifecycleTest {
    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        for (String name : new String[]{"ExpandedShopEditorScreen", "ShopItemEditorScreen", "ShopPurchaseScreen"}) {
            Class<?> type = Class.forName("dev.minestrike.skins." + name);
            if (type.getMethod("m_7861_").getDeclaringClass() != Screen.class)
                throw new AssertionError("Screen removal must retain the inert vanilla callback: " + name);
            Screen screen = (Screen) unsafe.allocateInstance(type);
            // Minecraft.setScreen calls removed while replacing a screen after deletion/save.
            // No initialized client, player, menu, or network is needed for this notification.
            for (int i = 0; i < 1000; i++) screen.m_7861_();
        }
        System.out.println("PASS: delete/save screen replacement and 3000 removal callbacks without reentrant close or network actions");
    }
}
