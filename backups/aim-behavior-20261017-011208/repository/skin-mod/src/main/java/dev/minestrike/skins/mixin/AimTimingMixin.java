package dev.minestrike.skins.mixin;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.entity.shooter.LivingEntityAim;
import com.tacz.guns.client.gameplay.LocalPlayerAim;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import dev.minestrike.skins.AimTiming;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

public final class AimTimingMixin {
    @Mixin(value = LivingEntityAim.class, remap = false)
    public static abstract class Server {
        @Shadow @Final private LivingEntity shooter;
        @ModifyArg(method = "tickAimingProgress", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), index = 1)
        private float minestrike$aim(float original) {
            return shooter instanceof net.minecraft.world.entity.player.Player
                    ? AimTiming.opening(original, IGunOperator.fromLivingEntity(shooter).getSynIsAiming()) : original;
        }
    }
    @Mixin(value = LocalPlayerAim.class, remap = false)
    public static abstract class Client {
        @Shadow @Final private LocalPlayer player;
        @ModifyArg(method = "getAlphaProgress", at = @At(value = "INVOKE", target = "Ljava/lang/Math;max(FF)F"), index = 1)
        private float minestrike$aim(float original) {
            return AimTiming.opening(original, IClientPlayerGunOperator.fromLocalPlayer(player).isAim());
        }
    }
    @Mixin(value = ClientEventHandler.class, remap = false)
    public static abstract class Superb {
        @Redirect(method = "handleWeaponZoom", at = @At(value = "INVOKE", target = "Lkotlin/ranges/RangesKt;coerceIn(DDD)D", ordinal = 0))
        private double minestrike$aim(double value, double min, double max) {
            return AimTiming.advance(ClientEventHandler.zoomTime, Minecraft.m_91087_().m_91297_());
        }
    }
    private AimTimingMixin() {}
}
