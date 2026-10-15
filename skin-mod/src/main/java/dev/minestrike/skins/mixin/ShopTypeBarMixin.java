package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ExpandedShopClient;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value = CSGameShopScreen.TypeBarLayout.class, remap = false)
public abstract class ShopTypeBarMixin {
    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 5), remap = false)
    private int expandedRows(int original) { return ExpandedShopClient.rows(); }
}
