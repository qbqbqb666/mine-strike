import dev.minestrike.skins.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.nio.file.*;
import java.util.*;

public class CombatSettingsTest {
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message){check(Math.abs(actual-expected)<1e-6,message+": "+actual+" != "+expected);}
    public static void main(String[] args)throws Exception {
        if(args.length>0 && args[0].equals("--compat")){compat();return;}
        var defaults=CombatSettings.defaults();
        check(defaults.health()==20 && defaults.armor().stream().allMatch(Objects::isNull),"Initial health and inherited armor");
        for(int region:new int[]{0,1,2,3})near(defaults.multiplier(region),region==1?4:1,"Default independent four-region mapping");
        var s=new CombatSettings(40,Arrays.asList(null,8.0,0.0,2.0),List.of(3.2,1.3,.8,.5));
        near(s.multiplier(1),3.2,"head");near(s.multiplier(0),1.3,"torso");near(s.multiplier(3),.8,"arms");near(s.multiplier(2),.5,"legs");
        for(String json:List.of("{\"health\":0,\"armor\":[null,null,null,null],\"parts\":[4,1,1,1]}",
                "{\"health\":20,\"armor\":[-1,null,null,null],\"parts\":[4,1,1,1]}",
                "{\"health\":20,\"armor\":[null,null,null,null],\"parts\":[4,1.23,1,1]}",
                "{\"health\":20,\"armor\":[null],\"parts\":[4,1,1,1]}")) {
            boolean rejected=false;try{CombatSettings.JSON.fromJson(json,CombatSettings.class);}catch(RuntimeException e){rejected=true;}check(rejected,"Reject invalid server-side JSON: "+json);
        }
        for(double n:new double[]{Double.NaN,Double.POSITIVE_INFINITY,-1,1025}){
            boolean rejected=false;try{new CombatSettings(n,defaults.armor(),defaults.parts());}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Reject invalid health");
        }
        Path world=Files.createTempDirectory("ms-combat-settings-");
        try {
            Path dust=CombatSettings.file(world,"minecraft:overworld","dust2"),other=CombatSettings.file(world,"minecraft:overworld","other"),dimension=CombatSettings.file(world,"minecraft:the_nether","dust2");
            check(CombatSettings.load(dust)==null,"Unsaved map remains native, no implicit config creation");
            s.save(dust);check(s.equals(CombatSettings.load(dust)),"Restart-safe persistence including inherited armor");
            defaults.save(other);check(!dust.equals(dimension)&&!dust.equals(other),"Map and dimension isolation");
            check(s.equals(CombatSettings.load(dust)),"Editing another map does not overwrite first");
            defaults.save(dust);check(defaults.equals(CombatSettings.load(dust)),"Atomic overwrite");
            check(CombatSettings.file(world,"minecraft:overworld","../../outside").startsWith(world),"Map names cannot escape data directory");
        } finally {try(var paths=Files.walk(world)){for(var path:paths.sorted(Comparator.reverseOrder()).toList())Files.delete(path);}}
        var model=PlayerVolumes.model(Vec3.f_82478_,0,0,0,false,false,0,0,false);
        int[] regions={1,0,3,2};double[] xs={0,0,.30,.08},ys={1.56,1.3,1.3,.3};
        for(int i=0;i<4;i++)check(PlayerVolumes.clip(model,new Vec3(xs[i],ys[i],3),new Vec3(xs[i],ys[i],-3)).region()==regions[i],"Actual shape distinguishes requested body part "+i);
        attributes();
        var state=new CombatNetwork.State(List.of("minecraft:overworld / dust2"),"minecraft:overworld / dust2",s,true,true,false,"已保存");
        check(state.equals(CombatSettings.JSON.fromJson(CombatSettings.JSON.toJson(state),CombatNetwork.State.class)),"Editor response round trip, null armor, Chinese map and feedback");
        System.out.println("PASS: combat data validation, four independent multipliers, six-volume region rays, inherited/zero armor, map/dimension persistence and response round trip");
    }
    private static void attributes() throws Exception {
        var armor=new AttributeInstance(new RangedAttribute("test.armor",0,0,30),a->{});
        List<Collection<AttributeModifier>> gear=new ArrayList<>();
        int[] values={3,7,2,1};
        for(int n:values){var mod=new AttributeModifier(UUID.randomUUID(),"Equipped armor",n,AttributeModifier.Operation.ADDITION);armor.m_22118_(mod);gear.add(List.of(mod));}
        near(armor.m_22135_(),13,"Original armor baseline");
        near(CombatStats.armorTarget(armor,CombatSettings.defaults(),gear),13,"All inherited slots preserve original armor");
        var s=new CombatSettings(20,Arrays.asList(0.0,8.0,null,null),CombatSettings.defaults().parts());
        near(CombatStats.armorTarget(armor,s,gear),11,"Only selected slots replace their contribution, zero supported");
        gear.set(0,null);armor.m_22120_(armor.m_22122_().stream().filter(m->m.m_22218_()==3).findFirst().orElseThrow().m_22209_());
        near(CombatStats.armorTarget(armor,s,gear),11,"Unequipped slot cannot supply configured armor");
        var extra=new AttributeModifier(UUID.randomUUID(),"Other mod",2,AttributeModifier.Operation.ADDITION);armor.m_22118_(extra);
        armor.m_22118_(new AttributeModifier(UUID.randomUUID(),"Other base",.5,AttributeModifier.Operation.MULTIPLY_BASE));
        armor.m_22118_(new AttributeModifier(UUID.randomUUID(),"Other total",.2,AttributeModifier.Operation.MULTIPLY_TOTAL));
        near(CombatStats.armorTarget(armor,s,gear),13*1.5*1.2,"Keep non-equipment modifiers and native operation ordering");
        near(armor.m_22135_(),12*1.5*1.2,"Computing override must not mutate original attributes");
        var health=new AttributeInstance(new RangedAttribute("test.health",20,1,1024),a->{});
        health.m_22118_(new AttributeModifier(UUID.randomUUID(),"Health effect",.5,AttributeModifier.Operation.MULTIPLY_BASE));
        UUID id=UUID.randomUUID();double original=health.m_22135_();
        double delta=(40-original)/CombatStats.additionScale(health,id);
        health.m_22118_(new AttributeModifier(id,"Temporary health",delta,AttributeModifier.Operation.ADDITION));near(health.m_22135_(),40,"Requested actual health under native multiplier");
        var permanent=AttributeInstance.class.getDeclaredField("f_22091_");permanent.setAccessible(true);
        try {check(((Set<?>)permanent.get(health)).isEmpty(),"Transient modifier must not persist on player");}catch(ReflectiveOperationException e){throw new AssertionError(e);}
        health.m_22120_(id);near(health.m_22135_(),original,"Removing our modifier restores prior health and effects");
        var capped=new CombatSettings(20,List.of(30.0,30.0,30.0,30.0),CombatSettings.defaults().parts());
        check(CombatStats.armorTarget(armor,capped,gear)>30,"Compute raw correction before native armor cap, avoiding feedback at cap");
        System.out.println("PASS: real AttributeInstance armor-slot overrides, unequipped slots, native modifiers/caps, transient health and exact removal");
    }
    private static byte[] bytes(String name)throws Exception{try(var in=CombatSettingsTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    public static void mixins(IMixinTransformer transformer)throws Exception {
        Map<String,byte[]> changed=new HashMap<>();
        for(String name:List.of("com.tacz.guns.entity.EntityKineticBullet$EntityResult","com.tacz.guns.util.TacHitResult","com.atsuishio.superbwarfare.world.phys.EntityResult","com.atsuishio.superbwarfare.world.phys.ExtendedEntityRayTraceResult","com.tacz.guns.entity.EntityKineticBullet","com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity")) {
            byte[] raw=bytes(name),after=transformer.transformClassBytes(name,name,raw);changed.put(name,after);
            var node=new ClassNode();new ClassReader(after).accept(node,0);
            check(!Arrays.equals(raw,after),"Actual combat Mixin applied to "+name);
            if(name.endsWith("Result"))check(node.interfaces.contains("dev/minestrike/skins/DamageRegion"),"Per-result region storage "+name);
            if(name.endsWith("EntityKineticBullet")) {
                var hit=node.methods.stream().filter(m->m.name.equals("onHitEntity")).findFirst().orElseThrow();
                check(count(hit,"minestrike$base")==1&&count(hit,"minestrike$head")==1,"TaCZ overrides event outputs after FPSMatch handlers, exactly once");
                check(count(hit,"post")==3&&count(hit,"tacAttackEntity")==2,"Keep original events and native AP/non-AP damage pipeline");
                check(count(hit,"isHeadShot")==1,"Preserve real headshot flag");
            }
            if(name.endsWith("ProjectileEntity")) {
                var hit=node.methods.stream().filter(m->m.name.equals("onHitEntity")).findFirst().orElseThrow();
                var damage=node.methods.stream().filter(m->m.name.equals("performDamage")).findFirst().orElseThrow();
                check(count(hit,"minestrike$leg")==1,"Replace native leg factor without dividing by possibly zero factor");
                check(count(damage,"minestrike$amount")==1&&count(damage,"minestrike$head")==1,"Single custom factor before AP split, replace native head factor");
                check(count(damage,"forceHurt")==2,"Keep Superb native normal/AP hurt calls");
            }
        }
        regionRuntime(changed);
        System.out.println("PASS: actual TaCZ/Superb Mixin transforms, per-result region copy, FPSMatch head override order, native AP split and headshot effects retained");
    }
    private static void compat() throws Exception {
        org.spongepowered.asm.launch.MixinBootstrap.init();
        org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setSide(org.spongepowered.asm.mixin.MixinEnvironment.Side.CLIENT);
        Path configs=Path.of(CombatSettingsTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Files.writeString(configs.resolve("combat-tweaks-compat.json"),CombatSettings.JSON.toJson(Map.of(
            "required",true,"minVersion","0.8","compatibilityLevel","JAVA_17",
            "refmap","tacztweaks.refmap.json","mixinextras",Map.of("minVersion","0.5.0"),"package","me.muksc.tacztweaks.mixin","mixins",List.of("features.EntityKineticBulletMixin","modifiers.EntityKineticBulletMixin","tweaks.EntityKineticBulletMixin","features.bullet_sounds.EntityKineticBulletMixin"),
            "injectors",Map.of("defaultRequire",1))));
        for(String config:List.of("minestrike-skins.mixins.json","combat-tweaks-compat.json"))org.spongepowered.asm.mixin.Mixins.addConfiguration(config);
        var ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);
        var transformer=(IMixinTransformer)ctor.newInstance();
        org.spongepowered.asm.mixin.MixinEnvironment.getDefaultEnvironment().setActiveTransformer(transformer);
        Class.forName("com.llamalad7.mixinextras.MixinExtrasBootstrap").getMethod("init").invoke(null);
        String name="com.tacz.guns.entity.EntityKineticBullet";
        var node=new ClassNode();new ClassReader(transformer.transformClassBytes(name,name,bytes(name))).accept(node,0);
        for(String suffix:List.of("minestrike$base","minestrike$head","tacztweaks$onHitEntity$playerDamageModifier","tacztweaks$onHitEntity$playerHeadshotModifier","tacztweaks$getDamage$applyDamageModifiers","tacztweaks$onHitEntity$onHitPlayer"))
            check(node.methods.stream().anyMatch(m->count(m,suffix)>0),"Coexisting installed combat hook: "+suffix);
        System.out.println("PASS: joint actual combat Mixin transformation with installed TaCZ Tweaks damage, headshot, entity-piercing and bullet-sound hooks");
    }
    private static long count(MethodNode m,String suffix){return Arrays.stream(m.instructions.toArray()).filter(i->i instanceof MethodInsnNode c&&c.name.endsWith(suffix)).count();}
    private static void regionRuntime(Map<String,byte[]> changed)throws Exception {
        // Instantiate the actual transformed TaCZ result classes. No running world or Forge registry required.
        ClassLoader loader=new ClassLoader(CombatSettingsTest.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!changed.containsKey(name)||name.endsWith("EntityKineticBullet")||name.endsWith("ProjectileEntity"))return super.loadClass(name,resolve);
                Class<?> type=findLoadedClass(name);if(type==null){byte[] b=changed.get(name);type=defineClass(name,b,0,b.length);}if(resolve)resolveClass(type);return type;
            }
        };
        var resultClass=loader.loadClass("com.tacz.guns.entity.EntityKineticBullet$EntityResult");
        var hitClass=loader.loadClass("com.tacz.guns.util.TacHitResult");
        for(int region:new int[]{0,1,2,3}){
            Object result=resultClass.getConstructors()[0].newInstance(null,Vec3.f_82478_,region==1);
            check(((DamageRegion)result).minestrike$region()==-1,"Untagged native results must not accidentally use torso override");
            ((DamageRegion)result).minestrike$region(region);
            Object hit=hitClass.getConstructor(resultClass).newInstance(result);
            check(((DamageRegion)hit).minestrike$region()==region,"Actual constructor preserves per-ray region "+region);
        }
    }
}
