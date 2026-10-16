package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = CSGameShopScreen.WindowLayout.class, remap = false)
public abstract class ShopWindowMixin {
    @ModifyConstant(method = "onMeasure", constant = @Constant(floatValue = 550f), remap = false)
    private float expandedHeight(float original) { return ShopRows.bodyHeight(ExpandedShopClient.rows()); }
}
