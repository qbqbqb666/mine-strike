package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MvpMusicHud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

public final class MvpMusicMixin {
    @Mixin(targets = "com.phasetranscrystal.fpsmatch.common.client.music.FPSClientMusicManager", remap = false)
    public static abstract class Player {
        @Redirect(method = "playMusic(Lnet/minecraft/resources/ResourceLocation;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sounds/SoundManager;m_120367_(Lnet/minecraft/client/resources/sounds/SoundInstance;)V"))
        private static void minestrike$playing(SoundManager manager,SoundInstance sound) {
            manager.m_120367_(sound);
            MvpMusicHud.played(sound);
        }
        @Inject(method = "stopMusic", at = @At("RETURN"))
        private static void minestrike$stopped(CallbackInfo ci) { MvpMusicHud.clear(); }
    }
    @Mixin(targets = "com.phasetranscrystal.blockoffensive.client.screen.hud.CSMvpHud", remap = false)
    public static abstract class Panel {
        @Inject(method = "renderMVPInfoPanel", at = @At("RETURN"))
        private void minestrike$music(GuiGraphics graphics,PoseStack pose,float progress,float scale,int width,int height,long now,CallbackInfo ci) {
            MvpMusicHud.render(graphics,progress,scale,width,height);
        }
    }
    private MvpMusicMixin() {}
}
