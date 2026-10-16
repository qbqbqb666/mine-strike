package dev.minestrike.skins.mixin;

import dev.minestrike.skins.ShopExpansion;
import com.phasetranscrystal.fpsmatch.common.item.ShopEditTool;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShopEditTool.class, remap = false)
public abstract class ShopToolOpenMixin {
    @Inject(method = "m_7203_", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/network/NetworkHooks;openScreen(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/MenuProvider;Ljava/util/function/Consumer;)V"), remap = false)
    private void prepareEditor(Level level, Player player, InteractionHand hand,
                               CallbackInfoReturnable<InteractionResultHolder<ItemStack>> ci) {
        if (player instanceof ServerPlayer) ShopExpansion.prepareTool(player.m_21120_(hand));
    }
}
