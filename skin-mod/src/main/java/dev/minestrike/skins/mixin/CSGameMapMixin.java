package dev.minestrike.skins.mixin;

import dev.minestrike.skins.MineStrikeSkins;
import dev.minestrike.skins.MatchLobby;
import dev.minestrike.skins.MapBaseline;
import dev.minestrike.skins.RoundItems;
import dev.minestrike.skins.ShopDeployments;
import dev.minestrike.skins.TeamRadar;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.map.BaseTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CSGameMap.class, remap = false)
public abstract class CSGameMapMixin {
    @Inject(method = "startGame", at = @At("HEAD"), cancellable = true, remap = false)
    private void skinsStart(CallbackInfo ci) {
        CSGameMap map = (CSGameMap) (Object) this;
        if (!MatchLobby.allowStart(map)) { ci.cancel(); return; }
        if (!MapBaseline.before(map, false)) { ci.cancel(); return; }
        MineStrikeSkins.begin(map);
    }

    @Inject(method = "startGame", at = @At("TAIL"), remap = false)
    private void skinsStarted(CallbackInfo ci) { MineStrikeSkins.updateMatchClients((CSGameMap) (Object) this); TeamRadar.sync(); }

    @Inject(method = "startNewRound", at = @At("HEAD"), cancellable = true, remap = false)
    private void baselineRound(CallbackInfo ci) {
        CSGameMap map = (CSGameMap) (Object) this;
        ShopDeployments.end(map);
        if (!map.isStart || !MapBaseline.before(map, true)) ci.cancel();
    }

    @Inject(method = {"roundVictory", "cleanupMap"}, at = @At("HEAD"), remap = false)
    private void expireRoundItems(CallbackInfo ci) { ShopDeployments.end((CSGameMap) (Object) this); RoundItems.end((CSGameMap) (Object) this); }

    @Inject(method = "resetGame", at = @At("HEAD"), remap = false)
    private void baselineCancel(CallbackInfo ci) { ShopDeployments.end((CSGameMap) (Object) this); RoundItems.end((CSGameMap) (Object) this); MapBaseline.cancel((CSGameMap) (Object) this); }

    @Inject(method = "resetGame", at = @At("TAIL"), remap = false)
    private void skinsEnd(CallbackInfo ci) { MineStrikeSkins.end((CSGameMap) (Object) this); MatchLobby.reset((CSGameMap) (Object) this); TeamRadar.sync(); }

    @Inject(method = {"startGame", "startNewRound"}, at = @At("TAIL"), remap = false)
    private void combatRound(CallbackInfo ci) { dev.minestrike.skins.CombatStats.round((CSGameMap)(Object)this); }

    @Inject(method = "resetGame", at = @At("TAIL"), remap = false)
    private void combatEnd(CallbackInfo ci) { dev.minestrike.skins.CombatStats.end((CSGameMap)(Object)this); }

    @Inject(method = "autoStartLogic", at = @At("HEAD"), cancellable = true, remap = false)
    private void lobbyAutoStart(CallbackInfo ci) { ci.cancel(); }

    @Inject(method = {"checkRoundVictory", "checkBlastingVictory"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void lobbySingleSide(CallbackInfo ci) {
        if (MatchLobby.singleSided((CSGameMap) (Object) this)) ci.cancel();
    }

    @Redirect(method = "lambda$giveAllPlayersKits$73", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;m_41777_()Lnet/minecraft/world/item/ItemStack;", remap = false), remap = false)
    private ItemStack skinsKit(ItemStack original, ServerPlayer player, BaseTeam team) {
        return MineStrikeSkins.apply(original, MineStrikeSkins.choices((CSGameMap) (Object) this, player.m_20148_()));
    }

    @Redirect(method = "lambda$cleanupMap$66", at = @At(value = "INVOKE",
            target = "Lcom/phasetranscrystal/blockoffensive/map/CSGameMap;givePlayerKits(Lnet/minecraft/server/level/ServerPlayer;)V",
            remap = false), remap = false, require = 2)
    private void skinsRespawnKits(CSGameMap map, ServerPlayer player) {
        map.givePlayerKits(player);
        MineStrikeSkins.applyIssuedKits(map, player);
    }
}
