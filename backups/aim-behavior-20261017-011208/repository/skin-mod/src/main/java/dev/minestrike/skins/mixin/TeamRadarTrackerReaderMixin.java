package dev.minestrike.skins.mixin;

import dev.minestrike.skins.TeamRadarClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderContext;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementReader", remap = false)
public abstract class TeamRadarTrackerReaderMixin {
    @Inject(method = "isHidden(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElementRenderContext;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void teammatesOnly(PlayerTrackerMinimapElement<?> player, PlayerTrackerMinimapElementRenderContext context,
                               CallbackInfoReturnable<Boolean> ci) {
        if (TeamRadarClient.hidden(player.getPlayerId())) ci.setReturnValue(true);
    }
}
