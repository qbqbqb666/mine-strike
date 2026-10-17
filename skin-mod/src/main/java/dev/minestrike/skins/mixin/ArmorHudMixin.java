package dev.minestrike.skins.mixin;

import com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=CSGameHud.class,remap=false)
public abstract class ArmorHudMixin {
    @Inject(method="renderArmorBar",at=@At("HEAD"),cancellable=true)
    private void minestrike$armor(Minecraft mc,ForgeGui gui,GuiGraphics g,int x,int y,CallbackInfo ci) {
        if(mc.f_91074_!=null) {
            boolean helmet=minestrike$equipped(mc.f_91074_.m_6844_(EquipmentSlot.HEAD),EquipmentSlot.HEAD);
            boolean chest=minestrike$equipped(mc.f_91074_.m_6844_(EquipmentSlot.CHEST),EquipmentSlot.CHEST);
            minestrike$drawArmor(g,x,y,helmet,chest);
        }
        ci.cancel();
    }
    @Unique private static boolean minestrike$equipped(ItemStack stack,EquipmentSlot slot) {
        return !stack.m_41619_() && stack.m_41720_() instanceof ArmorItem armor && armor.m_40402_()==slot;
    }
    @Unique private static void minestrike$drawArmor(GuiGraphics g,int x,int y,boolean helmet,boolean chest) {
        minestrike$armorIcon(g,x-28,y+2,"  ######  \n ######## \n###    ###\n##      ##\n##      ##\n##      ##\n##      ##\n##      ##\n ##    ## ",helmet);
        minestrike$armorIcon(g,x-14,y+2,"###    ###\n####  ####\n##########\n##########\n ######## \n  ######  \n  ######  \n  ######  \n  ######  ",chest);
    }
    @Unique private static void minestrike$armorIcon(GuiGraphics g,int x,int y,String pattern,boolean equipped) {
        g.m_280509_(x-1,y-1,x+11,y+10,0xA0181C22);
        int color=equipped?0xFFF1F3F5:0xFF626970;
        String[] rows=pattern.split("\n");
        for(int row=0;row<rows.length;row++)for(int col=0;col<rows[row].length();col++)if(rows[row].charAt(col)=='#') {
            g.m_280509_(x+col,y+row+1,x+col+1,y+row+2,0xFF11151A);
            g.m_280509_(x+col,y+row,x+col+1,y+row+1,color);
        }
    }
}
