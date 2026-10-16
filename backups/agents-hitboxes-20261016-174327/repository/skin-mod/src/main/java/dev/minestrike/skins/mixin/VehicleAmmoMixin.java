package dev.minestrike.skins.mixin;

import com.atsuishio.superbwarfare.data.gun.AmmoConsumer;
import com.atsuishio.superbwarfare.tools.MinecraftUtil;
import dev.minestrike.skins.ShopBundles;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AmmoConsumer.class, remap = false)
public abstract class VehicleAmmoMixin {
    @Redirect(method = "isAmmoItem", at = @At(value = "INVOKE", target = "Lcom/atsuishio/superbwarfare/tools/MinecraftUtil;isSameItemStack(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"))
    private boolean minestrike$ammo(ItemStack actual, ItemStack expected) {
        if (MinecraftUtil.isSameItemStack(actual, expected)) return true;
        if (actual.m_41619_() || expected.m_41619_() || actual.m_41720_() != expected.m_41720_()) return false;
        ItemStack a = actual.m_41777_(), b = expected.m_41777_();
        a.m_41751_(ShopBundles.ammoTag(actual.m_41783_()));
        b.m_41751_(ShopBundles.ammoTag(expected.m_41783_()));
        return MinecraftUtil.isSameItemStack(a, b);
    }
}
