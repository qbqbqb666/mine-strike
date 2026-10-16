package dev.minestrike.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class AgentClient {
    private static final Map<Player,Boolean> sized = new WeakHashMap<>();
    public static void accept(List<AgentPlayers.Appearance> state) {
        Map<UUID,String> next = new HashMap<>();
        for (var entry : state) if (AgentSkins.find(entry.agent()) != null) next.put(entry.player(), entry.agent());
        AgentPlayers.client = Map.copyOf(next);
        refresh();
    }
    private static void refresh() {
        var level = Minecraft.m_91087_().f_91073_;
        if (level == null) return;
        for (Player player : level.m_6907_()) {
            boolean active = AgentPlayers.active(player);
            if (!Objects.equals(sized.put(player, active), active)) player.m_6210_();
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) refresh();
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        AgentPlayers.client = Map.of(); sized.clear();
    }
    private AgentClient() {}
}
