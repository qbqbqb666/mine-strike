package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

@Mod.EventBusSubscriber(modid=MineStrikeSkins.ID)
public final class MatchDamage {
    public interface State { boolean minestrike$combatLive(); }
    private static final Map<CSGameMap,Map<UUID,Double>> totals=new IdentityHashMap<>();
    private static final Map<ServerPlayer,MatchFeedback.Stats> sent=new WeakHashMap<>();
    public static double units(double before,double after,double maximum) {
        if(!Double.isFinite(before)||!Double.isFinite(after)||!Double.isFinite(maximum)||maximum<=0)return 0;
        return Math.max(0,Math.max(0,before)-Math.max(0,after))/maximum*100;
    }
    public static void start(CSGameMap map) { if(map.isStart)totals.put(map,new HashMap<>()); }
    public static void end(CSGameMap map) { totals.remove(map); }
    public static String team(CSGameMap map,UUID id) {
        if(map.getTTeam().hasPlayer(id))return "t";
        if(map.getCTTeam().hasPlayer(id))return "ct";
        return "";
    }
    public static boolean eligible(boolean live,boolean sameMap,String attackerTeam,String victimTeam,boolean self) {
        return live && sameMap && !self && !attackerTeam.isEmpty() && !victimTeam.isEmpty() && !attackerTeam.equals(victimTeam);
    }
    public static void record(Player target,DamageSource source,float before,float maximum) {
        if(!(target instanceof ServerPlayer victim)||!(source.m_7639_() instanceof ServerPlayer attacker))return;
        CSGameMap map=MineStrikeSkins.map(victim);
        if(map==null||!eligible(map.isStart&&((State)map).minestrike$combatLive(),MineStrikeSkins.map(attacker)==map,
                team(map,attacker.m_20148_()),team(map,victim.m_20148_()),attacker==victim))return;
        double amount=units(before,victim.m_21223_(),maximum);
        if(amount>0 && totals.containsKey(map))totals.get(map).merge(attacker.m_20148_(),amount,Double::sum);
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var server=ServerLifecycleHooks.getCurrentServer();if(server==null)return;
        for(var player:server.m_6846_().m_11314_()) {
            CSGameMap map=MineStrikeSkins.map(player);
            var value=map!=null&&map.isStart?new MatchFeedback.Stats(CombatStats.key(map),Map.copyOf(totals.getOrDefault(map,Map.of()))):new MatchFeedback.Stats("",Map.of());
            if(!value.equals(sent.put(player,value)))MatchFeedback.send(player,value);
        }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent event) { totals.clear();sent.clear(); }
    private MatchDamage() {}
}
