package dev.minestrike.skins.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minestrike.skins.PlayerShape;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class PlayerShapeMixin {
    @Mixin(value = PlayerModel.class, remap = false)
    public static abstract class Model {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void minestrike$model(ModelPart root, boolean slim, CallbackInfo ci) { PlayerShape.tag(root); }
    }

    @Mixin(value = ModelPart.class, remap = false)
    public static abstract class Part implements PlayerShape.Part {
        @Unique private int minestrike$shape;
        @Override public int minestrike$shape() { return minestrike$shape; }
        @Override public void minestrike$shape(int shape) { minestrike$shape = shape; }

        @Inject(method = "m_104299_", at = @At("HEAD"))
        private void minestrike$pivot(PoseStack pose, CallbackInfo ci) {
            PlayerShape.narrowPivot((ModelPart)(Object)this, pose, minestrike$shape);
        }

        @Inject(method = "m_104306_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;m_104299_(Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER))
        private void minestrike$proportions(PoseStack pose, VertexConsumer out, int light, int overlay,
                                            float red, float green, float blue, float alpha, CallbackInfo ci) {
            PlayerShape.scale(pose, minestrike$shape);
        }

        @Inject(method = "m_104290_", at = @At("HEAD"), cancellable = true)
        private void minestrike$roundHead(PoseStack.Pose pose, VertexConsumer out, int light, int overlay,
                                         float red, float green, float blue, float alpha, CallbackInfo ci) {
            if (minestrike$shape != PlayerShape.HEAD && minestrike$shape != PlayerShape.HAT) return;
            PlayerShape.head(pose, out, light, overlay, red, green, blue, alpha, minestrike$shape == PlayerShape.HAT);
            ci.cancel();
        }
    }

    @Mixin(value = HumanoidArmorLayer.class, remap = false)
    public static abstract class Armor {
        @Inject(method = "m_117118_", at = @At("HEAD"))
        private void minestrike$armor(PoseStack pose, MultiBufferSource buffers, LivingEntity entity,
                                     EquipmentSlot slot, int light, HumanoidModel<?> model, CallbackInfo ci) {
            PlayerShape.armor(model, entity instanceof Player);
        }
    }

    @Mixin(value = CustomHeadLayer.class, remap = false)
    public static abstract class HeadItem {
        @Inject(method = "m_6494_(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;ILnet/minecraft/world/entity/LivingEntity;FFFFFF)V",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;m_104299_(Lcom/mojang/blaze3d/vertex/PoseStack;)V", shift = At.Shift.AFTER))
        private void minestrike$headItem(PoseStack pose, MultiBufferSource buffers, int light, LivingEntity entity,
                                        float walk, float speed, float partial, float age, float yaw, float pitch, CallbackInfo ci) {
            if (entity instanceof Player) PlayerShape.scale(pose, PlayerShape.ARMOR_HEAD);
        }
    }

    private PlayerShapeMixin() {}
}
