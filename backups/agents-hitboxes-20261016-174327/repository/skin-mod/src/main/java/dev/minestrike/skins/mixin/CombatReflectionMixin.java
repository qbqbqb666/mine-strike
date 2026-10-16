package dev.minestrike.skins.mixin;

import dev.minestrike.skins.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
public abstract class CombatReflectionMixin {
    @Inject(method = "processSound(IDDDLnet/minecraft/sounds/SoundSource;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private static void minestrike$direct(int source, double x, double y, double z, SoundSource category,
                                          ResourceLocation id, boolean auxiliary, CallbackInfoReturnable<Vec3> ci) {
        if (CombatAudio.environment(source, x, y, z)) ci.setReturnValue(new Vec3(x, y, z));
    }
}
