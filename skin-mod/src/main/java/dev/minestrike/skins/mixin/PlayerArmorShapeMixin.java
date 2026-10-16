package dev.minestrike.skins.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.minestrike.skins.PlayerShape;
import me.xjqsh.lesraisinsarmor.client.renderer.BedrockArmorRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;
import java.util.IdentityHashMap;
import java.util.Map;

@Mixin(value = GeoArmorRenderer.class, remap = false)
public abstract class PlayerArmorShapeMixin {
    @Shadow protected Entity currentEntity;
    @Shadow protected GeoBone head, body, rightArm, leftArm, rightLeg, leftLeg, rightBoot, leftBoot;
    @Unique private final Map<GeoBone, float[]> minestrike$saved = new IdentityHashMap<>();

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V", at = @At("HEAD"))
    private void minestrike$fit(PoseStack pose, Item item, GeoBone bone, RenderType type, MultiBufferSource buffers,
                               VertexConsumer vertices, boolean reRender, float partial, int light, int overlay,
                               float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (!(currentEntity instanceof Player) || !((Object)this instanceof BedrockArmorRenderer)
                || (bone != head && bone != body && bone != rightArm && bone != leftArm && bone != rightLeg
                && bone != leftLeg && bone != rightBoot && bone != leftBoot)) return;
        minestrike$saved.put(bone, new float[]{bone.getPosX(), bone.getPosY(), bone.getPosZ(), bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()});
        boolean arm = bone == rightArm || bone == leftArm;
        float scale = bone == head ? PlayerShape.HEAD_SCALE : bone == body ? PlayerShape.TORSO_WIDTH
                : arm ? PlayerShape.ARM_WIDTH : PlayerShape.BODY_WIDTH;
        bone.setPosX(bone.getPosX() - PlayerShape.pivotOffset(bone.getPivotX() - bone.getPosX(), arm ? PlayerShape.ARM : PlayerShape.BODY));
        bone.setPosY(bone.getPosY() + (bone.getPivotY() + bone.getPosY()) * (PlayerShape.BODY_HEIGHT - 1));
        bone.setPosZ(bone.getPosZ() + (PlayerShape.BODY_WIDTH - 1) * (bone.getPivotZ() + bone.getPosZ()));
        bone.setScaleX(bone.getScaleX() * scale);
        bone.setScaleY(bone.getScaleY() * (bone == head ? scale : PlayerShape.BODY_HEIGHT));
        bone.setScaleZ(bone.getScaleZ() * (bone == head ? PlayerShape.HEAD_SCALE : PlayerShape.BODY_WIDTH));
    }

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V", at = @At("RETURN"))
    private void minestrike$restore(PoseStack pose, Item item, GeoBone bone, RenderType type, MultiBufferSource buffers,
                                   VertexConsumer vertices, boolean reRender, float partial, int light, int overlay,
                                   float red, float green, float blue, float alpha, CallbackInfo ci) {
        float[] state = minestrike$saved.remove(bone);
        if (state == null) return;
        bone.setPosX(state[0]); bone.setPosY(state[1]); bone.setPosZ(state[2]);
        bone.setScaleX(state[3]); bone.setScaleY(state[4]); bone.setScaleZ(state[5]);
    }
}
