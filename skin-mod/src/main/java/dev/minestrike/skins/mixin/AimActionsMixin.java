package dev.minestrike.skins.mixin;

import com.tacz.guns.client.gameplay.LocalPlayerAim;
import com.tacz.guns.client.gameplay.LocalPlayerDraw;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClickEventHandler;
import dev.minestrike.skins.AimActions;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class AimActionsMixin {
    @Mixin(value = LocalPlayerAim.class, remap = false)
    public static abstract class Input {
        @ModifyVariable(method = "aim", at = @At("HEAD"), argsOnly = true, ordinal = 0)
        private boolean minestrike$input(boolean value) { return AimActions.request(value, true); }
    }
    @Mixin(value = AimKey.class, remap = false)
    public static abstract class Key {
        @Redirect(method = {"onAimPress", "onAimControllerPress", "onAimHoldingPreInput"}, at = @At(value = "INVOKE",
                target = "Lcom/tacz/guns/api/client/gameplay/IClientPlayerGunOperator;isAim()Z"))
        private static boolean minestrike$intent(IClientPlayerGunOperator operator) { return AimActions.inputView(operator.isAim()); }
    }
    @Mixin(value = LocalPlayerDraw.class, remap = false)
    public static abstract class Draw {
        @Inject(method = "draw", at = @At("HEAD"))
        private void minestrike$draw(ItemStack stack, CallbackInfo ci) { AimActions.clear(); }
    }
    @Mixin(value = ClientEventHandler.class, remap = false)
    public static abstract class SuperbShot {
        @Inject(method = "handleClientShoot", at = @At("TAIL"))
        private void minestrike$shot(CallbackInfo ci) { AimActions.superbShot(); }
    }
    @Mixin(value = ClickEventHandler.class, remap = false)
    public static abstract class SuperbInput {
        @Inject(method = "handleWeaponZoomPress", at = @At("HEAD"), cancellable = true)
        private void minestrike$press(Player player, ItemStack stack, CallbackInfo ci) {
            if (!AimActions.request(true, false)) ci.cancel();
        }
        @Inject(method = "handleWeaponZoomRelease", at = @At("HEAD"))
        private void minestrike$release(CallbackInfo ci) { AimActions.request(false, false); }
    }
    private AimActionsMixin() {}
}
