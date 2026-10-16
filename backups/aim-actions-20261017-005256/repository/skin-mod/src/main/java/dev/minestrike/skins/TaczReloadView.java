package dev.minestrike.skins;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.animation.screen.RefitTransform;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.List;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID, value = Dist.CLIENT)
public final class TaczReloadView {
    private record Context(Object player, Object level, int slot, ResourceLocation gun) {}
    private static final ReloadViewState state = new ReloadViewState();
    private static Context pending;
    private static long pendingUntil;
    private static float weight;
    private static Context context() {
        var mc = Minecraft.m_91087_();
        if (mc.f_91074_ == null || mc.f_91073_ == null || !mc.f_91066_.m_92176_().m_90612_()) return null;
        var stack = mc.f_91074_.m_21205_();
        if (!(stack.m_41720_() instanceof IGun gun)) return null;
        return new Context(mc.f_91074_, mc.f_91073_, mc.f_91074_.m_150109_().f_35977_, gun.getGunId(stack));
    }
    public static void start() { pending = context(); pendingUntil = System.nanoTime() + 500_000_000L; }
    public static void cancel() { pending = null; pendingUntil = 0; }
    @SubscribeEvent public static void frame(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Context key = context(); long now = System.nanoTime();
        if (key == null) { cancel(); weight = state.update(null, false, 0, now); return; }
        var player = Minecraft.m_91087_().f_91074_;
        var client = IClientPlayerGunOperator.fromLocalPlayer(player);
        boolean reloading = IGunOperator.fromLivingEntity(player).getSynReloadState().getStateType() != ReloadState.StateType.NOT_RELOADING;
        if (reloading || !key.equals(pending) || now >= pendingUntil) cancel();
        weight = state.update(key, reloading || key.equals(pending),
                client.getClientAimingProgress(event.renderTickTime) * (1 - RefitTransform.getOpeningProgress()), now);
    }
    public static float weight() { return weight; }
    public static List<BedrockPart> anchor(BedrockGunModel model) {
        var player = Minecraft.m_91087_().f_91074_;
        if (player == null || !(player.m_21205_().m_41720_() instanceof IGun gun)) return null;
        var stack = player.m_21205_();
        var scope = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.isEmptyAttachmentId(scope)) scope = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        var path = DefaultAssets.isEmptyAttachmentId(scope) ? model.getIronSightPath() : model.getScopePosPath();
        if (path == null || path.isEmpty()) path = model.getConstraintPath();
        return path;
    }
    private TaczReloadView() {}
}
