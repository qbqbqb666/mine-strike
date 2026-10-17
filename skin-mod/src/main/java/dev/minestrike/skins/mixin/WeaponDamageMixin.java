package dev.minestrike.skins.mixin;

import dev.minestrike.skins.WeaponDamage;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class WeaponDamageMixin {
    @Mixin(targets="com.tacz.guns.entity.EntityKineticBullet",remap=false)
    public static abstract class Tacz {
        @Shadow private ResourceLocation gunId;
        @Inject(method="getDamage",at=@At("RETURN"),cancellable=true)
        private void minestrike$weaponDamage(Vec3 hit,CallbackInfoReturnable<Float> cir) {
            cir.setReturnValue(WeaponDamage.scale(gunId==null?null:gunId.toString(),cir.getReturnValueF()));
        }
    }
    @Mixin(targets="com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity",remap=false)
    public static abstract class Superb {
        @Shadow private String gunItemId;
        @ModifyVariable(method="performOnHit",at=@At("HEAD"),argsOnly=true,ordinal=0)
        private float minestrike$weaponDamage(float amount) {
            return WeaponDamage.scale(gunItemId,amount);
        }
    }
    @Mixin(targets="com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity",remap=false)
    public static abstract class Taser {
        @Shadow private float damage;
        @Redirect(method="m_5790_",at=@At(value="FIELD",target="Lcom/atsuishio/superbwarfare/entity/projectile/TaserBulletEntity;damage:F",opcode=org.objectweb.asm.Opcodes.GETFIELD))
        private float minestrike$weaponDamage(com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity bullet) {
            return WeaponDamage.scale("superbwarfare:taser",damage);
        }
    }
    private WeaponDamageMixin() {}
}
