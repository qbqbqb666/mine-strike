package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ExpandedShopEditorScreen;
import dev.minestrike.skins.ShopItemEditorScreen;
import com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu;
import com.phasetranscrystal.fpsmatch.common.client.screen.VanillaGuiRegister;
import com.phasetranscrystal.fpsmatch.common.client.screen.EditorShopContainer;
import net.minecraft.client.gui.screens.MenuScreens;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = VanillaGuiRegister.class, remap = false)
public abstract class ShopEditorScreenFactoryMixin {
    @ModifyArg(method = "register", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/MenuScreens;m_96206_(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/gui/screens/MenuScreens$ScreenConstructor;)V", ordinal = 0), index = 1, remap = false)
    private static MenuScreens.ScreenConstructor<EditorShopContainer, ExpandedShopEditorScreen> expandedEditor(
            MenuScreens.ScreenConstructor<EditorShopContainer, ExpandedShopEditorScreen> original) {
        return ExpandedShopEditorScreen::new;
    }
    @ModifyArg(method = "register", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/MenuScreens;m_96206_(Lnet/minecraft/world/inventory/MenuType;Lnet/minecraft/client/gui/screens/MenuScreens$ScreenConstructor;)V", ordinal = 1), index = 1, remap = false)
    private static MenuScreens.ScreenConstructor<EditShopSlotMenu, ShopItemEditorScreen> itemEditor(
            MenuScreens.ScreenConstructor<EditShopSlotMenu, ShopItemEditorScreen> original) {
        return ShopItemEditorScreen::new;
    }
}
