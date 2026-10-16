package dev.minestrike.skins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class AgentPlayers {
    public record Appearance(UUID player, String agent) {}
    private static List<Appearance> sent = List.of();
    private static final Map<ServerPlayer, Boolean> sized = new WeakHashMap<>();
    public static volatile Map<UUID,String> client = Map.of();

    public static String team(ServerPlayer player) {
        var map = MineStrikeSkins.map(player);
        if (map == null || !map.isStart || player.m_5833_()) return "";
        if (map.getTTeam().hasPlayer(player.m_20148_())) return "t";
        if (map.getCTTeam().hasPlayer(player.m_20148_())) return "ct";
        return "";
    }
    public static boolean active(Player player) {
        if (player.m_20148_() == null) return false;
        return player instanceof ServerPlayer server ? !team(server).isEmpty()
                : client.containsKey(player.m_20148_()) && !player.m_5833_();
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null || MineStrikeSkins.catalog == null) return;
        List<Appearance> state = new ArrayList<>();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            String team = team(player); boolean active = !team.isEmpty();
            if (!Objects.equals(sized.put(player, active), active)) player.m_6210_();
            if (active) state.add(new Appearance(player.m_20148_(), AgentSkins.chosen(team,
                    MineStrikeSkins.choices(MineStrikeSkins.map(player), player.m_20148_()))));
        }
        if (!state.equals(sent)) {
            sent = List.copyOf(state);
            SkinNetwork.CHANNEL.send(PacketDistributor.ALL.noArg(), new SkinNetwork.Agents(sent));
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player)
            SkinNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SkinNetwork.Agents(sent));
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { sent = List.of(); sized.clear(); }
    private AgentPlayers() {}
}
