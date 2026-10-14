package dev.minestrike.skins;

import com.google.gson.reflect.TypeToken;
import icyllis.modernui.mc.forge.MuiForgeApi;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import java.util.Map;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class SkinClient {
    public static final KeyMapping OPEN = new KeyMapping("key.minestrike_skins.open", GLFW.GLFW_KEY_F6, "key.categories.minestrike_skins");

    @Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent public static void keys(RegisterKeyMappingsEvent event) { event.register(OPEN); }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && Minecraft.m_91087_().f_91074_ != null && OPEN.m_90859_()) {
            SkinNetwork.CHANNEL.sendToServer(new SkinNetwork.Request(false, "{}"));
        }
    }

    public static void accept(SkinNetwork.State state) {
        SkinCatalog catalog = SkinCatalog.JSON.fromJson(state.catalog(), SkinCatalog.class);
        catalog.validate();
        Map<String, String> choices = SkinCatalog.JSON.fromJson(state.choices(), new TypeToken<Map<String, String>>() {}.getType());
        if (state.open()) {
            MuiForgeApi.openScreen(new SkinSelectionFragment(catalog, choices, state.locked(), state.message()));
        } else if (Minecraft.m_91087_().f_91080_ instanceof icyllis.modernui.mc.MuiScreen screen
                && screen.getFragment() instanceof SkinSelectionFragment fragment) {
            MuiForgeApi.postToUiThread(() -> fragment.receive(choices, state.locked(), state.message()));
        }
    }
}
