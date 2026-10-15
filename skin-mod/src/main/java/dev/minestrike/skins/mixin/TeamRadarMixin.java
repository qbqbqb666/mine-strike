package dev.minestrike.skins.mixin;

import dev.minestrike.skins.TeamRadarClient;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import xaero.hud.minimap.element.render.MinimapElementRenderInfo;
import xaero.hud.minimap.element.render.MinimapElementRenderLocation;

@Pseudo
@Mixin(targets = "xaero.hud.minimap.radar.render.element.RadarRenderer", remap = false)
public abstract class TeamRadarMixin {
    @Inject(method = "renderElement(Lnet/minecraft/world/entity/Entity;ZZDFDDLxaero/hud/minimap/element/render/MinimapElementRenderInfo;Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/renderer/MultiBufferSource$BufferSource;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void teammatesOnly(Entity entity, boolean a, boolean b, double c, float d, double e, double f,
                               MinimapElementRenderInfo info, GuiGraphics graphics, MultiBufferSource.BufferSource buffers,
                               CallbackInfoReturnable<Boolean> ci) {
        if (TeamRadarClient.hidden(entity)) ci.setReturnValue(false);
    }
    @Inject(method = "renderSingleEntity", at = @At("HEAD"), cancellable = true, remap = false)
    private void teammatesOnlySingle(Entity entity, boolean a, boolean b, float c, boolean d, boolean e,
                                     MinimapElementRenderLocation location, RenderTarget target, GuiGraphics graphics, CallbackInfo ci) {
        if (TeamRadarClient.hidden(entity)) ci.cancel();
    }
}
