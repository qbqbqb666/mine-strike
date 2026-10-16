package dev.minestrike.skins.mixin;

import com.tacz.guns.client.sound.GunSoundInstance;
import dev.minestrike.skins.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.UUID;

@Mixin(value = GunSoundInstance.class, remap = false)
public abstract class TaczAudioMixin extends AbstractSoundInstance implements AudioPolicy.Source {
    @Unique private UUID minestrike$emitter;
    protected TaczAudioMixin(SoundEvent event, SoundSource source, RandomSource random) { super(event, source, random); }
    public UUID minestrike$audioEmitter() { return minestrike$emitter; }

    @Inject(method = "<init>(Lnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FFLnet/minecraft/world/entity/Entity;ILnet/minecraft/resources/ResourceLocation;ZZ)V", at = @At("RETURN"))
    private void minestrike$init(SoundEvent event, SoundSource category, float volume, float pitch, Entity entity,
                                  int range, ResourceLocation id, boolean mono, boolean relative, CallbackInfo ci) {
        minestrike$emitter = entity.m_20148_();
        var player = Minecraft.m_91087_().f_91074_;
        if (AudioConfig.ENABLED.get() && AudioConfig.ATTENUATION.get() && !relative && player != null
                && !player.m_20148_().equals(minestrike$emitter) && id != null && AudioPolicy.taczShot(id.toString()))
            f_119573_ = AudioPolicy.shotGain(volume, Math.sqrt(player.m_20275_(f_119575_, f_119576_, f_119577_)),
                    range, AudioConfig.GUN_EXPONENT.get());
    }
}
