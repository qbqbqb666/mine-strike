package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.event.CSGamePlayerJoinEvent;
import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import com.phasetranscrystal.fpsmatch.core.event.PlayerGetShopDataEvent;
import com.phasetranscrystal.fpsmatch.core.shop.slot.ShopSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.InputStream;
import java.util.*;

@Mod(MineStrikeSkins.ID)
public final class MineStrikeSkins {
    public static final String ID = "minestrike_skins";
    public static final Logger LOG = LoggerFactory.getLogger(ID);
    public static SkinCatalog catalog;
    private static SkinStore store;
    private static final Map<CSGameMap, Map<UUID, Map<String, String>>> matches = new IdentityHashMap<>();

    public MineStrikeSkins() {
        SkinNetwork.init();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void starting(ServerStartingEvent event) {
        Path game = FMLLoader.getGamePath();
        Path file = game.resolve("config/mine-strike-skins.json");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                try (InputStream in = getClass().getResourceAsStream("/default-skins.json")) {
                    if (in == null) throw new IllegalStateException("Missing bundled skin catalog");
                    Files.copy(in, file);
                }
            }
            catalog = SkinCatalog.JSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), SkinCatalog.class);
            catalog.validate();
            store = new SkinStore(game.resolve("mine-strike-data/player-skins"), catalog);
            matches.clear();
            LOG.info("Loaded {} skin groups from {}", catalog.groups.size(), file);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load skin catalog: " + file, e);
        }
    }

    @SubscribeEvent
    public void stopped(ServerStoppedEvent event) {
        matches.clear();
        store = null;
    }

    public static CSGameMap map(ServerPlayer player) {
        if (!FPSMCore.initialized()) return null;
        return FPSMCore.getInstance().getMapByPlayer(player).filter(CSGameMap.class::isInstance)
                .map(CSGameMap.class::cast).orElse(null);
    }

    public static boolean locked(ServerPlayer player) {
        CSGameMap map = map(player);
        return map != null && map.isStart;
    }

    public static Map<String, String> choices(CSGameMap map, UUID uuid) {
        Map<UUID, Map<String, String>> game = matches.get(map);
        if (game != null && map.isStart) return game.computeIfAbsent(uuid, key -> Map.copyOf(store.load(key)));
        return store.load(uuid);
    }

    public static void begin(CSGameMap map) {
        Map<UUID, Map<String, String>> game = new HashMap<>();
        for (UUID uuid : map.getMapTeams().getJoinedUUID()) game.put(uuid, Map.copyOf(store.load(uuid)));
        matches.put(map, game);
    }

    public static void end(CSGameMap map) {
        matches.remove(map);
    }

    public static void reply(ServerPlayer player, String message, boolean open) {
        if (store == null) return;
        CSGameMap map = map(player);
        SkinNetwork.send(player, new SkinNetwork.State(SkinCatalog.JSON.toJson(catalog),
                SkinCatalog.JSON.toJson(choices(map, player.m_20148_())), locked(player), open, message));
    }

    public static void save(ServerPlayer player, Map<String, String> choices) {
        if (locked(player)) {
            reply(player, "比赛已经开始，本局外观已锁定。", false);
            return;
        }
        try {
            for (SkinCatalog.Group group : catalog.groups) {
                String id = choices == null ? null : choices.get(group.key);
                if (id != null && !available(group, id)) {
                    reply(player, "皮肤资源未加载：" + group.name + "。请确认对应枪包或刀包已安装。", false);
                    return;
                }
            }
            store.save(player.m_20148_(), choices);
            CSGameMap map = map(player);
            if (map != null) map.getShop(player).ifPresent(shop -> {
                shop.getDefaultAndPutData(player.m_20148_());
                shop.syncShopData(player);
            });
            reply(player, "已保存，下一场比赛使用这些外观。", false);
        } catch (Exception e) {
            LOG.error("Cannot save skins for {}", player.m_20148_(), e);
            reply(player, "保存失败，请检查配置和服务器日志。", false);
        }
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) reply(player, "选择后点击确认保存。", true);
    }

    @SubscribeEvent
    public void join(CSGamePlayerJoinEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            if (event.getMap().isStart) choices(event.getMap(), player.m_20148_());
            reply(player, event.getMap().isStart ? "本局外观已锁定。" : "选择后点击确认保存。", !event.getMap().isStart);
        }
    }

    public static ItemStack apply(ItemStack source, Map<String, String> choices) {
        ItemStack copy = source.m_41777_();
        ShopBundles.applyExtraSkins(copy, choices);
        CompoundTag tag = copy.m_41783_();
        if (tag == null || catalog == null) return copy;
        String field = tag.m_128441_("GunId") ? "GunId" : "MeleeWeaponId";
        SkinCatalog.Group group = catalog.groupFor(field, tag.m_128461_(field));
        if (group == null || !group.item.equals(String.valueOf(ForgeRegistries.ITEMS.getKey(copy.m_41720_())))) return copy;
        String id = choices.get(group.key);
        if (id != null && group.skins.stream().anyMatch(s -> s.id.equals(id)) && available(group, id)) {
            tag.m_128359_(field, id);
        }
        return copy;
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void shop(PlayerGetShopDataEvent event) {
        if (store == null) return;
        ServerPlayer player = FPSMCore.getInstance().getPlayerByUUID(event.getPlayer()).orElse(null);
        if (player == null) return;
        Map<String, String> chosen = choices(map(player), event.getPlayer());
        Map data = new LinkedHashMap();
        ((Map<?, ?>) event.getData()).forEach((type, entries) -> {
            List<ShopSlot> personal = new ArrayList<>();
            for (Object value : (List<?>) entries) {
                ShopSlot original = (ShopSlot) value;
                ItemStack base = original.process().m_41777_();
                ItemStack skinned = apply(base, chosen);
                ShopSlot slot = new ShopSlot(() -> skinned.m_41777_(), original.getDefaultCost(),
                        original.getMaxBuyCount(), original.getGroupId(), candidate -> {
                            ItemStack normalized = candidate.m_41777_();
                            CompoundTag target = skinned.m_41783_();
                            CompoundTag actual = normalized.m_41783_();
                            CompoundTag before = base.m_41783_();
                            if (target != null && actual != null && before != null) {
                                for (String field : List.of("GunId", "MeleeWeaponId")) {
                                    if (target.m_128441_(field) && actual.m_128461_(field).equals(target.m_128461_(field))) {
                                        actual.m_128359_(field, before.m_128461_(field));
                                    } else if (target.m_128441_(field) && !target.m_128461_(field).equals(before.m_128461_(field))) {
                                        return false;
                                    }
                                }
                            }
                            return original.returningChecker.test(normalized);
                        });
                slot.setIndex(original.getIndex());
                slot.setCost(original.getCost());
                for (String listener : original.getListenerNames()) {
                    slot.addListener(FPSMCore.getInstance().getListenerModuleManager().getListenerModule(listener));
                }
                personal.add(slot);
            }
            data.put(type, personal);
        });
        event.setData(data);
    }

    public static ItemStack preview(SkinCatalog.Group group, String id) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(group.item));
        if (item == null) return ItemStack.f_41583_;
        ItemStack stack = new ItemStack(item);
        stack.m_41784_().m_128359_(group.tag, id);
        return stack;
    }

    public static void applyIssuedKits(CSGameMap map, ServerPlayer player) {
        Map<String, String> chosen = choices(map, player.m_20148_());
        var inventory = player.m_150109_();
        for (int slot = 0; slot < inventory.m_6643_(); slot++) {
            ItemStack original = inventory.m_8020_(slot);
            if (!original.m_41619_()) inventory.m_6836_(slot, apply(original, chosen));
        }
        player.f_36095_.m_6199_(inventory);
        player.f_36095_.m_38946_();
    }

    public static boolean available(SkinCatalog.Group group, String id) {
        if ("GunId".equals(group.tag)) {
            return com.tacz.guns.api.TimelessAPI.getCommonGunIndex(new ResourceLocation(id)).isPresent();
        }
        return me.xjqsh.lrtactical.api.LrTacticalAPI.getMeleeIndex(preview(group, id)).isPresent();
    }

    public static void updateMatchClients(CSGameMap map) {
        if (!map.isStart) { end(map); return; }
        for (var data : map.getMapTeams().getJoinedPlayers()) {
            data.getPlayer().ifPresent(player -> reply(player, "比赛已开始，本局外观已锁定。", false));
        }
    }
}
