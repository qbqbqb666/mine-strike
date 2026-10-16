package dev.minestrike.skins.mixin;

import com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = EditShopSlotMenu.class, remap = false)
public interface ShopSlotMenuAccess {
    @Accessor("repoIndex") int mineStrikeIndex();
    @Accessor("shopSlot") ShopSlot mineStrikeSlot();
    @Accessor("itemHandler") ItemStackHandler mineStrikeHandler();
}
