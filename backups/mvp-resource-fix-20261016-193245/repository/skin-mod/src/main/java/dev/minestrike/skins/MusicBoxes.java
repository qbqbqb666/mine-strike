package dev.minestrike.skins;

import com.google.gson.*;
import com.phasetranscrystal.blockoffensive.sound.MVPMusicManager;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import dev.minestrike.skins.mixin.MvpMusicAccess;
import net.minecraft.resources.ResourceLocation;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class MusicBoxes {
    public static final String KEY = "mvp_music", NONE = "fpsmatch:empty";
    public record Track(String id, String name) {}
    private static volatile List<Track> tracks;
    public static List<Track> parse(JsonObject sounds) {
        List<Track> result = new ArrayList<>();
        for (var entry : sounds.entrySet()) {
            if (!entry.getValue().isJsonObject()) continue;
            JsonObject sound = entry.getValue().getAsJsonObject();
            if (!sound.has("subtitle") || !"music.for.mvp".equals(sound.get("subtitle").getAsString())) continue;
            String id = "blockoffensive:" + entry.getKey();
            if (!SkinCatalog.validId(id) || !sound.has("sounds") || sound.getAsJsonArray("sounds").isEmpty())
                throw new IllegalArgumentException("Invalid MVP sound: " + id);
            result.add(new Track(id, entry.getKey()));
        }
        return List.copyOf(result);
    }
    public static List<Track> tracks() {
        if (tracks == null) {
            try (InputStream in = MusicBoxes.class.getResourceAsStream("/assets/blockoffensive/sounds.json")) {
                if (in == null) throw new IllegalStateException("Missing BlockOffensive sounds.json");
                tracks = parse(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
            } catch (IOException e) { throw new IllegalStateException("Cannot read MVP sound catalog", e); }
        }
        return tracks;
    }
    public static String name(String id) { return tracks().stream().filter(t -> t.id().equals(id)).map(Track::name).findFirst().orElse(null); }
    public static boolean group(SkinCatalog.Group group) { return "MvpMusic".equals(group.tag) && KEY.equals(group.key); }
    public static boolean available(String id) { return NONE.equals(id) || name(id) != null; }
    public static void append(SkinCatalog catalog) {
        catalog.groups.removeIf(g -> KEY.equals(g.key));
        SkinCatalog.Group group = new SkinCatalog.Group();
        group.key = KEY; group.name = "MVP 音乐"; group.item = "minecraft:jukebox"; group.tag = "MvpMusic";
        group.baseIds.add(NONE);
        SkinCatalog.Skin none = new SkinCatalog.Skin(); none.id = NONE; none.name = "不播放"; group.skins.add(none);
        for (Track track : tracks()) {
            SkinCatalog.Skin skin = new SkinCatalog.Skin(); skin.id = track.id(); skin.name = track.name(); group.skins.add(skin);
        }
        catalog.groups.add(group);
    }
    public static boolean update(Map<String,ResourceLocation> map, UUID uuid, String selection) {
        if (selection == null) return false; // Default leaves pre-existing native/online music settings intact.
        if (!available(selection)) throw new IllegalArgumentException("Unknown music box");
        String key = uuid.toString();
        if (NONE.equals(selection)) return map.remove(key) != null;
        ResourceLocation next = new ResourceLocation(selection);
        return !Objects.equals(map.put(key,next),next);
    }
    public static void apply(UUID uuid, Map<String,String> choices) {
        var manager = MVPMusicManager.getInstance();
        Map<String,ResourceLocation> map = ((MvpMusicAccess)(Object)manager).minestrike$music();
        if (update(map,uuid,choices.get(KEY))) MVPMusicManager.write(FPSMCore.getInstance().getFPSMDataManager());
    }
    public static Map<String,String> displayed(UUID uuid, Map<String,String> choices) {
        Map<String,String> result = new LinkedHashMap<>(choices);
        if (!result.containsKey(KEY)) {
            String current = MVPMusicManager.getInstance().getMvpMusic(uuid.toString()).toString();
            if (name(current) != null) result.put(KEY,current);
        }
        return result;
    }
    private MusicBoxes() {}
}
