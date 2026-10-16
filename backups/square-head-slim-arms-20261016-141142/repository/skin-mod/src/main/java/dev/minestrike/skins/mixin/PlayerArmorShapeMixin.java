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
        minestrike$saved.put(bone, new float[]{bone.getPosX(), bone.getPosZ(), bone.getScaleX(), bone.getScaleY(), bone.getScaleZ()});
        float scale = bone == head ? PlayerShape.HEAD_SCALE : PlayerShape.BODY_WIDTH;
        bone.setPosX(bone.getPosX() + (1 - PlayerShape.BODY_WIDTH) * (bone.getPivotX() - bone.getPosX()));
        bone.setPosZ(bone.getPosZ() + (PlayerShape.BODY_WIDTH - 1) * (bone.getPivotZ() + bone.getPosZ()));
        bone.setScaleX(bone.getScaleX() * scale);
        bone.setScaleY(bone.getScaleY() * (bone == head ? scale : 1));
        bone.setScaleZ(bone.getScaleZ() * scale);
    }

    @Inject(method = "renderRecursively(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/Item;Lsoftware/bernie/geckolib/cache/object/GeoBone;Lnet/minecraft/client/renderer/RenderType;Lnet/minecraft/client/renderer/MultiBufferSource;Lcom/mojang/blaze3d/vertex/VertexConsumer;ZFIIFFFF)V", at = @At("RETURN"))
    private void minestrike$restore(PoseStack pose, Item item, GeoBone bone, RenderType type, MultiBufferSource buffers,
                                   VertexConsumer vertices, boolean reRender, float partial, int light, int overlay,
                                   float red, float green, float blue, float alpha, CallbackInfo ci) {
        float[] state = minestrike$saved.remove(bone);
        if (state == null) return;
        bone.setPosX(state[0]); bone.setPosZ(state[1]);
        bone.setScaleX(state[2]); bone.setScaleY(state[3]); bone.setScaleZ(state[4]);
    }
}
