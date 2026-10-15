package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.*;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class MapBaseline {
    private static final String OWNER = "MineStrikeBaselineMap", GENERATION = "MineStrikeBaselineGeneration";
    private static Job job;
    private static CSGameMap resuming;
    private static String generation;
    private static boolean spawning;
    public static boolean busy(CSGameMap map) { return job != null && job.map == map; }
    public static void cancel(CSGameMap map) {
        if (busy(map)) { job.release(); job = null; }
    }
    public static boolean exists(CSGameMap map) { return Files.isRegularFile(file(map)); }
    private static Path directory(CSGameMap map) {
        return map.getServerLevel().m_7654_().m_129843_(LevelResource.f_78182_).resolve("mine-strike-data/map-baselines");
    }
    public static Path file(CSGameMap map) {
        String name = Base64.getUrlEncoder().withoutPadding().encodeToString(map.getMapName().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return directory(map).resolve(name + ".nbt");
    }
    private static BaselineData.Bounds bounds(CSGameMap map) {
        var a = map.getMapArea().pos1(); var b = map.getMapArea().pos2();
        return new BaselineData.Bounds(Math.min(a.m_123341_(), b.m_123341_()), Math.min(a.m_123342_(), b.m_123342_()),
                Math.min(a.m_123343_(), b.m_123343_()), Math.abs(a.m_123341_() - b.m_123341_()) + 1,
                Math.abs(a.m_123342_() - b.m_123342_()) + 1, Math.abs(a.m_123343_() - b.m_123343_()) + 1);
    }
    private static String dimension(CSGameMap map) { return map.getServerLevel().m_46472_().m_135782_().toString(); }
    private static void message(String text) {
        if (!FPSMCore.initialized()) return;
        MatchLobby.notifyAll(text);
        for (var player : FPSMCore.getInstance().getServer().m_6846_().m_11314_())
            player.m_5661_(Component.m_237113_(text), false);
    }
    private static String epoch(CSGameMap map) {
        if (generation == null) {
            Path path = directory(map).resolve("generation.txt");
            try { generation = Files.exists(path) ? Files.readString(path) : "initial"; }
            catch (Exception e) { throw new IllegalStateException("无法读取对象重置标记。", e); }
        }
        return generation;
    }
    private static void cancelPendingExplosions(ServerLevel level) throws Exception {
        try {
            Class<?> manager = Class.forName("com.vinlanx.explosionoverhaul.AsyncCraterManager");
            manager.getMethod("onLevelUnload", ServerLevel.class).invoke(null, level);
        } catch (ClassNotFoundException ignored) { }
    }
    private static boolean transientEntity(Entity entity) {
        String id = EntityType.m_20613_(entity.m_6095_()).toString();
        return entity instanceof ItemEntity || entity instanceof Projectile || id.contains("c4") ||
                id.contains("match_drop") || id.contains("blast_bomb") || entity instanceof net.minecraft.world.entity.item.PrimedTnt;
    }
    private static void mark(Entity entity, CSGameMap map) {
        if (entity instanceof Player) return;
        entity.getPersistentData().m_128359_(OWNER, map.getMapName());
        entity.getPersistentData().m_128359_(GENERATION, epoch(map));
    }
    public static boolean before(CSGameMap map, boolean round) {
        if (resuming == map) return true;
        if (busy(map)) return false;
        if (!exists(map)) { message("尚未保存地图基准，请管理员执行 /minestrike baseline save。"); return false; }
        try {
            job = new Job(map, false, round);
            for (var player : map.getServerLevel().m_7654_().m_6846_().m_11314_()) player.m_8127_();
            map.teleportPlayerToMatchEndPoint();
            message(round ? "正在恢复地图，完成后开始下一回合。" : "正在恢复地图，完成后开始比赛。");
            return false;
        } catch (Exception e) { job = null; MineStrikeSkins.LOG.error("Cannot start map restore", e); message("地图恢复启动失败：" + e.getMessage()); return false; }
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.m_82127_("minestrike")
            .then(Commands.m_82127_("baseline").requires(source -> source.m_6761_(2))
                .then(Commands.m_82127_("save").executes(context -> {
                    CSGameMap map = MatchLobby.map();
                    if (map == null || map.isStart || job != null) {
                        context.getSource().m_81352_(Component.m_237113_("需要唯一的 CS 地图，且比赛及保存/恢复任务均未运行。")); return 0;
                    }
                    try { job = new Job(map, true, false); message("正在保存当前地图基准，请勿修改地图。"); return 1; }
                    catch (Exception e) { context.getSource().m_81352_(Component.m_237113_("无法保存基准：" + e.getMessage())); return 0; }
                }))
                .then(Commands.m_82127_("restore").executes(context -> {
                    CSGameMap map = MatchLobby.map();
                    if (map == null || map.isStart || job != null) {
                        context.getSource().m_81352_(Component.m_237113_("需要唯一的 CS 地图，且比赛及保存/恢复任务均未运行。")); return 0;
                    }
                    if (!exists(map)) {
                        context.getSource().m_81352_(Component.m_237113_("当前地图尚未保存基准，无法恢复。")); return 0;
                    }
                    try {
                        job = new Job(map, false, false, false);
                        for (var player : map.getServerLevel().m_7654_().m_6846_().m_11314_()) player.m_8127_();
                        map.teleportPlayerToMatchEndPoint();
                        MatchLobby.reset(map);
                        message("正在恢复地图基准，完成后保持未开赛状态。"); return 1;
                    } catch (Exception e) {
                        if (job != null) { job.release(); job = null; }
                        MineStrikeSkins.LOG.error("Cannot start manual map restore", e);
                        context.getSource().m_81352_(Component.m_237113_("无法恢复基准：" + e.getMessage())); return 0;
                    }
                }))
                .then(Commands.m_82127_("status").executes(context -> {
                    CSGameMap map = MatchLobby.map();
                    String text = map == null ? "当前存档没有唯一 CS 地图。" : "地图：" + map.getMapName() + "，" +
                            (busy(map) ? "保存/恢复处理中" : exists(map) ? "已保存基准" : "尚未保存基准") + "，文件：" + file(map);
                    context.getSource().m_288197_(() -> Component.m_237113_(text), false); return 1;
                }))));
    }
    private static final class Job {
        final CSGameMap map; final ServerLevel level; final boolean saving, round, resumeGame;
        final BaselineData.Bounds bounds; final int[] blocks; final Path destination;
        final ListTag palette = new ListTag(), tiles = new ListTag(), entities = new ListTag();
        final Map<BlockState, Integer> ids = new HashMap<>();
        final List<int[]> pinned = new ArrayList<>();
        final CompletableFuture<BaselineData> loading;
        CompletableFuture<Void> writing;
        BaselineData data; List<BlockState> states;
        int index, phase, cx, cz, tileIndex; boolean chunksLoaded, entityDelay;
        Job(CSGameMap map, boolean saving, boolean round) {
            this(map, saving, round, true);
        }
        Job(CSGameMap map, boolean saving, boolean round, boolean resumeGame) {
            this.map = map; this.level = map.getServerLevel(); this.saving = saving; this.round = round; this.resumeGame = resumeGame;
            destination = file(map);
            bounds = bounds(map); blocks = saving ? new int[bounds.volume()] : null;
            cx = bounds.x() >> 4; cz = bounds.z() >> 4;
            loading = saving ? null : CompletableFuture.supplyAsync(() -> {
                try { return BaselineData.read(destination); } catch (Exception e) { throw new CompletionException(e); }
            });
        }
        BlockPos position(int i) { return new BlockPos(bounds.px(i), bounds.py(i), bounds.pz(i)); }
        boolean loadChunk() {
            if (chunksLoaded) return true;
            if (!level.m_8902_().contains(net.minecraft.world.level.ChunkPos.m_45589_(cx, cz))) {
                level.m_8602_(cx, cz, true); pinned.add(new int[]{cx, cz});
            }
            level.m_6325_(cx, cz);
            if (++cx > (bounds.x() + bounds.sx() - 1) >> 4) {
                cx = bounds.x() >> 4;
                if (++cz > (bounds.z() + bounds.sz() - 1) >> 4) chunksLoaded = true;
            }
            return false;
        }
        void release() { for (int[] chunk : pinned) level.m_8602_(chunk[0], chunk[1], false); }
        boolean step() throws Exception {
            if (!saving && data == null) {
                if (!loading.isDone()) return false;
                data = loading.join();
                if (!data.map().equals(map.getMapName()) || !data.dimension().equals(dimension(map)) || !data.bounds().equals(bounds))
                    throw new IllegalStateException("基准与当前地图区域不一致，需要重新保存。");
                states = new ArrayList<>();
                for (int i = 0; i < data.palette().size(); i++) {
                    CompoundTag tag = data.palette().m_128728_(i);
                    if (!ForgeRegistries.BLOCKS.containsKey(new ResourceLocation(tag.m_128461_("Name"))))
                        throw new IllegalStateException("基准使用的方块模组未安装：" + tag.m_128461_("Name"));
                    BlockState state = NbtUtils.m_247651_(BuiltInRegistries.f_256975_.m_255303_(), tag);
                    if (!NbtUtils.m_129202_(state).equals(tag)) throw new IllegalStateException("基准方块属性无法还原。");
                    states.add(state);
                }
                for (int i = 0; i < data.entities().size(); i++)
                    if (EntityType.m_20632_(data.entities().m_128728_(i).m_128461_("id")).isEmpty())
                        throw new IllegalStateException("基准实体模组未安装。");
            }
            if (!loadChunk()) return false;
            if (phase == 0) {
                cancelPendingExplosions(level);
                if (saving) {
                    for (Entity entity : level.m_8583_()) {
                        if (!map.getMapArea().isEntityInArea(entity) || entity instanceof Player) continue;
                        mark(entity, map);
                        if (transientEntity(entity) || entity.m_20159_()) continue;
                        if (entity.m_142429_().anyMatch(Player.class::isInstance)) throw new IllegalStateException("请先离开地图内载具，再保存基准。");
                        CompoundTag tag = new CompoundTag();
                        if (!entity.m_20086_(tag)) throw new IllegalStateException("实体不支持保存：" + EntityType.m_20613_(entity.m_6095_()));
                        entities.add(tag);
                    }
                } else {
                    String next = UUID.randomUUID().toString();
                    Files.createDirectories(directory(map));
                    Files.writeString(directory(map).resolve("generation.txt"), next);
                    generation = next;
                    for (ServerLevel world : level.m_7654_().m_129785_()) {
                        List<Entity> remove = new ArrayList<>();
                        for (Entity entity : world.m_8583_()) if (!(entity instanceof Player) &&
                            ((world == level && map.getMapArea().isEntityInArea(entity)) || entity.getPersistentData().m_128461_(OWNER).equals(map.getMapName()))) remove.add(entity);
                        for (Entity entity : remove) { entity.m_20153_(); entity.m_146870_(); }
                    }
                    clearTicks();
                }
                phase = 1;
            }
            long deadline = System.nanoTime() + 8_000_000;
            int count = 0;
            if (phase == 1) {
                while (index < bounds.volume() && count++ < 16384 && System.nanoTime() < deadline) {
                    BlockPos pos = position(index);
                    if (saving) {
                        BlockState state = level.m_8055_(pos);
                        Integer id = ids.get(state);
                        if (id == null) { id = palette.size(); ids.put(state, id); palette.add(NbtUtils.m_129202_(state)); }
                        blocks[index] = id;
                        BlockEntity tile = level.m_7702_(pos);
                        if (tile != null) tiles.add(tile.m_187480_());
                    } else {
                        BlockState wanted = states.get(data.blocks()[index]);
                        if (!level.m_8055_(pos).equals(wanted)) {
                            BlockEntity existing = level.m_7702_(pos);
                            Clearable.m_18908_(existing);
                            if (existing != null) level.m_46747_(pos);
                            level.m_7731_(pos, wanted, 18);
                        }
                    }
                    index++;
                }
                if (index < bounds.volume()) return false;
                phase = 2;
                if (saving) {
                    data = new BaselineData(map.getMapName(), dimension(map), bounds, palette, blocks, tiles, entities);
                    writing = CompletableFuture.runAsync(() -> {
                        try { data.write(destination); } catch (Exception e) { throw new CompletionException(e); }
                    });
                }
            }
            if (saving) {
                if (!writing.isDone()) return false;
                writing.join(); return true;
            }
            while (tileIndex < data.blockEntities().size() && System.nanoTime() < deadline) {
                CompoundTag tag = data.blockEntities().m_128728_(tileIndex++);
                BlockPos pos = new BlockPos(tag.m_128451_("x"), tag.m_128451_("y"), tag.m_128451_("z"));
                BlockEntity tile = level.m_7702_(pos);
                if (tile == null) throw new IllegalStateException("基准方块实体无法创建：" + pos);
                Clearable.m_18908_(tile); tile.m_142466_(tag); tile.m_6596_();
                level.m_7260_(pos, level.m_8055_(pos), level.m_8055_(pos), 2);
            }
            if (tileIndex < data.blockEntities().size()) return false;
            if (!entityDelay) { entityDelay = true; return false; }
            cancelPendingExplosions(level);
            spawning = true;
            try {
            for (int i = 0; i < data.entities().size(); i++) {
                CompoundTag tag = (CompoundTag) data.entities().m_128728_(i).m_6426_();
                Entity entity = EntityType.m_20645_(tag, level, e -> { mark(e, map); return e; });
                if (entity == null) throw new IllegalStateException("基准实体创建失败。");
                if (!level.m_8860_(entity) || entity.m_20199_().anyMatch(e -> level.m_8791_(e.m_20148_()) != e))
                    throw new IllegalStateException("基准实体或乘客加入世界失败。");
            }
            } finally { spawning = false; }
            clearTicks(); return true;
        }
        void clearTicks() {
            BoundingBox box = new BoundingBox(bounds.x(), bounds.y(), bounds.z(), bounds.x() + bounds.sx() - 1,
                    bounds.y() + bounds.sy() - 1, bounds.z() + bounds.sz() - 1);
            level.m_183326_().m_193234_(box); level.m_183324_().m_193234_(box);
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || job == null) return;
        Job active = job;
        try {
            if (!active.step()) return;
            active.release(); job = null;
            if (active.saving) message("地图基准保存完成：" + file(active.map));
            else if (!active.resumeGame) {
                MatchLobby.reset(active.map);
                message("地图基准恢复完成，当前未开赛。");
            }
            else {
                resuming = active.map;
                try { if (active.round) active.map.startNewRound(); else active.map.startGame(); }
                finally { resuming = null; }
                message(active.map.isStart ? "地图已恢复，游戏继续。" : "地图已恢复，等待全员重新准备。");
            }
        } catch (Exception e) {
            active.release(); job = null; resuming = null;
            MineStrikeSkins.LOG.error("Map baseline operation failed", e);
            if (active.map.isStart) active.map.resetGame();
            else MatchLobby.reset(active.map);
            message("地图基准操作失败，未开始新回合：" + e.getMessage());
        }
    }
    @SubscribeEvent public static void entityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().f_46443_ || event.getEntity() instanceof Player) return;
        CSGameMap map = MatchLobby.map();
        if (map == null) return;
        Entity entity = event.getEntity();
        if (job != null && !job.saving && !spawning && event.getLevel() == job.level && map.getMapArea().isEntityInArea(entity)) {
            event.setCanceled(true); return;
        }
        CompoundTag data = entity.getPersistentData();
        if (data.m_128461_(OWNER).equals(map.getMapName()) && !data.m_128461_(GENERATION).equals(epoch(map))) {
            event.setCanceled(true); return;
        }
        if (event.getLevel() == map.getServerLevel() && map.getMapArea().isEntityInArea(entity)) mark(entity, map);
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent event) {
        if (job != null && !event.getLevel().f_46443_ && event.isCancelable()) event.setCanceled(true);
    }
    @SubscribeEvent public static void blockBreak(BlockEvent.BreakEvent event) { if (job != null) event.setCanceled(true); }
    @SubscribeEvent public static void blockPlace(BlockEvent.EntityPlaceEvent event) { if (job != null) event.setCanceled(true); }
    @SubscribeEvent public static void explosionStart(ExplosionEvent.Start event) {
        if (job != null && event.getLevel() == job.level) event.setCanceled(true);
    }
    @SubscribeEvent public static void explosion(ExplosionEvent.Detonate event) {
        if (job != null) { event.getAffectedBlocks().clear(); event.getAffectedEntities().clear(); }
    }
    @SubscribeEvent public static void hurt(LivingHurtEvent event) { if (job != null) event.setCanceled(true); }
    @SubscribeEvent public static void stopping(ServerStoppingEvent event) {
        if (job != null) { job.release(); job = null; }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { job = null; resuming = null; generation = null; spawning = false; }
}
