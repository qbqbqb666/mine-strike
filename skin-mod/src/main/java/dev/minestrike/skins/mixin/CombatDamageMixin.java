package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import com.tacz.guns.util.TacHitResult;
import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

public final class CombatDamageMixin {
    // Region belongs to the individual ray result, never the last target queried by a bullet.
    @Mixin(targets={"com.tacz.guns.entity.EntityKineticBullet$EntityResult","com.tacz.guns.util.TacHitResult",
        "com.atsuishio.superbwarfare.world.phys.EntityResult","com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult"},remap=false)
    public static abstract class Region implements DamageRegion {
        @Unique private int minestrike$bodyRegion=-1;
        public int minestrike$region(){return minestrike$bodyRegion;}
        public void minestrike$region(int region){minestrike$bodyRegion=region;}
    }
    @Mixin(value=TacHitResult.class,remap=false)
    public static abstract class TaczResult {
        @Inject(method="<init>",at=@At("RETURN"))
        private void minestrike$copy(com.tacz.guns.entity.EntityKineticBullet.EntityResult result,CallbackInfo ci) {
            ((DamageRegion)this).minestrike$region(((DamageRegion)(Object)result).minestrike$region());
        }
    }
    @Mixin(value=ExtendedEntityRayTraceResult.class,remap=false)
    public static abstract class SuperbResult {
        @Inject(method="<init>",at=@At("RETURN"))
        private void minestrike$copy(com.atsuishio.superbwarfare.world.phys.EntityResult result,CallbackInfo ci) {
            ((DamageRegion)this).minestrike$region(((DamageRegion)(Object)result).minestrike$region());
        }
    }
    @Mixin(targets="com.tacz.guns.entity.EntityKineticBullet",remap=false)
    public static abstract class Tacz {
        @Redirect(method="onHitEntity",at=@At(value="INVOKE",target="Lcom/tacz/guns/api/event/common/EntityHurtByGunEvent$Pre;getBaseAmount()F"))
        private float minestrike$base(EntityHurtByGunEvent.Pre event,TacHitResult hit,Vec3 from,Vec3 to) {
            CombatSettings s=event.getHurtEntity() instanceof ServerPlayer p?CombatStats.settings(p):null;
            int region=((DamageRegion)hit).minestrike$region();
            return (float)(event.getBaseAmount()*(s!=null && region>=0 && !event.isHeadShot()?s.multiplier(region):1));
        }
        @Redirect(method="onHitEntity",at=@At(value="INVOKE",target="Lcom/tacz/guns/api/event/common/EntityHurtByGunEvent$Pre;getHeadshotMultiplier()F"))
        private float minestrike$head(EntityHurtByGunEvent.Pre event,TacHitResult hit,Vec3 from,Vec3 to) {
            CombatSettings s=event.getHurtEntity() instanceof ServerPlayer p?CombatStats.settings(p):null;
            int region=((DamageRegion)hit).minestrike$region();
            return s!=null && region>=0?(float)s.multiplier(region):event.getHeadshotMultiplier();
        }
    }
    @Mixin(targets="com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity",remap=false)
    public static abstract class Superb {
        @Unique private CombatSettings minestrike$settings;
        @Unique private int minestrike$region=-1;
        @Unique private Entity minestrike$target;
        @Inject(method="onHitEntity",at=@At("HEAD"))
        private void minestrike$begin(Entity entity,ExtendedEntityRayTraceResult hit,CallbackInfo ci) {
            minestrike$region=((DamageRegion)(Object)hit).minestrike$region();
            minestrike$target=entity;
            minestrike$settings=entity instanceof ServerPlayer p && minestrike$region>=0?CombatStats.settings(p):null;
        }
        @Inject(method="onHitEntity",at=@At("RETURN"))
        private void minestrike$end(Entity entity,ExtendedEntityRayTraceResult hit,CallbackInfo ci) {minestrike$settings=null;minestrike$target=null;minestrike$region=-1;}
        @Redirect(method="onHitEntity",at=@At(value="FIELD",target="Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;legShot:F",opcode=org.objectweb.asm.Opcodes.GETFIELD))
        private float minestrike$leg(com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity projectile) {
            return minestrike$settings!=null?1:minestrike$originalLeg();
        }
        @Shadow private float legShot;
        @Unique private float minestrike$originalLeg(){return legShot;}
        @Shadow private float headShot;
        @Redirect(method="performDamage",at=@At(value="FIELD",target="Lcom/atsuishio/superbwarfare/entity/projectile/ProjectileEntity;headShot:F",opcode=org.objectweb.asm.Opcodes.GETFIELD))
        private float minestrike$head(com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity projectile,Entity target,float amount,boolean head) {return minestrike$settings!=null && target==minestrike$target?1:headShot;}
        @ModifyVariable(method="performDamage",at=@At("HEAD"),argsOnly=true,ordinal=0)
        private float minestrike$amount(float amount,Entity target,float original,boolean head) {
            return minestrike$settings!=null && target==minestrike$target?(float)(amount*minestrike$settings.multiplier(minestrike$region)):amount;
        }
    }
    private CombatDamageMixin() {}
}
