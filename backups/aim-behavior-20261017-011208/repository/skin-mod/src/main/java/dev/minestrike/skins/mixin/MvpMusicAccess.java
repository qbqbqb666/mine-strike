package dev.minestrike.skins.mixin;

import com.phasetranscrystal.blockoffensive.sound.MVPMusicManager;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(value = MVPMusicManager.class, remap = false)
public interface MvpMusicAccess {
    @Accessor("mvpMusicMap") Map<String,ResourceLocation> minestrike$music();
}
