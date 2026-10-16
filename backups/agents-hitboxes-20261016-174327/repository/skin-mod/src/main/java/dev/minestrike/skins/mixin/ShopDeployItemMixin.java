package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ShopDeployments;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemStack.class, remap = false)
public abstract class ShopDeployItemMixin {
    @Inject(method = "m_204121_", at = @At("HEAD"), cancellable = true, remap = false)
    private void allowPurchasedDeployment(CallbackInfoReturnable<Boolean> ci) {
        if (ShopDeployments.authorized((ItemStack) (Object) this)) ci.setReturnValue(true);
    }
    @Redirect(method = "m_41661_", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeHooks;onPlaceItemIntoWorld(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;"), remap = false)
    private InteractionResult deploy(UseOnContext context) { return ShopDeployments.use(context); }
}
