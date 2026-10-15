package dev.minestrike.skins;

import com.mojang.blaze3d.audio.Channel;
import com.tacz.guns.client.sound.GunSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.EXTEfx;
import org.lwjgl.openal.EXTThreadLocalContext;
import org.lwjgl.openal.SOFTSourceSpatialize;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class CombatAudio {
    public record Profile(AudioPolicy.Kind kind, String id, double x, double y, double z) {}
    public interface ManagedChannel { void minestrike$audioProfile(Profile profile); }
    private record SourceKey(long context, int source) {}
    private static final Map<SoundInstance, Profile> profiles = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<SoundInstance, AudioPolicy.Emitter> emitters = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<SourceKey, Profile> sources = new ConcurrentHashMap<>();
    private static final AudioPolicy.TailLimiter<SoundInstance> tails = new AudioPolicy.TailLimiter<>();
    private static final AtomicBoolean unsupported = new AtomicBoolean();

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void play(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (!AudioConfig.ENABLED.get() || sound == null || sound.m_7796_()) return;
        var player = Minecraft.m_91087_().f_91074_;
        if (player == null) return;
        UUID emitter = sound instanceof AudioPolicy.Source source ? source.minestrike$audioEmitter() : null;
        if (AudioPolicy.local(emitter, player.m_20148_(), player.m_20202_() == null ? null : player.m_20202_().m_20148_())) return;
        String id = sound.m_7904_().toString();
        AudioPolicy.Kind kind;
        if (sound instanceof GunSoundInstance gun) {
            if (gun.getRegistryName() == null || !AudioPolicy.taczShot(gun.getRegistryName().toString())) return;
            id = gun.getRegistryName().toString();
            kind = AudioPolicy.Kind.GUN;
        } else {
            kind = AudioPolicy.kind(id);
            if (kind == AudioPolicy.Kind.STEP && sound.m_8070_() != SoundSource.PLAYERS) return;
        }
        if (kind == AudioPolicy.Kind.OTHER) return;
        Profile profile = new Profile(kind, id, sound.m_7772_(), sound.m_7780_(), sound.m_7778_());
        profiles.put(sound, profile);
        if (AudioConfig.DEBUG.get())
            MineStrikeSkins.LOG.info("[audio] request {} kind={} emitter={} xyz={},{},{} distance={}", id, kind,
                    emitter, profile.x(), profile.y(), profile.z(),
                    Math.sqrt(player.m_20275_(profile.x(), profile.y(), profile.z())));
        if (kind == AudioPolicy.Kind.GUN && emitter != null && !sound.m_7775_() && AudioConfig.TAILS.get()) {
            emitters.put(sound, new AudioPolicy.Emitter(emitter, id));
        }
    }

    public static void bind(SoundInstance sound, Channel channel) {
        ((ManagedChannel)channel).minestrike$audioProfile(profiles.get(sound));
        AudioPolicy.Emitter emitter = emitters.get(sound);
        if (emitter != null) Minecraft.m_91087_().execute(() -> {
            var manager = Minecraft.m_91087_().m_91106_();
            if (AudioConfig.ENABLED.get() && AudioConfig.TAILS.get() && manager.m_120403_(sound))
                for (SoundInstance old : tails.add(emitter, sound, System.nanoTime(), AudioConfig.MAX_TAILS.get()))
                    manager.m_120399_(old);
        });
    }

    public static void register(int source, Profile profile) {
        SourceKey key = new SourceKey(currentContext(), source);
        if (profile == null) sources.remove(key); else sources.put(key, profile);
    }

    public static boolean environment(int source, double x, double y, double z) {
        Profile p = sources.get(new SourceKey(currentContext(), source));
        if (p == null || !AudioConfig.ENABLED.get()) return false;
        source(new Profile(p.kind(), p.id(), x, y, z), source, false);
        return AudioConfig.POSITION.get();
    }

    public static long currentContext() {
        long thread = ALC.getCapabilities().ALC_EXT_thread_local_context ? EXTThreadLocalContext.alcGetThreadContext() : 0;
        return thread != 0 ? thread : ALC10.alcGetCurrentContext();
    }

    public static float gain(SoundInstance sound, float original) {
        if (!AudioConfig.ENABLED.get()) return original;
        Profile p = profiles.get(sound);
        if (p == null) return original;
        double multiplier = switch (p.kind()) {
            case STEP -> AudioConfig.STEP_VOLUME.get();
            case GUN -> AudioConfig.GUN_VOLUME.get();
            case VEHICLE -> AudioConfig.VEHICLE_VOLUME.get();
            default -> 1;
        };
        return AudioPolicy.mixGain(original, multiplier);
    }

    // Called on the sound thread immediately before alSourcePlay, after Sound Physics setup.
    public static boolean source(Profile p, int source, boolean previouslySpatialized) {
        boolean supported = AL.getCapabilities().AL_SOFT_source_spatialize;
        boolean spatial = p != null && AudioConfig.ENABLED.get() && AudioConfig.SPATIALIZE.get();
        if (supported && (spatial || previouslySpatialized))
            AL10.alSourcei(source, SOFTSourceSpatialize.AL_SOURCE_SPATIALIZE_SOFT,
                    spatial ? AL10.AL_TRUE : SOFTSourceSpatialize.AL_AUTO_SOFT);
        else if (spatial && unsupported.compareAndSet(false, true))
            MineStrikeSkins.LOG.warn("[audio] AL_SOFT_source_spatialize unavailable; stereo world sources retain native behavior");
        if (p != null && AudioConfig.ENABLED.get()) {
            if (AudioConfig.POSITION.get()) AL10.alSource3f(source, AL10.AL_POSITION, (float)p.x(), (float)p.y(), (float)p.z());
            if (p.kind() == AudioPolicy.Kind.STEP && AudioConfig.ATTENUATION.get()) {
                float range = Math.max(AL10.alGetSourcef(source, AL10.AL_MAX_DISTANCE), AudioConfig.STEP_RANGE.get().floatValue());
                AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, range);
                AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, 0);
            }
            if (p.kind() != AudioPolicy.Kind.VEHICLE && AudioConfig.DRY.get() && ALC.getCapabilities().ALC_EXT_EFX) {
                long device = ALC10.alcGetContextsDevice(currentContext());
                int sends = ALC10.alcGetInteger(device, EXTEfx.ALC_MAX_AUXILIARY_SENDS);
                for (int send = 0; send < sends; send++)
                    AL11.alSource3i(source, EXTEfx.AL_AUXILIARY_SEND_FILTER, EXTEfx.AL_EFFECTSLOT_NULL, send, EXTEfx.AL_FILTER_NULL);
            }
            if (AudioConfig.DEBUG.get()) MineStrikeSkins.LOG.info("[audio] source={} id={} spatialized={} maxDistance={}",
                    source, p.id(), supported && spatial, AL10.alGetSourcef(source, AL10.AL_MAX_DISTANCE));
        }
        return supported && spatial;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) tails.prune(Minecraft.m_91087_().m_91106_()::m_120403_);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { profiles.clear(); emitters.clear(); tails.clear(); sources.clear(); }
    private CombatAudio() {}
}
