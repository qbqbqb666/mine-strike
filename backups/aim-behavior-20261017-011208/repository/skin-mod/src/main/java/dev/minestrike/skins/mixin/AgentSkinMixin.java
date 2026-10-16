package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractClientPlayer.class, remap = false)
public abstract class AgentSkinMixin {
    @Inject(method = "m_108560_", at = @At("HEAD"), cancellable = true)
    private void minestrike$agent(CallbackInfoReturnable<ResourceLocation> cir) {
        var player = (AbstractClientPlayer)(Object)this;
        ResourceLocation texture = AgentSkins.texture(AgentPlayers.client.get(player.m_20148_()));
        if (texture != null) cir.setReturnValue(texture);
    }
    @Inject(method = "m_108564_", at = @At("HEAD"), cancellable = true)
    private void minestrike$wide(CallbackInfoReturnable<String> cir) {
        if (AgentPlayers.client.containsKey(((AbstractClientPlayer)(Object)this).m_20148_())) cir.setReturnValue("default");
    }
}
