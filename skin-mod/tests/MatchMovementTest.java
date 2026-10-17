import dev.minestrike.skins.MatchMovement;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.ai.attributes.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;

public class MatchMovementTest {
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    private static void near(double actual,double expected,String message) {check(Math.abs(actual-expected)<1.0E-7,message+": "+actual+" != "+expected);}
    private static double horizontal(Vec3 v) {return Math.hypot(v.f_82479_,v.f_82481_);}
    private static Vec3 tick(MatchMovement.State state,Vec3 velocity,Vec3 target) {
        var next=state.move(velocity,target);state.finish(next,.6f);return next.m_82542_(.6f*.91f,1,.6f*.91f);
    }
    public static void main(String[] args) throws Exception {
        jumps();
        unlock();
        double speed=MatchMovement.speed(.1,0);
        double vanilla=0;
        for(int i=0;i<400;i++) vanilla=vanilla*(.6f*.91f)+(.13f*.21600002f/(.6f*.6f*.6f))*.98f;
        Vec3 forward=MatchMovement.target(new Vec3(0,0,.98f),0,speed,.6f);
        near(forward.f_82481_,vanilla,"Target equals actual vanilla sprint steady ground speed");
        for(float yaw:new float[]{0,45,90,180,270})for(double slow:new double[]{1,.3,.6}) {
            double reference=horizontal(MatchMovement.target(new Vec3(0,0,.98f*slow),yaw,speed,.6f));
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(x!=0||z!=0) {
                var target=MatchMovement.target(new Vec3(x*.98f*slow,0,z*.98f*slow),yaw,speed,.6f);
                near(horizontal(target),reference,"Eight directions equal, including crouch/aim input strength");
            }
            near(reference,horizontal(forward)*slow,"Crouch/aim input reduction retained");
        }
        var attribute=new AttributeInstance(new RangedAttribute("test",.1,0,1024),a->{});
        attribute.m_22118_(new AttributeModifier(UUID.randomUUID(),"Gun weight",-.2,AttributeModifier.Operation.MULTIPLY_BASE));
        attribute.m_22118_(new AttributeModifier(UUID.randomUUID(),"Aim",-.4,AttributeModifier.Operation.MULTIPLY_TOTAL));
        double weighted=MatchMovement.speed(attribute.m_22135_(),0);
        var sprint=new AttributeModifier(UUID.randomUUID(),"Sprinting speed boost",.3f,AttributeModifier.Operation.MULTIPLY_TOTAL);
        attribute.m_22118_(sprint);
        near(MatchMovement.speed(attribute.m_22135_(),sprint.m_22218_()),weighted,"Sprint toggle does not stack or remove gun/ADS attribute modifiers");
        near(weighted,speed*.8*.6,"Native gun weight and ADS multipliers retained");
        for(double gunWeight:new double[]{1,.8,.55}) {
            double walk=.1*gunWeight, run=walk*MatchMovement.SPRINT_FACTOR;
            double fromWalk=MatchMovement.movementFov(walk,.1),fromRun=MatchMovement.movementFov(run,.1);
            near(MatchMovement.fov((float)fromWalk,fromWalk,fromRun),fromRun,"Ordinary movement uses existing fast FOV with native gun weight");
            near(MatchMovement.fov((float)fromRun,fromRun,fromRun),fromRun,"Ctrl/double-W cannot add another FOV tier");
            near(MatchMovement.fov((float)(fromWalk*.7),fromWalk,fromRun),fromRun*.7,"Preserve other multiplicative FOV effects");
        }
        near(MatchMovement.fov(1,1,1.15f),1.15,"TaCZ disables attribute FOV: ordinary movement gets native sprint view");
        near(MatchMovement.fov(1.15f,1.15f,1.15f),1.15,"TaCZ native sprint view does not stack");
        near(MatchMovement.movementFov(0,0),1,"Native invalid walking-speed FOV fallback retained");
        var lenses=new ClassNode();
        try(var in=MatchMovementTest.class.getClassLoader().getResourceAsStream("dev/minestrike/skins/MatchMovement$Client.class")){new ClassReader(in).accept(lenses,0);}
        var subscriber=lenses.visibleAnnotations.stream().filter(a->a.desc.endsWith("Mod$EventBusSubscriber;")).findFirst().orElseThrow();
        int side=subscriber.values.indexOf("value");
        var sides=(List<?>)subscriber.values.get(side+1);
        check(sides.size()==1&&((String[])sides.get(0))[1].equals("CLIENT"),"FOV subscriber only loads on clients");
        System.out.println("PASS: single fast FOV for ordinary movement/Ctrl/double-W, native TaCZ attribute-FOV override, gun-weight FOV, extra effects and client-only event registration");
        var state=new MatchMovement.State();Vec3 velocity=Vec3.f_82478_;
        var first=state.move(velocity,forward);near(first.f_82481_,forward.f_82481_/2,"Start reaches half speed at 50ms");
        state.finish(first,.6f);velocity=first.m_82542_(.6f*.91f,1,.6f*.91f);
        var second=state.move(velocity,forward);near(second.f_82481_,forward.f_82481_,"Start reaches target at 100ms");
        state.finish(second,.6f);velocity=second.m_82542_(.6f*.91f,1,.6f*.91f);
        for(int i=0;i<200;i++)velocity=tick(state,velocity,forward);
        near(velocity.f_82481_,forward.f_82481_*(.6f*.91f),"No accumulation or sprint oscillation during sustained movement");
        var stop=state.move(velocity,Vec3.f_82478_);near(stop.f_82481_,forward.f_82481_/2,"Stop first half");
        state.finish(stop,.6f);stop=state.move(stop.m_82542_(.6f*.91f,1,.6f*.91f),Vec3.f_82478_);near(stop.f_82481_,0,"Stopped at 100ms");
        state.finish(stop,.6f);velocity=tick(state,Vec3.f_82478_,forward);velocity=tick(state,velocity,forward);
        var reverse=state.move(velocity,forward.m_82490_(-1));near(reverse.f_82481_,0,"Reversal crosses zero at 50ms");
        state.finish(reverse,.6f);reverse=state.move(Vec3.f_82478_,forward.m_82490_(-1));near(reverse.f_82481_,-forward.f_82481_,"Reversed at 100ms");
        state.reset();velocity=tick(state,Vec3.f_82478_,forward);velocity=tick(state,velocity,forward);
        double impulse=.4;
        for(int i=0;i<5;i++) {
            var actual=state.move(velocity.m_82520_(i==0?impulse:0, -.08, 0),forward);
            near(actual.f_82479_,impulse,"External horizontal impulse survives movement control");
            near(actual.f_82480_,-.08*(i+1),"Vertical gravity/knockback component untouched");
            state.finish(actual,.6f);velocity=actual.m_82542_(.6f*.91f,1,.6f*.91f);impulse*=.6f*.91f;
        }
        check(velocity.f_82479_>0,"External impulse decays through native friction rather than hard clamp");
        state.reset();velocity=tick(state,Vec3.f_82478_,forward);velocity=tick(state,velocity,forward);
        state.finish(Vec3.f_82478_,.6f);
        var restart=state.move(Vec3.f_82478_,forward);near(restart.f_82481_,forward.f_82481_/2,"Wall collision clears blocked momentum; restart is smooth");
        state.reset();var landing=state.move(new Vec3(0,-.1,forward.f_82481_),forward);
        near(landing.f_82481_,forward.f_82481_,"Ground handoff never adds another full walking velocity on landing");
        near(landing.f_82480_,-.1,"Landing vertical velocity retained");
        System.out.println("PASS: native sprint baseline, eight equal directions/yaw/diagonal, gun/ADS/crouch reductions, 100ms start/stop/reverse, sustained speed, external impulses, collision restart and landing handoff");
    }
    private static void unlock() throws Exception {
        var state=new MatchMovement.UnlockKeys();
        check(!state.update("round-1",false,true),"No synthetic keys on ordinary ticks or initial join");
        boolean physicalW=true,gameW=true;
        for(int tick=0;tick<100;tick++) {
            gameW=false;
            check(!state.update("round-1",true,true),"Countdown keeps native movement lock for every frozen tick");
        }
        check(!gameW&&physicalW,"Reproduce native cleared W while physical W remains held");
        if(state.update("round-1",false,true))gameW=physicalW;
        check(gameW,"Countdown ends: held W restored without release/repress");
        check(!state.update("round-1",false,true),"Only one resynchronization, not continuous key override");
        state.update("round-1",true,true);physicalW=false;gameW=false;
        if(state.update("round-1",false,true))gameW=physicalW;
        check(!gameW,"Released key cannot create ghost movement");
        state.update("round-1",true,true);
        check(!state.update("round-1",false,false),"No re-arming while screen open, window unfocused or START phase");
        check(!state.update("round-1",false,false),"Keep deferred restore pending");
        check(state.update("round-1",false,true),"Resume once focus/gameplay/END phase is ready");
        state.update("round-1",true,true);state.update("round-1",false,false);
        check(!state.update("round-1",true,true),"Re-enter freeze cancels pending restore");
        check(state.update("round-1",false,true),"Second round/pause unlock also recovers keys");
        state.update("round-1",true,true);
        check(!state.update("new-player-world",false,true),"Player/world change clears old frozen context");
        state.update("new-player-world",true,true);
        check(!state.update(null,false,true)&&!state.update("new-player-world",false,true),"Disconnect/spectator/outside-CS clears old key recovery");
        var original=new ClassNode();
        try(var in=MatchMovementTest.class.getClassLoader().getResourceAsStream("com/phasetranscrystal/blockoffensive/client/BOClientEvent.class")){new ClassReader(in).accept(original,0);}
        var lock=original.methods.stream().filter(m->m.name.equals("lockMove")).findFirst().orElseThrow();
        var keys=new ArrayList<String>();int clears=0;
        for(var ins:lock.instructions) {
            if(ins instanceof FieldInsnNode f&&f.owner.equals("net/minecraft/client/Options"))keys.add(f.name);
            if(ins instanceof MethodInsnNode c&&c.owner.equals("net/minecraft/client/KeyMapping")&&c.name.equals("m_7249_"))clears++;
        }
        check(clears==5&&keys.equals(List.of("f_92085_","f_92086_","f_92087_","f_92088_","f_92089_")),"Actual native freeze clears exactly four movement keys plus jump");
        var bridge=new ClassNode();
        try(var in=MatchMovementTest.class.getClassLoader().getResourceAsStream("dev/minestrike/skins/MatchMovement$Client.class")){new ClassReader(in).accept(bridge,0);}
        var recovery=bridge.methods.stream().filter(m->m.name.equals("unlock")).findFirst().orElseThrow();
        var restored=new ArrayList<String>();
        for(var ins:recovery.instructions)if(ins instanceof FieldInsnNode f&&f.owner.equals("net/minecraft/client/Options"))restored.add(f.name);
        check(restored.equals(keys),"Only original frozen keys are restored; no fire/reload/toggle keys");
        System.out.println("PASS: native countdown held-W failure reproduced, frozen lock retained, single unlock recovery, released keys, focus/GUI deferral, repeated rounds/pause and player/world/spectator reset");
    }
    private static void jumps() {
        var jump=new MatchMovement.Jumps();
        check(!jump.nativeMovement(),"No chain before an actual jump");
        jump.start(true);jump.begin(true,true,true);
        check(jump.nativeMovement(),"Actual takeoff bypasses ground smoothing before onGround changes");
        jump.finish();jump.begin(true,true,true);
        check(jump.nativeMovement(),"Held repeated jump preserves native movement on landing/cooldown");
        jump.begin(true,false,true);check(!jump.nativeMovement(),"Release jump restores ordinary ground response");
        jump.start(true);jump.finish();jump.begin(true,true,false);
        check(!jump.nativeMovement(),"Aim/crouch/no-forward intent ends running chain");
        jump.start(false);jump.begin(true,true,false);
        check(jump.nativeMovement(),"Non-sprint takeoff also keeps native jump height and launch timing");
        jump.finish();check(!jump.nativeMovement(),"Non-sprint jump cannot leave a running chain");
        jump.start(true);jump.begin(false,true,true);check(!jump.nativeMovement(),"Death/outside match/world change clears chain");
        near(MatchMovement.airAcceleration(.02f,false),.025999999f,"Virtual run uses native sprint air acceleration");
        near(MatchMovement.airAcceleration(.025999999f,true),.025999999f,"Existing Ctrl sprint never receives a second air boost");
        for(double weight:new double[]{1,.8,.55}) {
            double[][] reference=jumpTrace(true,true,weight,1),unified=jumpTrace(false,true,weight,1);
            for(int tick=0;tick<reference.length;tick++)for(int field=0;field<reference[tick].length;field++)
                check(Math.abs(reference[tick][field]-unified[tick][field])<.0001,"1000-tick native sprint-jump replay matches without Ctrl, tick "+tick+" field "+field);
        }
        double normal=jumpTrace(false,true,1,1)[999][0];
        check(jumpTrace(false,false,.6,1)[999][0]<normal,"Aim movement reduction is not bypassed by jump chain");
        check(jumpTrace(false,false,1,.3)[999][0]<normal*.6,"Crouched input keeps reduced movement while jumping");
        check(jumpTrace(false,true,.55,1)[999][0]<normal,"Gun-weight reduction survives repeated jump replay");
        System.out.println("PASS: native takeoff/landing/cooldown chain, Ctrl/no-Ctrl 1000-tick trajectories with gun weights, single sprint boost, unchanged jump arc, aim/crouch reductions and chain cancellation");
    }
    private static double[][] jumpTrace(boolean ctrl,boolean running,double weight,double inputScale) {
        var chain=new MatchMovement.Jumps();
        double[][] trace=new double[1000][4];double z=0,y=0,vz=0,vy=0;boolean ground=true;int cooldown=0;
        for(int tick=0;tick<trace.length;tick++) {
            if(cooldown>0)cooldown--;
            boolean jump=ground&&cooldown==0;
            if(jump) {
                chain.start(running);vy=.42f;
                if(ctrl||running)vz+=.2f;
                cooldown=10;
            }
            chain.begin(true,true,running);
            float rawSpeed=(float)(.1*weight*(ctrl?MatchMovement.SPRINT_FACTOR:1));
            float acceleration;
            if(ground) {
                acceleration=rawSpeed*.21600002f/(.6f*.6f*.6f);
                if(!ctrl&&chain.nativeMovement())acceleration=(float)MatchMovement.speed(acceleration,0);
            } else {
                acceleration=ctrl?.025999999f:.02f;
                if(!ctrl&&running)acceleration=MatchMovement.airAcceleration(acceleration,false);
            }
            vz+=acceleration*.98f*inputScale;
            z+=vz;y+=vy;float drag=ground?.6f*.91f:.91f;
            if(y<=0){y=0;vy=0;ground=true;}else ground=false;
            vz*=drag;vy=(vy-.08)*.98f;chain.finish();
            trace[tick]=new double[]{z,y,vz,vy};
        }
        return trace;
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        String name="net.minecraft.world.entity.LivingEntity";byte[] raw;
        try(var in=MatchMovementTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){raw=in.readAllBytes();}
        var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);
        new ClassReader(transformer.transformClassBytes(name,name,raw)).accept(after,0);
        for(String hook:List.of("minestrike$travel","minestrike$groundMove","minestrike$input","minestrike$afterMove","minestrike$jump","minestrike$jumpSprint","minestrike$acceleration","minestrike$travelEnd")) {
            long count=after.methods.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).filter(i->i instanceof MethodInsnNode c&&c.name.endsWith(hook)).count();
            check(count==1,"Exactly one real movement hook: "+hook);
        }
        for(var method:before.methods) {
            var actual=after.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
            near(calls(actual).size(),calls(method).size(),"Native call count "+method.name);
            check(calls(actual).equals(calls(method)),"Native movement/collision/gravity/jump/sprint/air and all other calls preserved: "+method.name);
            check(writes(actual).equals(writes(method)),"Original field writes including jump flag/height/cooldown preserved: "+method.name);
        }
        System.out.println("PASS: actual shared LivingEntity movement Mixin; native collision/gravity/jump/air/sprint calls and attributes untouched");
    }
    private static List<String> calls(MethodNode method) {
        var result=new ArrayList<String>();
        for(var ins:method.instructions)if(ins instanceof MethodInsnNode c) {
            if(c.name.contains("minestrike$jumpSprint"))result.add("net/minecraft/world/entity/LivingEntitym_20142_()Z");
            else if(!c.name.contains("minestrike$")&&!c.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/")&&!c.owner.startsWith("dev/minestrike/skins/MatchMovement$")&&!c.owner.equals("java/lang/Float"))result.add(c.owner+c.name+c.desc);
        }
        return result;
    }
    private static List<String> writes(MethodNode method) {
        var result=new ArrayList<String>();
        for(var ins:method.instructions)if(ins instanceof FieldInsnNode f&&(f.getOpcode()==Opcodes.PUTFIELD||f.getOpcode()==Opcodes.PUTSTATIC)&&!f.name.contains("minestrike$"))result.add(f.owner+f.name+f.desc);
        return result;
    }

}
