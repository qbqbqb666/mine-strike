package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.common.entity.drop.MatchDropEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class RoundItems {
    public static final String ONLY = "MineStrikeRoundOnly", EPOCH = "MineStrikeIssuedRound";
    private static final Map<CSGameMap, String> rounds = new IdentityHashMap<>();
    private static final Set<String> active = ConcurrentHashMap.newKeySet();

    public static boolean enabled(ItemStack item) { return item.m_41783_() != null && item.m_41783_().m_128471_(ONLY); }
    public static void configure(CompoundTag tag, boolean enabled) {
        if (enabled) tag.m_128379_(ONLY, true); else tag.m_128473_(ONLY);
        tag.m_128473_(EPOCH);
    }
    public static boolean expired(CompoundTag tag, Set<String> activeRounds) {
        // Templates have no issued-round marker and must never be removed by runtime cleanup.
        return tag != null && tag.m_128441_(EPOCH) && !activeRounds.contains(tag.m_128461_(EPOCH));
    }
    private static String epoch(CSGameMap map) {
        return rounds.computeIfAbsent(map, key -> {
            String id = UUID.randomUUID().toString(); active.add(id); return id;
        });
    }
    public static ItemStack issue(ItemStack source, ServerPlayer player) {
        if (!enabled(source)) return source;
        ItemStack item = source.m_41777_();
        CSGameMap map = MineStrikeSkins.map(player);
        item.m_41784_().m_128359_(EPOCH, map == null ? "expired" : epoch(map));
        return item;
    }
    public static void end(CSGameMap map) {
        String id = rounds.remove(map);
        if (id != null) active.remove(id);
        sweep(map.getServerLevel().m_7654_());
    }
    private static boolean expired(ItemStack stack) { return !stack.m_41619_() && expired(stack.m_41783_(), active); }
    private static boolean clear(Container container) {
        boolean changed = false;
        for (int i = 0; i < container.m_6643_(); i++) if (expired(container.m_8020_(i))) {
            container.m_6836_(i, ItemStack.f_41583_); changed = true;
        }
        if (changed) container.m_6596_();
        return changed;
    }
    private static void clear(ServerPlayer player) {
        boolean changed = clear(player.m_150109_()) | clear(player.m_36327_());
        var menu = player.f_36096_;
        if (expired(menu.m_142621_())) { menu.m_142503_(ItemStack.f_41583_); changed = true; }
        // Also covers an item being moved through an open container at the round boundary.
        for (var slot : menu.f_38839_) if (expired(slot.m_7993_())) { slot.m_5852_(ItemStack.f_41583_); changed = true; }
        if (changed) { player.f_36095_.m_38946_(); menu.m_38946_(); }
    }
    private static ItemStack dropped(Entity entity) {
        if (entity instanceof ItemEntity item) return item.m_32055_();
        if (entity instanceof MatchDropEntity item) return item.getItem();
        return ItemStack.f_41583_;
    }
    private static void sweep(MinecraftServer server) {
        for (ServerPlayer player : server.m_6846_().m_11314_()) clear(player);
        for (var level : server.m_129785_()) {
            List<Entity> remove = new ArrayList<>();
            for (Entity entity : level.m_8583_()) if (expired(dropped(entity))) remove.add(entity);
            remove.forEach(Entity::m_146870_);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) clear(player);
    }
    @SubscribeEvent public static void entityJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().f_46443_ && expired(dropped(event.getEntity()))) event.setCanceled(true);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { rounds.clear(); active.clear(); }
}
