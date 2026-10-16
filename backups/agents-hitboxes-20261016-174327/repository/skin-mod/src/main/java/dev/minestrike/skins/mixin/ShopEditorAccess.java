package dev.minestrike.skins.mixin;

import com.phasetranscrystal.fpsmatch.common.client.screen.EditorShopContainer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import net.minecraft.world.entity.player.Player;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;

@Mixin(value = EditorShopContainer.class, remap = false)
public interface ShopEditorAccess {
    @Accessor(value = "guiItemStack", remap = false) ItemStack mineStrikeTool();
    @Invoker("openSecondMenu") void mineStrikeOpenSlot(Player player, ShopSlot slot, int index);
}
