package dev.minestrike.skins.mixin;

import com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu;
import dev.minestrike.skins.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.server.level.ServerPlayer;

@Mixin(value = EditShopSlotMenu.class, remap = false)
public abstract class ShopSlotMenuMixin {
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraftforge/items/ItemStackHandler;Lnet/minecraft/world/inventory/ContainerData;Lcom/phasetranscrystal/fpsmatch/core/shop/slot/ShopSlot;Lnet/minecraft/world/item/ItemStack;I)V", at = @At("TAIL"), remap = false)
    private void rememberSlot(int id, net.minecraft.world.entity.player.Inventory inv, net.minecraftforge.items.ItemStackHandler handler,
                              net.minecraft.world.inventory.ContainerData data, com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot slot,
                              net.minecraft.world.item.ItemStack tool, int index, CallbackInfo ci) {
        EditShopSlotMenu menu = (EditShopSlotMenu) (Object) this;
        if (!inv.f_35978_.m_9236_().f_46443_) ShopExpansion.remember(menu, tool);
    }
    @ModifyConstant(method = "saveData", constant = @Constant(intValue = 5), remap = false)
    private int sixColumns(int original) { return ShopRows.COLUMNS; }
    @Inject(method = "saveData", at = @At("HEAD"), cancellable = true, remap = false)
    private void guardSave(ServerPlayer player, CallbackInfo ci) {
        EditShopSlotMenu menu = (EditShopSlotMenu) (Object) this;
        if (!ShopExpansion.editable(player, menu, menu.getGuiItemStack())) ci.cancel();
    }
}
