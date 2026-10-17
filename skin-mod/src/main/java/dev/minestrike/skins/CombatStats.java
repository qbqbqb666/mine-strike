package dev.minestrike.skins;

import com.phasetranscrystal.blockoffensive.map.CSGameMap;
import com.phasetranscrystal.fpsmatch.core.FPSMCore;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.item.ArmorItem;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.Path;
import java.util.*;

@Mod.EventBusSubscriber(modid=MineStrikeSkins.ID)
public final class CombatStats {
    private static final Map<CSGameMap,CombatSettings> rounds=new IdentityHashMap<>();
    private static final Map<ServerPlayer,CombatSettings> applied=new WeakHashMap<>();
    private static final UUID HEALTH=UUID.fromString("175e99fc-30e6-4931-8bb5-3f5d56580e48"), ARMOR=UUID.fromString("c143b73d-ea23-4ff4-8fab-150754191d99");
    public static final EquipmentSlot[] SLOTS={EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET};
    public static boolean authorized(ServerPlayer p) { return p.m_7500_() && p.m_20310_(2); }
    public static List<CSGameMap> maps() { return FPSMCore.initialized()?FPSMCore.getInstance().getMapByClass(CSGameMap.class):List.of(); }
    public static String key(CSGameMap m) { return m.getServerLevel().m_46472_().m_135782_()+" / "+m.getMapName(); }
    public static Path file(CSGameMap m) {
        return CombatSettings.file(m.getServerLevel().m_7654_().m_129843_(LevelResource.f_78182_),m.getServerLevel().m_46472_().m_135782_().toString(),m.getMapName());
    }
    public static CombatSettings settings(ServerPlayer p) {
        return AgentPlayers.active(p)?rounds.get(MineStrikeSkins.map(p)):null;
    }
    public static void round(CSGameMap m) {
        if(!m.isStart) { end(m); return; }
        try {
            CombatSettings value=CombatSettings.load(file(m));
            if(value==null) rounds.remove(m); else rounds.put(m,value);
        } catch(Exception e) { MineStrikeSkins.LOG.error("Cannot load combat settings for {}",key(m),e); rounds.remove(m); }
        for(ServerPlayer p:m.getServerLevel().m_7654_().m_6846_().m_11314_()) if(MineStrikeSkins.map(p)==m) {
            update(p); if(settings(p)!=null && p.m_6084_()) p.m_21153_(p.m_21233_());
        }
    }
    public static void end(CSGameMap m) {
        rounds.remove(m);
        for(ServerPlayer p:new ArrayList<>(applied.keySet())) if(MineStrikeSkins.map(p)==m) update(p);
    }
    // Evaluate a temporary addition through the native multiply-base and multiply-total pipeline.
    public static double additionScale(AttributeInstance a, UUID own) {
        double base=1,total=1;
        for(var mod:a.m_22122_()) if(!mod.m_22209_().equals(own)) {
            if(mod.m_22217_()==AttributeModifier.Operation.MULTIPLY_BASE) base+=mod.m_22218_();
            if(mod.m_22217_()==AttributeModifier.Operation.MULTIPLY_TOTAL) total*=1+mod.m_22218_();
        }
        return base*total;
    }
    private static void modifier(AttributeInstance a, UUID id, double amount) {
        if(a==null) return;
        var old=a.m_22111_(id);
        if(old!=null && Math.abs(old.m_22218_()-amount)<1e-7) return;
        a.m_22120_(id);
        if(Math.abs(amount)>1e-7) a.m_22118_(new AttributeModifier(id,"Mine Strike combat settings",amount,AttributeModifier.Operation.ADDITION));
    }
    public static double armorTarget(AttributeInstance original, CombatSettings s, List<Collection<AttributeModifier>> equipped) {
        var copy=new AttributeInstance(original.m_22099_(),a->{});
        copy.m_22100_(original.m_22115_());
        for(var mod:original.m_22122_()) if(!mod.m_22209_().equals(ARMOR)) copy.m_22118_(mod);
        for(int i=0;i<4;i++) if(s.armor().get(i)!=null && equipped.get(i)!=null) {
            for(var mod:equipped.get(i)) copy.m_22120_(mod.m_22209_());
            copy.m_22118_(new AttributeModifier(UUID.nameUUIDFromBytes(("ms-slot-"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8)),"Configured armor slot",s.armor().get(i),AttributeModifier.Operation.ADDITION));
        }
        return raw(copy,null);
    }
    public static double raw(AttributeInstance a, UUID exclude) {
        double value=a.m_22115_();
        for(var m:a.m_22104_(AttributeModifier.Operation.ADDITION)) if(!m.m_22209_().equals(exclude)) value+=m.m_22218_();
        return value*additionScale(a,exclude);
    }
    public static void update(ServerPlayer p) {
        CombatSettings s=settings(p), previous=applied.get(p);
        var health=p.m_21051_(Attributes.f_22276_); var armor=p.m_21051_(Attributes.f_22284_);
        if(s==null) {
            if(previous!=null || health.m_22111_(HEALTH)!=null || armor.m_22111_(ARMOR)!=null) {
                modifier(health,HEALTH,0); modifier(armor,ARMOR,0);
                if(p.m_21223_()>p.m_21233_()) p.m_21153_(p.m_21233_());
            }
            applied.remove(p); return;
        }
        float oldMax=p.m_21233_(), oldHealth=p.m_21223_();
        double scaleHealth=additionScale(health,HEALTH);
        if(scaleHealth>0) modifier(health,HEALTH,(s.health()-raw(health,HEALTH))/scaleHealth);
        if(!s.equals(previous) && oldMax>0) p.m_21153_(p.m_21233_()*oldHealth/oldMax);
        else if(oldHealth>p.m_21233_()) p.m_21153_(p.m_21233_());
        applied.put(p,s);
        List<Collection<AttributeModifier>> equipment=new ArrayList<>();
        for(var slot:SLOTS) {
            var stack=p.m_6844_(slot);
            equipment.add(!stack.m_41619_() && stack.m_41720_() instanceof ArmorItem item && item.m_40402_()==slot
                    ?stack.m_41638_(slot).get(Attributes.f_22284_):null);
        }
        double target=armorTarget(armor,s,equipment);
        double scale=additionScale(armor,ARMOR);
        // Remove our prior contribution before comparing; avoids compounding corrections.
        double nativeValue=raw(armor,ARMOR);
        if(scale>0) modifier(armor,ARMOR,(target-nativeValue)/scale);
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.player instanceof ServerPlayer p) update(p);
    }
    @SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
    public static void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) update(p);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if(e.getEntity() instanceof ServerPlayer p) { modifier(p.m_21051_(Attributes.f_22276_),HEALTH,0); modifier(p.m_21051_(Attributes.f_22284_),ARMOR,0); applied.remove(p); }
    }
    @SubscribeEvent public static void stop(ServerStoppedEvent e) { rounds.clear(); applied.clear(); }
    private CombatStats() {}
}
