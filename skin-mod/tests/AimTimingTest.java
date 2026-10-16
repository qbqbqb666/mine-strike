import dev.minestrike.skins.AimTiming;
import dev.minestrike.skins.AimAnimations;
import dev.minestrike.skins.ReloadViewState;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.ObjectAnimationRunner;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.resources.ResourceLocation;
import java.nio.file.*;
import java.net.*;
import java.util.*;
import java.util.zip.ZipFile;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public class AimTimingTest {
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private static class Animation extends ObjectAnimation {Animation(String name){super(name);}}
    public static void main(String[] args)throws Exception {
        for(float old:new float[]{.05f,.15f,.25f,.8f}) {
            check(AimTiming.opening(old,true)==.25f,"All TaCZ opening uses requested 250ms time");
            check(AimTiming.opening(old,false)==old,"Original closing speed retained");
        }
        for(double weight:new double[]{1,3,7,15})for(int fps:new int[]{30,60,144}) {
            double progress=0;int frames=0;
            while(progress<1 && frames<fps){progress=AimTiming.advance(progress,20.0/fps);frames++;}
            check(Math.abs(frames/(double)fps-AimTiming.SECONDS)<=1.0/fps+.00001,"Superb opening duration is 250ms independent of weight/FPS");
        }
        check(AimTiming.advance(.3,-1)==.3&&AimTiming.advance(.9,100)==1,"Progress bounds and zero elapsed time");
        for(String name:List.of("bolt","awp.blot","animation.cs_awp.draw","weapon_equip")) {
            check(AimAnimations.name(name,true),"Action naming: "+name);
            var runner=new ObjectAnimationRunner(new Animation(name));
            runner.run();check(AimAnimations.active(runner,true),"Actual native action runner detected");
            runner.stop();check(!AimAnimations.active(runner,true),"Stopped action cannot pin ADS correction forever");
        }
        check(AimAnimations.name("shoot",true,true),"Packed AWP shoot clip includes bolt cycling even with OPEN_BOLT data");
        var tail=new ObjectAnimationRunner(new Animation("bolt")); tail.run();
        tail.transition(new ObjectAnimationRunner(new Animation("idle")),1000000000L);
        check(AimAnimations.active(tail,true),"Bolt-to-idle blending tail stays compensated after gameplay cooldown");
        var incoming=new ObjectAnimationRunner(new Animation("idle"));incoming.run();
        incoming.transition(new ObjectAnimationRunner(new Animation("draw")),1000000000L);
        check(AimAnimations.active(incoming,true),"Incoming draw is detected during animation crossfade");
        check(!AimAnimations.name("shoot",true)&&!AimAnimations.name("idle",false),"Ordinary firing and idle are not action compensation");
        check(AimAnimations.name("reload_tactical",false)&&!AimAnimations.name("reload_empty",true),"Keep reload transition separately");
        var state=new ReloadViewState();
        check(state.update("draw",true,.5f,0,true)==.5f,"Instant ADS during newly drawn gun is aligned on first frame");
        check(state.update("draw",true,1,10000000,true)==1,"Bolt/draw has full alignment without waiting for old reload ramp");
        check(Math.abs(state.update("draw",false,1,70000000)-.5)<.00001,"Action tail returns smoothly");
        try(var zip=new ZipFile(Path.of(args[0]).getParent().resolve("tacz-1.20.1-1.1.8-hotfix.jar").toFile())) {
            for(String name:List.of("m107","m870","m95")) {
                var data=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry("assets/tacz/custom/tacz_default_gun/data/tacz/data/guns/"+name+"_data.json")).readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                float nativeTime=data.has("bolt_action_time")?data.get("bolt_action_time").getAsFloat():0;
                var params=new HashMap<String,Object>();
                if(data.has("script_param")&&data.getAsJsonObject("script_param").has("bolt_time"))params.put("bolt_time",data.getAsJsonObject("script_param").get("bolt_time").getAsDouble());
                double time=AimAnimations.cycleSeconds(nativeTime,params);
                check(name.equals("m107")?time==0:time>0,"Original native gun distinguishes semi-auto from scripted pump/bolt: "+name);
            }
        }
        baseline(Path.of(args[0]),Path.of(args[1]));
        System.out.println("PASS: original packed AWP/scope baseline, unified 250ms TaCZ/Superb opening, original closing, live bolt/draw runners and immediate ADS alignment");
    }
    private static void baseline(Path jar,Path config)throws Exception {
        var tweaks=JsonParser.parseString(Files.readString(config)).getAsJsonObject().getAsJsonObject("modifiers").getAsJsonObject("aimTime");
        check(tweaks.get("addend").getAsFloat()==0&&tweaks.get("multiplier").getAsFloat()==1&&tweaks.get("function").getAsString().isEmpty(),"Current AWP global modifier is neutral");
        String prefix="top.shmfun.q7dfba46dc2a53842fd682d68fa7c4e4c.";
        try(var loader=new URLClassLoader(new URL[]{jar.toUri().toURL()},AimTimingTest.class.getClassLoader()) {
            public URL getResource(String name){return name.equals("META-INF/gp.seal")?findResource(name):super.getResource(name);}
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith(prefix))return super.loadClass(name,resolve);
                Class<?> type=findLoadedClass(name);if(type==null)type=findClass(name);if(resolve)resolveClass(type);return type;
            }
        };var zip=new ZipFile(jar.toFile())) {
            var type=loader.loadClass(prefix+"mem.MemZipPackResources");
            var pack=(PackResources)type.getConstructor(String.class,byte[].class).newInstance("aim-baseline",zip.getInputStream(zip.getEntry("assets/mcs2_gunpack/pack_01.zip")).readAllBytes());
            for(String name:List.of("cs_awp","cs_awp_dragon_lore","cs_awp_fade")) {
                var data=read(pack,"data/guns/"+name+"_data.json");
                check(data.get("aim_time").getAsFloat()==.15f,"Original "+name+" aim_time is still baseline 0.15 seconds");
                check(data.get("bolt").getAsString().equals("open_bolt") && data.get("bolt_action_time").getAsFloat()>0,"Current AWP explicitly encodes bolt cycling despite open_bolt type");
            }
            try(var in=pack.m_214146_(PackType.CLIENT_RESOURCES,new ResourceLocation("mcs2","animations/cs_awp_baked.animation.json")).m_247737_()) {
                var animations=JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonObject("animations");
                check(animations.has("shoot")&&animations.has("draw")&&!animations.has("bolt"),"Packed AWP uses combined shoot/bolt and draw animation names");
            }
            check(read(pack,"data/attachments/awp_scope_data.json").get("ads_addend").getAsFloat()==0,"Original AWP scope adds no ADS delay");
            pack.close();
        }
    }
    private static JsonObject read(PackResources pack,String path)throws Exception {
        try(var in=pack.m_214146_(PackType.SERVER_DATA,new ResourceLocation("mcs2",path)).m_247737_()) {
            return JsonParser.parseString(new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
    private static byte[] resource(String name)throws Exception {
        try(var in=AimTimingTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        var targets=Map.of(
            "com.tacz.guns.client.gameplay.LocalPlayerAim","getAlphaProgress",
            "com.tacz.guns.entity.shooter.LivingEntityAim","tickAimingProgress",
            "com.atsuishio.superbwarfare.event.ClientEventHandler","handleWeaponZoom");
        for(var entry:targets.entrySet()) {
            var before=new ClassNode();var actual=new ClassNode();byte[] raw=resource(entry.getKey());new ClassReader(raw).accept(before,0);
            new ClassReader(transformer.transformClassBytes(entry.getKey(),entry.getKey(),raw)).accept(actual,0);
            var target=actual.methods.stream().filter(m->m.name.equals(entry.getValue())).findFirst().orElseThrow();
            int hooks=0;for(var ins:target.instructions)if(ins instanceof MethodInsnNode call&&call.name.contains("minestrike$aim"))hooks++;
            check(hooks==1,"Exactly one opening timing hook: "+entry.getKey());
            for(var method:before.methods) {
                var after=actual.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
                var oldCalls=new ArrayList<String>();var newCalls=new ArrayList<String>();
                for(var ins:method.instructions)if(ins instanceof MethodInsnNode c)oldCalls.add(c.owner+c.name+c.desc);
                for(var ins:after.instructions)if(ins instanceof MethodInsnNode c) {
                    if(c.name.contains("minestrike$aim")&&entry.getValue().equals("handleWeaponZoom"))newCalls.add("kotlin/ranges/RangesKtcoerceIn(DDD)D");
                    else if(!c.name.contains("minestrike$")&&!c.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))newCalls.add(c.owner+c.name+c.desc);
                }
                check(oldCalls.equals(newCalls),"Native aim/recoil/zoom conditions and other methods preserved: "+entry.getKey()+"."+method.name);
            }
        }
        String controller="com.tacz.guns.api.client.animation.AnimationController";var actual=new ClassNode();
        new ClassReader(transformer.transformClassBytes(controller,controller,resource(controller))).accept(actual,0);
        check(actual.interfaces.contains("dev/minestrike/skins/mixin/TaczReloadViewMixin$Tracks"),"Read actual full track list when native updating-track filter is null");
        System.out.println("PASS: real TaCZ client/server and Superb opening timing Mixins, native closing/conditions retained and complete animation-track accessor");
    }
}
