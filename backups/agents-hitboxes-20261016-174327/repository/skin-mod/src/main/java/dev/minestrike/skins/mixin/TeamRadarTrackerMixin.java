package dev.minestrike.skins.mixin;

import dev.minestrike.skins.TeamRadarClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElement;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer", remap = false)
public abstract class TeamRadarTrackerMixin {
    @Inject(method = "renderElement(Lxaero/hud/minimap/player/tracker/PlayerTrackerMinimapElement;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void teammatesOnly(PlayerTrackerMinimapElement<?> player, boolean a, boolean b, double c, float d, double e, double f,
                               MinimapElementRenderInfo info, GuiGraphics graphics, MultiBufferSource.BufferSource buffers,
                               CallbackInfoReturnable<Boolean> ci) {
        if (TeamRadarClient.hidden(player.getPlayerId())) ci.setReturnValue(false);
    }
}
