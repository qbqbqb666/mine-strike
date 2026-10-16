package dev.minestrike.skins;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.tacz.guns.api.event.common.GunShootEvent;
import com.tacz.guns.client.renderer.item.GunItemRendererWrapper;
import com.tacz.guns.entity.EntityKineticBullet;
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
import net.minecraftforge.fml.LogicalSide;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class BulletTrails {
    private record Settings(double life, double width, double near, double boost, double distance,
                            int budget, boolean own, boolean tacz, boolean superb) {}
    private record FrameKey(TrailGeometry.Point eye, double now, long revision, Settings settings, boolean firstPerson) {}
    private record Visible(Entity bullet, TrailGeometry.Ribbon ribbon, double distance) {}
    private record VisiblePath(Entity bullet, List<Visible> pieces, double distance) {}
    private static final Map<Entity, TrailPath> paths = new LinkedHashMap<>();
    private static Object level;
    private static long revision;
    private static FrameKey preparedKey;
    private static List<Visible> prepared = List.of();
    private static Set<Entity> replacements = Set.of();
    private static final Map<Entity, Probe> probes = new LinkedHashMap<>();
    private static int shootEvents;
    private static long lastShotNanos;
    private static final class Probe {
        boolean spawned, ticked, recorded, spawnRecorded, clipped, zeroMove;
        int submissions;
        double maxSpanPixels, minShaderAlpha = 1;
    }

    @SubscribeEvent public static void shot(GunShootEvent event) {
        Minecraft mc = Minecraft.m_91087_();
        if (event.getLogicalSide() != LogicalSide.CLIENT || event.getShooter() != mc.f_91074_
                || !TracerConfig.ENABLED.get() || !TracerConfig.OWN.get()) return;
        if (level != mc.f_91073_) { clear(); level = mc.f_91073_; }
        shootEvents++;
        lastShotNanos = System.nanoTime();
    }

    private static Probe probe(Entity bullet) {
        if (!(bullet instanceof EntityKineticBullet tacz) || !managed(bullet)
                || tacz.m_19749_() != Minecraft.m_91087_().f_91074_) return null;
        if (level != Minecraft.m_91087_().f_91073_) { clear(); level = Minecraft.m_91087_().f_91073_; }
        if (probes.size() >= 256 && !probes.containsKey(bullet)) report();
        return probes.computeIfAbsent(bullet, ignored -> { lastShotNanos = System.nanoTime(); return new Probe(); });
    }

    public static void spawned(Entity bullet) {
        Probe probe = probe(bullet);
        if (probe != null) probe.spawned = true;
        if (!managed(bullet)) return;
        Vec3 from = bullet.m_20182_();
        recordPath(bullet, from, from.m_82549_(bullet.m_20184_()), true);
    }

    private static void report() {
        if (shootEvents == 0 && probes.isEmpty()) return;
        StringBuilder nativeMask = new StringBuilder(), recordedMask = new StringBuilder(), spawnMask = new StringBuilder(), drawnMask = new StringBuilder();
        int spawned = 0, ticked = 0, clipped = 0, zero = 0;
        double span = 0, shaderAlpha = 1;
        for (Map.Entry<Entity, Probe> entry : probes.entrySet()) {
            Probe p = entry.getValue();
            nativeMask.append(((EntityKineticBullet)entry.getKey()).isTracerAmmo() ? '1' : '0');
            recordedMask.append(p.recorded ? '1' : '0'); drawnMask.append(p.submissions > 0 ? '1' : '0');
            spawnMask.append(p.spawnRecorded ? '1' : '0');
            spawned += p.spawned ? 1 : 0; ticked += p.ticked ? 1 : 0;
            clipped += p.clipped ? 1 : 0; zero += p.zeroMove ? 1 : 0;
            span = Math.max(span, p.maxSpanPixels); shaderAlpha = Math.min(shaderAlpha, p.minShaderAlpha);
        }
        MineStrikeSkins.LOG.info("[tracers-burst] shootEvents={} entities={} spawned={} ticked={} clipped={} zeroMove={} nativeMask={} spawnRecordedMask={} recordedMask={} submittedMask={} maxSpanPixels={} minShaderAlpha={}",
                shootEvents, probes.size(), spawned, ticked, clipped, zero, nativeMask, spawnMask, recordedMask, drawnMask,
                String.format(Locale.ROOT, "%.2f", span), String.format(Locale.ROOT, "%.3f", shaderAlpha));
        probes.clear(); shootEvents = 0; lastShotNanos = 0;
    }

    public interface Source {
        boolean minestrike$eligible();
        Entity minestrike$owner();
    }

    private static boolean managed(Entity bullet) {
        Minecraft mc = Minecraft.m_91087_();
        if (!(bullet instanceof Source source) || !source.minestrike$eligible() || !TracerConfig.ENABLED.get()
                || mc.f_91073_ == null || bullet.m_9236_() != mc.f_91073_) return false;
        return source.minestrike$owner() != mc.f_91074_ || TracerConfig.OWN.get();
    }

    public static Vec3 begin(Entity bullet, boolean blocked) {
        Probe probe = probe(bullet);
        if (probe != null) probe.ticked = true;
        return !blocked && managed(bullet) ? bullet.m_20182_() : null;
    }

    public static boolean replacesNative(Entity bullet) {
        if (!managed(bullet) || level != Minecraft.m_91087_().f_91073_) return false;
        Minecraft mc = Minecraft.m_91087_();
        prepare(point(mc.f_91063_.m_109153_().m_90583_()), mc.f_91073_.m_46467_() + mc.m_91296_());
        return replacements.contains(bullet);
    }

    private static void clear() {
        paths.clear(); revision++;
        preparedKey = null; prepared = List.of(); replacements = Set.of();
        probes.clear(); shootEvents = 0; lastShotNanos = 0;
    }

    private static void prepare(TrailGeometry.Point eye, double now) {
        Settings settings = new Settings(TracerConfig.LIFETIME.get() * 20, TracerConfig.WIDTH.get(),
                TracerConfig.NEAR_RADIUS.get(), TracerConfig.NEAR_BOOST.get(), TracerConfig.DISTANCE.get(),
                TracerConfig.BUDGET.get(), TracerConfig.OWN.get(), TracerConfig.TACZ.get(), TracerConfig.SUPERB.get());
        boolean firstPerson = Minecraft.m_91087_().f_91066_.m_92176_().m_90612_();
        FrameKey key = new FrameKey(eye, now, revision, settings, firstPerson);
        if (key.equals(preparedKey)) return;
        List<VisiblePath> candidates = new ArrayList<>();
        for (Map.Entry<Entity, TrailPath> entry : paths.entrySet()) {
            if (!managed(entry.getKey())) continue;
            List<Visible> pieces = new ArrayList<>();
            double distance = Double.POSITIVE_INFINITY;
            for (TrailGeometry.Segment segment : entry.getValue().segments(firstPerson, now)) {
                if (TrailGeometry.distance(segment, eye) > settings.distance()) continue;
                TrailGeometry.Ribbon ribbon = TrailGeometry.frame(segment, eye, now, settings.life(),
                        settings.width(), settings.near(), settings.boost());
                if (ribbon == null) continue;
                double visibleDistance = TrailGeometry.closest(ribbon.tail(), ribbon.head(), eye).sub(eye).length();
                if (visibleDistance <= settings.distance()) {
                    pieces.add(new Visible(entry.getKey(), ribbon, visibleDistance));
                    distance = Math.min(distance, visibleDistance);
                }
            }
            if (!pieces.isEmpty()) candidates.add(new VisiblePath(entry.getKey(), pieces, distance));
        }
        candidates.sort(Comparator.comparingDouble(VisiblePath::distance));
        List<Visible> selectedPieces = new ArrayList<>();
        Set<Entity> selected = new HashSet<>();
        for (VisiblePath path : candidates) {
            if (selectedPieces.size() + path.pieces().size() > settings.budget()) continue;
            selectedPieces.addAll(path.pieces()); selected.add(path.bullet());
        }
        prepared = List.copyOf(selectedPieces);
        replacements = selected;
        preparedKey = key;
    }

    // Observe the completed native movement. Never change the bullet or its hit result.
    public static boolean record(Entity bullet, Vec3 from) {
        return from != null && recordPath(bullet, from, bullet.m_20182_(), false);
    }

    private static boolean recordPath(Entity bullet, Vec3 from, Vec3 to, boolean preview) {
        if (from == null || !TracerConfig.ENABLED.get()) return false;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || bullet.m_9236_() != mc.f_91073_) return false;
        if (level != mc.f_91073_) { clear(); level = mc.f_91073_; }
        boolean own = bullet instanceof Source source && source.minestrike$owner() == mc.f_91074_;
        TrailGeometry.Segment original = new TrailGeometry.Segment(point(from), point(to), mc.f_91073_.m_46467_(), own);
        Probe probe = probe(bullet);
        if (!original.from().finite() || !original.to().finite() || from.m_82557_(to) < 1e-10) {
            if (probe != null) probe.zeroMove = true;
            return false;
        }
        Vec3 camera = mc.f_91063_.m_109153_().m_90583_();
        if (TrailGeometry.distance(original, point(camera)) > TracerConfig.DISTANCE.get()) return false;
        BlockHitResult hit = mc.f_91073_.m_45547_(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
        boolean blocked = hit.m_6662_() != HitResult.Type.MISS;
        if (probe != null && blocked) probe.clipped = true;
        if (blocked) to = hit.m_82450_();
        if (from.m_82557_(to) < 1e-10) return blocked;
        TrailGeometry.Segment segment = new TrailGeometry.Segment(point(from), point(to), original.born(), own);
        TrailGeometry.Segment firstPerson = segment;
        if (own && bullet instanceof EntityKineticBullet tacz && mc.f_91066_.m_92176_().m_90612_()) {
            if (tacz.getFirstPersonRenderOffset() == null) {
                tacz.setFirstPersonRenderOffset(new Vector3f(GunItemRendererWrapper.muzzleRenderOffset));
                tacz.setCameraXRot(mc.f_91063_.m_109153_().m_90589_());
                tacz.setCameraYRot(mc.f_91063_.m_109153_().m_90590_());
            }
            Vector3f offset = new Vector3f(tacz.getFirstPersonRenderOffset())
                    .rotate(Axis.f_252495_.m_252977_(tacz.getCameraXRot()))
                    .rotate(Axis.f_252392_.m_252977_(tacz.getCameraYRot() + 180));
            firstPerson = TrailGeometry.firstPerson(segment, point(camera),
                    new TrailGeometry.Point(offset.x(), offset.y(), offset.z()));
            BlockHitResult visualHit = mc.f_91073_.m_45547_(new ClipContext(vec(firstPerson.from()), vec(firstPerson.to()),
                    ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, bullet));
            if (visualHit.m_6662_() != HitResult.Type.MISS)
                firstPerson = new TrailGeometry.Segment(firstPerson.from(), point(visualHit.m_82450_()), original.born(), true);
        }
        TrailPath path = paths.computeIfAbsent(bullet, ignored -> new TrailPath());
        if (!path.append(segment, firstPerson, preview, TracerConfig.DISTANCE.get(), original.to().sub(original.from()).length())) return blocked;
        if (probe != null) {
            probe.recorded = true;
            if (preview) probe.spawnRecorded = true;
        }
        while (paths.size() > TracerConfig.BUDGET.get() * 4) paths.remove(paths.keySet().iterator().next());
        revision++;
        return blocked;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.m_91087_();
        if (!TracerConfig.ENABLED.get() || mc.f_91073_ == null || level != mc.f_91073_) {
            clear(); level = mc.f_91073_; return;
        }
        double now = mc.f_91073_.m_46467_(), life = TracerConfig.LIFETIME.get() * 20;
        if (paths.entrySet().removeIf(e -> e.getValue().expired(now, life))) revision++;
        if (lastShotNanos != 0 && System.nanoTime() - lastShotNanos > 1_500_000_000L) report();
    }

    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); level = null; }

    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || paths.isEmpty() || !TracerConfig.ENABLED.get()) return;
        Minecraft mc = Minecraft.m_91087_();
        if (mc.f_91073_ == null || level != mc.f_91073_) return;
        TrailGeometry.Point eye = point(event.getCamera().m_90583_());
        double now = mc.f_91073_.m_46467_() + event.getPartialTick();
        prepare(eye, now);
        if (prepared.isEmpty()) return;
        VertexConsumer vertices = Draw.BUFFERS.m_6299_(Draw.TYPE);
        Matrix4f matrix = event.getPoseStack().m_85850_().m_252922_();
        try {
            for (Visible visible : prepared) {
                TrailGeometry.Ribbon ribbon = visible.ribbon();
                quad(vertices, matrix, ribbon, eye, 1.8, .06);
                quad(vertices, matrix, ribbon, eye, .85, .55);
                Probe probe = probes.get(visible.bullet());
                if (probe != null) {
                    probe.submissions++;
                    probe.minShaderAlpha = Math.min(probe.minShaderAlpha, RenderSystem.getShaderColor()[3]);
                    Vector4f a = projected(ribbon.tail().sub(eye), matrix), b = projected(ribbon.head().sub(eye), matrix);
                    if (a.w() > 0 && b.w() > 0) {
                        double dx = (a.x() / a.w() - b.x() / b.w()) * mc.m_91268_().m_85441_() / 2;
                        double dy = (a.y() / a.w() - b.y() / b.w()) * mc.m_91268_().m_85442_() / 2;
                        probe.maxSpanPixels = Math.max(probe.maxSpanPixels, Math.hypot(dx, dy));
                    }
                }
            }
        } finally { Draw.BUFFERS.m_109912_(Draw.TYPE); }
    }

    private static void quad(VertexConsumer out, Matrix4f matrix, TrailGeometry.Ribbon r, TrailGeometry.Point eye, double scale, double alpha) {
        TrailGeometry.Point side = r.side().scale(r.width() * scale);
        TrailGeometry.Point up = r.up().scale(r.width() * scale);
        TrailGeometry.Point tailSide = r.side().scale(r.tailWidth() * scale);
        TrailGeometry.Point tailUp = r.up().scale(r.tailWidth() * scale);
        TrailGeometry.Point head = r.head().sub(eye), tail = r.tail().sub(eye);
        int headAlpha = (int)(255 * r.alpha() * alpha);
        for (int face = 0; face < 6; face++) {
            double a = face * Math.PI / 3, b = (face + 1) * Math.PI / 3;
            TrailGeometry.Point first = side.scale(Math.cos(a)).add(up.scale(Math.sin(a)));
            TrailGeometry.Point second = side.scale(Math.cos(b)).add(up.scale(Math.sin(b)));
            TrailGeometry.Point tailFirst = tailSide.scale(Math.cos(a)).add(tailUp.scale(Math.sin(a)));
            TrailGeometry.Point tailSecond = tailSide.scale(Math.cos(b)).add(tailUp.scale(Math.sin(b)));
            vertex(out, matrix, tail.add(tailFirst), (int)(headAlpha * .4));
            vertex(out, matrix, head.add(first), headAlpha);
            vertex(out, matrix, head.add(second), headAlpha);
            vertex(out, matrix, tail.add(tailSecond), (int)(headAlpha * .4));
        }
    }
    private static void vertex(VertexConsumer out, Matrix4f matrix, TrailGeometry.Point p, int alpha) {
        out.m_252986_(matrix, (float)p.x(), (float)p.y(), (float)p.z()).m_6122_(255, 237, 190, alpha).m_5752_();
    }
    private static TrailGeometry.Point point(Vec3 p) { return new TrailGeometry.Point(p.f_82479_, p.f_82480_, p.f_82481_); }
    private static Vec3 vec(TrailGeometry.Point p) { return new Vec3(p.x(), p.y(), p.z()); }
    private static Vector4f projected(TrailGeometry.Point p, Matrix4f matrix) {
        return new Vector4f((float)p.x(), (float)p.y(), (float)p.z(), 1).mul(matrix).mul(RenderSystem.getProjectionMatrix());
    }

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
