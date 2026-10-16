package dev.minestrike.skins.mixin;

import com.phasetranscrystal.fpsmatch.util.FPSMUtil;
import com.tacz.guns.api.item.IGun;
import dev.minestrike.skins.ShopAmmo;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FPSMUtil.class, remap = false)
public abstract class ShopAmmoResetMixin {
    @Inject(method = "resetGunAmmo", at = @At("TAIL"), remap = false)
    private static void configuredTotal(ItemStack stack, IGun gun, CallbackInfo ci) {
        ShopAmmo.restore(stack);
    }
}
