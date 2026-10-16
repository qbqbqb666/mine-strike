package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MapBaseline;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = BaseMap.class, remap = false)
public abstract class BaseMapMixin {
    @Inject(method = "mapTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void baselinePause(CallbackInfo ci) {
        if ((Object) this instanceof CSGameMap map && MapBaseline.busy(map)) ci.cancel();
    }
}
