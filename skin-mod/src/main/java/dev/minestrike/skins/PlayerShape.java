package dev.minestrike.skins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import java.util.List;

public final class PlayerShape {
    public static final float BODY_WIDTH = PlayerMetrics.WIDTH, ARM_WIDTH = PlayerMetrics.WIDTH,
            BODY_HEIGHT = PlayerMetrics.LENGTH, HEAD_SCALE = PlayerMetrics.HEAD;
    public static final int NONE = 0, BODY = 1, HEAD = 2, HAT = 3, ARMOR_HEAD = 4, ARM = 5;

    public interface Part {
        int minestrike$shape();
        void minestrike$shape(int shape);
    }

    public static void tag(ModelPart root) {
        for (String name : List.of("body", "jacket", "right_leg", "left_leg", "right_pants", "left_pants"))
            set(root.m_171324_(name), BODY);
        for (String name : List.of("right_arm", "left_arm", "right_sleeve", "left_sleeve"))
            set(root.m_171324_(name), ARM);
        set(root.m_171324_("head"), HEAD);
        set(root.m_171324_("hat"), HAT);
    }

    public static void armor(HumanoidModel<?> model, boolean player) {
        for (ModelPart part : List.of(model.f_102810_, model.f_102813_, model.f_102814_))
            set(part, player ? BODY : NONE);
        set(model.f_102811_, player ? ARM : NONE);
        set(model.f_102812_, player ? ARM : NONE);
        set(model.f_102808_, player ? ARMOR_HEAD : NONE);
        set(model.f_102809_, player ? ARMOR_HEAD : NONE);
    }

    private static void set(ModelPart part, int shape) { ((Part)(Object)part).minestrike$shape(shape); }

    public static void narrowPivot(ModelPart part, PoseStack pose, int shape) {
        if (shape == NONE) return;
        pose.m_85837_(pivotOffset(part.f_104200_, shape) / 16, verticalOffset(part.f_104201_) / 16,
                part.f_104202_ * (BODY_WIDTH - 1) / 16);
    }

    public static float pivotOffset(float x, int shape) {
        return x * (BODY_WIDTH - 1) - (shape == ARM ? Math.signum(x) * (BODY_WIDTH - ARM_WIDTH) : 0);
    }

    public static float verticalOffset(float y) {
        return (24 - y) * (1 - BODY_HEIGHT);
    }

    public static void hand(PoseStack pose) {
        pose.m_85837_(0, 10 * (BODY_HEIGHT - 1) / 16, 0);
    }

    public static void scale(PoseStack pose, int shape) {
        if (shape == BODY) pose.m_85841_(BODY_WIDTH, BODY_HEIGHT, BODY_WIDTH);
        else if (shape == ARM) pose.m_85841_(ARM_WIDTH, BODY_HEIGHT, ARM_WIDTH);
        else if (shape != NONE) pose.m_85841_(HEAD_SCALE, HEAD_SCALE, HEAD_SCALE);
    }

    private PlayerShape() {}
}
