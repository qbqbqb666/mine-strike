package dev.minestrike.skins.mixin;

import static dev.minestrike.skins.BombAudio.*;
import com.phasetranscrystal.blockoffensive.entity.CompositionC4Entity;
import com.phasetranscrystal.blockoffensive.item.CompositionC4;
import dev.minestrike.skins.MatchFeedback;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

public final class BombAudioMixin {
    @Mixin(value = CompositionC4.class, remap = false)
    public static abstract class Plant {
        @Redirect(method = "m_5922_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_6263_(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"))
        private void minestrike$announce(Level level, Player except, double x, double y, double z,
                                          SoundEvent event, SoundSource category, float volume, float pitch,
                                          ItemStack stack, Level originalLevel, LivingEntity actor) {
            if (actor instanceof Player player && PLANTED.equals(event.m_11660_().toString())) {
                var map = FPSMCore.getInstance().getMapByPlayer(player);
                if (map.isPresent()) {
                    MatchFeedback.announce(map.get());
                    return;
                }
            }
            level.m_6263_(except, x, y, z, event, category, volume, pitch);
        }
    }

    @Mixin(value = CompositionC4Entity.class, remap = false)
    public static abstract class Beep {
        @ModifyArg(method = "playBeepSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;m_6263_(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V"), index = 6)
        private float minestrike$range(float original) { return BEEP_RANGE / 16; }
    }

    @Pseudo
    @Mixin(targets = "com.sonicether.soundphysics.SoundPhysics", remap = false)
    public static abstract class Clear {
        @Shadow public static void setDefaultEnvironment(int source, boolean auxOnly) { throw new AssertionError(); }
        @Inject(method = "evaluateEnvironment(IDDDLnet/minecraft/sounds/SoundSource;Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/world/phys/Vec3;", at = @At("HEAD"), cancellable = true)
        private static void minestrike$clear(int source, double x, double y, double z, SoundSource category,
                                              ResourceLocation sound, boolean auxiliary, CallbackInfoReturnable<Vec3> ci) {
            if (sound == null) return;
            String id = sound.toString();
            boolean announcement = PLANTED.equals(id) || LOUD_PLANTED.equals(id);
            if (announcement || BEEP.equals(id) || LOUD_BEEP.equals(id)) {
                setDefaultEnvironment(source, false);
                ci.setReturnValue(null);
            }
        }
    }
    private BombAudioMixin() {}
}
