package dev.minestrike.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class MvpMusicHud {
    public static final ResourceLocation ICON = new ResourceLocation(MineStrikeSkins.ID,"textures/gui/mvp-music-box.png");
    private static SoundInstance playing;
    public record Row(int x,int y,int width,int height,int icon) {}
    public static Row layout(int width,int height,float scale) {
        int w = Math.min(width-16,(int)(580*scale));
        int h = Math.max(18,(int)(32*scale));
        int y = Math.min(height-h-4,(int)(190f*height/1080)+(int)(96*scale)+(int)(90*scale));
        return new Row((width-w)/2,y,w,h,Math.max(12,(int)(24*scale)));
    }
    public static String caption(String id,boolean active) {
        String name = MusicBoxes.name(id);
        return active && name != null ? "正在播放：" + name : null;
    }
    public static void played(SoundInstance sound) { playing = MusicBoxes.name(sound.m_7904_().toString()) == null ? null : sound; }
    public static void clear() { playing = null; }
    public static void render(GuiGraphics graphics,float progress,float scale,int width,int height) {
        if (progress < 1 || playing == null) return;
        Minecraft mc = Minecraft.m_91087_();
        String text = caption(playing.m_7904_().toString(),mc.m_91106_().m_120403_(playing));
        if (text == null) return;
        Row row = layout(width,height,scale);
        graphics.m_280509_(row.x(),row.y(),row.x()+row.width(),row.y()+row.height(),0xAA000000);
        int padding = Math.max(6,(int)(12*scale));
        graphics.m_280163_(ICON,row.x()+padding,row.y()+(row.height()-row.icon())/2,0,0,row.icon(),row.icon(),row.icon(),row.icon());
        float textScale = Math.max(.75f,1.1f*scale);
        var pose = graphics.m_280168_(); pose.m_85836_();
        try {
            pose.m_252880_(row.x()+padding+row.icon()+padding,row.y()+(row.height()-9*textScale)/2,0);
            pose.m_85841_(textScale,textScale,1);
            graphics.m_280056_(mc.f_91062_,text,0,0,0xFFE9D39C,true);
            graphics.m_280262_();
        } finally { pose.m_85849_(); }
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { clear(); }
    private MvpMusicHud() {}
}
