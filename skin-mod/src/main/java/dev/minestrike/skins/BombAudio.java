package dev.minestrike.skins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public final class BombAudio {
    public static final String PLANTED = "blockoffensive:planted", BEEP = "blockoffensive:beep";
    public static final String LOUD_PLANTED = "minestrike_skins:c4_planted_loud", LOUD_BEEP = "minestrike_skins:c4_beep_loud";
    public static final float BEEP_RANGE = 96;

    @Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
    public static final class Client {
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void play(PlaySoundEvent event) {
            SoundInstance original = event.getSound();
            if (original == null) return;
            String id = original.m_7904_().toString();
            if (PLANTED.equals(id)) {
                event.setSound(new SimpleSoundInstance(new ResourceLocation(LOUD_PLANTED), SoundSource.MASTER,
                        1, 1, SoundInstance.m_235150_(), false, 0, SoundInstance.Attenuation.NONE, 0, 0, 0, true));
            } else if (BEEP.equals(id) && !original.m_7796_()) {
                if (original.m_6775_(Minecraft.m_91087_().m_91106_()) == null) return;
                event.setSound(new SimpleSoundInstance(new ResourceLocation(LOUD_BEEP), original.m_8070_(),
                        1, original.m_7783_(), SoundInstance.m_235150_(), false, 0, SoundInstance.Attenuation.LINEAR,
                        original.m_7772_(), original.m_7780_(), original.m_7778_(), false));
            }
        }
    }

    private BombAudio() {}
}
