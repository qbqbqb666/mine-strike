package dev.minestrike.skins;

import com.google.gson.reflect.TypeToken;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;

public final class SkinNetwork {
    private static final int MAX_JSON = 65536;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineStrikeSkins.ID, "main"), () -> "1", "1"::equals, "1"::equals);

    public record Request(boolean save, String choices) {
        public static void encode(Request value, FriendlyByteBuf buf) {
            buf.writeBoolean(value.save); buf.m_130072_(value.choices, 8192);
        }
        public static Request decode(FriendlyByteBuf buf) {
            return new Request(buf.readBoolean(), buf.m_130136_(8192));
        }
        public static void handle(Request value, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!value.save) { MineStrikeSkins.reply(player, "选择后点击确认保存。", true); return; }
                try {
                    Map<String, String> choices = SkinCatalog.JSON.fromJson(value.choices,
                            new TypeToken<Map<String, String>>() {}.getType());
                    MineStrikeSkins.save(player, choices);
                } catch (RuntimeException e) {
                    MineStrikeSkins.reply(player, "无效的皮肤选择。", false);
                }
            });
            context.setPacketHandled(true);
        }
    }

    public record State(String catalog, String choices, boolean locked, boolean open, String message) {
        public static void encode(State value, FriendlyByteBuf buf) {
            buf.m_130072_(value.catalog, MAX_JSON); buf.m_130072_(value.choices, 8192);
            buf.writeBoolean(value.locked); buf.writeBoolean(value.open); buf.m_130072_(value.message, 512);
        }
        public static State decode(FriendlyByteBuf buf) {
            return new State(buf.m_130136_(MAX_JSON), buf.m_130136_(8192), buf.readBoolean(), buf.readBoolean(), buf.m_130136_(512));
        }
        public static void handle(State value, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkinClient.accept(value)));
            context.setPacketHandled(true);
        }
    }

    public static void init() {
        CHANNEL.registerMessage(0, Request.class, Request::encode, Request::decode, Request::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, State.class, State::encode, State::decode, State::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerPlayer player, State state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
    }
}
