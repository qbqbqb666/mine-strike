import dev.minestrike.skins.AudioPolicy;
import java.util.*;

public class AudioPolicyTest {
    private static void close(float actual, double expected) {
        if (Math.abs(actual - expected) > 0.00001) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        assert AudioPolicy.kind("minecraft:block.stone.step") == AudioPolicy.Kind.STEP;
        assert AudioPolicy.kind("minecraft:block.stone.break") == AudioPolicy.Kind.OTHER;
        assert AudioPolicy.kind("voicechat:player_voice") == AudioPolicy.Kind.OTHER;
        assert AudioPolicy.kind("superbwarfare:rpg_fire_1p") == AudioPolicy.Kind.OTHER;
        assert AudioPolicy.kind("superbwarfare:rpg_reload") == AudioPolicy.Kind.OTHER;
        assert AudioPolicy.kind("superbwarfare:rpg_fire_3p") == AudioPolicy.Kind.GUN;
        assert AudioPolicy.kind("superbwarfare:rpg_far") == AudioPolicy.Kind.GUN;
        assert AudioPolicy.kind("superbwarfare:lav_ad_fire") == AudioPolicy.Kind.GUN;
        assert AudioPolicy.kind("superbwarfare:lav_engine") == AudioPolicy.Kind.VEHICLE;
        assert AudioPolicy.taczShot("mcs2:ak47/ak47_shoot_3p");
        assert AudioPolicy.taczShot("mcs2:m4/m4_silence_3p");
        assert !AudioPolicy.taczShot("mcs2:ak47/ak47_reload_empty");
        close(AudioPolicy.shotGain(1, 0, 64, 1), 1);
        close(AudioPolicy.shotGain(1, 32, 64, 1), .5);
        close(AudioPolicy.shotGain(1, 48, 64, 1), .25);
        close(AudioPolicy.shotGain(1, 64, 64, 1), 0);
        close(AudioPolicy.shotGain(1, 65, 64, 1), 0);
        close(AudioPolicy.shotGain(1, 32, 16, 1), 0);
        close(AudioPolicy.shotGain(.4f, 0, 64, 1), .4);
        float previous = 1;
        for (int distance = 0; distance <= 65; distance++) {
            float gain = AudioPolicy.shotGain(1, distance, 64, 1);
            assert gain >= 0 && gain <= previous;
            previous = gain;
        }
        close(AudioPolicy.mixGain(0, 2), 0);
        close(AudioPolicy.mixGain(.4f, .5), .2);
        close(AudioPolicy.mixGain(.8f, 2), 1);
        var limiter = new AudioPolicy.TailLimiter<String>();
        UUID one = UUID.randomUUID(), two = UUID.randomUUID();
        assert AudioPolicy.local(one, one, null);
        assert AudioPolicy.local(two, one, two);
        assert !AudioPolicy.local(two, one, null);
        assert !AudioPolicy.local(null, one, null);
        var a = new AudioPolicy.Emitter(one, "ak47");
        var b = new AudioPolicy.Emitter(two, "ak47");
        assert limiter.add(a, "a1", 0, 2).isEmpty();
        assert limiter.add(a, "a2", 50_000_000, 2).isEmpty();
        assert limiter.add(b, "b1", 200_000_000, 2).isEmpty();
        assert limiter.add(a, "a3", 200_000_000, 2).equals(List.of("a1"));
        assert limiter.add(new AudioPolicy.Emitter(one, "awp"), "awp1", 300_000_000, 2).isEmpty();
        limiter.clear();
        assert limiter.add(a, "burst1", 0, 1).isEmpty();
        assert limiter.add(a, "burst2", 1_000_000, 1).isEmpty() : "Never cut a new attack to enforce the cap";
        limiter.prune(sound -> false);
        assert limiter.add(a, "after-reload", 400_000_000, 1).isEmpty();
        System.out.println("PASS: audio selection, distance curves, mute preservation, per-shooter tails and lifecycle cleanup");
    }
}
