package dev.minestrike.skins.mixin;

import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import dev.minestrike.skins.ShopBundles;
import dev.minestrike.skins.RoundItems;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.*;

@Mixin(value = ShopSlot.class, remap = false)
public abstract class ShopBundleSlotMixin implements ShopBundles.SlotState {
    @Shadow private int boughtCount;
    @Unique private final List<ShopBundles.Receipt> mineStrikeReceipts = new ArrayList<>();
    @Override public List<ShopBundles.Receipt> mineStrikeReceipts() { return mineStrikeReceipts; }
    @Inject(method = "buy", at = @At("HEAD"), cancellable = true, remap = false)
    private void buyBundle(ServerPlayer player, int money, CallbackInfoReturnable<Integer> ci) {
        ShopSlot slot = (ShopSlot) (Object) this;
        if (!ShopBundles.isBundle(slot)) return;
        if (ShopBundles.buy(slot, player)) { boughtCount++; ci.setReturnValue(money - slot.getCost()); }
        else ci.setReturnValue(money);
    }
    @Redirect(method = "buy", at = @At(value = "INVOKE", target = "Lcom/phasetranscrystal/fpsmatch/core/shop/slot/ShopSlot;process()Lnet/minecraft/world/item/ItemStack;"), remap = false)
    private ItemStack issueRoundItem(ShopSlot slot, ServerPlayer player, int money) {
        return RoundItems.issue(slot.process(), player);
    }
    @Inject(method = "canReturn", at = @At("HEAD"), cancellable = true, remap = false)
    private void returnableBundle(Player player, CallbackInfoReturnable<Boolean> ci) {
        ShopSlot slot = (ShopSlot) (Object) this;
        if (ShopBundles.isBundle(slot)) ci.setReturnValue(ShopBundles.canReturn(slot, player));
    }
    @Inject(method = "returnItem(Lnet/minecraft/world/entity/player/Player;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void returnBundle(Player player, int count, CallbackInfo ci) {
        ShopSlot slot = (ShopSlot) (Object) this;
        if (!ShopBundles.isBundle(slot)) return;
        boughtCount -= ShopBundles.returnItems(slot, player, count);
        ci.cancel();
    }
    @Inject(method = "reset", at = @At("TAIL"), remap = false)
    private void resetBundle(CallbackInfo ci) { mineStrikeReceipts.clear(); }
}
