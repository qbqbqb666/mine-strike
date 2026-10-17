package dev.minestrike.skins;

import com.phasetranscrystal.fpsmatch.core.map.BaseMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;

public final class MatchFeedback {
    public static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(MineStrikeSkins.ID,"match_feedback"),()->"1","1"::equals,"1"::equals);
    public record Notice(UUID id,String map) {
        public static void encode(Notice v,FriendlyByteBuf b){b.m_130077_(v.id);b.m_130072_(v.map,512);}
        public static Notice decode(FriendlyByteBuf b){return new Notice(b.m_130259_(),b.m_130136_(512));}
    }
    public record Stats(String map,Map<UUID,Double> damage) {
        public Stats {
            if(map==null||map.length()>512||damage==null||damage.size()>1024)throw new IllegalArgumentException("Invalid match damage snapshot");
            damage=Map.copyOf(damage);
            for(double n:damage.values())if(!Double.isFinite(n)||n<0)throw new IllegalArgumentException("Invalid match damage value");
        }
        public static void encode(Stats v,FriendlyByteBuf b){b.m_130072_(v.map,512);b.writeInt(v.damage.size());for(var e:v.damage.entrySet()){b.m_130077_(e.getKey());b.writeDouble(e.getValue());}}
        public static Stats decode(FriendlyByteBuf b){String map=b.m_130136_(512);int size=b.readInt();if(size<0||size>1024)throw new IllegalArgumentException("Invalid match damage count");Map<UUID,Double> d=new HashMap<>();for(int i=0;i<size;i++){UUID id=b.m_130259_();if(d.put(id,b.readDouble())!=null)throw new IllegalArgumentException("Duplicate player");}return new Stats(map,d);}
    }
    public static void init() {
        CHANNEL.registerMessage(0,Notice.class,Notice::encode,Notice::decode,(v,supplier)->{
            var context=supplier.get();context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Client.notice(v)));context.setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(1,Stats.class,Stats::encode,Stats::decode,(v,supplier)->{
            var context=supplier.get();context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->Client.stats=v));context.setPacketHandled(true);
        },Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }
    public static void send(ServerPlayer player,Stats stats){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),stats);}
    public static void announce(BaseMap map) {
        var notice=new Notice(UUID.randomUUID(),map.getMapName());int recipients=0;
        for(UUID id:map.getMapTeams().getJoinedPlayersWithSpec()) {
            ServerPlayer player=map.getServerLevel().m_7654_().m_6846_().m_11259_(id);
            if(player!=null){CHANNEL.send(PacketDistributor.PLAYER.with(()->player),notice);recipients++;}
        }
        MineStrikeSkins.LOG.info("[c4] planted send id={} map={} recipients={}",notice.id,notice.map,recipients);
    }
    @Mod.EventBusSubscriber(modid=MineStrikeSkins.ID,value=Dist.CLIENT)
    public static final class Client {
        private static final LinkedHashSet<UUID> notices=new LinkedHashSet<>();
        private static volatile Stats stats=new Stats("",Map.of());
        public static float damage(UUID player){return stats.damage.getOrDefault(player,0.0).floatValue();}
        public static boolean first(UUID id) {
            if(!notices.add(id))return false;
            if(notices.size()>128)notices.remove(notices.iterator().next());return true;
        }
        private static void notice(Notice notice) {
            if(first(notice.id))BombAudio.Client.announce(notice.id,notice.map);
        }
        @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event){notices.clear();stats=new Stats("",Map.of());}
    }
    private MatchFeedback() {}
}
