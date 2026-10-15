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
            new ResourceLocation(MineStrikeSkins.ID, "main"), () -> "4", "4"::equals, "4"::equals);

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

    public record LobbyAction(String action) {
        public static void encode(LobbyAction value, FriendlyByteBuf buf) { buf.m_130072_(value.action, 16); }
        public static LobbyAction decode(FriendlyByteBuf buf) { return new LobbyAction(buf.m_130136_(16)); }
        public static void handle(LobbyAction value, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player != null) MatchLobby.action(player, value.action);
            });
            context.setPacketHandled(true);
        }
    }
    public record LobbyState(String json) {
        public static void encode(LobbyState value, FriendlyByteBuf buf) { buf.m_130072_(value.json, 4096); }
        public static LobbyState decode(FriendlyByteBuf buf) { return new LobbyState(buf.m_130136_(4096)); }
        public static void handle(LobbyState value, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> SkinClient.acceptLobby(value)));
            context.setPacketHandled(true);
        }
    }

    public record ShopEditAction(int menu, String operation, int column, int row) {
        public static void encode(ShopEditAction v, FriendlyByteBuf b) { b.writeInt(v.menu); b.m_130072_(v.operation, 8); b.writeInt(v.column); b.writeInt(v.row); }
        public static ShopEditAction decode(FriendlyByteBuf b) { return new ShopEditAction(b.readInt(), b.m_130136_(8), b.readInt(), b.readInt()); }
        public static void handle(ShopEditAction v, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> { if (c.getSender() != null) ShopExpansion.action(c.getSender(), v); }); c.setPacketHandled(true);
        }
    }
    public record ShopShape(int[] counts) {
        public static void encode(ShopShape v, FriendlyByteBuf b) { for (int n : v.counts) b.writeInt(n); }
        public static ShopShape decode(FriendlyByteBuf b) { int[] n = new int[6]; for (int i=0;i<6;i++) n[i]=b.readInt(); return new ShopShape(n); }
        public static void handle(ShopShape v, Supplier<NetworkEvent.Context> supplier) {
            var c=supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExpandedShopClient.accept(v.counts))); c.setPacketHandled(true);
        }
    }
    public record ShopSave(int menu, String name, int price, int ammo, int group, int maxBuy, boolean ammoChanged) {
        public static void encode(ShopSave v, FriendlyByteBuf b) { b.writeInt(v.menu); b.m_130072_(v.name,80); b.writeInt(v.price); b.writeInt(v.ammo); b.writeInt(v.group); b.writeInt(v.maxBuy); b.writeBoolean(v.ammoChanged); }
        public static ShopSave decode(FriendlyByteBuf b) { return new ShopSave(b.readInt(),b.m_130136_(80),b.readInt(),b.readInt(),b.readInt(),b.readInt(),b.readBoolean()); }
        public static void handle(ShopSave v, Supplier<NetworkEvent.Context> supplier) {
            var c=supplier.get(); c.enqueueWork(() -> { if(c.getSender()!=null) ShopExpansion.save(c.getSender(),v); }); c.setPacketHandled(true);
        }
    }
    public record ShopBack(int menu) {
        public static void encode(ShopBack v, FriendlyByteBuf b) { b.writeInt(v.menu); }
        public static ShopBack decode(FriendlyByteBuf b) { return new ShopBack(b.readInt()); }
        public static void handle(ShopBack v, Supplier<NetworkEvent.Context> supplier) {
            var c=supplier.get(); c.enqueueWork(() -> { if(c.getSender()!=null) ShopExpansion.back(c.getSender(),v.menu); }); c.setPacketHandled(true);
        }
    }

    public record ShopPick(int menu, int inventoryIndex) {
        public static void encode(ShopPick v, FriendlyByteBuf b) { b.writeInt(v.menu); b.writeInt(v.inventoryIndex); }
        public static ShopPick decode(FriendlyByteBuf b) { return new ShopPick(b.readInt(),b.readInt()); }
        public static void handle(ShopPick v, Supplier<NetworkEvent.Context> supplier) {
            var c=supplier.get(); c.enqueueWork(() -> { if(c.getSender()!=null) ShopExpansion.pick(c.getSender(),v.menu,v.inventoryIndex); }); c.setPacketHandled(true);
        }
    }

    public record ShopFeedback(int menu, String message) {
        public static void encode(ShopFeedback v, FriendlyByteBuf b) { b.writeInt(v.menu); b.m_130072_(v.message,256); }
        public static ShopFeedback decode(FriendlyByteBuf b) { return new ShopFeedback(b.readInt(),b.m_130136_(256)); }
        public static void handle(ShopFeedback v, Supplier<NetworkEvent.Context> supplier) {
            var c=supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExpandedShopClient.feedback(v))); c.setPacketHandled(true);
        }
    }

    public static void init() {
        CHANNEL.registerMessage(0, Request.class, Request::encode, Request::decode, Request::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(1, State.class, State::encode, State::decode, State::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(2, LobbyAction.class, LobbyAction::encode, LobbyAction::decode, LobbyAction::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(3, LobbyState.class, LobbyState::encode, LobbyState::decode, LobbyState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(4, ShopEditAction.class, ShopEditAction::encode, ShopEditAction::decode, ShopEditAction::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(5, ShopShape.class, ShopShape::encode, ShopShape::decode, ShopShape::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(6, ShopSave.class, ShopSave::encode, ShopSave::decode, ShopSave::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(7, ShopBack.class, ShopBack::encode, ShopBack::decode, ShopBack::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(8, ShopPick.class, ShopPick::encode, ShopPick::decode, ShopPick::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(9, ShopFeedback.class, ShopFeedback::encode, ShopFeedback::decode, ShopFeedback::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void sendShopShape(ServerPlayer player, int[] counts) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new ShopShape(counts));
    }
    public static void sendLobby(ServerPlayer player, String json) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new LobbyState(json));
    }

    public static void send(ServerPlayer player, State state) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state);
        MatchLobby.send(player, "");
    }
}
