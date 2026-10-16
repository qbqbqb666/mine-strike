package dev.minestrike.skins.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import dev.minestrike.skins.*;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class TaczBulletTrailMixin implements BulletTrails.Source {
    @Shadow private boolean explosion;
    @Unique private Vec3 minestrike$trailStart;
    @Unique private boolean minestrike$trailBlocked;
    @Override public boolean minestrike$eligible() { return !explosion && TracerConfig.TACZ.get(); }
    @Override public net.minecraft.world.entity.Entity minestrike$owner() { return ((Projectile)(Object)this).m_19749_(); }
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
