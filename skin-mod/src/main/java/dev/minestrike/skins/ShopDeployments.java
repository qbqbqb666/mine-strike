package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class ShopDeployments {
    public static final String ENABLED = "MineStrikeAllowDeploy", ISSUED = "MineStrikeDeployMap";
    private static final Map<CSGameMap, Set<BlockPos>> placed = new IdentityHashMap<>();
    private static final ThreadLocal<Attempt> current = new ThreadLocal<>();
    private static final class Attempt {
        final CSGameMap map;
        final List<BlockSnapshot> snapshots = new ArrayList<>();
        Attempt(CSGameMap map) { this.map = map; }
    }
    public static void configure(CompoundTag tag, boolean enabled) {
        if (enabled) tag.m_128379_(ENABLED, true); else tag.m_128473_(ENABLED);
        tag.m_128473_(ISSUED);
    }
    public static boolean enabled(ItemStack item) { return item.m_41783_() != null && item.m_41783_().m_128471_(ENABLED); }
    public static boolean issued(CompoundTag tag) {
        return tag != null && tag.m_128471_(ENABLED) && !tag.m_128461_(ISSUED).isEmpty();
    }
    public static boolean authorized(ItemStack item) { return item.m_41720_() instanceof BlockItem && issued(item.m_41783_()); }
    public static ItemStack issue(ItemStack source, ServerPlayer player) {
        if (!enabled(source) || !(source.m_41720_() instanceof BlockItem)) return source;
        CSGameMap map = MineStrikeSkins.map(player);
        if (map == null) return source;
        ItemStack item = source.m_41777_();
        item.m_41784_().m_128359_(ISSUED, map.getMapName());
        return item;
    }
    private static boolean allowed(CSGameMap map, BlockPos pos) {
        List<BlockPos> spawns = new ArrayList<>();
        for (var team : List.of(map.getTTeam(), map.getCTTeam())) for (var spawn : team.getSpawnPointsData())
            if (spawn.getDimension().equals(map.getServerLevel().m_46472_())) spawns.add(spawn.getPosition());
        return allowedPosition(pos, map.getMapArea(), map.getBombAreaData(), spawns);
    }
    public static boolean allowedPosition(BlockPos pos, com.phasetranscrystal.fpsmatch.core.data.AreaData mapArea,
                                          List<com.phasetranscrystal.fpsmatch.core.data.AreaData> bombs, List<BlockPos> spawns) {
        return contains(mapArea, pos) && bombs.stream().noneMatch(area -> contains(area, pos))
                && spawns.stream().noneMatch(spawn -> nearSpawn(pos, spawn));
    }
    private static boolean contains(com.phasetranscrystal.fpsmatch.core.data.AreaData area, BlockPos pos) {
        BlockPos a = area.pos1(), b = area.pos2();
        return pos.m_123341_() >= Math.min(a.m_123341_(), b.m_123341_()) && pos.m_123341_() <= Math.max(a.m_123341_(), b.m_123341_())
                && pos.m_123342_() >= Math.min(a.m_123342_(), b.m_123342_()) && pos.m_123342_() <= Math.max(a.m_123342_(), b.m_123342_())
                && pos.m_123343_() >= Math.min(a.m_123343_(), b.m_123343_()) && pos.m_123343_() <= Math.max(a.m_123343_(), b.m_123343_());
    }
    public static boolean nearSpawn(BlockPos pos, BlockPos spawn) {
        return Math.abs((long) pos.m_123341_() - spawn.m_123341_()) <= 2
                && Math.abs((long) pos.m_123343_() - spawn.m_123343_()) <= 2
                && Math.abs((long) pos.m_123342_() - spawn.m_123342_()) <= 3;
    }
    public static InteractionResult use(UseOnContext context) {
        ItemStack item = context.m_43722_();
        if (!authorized(item)) return ForgeHooks.onPlaceItemIntoWorld(context);
        if (!(context.m_43723_() instanceof ServerPlayer player)) return InteractionResult.FAIL;
        CSGameMap map = MineStrikeSkins.map(player);
        if (map == null || !map.isStart || MapBaseline.busy(map) || player.m_5833_()
                || map.getServerLevel() != context.m_43725_() || RoundItems.expired(item)
                || !map.getMapName().equals(item.m_41783_().m_128461_(ISSUED))) return deny(player, "当前无法部署，请在购买物品的地图比赛中使用。");
        BlockPos pos = new BlockPlaceContext(context).m_8083_();
        if (!allowed(map, pos) || !context.m_43725_().m_8055_(pos).m_60795_()) return deny(player, "请放在地图内的空位，并避开包点和出生点。");
        Attempt prior = current.get(), attempt = new Attempt(map);
        current.set(attempt);
        try {
            InteractionResult result = ForgeHooks.onPlaceItemIntoWorld(context);
            // Forge has finished all event handlers and rolled back canceled placements at this point.
            if (result.m_19077_()) for (BlockSnapshot snapshot : attempt.snapshots)
                placed.computeIfAbsent(map, key -> new LinkedHashSet<>()).add(snapshot.getPos().m_7949_());
            return result;
        } finally { if (prior == null) current.remove(); else current.set(prior); }
    }
    private static InteractionResult deny(ServerPlayer player, String text) {
        player.m_5661_(Component.m_237113_(text), true); return InteractionResult.FAIL;
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void place(BlockEvent.EntityPlaceEvent event) {
        Attempt attempt = current.get();
        if (attempt == null) return;
        List<BlockSnapshot> snapshots = event instanceof BlockEvent.EntityMultiPlaceEvent multi
                ? multi.getReplacedBlockSnapshots() : List.of(event.getBlockSnapshot());
        for (BlockSnapshot snapshot : snapshots) if (snapshot.getLevel() != attempt.map.getServerLevel()
                || !snapshot.getReplacedBlock().m_60795_() || !allowed(attempt.map, snapshot.getPos())) {
            event.setCanceled(true); return;
        }
        attempt.snapshots.addAll(snapshots);
    }
    public static void end(CSGameMap map) {
        Set<BlockPos> positions = placed.remove(map);
        if (positions == null) return;
        var level = map.getServerLevel();
        for (BlockPos pos : positions) {
            Clearable.m_18908_(level.m_7702_(pos));
            level.m_7731_(pos, Blocks.f_50016_.m_49966_(), 18);
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { placed.clear(); current.remove(); }
}
