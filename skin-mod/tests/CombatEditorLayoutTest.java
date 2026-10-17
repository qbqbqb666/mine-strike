import dev.minestrike.skins.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.*;
import net.minecraft.network.chat.*;
import java.util.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.geom.Path2D;
import javax.imageio.ImageIO;
import java.nio.file.*;

// Draws the actual screen geometry into an offline preview; native widget rectangles are checked separately.
public class CombatEditorLayoutTest {
    private static final class Metrics extends Font {
        Metrics(){super(id->null,false);}
        @Override public int m_92895_(String s){return s.codePoints().map(c->c>255?9:6).sum();}
        @Override public int m_92852_(FormattedText text){return m_92895_(text.getString());}
        @Override public String m_92834_(String s,int max){while(m_92895_(s)>max&&!s.isEmpty())s=s.substring(0,s.length()-1);return s;}
        @Override public String m_92837_(String s,int max,boolean reverse){return m_92834_(s,max);}
        @Override public boolean m_92718_(){return false;}
    }
    private static final class Canvas extends GuiGraphics {
        BufferedImage image;
        Graphics2D draw;
        PoseStack pose;
        Canvas(){super(null,null);}
        void initialize(){image=new BufferedImage(1200,832,BufferedImage.TYPE_INT_ARGB);draw=image.createGraphics();pose=new PoseStack();draw.setColor(new Color(0xff20252a,true));draw.fillRect(0,0,1200,832);draw.scale(2,2);draw.translate(10,10);draw.setFont(new java.awt.Font("PingFang SC",java.awt.Font.PLAIN,9));}
        @Override public PoseStack m_280168_(){return pose;}
        @Override public void m_280509_(int x,int y,int x2,int y2,int color){
            var matrix=m_280168_().m_85850_().m_252922_();
            var path=new Path2D.Float();
            int[] xx={x,x2,x2,x}, yy={y,y,y2,y2};
            for(int i=0;i<4;i++){var p=matrix.transformPosition(new org.joml.Vector3f(xx[i],yy[i],0));if(i==0)path.moveTo(p.x,p.y);else path.lineTo(p.x,p.y);}path.closePath();
            draw.setColor(new Color(color,true));draw.fill(path);
        }
        @Override public int m_280614_(Font font,Component text,int x,int y,int color,boolean shadow){draw.setColor(new Color(color,true));draw.drawString(text.getString(),x,y+8);return x;}
    }
    private static void set(Object object,Class<?> owner,String name,Object value)throws Exception{var f=owner.getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    private static void check(boolean b,String msg){if(!b)throw new AssertionError(msg);}
    public static void main(String[] args)throws Exception {
        var state=new CombatNetwork.State(java.util.List.of("minecraft:overworld / dust2"),"minecraft:overworld / dust2",CombatSettings.defaults(),false,false,true,"");
        var screen=new CombatEditorScreen(state);var font=new Metrics();
        set(screen,Screen.class,"f_96547_",font);set(screen,Screen.class,"f_96543_",600);set(screen,Screen.class,"f_96544_",414);
        var init=CombatEditorScreen.class.getDeclaredMethod("m_7856_");init.setAccessible(true);init.invoke(screen);
        var widgetsField=Screen.class.getDeclaredField("f_169369_");widgetsField.setAccessible(true);
        var widgets=(java.util.List<?>)widgetsField.get(screen);
        check(widgets.size()==14,"Nine fields, map arrows, close/inherit/save buttons");
        java.util.List<Rectangle> bounds=new ArrayList<>();
        for(Object o:widgets){var w=(AbstractWidget)o;var rect=new Rectangle(w.m_252754_(),w.m_252907_(),w.m_5711_(),w.m_93694_());check(rect.x>=10&&rect.y>=10&&rect.x+rect.width<=590&&rect.y+rect.height<=404,"Widget stays within panel");for(var old:bounds)check(!rect.intersects(old),"Widgets do not overlap");bounds.add(rect);}
        var unsafeField=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        var canvas=(Canvas)((sun.misc.Unsafe)unsafeField.get(null)).allocateInstance(Canvas.class);canvas.initialize();
        set(screen,ShopPanelScreen.class,"left",0);set(screen,ShopPanelScreen.class,"top",0);
        var panel=CombatEditorScreen.class.getDeclaredMethod("drawPanel",GuiGraphics.class,int.class,int.class,float.class);panel.setAccessible(true);panel.invoke(screen,canvas,0,0,0f);
        for(int i=0;i<widgets.size();i++){
            var widget=(AbstractWidget)widgets.get(i);var b=bounds.get(i);int x=b.x-10,y=b.y-10;
            canvas.m_280509_(x,y,x+b.width,y+b.height,widget instanceof EditBox?0xff888888:0xffeeeeee);
            canvas.m_280509_(x+1,y+1,x+b.width-1,y+b.height-1,widget instanceof EditBox?0xff101010:0xff777777);
            String text=widget instanceof EditBox box?box.m_94155_():widget.m_6035_().getString();
            canvas.m_280614_(font,Component.m_237113_(text),x+4,y+6,0xffffffff,false);
        }
        canvas.m_280614_(font,Component.m_237113_("程序绘制预览（非游戏截图）"),4,398,0xffcccccc,false);
        if(args.length>0)ImageIO.write(canvas.image,"png",Path.of(args[0]).toFile());canvas.draw.dispose();
        System.out.println("PASS: actual editor widget creation, all nine input fields, button layout, panel bounds and offline geometry render");
    }
}
