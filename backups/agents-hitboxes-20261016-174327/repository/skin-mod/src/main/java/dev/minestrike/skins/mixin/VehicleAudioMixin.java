package dev.minestrike.skins.mixin;

import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import dev.minestrike.skins.AudioPolicy;
import org.spongepowered.asm.mixin.*;
import java.util.UUID;

@Pseudo
@Mixin(targets = "com.atsuishio.superbwarfare.client.sound.VehicleSoundInstance", remap = false)
public abstract class VehicleAudioMixin implements AudioPolicy.Source {
    @Shadow @Final private VehicleEntity mobileVehicle;
    public UUID minestrike$audioEmitter() { return mobileVehicle.m_20148_(); }
}
