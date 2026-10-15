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
    private static final float MAX_HEALTH = 150f;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void join(EntityJoinLevelEvent event) {
        if (event.getLevel().f_46443_ || !(event.getEntity() instanceof VehicleEntity vehicle)
                || !EntityType.m_20613_(vehicle.m_6095_()).toString().equals("superbwarfare:lav_ad")) return;
        float beforeMax = vehicle.getMaxHealth(), beforeHealth = vehicle.getHealth();
        String original = vehicle.getOverride();
        JsonObject override = original.isBlank() ? new JsonObject() : JsonParser.parseString(original).getAsJsonObject();
        override.addProperty("MaxHealth", MAX_HEALTH);
        vehicle.setOverride(override.toString());
        VehicleData.from(vehicle).update();
        float health = beforeMax > 0 ? beforeHealth / beforeMax * MAX_HEALTH : beforeHealth;
        vehicle.setHealth(Math.min(MAX_HEALTH, Math.max(0, health)));
    }
}
