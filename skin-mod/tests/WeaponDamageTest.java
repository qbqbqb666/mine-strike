import dev.minestrike.skins.*;
import com.google.gson.*;
import com.tacz.guns.resource.pojo.data.gun.ExtraDamage.DistanceDamagePair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.nio.file.*;
import java.util.*;

public class WeaponDamageTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void near(double a,double b,String message){check(Math.abs(a-b)<1e-4,message+": "+a+" != "+b);}
    public static void main(String[] args)throws Exception {
        if(args.length>0 && !args[0].equals("--no-workbook")) {
            var data=JsonParser.parseString(Files.readString(Path.of(args[0]))).getAsJsonObject();
            check(data.getAsJsonArray("weapons").size()==27,"27 unique workbook weapon IDs");
            for(var item:data.getAsJsonArray("weapons")) {
                var r=item.getAsJsonObject();String id=r.get("id").getAsString();
                double original=r.get("original").getAsDouble(),target=r.get("target").getAsDouble(),pellets=r.get("pellets").getAsDouble();
                near(WeaponDamage.value(id).target(),target,"Exact workbook target "+id);
                near(WeaponDamage.value(id).original(),original,"Exact workbook baseline "+id);
                for(double falloff:new double[]{1,.85,.5,.1}) for(double accessory:new double[]{1,.8,1.2})
                    near(WeaponDamage.scale(id,(float)(original/pellets*falloff*accessory))*pellets,target*falloff*accessory,"Keep per-pellet totals and native falloff/accessory factor "+id);
            }
        }
        for(String id:Arrays.asList(null,"unknown:weapon","mcs2:cs_ak_unknown","superbwarfare:ak_47"))near(WeaponDamage.scale(id,12.5f),12.5,"Unlisted ID unchanged");
        var limbs=new CombatSettings(20,Arrays.asList(null,null,null,null),List.of(4.0,1.0,.7,.7));
        near(limbs.multiplier(1),4,"Head preserved");near(limbs.multiplier(0),1,"Torso preserved");
        near(limbs.multiplier(2),.7,"Legs");near(limbs.multiplier(3),.7,"Arms");
        try(var in=WeaponDamageTest.class.getResourceAsStream("/default-skins.json")) {
            var c=SkinCatalog.JSON.fromJson(new java.io.InputStreamReader(in),SkinCatalog.class);
            for(var g:c.groups)if("GunId".equals(g.tag))for(var s:g.skins)
                check(WeaponDamage.value(s.id).equals(WeaponDamage.value(g.baseIds.get(0))),"F6 skin target and ratio retained: "+s.id);
        }
        near(WeaponDamage.scale("superbwarfare:taser",5),20,"Only initial taser direct damage");
        near(WeaponDamage.scale("superbwarfare:bocek",24),5,"BOCEK retains half-speed damage");
        near(WeaponDamage.scale("item.superbwarfare.bocek",48),10,"Actual Superb projectile description ID");
        near(WeaponDamage.scale("superbwarfare:rpg",300),300,"RPG direct unchanged");
        if(args.length>1)skinBaselines(Path.of(args[1]));
        System.out.println("PASS: workbook weapon targets, global ID mapping, F6 aliases, distance/accessory ratios, shotgun totals, taser direct damage and independent 0.7 limbs");
    }
    private static void skinBaselines(Path jar)throws Exception {
        String prefix="top.shmfun.q7dfba46dc2a53842fd682d68fa7c4e4c.";
        try(var loader=new java.net.URLClassLoader(new java.net.URL[]{jar.toUri().toURL()},WeaponDamageTest.class.getClassLoader()) {
            public java.net.URL getResource(String name){return name.equals("META-INF/gp.seal")?findResource(name):super.getResource(name);}
            protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if(!name.startsWith(prefix))return super.loadClass(name,resolve);
                Class<?> t=findLoadedClass(name);if(t==null)t=findClass(name);if(resolve)resolveClass(t);return t;
            }
        };var zip=new java.util.zip.ZipFile(jar.toFile());var in=WeaponDamageTest.class.getResourceAsStream("/default-skins.json")) {
            var type=loader.loadClass(prefix+"mem.MemZipPackResources");
            var pack=(net.minecraft.server.packs.PackResources)type.getConstructor(String.class,byte[].class).newInstance("damage-baseline",zip.getInputStream(zip.getEntry("assets/mcs2_gunpack/pack_01.zip")).readAllBytes());
            var catalog=SkinCatalog.JSON.fromJson(new java.io.InputStreamReader(in),SkinCatalog.class);
            for(var group:catalog.groups)if("GunId".equals(group.tag)) {
                var ids=new ArrayList<>(group.baseIds);for(var skin:group.skins)ids.add(skin.id);
                for(String id:ids) {
                    var supplier=pack.m_214146_(net.minecraft.server.packs.PackType.SERVER_DATA,new ResourceLocation("mcs2","data/guns/"+id.split(":")[1]+"_data.json"));
                    if(supplier==null) {
                        var index=pack.m_214146_(net.minecraft.server.packs.PackType.SERVER_DATA,new ResourceLocation("mcs2","index/guns/"+id.split(":")[1]+".json"));
                        try(var data=index.m_247737_()) {
                            String reference=JsonParser.parseReader(new java.io.InputStreamReader(data)).getAsJsonObject().get("data").getAsString();
                            String[] location=reference.split(":");
                            supplier=pack.m_214146_(net.minecraft.server.packs.PackType.SERVER_DATA,new ResourceLocation(location[0],"data/guns/"+location[1]+".json"));
                        }
                    }
                    try(var data=supplier.m_247737_()) {
                    var bullet=JsonParser.parseReader(new java.io.InputStreamReader(data)).getAsJsonObject().getAsJsonObject("bullet");
                    double first=bullet.getAsJsonObject("extra_damage").getAsJsonArray("damage_adjust").get(0).getAsJsonObject().get("damage").getAsDouble();
                    near(first,WeaponDamage.value(id).original(),"Actual installed skin baseline: "+id);
                    }
                }
            }
            pack.close();
        }
        System.out.println("PASS: installed encrypted gunpack base/skin close-range damage matches every applied ratio");
    }
    private static byte[] bytes(String name)throws Exception {try(var in=WeaponDamageTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    private static long calls(MethodNode m,String suffix){return Arrays.stream(m.instructions.toArray()).filter(i->i instanceof MethodInsnNode c&&c.name.endsWith(suffix)).count();}
    private static List<String> nativeCalls(MethodNode m) {
        List<String> out=new ArrayList<>();for(var i:m.instructions)if(i instanceof MethodInsnNode c&&!c.name.contains("minestrike$")&&!c.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))out.add(c.owner+"."+c.name+c.desc);return out;
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        for(String name:List.of("com.tacz.guns.entity.EntityKineticBullet","com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity","com.atsuishio.superbwarfare.entity.projectile.TaserBulletEntity")) {
            ClassNode before=new ClassNode(),after=new ClassNode();byte[] original=bytes(name);
            new ClassReader(original).accept(before,0);new ClassReader(transformer.transformClassBytes(name,name,original)).accept(after,0);
            String target=name.contains("tacz")?"getDamage":name.endsWith("TaserBulletEntity")?"m_5790_":"performOnHit";
            MethodNode a=before.methods.stream().filter(m->m.name.equals(target)).findFirst().orElseThrow(),b=after.methods.stream().filter(m->m.name.equals(target)).findFirst().orElseThrow();
            check(calls(b,"minestrike$weaponDamage")==1,"Exactly one global scaling hook: "+name);
            check(nativeCalls(a).equals(nativeCalls(b)),"Preserve native damage/events/knockback calls: "+name);
            if(name.contains("tacz"))runNativeCurve(after);
            if(name.endsWith("ProjectileEntity")&&!name.endsWith("TaserBulletEntity")) {
                runSuperbHook(after);
                var e=before.methods.stream().filter(m->m.name.equals("explosionBullet")).findFirst().orElseThrow();
                var f=after.methods.stream().filter(m->m.name.equals("explosionBullet")).findFirst().orElseThrow();
                check(nativeCalls(e).equals(nativeCalls(f))&&calls(f,"minestrike$weaponDamage")==0,"Explosions must remain unscaled");
            }
        }
        System.out.println("PASS: real TaCZ/Superb/taser Mixin transforms, one scaling hook, native event/knockback calls and explosion pipeline retained");
    }
    private static void runSuperbHook(ClassNode transformed)throws Exception {
        String name="WeaponDamageSuperbHook";
        ClassNode harness=new ClassNode();harness.version=Opcodes.V17;harness.access=Opcodes.ACC_PUBLIC;harness.name=name;harness.superName="java/lang/Object";
        harness.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,"gunItemId","Ljava/lang/String;",null,null));
        var hook=transformed.methods.stream().filter(m->m.name.endsWith("minestrike$weaponDamage")).findFirst().orElseThrow();
        var copy=new MethodNode(Opcodes.ASM9,Opcodes.ACC_PUBLIC,hook.name,hook.desc,null,null);hook.accept(copy);copy.access=Opcodes.ACC_PUBLIC;
        for(var i:copy.instructions)if(i instanceof FieldInsnNode f&&f.owner.equals(transformed.name))f.owner=name;
        harness.methods.add(copy);
        MethodNode ctor=new MethodNode(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new InsnNode(Opcodes.RETURN));harness.methods.add(ctor);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);harness.accept(writer);byte[] code=writer.toByteArray();
        Class<?> type=new ClassLoader(WeaponDamageTest.class.getClassLoader()){Class<?> define(){return defineClass(name,code,0,code.length);}}.define();Object bullet=type.getConstructor().newInstance();
        for(String id:List.of("item.superbwarfare.bocek","superbwarfare:bocek","item.superbwarfare.ak_47")) {
            type.getField("gunItemId").set(bullet,id);
            for(float input:new float[]{48,24,0})near((float)type.getMethod(hook.name,float.class).invoke(bullet,input),id.contains("bocek")?input*10/48:input,"Executed Superb global hook with actual stored gun ID");
        }
        var setter=transformed.methods.stream().filter(m->m.name.equals("setGunItemId")&&m.desc.startsWith("(Lnet/minecraft/world/item/ItemStack;")).findFirst().orElseThrow();
        check(calls(setter,"m_41778_")==1,"Verify installed Superb stores item description ID");
        System.out.println("PASS: executed actual Superb scaling hook for description/registry IDs, velocity loss and unknown weapons");
    }
    private static void runNativeCurve(ClassNode transformed)throws Exception {
        // Run the transformed native distance algorithm, without constructing a Forge entity/world.
        // Only the owner/property lookup is stubbed; its numeric result is supplied via damageModifier.
        String original=transformed.name,name="WeaponDamageNativeCurve";
        ClassNode harness=new ClassNode();harness.version=Opcodes.V17;harness.access=Opcodes.ACC_PUBLIC;harness.name=name;harness.superName="java/lang/Object";
        for(var f:transformed.fields)if(Set.of("gunId","damageAmount","distanceAmount","damageModifier","shotDamageMultiplier","startPos").contains(f.name))harness.fields.add(new FieldNode(Opcodes.ACC_PUBLIC,f.name,f.desc,null,null));
        for(var m:transformed.methods)if(m.name.equals("getDamage")||m.name.endsWith("minestrike$weaponDamage")) {
            var copy=new MethodNode(Opcodes.ASM9,m.access,m.name,m.desc,null,null);m.accept(copy);
            for(var i:copy.instructions.toArray()) {
                if(i instanceof FieldInsnNode f&&f.owner.equals(original))f.owner=name;
                if(i instanceof MethodInsnNode c&&c.owner.equals(original))c.owner=name;
                if(i instanceof FieldInsnNode f&&f.owner.equals("com/tacz/guns/api/GunProperties"))copy.instructions.set(i,new InsnNode(Opcodes.ACONST_NULL));
            }
            harness.methods.add(copy);
        }
        MethodNode stub=new MethodNode(Opcodes.ACC_PRIVATE,"modifyProperty","(Lcom/tacz/guns/api/GunProperty;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;",null,null);
        stub.instructions.add(new VarInsnNode(Opcodes.ALOAD,3));stub.instructions.add(new InsnNode(Opcodes.ARETURN));harness.methods.add(stub);
        MethodNode ctor=new MethodNode(Opcodes.ACC_PUBLIC,"<init>","()V",null,null);
        ctor.instructions.add(new VarInsnNode(Opcodes.ALOAD,0));ctor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,"java/lang/Object","<init>","()V",false));ctor.instructions.add(new InsnNode(Opcodes.RETURN));harness.methods.add(ctor);
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);harness.accept(writer);byte[] code=writer.toByteArray();
        Class<?> type=new ClassLoader(WeaponDamageTest.class.getClassLoader()){Class<?> define(){return defineClass(name,code,0,code.length);}}.define();
        Object bullet=type.getConstructor().newInstance();
        var curve=new LinkedList<DistanceDamagePair>(List.of(new DistanceDamagePair(10,7.2f),new DistanceDamagePair(30,6),new DistanceDamagePair(Float.MAX_VALUE,3.6f)));
        type.getField("damageAmount").set(bullet,curve);type.getField("distanceAmount").setFloat(bullet,10);type.getField("damageModifier").setFloat(bullet,.8f);type.getField("shotDamageMultiplier").setFloat(bullet,.5f);type.getField("startPos").set(bullet,Vec3.f_82478_);
        for(String id:List.of("mcs2:cs_deagle","unknown:gun")) {
            type.getField("gunId").set(bullet,new ResourceLocation(id));
            double ratio=id.startsWith("unknown")?1:10/7.2;
            for(double distance:new double[]{0,9.99,10,29.99,30,100}) {
                double raw=distance<10?7.2:distance<30?6:3.6;
                double actual=(float)type.getMethod("getDamage",Vec3.class).invoke(bullet,new Vec3(distance,0,0));
                near(actual,raw*.8*.5*ratio,"Native transformed curve boundary "+id+" at "+distance);
            }
        }
        System.out.println("PASS: executed transformed native TaCZ getDamage at distance boundaries with attachment and shot factors, mapped and unknown IDs");
    }
}
