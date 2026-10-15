package dev.minestrike.skins.mixin;

import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import dev.minestrike.skins.BombDamage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = Explosion.class, remap = false)
public abstract class BombDamageMixin {
    @Shadow @Final private Entity f_46016_;
    @Shadow @Final private double f_46013_, f_46014_, f_46015_;

    @Redirect(method = "m_46061_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private boolean minestrike$c4Damage(Entity target, DamageSource source, float amount) {
        if (f_46016_ instanceof CompositionC4Entity && target instanceof ServerPlayer player) {
            double dx = player.m_20185_() - f_46013_;
            double dy = player.m_20186_() - f_46014_;
            double dz = player.m_20189_() - f_46015_;
            amount = BombDamage.amount(Math.sqrt(dx * dx + dy * dy + dz * dz), player.m_21233_());
            if (amount <= 0) return false;
        }
        return target.m_6469_(source, amount);
    }
}
