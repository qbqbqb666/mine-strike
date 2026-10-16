package dev.minestrike.skins.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer;
import com.tacz.guns.client.gameplay.LocalPlayerReload;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import dev.minestrike.skins.*;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class TaczReloadViewMixin {
    @Mixin(value = LocalPlayerReload.class, remap = false)
    public static abstract class Reload {
        @Inject(method = "doReload", at = @At("HEAD"))
        private void minestrike$reload(IGun gun, GunDisplayInstance display, GunData data, ItemStack stack, CallbackInfo ci) { TaczReloadView.start(); }
        @Inject(method = "cancelReload(Lcom/tacz/guns/client/resource/GunDisplayInstance;)V", at = @At("TAIL"))
        private void minestrike$cancel(GunDisplayInstance display, CallbackInfo ci) { TaczReloadView.cancel(); }
    }
    @Mixin(value = AnimateGeoItemRenderer.class, remap = false)
    public static abstract class WorldCamera {
        @ModifyArg(method = "applyLevelCameraAnimation(Lnet/minecraftforge/client/event/ViewportEvent$ComputeCameraAngles;Lnet/minecraft/world/item/ItemStack;F)V",
                at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/math/MathUtil;multiplyQuaternion(Lorg/joml/Quaternionf;F)Lorg/joml/Quaternionf;"), index = 1)
        private float minestrike$camera(float multiplier) {
            return (Object)this instanceof GunItemRendererWrapper ? multiplier * (1 - TaczReloadView.weight()) : multiplier;
        }
    }
    @Mixin(value = GunItemRendererWrapper.class, remap = false)
    public static abstract class HandCamera {
        @ModifyArg(method = "lambda$applyItemInHandCameraAnimation$3",
                at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/math/MathUtil;multiplyQuaternion(Lorg/joml/Quaternionf;F)Lorg/joml/Quaternionf;"), index = 1)
        private static float minestrike$camera(float multiplier) { return multiplier * (1 - TaczReloadView.weight()); }
    }
    @Mixin(value = FirstPersonRenderGunEvent.class, remap = false)
    public static abstract class Alignment {
        @Redirect(method = "applyFirstPersonGunTransform", at = @At(value = "INVOKE",
                target = "Lcom/tacz/guns/client/event/FirstPersonRenderGunEvent;applyAnimationConstraintTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/model/BedrockGunModel;F)V"))
        private static void minestrike$align(PoseStack pose, BedrockGunModel model, float aiming) {
            Matrix4f before = new Matrix4f(pose.m_85850_().m_252922_());
            FirstPersonRenderGunEvent.applyAnimationConstraintTransform(pose, model, aiming);
            ReloadAlignment.apply(pose, before, TaczReloadView.anchor(model), TaczReloadView.weight());
        }
    }
    private TaczReloadViewMixin() {}
}
