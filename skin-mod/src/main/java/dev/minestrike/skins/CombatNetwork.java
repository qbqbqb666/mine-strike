package dev.minestrike.skins;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import java.util.*;
import java.util.function.Supplier;

public final class CombatNetwork {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(MineStrikeSkins.ID,"combat"),()->"1","1"::equals,"1"::equals);
    public record Request(String map, String settings) {}
    public record State(List<String> maps,String selected,CombatSettings settings,boolean configured,boolean success,boolean open,String message) {}
    public static void init() {
        CHANNEL.registerMessage(0,Request.class,(v,b)->{b.m_130072_(v.map,512);b.m_130072_(v.settings,4096);},
            b->new Request(b.m_130136_(512),b.m_130136_(4096)),CombatNetwork::request,Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1,State.class,(v,b)->b.m_130072_(CombatSettings.JSON.toJson(v),32768),
            b->CombatSettings.JSON.fromJson(b.m_130136_(32768),State.class),CombatNetwork::state,Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void open(ServerPlayer p) { respond(p,"",false,"",true); }
    private static void respond(ServerPlayer p,String selected,boolean success,String message) { respond(p,selected,success,message,false); }
    private static void respond(ServerPlayer p,String selected,boolean success,String message,boolean open) {
        if(!CombatStats.authorized(p)) {p.m_5661_(Component.m_237113_("需要创造模式及管理员权限"),true);return;}
        var maps=CombatStats.maps();
        var map=maps.stream().filter(m->CombatStats.key(m).equals(selected)).findFirst().orElseGet(()->maps.isEmpty()?null:maps.get(0));
        CombatSettings s=null;
        if(map!=null)try{s=CombatSettings.load(CombatStats.file(map));}
        catch(Exception e){message="读取失败，请检查服务器日志";MineStrikeSkins.LOG.error("Cannot read combat settings",e);}
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new State(maps.stream().map(CombatStats::key).toList(),map==null?"":CombatStats.key(map),s==null?CombatSettings.defaults():s,s!=null,success,open,message));
    }
    private static void request(Request v,Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get();c.enqueueWork(()->{
            var p=c.getSender();if(p==null || !CombatStats.authorized(p))return;
            var map=CombatStats.maps().stream().filter(m->CombatStats.key(m).equals(v.map)).findFirst().orElse(null);
            if(map==null){respond(p,v.map,false,"比赛地图不存在");return;}
            if(v.settings.isEmpty()){respond(p,v.map,false,"");return;}
            try {
                CombatSettings s=CombatSettings.JSON.fromJson(v.settings,CombatSettings.class);
                if(s==null)throw new IllegalArgumentException("设置不能为空");
                s.save(CombatStats.file(map));
                respond(p,v.map,true,"已保存；下一小局生效");
            } catch(Exception e) {
                MineStrikeSkins.LOG.warn("Cannot save combat settings for {}",v.map,e);
                respond(p,v.map,false,e instanceof IllegalArgumentException?e.getMessage():"保存失败，请检查服务器日志");
            }
        });c.setPacketHandled(true);
    }
    private static void state(State v,Supplier<NetworkEvent.Context> supplier) {
        var c=supplier.get();c.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->CombatEditorScreen.accept(v)));c.setPacketHandled(true);
    }
    private CombatNetwork() {}
}
