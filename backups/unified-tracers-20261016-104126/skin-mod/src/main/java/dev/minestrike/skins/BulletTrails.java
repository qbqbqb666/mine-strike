package dev.minestrike.skins;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class BulletTrails {
    private static final ArrayDeque<TrailGeometry.Segment> segments = new ArrayDeque<>();
    private static Object level;

    public static Vec3 begin(Entity bullet, Entity owner, boolean supported) {
        Minecraft mc = Minecraft.m_91087_();
        if (!supported || !TracerConfig.ENABLED.get() || !bullet.m_9236_().f_46443_ || mc.f_91073_ == null) return null;
        if (owner == mc.f_91074_ && !TracerConfig.OWN.get()) return null;
        return bullet.m_20182_();
    }

    // Observe the completed native movement. Never change the bullet or its hit result.
    public static boolean record(Entity bullet, Vec3 from) {
        if (from == null || !TracerConfig.ENABLED.get()) return false;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || bullet.m_9236_() != mc.f_91073_) return false;
        if (level != mc.f_91073_) { segments.clear(); level = mc.f_91073_; }
        Vec3 to = bullet.m_20182_();
        TrailGeometry.Segment original = new TrailGeometry.Segment(point(from), point(to), mc.f_91073_.m_46467_());
        if (!original.from().finite() || !original.to().finite() || from.m_82557_(to) < 1e-10) return false;
        Vec3 camera = mc.f_91063_.m_109153_().m_90583_();
        if (TrailGeometry.distance(original, point(camera)) > TracerConfig.DISTANCE.get()) return false;
        BlockHitResult hit = mc.f_91073_.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
        boolean blocked = hit.m_6662_() != HitResult.Type.MISS;
        if (blocked) to = hit.m_82450_();
        segments.addLast(new TrailGeometry.Segment(point(from), point(to), original.born()));
        while (segments.size() > TracerConfig.BUDGET.get() * 4) segments.removeFirst();
        return blocked;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.m_91087_();
        if (!TracerConfig.ENABLED.get() || mc.f_91073_ == null || level != mc.f_91073_) {
            segments.clear(); level = mc.f_91073_; return;
        }
        double now = mc.f_91073_.m_46467_(), life = TracerConfig.LIFETIME.get() * 20;
        segments.removeIf(s -> now - s.born() >= life || now < s.born());
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { segments.clear(); level = null; }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || segments.isEmpty() || !TracerConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || level != mc.f_91073_) return;
        TrailGeometry.Point eye = point(event.getCamera().m_90583_());
        double now = mc.f_91073_.m_46467_() + event.getPartialTick();
        double life = TracerConfig.LIFETIME.get() * 20;
        List<TrailGeometry.Segment> visible = segments.stream()
                .filter(s -> now >= s.born() && now - s.born() < life && TrailGeometry.distance(s, eye) <= TracerConfig.DISTANCE.get())
                .sorted(Comparator.comparingDouble(s -> TrailGeometry.distance(s, eye)))
                .limit(TracerConfig.BUDGET.get()).toList();
        if (visible.isEmpty()) return;
        VertexConsumer vertices = Draw.BUFFERS.m_6299_(Draw.TYPE);
        Matrix4f matrix = event.getPoseStack().m_85850_().m_252922_();
        try {
            for (TrailGeometry.Segment segment : visible) {
                TrailGeometry.Ribbon ribbon = TrailGeometry.frame(segment, eye, now, life, TracerConfig.LENGTH.get(),
                        TracerConfig.WIDTH.get(), TracerConfig.NEAR_RADIUS.get(), TracerConfig.NEAR_BOOST.get());
                if (ribbon == null) continue;
                quad(vertices, matrix, ribbon, eye, 2.8, .18);
                quad(vertices, matrix, ribbon, eye, 1, 1);
            }
        } finally { Draw.BUFFERS.m_109912_(Draw.TYPE); }
    }

    private static void quad(VertexConsumer out, Matrix4f matrix, TrailGeometry.Ribbon r, TrailGeometry.Point eye, double scale, double alpha) {
        TrailGeometry.Point side = r.side().scale(r.width() * scale);
        TrailGeometry.Point head = r.head().sub(eye), tail = r.tail().sub(eye);
        vertex(out, matrix, tail.sub(side), 0);
        vertex(out, matrix, tail.add(side), 0);
        vertex(out, matrix, head.add(side), (int)(255 * r.alpha() * alpha));
        vertex(out, matrix, head.sub(side), (int)(255 * r.alpha() * alpha));
        TrailGeometry.Point view = eye.sub(r.head());
        if (view.length() < 1e-6) view = r.head().sub(r.tail());
        TrailGeometry.Point up = view.cross(r.side());
        up = up.scale(r.width() * scale / up.length());
        int headAlpha = (int)(255 * r.alpha() * alpha);
        vertex(out, matrix, head.sub(side).sub(up), headAlpha);
        vertex(out, matrix, head.add(side).sub(up), headAlpha);
        vertex(out, matrix, head.add(side).add(up), headAlpha);
        vertex(out, matrix, head.sub(side).add(up), headAlpha);
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, TrailGeometry.Point p, int alpha) {
        out.m_252986_(matrix, (float)p.x(), (float)p.y(), (float)p.z()).m_6122_(255, 237, 190, alpha).m_5752_();
    }
    private static TrailGeometry.Point point(Vec3 p) { return new TrailGeometry.Point(p.f_82479_, p.f_82480_, p.f_82481_); }

    private static final class Draw extends RenderType {
        private static final RenderType TYPE = new Draw();
        private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.m_109898_(new BufferBuilder(65536));
        private Draw() {
            super("minestrike_bullet_trails", DefaultVertexFormat.f_85815_, VertexFormat.Mode.QUADS, 65536, false, true,
                    () -> { f_173104_.m_110185_(); f_110139_.m_110185_(); f_110110_.m_110185_();
                        f_110113_.m_110185_(); f_110115_.m_110185_(); f_110123_.m_110185_(); },
                    () -> { f_110123_.m_110188_(); f_110115_.m_110188_(); f_110113_.m_110188_();
                        f_110110_.m_110188_(); f_110139_.m_110188_(); f_173104_.m_110188_(); });
        }
    }
    private BulletTrails() {}
}
