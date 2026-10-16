package dev.minestrike.skins.mixin;

import com.phasetranscrystal.blockoffensive.client.key.OpenShopKey;
import dev.minestrike.skins.ShopPurchaseScreen;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OpenShopKey.class, remap = false)
public abstract class ShopPurchaseOpenMixin {
    @Inject(method = "openShop", at = @At("HEAD"), cancellable = true, remap = false)
    private static void openExpandedShop(CallbackInfo ci) {
        Minecraft.m_91087_().m_91152_(new ShopPurchaseScreen());
        ci.cancel();
    }
}
