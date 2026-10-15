package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import com.google.common.collect.Multimap;
import com.phasetranscrystal.fpsmatch.core.shop.ShopData;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(value = ShopData.class, remap = false)
public abstract class ShopDataMixin {
    @Shadow private Multimap<Integer, ShopSlot> grouped;
    @Shadow protected abstract void broadcastGroupChangeEvent(ServerPlayer player, ShopSlot slot, int flag);
    @Shadow public abstract void addMoney(int money);
    @Inject(method = "checkData", at = @At("HEAD"), cancellable = true, remap = false)
    private void expandedValidation(Map<?, ? extends List<ShopSlot>> data, int count, CallbackInfo ci) {
        if (ShopExpansion.isCS(data)) { ShopExpansion.validate(data, count); ci.cancel(); }
    }
    @Inject(method = "handleBuy", at = @At("HEAD"), cancellable = true, remap = false)
    private void bundleCapacity(ServerPlayer player, ShopSlot slot, CallbackInfo ci) {
        if (ShopBundles.isBundle(slot) && !ShopBundles.canFit(slot, player)) { ShopBundles.full(player); ci.cancel(); }
    }
    @Inject(method = "handleReturn", at = @At("HEAD"), cancellable = true, remap = false)
    private void refundBundle(ServerPlayer player, ShopSlot slot, CallbackInfo ci) {
        if (!ShopBundles.isBundle(slot)) return;
        ci.cancel();
        if (slot.haveGroup()) for (ShopSlot other : grouped.get(slot.getGroupId())) {
            if (other != slot && other.getBoughtCount() > 0
                    && other.getListenerNames().stream().anyMatch(name -> name.contains("changeItem")) && !other.canReturn(player)) return;
        }
        if (!slot.canReturn(player)) return;
        broadcastGroupChangeEvent(player, slot, -1);
        int before = slot.getBoughtCount();
        slot.returnItem(player);
        // Credit only after the complete purchase has actually been removed.
        if (slot.getBoughtCount() == before - 1) addMoney(slot.getCost());
    }
}
