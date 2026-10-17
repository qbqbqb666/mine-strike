package dev.minestrike.skins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class MatchMovement {
    public static final int RESPONSE_TICKS = 2;
    public static final double SPRINT_FACTOR = 1 + (double).3f;
    private static final UUID SPRINT = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    public static boolean eligible(Player player) {
        return AgentPlayers.active(player) && player.m_6084_() && player.m_20096_()
                && !player.m_20069_() && !player.m_20077_() && !player.m_6147_()
                && !player.m_20159_() && !player.m_21255_() && !player.m_6069_()
                && !player.m_150110_().f_35935_ && !player.m_147223_() && player.m_6113_() > 0;
    }
    public static double speed(Player player) {
        var attribute = player.m_21051_(Attributes.f_22279_);
        var sprint = attribute == null ? null : attribute.m_22111_(SPRINT);
        return speed(player.m_6113_(), sprint == null ? 0 : sprint.m_22218_());
    }
    public static double speed(double actual, double sprintBonus) { return actual / (1 + sprintBonus) * SPRINT_FACTOR; }
    public static Vec3 target(Vec3 input, float yaw, double speed, float friction) {
        double length = Math.hypot(input.f_82479_, input.f_82481_);
        if (length < 1.0E-7) return Vec3.f_82478_;
        double strength = Math.min(1, Math.max(Math.abs(input.f_82479_), Math.abs(input.f_82481_)));
        double drag = friction * .91f;
        double scale = speed * .21600002f / (friction * friction * friction) / (1 - drag) * strength / length;
        double angle = Math.toRadians(yaw), sin = Math.sin(angle), cos = Math.cos(angle);
        return new Vec3((input.f_82479_ * cos - input.f_82481_ * sin) * scale, 0,
                (input.f_82481_ * cos + input.f_82479_ * sin) * scale);
    }
    public static final class State {
        private Vec3 command = Vec3.f_82478_, target = Vec3.f_82478_, start = Vec3.f_82478_, retained = Vec3.f_82478_;
        private int elapsed = RESPONSE_TICKS;
        private boolean initialized;
        public void reset() { command = target = start = retained = Vec3.f_82478_; elapsed = RESPONSE_TICKS; initialized = false; }
        public Vec3 move(Vec3 actual, Vec3 desired) {
            if (actual.f_82479_ == 0 && actual.f_82481_ == 0 && retained.m_82556_() > 0) reset();
            if (!initialized) {
                command = target = start = retained = new Vec3(actual.f_82479_, 0, actual.f_82481_);
                initialized = true;
            }
            if (target.m_82546_(desired).m_82556_() > 1.0E-12) { target = desired; start = command; elapsed = 0; }
            elapsed = Math.min(RESPONSE_TICKS, elapsed + 1);
            command = start.m_165921_(target, elapsed / (double)RESPONSE_TICKS);
            return new Vec3(actual.f_82479_ - retained.f_82479_ + command.f_82479_, actual.f_82480_,
                    actual.f_82481_ - retained.f_82481_ + command.f_82481_);
        }
        public void finish(Vec3 actual, float friction) {
            double x = actual.f_82479_ == 0 ? 0 : command.f_82479_;
            double z = actual.f_82481_ == 0 ? 0 : command.f_82481_;
            if (x != command.f_82479_ || z != command.f_82481_) {
                command = new Vec3(x, 0, z); target = command; start = command; elapsed = RESPONSE_TICKS;
            }
            retained = new Vec3(x, 0, z).m_82490_(friction * .91f);
        }
    }
    private MatchMovement() {}
}
