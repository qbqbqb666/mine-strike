import dev.minestrike.skins.ReloadViewState;
import dev.minestrike.skins.ReloadAlignment;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.util.*;
import java.nio.file.*;
import com.google.gson.Gson;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.*;

public class ReloadViewTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static void near(Matrix4f a, Matrix4f b, String message) {
        check(a.equals(b, .0003f), message + "\n" + a + "\n" + b);
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("--compat")) { compat(); return; }
        var state = new ReloadViewState();
        check(state.update("gun-a", true, 1, 0) == 0, "Reload entry is continuous");
        check(state.update("gun-a", true, 1, 120000000) == 1, "Full reload alignment after transition");
        check(Math.abs(state.update("gun-a", false, 1, 180000000) - .5) < .00001, "Finish/cancel fades rather than snaps");
        check(state.update("gun-b", true, 1, 190000000) == 0, "Weapon/context change resets immediately");
        check(state.update(null, true, 1, 200000000) == 0, "Disconnect/third person resets state");
        check(state.update("gun-b", true, 0, 210000000) == 0, "Hip-fire reload is untouched");
        check(state.update("gun-b", true, 0, 400000000) == 0, "No forced aiming");
        check(state.update("gun-b", true, .5f, 410000000) == .5f, "Use actual ADS blend, without changing aim speed");
        for (int fps : new int[]{30,60,144}) {
            var timed = new ReloadViewState();
            for (int frame = 0; frame <= fps; frame++) timed.update("same", true, 1, frame * 1000000000L / fps);
            check(timed.update("same", true, 1, 1000000000) == 1, "FPS independent reload envelope");
        }
        var random = new Random(47);
        for (int trial = 0; trial < 300; trial++) {
            var root = new BedrockPart("root"); var pivot = new BedrockPart("gun"); var sight = new BedrockPart("scope_pos");
            root.addChild(pivot); pivot.addChild(sight);
            var parts = List.of(root,pivot,sight);
            for (var part : parts) {
                part.x = random.nextFloat() * 12; part.y = random.nextFloat() * 24; part.z = random.nextFloat() * 12;
                part.xRot = random.nextFloat(); part.yRot = random.nextFloat(); part.zRot = random.nextFloat();
                part.offsetX = random.nextFloat() - .5f; part.offsetY = random.nextFloat() - .5f; part.offsetZ = random.nextFloat() - .5f;
                part.additionalQuaternion.rotationXYZ(random.nextFloat()*3,random.nextFloat()*3,random.nextFloat()*3);
                part.xScale = .8f+random.nextFloat()*.4f; part.yScale = .8f+random.nextFloat()*.4f; part.zScale = .8f+random.nextFloat()*.4f;
            }
            var nativeAnimated = new PoseStack();
            for (var part : parts) part.translateAndRotateAndScale(nativeAnimated);
            near(ReloadAlignment.path(parts,true), nativeAnimated.m_85850_().m_252922_(), "Follow actual installed bone transform order");
            var correction = ReloadAlignment.correction(parts);
            check(correction != null, "Valid bone paths have compensation");
            near(new Matrix4f(correction).mul(nativeAnimated.m_85850_().m_252922_()), ReloadAlignment.path(parts,false), "Sight position and orientation stay fixed during arbitrary reload rotations/scales");
            var pose = new PoseStack();
            pose.m_252880_(.3f,1.5f,-.6f);
            if (trial % 2 == 0) pose.m_85841_(-.75f,1.2f,.9f);
            Matrix4f before = new Matrix4f(pose.m_85850_().m_252922_());
            pose.m_252781_(new Quaternionf().rotationXYZ(.2f,.5f,-.1f));
            ReloadAlignment.apply(pose,before,parts,1);
            near(new Matrix4f(pose.m_85850_().m_252922_()).mul(nativeAnimated.m_85850_().m_252922_()),
                    new Matrix4f(before).mul(ReloadAlignment.path(parts,false)), "Replace native constraint once, keeping optic anchor stable");
            check(pose.m_85850_().m_252943_().isFinite(), "Corrected normals remain finite");
            near(ReloadAlignment.path(parts,true), nativeAnimated.m_85850_().m_252922_(), "Never mutate shared gun/hand/magazine bone state");
            near(ReloadAlignment.blend(before,pose.m_85850_().m_252922_(),0),before,"Zero weight preserves original rendering exactly");
        }
        var mirrored = new PoseStack(); mirrored.m_85841_(-1,1,1);
        var mirrorBefore = new Matrix4f(mirrored.m_85850_().m_252922_());
        ReloadAlignment.apply(mirrored,mirrorBefore,List.of(new BedrockPart("idle")),.5f);
        near(mirrorBefore,mirrored.m_85850_().m_252922_(),"Partial blend preserves left-handed reflection and original scale");
        var broken = new BedrockPart("singular"); broken.xScale = 0;
        check(ReloadAlignment.correction(List.of(broken)) == null, "Singular custom models fall back without NaN");
        var pose = new PoseStack(); var initial = new Matrix4f(pose.m_85850_().m_252922_());
        ReloadAlignment.apply(pose,initial,null,1); ReloadAlignment.apply(pose,initial,List.of(broken),1);
        near(initial,pose.m_85850_().m_252922_(),"Missing or degenerate anchors preserve native result");
        System.out.println("PASS: reload ADS entry/finish/cancel/context lifecycle, FPS independence, 300 native bone-path alignments, stable optic orientation, normals and untouched animation bones");
    }
    private static void compat() throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        Path configs=Path.of(ReloadViewTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        Files.writeString(configs.resolve("reload-tweaks-compat.json"),new Gson().toJson(Map.of(
            "required",true,"minVersion","0.8","compatibilityLevel","JAVA_17",
            "package","me.muksc.tacztweaks.mixin","client",List.of("gun.LocalPlayerReloadMixin","gun.movement.LocalPlayerReloadMixin","gun.LocalPlayerDrawMixin"),
            "injectors",Map.of("defaultRequire",1))));
        Files.writeString(configs.resolve("reload-addon-compat.json"),new Gson().toJson(Map.of(
            "required",true,"minVersion","0.8","compatibilityLevel","JAVA_17",
            "package","com.mafuyu404.taczaddon.mixin","client",List.of("LocalPlayerReloadMixin","LocalPlayerDrawMixin","AimKeyMixin"),
            "injectors",Map.of("defaultRequire",1))));
        for(String name:List.of("minestrike-skins.mixins.json","reload-tweaks-compat.json","reload-addon-compat.json"))Mixins.addConfiguration(name);
        var ctor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();ctor.setAccessible(true);
        var transformer=(IMixinTransformer)ctor.newInstance();
        MixinEnvironment.getDefaultEnvironment().setActiveTransformer(transformer);
        Class.forName("com.llamalad7.mixinextras.MixinExtrasBootstrap").getMethod("init").invoke(null);
        String target="com.tacz.guns.client.gameplay.LocalPlayerReload";var actual=new ClassNode();
        new ClassReader(transformer.transformClassBytes(target,target,resource(target))).accept(actual,0);
        for(String hook:List.of("minestrike$reload","minestrike$cancel","tacztweaks$reload$boltBeforeReload","tacztweaks$doReload$setNoAmmo","$nnn")) {
            boolean present=false;
            for(var method:actual.methods)for(var ins:method.instructions)if(ins instanceof MethodInsnNode call&&call.name.contains(hook))present=true;
            check(present,"Coexisting installed reload hook: "+hook);
        }
        for(var entry:Map.of(
                "com.tacz.guns.client.input.AimKey",List.of("minestrike$intent","onAimPressInject"),
                "com.tacz.guns.client.gameplay.LocalPlayerDraw",List.of("minestrike$draw","modifyDrawTime","modifyPutAwayTime","tacztweaks$resetData")).entrySet()) {
            var joint=new ClassNode();new ClassReader(transformer.transformClassBytes(entry.getKey(),entry.getKey(),resource(entry.getKey()))).accept(joint,0);
            for(String hook:entry.getValue()) {
                boolean found=false;
                for(var method:joint.methods)for(var ins:method.instructions)if(ins instanceof MethodInsnNode call&&call.name.contains(hook))found=true;
                check(found,"Coexisting installed aim/draw hook: "+hook);
            }
        }
        System.out.println("PASS: joint real TaCZ reload/aim-key/draw transformation with installed TaCZ Tweaks and TACZ-addon hooks");
    }
    private static byte[] resource(String name) throws Exception {
        try(var in=ReloadViewTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}
    }
    public static void mixins(IMixinTransformer transformer) throws Exception {
        var targets = Map.of(
            "com.tacz.guns.client.gameplay.LocalPlayerReload",List.of("minestrike$reload","minestrike$cancel"),
            "com.tacz.guns.client.renderer.item.AnimateGeoItemRenderer",List.of("minestrike$camera"),
            "com.tacz.guns.client.renderer.item.GunItemRendererWrapper",List.of("minestrike$camera"),
            "com.tacz.guns.client.event.FirstPersonRenderGunEvent",List.of("minestrike$align"));
        for(var entry:targets.entrySet()) {
            var original=new ClassNode();var actual=new ClassNode();byte[] before=resource(entry.getKey());
            new ClassReader(before).accept(original,0);
            new ClassReader(transformer.transformClassBytes(entry.getKey(),entry.getKey(),before)).accept(actual,0);
            for(String hook:entry.getValue()) {
                int count=0;
                for(var method:actual.methods)for(var ins:method.instructions)if(ins instanceof MethodInsnNode call&&call.name.contains(hook))count++;
                check(count==1,"Exact reload/camera/alignment hook: "+entry.getKey()+" / "+hook+" = "+count);
            }
            for(var method:original.methods) {
                var after=actual.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
                var oldCalls=new ArrayList<String>();var newCalls=new ArrayList<String>();
                for(var ins:method.instructions)if(ins instanceof MethodInsnNode call)oldCalls.add(call.owner+call.name+call.desc);
                for(var ins:after.instructions)if(ins instanceof MethodInsnNode call) {
                    if(call.name.contains("minestrike$align")) {
                        newCalls.add("com/tacz/guns/client/event/FirstPersonRenderGunEventapplyAnimationConstraintTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/model/BedrockGunModel;F)V");
                    } else if(!call.name.contains("minestrike$")&&!call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))newCalls.add(call.owner+call.name+call.desc);
                }
                check(oldCalls.equals(newCalls),"Preserve native reload packets, sounds, animations, camera cleanup and other renderer calls: "+entry.getKey()+"."+method.name);
            }
        }
        System.out.println("PASS: real installed TaCZ reload/cancel, world/hand camera and sight-alignment Mixins; all original calls retained");
    }
}
