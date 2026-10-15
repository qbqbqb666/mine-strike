package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class TeamRadar {
    private static final Map<UUID, RadarPolicy> sent = new HashMap<>();
    private static RadarPolicy policy(ServerPlayer player, boolean matchRunning) {
        if (!matchRunning) return new RadarPolicy(false, Set.of());
        CSGameMap map = MineStrikeSkins.map(player);
        if (map != null && map.isStart) for (var team : List.of(map.getTTeam(), map.getCTTeam()))
            if (team.hasPlayer(player.m_20148_())) return new RadarPolicy(true, new HashSet<>(team.getPlayerList()));
        return new RadarPolicy(true, Set.of());
    }
    public static void sync() {
        if (!FPSMCore.initialized()) return;
        var core = FPSMCore.getInstance();
        boolean running = core.getMapByClass(CSGameMap.class).stream().anyMatch(map -> map.isStart);
        for (ServerPlayer player : core.getServer().m_6846_().m_11314_()) {
            RadarPolicy state = policy(player, running);
            if (state.equals(sent.get(player.m_20148_()))) continue;
            SkinNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new SkinNetwork.RadarState(state.restricted(), new ArrayList<>(state.teammates())));
            sent.put(player.m_20148_(), state);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) { if (event.phase == TickEvent.Phase.END) sync(); }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) { sent.remove(event.getEntity().m_20148_()); sync(); }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { sent.remove(event.getEntity().m_20148_()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { sent.clear(); }
}
