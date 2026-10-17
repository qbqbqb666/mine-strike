package dev.minestrike.skins;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class FallDamage {
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer && event.getSource().m_276093_(DamageTypes.f_268671_))
            event.setAmount(event.getAmount() * 0.25f);
    }
}
