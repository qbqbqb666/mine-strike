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
    public static void main(String[] args) {
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
    public static void mixins(IMixinTransformer transformer)throws Exception {
        String name="net.minecraft.world.entity.LivingEntity";byte[] raw;
        try(var in=MatchMovementTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){raw=in.readAllBytes();}
        var before=new ClassNode();var after=new ClassNode();new ClassReader(raw).accept(before,0);
        new ClassReader(transformer.transformClassBytes(name,name,raw)).accept(after,0);
        for(String hook:List.of("minestrike$travel","minestrike$groundMove","minestrike$input","minestrike$afterMove")) {
            long count=after.methods.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).filter(i->i instanceof MethodInsnNode c&&c.name.contains(hook)).count();
            check(count==1,"Exactly one real movement hook: "+hook);
        }
        for(var method:before.methods) {
            var actual=after.methods.stream().filter(m->m.name.equals(method.name)&&m.desc.equals(method.desc)).findFirst().orElseThrow();
            near(calls(actual).size(),calls(method).size(),"Native call count "+method.name);
            check(calls(actual).equals(calls(method)),"Native movement/collision/gravity/jump/sprint/air and all other calls preserved: "+method.name);
        }
        System.out.println("PASS: actual shared LivingEntity movement Mixin; native collision/gravity/jump/air/sprint calls and attributes untouched");
    }
    private static List<String> calls(MethodNode method) {
        var result=new ArrayList<String>();
        for(var ins:method.instructions)if(ins instanceof MethodInsnNode c&&!c.name.contains("minestrike$")&&!c.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/")&&!c.owner.equals("dev/minestrike/skins/MatchMovement$State"))result.add(c.owner+c.name+c.desc);
        return result;
    }
}
