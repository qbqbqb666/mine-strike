import dev.minestrike.skins.MatchVotes;
import java.util.*;

public class MatchVotesTest {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
    }
    public static void main(String[] args) {
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID();
        MatchVotes votes = new MatchVotes();
        votes.update(Map.of(a, "t", b, ""), false);
        votes.toggleReady(a); votes.toggleReady(b);
        check(!votes.allReady() && votes.readyCount() == 1, "Unselected online player blocks start");
        votes.update(Map.of(a, "t", b, "t"), false);
        votes.toggleReady(b);
        check(votes.allReady(), "Single side with everyone ready can start");
        votes.update(Map.of(a, "t", b, "t", c, "ct"), false);
        check(!votes.allReady(), "New online player blocks start");
        votes.toggleReady(c);
        check(votes.allReady(), "Unequal teams can start");
        votes.update(Map.of(a, "ct", b, "t", c, "ct"), false);
        check(!votes.ready(a) && !votes.allReady(), "Team changes clear readiness");
        votes.update(Map.of(b, "t", c, "ct"), false);
        check(votes.allReady(), "Offline players do not block ready");
        votes.update(Map.of(a, "ct", b, "t", c, "ct"), false);
        check(!votes.ready(a) && !votes.allReady(), "Reconnect requires ready again");
        votes.clear(); votes.update(Map.of(a, "t", b, ""), true);
        votes.toggleEnd(a);
        check(!votes.allEnding(), "Unassigned online player also counts in end vote");
        votes.toggleEnd(b);
        check(votes.allEnding(), "Unanimous online players can end");
        votes.toggleEnd(b); check(!votes.allEnding(), "Withdraw end approval");
        votes.update(Map.of(a, "t"), true);
        check(votes.allEnding(), "Offline player removed from end electorate");
        votes.update(Map.of(a, "t", c, ""), true);
        check(!votes.allEnding(), "New online player must approve ending");
        votes.clear(); votes.update(Map.of(a, "ct"), false); votes.toggleReady(a);
        check(votes.allReady(), "One player can start");
        votes.toggleReady(a); check(!votes.allReady(), "Cancel ready");
        votes.toggleReady(a); votes.forget(a); votes.update(Map.of(a, "ct"), false);
        check(!votes.ready(a), "Immediate reconnect cannot retain ready vote");
        votes.clear(); votes.update(Map.of(), false);
        check(!votes.allReady() && !votes.allEnding(), "Empty server never starts or ends");
        System.out.println("PASS: all online players, unselected, single side, unequal sides, team changes, reconnect, unanimous end and vote withdrawal");
    }
}
