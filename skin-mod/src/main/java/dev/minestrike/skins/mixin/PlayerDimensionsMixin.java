package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Player.class, remap = false)
public abstract class PlayerDimensionsMixin {
    @Inject(method = "m_6972_", at = @At("RETURN"), cancellable = true)
    private void minestrike$dimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (AgentPlayers.active((Player)(Object)this))
            cir.setReturnValue(cir.getReturnValue().m_20390_(PlayerMetrics.WIDTH,
                    pose == Pose.STANDING || pose == Pose.CROUCHING ? PlayerMetrics.TOTAL_HEIGHT : 1));
    }
}
