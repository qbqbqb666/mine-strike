package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MatchMovement;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, remap = false)
public abstract class MatchMovementMixin {
    @Unique private final MatchMovement.State minestrike$movement = new MatchMovement.State();
    @Unique private final MatchMovement.Jumps minestrike$jumps = new MatchMovement.Jumps();
    @Shadow protected boolean f_20899_;
    @Unique private boolean minestrike$ground;
    @Unique private Object minestrike$level;
    @Inject(method = "m_7023_", at = @At("HEAD"))
    private void minestrike$travel(Vec3 input, CallbackInfo ci) {
        minestrike$ground = false;
        boolean allowed = (Object)this instanceof Player player && MatchMovement.applicable(player);
        if (allowed && minestrike$level != ((Player)(Object)this).m_9236_()) {
            if (minestrike$level != null) minestrike$jumps.begin(false, false, false);
            minestrike$level = ((Player)(Object)this).m_9236_(); minestrike$movement.reset();
        }
        minestrike$jumps.begin(allowed, f_20899_, allowed && MatchMovement.running((Player)(Object)this));
        if (!allowed || !MatchMovement.eligible((Player)(Object)this) || minestrike$jumps.nativeMovement()) minestrike$movement.reset();
    }
    @Inject(method = "m_21074_", at = @At("HEAD"))
    private void minestrike$groundMove(Vec3 input, float friction, CallbackInfoReturnable<Vec3> cir) {
        minestrike$ground = (Object)this instanceof Player player && MatchMovement.eligible(player) && !minestrike$jumps.nativeMovement();
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
    @Inject(method = "m_6135_", at = @At("HEAD"))
    private void minestrike$jump(CallbackInfo ci) {
        if ((Object)this instanceof Player player && MatchMovement.applicable(player))
            minestrike$jumps.start(MatchMovement.running(player));
    }
    @Redirect(method = "m_6135_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;m_20142_()Z"))
    private boolean minestrike$jumpSprint(LivingEntity entity) {
        return entity.m_20142_() || entity instanceof Player player && MatchMovement.running(player);
    }
    @Inject(method = "m_21330_", at = @At("RETURN"), cancellable = true)
    private void minestrike$acceleration(float friction, CallbackInfoReturnable<Float> cir) {
        if (!((Object)this instanceof Player player) || !MatchMovement.applicable(player)) return;
        if (player.m_20096_() && minestrike$jumps.nativeMovement())
            cir.setReturnValue((float)(cir.getReturnValueF() * MatchMovement.speed(player) / player.m_6113_()));
        else if (!player.m_20096_() && MatchMovement.running(player))
            cir.setReturnValue(MatchMovement.airAcceleration(cir.getReturnValueF(), player.m_20142_()));
    }
    @Inject(method = "m_7023_", at = @At("RETURN"))
    private void minestrike$travelEnd(Vec3 input, CallbackInfo ci) { minestrike$jumps.finish(); }

}
