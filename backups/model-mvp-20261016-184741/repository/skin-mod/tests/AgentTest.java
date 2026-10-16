import dev.minestrike.skins.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.nio.file.*;
import java.util.*;

public class AgentTest {
    private static void check(boolean value,String message) { if(!value)throw new AssertionError(message); }
    public static void main(String[] args)throws Exception {
        SkinCatalog catalog=SkinCatalog.JSON.fromJson(Files.readString(Path.of(args[0])),SkinCatalog.class);
        AgentSkins.append(catalog);AgentSkins.append(catalog);catalog.validate();
        check(catalog.groups.size()==7,"Append once without replacing weapon groups");
        check(AgentSkins.ALL.size()==10,"Ten selected agents");
        var choices=Map.of("agent_t","minestrike_skins:t7_numberk","agent_ct","minestrike_skins:ct3_ava","ak","mcs2:cs_ak_asiimov");
        check(catalog.checked(choices).equals(choices),"Weapon and both team choices survive checking");
        check(catalog.checked(Map.of("agent_t","minestrike_skins:ct3_ava")).isEmpty(),"Reject cross-team selection");
        check(AgentSkins.chosen("ct",choices).endsWith("ct3_ava") && AgentSkins.chosen("t",choices).endsWith("t7_numberk"),"Switch sides uses separately saved agent");
        check(AgentSkins.chosen("ct",Map.of()).endsWith("ct1_sas") && AgentSkins.chosen("t",Map.of()).endsWith("t1_phoenix"),"Default agents");
        Path directory=Files.createTempDirectory("ms-agents-");UUID uuid=UUID.randomUUID();
        try {
            SkinStore store=new SkinStore(directory,catalog);store.save(uuid,choices);
            check(new SkinStore(directory,catalog).load(uuid).equals(choices),"Both agents persist with existing weapon choices");
        } finally {Files.deleteIfExists(directory.resolve(uuid+".json"));Files.delete(directory);}
        FriendlyByteBuf bytes=new FriendlyByteBuf(Unpooled.buffer());
        try {
            var packet=new SkinNetwork.Agents(List.of(new AgentPlayers.Appearance(uuid,"minestrike_skins:ct3_ava")));
            SkinNetwork.Agents.encode(packet,bytes);check(packet.equals(SkinNetwork.Agents.decode(bytes)),"UUID agent sync round trip");
            bytes.clear();bytes.writeInt(1025);boolean rejected=false;
            try {SkinNetwork.Agents.decode(bytes);} catch(IllegalArgumentException e){rejected=true;}
            check(rejected,"Bound snapshot count before allocation");
            bytes.clear();var clear=new SkinNetwork.Agents(List.of());SkinNetwork.Agents.encode(clear,bytes);
            check(SkinNetwork.Agents.decode(bytes).players().isEmpty(),"Match end clears all skins");
        } finally {bytes.release();}
        var standing=PlayerVolumes.model(Vec3.f_82478_,0,0,0,false,false,0,0,false);
        var head=PlayerVolumes.clip(standing,new Vec3(0,1.72,3),new Vec3(0,1.72,-3));
        check(head!=null&&head.head(),"Head interior hit");
        check(Math.abs(head.point().f_82481_-.15234375)<.000001,"Head boundary follows rendered 65 percent head");
        check(PlayerVolumes.clip(standing,new Vec3(.2,1.72,3),new Vec3(.2,1.72,-3))==null,"Old large head edge must miss");
        var torso=PlayerVolumes.clip(standing,new Vec3(0,1.3,3),new Vec3(0,1.3,-3));
        check(torso!=null&&!torso.head()&&!torso.leg(),"Torso remains body hit");
        check(PlayerVolumes.clip(standing,new Vec3(.30,1.3,3),new Vec3(.30,1.3,-3))!=null,"Arm outside movement box can be hit");
        check(PlayerVolumes.clip(standing,new Vec3(.40,1.3,3),new Vec3(.40,1.3,-3))==null,"Outside silhouette misses");
        var leg=PlayerVolumes.clip(standing,new Vec3(.08,.3,3),new Vec3(.08,.3,-3));
        check(leg!=null&&leg.leg(),"Keep Superb leg classification");
        var crouch=PlayerVolumes.model(Vec3.f_82478_,0,0,0,true,false,0,0,false);
        check(PlayerVolumes.clip(crouch,new Vec3(0,1.72,3),new Vec3(0,1.72,-3))==null,"Crouch has no standing head hit");
        var prone=PlayerVolumes.model(Vec3.f_82478_,0,0,0,false,true,0,0,false);
        check(PlayerVolumes.clip(prone,new Vec3(0,1.72,3),new Vec3(0,1.72,-3))==null,"Prone has no standing head hit");
        var turned=PlayerVolumes.model(Vec3.f_82478_,Math.PI/2,Math.PI/2,0,false,false,0,0,false);
        var side=PlayerVolumes.clip(turned,new Vec3(3,1.3,0),new Vec3(-3,1.3,0));
        check(side!=null&&!side.head(),"Rotated model clip");
        check(Math.abs(24*PlayerMetrics.LENGTH+8*PlayerMetrics.HEAD-32)<.00001,"Original total model height");
        System.out.println("PASS: agent catalog, persistence, team defaults, packet bounds, rotated head/body/arm/leg rays and stance exclusions");
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        for(var entry:Map.of("com.tacz.guns.util.EntityUtil","minestrike$clip",
                "com.atsuishio.superbwarfare.entity.projectile.ProjectileEntity","minestrike$clip",
                "net.minecraft.world.entity.player.Player","minestrike$dimensions",
                "net.minecraft.client.player.AbstractClientPlayer","minestrike$agent").entrySet()) {
            String name=entry.getKey();byte[] before;
            try(var in=AgentTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){before=in.readAllBytes();}
            byte[] after=transformer.transformClassBytes(name,name,before);
            ClassNode node=new ClassNode();new ClassReader(after).accept(node,0);
            check(!Arrays.equals(before,after),"Mixin target unchanged: "+name);
            int calls=0;
            for(MethodNode method:node.methods)for(AbstractInsnNode ins:method.instructions)
                if(ins instanceof MethodInsnNode call&&call.name.contains(entry.getValue()))calls++;
            check(calls>=1,"Missing real Mixin call: "+name);
            if(name.endsWith("EntityUtil")||name.endsWith("ProjectileEntity")) {
                MethodNode clip=node.methods.stream().filter(m->m.name.contains("minestrike$clip")).findFirst().orElseThrow();
                boolean scoped=false,nativeFallback=false;
                for(AbstractInsnNode ins:clip.instructions)if(ins instanceof MethodInsnNode call) {
                    scoped|=call.owner.equals("dev/minestrike/skins/AgentPlayers")&&call.name.equals("active");
                    nativeFallback|=call.owner.equals("net/minecraft/world/phys/AABB")&&call.name.equals("m_82371_");
                }
                check(scoped&&nativeFallback,"Only match players use shape rays; native fallback kept");
                MethodNode original=node.methods.stream().filter(m->m.name.equals("getHitResult")).findFirst().orElseThrow();
                boolean compensation=false;
                for(AbstractInsnNode ins:original.instructions)if(ins instanceof MethodInsnNode call)
                    compensation|=call.name.equals("getFixedBoundingBox")||call.name.equals("getBoundingBox");
                check(compensation,"Native latency helper retained");
            }
        }
        System.out.println("PASS: real TaCZ/Superb/Player/client-skin Mixins, match-only guards, original ray fallback and latency helpers");
    }
}
