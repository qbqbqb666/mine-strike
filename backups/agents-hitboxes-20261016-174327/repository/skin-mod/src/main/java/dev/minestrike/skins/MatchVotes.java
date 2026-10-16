package dev.minestrike.skins;

import java.util.*;

public final class MatchVotes {
    private final Set<UUID> ready = new HashSet<>();
    private final Set<UUID> ending = new HashSet<>();
    private final Map<UUID, String> teams = new HashMap<>();
    public void update(Map<UUID, String> online, boolean started) {
        ready.retainAll(online.keySet());
        ending.retainAll(online.keySet());
        if (!started) online.forEach((id, team) -> {
            if (!Objects.equals(teams.get(id), team)) ready.remove(id);
        });
        teams.clear(); teams.putAll(online);
    }
    public void toggleReady(UUID id) {
        if (!isTeam(teams.get(id))) return;
        if (!ready.remove(id)) ready.add(id);
    }
    public void toggleEnd(UUID id) {
        if (!teams.containsKey(id)) return;
        if (!ending.remove(id)) ending.add(id);
    }
    public boolean allReady() {
        return !teams.isEmpty() && teams.values().stream().allMatch(MatchVotes::isTeam)
                && ready.containsAll(teams.keySet());
    }
    public boolean allEnding() { return !teams.isEmpty() && ending.containsAll(teams.keySet()); }
    public boolean ready(UUID id) { return ready.contains(id); }
    public boolean ending(UUID id) { return ending.contains(id); }
    public int readyCount() { return ready.size(); }
    public int endCount() { return ending.size(); }
    public void forget(UUID id) { ready.remove(id); ending.remove(id); teams.remove(id); }
    public void clear() { ready.clear(); ending.clear(); teams.clear(); }
    private static boolean isTeam(String team) { return "t".equals(team) || "ct".equals(team); }
}
