import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.util.*;

public class AimActionsTest {
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        for(var entry:Map.of(
                "com.tacz.guns.client.gameplay.LocalPlayerAim",List.of("minestrike$input"),
                "com.tacz.guns.client.input.AimKey",List.of("minestrike$intent"),
                "com.tacz.guns.client.gameplay.LocalPlayerDraw",List.of("minestrike$draw"),
                "com.atsuishio.superbwarfare.event.ClientEventHandler",List.of("minestrike$shot"),
                "com.atsuishio.superbwarfare.event.ClickEventHandler",List.of("minestrike$press","minestrike$release")).entrySet()) {
            byte[] raw;try(var in=AimActionsTest.class.getClassLoader().getResourceAsStream(entry.getKey().replace('.','/')+".class")){raw=in.readAllBytes();}
            var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);
            new ClassReader(transformer.transformClassBytes(entry.getKey(),entry.getKey(),raw)).accept(after,0);
            for(String hook:entry.getValue()) {
                long count=after.methods.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).filter(i->i instanceof MethodInsnNode c&&c.name.contains(hook)).count();
                check(count>0,"Native input/action hook applied: "+entry.getKey()+"."+hook);
            }
            for(var method:before.methods) {
                var actual=after.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
                List<String> oldCalls=calls(method,false),newCalls=calls(actual,true);
                check(oldCalls.equals(newCalls),"Original packets/animation/sound/recoil calls preserved: "+entry.getKey()+"."+method.name);
            }
        }
        System.out.println("PASS: actual TaCZ aim/key/draw and Superb shot/zoom Mixin transformations; original native packets and side effects preserved");
    }
    private static List<String> calls(MethodNode method,boolean patched) {
        var result=new ArrayList<String>();
        for(var ins:method.instructions)if(ins instanceof MethodInsnNode c) {
            if(patched&&c.name.contains("minestrike$aim")&&method.name.equals("handleWeaponZoom"))result.add("kotlin/ranges/RangesKtcoerceIn(DDD)D");
            else if(patched&&c.name.contains("minestrike$intent"))result.add("com/tacz/guns/api/client/gameplay/IClientPlayerGunOperatorisAim()Z");
            else if(!patched||(!c.name.contains("minestrike$")&&!c.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/")))result.add(c.owner+c.name+c.desc);
        }
        return result;
    }
}
