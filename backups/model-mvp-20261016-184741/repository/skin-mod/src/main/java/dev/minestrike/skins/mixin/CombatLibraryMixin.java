package dev.minestrike.skins.mixin;

import com.mojang.blaze3d.audio.Library;
import dev.minestrike.skins.AudioConfig;
import dev.minestrike.skins.MineStrikeSkins;
import dev.minestrike.skins.CombatAudio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.lwjgl.openal.*;

@Mixin(value = Library.class, remap = false)
public abstract class CombatLibraryMixin {
    @ModifyVariable(method = "m_231084_", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean minestrike$hrtf(boolean requested) {
        return requested || AudioConfig.ENABLED.get() && AudioConfig.HRTF.get();
    }
    @Inject(method = "m_231084_", at = @At("RETURN"))
    private void minestrike$device(String deviceName, boolean requested, CallbackInfo ci) {
        if (AudioConfig.ENABLED.get()) {
            long device = ALC10.alcGetContextsDevice(CombatAudio.currentContext());
            int status = ALC.getCapabilities().ALC_SOFT_HRTF
                    ? ALC10.alcGetInteger(device, SOFTHRTF.ALC_HRTF_STATUS_SOFT) : -1;
            MineStrikeSkins.LOG.info("[audio] OpenAL HRTF requested={} actualStatus={} (1=enabled, -1=unsupported)", requested, status);
        }
    }
}
