package dev.minestrike.skins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.joml.Quaternionf;
import java.util.List;

public final class ReloadAlignment {
    public static Matrix4f path(List<BedrockPart> parts, boolean animated) {
        Matrix4f result = new Matrix4f();
        for (BedrockPart part : parts) {
            if (animated) result.translate(part.offsetX, part.offsetY, part.offsetZ);
            result.translate(part.x / 16, part.y / 16, part.z / 16)
                    .rotateZ(part.zRot).rotateY(part.yRot).rotateX(part.xRot);
            if (animated) result.rotate(part.additionalQuaternion).scale(part.xScale, part.yScale, part.zScale);
        }
        return result;
    }
    public static Matrix4f correction(List<BedrockPart> parts) {
        Matrix4f animated = path(parts, true);
        if (!animated.isFinite() || Math.abs(animated.determinant()) < 1e-8) return null;
        Matrix4f result = path(parts, false).mul(animated.invert());
        return result.isFinite() ? result : null;
    }
    public static Matrix4f blend(Matrix4f from, Matrix4f to, float weight) {
        if (weight >= 1) return new Matrix4f(to);
        if (weight <= 0) return new Matrix4f(from);
        Vector3f position = from.getTranslation(new Vector3f()).lerp(to.getTranslation(new Vector3f()), weight);
        Vector3f scale = from.getScale(new Vector3f()).lerp(to.getScale(new Vector3f()), weight);
        Quaternionf rotation = from.getUnnormalizedRotation(new Quaternionf()).normalize()
                .slerp(to.getUnnormalizedRotation(new Quaternionf()).normalize(), weight);
        return new Matrix4f().translationRotateScale(position, rotation, scale);
    }
    public static void apply(PoseStack pose, Matrix4f before, List<BedrockPart> anchor, float weight) {
        if (weight <= 0 || anchor == null || anchor.isEmpty()) return;
        Matrix4f correction = correction(anchor);
        if (correction == null) return;
        Matrix4f current = pose.m_85850_().m_252922_();
        if (!current.isFinite() || Math.abs(current.determinant()) < 1e-8) return;
        Matrix4f target = new Matrix4f(before).mul(correction);
        Matrix4f delta = new Matrix4f(current).invert().mul(target);
        Matrix4f result = weight >= 1 ? target : new Matrix4f(current).mul(blend(new Matrix4f(), delta, weight));
        if (!result.isFinite() || Math.abs(result.determinant()) < 1e-8) return;
        pose.m_85850_().m_252922_().set(result);
        pose.m_85850_().m_252943_().set(new Matrix3f(result).invert().transpose());
    }
    private ReloadAlignment() {}
}
