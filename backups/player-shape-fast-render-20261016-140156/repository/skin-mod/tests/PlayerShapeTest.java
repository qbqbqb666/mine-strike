import dev.minestrike.skins.RoundHeadMesh;
import com.google.gson.Gson;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
            for(String name:List.of("minestrike-skins.mixins.json","shape-playeranimator-compat.json","shape-tacz-compat.json"))Mixins.addConfiguration(name);
            var ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);
            var transformer=(IMixinTransformer)ctor.newInstance();
            for(String name:List.of("net.minecraft.client.model.HumanoidModel","net.minecraft.client.model.PlayerModel",
                    "net.minecraft.client.model.geom.ModelPart","net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer")) {
                byte[] before=resource(name),after=transformer.transformClassBytes(name,name,before);
                check(!Arrays.equals(before,after),"Animation integration target unchanged: "+name);
            }
            System.out.println("PASS: joint real Mixin transformation with installed PlayerAnimator and TaCZ model/armor hooks");
            return;
        }
        for(var mesh:List.of(RoundHeadMesh.HEAD,RoundHeadMesh.HAT)) {
            boolean hat=mesh==RoundHeadMesh.HAT; float radius=hat?4.5f:4;
            Map<String,Integer> edges=new HashMap<>();
            for(int q=0;q<mesh.size();q+=4) {
                var a=mesh.get(q);var b=mesh.get(q+1);var c=mesh.get(q+2);
                double nx=(b.y()-a.y())*(c.z()-a.z())-(b.z()-a.z())*(c.y()-a.y());
                double ny=(b.z()-a.z())*(c.x()-a.x())-(b.x()-a.x())*(c.z()-a.z());
                double nz=(b.x()-a.x())*(c.y()-a.y())-(b.y()-a.y())*(c.x()-a.x());
                check(nx*a.nx()+ny*a.ny()+nz*a.nz()>0,"Inward or degenerate quad");
                for(int i=0;i<4;i++) {
                    var v=mesh.get(q+i);var end=mesh.get(q+(i+1)%4);
                    check(Math.abs(Math.sqrt(v.x()*v.x()+(v.y()+4)*(v.y()+4)+v.z()*v.z())-radius)<.00001,"Spherical surface");
                    check(Math.abs(v.nx()*v.nx()+v.ny()*v.ny()+v.nz()*v.nz()-1)<.00001,"Smooth unit normal");
                    check(v.u()>=(hat?.5f:0)&&v.u()<=(hat?1:.5f)&&v.v()>=0&&v.v()<=.25f,"Skin UV bounds");
                    String s=key(v),t=key(end);edges.merge(s.compareTo(t)<0?s+":"+t:t+":"+s,1,Integer::sum);
                }
            }
            check(edges.values().stream().allMatch(n->n==2),"Closed mesh without seams");
        }
        int faceSize=12*12*4;
        var front=RoundHeadMesh.HEAD.subList(3*faceSize,4*faceSize);
        check(front.stream().allMatch(v->v.z()<0&&v.u()>=.125f&&v.u()<=.25f&&v.v()>=.125f&&v.v()<=.25f),"Vanilla face rectangle");
        check(front.get(0).x()>0&&front.get(0).u()==.25f&&front.get(0).v()==.125f,"Vanilla face orientation");
        if(args.length>0)Files.writeString(Path.of(args[0]),new Gson().toJson(Map.of("head",RoundHeadMesh.HEAD,"hat",RoundHeadMesh.HAT)));
        System.out.println("PASS: smooth closed sphere, outward winding, seams and vanilla face/hat UVs");
    }
    private static String key(RoundHeadMesh.Vertex v) { return Math.round(v.x()*100000)+","+Math.round(v.y()*100000)+","+Math.round(v.z()*100000); }
    private static byte[] resource(String name)throws Exception {
        try(var in=PlayerShapeTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        Map<String,byte[]> changed=new HashMap<>();
        Map<String,List<String>> expected=Map.of(
                "net.minecraft.client.model.PlayerModel",List.of("minestrike$model"),
                "net.minecraft.client.model.geom.ModelPart",List.of("minestrike$pivot","minestrike$proportions","minestrike$roundHead"),
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
        ClassLoader loader=new ClassLoader(PlayerShapeTest.class.getClassLoader()) {
            @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith("net.minecraft.client.model.")&&!name.startsWith("dev.minestrike.skins.PlayerShape"))return super.loadClass(name,resolve);
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
        check(Math.abs(pose.m_85850_().m_252922_().m30()-5*.78/16)<.000001,"Runtime pivot contraction");
        check(Math.abs(pose.m_85850_().m_252922_().m00()-1)<.000001,"Held-item basis must retain weapon size");
        part.getMethod("minestrike$shape",int.class).invoke(head,0);
        var ordinary=new PoseStack();
        part.getMethod("m_104299_",PoseStack.class).invoke(head,ordinary);
        check(Math.abs(ordinary.m_85850_().m_252922_().m30()-5f/16)<.000001,"Unmarked model must remain vanilla");
        Object drawable=part.getConstructor(List.class,Map.class).newInstance(List.of(new Object()),Map.of());
        part.getMethod("minestrike$shape",int.class).invoke(drawable,2);
        List<float[]> emitted=new ArrayList<>();
        VertexConsumer vertices=(VertexConsumer)java.lang.reflect.Proxy.newProxyInstance(PlayerShapeTest.class.getClassLoader(),new Class<?>[]{VertexConsumer.class},(proxy,method,values)->{
            if(method.getName().equals("m_252986_")) {
                var vector=new org.joml.Vector4f((float)values[1],(float)values[2],(float)values[3],1).mul((org.joml.Matrix4f)values[0]);
                emitted.add(new float[]{vector.x(),vector.y(),vector.z()});
            }
            return method.getReturnType()==void.class?null:proxy;
        });
        var renderPose=new PoseStack();
        part.getMethod("m_104306_",PoseStack.class,VertexConsumer.class,int.class,int.class,float.class,float.class,float.class,float.class)
                .invoke(drawable,renderPose,vertices,0,0,1f,1f,1f,1f);
        check(emitted.size()==RoundHeadMesh.HEAD.size(),"Actual ModelPart must draw the complete replacement mesh");
        for(float[] v:emitted)check(Math.abs(Math.sqrt(v[0]*v[0]+Math.pow(v[1]+4*.82/16,2)+v[2]*v[2])-4*.82/16)<.000001,"Actual rendered head radius");
        check(renderPose.m_85850_().m_252922_().equals(new org.joml.Matrix4f()),"ModelPart must restore its pose after drawing");
        System.out.println("PASS: real player/part/armor Mixins; native render calls retained; runtime pivot, unscaled weapon basis and unmarked fallback");
    }
}
