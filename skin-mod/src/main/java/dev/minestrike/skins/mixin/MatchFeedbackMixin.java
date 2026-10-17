package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.data.PlayerData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class MatchFeedbackMixin {
    @Mixin(value=Player.class,remap=false)
    public static abstract class Health {
        @Redirect(method="m_6475_",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/player/Player;m_21153_(F)V"))
        private void minestrike$damage(Player player,float health,DamageSource source,float amount) {
            float before=player.m_21223_(),maximum=player.m_21233_();
            player.m_21153_(health);
            MatchDamage.record(player,source,before,maximum);
        }
    }
    @Mixin(value=CSGameMap.class,remap=false)
    public static abstract class Lifecycle implements MatchDamage.State {
        @Shadow private boolean isWaiting,isPause,isWarmTime,isWaitingWinner;
        public boolean minestrike$combatLive(){return !isWaiting&&!isPause&&!isWarmTime&&!isWaitingWinner;}
        @Inject(method="startGame",at=@At("TAIL"))
        private void minestrike$start(CallbackInfo ci){MatchDamage.start((CSGameMap)(Object)this);}
        @Inject(method="resetGame",at=@At("HEAD"))
        private void minestrike$end(CallbackInfo ci){MatchDamage.end((CSGameMap)(Object)this);}
    }
    @Mixin(targets="com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameTabRenderer",remap=false)
    public static abstract class Tab {
        @Redirect(method="renderPlayerRow",at=@At(value="INVOKE",target="Lcom/phasetranscrystal/fpsmatch/core/data/PlayerData;getDamage()F"))
        private float minestrike$damage(PlayerData data){return MatchFeedback.Client.damage(data.getOwner());}
        @Redirect(method="lambda$render$0",at=@At(value="INVOKE",target="Lcom/phasetranscrystal/fpsmatch/core/data/PlayerData;getDamage()F"))
        private static float minestrike$sortDamage(PlayerData data){return MatchFeedback.Client.damage(data.getOwner());}
    }
    private MatchFeedbackMixin() {}
}
