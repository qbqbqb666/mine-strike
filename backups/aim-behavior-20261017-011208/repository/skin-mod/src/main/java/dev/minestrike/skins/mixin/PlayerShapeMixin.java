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
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
import java.util.Map;

public final class PlayerShapeMixin {
    @Mixin(value = PlayerModel.class, remap = false)
    public static abstract class Model {
        @Inject(method = "<init>", at = @At("RETURN"))
        private void minestrike$model(ModelPart root, boolean slim, CallbackInfo ci) { PlayerShape.tag(root); }

        @Inject(method = "m_6002_", at = @At("RETURN"))
        private void minestrike$hand(HumanoidArm arm, PoseStack pose, CallbackInfo ci) { PlayerShape.hand(pose); }
    }

    @Mixin(value = ModelPart.class, remap = false, priority = 900)
    public static abstract class Part implements PlayerShape.Part {
        @Shadow public boolean f_104207_, f_233556_;
        @Shadow @Final private List<ModelPart.Cube> f_104212_;
        @Shadow @Final private Map<String, ModelPart> f_104213_;
        @Shadow public abstract void m_104299_(PoseStack pose);
        @Shadow private void m_104290_(PoseStack.Pose pose, VertexConsumer out, int light, int overlay,
                                     float red, float green, float blue, float alpha) { throw new AssertionError(); }
        @Unique private int minestrike$shape;
        @Unique private static int minestrike$logged;
        @Override public int minestrike$shape() { return minestrike$shape; }
        @Override public void minestrike$shape(int shape) { minestrike$shape = shape; }

        @Inject(method = "m_104299_", at = @At("HEAD"))
        private void minestrike$pivot(PoseStack pose, CallbackInfo ci) {
            PlayerShape.narrowPivot((ModelPart)(Object)this, pose, minestrike$shape);
        }

        @Inject(method = "m_104306_", at = @At("HEAD"), cancellable = true)
        private void minestrike$render(PoseStack pose, VertexConsumer out, int light, int overlay,
                                      float red, float green, float blue, float alpha, CallbackInfo ci) {
            if (minestrike$shape == PlayerShape.NONE) return;
            ci.cancel();
            if (!f_104207_ || (f_104212_.isEmpty() && f_104213_.isEmpty())) return;
            if ((minestrike$logged & (1 << minestrike$shape)) == 0) {
                minestrike$logged |= 1 << minestrike$shape;
                org.slf4j.LoggerFactory.getLogger("minestrike_skins").info("[player-shape] shape={} route=scoped-modelpart bodyScale={} torsoWidth={} armScale={} bodyHeight={} headScale={} head=cube",
                        minestrike$shape, PlayerShape.BODY_WIDTH, PlayerShape.TORSO_WIDTH, PlayerShape.ARM_WIDTH, PlayerShape.BODY_HEIGHT, PlayerShape.HEAD_SCALE);
            }
            pose.m_85836_();
            try {
                m_104299_(pose);
                PlayerShape.scale(pose, minestrike$shape);
                if (!f_233556_) m_104290_(pose.m_85850_(), out, light, overlay, red, green, blue, alpha);
                for (ModelPart child : f_104213_.values())
                    child.m_104306_(pose, out, light, overlay, red, green, blue, alpha);
            } finally {
                pose.m_85849_();
            }
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
