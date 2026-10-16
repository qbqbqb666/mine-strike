package dev.minestrike.skins;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

public final class AgentSkins {
    public record Agent(String id, String name, String team) {}
    public static final List<Agent> ALL = List.of(
            new Agent("ct1_sas", "SAS 中队军官", "ct"), new Agent("ct3_ava", "FBI 艾娃", "ct"),
            new Agent("ct4_ksk", "KSK 第三突击队", "ct"), new Agent("ct6_ricksaw", "海豹 瑞克索", "ct"),
            new Agent("ct8_chemhaz", "SWAT 生化专家", "ct"), new Agent("t1_phoenix", "凤凰战士", "t"),
            new Agent("t3_muhlik", "精英 穆赫里克", "t"), new Agent("t4_osiris", "精英 奥西里斯", "t"),
            new Agent("t5_romanov", "军刀 罗曼诺夫", "t"), new Agent("t7_numberk", "专业人士 K", "t"));
    public static boolean group(SkinCatalog.Group group) { return "AgentTeam".equals(group.tag); }
    public static String id(Agent agent) { return MineStrikeSkins.ID + ":" + agent.id(); }
    public static Agent find(String id) { return ALL.stream().filter(a -> id(a).equals(id)).findFirst().orElse(null); }
    public static String chosen(String team, Map<String,String> choices) {
        Agent selected = find(choices.get("agent_" + team));
        return selected != null && selected.team().equals(team) ? id(selected)
                : ALL.stream().filter(a -> a.team().equals(team)).map(AgentSkins::id).findFirst().orElse("");
    }
    public static ResourceLocation texture(String id) {
        Agent agent = find(id);
        return agent == null ? null : new ResourceLocation(MineStrikeSkins.ID, "textures/entity/agents/" + agent.id() + ".png");
    }
    public static ResourceLocation portrait(String id) {
        Agent agent = find(id);
        return agent == null ? null : new ResourceLocation(MineStrikeSkins.ID, "textures/gui/agents/" + agent.id() + ".png");
    }
    public static void append(SkinCatalog catalog) {
        for (String team : List.of("t", "ct")) {
            String key = "agent_" + team;
            catalog.groups.removeIf(g -> key.equals(g.key));
            SkinCatalog.Group g = new SkinCatalog.Group();
            g.key = key; g.name = team.toUpperCase(Locale.ROOT) + " 探员";
            g.item = "minecraft:player_head"; g.tag = "AgentTeam";
            for (Agent a : ALL) if (a.team().equals(team)) {
                SkinCatalog.Skin skin = new SkinCatalog.Skin(); skin.id = id(a); skin.name = a.name(); g.skins.add(skin);
            }
            g.baseIds.add(g.skins.get(0).id); catalog.groups.add(g);
        }
    }
    private AgentSkins() {}
}
