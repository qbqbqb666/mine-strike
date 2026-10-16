package dev.minestrike.skins;

import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class PlayerHitboxes {
    public static PlayerVolumes.Hit clip(Player player, AABB compensated, Vec3 start, Vec3 end) {
        // Keep each gun mod's historical position / velocity translation, but do
        // not reuse its whole-body inflation as the final narrow-phase volume.
        Vec3 shift = compensated.m_82399_().m_82546_(player.m_20191_().m_82399_());
        var held = player.m_21205_();
        boolean gun = held.m_41720_() instanceof com.tacz.guns.api.item.IGun
                || held.m_41720_() instanceof com.atsuishio.superbwarfare.item.gun.GunItem;
        var pose = player.m_20089_();
        boolean prone = pose == Pose.SWIMMING || pose == Pose.FALL_FLYING || pose == Pose.SPIN_ATTACK;
        return PlayerVolumes.clip(PlayerVolumes.model(player.m_20182_().m_82549_(shift),
                Math.toRadians(player.f_20883_), Math.toRadians(player.m_6080_()), Math.toRadians(player.m_146909_()),
                player.m_6047_(), prone, player.f_267362_.m_267756_(), player.f_267362_.m_267731_(), gun),start,end);
    }
    private PlayerHitboxes() {}
}
