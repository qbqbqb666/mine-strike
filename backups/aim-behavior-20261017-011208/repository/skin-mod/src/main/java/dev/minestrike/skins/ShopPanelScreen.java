package dev.minestrike.skins;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class ShopPanelScreen extends Screen {
    protected final int panelWidth, panelHeight;
    protected float scale;
    protected int left, top;
    protected boolean ct;
    protected ShopPanelScreen(String title,int width,int height) { super(Component.m_237113_(title));panelWidth=width;panelHeight=height; }
    @Override protected void m_7856_() {
        scale=Math.min(1f,Math.min((f_96543_-20f)/panelWidth,(f_96544_-20f)/panelHeight));
        left=Math.round((f_96543_/scale-panelWidth)/2);top=Math.round((f_96544_/scale-panelHeight)/2);
    }
    @Override public void m_88315_(GuiGraphics g,int mx,int my,float tick) {
        ShopVisuals.background(g,f_96543_,f_96544_,ct);
        g.m_280168_().m_85836_();g.m_280168_().m_85841_(scale,scale,1);
        drawPanel(g,Math.round(mx/scale),Math.round(my/scale),tick);
        super.m_88315_(g,Math.round(mx/scale),Math.round(my/scale),tick);
        g.m_280168_().m_85849_();
    }
    protected abstract void drawPanel(GuiGraphics g,int mx,int my,float tick);
    protected boolean panelClick(double x,double y,int button) { return false; }
    @Override public boolean m_6375_(double x,double y,int button) { return super.m_6375_(x/scale,y/scale,button)||panelClick(x/scale,y/scale,button); }
    @Override public boolean m_6348_(double x,double y,int button) { return super.m_6348_(x/scale,y/scale,button); }
    @Override public boolean m_7979_(double x,double y,int button,double dx,double dy) { return super.m_7979_(x/scale,y/scale,button,dx/scale,dy/scale); }
    @Override public void m_94757_(double x,double y) { super.m_94757_(x/scale,y/scale); }
    @Override public boolean m_7043_() { return false; }
}
