package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity", remap = false)
public abstract class SuperbBulletTrailMixin implements BulletTrails.Source {
    @Shadow private int shooterId;
    @Shadow private float explosionRadius;
    @Shadow private float explosionDamage;
    @Unique private Vec3 minestrike$trailStart;
    @Unique private boolean minestrike$trailBlocked;
    @Override public boolean minestrike$eligible() { return explosionRadius <= 0 && explosionDamage <= 0 && TracerConfig.SUPERB.get(); }
    @Override public net.minecraft.world.entity.Entity minestrike$owner() {
        Projectile bullet = (Projectile)(Object)this;
        return bullet.m_9236_().m_6815_(shooterId);
    }
    @Inject(method = "m_8119_", at = @At("HEAD"))
    private void minestrike$begin(CallbackInfo ci) {
        Projectile bullet = (Projectile)(Object)this;
        minestrike$trailStart = BulletTrails.begin(bullet, minestrike$trailBlocked);
    }
    @Inject(method = "m_8119_", at = @At("RETURN"))
    private void minestrike$end(CallbackInfo ci) {
        if (BulletTrails.record((Projectile)(Object)this, minestrike$trailStart)) minestrike$trailBlocked = true;
    }
}
