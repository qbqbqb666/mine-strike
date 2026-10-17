package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MatchMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class MatchMovementMixin {
    @Unique private final MatchMovement.State minestrike$movement = new MatchMovement.State();
    @Unique private boolean minestrike$ground;
    @Unique private Object minestrike$level;
    @Inject(method = "m_7023_", at = @At("HEAD"))
    private void minestrike$travel(Vec3 input, CallbackInfo ci) {
        minestrike$ground = false;
        if (!((Object)this instanceof Player player) || !MatchMovement.eligible(player)) minestrike$movement.reset();
        else if (minestrike$level != player.m_9236_()) { minestrike$level = player.m_9236_(); minestrike$movement.reset(); }
    }
    @Inject(method = "m_21074_", at = @At("HEAD"))
    private void minestrike$groundMove(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
        minestrike$ground = (Object)this instanceof Player player && MatchMovement.eligible(player);
        if (minestrike$ground) {
            var player = (Player)(Object)this;
            player.m_20256_(minestrike$movement.move(player.m_20184_(),
                    MatchMovement.target(input, player.m_146908_(), MatchMovement.speed(player), friction)));
        } else minestrike$movement.reset();
    }
    @ModifyArg(method = "m_21074_", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;m_19920_(FLnet/minecraft/world/phys/Vec3;)V"), index = 1)
    private Vec3 minestrike$input(Vec3 input) { return minestrike$ground ? Vec3.f_82478_ : input; }
    @Inject(method = "m_21074_", at = @At("RETURN"))
    private void minestrike$afterMove(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
        if (minestrike$ground) minestrike$movement.finish(cir.getReturnValue(), friction);
        minestrike$ground = false;
    }
}
