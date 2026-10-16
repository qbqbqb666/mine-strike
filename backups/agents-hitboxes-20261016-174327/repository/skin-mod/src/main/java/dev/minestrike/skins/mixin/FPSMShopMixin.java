package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ShopExpansion;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.shop.FPSMShop;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(value = FPSMShop.class, remap = false)
public abstract class FPSMShopMixin {
    @Shadow @Final private Map<?, ArrayList<ShopSlot>> defaultShopData;
    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void expandedDefaults(CallbackInfo ci) { ShopExpansion.normalize(defaultShopData); }
    @Inject(method = "setDefaultShopData", at = @At("HEAD"), remap = false)
    private void expandedReload(Map<?, ArrayList<ShopSlot>> data, CallbackInfo ci) { ShopExpansion.normalize(data); }
    @Inject(method = "syncShopData(Lnet/minecraft/server/level/ServerPlayer;)V", at = @At("HEAD"), remap = false)
    private void expandedShape(ServerPlayer player, CallbackInfo ci) { ShopExpansion.sendShape((FPSMShop<?>) (Object) this, player); }
    @Inject(method = "syncShopData()V", at = @At("HEAD"), remap = false)
    private void expandedShapes(CallbackInfo ci) {
        if (FPSMCore.initialized()) for (ServerPlayer player : FPSMCore.getInstance().getServer().m_6846_().m_11314_())
            ShopExpansion.sendShape((FPSMShop<?>) (Object) this, player);
    }
}
