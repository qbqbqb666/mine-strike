package dev.minestrike.skins.mixin;

import com.atsuishio.superbwarfare.client.renderer.projectile.ProjectileEntityRenderer;
import com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.entity.EntityBulletRenderer;
import com.tacz.guns.entity.EntityKineticBullet;
import dev.minestrike.skins.BulletTrails;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class NativeBulletTrailsMixin {
    @Mixin(value = EntityBulletRenderer.class, remap = false)
    public static abstract class Tacz {
        @Inject(method = "renderTracerAmmo", at = @At("HEAD"), cancellable = true)
        private void minestrike$nativeTracer(EntityKineticBullet bullet, float[] color, float partialTick,
                                              PoseStack pose, int light, CallbackInfo ci) {
            if (BulletTrails.replacesNative(bullet)) ci.cancel();
        }
    }

    @Mixin(value = ProjectileEntityRenderer.class, remap = false)
    public static abstract class Superb {
        @Inject(method = "render(Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
        private void minestrike$nativeTracer(ProjectileEntity bullet, float yaw, float partialTick,
                                              PoseStack pose, MultiBufferSource buffers, int light, CallbackInfo ci) {
            if (BulletTrails.replacesNative(bullet)) ci.cancel();
        }
    }

    private NativeBulletTrailsMixin() {}
}
