import dev.minestrike.skins.PlayerShape;
import dev.minestrike.skins.PlayerMetrics;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.core.Direction;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.nio.file.*;
import java.util.*;

public class PlayerShapeTest {
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void main(String[] args)throws Exception {
        if(args.length>0 && args[0].equals("--compat")) {
            MixinBootstrap.init();
            MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
            MixinEnvironment.getDefaultEnvironment().setObfuscationContext("searge");
            Path configs=Path.of(PlayerShapeTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Files.writeString(configs.resolve("shape-oculus-compat.json"),new Gson().toJson(Map.of(
                    "required",true,"minVersion","0.8","compatibilityLevel","JAVA_17",
                    "package","net.irisshaders.iris.compat.sodium.mixin.copyEntity",
                    "client",List.of("ModelPartMixin","CuboidMixin"),"refmap","sodiumCompatibility-oculus-mixins-refmap.json",
                    "injectors",Map.of("defaultRequire",1))));
            for(String name:List.of("minestrike-skins.mixins.json","shape-playeranimator-compat.json","shape-tacz-compat.json","shape-oculus-compat.json"))Mixins.addConfiguration(name);
            var ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);
            var transformer=(IMixinTransformer)ctor.newInstance();
            Map<String,byte[]> changed=new HashMap<>();
            for(String name:List.of("net.minecraft.client.model.HumanoidModel","net.minecraft.client.model.PlayerModel",
                    "net.minecraft.client.model.geom.ModelPart","net.minecraft.client.model.geom.ModelPart$Cube",
                    "net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer")) {
                byte[] before=resource(name),after=transformer.transformClassBytes(name,name,before);
                check(!Arrays.equals(before,after),"Animation integration target unchanged: "+name);
                changed.put(name,after);
            }
            Files.write(configs.resolve("shape-combined-ModelPart.class"),changed.get("net.minecraft.client.model.geom.ModelPart"));
            runtime(changed,true);
            System.out.println("PASS: joint real Mixin transformation with installed PlayerAnimator and TaCZ model/armor hooks");
            System.out.println("PASS: Oculus fast-render + Embeddium writer still emits the scaled cube head and 75 percent arms at restored total height through the scoped path");
            return;
        }
        check(PlayerShape.BODY_WIDTH==.75f && PlayerShape.ARM_WIDTH==.75f && PlayerShape.BODY_HEIGHT==PlayerMetrics.LENGTH && PlayerShape.HEAD_SCALE==.65f,"Approved appearance ratios");
        check(Math.abs(24*PlayerMetrics.LENGTH+8*PlayerMetrics.HEAD-32)<.00001,"Full foot-to-head height restored");
        for(float shoulder:new float[]{-5,5}) {
            float pivot=shoulder+PlayerShape.pivotOffset(shoulder,PlayerShape.ARM);
            float inner=pivot-Math.signum(shoulder)*PlayerShape.ARM_WIDTH;
            check(Math.abs(inner-Math.signum(shoulder)*4*PlayerShape.BODY_WIDTH)<.000001,"Arm must meet torso without a shoulder gap");
        }
        check(Math.abs(PlayerShape.verticalOffset(0)+2.8)<.00001 && Math.abs(PlayerShape.verticalOffset(12)+1.4)<.000001,"Neck and hip move together");
        check(Math.abs(12+PlayerShape.verticalOffset(12)+12*PlayerShape.BODY_HEIGHT-24)<.000001,"Resized legs retain standing foot height");
        var hand=new PoseStack();PlayerShape.hand(hand);
        check(Math.abs(hand.m_85850_().m_252922_().m31()-10*(PlayerMetrics.LENGTH-1)/16)<.000001 && hand.m_85850_().m_252922_().m11()==1,"Hand anchor moves without scaling weapon geometry");
        if(args.length>0)Files.writeString(Path.of(args[0]),new Gson().toJson(Map.of(
                "body",PlayerShape.BODY_WIDTH,"arm",PlayerShape.ARM_WIDTH,"head",PlayerShape.HEAD_SCALE,"height",PlayerShape.BODY_HEIGHT,"headShape","cube")));
        System.out.println("PASS: retained torso/head ratios, 75 percent arms at restored total height and attached shoulder pivots");
    }
    private static byte[] resource(String name)throws Exception {
        try(var in=PlayerShapeTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        Map<String,byte[]> changed=new HashMap<>();
        Map<String,List<String>> expected=Map.of(
                "net.minecraft.client.model.PlayerModel",List.of("minestrike$model","minestrike$hand"),
                "net.minecraft.client.model.geom.ModelPart",List.of("minestrike$pivot","minestrike$render"),
                "net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer",List.of("minestrike$armor"),
                "net.minecraft.client.renderer.entity.layers.CustomHeadLayer",List.of("minestrike$headItem"),
                "software.bernie.geckolib.renderer.GeoArmorRenderer",List.of("minestrike$fit","minestrike$restore"));
        for(var entry:expected.entrySet()) {
            String name=entry.getKey();byte[] before=resource(name),after=transformer.transformClassBytes(name,name,before);
            ClassNode original=new ClassNode(),actual=new ClassNode();
            new ClassReader(before).accept(original,0);new ClassReader(after).accept(actual,0);
            for(String hook:entry.getValue()) {
                int count=0;
                for(MethodNode m:actual.methods)for(AbstractInsnNode ins:m.instructions)
                    if(ins instanceof MethodInsnNode call&&call.name.contains(hook))count++;
                check(count==1,"Shape hook count: "+name+" / "+hook+" = "+count);
            }
            for(MethodNode m:original.methods) {
                MethodNode a=actual.methods.stream().filter(n->n.name.equals(m.name)&&n.desc.equals(m.desc)).findFirst().orElseThrow();
                List<String> oldCalls=new ArrayList<>(),newCalls=new ArrayList<>();
                for(AbstractInsnNode ins:m.instructions)if(ins instanceof MethodInsnNode call)oldCalls.add(call.owner+call.name+call.desc);
                for(AbstractInsnNode ins:a.instructions)if(ins instanceof MethodInsnNode call&&!call.name.contains("minestrike$")
                        &&!call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))newCalls.add(call.owner+call.name+call.desc);
                if(m.name.equals("<init>")) {
                    oldCalls.removeIf(s->s.equals("java/util/IdentityHashMap<init>()V"));
                    newCalls.removeIf(s->s.equals("java/util/IdentityHashMap<init>()V"));
                }
                check(oldCalls.equals(newCalls),"Native rendering calls changed: "+name+" / "+m.name);
            }
            changed.put(name,after);
        }
        runtime(changed,false);
        System.out.println("PASS: real player/part/armor Mixins; native render calls retained; runtime pivot, unscaled weapon basis and unmarked fallback");
    }
    private static void runtime(Map<String,byte[]> changed,boolean fast)throws Exception {
        ClassLoader loader=new ClassLoader(PlayerShapeTest.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith("net.minecraft.client.model.")&&!name.startsWith("dev.minestrike.skins.PlayerShape")
                        &&!name.equals("me.jellysquid.mods.sodium.client.render.immediate.model.ModelPartData"))return super.loadClass(name,resolve);
                Class<?> cls=findLoadedClass(name);
                if(cls==null)try{byte[] b=changed.get(name);if(b==null)b=resource(name);cls=defineClass(name,b,0,b.length);}
                catch(Exception e){throw new ClassNotFoundException(name,e);}
                if(resolve)resolveClass(cls);return cls;
            }
        };
        Class<?> part=loader.loadClass("net.minecraft.client.model.geom.ModelPart");
        Object head=part.getConstructor(List.class,Map.class).newInstance(List.of(),Map.of());
        part.getMethod("minestrike$shape",int.class).invoke(head,2);
        part.getField("f_104200_").setFloat(head,5);
        var pose=new PoseStack();
        part.getMethod("m_104299_",PoseStack.class).invoke(head,pose);
        check(Math.abs(pose.m_85850_().m_252922_().m30()-5*.75/16)<.000001,"Runtime pivot contraction");
        check(Math.abs(pose.m_85850_().m_252922_().m00()-1)<.000001,"Held-item basis must retain weapon size");
        part.getMethod("minestrike$shape",int.class).invoke(head,0);
        var ordinary=new PoseStack();
        part.getMethod("m_104299_",PoseStack.class).invoke(head,ordinary);
        check(Math.abs(ordinary.m_85850_().m_252922_().m30()-5f/16)<.000001,"Unmarked model must remain vanilla");
        Class<?> cube=loader.loadClass("net.minecraft.client.model.geom.ModelPart$Cube");
        Object headCube=cube.getConstructors()[0].newInstance(0,0,-4f,-8f,-4f,8f,8f,8f,0f,0f,0f,false,64f,64f,EnumSet.allOf(Direction.class));
        Object drawable=part.getConstructor(List.class,Map.class).newInstance(List.of(headCube),Map.of());
        part.getMethod("minestrike$shape",int.class).invoke(drawable,2);
        List<float[]> emitted=new ArrayList<>();
        VertexConsumer vertices=(VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(PlayerShapeTest.class.getClassLoader(),
                fast?new Class<?>[]{VertexConsumer.class,VertexBufferWriter.class}:new Class<?>[]{VertexConsumer.class},(proxy,method,values)->{
            if(method.getName().equals("push"))throw new AssertionError("Fast cuboid writer bypassed the scoped proportions");
            if(method.getReturnType()==boolean.class)return true;
            if(method.getName().equals("m_252986_")) {
                var vector=new org.joml.Vector4f((float)values[1],(float)values[2],(float)values[3],1).mul((org.joml.Matrix4f)values[0]);
                emitted.add(new float[]{vector.x(),vector.y(),vector.z()});
            }
            if(method.getName().equals("m_5954_"))emitted.add(new float[]{(float)values[0],(float)values[1],(float)values[2],(float)values[7],(float)values[8]});
            return method.getReturnType()==void.class?null:proxy;
        });
        var renderPose=new PoseStack();
        part.getMethod("m_104306_",PoseStack.class,VertexConsumer.class,int.class,int.class,float.class,float.class,float.class,float.class)
                .invoke(drawable,renderPose,vertices,0,0,1f,1f,1f,1f);
        check(emitted.size()==24,"Head must use the original six cube faces");
        for(float[] v:emitted) {
            check(Math.abs(Math.abs(v[0])-4*.65/16)<.000001 && Math.abs(Math.abs(v[2])-4*.65/16)<.000001,"Square head corners");
            check(Math.abs(v[1]-PlayerShape.verticalOffset(0)/16)<.000001 || Math.abs(v[1]-PlayerShape.verticalOffset(0)/16+8*.65/16)<.000001,"Original head height scaled to 65 percent");
            check(v[3]>=0 && v[3]<=.5 && v[4]>=0 && v[4]<=.25,"Native head UVs");
        }
        check(renderPose.m_85850_().m_252922_().equals(new org.joml.Matrix4f()),"ModelPart must restore its pose after drawing");
        part.getMethod("minestrike$shape",int.class).invoke(drawable,1);
        emitted.clear();
        var draw=part.getMethod("m_104306_",PoseStack.class,VertexConsumer.class,int.class,int.class,float.class,float.class,float.class,float.class);
        draw.invoke(drawable,renderPose,vertices,0,0,1f,1f,1f,1f);
        check(emitted.size()==24,"Slim body must retain the six native cube faces");
        double width=emitted.stream().mapToDouble(v->v[0]).max().orElseThrow()-emitted.stream().mapToDouble(v->v[0]).min().orElseThrow();
        check(Math.abs(width-8*.75/16)<.000001,"Actual rendered body width");
        part.getMethod("minestrike$shape",int.class).invoke(drawable,5);emitted.clear();
        draw.invoke(drawable,renderPose,vertices,0,0,1f,1f,1f,1f);
        width=emitted.stream().mapToDouble(v->v[0]).max().orElseThrow()-emitted.stream().mapToDouble(v->v[0]).min().orElseThrow();
        double height=emitted.stream().mapToDouble(v->v[1]).max().orElseThrow()-emitted.stream().mapToDouble(v->v[1]).min().orElseThrow();
        check(emitted.size()==24 && Math.abs(width-8*.75/16)<.000001 && Math.abs(height-8*PlayerMetrics.LENGTH/16)<.000001,"75 percent arm width and restored total height");
        for(float shoulder:new float[]{-5,5}) {
            part.getField("f_104200_").setFloat(drawable,shoulder);
            var attachment=new PoseStack();part.getMethod("m_104299_",PoseStack.class).invoke(drawable,attachment);
            check(Math.abs(attachment.m_85850_().m_252922_().m30()-(shoulder+PlayerShape.pivotOffset(shoulder,PlayerShape.ARM))/16)<.000001,"Arm attachment pivot follows narrowed shoulders");
            check(attachment.m_85850_().m_252922_().m00()==1,"Arm attachment must retain weapon scale");
        }
        Object legCube=cube.getConstructors()[0].newInstance(0,16,-2f,0f,-2f,4f,12f,4f,0f,0f,0f,false,64f,64f,EnumSet.allOf(Direction.class));
        Object leg=part.getConstructor(List.class,Map.class).newInstance(List.of(legCube),Map.of());
        part.getMethod("minestrike$shape",int.class).invoke(leg,1);part.getField("f_104201_").setFloat(leg,12);
        emitted.clear();draw.invoke(leg,renderPose,vertices,0,0,1f,1f,1f,1f);
        double foot=emitted.stream().mapToDouble(v->v[1]).max().orElseThrow();
        double hip=emitted.stream().mapToDouble(v->v[1]).min().orElseThrow();
        check(Math.abs(foot-24f/16)<.000001 && Math.abs(hip-10.6/16)<.000001,"Actual resized leg remains grounded");
        Object torsoCube=cube.getConstructors()[0].newInstance(16,16,-4f,0f,-2f,8f,12f,4f,0f,0f,0f,false,64f,64f,EnumSet.allOf(Direction.class));
        Object torso=part.getConstructor(List.class,Map.class).newInstance(List.of(torsoCube),Map.of());
        part.getMethod("minestrike$shape",int.class).invoke(torso,1);emitted.clear();
        draw.invoke(torso,renderPose,vertices,0,0,1f,1f,1f,1f);
        check(Math.abs(emitted.stream().mapToDouble(v->v[1]).max().orElseThrow()-hip)<.000001,"Torso and leg join at the same resized hip");
        check(Math.abs(emitted.stream().mapToDouble(v->v[1]).min().orElseThrow()+2.8/16)<.000001,"Torso meets shifted neck");
        Map<String,Object> children=new HashMap<>();
        for(String name:List.of("head","hat","body","jacket","right_arm","left_arm","right_sleeve","left_sleeve","right_leg","left_leg","right_pants","left_pants"))
            children.put(name,part.getConstructor(List.class,Map.class).newInstance(List.of(),Map.of()));
        Object root=part.getConstructor(List.class,Map.class).newInstance(List.of(),children);
        loader.loadClass("dev.minestrike.skins.PlayerShape").getMethod("tag",part).invoke(null,root);
        for(var entry:children.entrySet()) {
            int expected=entry.getKey().contains("arm") || entry.getKey().contains("sleeve")?5:entry.getKey().equals("head")?2:entry.getKey().equals("hat")?3:1;
            check((int)part.getMethod("minestrike$shape").invoke(entry.getValue())==expected,"Correct player/outer-layer classification: "+entry.getKey());
        }
        part.getField("f_104207_").setBoolean(drawable,false);emitted.clear();
        draw.invoke(drawable,renderPose,vertices,0,0,1f,1f,1f,1f);
        check(emitted.isEmpty()&&renderPose.m_85850_().m_252922_().equals(new org.joml.Matrix4f()),"Hidden parts must not render or change pose");
    }
}
