package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import com.phasetranscrystal.fpsmatch.common.client.screen.EditorShopContainer;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;

@Mixin(value = EditorShopContainer.class, remap = false)
public abstract class ShopEditorContainerMixin {
    @Unique private List<ShopSlot> mineStrikeSlots;
    @Inject(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"), remap = false)
    private void rememberEditor(int id, Inventory inv, ItemStack tool, CallbackInfo ci) {
        if (!inv.f_35978_.m_9236_().f_46443_) ShopExpansion.remember((EditorShopContainer) (Object) this, tool);
    }
    @ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            constant = {@Constant(intValue = 5, ordinal = 1), @Constant(intValue = 5, ordinal = 2), @Constant(intValue = 5, ordinal = 3)}, remap = false)
    private int expandedColumns(int original) { return ShopRows.COLUMNS; }
    @Redirect(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "INVOKE", target = "Lcom/phasetranscrystal/fpsmatch/common/client/screen/EditorShopContainer;getAllSlots()Ljava/util/List;"), remap = false)
    private List<ShopSlot> expandedData(EditorShopContainer menu, int id, Inventory inv, ItemStack stack) {
        mineStrikeSlots = ShopExpansion.editorData(menu, stack, inv.f_35978_.m_9236_().f_46443_);
        return mineStrikeSlots;
    }
    @Inject(method = "getAllSlots", at = @At("HEAD"), cancellable = true, remap = false)
    private void expandedCached(CallbackInfoReturnable<List<ShopSlot>> ci) { if (mineStrikeSlots != null) ci.setReturnValue(mineStrikeSlots); }
    @ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 5, ordinal = 0), remap = false)
    private int expandedRows(int original) { return mineStrikeSlots.size() / ShopRows.COLUMNS; }
    @Redirect(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            at = @At(value = "NEW", target = "net/minecraftforge/items/SlotItemHandler"), remap = false)
    private SlotItemHandler expandedHandler(IItemHandler handler, int index, int x, int y) {
        if (index == 0 && handler instanceof ItemStackHandler stacks) stacks.setSize(mineStrikeSlots.size());
        return new SlotItemHandler(handler, index, x, y);
    }
    @ModifyConstant(method = "<init>(ILnet/minecraft/world/entity/player/Inventory;Lnet/minecraft/world/item/ItemStack;)V",
            constant = @Constant(intValue = 163), remap = false)
    private int expandedInventoryY(int original) { return 23 + 28 * mineStrikeSlots.size() / ShopRows.COLUMNS; }
    @Redirect(method = "m_6877_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/nbt/CompoundTag;m_128365_(Ljava/lang/String;Lnet/minecraft/nbt/Tag;)Lnet/minecraft/nbt/Tag;"), remap = false)
    private net.minecraft.nbt.Tag skipDuplicatePreview(net.minecraft.nbt.CompoundTag tag, String key, net.minecraft.nbt.Tag value) {
        // Previews are already sent as compressed editor data, never as a second raw inventory.
        if (key.equals("ShopItems")) { tag.m_128473_(key); return null; }
        return tag.m_128365_(key, value);
    }
    @ModifyConstant(method = "m_150399_", constant = @Constant(intValue = 24), remap = false)
    private int expandedClickLimit(int original) { return mineStrikeSlots.size() - 1; }
}
