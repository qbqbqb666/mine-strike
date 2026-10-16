package dev.minestrike.skins.mixin;

import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.network.message.ServerMessageSound;
import dev.minestrike.skins.*;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundPlayManager.class, remap = false)
public abstract class TaczAudioPacketMixin {
    @Inject(method = "playMessageSound", at = @At("HEAD"))
    private static void minestrike$packet(ServerMessageSound packet, CallbackInfo ci) {
        if (AudioConfig.ENABLED.get() && AudioConfig.DEBUG.get()) {
            var level = Minecraft.m_91087_().f_91073_;
            MineStrikeSkins.LOG.info("[audio] TaCZ packet entity={} loaded={} gun={} sound={} volume={} range={}",
                    packet.getEntityId(), level != null && level.m_6815_(packet.getEntityId()) != null,
                    packet.getGunId(), packet.getSoundName(), packet.getVolume(), packet.getDistance());
        }
    }
}
