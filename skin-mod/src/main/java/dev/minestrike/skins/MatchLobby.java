package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.map.BaseTeam;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStoppedEvent;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class MatchLobby {
    private static final MatchVotes votes = new MatchVotes();
    private static final Set<CSGameMap> managed = Collections.newSetFromMap(new IdentityHashMap<>());
    private static CSGameMap current;
    private static boolean wasStarted;
    private static int ticks;
    public record State(String map, boolean available, boolean started, String team,
                        int total, int readyCount, int endCount, int tCount, int ctCount,
                        boolean ready, boolean ending, String message) {}
    public static CSGameMap map() {
        if (!FPSMCore.initialized()) return null;
        List<CSGameMap> maps = FPSMCore.getInstance().getMapByClass(CSGameMap.class);
        return maps.size() == 1 ? maps.get(0) : null;
    }
    private static Map<UUID, String> roster(CSGameMap map) {
        Map<UUID, String> online = new LinkedHashMap<>();
        for (ServerPlayer player : FPSMCore.getInstance().getServer().m_6846_().m_11314_()) {
            String team = map == null ? "" : map.getMapTeams().getTeamByPlayer(player)
                    .map(t -> t.name).filter(t -> t.equals("t") || t.equals("ct")).orElse("");
            online.put(player.m_20148_(), team);
        }
        return online;
    }
    private static CSGameMap refresh() {
        CSGameMap map = map();
        boolean started = map != null && map.isStart;
        if (current != map || wasStarted != started) votes.clear();
        current = map; wasStarted = started;
        if (FPSMCore.initialized()) votes.update(roster(map), started);
        return map;
    }
    public static void send(ServerPlayer player, String message) {
        CSGameMap map = refresh();
        Map<UUID, String> online = FPSMCore.initialized() ? roster(map) : Map.of();
        UUID id = player.m_20148_();
        State state = new State(map == null ? "" : map.getMapName(), map != null,
                map != null && map.isStart, online.getOrDefault(id, ""), online.size(),
                votes.readyCount(), votes.endCount(), (int) online.values().stream().filter("t"::equals).count(),
                (int) online.values().stream().filter("ct"::equals).count(), votes.ready(id), votes.ending(id), message);
        SkinNetwork.sendLobby(player, SkinCatalog.JSON.toJson(state));
    }
    private static void broadcast(String message) {
        if (FPSMCore.initialized()) for (ServerPlayer player : FPSMCore.getInstance().getServer().m_6846_().m_11314_())
            send(player, message);
    }
    public static void action(ServerPlayer player, String action) {
        CSGameMap map = refresh();
        if (action.equals("status")) { send(player, ""); return; }
        if (map == null) { send(player, "当前存档没有唯一的 CS 比赛配置。"); return; }
        String message = "";
        UUID id = player.m_20148_();
        switch (action) {
            case "t", "ct" -> {
                if (map.isStart) { send(player, "比赛中无法更换阵营。"); return; }
                BaseTeam target = action.equals("t") ? map.getTTeam() : map.getCTTeam();
                if (target.hasPlayer(id)) { send(player, "已经在该阵营。"); return; }
                if (target.getRemainingLimit() == 0) { send(player, "该阵营人数已满。"); return; }
                map.join(target.name, player);
                refresh();
                message = target.hasPlayer(id) ? "已加入 " + action.toUpperCase(Locale.ROOT) : "加入失败。";
            }
            case "ready" -> {
                if (map.isStart) { send(player, "比赛已开始。"); return; }
                String team = roster(map).get(id);
                if (!"t".equals(team) && !"ct".equals(team)) { send(player, "请先选择阵营。"); return; }
                votes.toggleReady(id);
                message = votes.ready(id) ? "已准备。" : "已取消准备。";
            }
            case "end" -> {
                if (!map.isStart) { send(player, "比赛开始后才能发起结束。"); return; }
                votes.toggleEnd(id);
                message = votes.ending(id) ? "已同意结束比赛，等待全员同意。" : "已撤回结束同意。";
            }
            default -> { send(player, "无效的比赛操作。"); return; }
        }
        String outcome = evaluate(map);
        broadcast(outcome.isEmpty() ? message : outcome);
    }
    private static String evaluate(CSGameMap map) {
        if (map == null) return "";
        if (!map.isStart && votes.allReady()) {
            managed.add(map);
            map.startGame();
            if (!map.isStart) {
                managed.remove(map); votes.clear(); refresh();
                return "开赛失败，请检查出生点和服务器日志。";
            }
            refresh();
            return "全员已准备，比赛已开始。";
        } else if (map.isStart && votes.allEnding()) {
            map.resetGame();
            refresh();
            return "全员同意，比赛已结束。";
        }
        return "";
    }
    public static boolean allowStart(CSGameMap map) {
        if (map != map()) return false;
        refresh();
        boolean allowed = !map.isStart && votes.allReady();
        if (!allowed) broadcast("所有在线玩家选边并准备后才能开赛。");
        return allowed;
    }
    public static boolean singleSided(CSGameMap map) {
        return managed.contains(map) && map.isStart &&
                (map.getTTeam().getOnlinePlayers().isEmpty() || map.getCTTeam().getOnlinePlayers().isEmpty());
    }
    public static boolean currentTeam(BaseTeam team) {
        CSGameMap map = map();
        return map != null && (team == map.getTTeam() || team == map.getCTTeam());
    }
    public static boolean sharedSpawns(BaseTeam team) {
        CSGameMap map = map();
        return map != null && managed.contains(map) && (team == map.getTTeam() || team == map.getCTTeam());
    }
    public static void reset(CSGameMap map) {
        managed.remove(map);
        if (current == map) { votes.clear(); wasStarted = false; }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !FPSMCore.initialized() || ++ticks % 20 != 0) return;
        CSGameMap map = refresh();
        broadcast(evaluate(map));
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        votes.forget(event.getEntity().m_20148_());
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        votes.clear(); managed.clear(); current = null; wasStarted = false; ticks = 0;
    }
}
