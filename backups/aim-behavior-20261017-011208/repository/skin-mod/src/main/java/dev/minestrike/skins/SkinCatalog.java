package dev.minestrike.skins;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.*;

public final class SkinCatalog {
    public static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    public List<Group> groups = new ArrayList<>();

    public static final class Group {
        public String key;
        public String name;
        public String item;
        public String tag;
        public List<String> baseIds = new ArrayList<>();
        public List<Skin> skins = new ArrayList<>();
    }

    public static final class Skin {
        public String name;
        public String id;
    }

    public void validate() {
        if (groups == null || groups.isEmpty() || groups.size() > 32) throw new IllegalArgumentException("Invalid skin groups");
        Set<String> keys = new HashSet<>();
        for (Group g : groups) {
            if (g.key == null || !keys.add(g.key) || g.name == null || g.name.length() > 100
                    || !validId(g.item) || !("GunId".equals(g.tag) || "MeleeWeaponId".equals(g.tag) || "AgentTeam".equals(g.tag) || "MvpMusic".equals(g.tag))
                    || g.baseIds == null || g.baseIds.isEmpty() || g.skins == null || g.skins.size() > 128) {
                throw new IllegalArgumentException("Invalid skin group: " + g.key);
            }
            for (String id : g.baseIds) if (!validId(id)) throw new IllegalArgumentException("Invalid base ID: " + id);
            Set<String> ids = new HashSet<>();
            for (Skin skin : g.skins) {
                if (skin == null || skin.name == null || skin.name.length() > 100 || !validId(skin.id) || !ids.add(skin.id)) {
                    throw new IllegalArgumentException("Invalid skin in: " + g.key);
                }
                if (AgentSkins.group(g)) {
                    var agent = AgentSkins.find(skin.id);
                    if (agent == null || !g.key.equals("agent_" + agent.team()))
                        throw new IllegalArgumentException("Invalid agent team: " + skin.id);
                }
            }
        }
    }

    public static boolean validId(String id) {
        return id != null && id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+");
    }

    public Map<String, String> checked(Map<String, String> choices) {
        Map<String, String> result = new LinkedHashMap<>();
        for (Group g : groups) {
            String id = choices == null ? null : choices.get(g.key);
            if (id != null && g.skins.stream().anyMatch(s -> s.id.equals(id))) result.put(g.key, id);
        }
        return result;
    }

    public Group groupFor(String tag, String id) {
        for (Group g : groups) {
            if (g.tag.equals(tag) && (g.baseIds.contains(id) || g.skins.stream().anyMatch(s -> s.id.equals(id)))) return g;
        }
        return null;
    }
}
