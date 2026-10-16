package dev.minestrike.skins;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import java.util.List;

public final class PlayerShape {
    public static final float BODY_WIDTH = .78f, HEAD_SCALE = .82f;
    public static final int NONE = 0, BODY = 1, HEAD = 2, HAT = 3, ARMOR_HEAD = 4;

    public interface Part {
        int minestrike$shape();
        void minestrike$shape(int shape);
    }

    public static void tag(ModelPart root) {
        for (String name : List.of("body", "jacket", "right_arm", "left_arm", "right_sleeve",
                "left_sleeve", "right_leg", "left_leg", "right_pants", "left_pants"))
            set(root.m_171324_(name), BODY);
        set(root.m_171324_("head"), HEAD);
        set(root.m_171324_("hat"), HAT);
    }

    public static void armor(HumanoidModel<?> model, boolean player) {
        for (ModelPart part : List.of(model.f_102810_, model.f_102811_, model.f_102812_, model.f_102813_, model.f_102814_))
            set(part, player ? BODY : NONE);
        set(model.f_102808_, player ? ARMOR_HEAD : NONE);
        set(model.f_102809_, player ? ARMOR_HEAD : NONE);
    }

    private static void set(ModelPart part, int shape) { ((Part)(Object)part).minestrike$shape(shape); }

    public static void narrowPivot(ModelPart part, PoseStack pose, int shape) {
        if (shape == NONE) return;
        pose.m_85837_(part.f_104200_ * (BODY_WIDTH - 1) / 16, 0, part.f_104202_ * (BODY_WIDTH - 1) / 16);
    }

    public static void scale(PoseStack pose, int shape) {
        if (shape == BODY) pose.m_85841_(BODY_WIDTH, 1, BODY_WIDTH);
        else if (shape != NONE) pose.m_85841_(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
    }

    public static void head(PoseStack.Pose pose, VertexConsumer out, int light, int overlay,
                            float red, float green, float blue, float alpha, boolean hat) {
        for (RoundHeadMesh.Vertex v : hat ? RoundHeadMesh.HAT : RoundHeadMesh.HEAD) {
            out.m_252986_(pose.m_252922_(), v.x() / 16, v.y() / 16, v.z() / 16)
                    .m_85950_(red, green, blue, alpha).m_7421_(v.u(), v.v())
                    .m_86008_(overlay).m_85969_(light)
                    .m_252939_(pose.m_252943_(), v.nx(), v.ny(), v.nz()).m_5752_();
        }
    }

    private PlayerShape() {}
}
