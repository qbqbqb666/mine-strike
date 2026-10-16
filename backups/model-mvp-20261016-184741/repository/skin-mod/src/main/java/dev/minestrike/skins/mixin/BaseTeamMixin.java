package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MatchLobby;
import com.phasetranscrystal.fpsmatch.core.map.BaseTeam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BaseTeam.class, remap = false)
public abstract class BaseTeamMixin {
    @Inject(method = {"getPlayerLimit", "getRemainingLimit"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void lobbyCapacity(CallbackInfoReturnable<Integer> ci) {
        if (MatchLobby.currentTeam((BaseTeam) (Object) this)) ci.setReturnValue(Integer.MAX_VALUE);
    }

    @Inject(method = "randomSpawnPoints", at = @At("HEAD"), cancellable = true, remap = false)
    private void lobbySharedSpawns(CallbackInfoReturnable<Boolean> ci) {
        BaseTeam team = (BaseTeam) (Object) this;
        var points = team.getSpawnPointsData();
        if (!MatchLobby.sharedSpawns(team) || points.isEmpty() || points.size() >= team.getPlayerCount()) return;
        int i = 0;
        for (var player : team.getPlayersData()) player.setSpawnPointsData(points.get(i++ % points.size()));
        ci.setReturnValue(true);
    }
}
