package dev.minestrike.skins.mixin;

import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.SoundBuffer;
import dev.minestrike.skins.CombatAudio;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value = SoundEngine.class, remap = false)
public abstract class CombatSoundEngineMixin {
    @Shadow private float m_235257_(float volume, SoundSource category) { throw new AssertionError(); }

    @Inject(method = "lambda$play$6", at = @At("HEAD"))
    private void minestrike$buffer(SoundBuffer buffer, SoundInstance sound, Channel channel, CallbackInfo ci) {
        CombatAudio.bind(sound, channel);
    }
    @Inject(method = "lambda$play$8", at = @At("HEAD"))
    private void minestrike$stream(AudioStream stream, SoundInstance sound, Channel channel, CallbackInfo ci) {
        CombatAudio.bind(sound, channel);
    }
    @Redirect(method = "m_120312_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundEngine;m_235257_(FLnet/minecraft/sounds/SoundSource;)F"))
    private float minestrike$startGain(SoundEngine engine, float volume, SoundSource category, SoundInstance sound) {
        return CombatAudio.gain(sound, m_235257_(volume, category));
    }
    @Inject(method = "m_120327_", at = @At("RETURN"), cancellable = true)
    private void minestrike$tickGain(SoundInstance sound, CallbackInfoReturnable<Float> ci) {
        ci.setReturnValue(CombatAudio.gain(sound, ci.getReturnValue()));
    }
}
