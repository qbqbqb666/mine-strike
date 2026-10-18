package dev.minestrike.skins;

import com.atsuishio.superbwarfare.data.vehicle.VehicleData;
import com.atsuishio.superbwarfare.entity.vehicle.base.VehicleEntity;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MineStrikeSkins.ID)
public final class LavAdHealth {
    public static float maximum(String entityId) {
        return switch (entityId) {
            case "superbwarfare:lav_ad" -> 150f;
            case "superbwarfare:ah_6" -> 1000f;
            default -> 0f;
        };
    }

    public static float health(float beforeMax, float beforeHealth, float maximum) {
        float scaled = beforeMax > 0 ? beforeHealth / beforeMax * maximum : beforeHealth;
        return Math.min(maximum, Math.max(0, scaled));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().f_46443_ || !(event.getEntity() instanceof VehicleEntity vehicle)) return;
        float maximum = maximum(EntityType.m_20613_(vehicle.m_6095_()).toString());
        if (maximum == 0) return;
        float beforeMax = vehicle.getMaxHealth(), beforeHealth = vehicle.getHealth();
        String original = vehicle.getOverride();
        JsonObject override = original.isBlank() ? new JsonObject() : JsonParser.parseString(original).getAsJsonObject();
        override.addProperty("MaxHealth", maximum);
        vehicle.setOverride(override.toString());
        VehicleData.from(vehicle).update();
        vehicle.setHealth(health(beforeMax, beforeHealth, maximum));
    }
}
