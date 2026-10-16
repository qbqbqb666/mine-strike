package dev.minestrike.skins.mixin;

import com.atsuishio.superbwarfare.block.ContainerBlock;
import com.atsuishio.superbwarfare.init.ModItems;
import com.atsuishio.superbwarfare.init.ModTags;
import dev.minestrike.skins.MineStrikeSkins;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ContainerBlock.class, remap = false)
public abstract class ContainerToolMixin {
    @Inject(method = "m_6227_", at = @At("HEAD"), cancellable = true)
    private void minestrike$offhand(BlockState state, Level level, BlockPos pos, Player player,
                                    InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> ci) {
        if (hand != InteractionHand.OFF_HAND) return;
        ItemStack main = player.m_21120_(InteractionHand.MAIN_HAND), off = player.m_21120_(hand);
        boolean mainTool = main.m_204117_(ModTags.Items.TOOLS_CROWBAR) || main.m_150930_(ModItems.CROWBAR.get());
        boolean offTool = off.m_204117_(ModTags.Items.TOOLS_CROWBAR) || off.m_150930_(ModItems.CROWBAR.get());
        if (mainTool && !offTool) {
            minestrike$log("ignored-offhand", off, level, pos, player, hand);
            ci.setReturnValue(InteractionResult.PASS);
        }
    }

    @Redirect(method = "m_6227_", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;m_204117_(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean minestrike$crowbar(ItemStack item, TagKey<Item> tag, BlockState state, Level level,
                                        BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean tagged = item.m_204117_(tag);
        if (tagged || !tag.equals(ModTags.Items.TOOLS_CROWBAR)) return tagged;
        boolean nativeCrowbar = item.m_150930_(ModItems.CROWBAR.get());
        minestrike$log(nativeCrowbar ? "native-crowbar-fallback" : "rejected", item, level, pos, player, hand);
        return nativeCrowbar;
    }

    @Unique
    private static void minestrike$log(String result, ItemStack item, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!level.f_46443_) {
            String mode = player instanceof ServerPlayer server ? server.f_8941_.m_9290_().toString() : "unknown";
            MineStrikeSkins.LOG.info("[vehicle] container tool={} player={} hand={} item={} mode={} pos={}",
                    result, player.m_20148_(), hand,
                    ForgeRegistries.ITEMS.getKey(item.m_41720_()), mode, pos);
        }
    }
}
