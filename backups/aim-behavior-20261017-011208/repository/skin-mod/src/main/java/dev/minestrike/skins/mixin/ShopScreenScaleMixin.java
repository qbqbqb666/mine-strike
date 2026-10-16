package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CSGameShopScreen.class, remap = false)
public abstract class ShopScreenScaleMixin {
    @Inject(method = "calculateScaleFactor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void expandedScale(int width, int height, CallbackInfoReturnable<Float> ci) {
        ci.setReturnValue(ShopRows.scale(width, height, ExpandedShopClient.rows()));
    }
}
