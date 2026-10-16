package dev.minestrike.skins;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.event.common.GunFireEvent;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.input.AimKey;
import com.tacz.guns.config.client.KeyConfig;
import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.event.ClientEventHandler;
import com.atsuishio.superbwarfare.event.ClickEventHandler;
import com.atsuishio.superbwarfare.init.ModKeyMappings;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class AimActions {
    private record Context(Object player, Object level, int slot, String gun, boolean tacz) {}
    private static final AimReturnState state = new AimReturnState();
    private static boolean internal;
    private static Context current() {
        var mc = Minecraft.m_91087_(); var player = mc.f_91074_;
        if (player == null || mc.f_91073_ == null || mc.f_91080_ != null || !player.m_6084_() || player.m_5833_()
                || !mc.f_91066_.m_92176_().m_90612_()) return null;
        var stack = player.m_21205_(); String gun; boolean tacz;
        if (stack.m_41720_() instanceof IGun item) { gun = item.getGunId(stack).toString(); tacz = true; }
        else if (stack.m_41720_() instanceof GunItem) {
            gun = String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.m_41720_()));
            var tag = stack.m_41783_();
            if (tag != null && tag.m_128441_("GunData")) gun += java.util.Arrays.toString(tag.m_128469_("GunData").m_128465_("UUID"));
            tacz = false;
        } else return null;
        return new Context(player, mc.f_91073_, player.m_150109_().f_35977_, gun, tacz);
    }
    public static void clear() { state.bind(null); }
    public static boolean request(boolean requested, boolean tacz) {
        if (internal) return requested;
        Context key = current();
        if (key == null || key.tacz() != tacz) return requested;
        state.bind(key); return state.input(requested);
    }
    public static boolean inputView(boolean actual) {
        Context key = current(); if (key == null || !key.tacz()) return actual;
        state.bind(key); return state.inputView(actual);
    }
    private static boolean aimed(Context key) {
        return key.tacz() ? IClientPlayerGunOperator.fromLocalPlayer(Minecraft.m_91087_().f_91074_).isAim() : ClientEventHandler.zoom;
    }
    private static void setAim(Context key, boolean value) {
        if (aimed(key) == value) return;
        internal = true;
        try {
            var player = Minecraft.m_91087_().f_91074_;
            if (key.tacz()) IClientPlayerGunOperator.fromLocalPlayer(player).aim(value);
            else if (value) ClickEventHandler.INSTANCE.handleWeaponZoomPress(player, player.m_21205_());
            else ClickEventHandler.INSTANCE.handleWeaponZoomRelease();
        } finally { internal = false; }
    }
    public static void reload() {
        Context key = current(); if (key == null) return;
        state.reload(key); if (!key.tacz()) ClickEventHandler.switchZoom = false;
        setAim(key, false);
    }
    @SubscribeEvent public static void fired(GunFireEvent event) {
        Context key = current();
        if (key == null || !key.tacz() || !event.getLogicalSide().isClient() || event.getShooter() != Minecraft.m_91087_().f_91074_) return;
        var index = TimelessAPI.getCommonGunIndex(((IGun)event.getGunItemStack().m_41720_()).getGunId(event.getGunItemStack())).orElse(null);
        if (index == null) return;
        double seconds = AimAnimations.cycleSeconds(index, ((IGun)event.getGunItemStack().m_41720_()).getFireMode(event.getGunItemStack()));
        if (seconds <= 0) return;
        state.cycle(key, aimed(key), System.nanoTime(), (long)(seconds * 1_000_000_000L));
    }
    public static void superbShot() {
        Context key = current(); if (key == null || key.tacz()) return;
        var data = GunData.from(Minecraft.m_91087_().f_91074_.m_21205_());
        int ticks = data.get(GunProp.BOLT_ACTION_TIME);
        if (ticks > 0 && data.bolt.needed.get()) state.cycle(key, aimed(key), System.nanoTime(), ticks * 50_000_000L);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST) public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Context key = current();
        if (key == null) { clear(); return; }
        state.bind(key); var player = Minecraft.m_91087_().f_91074_;
        boolean reloading, cycling;
        if (key.tacz()) {
            var client = IClientPlayerGunOperator.fromLocalPlayer(player); var operator = IGunOperator.fromLivingEntity(player);
            reloading = operator.getSynReloadState().getStateType() != ReloadState.StateType.NOT_RELOADING;
            var data = TimelessAPI.getCommonGunIndex(((IGun)player.m_21205_().m_41720_()).getGunId(player.m_21205_())).orElse(null);
            boolean combined = data != null && AimAnimations.cycleSeconds(data, ((IGun)player.m_21205_().m_41720_()).getFireMode(player.m_21205_())) > 0;
            cycling = client.getDataHolder().isBolting || operator.getSynIsBolting() || (combined && TaczReloadView.animation(true, true));
            if (!KeyConfig.HOLD_TO_AIM.get() || !AimKey.AIM_KEY.m_90857_()) state.release();
        } else {
            var data = GunData.from(player.m_21205_());
            reloading = data.reloading() || data.reload.time() > 0;
            cycling = data.bolt.needed.get() || data.bolt.actionTimer.get() > 0;
            if (!ModKeyMappings.HOLD_ZOOM.m_90857_()) state.release();
            if (reloading) ClickEventHandler.switchZoom = false;
        }
        Boolean next = state.update(key, reloading, cycling, System.nanoTime());
        if (next != null) setAim(key, next);
    }
    private AimActions() {}
}
