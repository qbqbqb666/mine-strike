package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ShopExpansion;
import com.phasetranscrystal.fpsmatch.common.packet.shop.OpenEditorC2SPacket;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = OpenEditorC2SPacket.class, remap = false)
public abstract class ShopOpenEditorMixin {
    @Inject(method = "handleOpenEditor", at = @At("HEAD"), remap = false)
    private static void prepareEditor(ServerPlayer player, CallbackInfo ci) {
        ShopExpansion.prepareTool(player.m_21205_());
    }
}
