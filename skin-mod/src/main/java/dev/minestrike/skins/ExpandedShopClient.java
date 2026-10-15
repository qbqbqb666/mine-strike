package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen;
import icyllis.modernui.mc.MuiScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import com.phasetranscrystal.fpsmatch.common.client.event.FPSMClientResetEvent;
import java.util.Arrays;

@Mod.EventBusSubscriber(modid=MineStrikeSkins.ID,value=Dist.CLIENT)
public final class ExpandedShopClient {
    private static int[] counts={5,5,5,5,5,0};
    public static int rows() { return Arrays.stream(counts).max().orElse(0); }
    public static int count(int column) { return counts[column]; }
    public static void accept(int[] shape) {
        if(shape.length!=6||Arrays.stream(shape).anyMatch(n->n<0||n>32))return;
        boolean changed=!Arrays.equals(counts,shape);counts=shape.clone();
        if(changed&&Minecraft.m_91087_().f_91080_ instanceof ShopPurchaseScreen)
            Minecraft.m_91087_().m_91152_(new ShopPurchaseScreen());
    }
    public static void feedback(SkinNetwork.ShopFeedback packet) {
        var screen=Minecraft.m_91087_().f_91080_;
        if(screen instanceof ShopItemEditorScreen item && item.m_6262_().f_38840_==packet.menu()) item.feedback(packet.message());
        if(screen instanceof ExpandedShopEditorScreen editor && editor.m_6262_().f_38840_==packet.menu()) editor.feedback(packet.message());
    }
    @SubscribeEvent public static void opening(ScreenEvent.Opening event) {
        if(event.getNewScreen() instanceof MuiScreen screen&&screen.getFragment() instanceof CSGameShopScreen)
            event.setNewScreen(new ShopPurchaseScreen());
    }
    @SubscribeEvent public static void reset(FPSMClientResetEvent event) { counts=new int[]{5,5,5,5,5,0}; }
}
