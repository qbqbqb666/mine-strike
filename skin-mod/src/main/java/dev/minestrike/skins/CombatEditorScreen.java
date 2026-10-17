package dev.minestrike.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class CombatEditorScreen extends ShopPanelScreen {
    private CombatNetwork.State state;
    private EditBox health;
    private final EditBox[] armor=new EditBox[4], parts=new EditBox[4];
    private String message="";
    private boolean pending;
    private Button save;
    private static final String[] ARMOR={"头盔护甲","胸甲护甲","护腿护甲","靴子护甲"};
    private static final String[] PARTS={"头部倍率","躯干倍率","双臂倍率","双腿倍率"};
    public CombatEditorScreen(CombatNetwork.State state) {super("玩家数值编辑器",580,394);this.state=state;message=state.message();}
    public static void accept(CombatNetwork.State state) {
        var mc=Minecraft.m_91087_();
        if(mc.f_91080_ instanceof CombatEditorScreen screen && screen.state.selected().equals(state.selected())) {
            screen.state=state;screen.message=state.message();screen.pending=false;
            if(screen.save!=null)screen.save.f_93623_=!state.maps().isEmpty();
        } else if(state.open() || mc.f_91080_ instanceof CombatEditorScreen) mc.m_91152_(new CombatEditorScreen(state));
    }
    private EditBox field(String name,int x,int y,int width,String value) {
        var box=new EditBox(f_96547_,left+x,top+y,width,20,Component.m_237113_(name));
        box.m_94199_(12);box.m_94153_(s->s.matches("\\d*(\\.\\d*)?"));box.m_94144_(value);m_142416_(box);return box;
    }
    private Button button(String name,int x,int y,int width,Button.OnPress press) {
        return m_142416_(Button.m_253074_(Component.m_237113_(name),press).m_252987_(left+x,top+y,width,20).m_253136_());
    }
    @Override protected void m_7856_() {
        super.m_7856_();
        var s=state.settings();
        String h=health==null?number(s.health()):health.m_94155_();
        String[] a=new String[4],p=new String[4];
        for(int i=0;i<4;i++) {a[i]=armor[i]==null?(s.armor().get(i)==null?"":number(s.armor().get(i))):armor[i].m_94155_();p[i]=parts[i]==null?String.format(Locale.ROOT,"%.1f",s.parts().get(i)):parts[i].m_94155_();}
        button("<",18,36,24,b->select(-1));button(">",538,36,24,b->select(1));
        health=field("总体生命值",22,87,102,h);
        for(int i=0;i<4;i++)armor[i]=field(ARMOR[i],146+i*104,87,96,a[i]);
        parts[0]=field(PARTS[0],30,164,104,p[0]);parts[1]=field(PARTS[1],30,239,104,p[1]);
        parts[2]=field(PARTS[2],446,211,104,p[2]);parts[3]=field(PARTS[3],446,291,104,p[3]);
        for(var box:parts)box.m_94153_(value->value.matches("\\d*(\\.\\d?)?"));
        button("关闭",22,365,90,b->m_7379_());
        button("护甲沿用原值",124,365,116,b->{if(!pending)for(var box:armor)box.m_94144_("");});
        save=button("保存当前地图",422,365,136,b->save());save.f_93623_=!state.maps().isEmpty();
    }
    private static String number(double n){return n==Math.rint(n)?Long.toString((long)n):Double.toString(n);}
    private void select(int step) {
        if(pending || state.maps().isEmpty())return;
        int index=state.maps().indexOf(state.selected());
        pending=true;save.f_93623_=false;
        CombatNetwork.CHANNEL.sendToServer(new CombatNetwork.Request(state.maps().get(Math.floorMod(index+step,state.maps().size())),""));
    }
    private void save() {
        if(pending || state.maps().isEmpty())return;
        try {
            List<Double>a=new ArrayList<>(),p=new ArrayList<>();
            for(var box:armor)a.add(box.m_94155_().isEmpty()?null:Double.parseDouble(box.m_94155_()));
            for(var box:parts)p.add(Double.parseDouble(box.m_94155_()));
            var settings=new CombatSettings(Double.parseDouble(health.m_94155_()),a,p);
            pending=true;save.f_93623_=false;message="正在保存…";
            CombatNetwork.CHANNEL.sendToServer(new CombatNetwork.Request(state.selected(),CombatSettings.JSON.toJson(settings)));
        } catch(NumberFormatException e){message="请完整填写生命值与四部位倍率";}
        catch(IllegalArgumentException e){message=e.getMessage();}
    }
    private void text(GuiGraphics g,String s,int x,int y,int color){g.m_280614_(f_96547_,Component.m_237113_(s),left+x,top+y,color,false);}
    private void rect(GuiGraphics g,int x,int y,int w,int h,int color){g.m_280509_(left+x,top+y,left+x+w,top+y+h,color);}
    private void block(GuiGraphics g,int x,int y,int w,int h,int color,int light,int dark) {
        rect(g,x,y,w,h,0xff161616);rect(g,x+2,y+2,w-4,h-4,color);
        rect(g,x+2,y+2,w-4,2,light);rect(g,x+2,y+2,2,h-4,light);
        rect(g,x+w-4,y+4,2,h-6,dark);rect(g,x+4,y+h-4,w-6,2,dark);
    }
    private void limb(GuiGraphics g,int x,int y,float angle) {
        var pose=g.m_280168_();pose.m_85836_();pose.m_252880_(left+x,top+y,0);pose.m_252781_(new org.joml.Quaternionf().rotationZ(angle));
        g.m_280509_(-10,0,10,69,0xff17212a);g.m_280509_(-8,2,8,45,0xff3a8c97);g.m_280509_(-8,45,8,67,0xffbd8c69);
        g.m_280509_(-8,2,-5,45,0xff61adb5);pose.m_85849_();
    }
    private void arrow(GuiGraphics g,int x,int y,int tx,int ty,int color) {
        int bend=(x+tx)/2;rect(g,Math.min(x,bend),y,Math.abs(x-bend)+2,2,color);
        rect(g,bend,Math.min(y,ty),2,Math.abs(y-ty)+2,color);rect(g,Math.min(bend,tx),ty,Math.abs(tx-bend)+2,2,color);
        int direction=tx>x?1:-1;
        for(int i=0;i<5;i++)rect(g,tx-direction*i,ty-i,2,2*i+2,color);
    }
    @Override public void m_88315_(GuiGraphics g,int mx,int my,float tick) {
        g.m_280509_(0,0,f_96543_,f_96544_,0xb0101010);
        g.m_280168_().m_85836_();g.m_280168_().m_85841_(scale,scale,1);
        drawPanel(g,Math.round(mx/scale),Math.round(my/scale),tick);
        // Render the native widgets directly, avoiding the shop's themed background.
        for(var widget:f_169369_)widget.m_88315_(g,Math.round(mx/scale),Math.round(my/scale),tick);
        g.m_280168_().m_85849_();
    }
    @Override protected void drawPanel(GuiGraphics g,int mx,int my,float tick) {
        block(g,0,0,panelWidth,panelHeight,0xffb9b9b9,0xffeeeeee,0xff555555);
        block(g,8,8,564,53,0xff555555,0xff888888,0xff292929);
        text(g,"玩家数值编辑器",22,17,0xffffffff);
        String map=state.selected().isEmpty()?"没有可用的比赛地图":state.selected();
        g.m_280614_(f_96547_,Component.m_237113_(f_96547_.m_92834_(map,476)),left+50,top+42,0xffffffff,false);
        text(g,"总体生命值",22,73,0xff252525);for(int i=0;i<4;i++)text(g,ARMOR[i],146+i*104,73,0xff252525);
        text(g,"实际生命点 · 护甲留空沿用原装备 · 部位倍率保留一位小数",22,115,0xff333333);
        block(g,17,134,546,211,0xff454545,0xff666666,0xff242424);
        rect(g,219,335,142,3,0xff303030);
        limb(g,256,197,.28f);limb(g,324,197,-.28f);
        block(g,266,193,48,66,0xff348e9b,0xff65bac1,0xff215761);
        rect(g,269,197,5,52,0xff3da0a9);rect(g,280,199,20,4,0xff2b6d7c);
        block(g,266,259,23,75,0xff454982,0xff6568a2,0xff2b305b);block(g,291,259,23,75,0xff454982,0xff6568a2,0xff2b305b);
        rect(g,269,324,18,7,0xff34343c);rect(g,294,324,18,7,0xff34343c);
        block(g,266,144,48,48,0xffbd8c69,0xffd4aa85,0xff7b513b);
        rect(g,269,147,42,10,0xff4c3529);rect(g,269,153,7,11,0xff4c3529);rect(g,305,153,6,11,0xff4c3529);
        rect(g,276,166,9,5,0xfff0e8d7);rect(g,296,166,9,5,0xfff0e8d7);rect(g,280,166,4,5,0xff546b9a);rect(g,296,166,4,5,0xff546b9a);
        rect(g,285,177,12,6,0xff714832);
        int head=0xffe5ba73,body=0xff83cdd1,arm=0xffa7d18a,leg=0xffa5acf0;
        text(g,PARTS[0],30,150,head);text(g,PARTS[1],30,225,body);text(g,PARTS[2],446,197,arm);text(g,PARTS[3],446,277,leg);
        arrow(g,140,175,260,168,head);arrow(g,140,250,260,232,body);arrow(g,440,222,347,231,arm);arrow(g,440,302,320,301,leg);
        text(g,pending?"正在处理…":message.isEmpty()?(state.configured()?"已配置 · 比赛内 T / CT 共用 · 下一小局应用":"未配置 · 保存后启用此地图数值"):message,22,351,0xff252525);
    }
}
