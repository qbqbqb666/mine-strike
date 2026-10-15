package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ShopExpansion;
import com.phasetranscrystal.fpsmatch.core.shop.ShopData;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(value = ShopData.class, remap = false)
public abstract class ShopDataMixin {
    @Inject(method = "checkData", at = @At("HEAD"), cancellable = true, remap = false)
    private void expandedValidation(Map<?, ? extends List<ShopSlot>> data, int count, CallbackInfo ci) {
        if (ShopExpansion.isCS(data)) { ShopExpansion.validate(data, count); ci.cancel(); }
    }
}
