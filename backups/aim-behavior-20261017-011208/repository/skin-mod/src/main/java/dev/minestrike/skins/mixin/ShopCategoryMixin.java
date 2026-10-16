package dev.minestrike.skins.mixin;

import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Arrays;

@Mixin(value = ItemType.class, remap = false)
public abstract class ShopCategoryMixin {
    @Shadow @Final @Mutable private static ItemType[] $VALUES;
    @Invoker("<init>")
    private static ItemType mineStrikeCreate(String name, int ordinal, int index, boolean dropUnlock) {
        throw new AssertionError();
    }
    @Inject(method = "<clinit>", at = @At("TAIL"), remap = false)
    private static void mysteryCategory(CallbackInfo ci) {
        int next = $VALUES.length;
        ItemType mystery = mineStrikeCreate("MYSTERY", next, next, true);
        $VALUES = Arrays.copyOf($VALUES, next + 1);
        $VALUES[next] = mystery;
    }
}
