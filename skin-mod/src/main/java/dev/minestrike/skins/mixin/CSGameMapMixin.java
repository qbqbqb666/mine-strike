package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MineStrikeSkins;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.map.BaseTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CSGameMap.class, remap = false)
public abstract class CSGameMapMixin {
    @Inject(method = "startGame", at = @At("HEAD"), remap = false)
    private void skinsStart(CallbackInfo ci) { MineStrikeSkins.begin((CSGameMap) (Object) this); }

    @Inject(method = "startGame", at = @At("TAIL"), remap = false)
    private void skinsStarted(CallbackInfo ci) { MineStrikeSkins.updateMatchClients((CSGameMap) (Object) this); }

    @Inject(method = "resetGame", at = @At("TAIL"), remap = false)
    private void skinsEnd(CallbackInfo ci) { MineStrikeSkins.end((CSGameMap) (Object) this); }

    @Redirect(method = "lambda$giveAllPlayersKits$73", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;m_41777_()Lnet/minecraft/world/item/ItemStack;", remap = false), remap = false)
    private ItemStack skinsKit(ItemStack original, ServerPlayer player, BaseTeam team) {
        return MineStrikeSkins.apply(original, MineStrikeSkins.choices((CSGameMap) (Object) this, player.m_20148_()));
    }
}
