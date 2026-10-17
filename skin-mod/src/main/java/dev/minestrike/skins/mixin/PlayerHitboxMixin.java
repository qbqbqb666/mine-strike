package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import java.util.Optional;

public final class PlayerHitboxMixin {
    @Mixin(targets = "com.tacz.guns.util.EntityUtil", remap = false)
    public static abstract class Tacz {
        @Unique private static final ThreadLocal<PlayerVolumes.Hit> minestrike$hit = new ThreadLocal<>();
        @Redirect(method = "getHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;m_82371_(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"))
        private static Optional<Vec3> minestrike$clip(AABB box, Vec3 from, Vec3 to, Projectile bullet, Entity target, Vec3 start, Vec3 end) {
            minestrike$hit.remove();
            if (!(target instanceof Player player) || !AgentPlayers.active(player)) return box.m_82371_(from,to);
            var hit = PlayerHitboxes.clip(player,box,from,to); minestrike$hit.set(hit);
            return hit == null ? Optional.empty() : Optional.of(hit.point());
        }
        @Inject(method = "getHitResult", at = @At("RETURN"), cancellable = true)
        private static void minestrike$region(Projectile bullet, Entity target, Vec3 start, Vec3 end,
                CallbackInfoReturnable<com.tacz.guns.entity.EntityKineticBullet.EntityResult> cir) {
            var hit = minestrike$hit.get(); minestrike$hit.remove();
            if (hit != null && cir.getReturnValue() != null && target instanceof Player player && AgentPlayers.active(player))
                {
                var result=new com.tacz.guns.entity.EntityKineticBullet.EntityResult(target,hit.point(),hit.head());
                ((DamageRegion)(Object)result).minestrike$region(hit.region());
                cir.setReturnValue(result);
            }
        }
    }
    @Mixin(targets = "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity", remap = false)
    public static abstract class Superb {
        @Unique private final ThreadLocal<PlayerVolumes.Hit> minestrike$hit = new ThreadLocal<>();
        @Redirect(method = "getHitResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;m_82371_(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/Optional;"))
        private Optional<Vec3> minestrike$clip(AABB box, Vec3 from, Vec3 to, Entity target, Vec3 start, Vec3 end) {
            minestrike$hit.remove();
            if (!(target instanceof Player player) || !AgentPlayers.active(player)) return box.m_82371_(from,to);
            var hit = PlayerHitboxes.clip(player,box,from,to); minestrike$hit.set(hit);
            return hit == null ? Optional.empty() : Optional.of(hit.point());
        }
        @Inject(method = "getHitResult", at = @At("RETURN"), cancellable = true)
        private void minestrike$region(Entity target, Vec3 start, Vec3 end,
                CallbackInfoReturnable<com.atsuishio.superbwarfare.world.phys.EntityResult> cir) {
            var hit = minestrike$hit.get(); minestrike$hit.remove();
            if (hit != null && cir.getReturnValue() != null && target instanceof Player player && AgentPlayers.active(player))
                {
                var result=new com.atsuishio.superbwarfare.world.phys.EntityResult(target,hit.point(),hit.head(),hit.leg());
                ((DamageRegion)(Object)result).minestrike$region(hit.region());
                cir.setReturnValue(result);
            }
        }
    }
    private PlayerHitboxMixin() {}
}
