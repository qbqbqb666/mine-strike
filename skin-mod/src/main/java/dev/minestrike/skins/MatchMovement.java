package dev.minestrike.skins;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

public final class MatchMovement {
    public static final int RESPONSE_TICKS = 2;
    public static final double SPRINT_FACTOR = 1 + (double).3f;
    private static final UUID SPRINT = UUID.fromString("662A6B8D-DA3E-4C1C-8813-96EA6097278D");
    public static boolean eligible(Player player) { return applicable(player) && player.m_20096_(); }
    public static boolean applicable(Player player) {
        return AgentPlayers.active(player) && player.m_6084_()
                && !player.m_20069_() && !player.m_20077_() && !player.m_6147_()
                && !player.m_20159_() && !player.m_21255_() && !player.m_6069_()
                && !player.m_150110_().f_35935_ && !player.m_147223_() && player.m_6113_() > 0;
    }
    public static boolean running(Player player) {
        return applicable(player) && player.f_20902_ > 1.0E-5 && !player.m_6144_() && !player.m_6117_()
                && !player.m_21023_(net.minecraft.world.effect.MobEffects.f_19610_)
                && (player.m_150110_().f_35936_ || player.m_36324_().m_38702_() > 6) && !aiming(player);
    }
    public static boolean aiming(Player player) {
        var item = player.m_21205_().m_41720_();
        if (item instanceof com.tacz.guns.api.item.IGun) {
            var operator = com.tacz.guns.api.entity.IGunOperator.fromLivingEntity(player);
            if (operator.getSynIsAiming() || operator.getSynAimingProgress() > 0) return true;
        } else if (item instanceof com.atsuishio.superbwarfare.item.gun.GunItem
                && com.atsuishio.superbwarfare.data.gun.GunData.from(player.m_21205_()).zooming.get()) return true;
        return player.m_9236_().f_46443_ && Client.aiming(player);
    }
    public static float airAcceleration(float original, boolean nativeSprinting) {
        return nativeSprinting ? original : (float)(original * SPRINT_FACTOR);
    }
    public static final class Jumps {
        private boolean jumped, chain;
        public void start(boolean running) { jumped = true; chain = running; }
        public void begin(boolean allowed, boolean jumpHeld, boolean running) {
            if (!allowed) jumped = chain = false;
            else if (!jumpHeld || !running) chain = false;
        }
        public boolean nativeMovement() { return jumped || chain; }
        public void finish() { jumped = false; }
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
    public static double movementFov(double speed, double walkingSpeed) {
        double value = (speed / walkingSpeed + 1) / 2;
        return walkingSpeed == 0 || !Double.isFinite(value) ? 1 : value;
    }
    public static float fov(float current, double originalMovement, double unifiedMovement) {
        return (float)(current * unifiedMovement / originalMovement);
    }
    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = MineStrikeSkins.ID,
            value = net.minecraftforge.api.distmarker.Dist.CLIENT)
    public static final class Client {
        private record InputContext(Object player, Object level) {}
        private static final UnlockKeys unlockKeys = new UnlockKeys();
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void unlock(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
            var mc = net.minecraft.client.Minecraft.m_91087_();
            var global = com.phasetranscrystal.fpsmatch.common.client.FPSMClient.getGlobalData();
            Object key = mc.f_91074_ != null && mc.f_91073_ != null && global.equalsGame("cs") && !global.isSpectator()
                    ? new InputContext(mc.f_91074_, mc.f_91073_) : null;
            boolean frozen = com.phasetranscrystal.blockoffensive.client.data.CSClientData.isWaiting
                    || com.phasetranscrystal.blockoffensive.client.data.CSClientData.isPause;
            boolean ready = event.phase == net.minecraftforge.event.TickEvent.Phase.END && mc.m_91302_() && mc.f_91080_ == null;
            if (!unlockKeys.update(key, frozen, ready)) return;
            var options = mc.f_91066_;
            for (var mapping : new net.minecraft.client.KeyMapping[]{options.f_92085_, options.f_92086_, options.f_92087_, options.f_92088_, options.f_92089_})
                mapping.m_7249_(physical(mapping, mc.m_91268_().m_85439_()));
        }
        private static boolean physical(net.minecraft.client.KeyMapping mapping, long window) {
            if (!mapping.getKeyConflictContext().isActive() || !mapping.getKeyModifier().isActive(mapping.getKeyConflictContext())) return false;
            var key = mapping.getKey(); int code = key.m_84873_();
            if (code < 0) return false;
            return switch (key.m_84868_()) {
                case KEYSYM -> com.mojang.blaze3d.platform.InputConstants.m_84830_(window, code);
                case MOUSE -> org.lwjgl.glfw.GLFW.glfwGetMouseButton(window, code) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
                default -> false;
            };
        }
        public static boolean aiming(Player player) {
            var item = player.m_21205_().m_41720_();
            if (player instanceof net.minecraft.client.player.LocalPlayer local && item instanceof com.tacz.guns.api.item.IGun) {
                var aim = com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator.fromLocalPlayer(local).getDataHolder();
                return aim.clientIsAiming || aim.clientAimingProgress > 0
                        || com.tacz.guns.client.gameplay.LocalPlayerDataHolder.oldAimingProgress > 0;
            }
            return item instanceof com.atsuishio.superbwarfare.item.gun.GunItem
                    && (com.atsuishio.superbwarfare.event.ClientEventHandler.zoom
                    || com.atsuishio.superbwarfare.event.ClientEventHandler.zoomTime > 0);
        }
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority = net.minecraftforge.eventbus.api.EventPriority.LOWEST)
        public static void fov(net.minecraftforge.client.event.ComputeFovModifierEvent event) {
            var player = event.getPlayer();
            if (!(player instanceof net.minecraft.client.player.LocalPlayer) || !applicable(player)
                    || player.m_6144_() || Math.hypot(player.f_20900_, player.f_20902_) < 1.0E-5 || aiming(player)) return;
            var item = player.m_21205_().m_41720_();
            double original, unified;
            if (item instanceof com.tacz.guns.api.item.gun.AbstractGunItem
                    && com.tacz.guns.config.client.RenderConfig.DISABLE_MOVEMENT_ATTRIBUTE_FOV.get()) {
                original = player.m_20142_() ? 1.15f : 1;
                unified = 1.15f;
            } else {
                double walking = player.m_150110_().m_35947_();
                original = movementFov(player.m_21133_(Attributes.f_22279_), walking);
                unified = movementFov(speed(player), walking);
            }
            event.setNewFovModifier(MatchMovement.fov(event.getNewFovModifier(), original, unified));
        }
    }
    public static final class UnlockKeys {
        private Object context;
        private boolean locked, pending;
        public boolean update(Object key, boolean frozen, boolean ready) {
            if (!java.util.Objects.equals(context, key)) { context = key; locked = pending = false; }
            if (key == null) return false;
            if (frozen) { locked = true; pending = false; return false; }
            if (locked) { pending = true; locked = false; }
            if (pending && ready) { pending = false; return true; }
            return false;
        }
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
