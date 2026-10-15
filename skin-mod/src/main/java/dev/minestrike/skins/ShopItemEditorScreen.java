package dev.minestrike.skins;

import com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu;
import dev.minestrike.skins.mixin.ShopSlotMenuAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import com.tacz.guns.api.item.IGun;
import java.util.List;

public final class ShopItemEditorScreen extends ShopPanelScreen implements MenuAccess<EditShopSlotMenu> {
    private final EditShopSlotMenu menu;
    private final Inventory inventory;
    private EditBox name,price,ammo,group,maxBuy;
    private Button save;
    private String error="", initialAmmo="";
    private boolean pending;
    public ShopItemEditorScreen(EditShopSlotMenu menu,Inventory inventory,Component title) { super("编辑商品",458,320);this.menu=menu;this.inventory=inventory; }
    @Override public EditShopSlotMenu m_6262_() { return menu; }
    private ItemStack item() { return menu.f_38839_.get(0).m_7993_(); }
    private EditBox field(String label,int x,int y,int width,String value,boolean numeric) {
        EditBox field=new EditBox(f_96547_,left+x,top+y,width,20,Component.m_237113_(label));
        field.m_94199_(numeric?8:80);
        if(numeric) field.m_94153_(s->s.matches("-?\\d*"));
        field.m_94144_(value);m_142416_(field);return field;
    }
    @Override protected void m_7856_() {
        super.m_7856_();ct=menu.getGuiItemStack().m_41784_().m_128461_("SelectedShop").equals("ct");
        var access=(ShopSlotMenuAccess)menu;
        String[] old=name==null?null:new String[]{name.m_94155_(),price.m_94155_(),ammo.m_94155_(),group.m_94155_(),maxBuy.m_94155_()};
        name=field("商品名称",222,60,214,old==null?menu.getName():old[0],false);
        price=field("价格",222,108,98,old==null?""+menu.getPrice():old[1],true);
        ammo=field("虚拟弹药",334,108,102,old==null?""+menu.getAmmo():old[2],true);
        if(old==null) initialAmmo=ammo.m_94155_();
        group=field("组 ID",222,156,98,old==null?""+menu.getGroupId():old[3],true);
        maxBuy=field("购买上限",334,156,102,old==null?""+access.mineStrikeSlot().getMaxBuyCount():old[4],true);
        m_142416_(ShopVisuals.button(left+350,top+42,86,14,"恢复默认名称",ct,b->name.m_94144_("")));
        m_142416_(ShopVisuals.button(left+222,top+260,98,26,"返回商店",ct,b->back()));
        save=m_142416_(ShopVisuals.button(left+332,top+260,104,26,"保存商品",ct,b->save()));
    }
    public void feedback(String message) { pending=false;save.f_93623_=true;error=message; }
    private void save() {
        try {
            int p=Integer.parseInt(price.m_94155_()),a=Integer.parseInt(ammo.m_94155_()),g=Integer.parseInt(group.m_94155_()),m=Integer.parseInt(maxBuy.m_94155_());
            if(p<0||p>16000||a<0||a>1000000||g< -1||m<1||m>64) {error="检查数值：价格 0–16000，上限 1–64";return;}
            if(item().m_41619_()) {error="请先点击背包中的物品";return;}
            pending=true;save.f_93623_=false;
            SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopSave(menu.f_38840_,name.m_94155_(),p,a,g,m,!ammo.m_94155_().equals(initialAmmo)));
        } catch(NumberFormatException e) {error="请完整填写数字";}
    }
    private void back() { if(!pending) {pending=true;SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopBack(menu.f_38840_));} }
    @Override public void m_7379_() { back(); }
    @Override protected void drawPanel(GuiGraphics g,int mx,int my,float tick) {
        ShopVisuals.panel(g,left,top,panelWidth,panelHeight,ct);
        ShopVisuals.heading(g,(ct?"CT":"T")+" / 编辑商品",left+20,top+18,ShopVisuals.accent(ct));
        ShopVisuals.card(g,left+20,top+44,182,110,ct,false);
        ShopVisuals.icon(g,item(),left+36,top+50,150,72);
        ShopVisuals.fitText(g,item().m_41619_()?"从下方背包选择商品":item().m_41611_().getString(),left+30,top+132,164,ShopVisuals.TEXT);
        ShopVisuals.text(g,"背包",left+20,top+168,ShopVisuals.MUTED);
        for(int r=0;r<4;r++) for(int c=0;c<9;c++) {
            int x=left+20+c*20,y=top+186+r*20+(r==3?5:0),index=r==3?c:9+r*9+c;
            boolean hover=mx>=x&&mx<x+18&&my>=y&&my<y+18;
            ShopVisuals.card(g,x-1,y-1,19,19,ct,hover);
            g.m_280480_(inventory.m_8020_(index),x,y);
            if(hover&&!inventory.m_8020_(index).m_41619_())
                ShopVisuals.fitText(g,inventory.m_8020_(index).m_41611_().getString(),left+20,top+286,182,ShopVisuals.MUTED);
        }
        String[] labels={"商品名称","价格","虚拟弹药","组 ID","购买上限"};
        int[] xs={222,222,334,222,334},ys={46,94,94,142,142};
        for(int i=0;i<labels.length;i++)ShopVisuals.text(g,labels[i],left+xs[i],top+ys[i],ShopVisuals.MUTED);
        ShopVisuals.text(g,"监听模块",left+222,top+192,ShopVisuals.MUTED);
        ShopVisuals.fitText(g,menu.getListeners(),left+222,top+208,214,ShopVisuals.TEXT);

        if(!error.isEmpty()) ShopVisuals.fitText(g,error,left+20,top+304,418,0xFFFF9B8C);
    }
    @Override protected boolean panelClick(double x,double y,int button) {
        if(button!=0||pending)return false;
        for(int r=0;r<4;r++)for(int c=0;c<9;c++) {
            int sx=left+20+c*20,sy=top+186+r*20+(r==3?5:0),index=r==3?c:9+r*9+c;
            if(x>=sx&&x<sx+18&&y>=sy&&y<sy+18) {
                ItemStack stack=inventory.m_8020_(index).m_41777_();if(stack.m_41619_())return true;
                ((ShopSlotMenuAccess)menu).mineStrikeHandler().setStackInSlot(0,stack);
                name.m_94144_(stack.m_41611_().getString());
                ammo.m_94144_(""+(stack.m_41720_() instanceof IGun gun?gun.getMaxDummyAmmoAmount(stack):0));
                initialAmmo=ammo.m_94155_();
                SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.ShopPick(menu.f_38840_,index));error="";return true;
            }
        }
        return false;
    }
    @Override public boolean m_7933_(int key,int scan,int mods) {
        if(key==256){back();return true;}
        for(EditBox field:List.of(name,price,ammo,group,maxBuy))if(field.m_7933_(key,scan,mods))return true;
        return super.m_7933_(key,scan,mods);
    }
}
