package dev.minestrike.skins;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import java.util.*;

/** Six rotated base-model volumes, independent of texture and armor cosmetics. */
public final class PlayerVolumes {
    public record Box(AABB bounds, Matrix4d transform, int region) {}
    public record Hit(Vec3 point, int region) {
        public boolean head() { return region == 1; }
        public boolean leg() { return region == 2; }
    }
    public static List<Box> model(Vec3 origin, double yaw, double headYaw, double pitch,
                                  boolean crouch, boolean prone, double walk, double amount, boolean gun) {
        double unit = PlayerMetrics.UNIT;
        Matrix4d root = new Matrix4d().translate(origin.f_82479_, origin.f_82480_, origin.f_82481_).rotateY(-yaw);
        if (prone) root.translate(0, .3, 0).rotateX(Math.PI / 2).translate(0, -.9, 0);
        else if (crouch) root.translate(0, -.125, 0);
        root.translate(0, 24 * unit, 0).scale(unit, -unit, -unit);
        List<Box> parts = new ArrayList<>();
        part(parts, root, 0, crouch ? 4.2 : 0, 0, -4,-8,-4,8,8,8,
                0, headYaw-yaw, prone ? 0 : pitch, PlayerMetrics.HEAD, PlayerMetrics.HEAD, PlayerMetrics.HEAD, 1);
        part(parts, root, 0, crouch ? 3.2 : 0, 0, -4,0,-2,8,12,4,
                0,0,crouch ? .5 : 0,PlayerMetrics.TORSO_WIDTH,PlayerMetrics.LENGTH,PlayerMetrics.WIDTH,0);
        double stride = Math.cos(walk * .6662) * 1.4 * Math.min(1, amount);
        for (int side : new int[]{-1,1}) {
            part(parts,root,side*1.9,crouch ? 12.2 : 12,crouch ? 4 : 0,-2,0,-2,4,12,4,
                    0,0,side*stride,PlayerMetrics.WIDTH,PlayerMetrics.LENGTH,PlayerMetrics.WIDTH,2);
            part(parts,root,side*5,crouch ? 5.2 : 2,0,side < 0 ? -3 : -1,-2,-2,4,12,4,
                    0,gun ? -side*.1 : 0,gun ? -Math.PI/2+pitch : -side*stride/2+(crouch ? .4 : 0),
                    PlayerMetrics.WIDTH,PlayerMetrics.LENGTH,PlayerMetrics.WIDTH,3);
        }
        return parts;
    }
    private static void part(List<Box> parts, Matrix4d root, double x,double y,double z,
                             double ox,double oy,double oz,double w,double h,double d,
                             double rz,double ry,double rx,double width,double height,double depth,int region) {
        double pivotX = x*PlayerMetrics.WIDTH + (Math.abs(x)==5 ? Math.signum(x)*4*(PlayerMetrics.TORSO_WIDTH-PlayerMetrics.WIDTH) : 0);
        Matrix4d transform = new Matrix4d(root).translate(pivotX,
                24+(y-24)*PlayerMetrics.LENGTH,z*PlayerMetrics.WIDTH)
                .rotateZ(rz).rotateY(ry).rotateX(rx).scale(width,height,depth);
        parts.add(new Box(new AABB(ox,oy,oz,ox+w,oy+h,oz+d),transform,region));
    }
    public static Hit clip(List<Box> parts, Vec3 start, Vec3 end) {
        Hit closest = null; double distance = Double.POSITIVE_INFINITY;
        for (Box box : parts) {
            Matrix4d inverse = new Matrix4d(box.transform()).invert();
            Vec3 from = point(inverse,start), to = point(inverse,end);
            Vec3 local = box.bounds().m_82390_(from) ? from : box.bounds().m_82371_(from,to).orElse(null);
            if (local == null) continue;
            Vec3 hit = point(box.transform(),local); double squared = start.m_82557_(hit);
            if (squared < distance) { distance = squared; closest = new Hit(hit,box.region()); }
        }
        return closest;
    }
    private static Vec3 point(Matrix4d matrix,Vec3 point) {
        Vector3d p = matrix.transformPosition(new Vector3d(point.f_82479_,point.f_82480_,point.f_82481_));
        return new Vec3(p.x,p.y,p.z);
    }
    private PlayerVolumes() {}
}
