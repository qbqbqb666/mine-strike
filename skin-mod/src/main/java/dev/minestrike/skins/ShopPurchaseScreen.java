package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.client.data.CSClientData;
import com.phasetranscrystal.blockoffensive.map.shop.ItemType;
import com.phasetranscrystal.fpsmatch.common.client.FPSMClient;
import com.phasetranscrystal.fpsmatch.common.packet.register.NetworkPacketRegister;
import com.phasetranscrystal.fpsmatch.common.packet.shop.ShopActionC2SPacket;
import com.phasetranscrystal.fpsmatch.core.shop.ShopAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class ShopPurchaseScreen extends ShopPanelScreen {
    private static final int CELL=172,ROW=98;
    public ShopPurchaseScreen() { super("购买装备",1080,190+ROW*Math.max(1,ExpandedShopClient.rows())); }
    @Override protected void m_7856_() {
        super.m_7856_();ct=FPSMClient.getGlobalData().equalsTeam("ct");
        m_142416_(ShopVisuals.button(left+panelWidth-136,top+24,110,30,"返回游戏",ct,b->m_7379_()));
    }
    @Override public void m_86600_() {
        if(CSClientData.isStart&&!CSClientData.canOpenShop) m_7379_();
    }
    @Override protected void drawPanel(GuiGraphics g,int mx,int my,float tick) {
        ShopVisuals.panel(g,left,top,panelWidth,panelHeight,ct);
        ShopVisuals.heading(g,(ct?"CT / 反恐精英":"T / 恐怖分子")+"  ·  装备补给",left+24,top+24,ShopVisuals.accent(ct));
        ShopVisuals.text(g,"可用资金 $ "+CSClientData.getMoney(),left+24,top+46,ShopVisuals.TEXT);
        ShopVisuals.text(g,"购买时间 "+CSClientData.shopCloseTime+" 秒",left+260,top+46,ShopVisuals.MUTED);
        for(int col=0;col<6;col++) {
            int x=left+24+col*CELL;
            ShopVisuals.heading(g,ShopVisuals.NAMES[col],x+8,top+84,ShopVisuals.accent(ct));
            if(ExpandedShopClient.count(col)==0) ShopVisuals.text(g,"暂无商品",x+8,top+146,ShopVisuals.MUTED);
            for(int row=0;row<ExpandedShopClient.count(col);row++) {
                int y=top+106+row*ROW;
                var slot=FPSMClient.getGlobalData().getSlotData(ItemType.values()[col].name(),row);
                var item=slot.itemStack();boolean hover=mx>=x&&mx<x+CELL-10&&my>=y&&my<y+ROW-8;
                boolean buy=!item.m_41619_()&&!slot.isLocked()&&CSClientData.getMoney()>=slot.cost();
                ShopVisuals.card(g,x,y,CELL-10,ROW-8,ct,hover&&buy);
                if(slot.boughtCount()>0) g.m_280509_(x+3,y+3,x+CELL-13,y+4,ShopVisuals.accent(ct));
                ShopVisuals.icon(g,item,x+8,y+17,CELL-26,45);
                ShopVisuals.fitText(g,item.m_41619_()?"未配置商品":item.m_41611_().getString(),x+8,y+7,CELL-26,buy?ShopVisuals.TEXT:ShopVisuals.MUTED);
                ShopVisuals.text(g,"$ "+slot.cost(),x+8,y+73,buy?ShopVisuals.accent(ct):ShopVisuals.MUTED);
                if(slot.canReturn()) {
                    g.m_280509_(x+CELL-62,y+68,x+CELL-16,y+85,0xDD315743);
                    ShopVisuals.text(g,"退款",x+CELL-51,y+73,0xFFBEE5CA);
                } else if(slot.isLocked()) ShopVisuals.text(g,"已购买",x+CELL-62,y+73,ShopVisuals.MUTED);
            }
        }

    }
    @Override protected boolean panelClick(double x,double y,int button) {
        if(button!=0||x<left+24||y<top+106)return false;
        int col=(int)(x-left-24)/CELL,row=(int)(y-top-106)/ROW;
        if(col<0||col>=6||row<0||row>=ExpandedShopClient.count(col)||x>=left+24+(col+1)*CELL-10||y>=top+106+(row+1)*ROW-8)return false;
        var slot=FPSMClient.getGlobalData().getSlotData(ItemType.values()[col].name(),row);
        boolean refund=slot.canReturn()&&x>=left+24+col*CELL+CELL-62&&y>=top+106+row*ROW+68;
        if(!refund&&(slot.itemStack().m_41619_()||slot.isLocked()||CSClientData.getMoney()<slot.cost()))return true;
        NetworkPacketRegister.getChannelFromCache(ShopActionC2SPacket.class).sendToServer(new ShopActionC2SPacket(
                FPSMClient.getGlobalData().getCurrentMap(),ItemType.values()[col],row,refund?ShopAction.RETURN:ShopAction.BUY));
        Minecraft.m_91087_().m_91106_().m_120367_(SimpleSoundInstance.m_263171_(SoundEvents.f_12490_,1));return true;
    }
}
