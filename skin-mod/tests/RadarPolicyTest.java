import dev.minestrike.skins.RadarPolicy;
import java.util.*;

public class RadarPolicyTest {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) {
        UUID self = UUID.randomUUID(), teammate = UUID.randomUUID(), enemy = UUID.randomUUID();
        Set<UUID> roster = new HashSet<>(List.of(teammate));
        RadarPolicy state = new RadarPolicy(true, roster);
        check(state.visible(self, self), "Self is visible even if not included in the roster");
        check(state.visible(self, teammate), "Teammate is visible");
        check(!state.visible(self, enemy), "Enemy is hidden regardless of friend/track categories");
        check(!state.visible(self, null), "Unknown tracked identities are hidden during matches");
        roster.add(enemy);
        check(!state.visible(self, enemy), "Received roster cannot mutate after installation");
        // The observer identity is always the local player, even when the camera follows an enemy.
        check(!state.visible(self, enemy) && state.visible(self, teammate), "Death/spectating does not change roster ownership");
        RadarPolicy switched = new RadarPolicy(true, Set.of(enemy));
        check(switched.visible(self, enemy) && !switched.visible(self, teammate), "Team switch replaces the allow-list");
        RadarPolicy unselected = new RadarPolicy(true, Set.of());
        check(unselected.visible(self, self) && !unselected.visible(self, teammate) && !unselected.visible(self, enemy), "Unselected player sees no other players");
        RadarPolicy outside = new RadarPolicy(false, Set.of());
        check(outside.visible(self, enemy) && outside.visible(self, teammate), "Outside a match preserves original radar choices");
        RadarPolicy reconnect = new RadarPolicy(true, Set.of());
        check(!reconnect.visible(self, teammate), "Reconnect hides other players until fresh server state arrives");
        System.out.println("PASS: teammate-only UUID policy, enemy/unknown tracking, spectator ownership, switching, unselected, outside-match and reconnect defaults");
    }
}
