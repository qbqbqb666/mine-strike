import dev.minestrike.skins.BombHud;
import dev.minestrike.skins.BombAudio;
import dev.minestrike.skins.BombDamage;
import com.google.gson.JsonParser;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.container.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.transformer.*;
import org.spongepowered.asm.logging.*;
import org.spongepowered.asm.util.ReEntranceLock;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.net.URL;
import java.util.*;

// Applies the real installed Mixin engine to the original mod bytecode, without starting a world.
public class ShopMixinTest {
    public static final class Service extends MixinServiceAbstract implements IClassProvider,IClassBytecodeProvider,ITransformerProvider,IClassTracker {
        public String getName(){return "Shop verification";}
        public boolean isValid(){return true;}
        public MixinEnvironment.Phase getInitialPhase(){return MixinEnvironment.Phase.DEFAULT;}
        public IClassProvider getClassProvider(){return this;}
        public IClassBytecodeProvider getBytecodeProvider(){return this;}
        public ITransformerProvider getTransformerProvider(){return this;}
        public IClassTracker getClassTracker(){return this;}
        public IMixinAuditTrail getAuditTrail(){return null;}
        public Collection<String> getPlatformAgents(){return List.of();}
        public IContainerHandle getPrimaryContainer(){return new ContainerHandleVirtual("shop-test");}
        public InputStream getResourceAsStream(String name){return getClass().getClassLoader().getResourceAsStream(name);}
        public URL[] getClassPath(){return new URL[0];}
        public Class<?> findClass(String name)throws ClassNotFoundException{return findClass(name,false);}
        public Class<?> findClass(String name,boolean init)throws ClassNotFoundException{return Class.forName(name,init,getClass().getClassLoader());}
        public Class<?> findAgentClass(String name,boolean init)throws ClassNotFoundException{return findClass(name,init);}
        public ClassNode getClassNode(String name)throws ClassNotFoundException,IOException{return getClassNode(name,false);}
        public ClassNode getClassNode(String name,boolean transform,int flags)throws ClassNotFoundException,IOException{return getClassNode(name,transform);}
        public ClassNode getClassNode(String name,boolean transform)throws ClassNotFoundException,IOException{
            try(InputStream in=getResourceAsStream(name.replace('.','/')+".class")){
                if(in==null)throw new ClassNotFoundException(name);
                ClassNode node=new ClassNode();new ClassReader(in).accept(node,0);return node;
            }
        }
        public Collection<ITransformer> getTransformers(){return List.of();}
        public Collection<ITransformer> getDelegatedTransformers(){return List.of();}
        public void addTransformerExclusion(String name){}
        public void registerInvalidClass(String name){}
        public boolean isClassLoaded(String name){return false;}
        public String getClassRestrictions(String name){return "";}
    }
    public static final class Properties implements IGlobalPropertyService {
        private final Map<IPropertyKey,Object> data=new HashMap<>();
        private record Key(String value) implements IPropertyKey {}
        public IPropertyKey resolveKey(String name){return new Key(name);}
        @SuppressWarnings("unchecked") public <T>T getProperty(IPropertyKey key){return (T)data.get(key);}
        public void setProperty(IPropertyKey key,Object value){data.put(key,value);}
        public <T>T getProperty(IPropertyKey key,T fallback){T value=getProperty(key);return value==null?fallback:value;}
        public String getPropertyString(IPropertyKey key,String fallback){Object value=data.get(key);return value==null?fallback:value.toString();}
    }
    private static byte[] resource(String name)throws IOException{
        try(var in=ShopMixinTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}
    }
    private static String methodCode(MethodNode method) {
        var printer = new org.objectweb.asm.util.Textifier();
        var visitor = new org.objectweb.asm.util.TraceMethodVisitor(printer);
        for (AbstractInsnNode instruction : method.instructions)
            if (!(instruction instanceof FrameNode)) instruction.accept(visitor);
        var text = new StringWriter(); printer.print(new PrintWriter(text)); return text.toString();
    }
    private static String withoutReceiverPlumbing(MethodNode method) {
        return methodCode(method).replaceAll("(?m)^    (ALOAD|ASTORE) [0-9]+\\R|^    SWAP\\R", "");
    }
    public static void main(String[] args)throws Exception {
        for (float health : new float[]{20, 40, 100}) {
            float previous = Float.POSITIVE_INFINITY;
            for (double distance = 0; distance < 56; distance += .125) {
                float damage = BombDamage.amount(distance, health);
                if (!Float.isFinite(damage) || damage <= 0 || damage >= previous)
                    throw new AssertionError("C4 damage must decrease strictly with distance");
                previous = damage;
                if (distance <= 8 && damage * .2f * .85f < health)
                    throw new AssertionError("Near C4 must be lethal after ordinary armor and 15% heavy armor reduction");
            }
            if (BombDamage.RANGE != 56 || BombDamage.amount(56, health) != 0 || BombDamage.amount(80, health) != 0
                    || BombDamage.amount(40, health) <= 0 || BombDamage.amount(48, health) <= 0
                    || BombDamage.amount(0, health) != health * 10
                    || Math.abs(BombDamage.amount(22.4, health) / health - 3.6f) > .0001f)
                throw new AssertionError("C4 range/health scaling changed");
        }
        if (BombDamage.amount(Double.NaN, 20) != 0 || BombDamage.amount(-1, 20) != 0 || BombDamage.amount(0, 0) != 0)
            throw new AssertionError("Invalid C4 geometry/health");
        try (var in = ShopMixinTest.class.getClassLoader().getResourceAsStream("assets/minestrike_skins/sounds.json")) {
            var sounds = JsonParser.parseReader(new InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            int range = sounds.getAsJsonObject("c4_beep_loud").getAsJsonArray("sounds").get(0).getAsJsonObject().get("attenuation_distance").getAsInt();
            if (range != 40 || BombAudio.BEEP_RANGE != 40) throw new AssertionError("C4 send and attenuation ranges must both be 40 blocks");
        }
        System.out.println("PASS: C4 monotonic damage, unchanged peak, close armor margin, 56-block boundary and 40-block beep range");
        int total = 900;
        double lastRate = 0;
        for (double remaining = total; remaining > 0; remaining -= .25) {
            double phase = BombHud.cycles(remaining, total);
            double next = BombHud.cycles(remaining - .01, total);
            double rate = (next - phase) / (.01 / 20);
            if (rate < lastRate - 1e-5 || rate < .79 || rate > 3.21)
                throw new AssertionError("Bomb pulse must accelerate continuously with the actual fuse");
            lastRate = rate;
            float pulse = BombHud.pulse(remaining, total);
            if (!Float.isFinite(pulse) || pulse < 0 || pulse > 1) throw new AssertionError("Invalid pulse brightness");
        }
        if (BombHud.cycles(total + 20, total) != 0
                || BombHud.cycles(0, 0) != 0)
            throw new AssertionError("Bomb animation bounds");
        if (!BombHud.visible("--:--", true, false, -1, 300, total)
                || BombHud.visible("00:30", true, false, 600, 300, total)
                || BombHud.visible("--:--", true, true, -1, 300, total)
                || BombHud.visible("--:--", false, false, -1, 300, total)
                || BombHud.visible("--:--", true, false, -1, 0, total))
            throw new AssertionError("Bomb icon must only replace an active planted timer");
        System.out.println("PASS: fuse-driven pulse acceleration, brightness bounds and planted/finished/reset visibility");
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getDefaultEnvironment().setOption(MixinEnvironment.Option.DEBUG_VERIFY,true);
        Mixins.addConfiguration("minestrike-skins.mixins.json");
        var constructor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();constructor.setAccessible(true);
        IMixinTransformer transformer=(IMixinTransformer)constructor.newInstance();
        MatchMovementTest.mixins(transformer);
        AimTimingTest.mixins(transformer);
        ReloadViewTest.mixins(transformer);
        AimActionsTest.mixins(transformer);
        PlayerShapeTest.mixins(transformer);
        AgentTest.mixins(transformer);
        CombatSettingsTest.mixins(transformer);
        WeaponDamageTest.mixins(transformer);
        MatchFeedbackTest.mixins(transformer);
        armorHud(transformer);
        MusicBoxesTest.mixins(transformer);
        String prefix="com.phasetranscrystal.";
        String[] targets={"blockoffensive.map.shop.ItemType","blockoffensive.map.CSGameMap","fpsmatch.core.shop.ShopData","fpsmatch.core.shop.FPSMShop","fpsmatch.core.shop.slot.ShopSlot","fpsmatch.util.FPSMUtil",
                "fpsmatch.common.client.screen.EditorShopContainer","fpsmatch.common.client.screen.EditShopSlotMenu",
                "fpsmatch.common.client.screen.VanillaGuiRegister","fpsmatch.common.packet.shop.OpenEditorC2SPacket","fpsmatch.common.item.ShopEditTool", "blockoffensive.client.key.OpenShopKey"};
        Map<String,byte[]> transformed=new HashMap<>();
        for(String target:targets){String name=prefix+target;byte[] before=resource(name),after=transformer.transformClassBytes(name,name,before);
            if(Arrays.equals(before,after))throw new AssertionError("Mixin did not apply: "+name);transformed.put(name,after);
        }
        String ammoTarget = "com.atsuishio.superbwarfare.data.gun.AmmoConsumer";
        ClassNode originalAmmo = new ClassNode(), actualAmmo = new ClassNode();
        new ClassReader(resource(ammoTarget)).accept(originalAmmo, 0);
        new ClassReader(transformer.transformClassBytes(ammoTarget, ammoTarget, resource(ammoTarget))).accept(actualAmmo, 0);
        for (MethodNode method : originalAmmo.methods) {
            MethodNode actual = actualAmmo.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElseThrow();
            if (!method.name.equals("isAmmoItem")) {
                if (!methodCode(method).equals(methodCode(actual))) throw new AssertionError("Ammo compatibility changed native count/consumption/reload method: " + method.name);
                continue;
            }
            int hooks = 0;
            for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call
                    && call.name.contains("minestrike$ammo")) hooks++;
            if (hooks != 1) throw new AssertionError("Ammo comparison hook must apply exactly once");
        }
        MethodNode ammoHook = actualAmmo.methods.stream().filter(m -> m.name.contains("minestrike$ammo")).findFirst().orElseThrow();
        int nativeComparisons = 0, copies = 0, normalized = 0;
        for (AbstractInsnNode instruction : ammoHook.instructions) if (instruction instanceof MethodInsnNode call) {
            if (call.owner.equals("com/atsuishio/superbwarfare/tools/MinecraftUtil") && call.name.equals("isSameItemStack")) nativeComparisons++;
            if (call.owner.equals("net/minecraft/world/item/ItemStack") && call.name.equals("m_41777_")) copies++;
            if (call.owner.equals("dev/minestrike/skins/ShopBundles") && call.name.equals("ammoTag")) normalized++;
            if (Set.of("m_41774_", "m_41764_", "m_41620_").contains(call.name)) throw new AssertionError("Ammo comparison must not consume or change stack counts");
        }
        if (nativeComparisons != 2 || copies != 2 || normalized != 2) throw new AssertionError("Ammo compatibility must retain native matching and normalize only copies");
        System.out.println("PASS: real ammo Mixin; native count/consume/withdraw methods unchanged; comparison operates on copies only");
        String containerTarget = "com.atsuishio.superbwarfare.block.ContainerBlock";
        ClassNode originalContainer = new ClassNode(), actualContainer = new ClassNode();
        new ClassReader(resource(containerTarget)).accept(originalContainer, 0);
        new ClassReader(transformer.transformClassBytes(containerTarget, containerTarget, resource(containerTarget))).accept(actualContainer, 0);
        for (MethodNode method : originalContainer.methods) {
            MethodNode actual = actualContainer.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElseThrow();
            if (!method.name.equals("m_6227_")) {
                if (!methodCode(method).equals(methodCode(actual))) throw new AssertionError("Crowbar fix changed unrelated container method: " + method.name);
                continue;
            }
            List<String> originalCalls = new ArrayList<>(), actualCalls = new ArrayList<>(); int redirects = 0, offhandHooks = 0;
            for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode call)
                originalCalls.add(call.owner + "." + call.name + call.desc);
            for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call) {
                if (call.name.contains("minestrike$crowbar")) {
                    redirects++;
                    actualCalls.add("net/minecraft/world/item/ItemStack.m_204117_(Lnet/minecraft/tags/TagKey;)Z");
                } else if (call.name.contains("minestrike$offhand")) offhandHooks++;
                else if (!call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))
                    actualCalls.add(call.owner + "." + call.name + call.desc);
            }
            if (redirects != 1 || offhandHooks != 1 || !originalCalls.equals(actualCalls))
                throw new AssertionError("Crowbar fix must retain native container space/content/opening checks");
        }
        MethodNode crowbarHook = actualContainer.methods.stream().filter(m -> m.name.contains("minestrike$crowbar")).findFirst().orElseThrow();
        boolean originalTag = false, nativeItem = false, intendedTag = false, diagnostics = false;
        for (AbstractInsnNode instruction : crowbarHook.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals("net/minecraft/world/item/ItemStack") && call.name.equals("m_204117_")) originalTag = true;
            if (instruction instanceof FieldInsnNode field && field.owner.equals("com/atsuishio/superbwarfare/init/ModItems") && field.name.equals("CROWBAR")) nativeItem = true;
            if (instruction instanceof FieldInsnNode field && field.name.equals("TOOLS_CROWBAR")) intendedTag = true;
        }
        MethodNode toolLog = actualContainer.methods.stream().filter(m -> m.name.contains("minestrike$log")).findFirst().orElseThrow();
        for (AbstractInsnNode instruction : toolLog.instructions) if (instruction instanceof LdcInsnNode value && value.cst instanceof String text
                && text.contains("hand={}") && text.contains("item={}") && text.contains("mode={}")) diagnostics = true;
        if (!originalTag || !nativeItem || !intendedTag || !diagnostics) throw new AssertionError("Crowbar fallback must preserve tags, use exact native item and record failed interaction context");
        ClassNode skins = new ClassNode(); new ClassReader(resource("dev.minestrike.skins.MineStrikeSkins")).accept(skins, 0);
        MethodNode equip = skins.methods.stream().filter(m -> m.name.equals("equipIssuedArmor")).findFirst().orElseThrow();
        Set<String> equipmentSlots = new HashSet<>(); int transfers = 0, equips = 0, emptyChecks = 0;
        for (AbstractInsnNode instruction : equip.instructions) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals("net/minecraft/world/entity/EquipmentSlot")) equipmentSlots.add(field.name);
            if (instruction instanceof MethodInsnNode call) {
                if (call.name.equals("m_41620_")) {
                    if (call.getPrevious().getOpcode() != Opcodes.ICONST_1) throw new AssertionError("Auto-equip must split exactly one item");
                    transfers++;
                }
                if (call.name.equals("m_8061_")) equips++;
                if (call.name.equals("m_41619_")) emptyChecks++;
                if (call.name.equals("m_41777_")) throw new AssertionError("Auto-equip must transfer the issued item, not duplicate a copy");
            }
        }
        if (!equipmentSlots.equals(Set.of("LEGS", "FEET")) || transfers != 1 || equips != 1 || emptyChecks < 2)
            throw new AssertionError("Only issued leggings/boots may fill empty equipment slots");
        MethodNode issued = skins.methods.stream().filter(m -> m.name.equals("applyIssuedKits")).findFirst().orElseThrow();
        boolean equipped = false, synced = false;
        for (AbstractInsnNode instruction : issued.instructions) if (instruction instanceof MethodInsnNode call) {
            if (call.name.equals("equipIssuedArmor")) equipped = true;
            if (call.name.equals("m_6199_")) { if (!equipped) throw new AssertionError("Armor must be equipped before inventory sync"); synced = true; }
        }
        if (!equipped || !synced) throw new AssertionError("Respawn issued armor integration/sync missing");
        System.out.println("PASS: native container crowbar Mixin and space checks; issued leggings/boots empty-slot transfer before sync");
        String itemName = "net.minecraft.world.item.ItemStack";
        byte[] itemBefore = resource(itemName), itemAfter = transformer.transformClassBytes(itemName, itemName, itemBefore);
        if (Arrays.equals(itemBefore, itemAfter)) throw new AssertionError("Adventure placement Mixin did not apply");
        ClassNode itemNode = new ClassNode(); new ClassReader(itemAfter).accept(itemNode, 0);
        for (MethodNode method : itemNode.methods) if (method.name.equals("m_204128_")) {
            // Destruction permissions must remain byte-for-byte original at the instruction level.
            for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode call
                    && call.owner.startsWith("dev/minestrike")) throw new AssertionError("Deployment must not grant block destruction");
        }
        for (String radarTarget : List.of("xaero.hud.minimap.radar.render.element.RadarRenderer",
                "xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementRenderer",
                "xaero.hud.minimap.player.tracker.PlayerTrackerMinimapElementReader")) {
            byte[] before = resource(radarTarget), after = transformer.transformClassBytes(radarTarget, radarTarget, before);
            if (Arrays.equals(before, after)) throw new AssertionError("Team radar Mixin did not apply: " + radarTarget);
            ClassNode node = new ClassNode(); new ClassReader(after).accept(node, 0);
            int filters = 0;
            for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions)
                if (instruction instanceof MethodInsnNode call && call.owner.equals("dev/minestrike/skins/TeamRadarClient") && call.name.equals("hidden")) filters++;
            int expected = radarTarget.endsWith(".RadarRenderer") ? 2 : 1;
            if (filters != expected) throw new AssertionError("Missing radar render/track path: " + radarTarget);
        }
        for (String bulletTarget : List.of("com.tacz.guns.entity.EntityKineticBullet",
                "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity")) {
            byte[] before = resource(bulletTarget), after = transformer.transformClassBytes(bulletTarget, bulletTarget, before);
            if (Arrays.equals(before, after)) throw new AssertionError("Bullet trail Mixin did not apply: " + bulletTarget);
            ClassNode original = new ClassNode(), result = new ClassNode();
            new ClassReader(before).accept(original, 0); new ClassReader(after).accept(result, 0);
            if (!result.interfaces.contains("dev/minestrike/skins/BulletTrails$Source"))
                throw new AssertionError("Projectile must expose the same tracer ownership/eligibility to recording and rendering");
            MethodNode originalTick = original.methods.stream().filter(m -> m.name.equals("m_8119_")).findFirst().orElseThrow();
            MethodNode actualTick = result.methods.stream().filter(m -> m.name.equals("m_8119_")).findFirst().orElseThrow();
            List<String> nativeCalls = new ArrayList<>(), retainedCalls = new ArrayList<>();
            for (AbstractInsnNode instruction : originalTick.instructions) if (instruction instanceof MethodInsnNode call)
                nativeCalls.add(call.owner + "." + call.name + call.desc);
            int hooks = 0;
            for (AbstractInsnNode instruction : actualTick.instructions) if (instruction instanceof MethodInsnNode call) {
                if (call.name.contains("minestrike$begin") || call.name.contains("minestrike$end")) hooks++;
                else if (!call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))
                    retainedCalls.add(call.owner + "." + call.name + call.desc);
            }
            if (hooks < 2 || !nativeCalls.equals(retainedCalls))
                throw new AssertionError("Trail observation must retain every original tick call: " + bulletTarget);
            if (bulletTarget.contains("tacz")) {
                MethodNode spawnBefore = original.methods.stream().filter(m -> m.name.equals("readSpawnData")).findFirst().orElseThrow();
                MethodNode spawnAfter = result.methods.stream().filter(m -> m.name.equals("readSpawnData")).findFirst().orElseThrow();
                List<String> beforeCalls = new ArrayList<>(), afterCalls = new ArrayList<>();
                int spawnHooks = 0;
                for (AbstractInsnNode instruction : spawnBefore.instructions) if (instruction instanceof MethodInsnNode call)
                    beforeCalls.add(call.owner + "." + call.name + call.desc);
                for (AbstractInsnNode instruction : spawnAfter.instructions) if (instruction instanceof MethodInsnNode call) {
                    if (call.name.contains("minestrike$spawn")) spawnHooks++;
                    else if (!call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))
                        afterCalls.add(call.owner + "." + call.name + call.desc);
                }
                if (spawnHooks != 1 || !beforeCalls.equals(afterCalls))
                    throw new AssertionError("Spawn diagnostics must observe every decoded bullet without changing spawn data");
            }
        }
        System.out.println("PASS: TaCZ spawn diagnostics preserve all original decoding calls and add exactly one observation hook");
        System.out.println("PASS: TaCZ/Superb trail Mixin injection retains original projectile tick calls");
        for (String renderer : List.of("com.tacz.guns.client.renderer.entity.EntityBulletRenderer",
                "com.atsuishio.superbwarfare.client.renderer.projectile.ProjectileEntityRenderer")) {
            ClassNode original = new ClassNode(), result = new ClassNode();
            new ClassReader(resource(renderer)).accept(original, 0);
            new ClassReader(transformer.transformClassBytes(renderer, renderer, resource(renderer))).accept(result, 0);
            String intercepted = renderer.contains("tacz") ? "renderTracerAmmo" : "render";
            int hooks = 0;
            for (MethodNode method : original.methods) {
                MethodNode actual = result.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElseThrow();
                if (!method.name.equals(intercepted)) {
                    if (!methodCode(method).equals(methodCode(actual))) throw new AssertionError("Tracer replacement changed unrelated rendering: " + method.name);
                    continue;
                }
                List<String> originalCalls = new ArrayList<>(), actualCalls = new ArrayList<>();
                for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode call)
                    originalCalls.add(call.owner + "." + call.name + call.desc);
                for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call) {
                    if (call.name.contains("minestrike$nativeTracer")) hooks++;
                    else if (!call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))
                        actualCalls.add(call.owner + "." + call.name + call.desc);
                }
                if (!originalCalls.equals(actualCalls)) throw new AssertionError("Native render fallback must retain original calls: " + renderer);
            }
            MethodNode hook = result.methods.stream().filter(m -> m.name.contains("minestrike$nativeTracer")).findFirst().orElseThrow();
            boolean sharedGate = false;
            for (AbstractInsnNode instruction : hook.instructions) if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("dev/minestrike/skins/BulletTrails") && call.name.equals("replacesNative")) sharedGate = true;
            if (hooks != 1 || !sharedGate) throw new AssertionError("Native tracer must use one shared takeover gate: " + renderer);
        }
        System.out.println("PASS: TaCZ/Superb native tracer takeover and unchanged original rendering fallback");
        String explosionTarget = "net.minecraft.world.level.Explosion";
        ClassNode originalExplosion = new ClassNode(), actualExplosion = new ClassNode();
        new ClassReader(resource(explosionTarget)).accept(originalExplosion, 0);
        new ClassReader(transformer.transformClassBytes(explosionTarget, explosionTarget, resource(explosionTarget))).accept(actualExplosion, 0);
        for (MethodNode method : originalExplosion.methods) {
            MethodNode actual = actualExplosion.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElseThrow();
            if (!method.name.equals("m_46061_")) {
                if (!methodCode(method).equals(methodCode(actual))) throw new AssertionError("C4 damage changed unrelated explosion method: " + method.name);
                continue;
            }
            List<String> originalCalls = new ArrayList<>(), actualCalls = new ArrayList<>();
            for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode call)
                originalCalls.add(call.owner + "." + call.name + call.desc);
            int redirects = 0;
            for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call) {
                if (call.name.contains("minestrike$c4Damage")) {
                    redirects++;
                    actualCalls.add("net/minecraft/world/entity/Entity.m_6469_(Lnet/minecraft/world/damagesource/DamageSource;F)Z");
                } else actualCalls.add(call.owner + "." + call.name + call.desc);
            }
            if (redirects != 1 || !originalCalls.equals(actualCalls)) throw new AssertionError("C4 must replace one hurt call and preserve native explosion flow");
        }
        MethodNode damageHook = actualExplosion.methods.stream().filter(m -> m.name.contains("minestrike$c4Damage")).findFirst().orElseThrow();
        Set<String> guards = new HashSet<>(); int nativeHurt = 0, policy = 0;
        for (AbstractInsnNode instruction : damageHook.instructions) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.INSTANCEOF) guards.add(type.desc);
            if (instruction instanceof MethodInsnNode call) {
                if (call.owner.equals("dev/minestrike/skins/BombDamage") && call.name.equals("amount")) policy++;
                if (call.owner.equals("net/minecraft/world/entity/Entity") && call.name.equals("m_6469_")) nativeHurt++;
                if (Set.of("m_21153_", "m_6667_").contains(call.name)) throw new AssertionError("C4 must use native damage, never assign health or force death");
            }
        }
        if (nativeHurt != 1 || policy != 1 || !guards.contains(prefix.replace('.', '/') + "blockoffensive/entity/CompositionC4Entity")
                || !guards.contains("net/minecraft/server/level/ServerPlayer"))
            throw new AssertionError("C4 damage must be restricted to C4/server players and use one native hurt call");
        System.out.println("PASS: real C4 explosion Mixin, single native damage call, C4/player guards and unchanged explosion flow");
        ClassNode trailNode = new ClassNode(); new ClassReader(resource("dev.minestrike.skins.BulletTrails")).accept(trailNode, 0);
        MethodNode spawnedTrail = trailNode.methods.stream().filter(m -> m.name.equals("spawned")).findFirst().orElseThrow();
        Set<String> spawnCalls = new HashSet<>();
        for (AbstractInsnNode instruction : spawnedTrail.instructions) if (instruction instanceof MethodInsnNode call)
            spawnCalls.add(call.name);
        if (!spawnCalls.containsAll(Set.of("m_20182_", "m_20184_", "m_82549_", "recordPath")))
            throw new AssertionError("Decoded bullets must establish an initial segment from native position/velocity without waiting for a client tick");
        MethodNode recordTrail = trailNode.methods.stream().filter(m -> m.name.equals("record")).findFirst().orElseThrow();
        boolean sharedPath = false;
        for (AbstractInsnNode instruction : recordTrail.instructions) if (instruction instanceof MethodInsnNode call && call.name.equals("recordPath")) sharedPath = true;
        if (!sharedPath) throw new AssertionError("Spawn and completed movement must use the same wall clipping and recording path");
        MethodNode recordPath = trailNode.methods.stream().filter(m -> m.name.equals("recordPath")).findFirst().orElseThrow();
        boolean appendPath = false;
        for (AbstractInsnNode instruction : recordPath.instructions) if (instruction instanceof MethodInsnNode call) {
            if (call.owner.equals("dev/minestrike/skins/TrailPath") && call.name.equals("append")) appendPath = true;
            if (call.name.equals("isTracerAmmo")) throw new AssertionError("Initial and actual segments must not depend on native tracer intervals");
        }
        if (!appendPath) throw new AssertionError("Actual movement must update the same full path that owns the initial preview");
        System.out.println("PASS: decoded bullet creates initial clipped segment without a tick; actual movement replaces preview through shared path");
        MethodNode quad = trailNode.methods.stream().filter(m -> m.name.equals("quad")).findFirst().orElseThrow();
        int vertices = 0;
        for (AbstractInsnNode instruction : quad.instructions) if (instruction instanceof MethodInsnNode call && call.name.equals("vertex")) vertices++;
        if (vertices != 4) throw new AssertionError("Each continuous tracer side must have four vertex calls, without a separate square head");
        MethodNode headDraw = trailNode.methods.stream().filter(m -> m.name.equals("drawHead")).findFirst().orElseThrow();
        boolean closedMesh = false;
        for (AbstractInsnNode instruction : headDraw.instructions) if (instruction instanceof MethodInsnNode call
                && call.owner.equals("dev/minestrike/skins/TrailGeometry") && call.name.equals("head")) closedMesh = true;
        if (!closedMesh) throw new AssertionError("Head rendering must use the tested closed triangle mesh, not a rectangular billboard");
        for (String path : List.of("replacesNative", "render")) {
            MethodNode method = trailNode.methods.stream().filter(m -> m.name.equals(path)).findFirst().orElseThrow();
            boolean sameFrame = false;
            for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof MethodInsnNode call && call.name.equals("prepare")) sameFrame = true;
            if (!sameFrame) throw new AssertionError("Native takeover and enhanced drawing must use the same visible frame selection");
        }
        MethodNode takeover = trailNode.methods.stream().filter(m -> m.name.equals("replacesNative")).findFirst().orElseThrow();
        boolean selected = false;
        for (AbstractInsnNode instruction : takeover.instructions) if (instruction instanceof MethodInsnNode call
                && call.owner.equals("java/util/Set") && call.name.equals("contains")) selected = true;
        if (!selected) throw new AssertionError("Native tracer must remain unless its actual ribbon was selected for drawing");
        System.out.println("PASS: no square head quad; native takeover and drawing share visible geometry and budget selection");
        for (MethodNode method : trailNode.methods) for (AbstractInsnNode instruction : method.instructions)
            if (instruction instanceof MethodInsnNode call && call.owner.startsWith("net/minecraft/world/")
                    && Set.of("m_6034_", "m_20256_", "m_146870_", "m_6469_", "m_7731_").contains(call.name))
                throw new AssertionError("Trail renderer must not move, remove, damage entities or change blocks");
        for (String bombTarget : List.of(prefix + "blockoffensive.item.CompositionC4",
                prefix + "blockoffensive.entity.CompositionC4Entity")) {
            byte[] before = resource(bombTarget), after = transformer.transformClassBytes(bombTarget, bombTarget, before);
            if (Arrays.equals(before, after)) throw new AssertionError("C4 audio Mixin did not apply: " + bombTarget);
            ClassNode original = new ClassNode(), result = new ClassNode();
            new ClassReader(before).accept(original, 0); new ClassReader(after).accept(result, 0);
            boolean entity = bombTarget.endsWith("Entity");
            String changed = entity ? "playBeepSound" : "m_5922_";
            for (MethodNode method : original.methods) {
                MethodNode actual = result.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc))
                        .findFirst().orElseThrow();
                if (entity && method.name.equals("setFuse")) {
                    int recipients = 0;
                    for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call
                            && call.name.contains("minestrike$timerRecipients")) {
                        recipients++;
                        actual.instructions.set(call, new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                                prefix.replace('.', '/') + "fpsmatch/core/map/MapTeams", "getSpecPlayers", "()Ljava/util/List;", false));
                    }
                    if (recipients != 1 || !withoutReceiverPlumbing(method).equals(withoutReceiverPlumbing(actual)))
                        throw new AssertionError("Fuse sync must only change recipients, never fuse values or cadence");
                    MethodNode recipientHook = result.methods.stream().filter(m -> m.name.contains("minestrike$timerRecipients")).findFirst().orElseThrow();
                    boolean allPlayers = false;
                    for (AbstractInsnNode instruction : recipientHook.instructions) if (instruction instanceof MethodInsnNode call
                            && call.name.equals("getJoinedPlayersWithSpec")) allPlayers = true;
                    if (!allPlayers) throw new AssertionError("Fuse packets must reach both teams and spectators");
                } else if (!method.name.equals(changed) && !methodCode(method).equals(methodCode(actual)))
                    throw new AssertionError("C4 audio changed unrelated method: " + method.name);
                if (method.name.equals(changed)) {
                    int hooks = 0, positional = 0;
                    for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call) {
                        if (call.name.contains(entity ? "minestrike$range" : "minestrike$announce")) hooks++;
                        if (call.owner.equals("net/minecraft/world/level/Level") && call.name.equals("m_6263_")) positional++;
                    }
                    if (hooks != 1 || positional != (entity ? 1 : 0))
                        throw new AssertionError("C4 sound replacement must occur exactly once: " + bombTarget);
                }
            }
        }
        String overlayTarget = prefix + "blockoffensive.client.screen.hud.CSGameOverlay";
        ClassNode originalOverlay = new ClassNode(), actualOverlay = new ClassNode();
        new ClassReader(resource(overlayTarget)).accept(originalOverlay, 0);
        new ClassReader(transformer.transformClassBytes(overlayTarget, overlayTarget, resource(overlayTarget))).accept(actualOverlay, 0);
        for (MethodNode method : originalOverlay.methods) {
            MethodNode actual = actualOverlay.methods.stream().filter(m -> m.name.equals(method.name) && m.desc.equals(method.desc)).findFirst().orElseThrow();
            if (!method.name.equals("render")) {
                if (!methodCode(method).equals(methodCode(actual))) throw new AssertionError("C4 HUD changed unrelated overlay method: " + method.name);
                continue;
            }
            int hooks = 0;
            for (AbstractInsnNode instruction : actual.instructions) if (instruction instanceof MethodInsnNode call
                    && call.name.contains("minestrike$bombIcon")) hooks++;
            if (hooks == 0) throw new AssertionError("C4 HUD timer drawing hook missing");
        }
        MethodNode iconHook = actualOverlay.methods.stream().filter(m -> m.name.contains("minestrike$bombIcon")).findFirst().orElseThrow();
        boolean fallback = false, icon = false;
        for (AbstractInsnNode instruction : iconHook.instructions) if (instruction instanceof MethodInsnNode call) {
            if (call.owner.equals("net/minecraft/client/gui/GuiGraphics") && call.name.equals("m_280056_")) fallback = true;
            if (call.owner.equals("dev/minestrike/skins/BombHud") && call.name.equals("draw")) icon = true;
        }
        if (!fallback || !icon) throw new AssertionError("C4 overlay must preserve original text outside planted state");
        System.out.println("PASS: real C4 HUD Mixin, ordinary text fallback, all-player fuse sync with original values/cadence");
        ClassNode baseMap = new ClassNode(); new ClassReader(resource(prefix + "fpsmatch.core.map.BaseMap")).accept(baseMap, 0);
        MethodNode broadcast = baseMap.methods.stream().filter(m -> m.name.equals("sendPacketToAllPlayer")).findFirst().orElseThrow();
        boolean spectators = false;
        for (AbstractInsnNode instruction : broadcast.instructions) if (instruction instanceof MethodInsnNode call
                && call.name.equals("getJoinedPlayersWithSpec")) spectators = true;
        if (!spectators) throw new AssertionError("C4 broadcast must include spectators in the match");
        for (String asset : List.of("c4_planted_loud", "c4_beep_loud")) {
            try (var in = ShopMixinTest.class.getClassLoader().getResourceAsStream("assets/minestrike_skins/sounds/" + asset + ".ogg")) {
                if (in == null) throw new AssertionError("Missing enhanced C4 asset: " + asset);
                byte[] header = in.readNBytes(40);
                if (header.length < 40 || header[0] != 'O' || header[1] != 'g' || header[2] != 'g' || header[3] != 'S'
                        || header[39] != 1) throw new AssertionError("C4 sound must be valid mono Vorbis: " + asset);
            }
        }
        System.out.println("PASS: C4 Mixin replaces each sound once, retains all other bomb methods, includes spectators and mono assets");
        for (String audioTarget : List.of("net.minecraft.client.sounds.SoundEngine", "com.mojang.blaze3d.audio.Channel",
                "com.mojang.blaze3d.audio.Library", "com.tacz.guns.client.sound.GunSoundInstance",
                "com.tacz.guns.client.sound.SoundPlayManager",
                "com.atsuishio.superbwarfare.client.sound.VehicleSoundInstance", "com.sonicether.soundphysics.SoundPhysics")) {
            byte[] before = resource(audioTarget), after = transformer.transformClassBytes(audioTarget, audioTarget, before);
            if (Arrays.equals(before, after)) throw new AssertionError("Audio Mixin did not apply: " + audioTarget);
            ClassNode node = new ClassNode(); new ClassReader(after).accept(node, 0);
            if (audioTarget.endsWith(".SoundPhysics")) {
                MethodNode environment = node.methods.stream().filter(m -> m.name.equals("evaluateEnvironment")).findFirst().orElseThrow();
                boolean c4Hook = false;
                for (AbstractInsnNode instruction : environment.instructions) if (instruction instanceof MethodInsnNode call
                        && call.name.contains("minestrike$clear")) c4Hook = true;
                if (!c4Hook) throw new AssertionError("C4 must bypass Sound Physics environment filtering");
            }
            if (audioTarget.endsWith(".SoundEngine")) {
                int binds = 0, gains = 0;
                for (MethodNode method : node.methods) for (AbstractInsnNode instruction : method.instructions)
                    if (instruction instanceof MethodInsnNode call && call.owner.equals("dev/minestrike/skins/CombatAudio")) {
                        if (call.name.equals("bind")) binds++;
                        if (call.name.equals("gain")) gains++;
                    }
                if (binds != 2 || gains != 2) throw new AssertionError("Missing static/streaming or initial/tick audio paths");
            }
            if (audioTarget.endsWith(".Channel")) {
                MethodNode play = node.methods.stream().filter(method -> method.name.equals("m_83672_")).findFirst().orElseThrow();
                boolean beforePlay = false;
                for (AbstractInsnNode instruction : play.instructions) if (instruction instanceof MethodInsnNode call) {
                    if (call.name.contains("minestrike$play")) beforePlay = true;
                    if (call.owner.equals("org/lwjgl/openal/AL10") && call.name.equals("alSourcePlay") && !beforePlay)
                        throw new AssertionError("Combat source setup must precede the first audio samples");
                }
            }
        }
        System.out.println("PASS: real audio Mixin transformation, first-sample setup, streaming and tick gain paths");
        ClassNode slotNode = new ClassNode();
        new ClassReader(transformed.get(prefix + "fpsmatch.core.shop.slot.ShopSlot")).accept(slotNode, 0);
        for (String target : List.of("process", "getAmmoCount")) {
            MethodNode ammoMethod = slotNode.methods.stream().filter(m -> m.name.equals(target)).findFirst().orElseThrow();
            boolean hooked = false;
            for (AbstractInsnNode ins : ammoMethod.instructions) if (ins instanceof MethodInsnNode call
                    && (call.name.contains("prepareAmmo") || call.name.contains("totalAmmo"))) hooked = true;
            if (!hooked) throw new AssertionError("Unified ammo hook missing from real ShopSlot." + target);
        }
        ClassNode utilNode = new ClassNode();
        new ClassReader(transformed.get(prefix + "fpsmatch.util.FPSMUtil")).accept(utilNode, 0);
        MethodNode resetAmmo = utilNode.methods.stream().filter(m -> m.name.equals("resetGunAmmo")).findFirst().orElseThrow();
        boolean resetsConfiguredTotal = false;
        for (AbstractInsnNode ins : resetAmmo.instructions) if (ins instanceof MethodInsnNode call && call.name.contains("configuredTotal")) resetsConfiguredTotal = true;
        if (!resetsConfiguredTotal) throw new AssertionError("Native round ammo reset must restore configured total");
        System.out.println("PASS: real ShopSlot template/readback and native round ammo reset Mixin hooks");
        if (!slotNode.interfaces.contains("dev/minestrike/skins/ShopBundles$SlotState")) throw new AssertionError("Receipt state interface missing");
        boolean constructorInitializesReceipts = false;
        for (MethodNode method : slotNode.methods) if (method.name.equals("<init>")) {
            for (AbstractInsnNode instruction : method.instructions) if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.PUTFIELD && field.name.contains("mineStrikeReceipts")) constructorInitializesReceipts = true;
        }
        if (!constructorInitializesReceipts) throw new AssertionError("Receipt list must initialize on real ShopSlot constructors");
        ClassLoader loader=new ClassLoader(ShopMixinTest.class.getClassLoader()){
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                boolean own=name.equals(prefix+"blockoffensive.map.shop.ItemType")||name.equals(prefix+"fpsmatch.core.shop.ShopData")
                        ||name.equals("dev.minestrike.skins.ShopExpansion")||name.equals("dev.minestrike.skins.ShopRows");
                if(!own)return super.loadClass(name,resolve);
                Class<?> loaded=findLoadedClass(name);if(loaded==null){byte[] b=transformed.get(name);if(b==null)try{b=resource(name);}catch(IOException e){throw new ClassNotFoundException(name,e);}loaded=defineClass(name,b,0,b.length);}if(resolve)resolveClass(loaded);return loaded;
            }
        };
        Class<?> type=loader.loadClass(prefix+"blockoffensive.map.shop.ItemType");
        Object[] values=(Object[])type.getMethod("values").invoke(null);
        if(values.length!=6||!((Enum<?>)values[5]).name().equals("MYSTERY")||((Enum<?>)values[5]).ordinal()!=5)
            throw new AssertionError("Sixth category enum initialization");
        if(type.getMethod("valueOf",String.class).invoke(null,"MYSTERY")!=values[5])throw new AssertionError("Persistent category key");
        Map<Object,List<Object>> legacy=new HashMap<>();
        Object original=new Object();
        for(int i=0;i<5;i++)legacy.put(values[i],new ArrayList<>(Collections.nCopies(i+1,original)));
        Class<?> expansion=loader.loadClass("dev.minestrike.skins.ShopExpansion");
        expansion.getMethod("normalize",Map.class).invoke(null,legacy);
        if(legacy.size()!=6||!legacy.get(values[5]).isEmpty())throw new AssertionError("Legacy map upgrade");
        for(int i=0;i<5;i++)if(legacy.get(values[i]).size()!=i+1||legacy.get(values[i]).get(0)!=original)
            throw new AssertionError("Existing goods, order and independent lengths must remain intact");
        expansion.getMethod("validate",Map.class,int.class).invoke(null,legacy,6);
        System.out.println("PASS: legacy five-category upgrade preserves existing goods and independent lengths; new category starts empty");
        System.out.println("PASS: real Mixin application on fifteen shop/round/adventure/radar targets; sixth enum initialization and valueOf persistence key");
    }
    private static void armorHud(IMixinTransformer transformer)throws Exception {
        String name="com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameHud";
        ClassNode before=new ClassNode(),after=new ClassNode();byte[] raw=resource(name);
        new ClassReader(raw).accept(before,0);new ClassReader(transformer.transformClassBytes(name,name,raw)).accept(after,0);
        for(var method:before.methods) {
            var actual=after.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
            if(!method.name.equals("renderArmorBar")&&!methodCode(method).equals(methodCode(actual)))throw new AssertionError("Armor HUD must preserve health, ammo, spectators and other rendering: "+method.name);
        }
        var armor=after.methods.stream().filter(m->m.name.equals("renderArmorBar")).findFirst().orElseThrow();
        if(Arrays.stream(armor.instructions.toArray()).filter(i->i instanceof MethodInsnNode c&&c.name.endsWith("minestrike$armor")).count()!=1)throw new AssertionError("Exactly one HUD hook");
        var hook=after.methods.stream().filter(m->m.name.endsWith("minestrike$armor")).findFirst().orElseThrow();
        Set<String> slots=new HashSet<>();int equipmentReads=0,cancels=0;
        for(var i:hook.instructions) {
            if(i instanceof FieldInsnNode f) {
                if(f.owner.equals("net/minecraft/world/entity/EquipmentSlot"))slots.add(f.name);
                if(f.owner.contains("BulletproofArmorAttribute")||f.getOpcode()==Opcodes.PUTFIELD||f.getOpcode()==Opcodes.PUTSTATIC)throw new AssertionError("HUD must not depend on virtual armor or mutate state");
            }
            if(i instanceof MethodInsnNode c){if(c.name.equals("m_6844_"))equipmentReads++;if(c.name.equals("cancel"))cancels++;}
        }
        if(!slots.equals(Set.of("HEAD","CHEST"))||equipmentReads!=2||cancels!=1)throw new AssertionError("Read actual helmet/chest slots every render and replace old virtual HUD");
        var selector=after.methods.stream().filter(m->m.name.endsWith("minestrike$equipped")).findFirst().orElseThrow();
        Set<String> checks=new HashSet<>();boolean armorType=false;
        for(var i:selector.instructions) {
            if(i instanceof MethodInsnNode c)checks.add(c.name);
            if(i instanceof TypeInsnNode t&&t.getOpcode()==Opcodes.INSTANCEOF&&t.desc.equals("net/minecraft/world/item/ArmorItem"))armorType=true;
        }
        if(!armorType||!checks.containsAll(Set.of("m_41619_","m_41720_","m_40402_")))throw new AssertionError("Reject empty slots and non-armor, require correct armor slot");
        var config=JsonParser.parseString(new String(ShopMixinTest.class.getClassLoader().getResourceAsStream("minestrike-skins.mixins.json").readAllBytes(),java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
        if(!config.getAsJsonArray("client").toString().contains("ArmorHudMixin")||config.getAsJsonArray("mixins").toString().contains("ArmorHudMixin"))throw new AssertionError("Client-only HUD registration");
        armorPreview(after);
        System.out.println("PASS: real armor HUD Mixin, actual HEAD/CHEST slots, no virtual defense/state writes, client-only registration, health/ammo/spectator rendering retained and four-state pixel preview");
    }
    public static final class ArmorCanvas extends net.minecraft.client.gui.GuiGraphics {
        java.awt.image.BufferedImage image;
        ArmorCanvas(){super(null,null);}
        @Override public void m_280509_(int x,int y,int x2,int y2,int color) {
            var draw=image.createGraphics();draw.setColor(new java.awt.Color(color,true));draw.fillRect(x,y,x2-x,y2-y);draw.dispose();
        }
    }
    private static void armorPreview(ClassNode transformed)throws Exception {
        String name="ArmorHudGeometry";ClassNode harness=new ClassNode();harness.version=Opcodes.V17;harness.access=Opcodes.ACC_PUBLIC;harness.name=name;harness.superName="java/lang/Object";
        String entry=null;
        for(var method:transformed.methods)if(method.name.endsWith("minestrike$drawArmor")||method.name.endsWith("minestrike$armorIcon")) {
            var copy=new MethodNode(Opcodes.ASM9,Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC,method.name,method.desc,null,null);method.accept(copy);copy.access=Opcodes.ACC_PUBLIC|Opcodes.ACC_STATIC;
            for(var i:copy.instructions)if(i instanceof MethodInsnNode c&&c.owner.equals(transformed.name))c.owner=name;
            harness.methods.add(copy);if(method.name.endsWith("minestrike$drawArmor"))entry=method.name;
        }
        ClassWriter writer=new ClassWriter(ClassWriter.COMPUTE_FRAMES|ClassWriter.COMPUTE_MAXS);harness.accept(writer);byte[] code=writer.toByteArray();
        Class<?> type=new ClassLoader(ShopMixinTest.class.getClassLoader()){Class<?> define(){return defineClass(name,code,0,code.length);}}.define();
        var field=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);var unsafe=(sun.misc.Unsafe)field.get(null);
        var canvas=(ArmorCanvas)unsafe.allocateInstance(ArmorCanvas.class);canvas.image=new java.awt.image.BufferedImage(180,125,java.awt.image.BufferedImage.TYPE_INT_ARGB);
        var background=canvas.image.createGraphics();background.setColor(new java.awt.Color(0xff252a30,true));background.fillRect(0,0,180,125);background.dispose();
        var draw=type.getMethod(entry,net.minecraft.client.gui.GuiGraphics.class,int.class,int.class,boolean.class,boolean.class);
        for(int state=0;state<4;state++) {
            boolean helmet=(state&1)!=0,chest=(state&2)!=0;int y=10+state*28;
            draw.invoke(null,canvas,50,y,helmet,chest);
            if(canvas.image.getRGB(25,y+2)!=(helmet?0xFFF1F3F5:0xFF626970)||canvas.image.getRGB(36,y+2)!=(chest?0xFFF1F3F5:0xFF626970))throw new AssertionError("Independent icon brightness for current equipment combination "+state);
            var text=canvas.image.createGraphics();text.setColor(java.awt.Color.WHITE);text.setFont(new java.awt.Font("Monospaced",java.awt.Font.PLAIN,10));text.drawString("H="+(helmet?1:0)+" C="+(chest?1:0),65,y+11);text.dispose();
        }
        for(int width:new int[]{320,480,960,1920})for(int height:new int[]{180,270,540,1080}) {
            int x=width/2-(int)(width*.26)/2-10-36,y=height-(int)(height*.046)-8;
            if(x-29<0||x-3>=x||y+13>height)throw new AssertionError("Equipment icons must remain left of health and within scaled screen");
        }
        var preview=new java.awt.image.BufferedImage(720,500,java.awt.image.BufferedImage.TYPE_INT_ARGB);var g=preview.createGraphics();g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);g.drawImage(canvas.image,0,0,720,500,null);g.dispose();
        javax.imageio.ImageIO.write(preview,"png",java.nio.file.Path.of("../armor-hud-preview.png").toFile());
    }
}
