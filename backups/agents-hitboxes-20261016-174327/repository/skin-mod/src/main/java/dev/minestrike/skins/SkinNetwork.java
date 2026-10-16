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
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

public final class SkinNetwork {
    private static final int MAX_JSON = 65536;
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MineStrikeSkins.ID, "main"), () -> "8", "8"::equals, "8"::equals);

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

    public record ShopRemoveItem(int menu, int index) {
        public static void encode(ShopRemoveItem v, FriendlyByteBuf b) { b.writeInt(v.menu); b.writeInt(v.index); }
        public static ShopRemoveItem decode(FriendlyByteBuf b) { return new ShopRemoveItem(b.readInt(), b.readInt()); }
        public static void handle(ShopRemoveItem v, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> { if (c.getSender() != null) ShopExpansion.removeItem(c.getSender(), v.menu, v.index); }); c.setPacketHandled(true);
        }
    }
    public record ShopRoundOnly(int menu, int index, boolean enabled) {
        public static void encode(ShopRoundOnly v, FriendlyByteBuf b) { b.writeInt(v.menu); b.writeInt(v.index); b.writeBoolean(v.enabled); }
        public static ShopRoundOnly decode(FriendlyByteBuf b) { return new ShopRoundOnly(b.readInt(), b.readInt(), b.readBoolean()); }
        public static void handle(ShopRoundOnly v, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> { if (c.getSender() != null) ShopExpansion.roundOnly(c.getSender(), v.menu, v.index, v.enabled); }); c.setPacketHandled(true);
        }
    }
    public record ShopDeployable(int menu, int index, boolean enabled) {
        public static void encode(ShopDeployable v, FriendlyByteBuf b) { b.writeInt(v.menu); b.writeInt(v.index); b.writeBoolean(v.enabled); }
        public static ShopDeployable decode(FriendlyByteBuf b) { return new ShopDeployable(b.readInt(), b.readInt(), b.readBoolean()); }
        public static void handle(ShopDeployable v, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> { if (c.getSender() != null) ShopExpansion.deployable(c.getSender(), v.menu, v.index, v.enabled); }); c.setPacketHandled(true);
        }
    }
    public record ShopBundleState(int menu, List<ItemStack> items) {
        public static void encode(ShopBundleState v, FriendlyByteBuf b) {
            b.writeInt(v.menu); b.writeInt(v.items.size());
            for (ItemStack item : v.items) b.writeItemStack(item, false);
        }
        public static ShopBundleState decode(FriendlyByteBuf b) {
            int menu = b.readInt(), count = b.readInt();
            if (count < 0 || count > ShopBundles.MAX_ITEMS) throw new IllegalArgumentException("商品物品数量无效。");
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < count; i++) items.add(b.m_130267_());
            return new ShopBundleState(menu, items);
        }
        public static void handle(ShopBundleState v, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExpandedShopClient.bundleState(v))); c.setPacketHandled(true);
        }
    }
    public record ShopBundleView(String json) {
        public static void encode(ShopBundleView v, FriendlyByteBuf b) { b.m_130072_(v.json, 32768); }
        public static ShopBundleView decode(FriendlyByteBuf b) { return new ShopBundleView(b.m_130136_(32768)); }
        public static void handle(ShopBundleView v, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ExpandedShopClient.bundleView(v))); c.setPacketHandled(true);
        }
    }

    public record RadarState(boolean restricted, List<UUID> teammates) {
        public static void encode(RadarState state, FriendlyByteBuf b) {
            if (state.teammates.size() > 1024) throw new IllegalArgumentException("雷达队伍人数过多。");
            b.writeBoolean(state.restricted); b.writeInt(state.teammates.size());
            for (UUID id : state.teammates) { b.writeLong(id.getMostSignificantBits()); b.writeLong(id.getLeastSignificantBits()); }
        }
        public static RadarState decode(FriendlyByteBuf b) {
            boolean restricted = b.readBoolean(); int count = b.readInt();
            if (count < 0 || count > 1024) throw new IllegalArgumentException("雷达队伍人数无效。");
            List<UUID> teammates = new ArrayList<>();
            for (int i = 0; i < count; i++) teammates.add(new UUID(b.readLong(), b.readLong()));
            return new RadarState(restricted, teammates);
        }
        public static void handle(RadarState state, Supplier<NetworkEvent.Context> supplier) {
            var c = supplier.get(); c.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> TeamRadarClient.accept(state))); c.setPacketHandled(true);
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
        CHANNEL.registerMessage(10, ShopRemoveItem.class, ShopRemoveItem::encode, ShopRemoveItem::decode, ShopRemoveItem::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(11, ShopBundleState.class, ShopBundleState::encode, ShopBundleState::decode, ShopBundleState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(12, ShopBundleView.class, ShopBundleView::encode, ShopBundleView::decode, ShopBundleView::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
        CHANNEL.registerMessage(13, ShopRoundOnly.class, ShopRoundOnly::encode, ShopRoundOnly::decode, ShopRoundOnly::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(14, ShopDeployable.class, ShopDeployable::encode, ShopDeployable::decode, ShopDeployable::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
        CHANNEL.registerMessage(15, RadarState.class, RadarState::encode, RadarState::decode, RadarState::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
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
