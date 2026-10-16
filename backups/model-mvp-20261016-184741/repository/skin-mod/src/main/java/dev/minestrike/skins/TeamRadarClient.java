package dev.minestrike.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class TeamRadarClient {
    private static RadarPolicy policy = new RadarPolicy(true, Set.of());
    public static void accept(SkinNetwork.RadarState state) { policy = new RadarPolicy(state.restricted(), Set.copyOf(state.teammates())); }
    public static boolean hidden(Entity entity) {
        if (!(entity instanceof Player)) return false;
        return hidden(entity.m_20148_());
    }
    public static boolean hidden(UUID player) {
        var self = Minecraft.m_91087_().f_91074_;
        return self == null || !policy.visible(self.m_20148_(), player);
    }
    @SubscribeEvent public static void login(ClientPlayerNetworkEvent.LoggingIn event) { policy = new RadarPolicy(true, Set.of()); }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { policy = new RadarPolicy(true, Set.of()); }
}
