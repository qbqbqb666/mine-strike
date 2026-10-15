package dev.minestrike.skins.mixin;

import com.mojang.blaze3d.audio.Channel;
import dev.minestrike.skins.CombatAudio;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Channel.class, remap = false)
public abstract class CombatChannelMixin implements CombatAudio.ManagedChannel {
    @Shadow @Final private int f_83642_;
    @Unique private CombatAudio.Profile minestrike$profile;
    @Unique private boolean minestrike$spatialized;

    public void minestrike$audioProfile(CombatAudio.Profile profile) {
        minestrike$profile = profile;
        CombatAudio.register(f_83642_, profile);
    }
    @Inject(method = "m_83665_", at = @At("HEAD"))
    private void minestrike$destroy(CallbackInfo ci) { CombatAudio.register(f_83642_, null); }
    @Inject(method = "m_83672_", at = @At(value = "INVOKE", target = "Lorg/lwjgl/openal/AL10;alSourcePlay(I)V"))
    private void minestrike$play(CallbackInfo ci) {
        minestrike$spatialized = CombatAudio.source(minestrike$profile, f_83642_, minestrike$spatialized);
    }
}
