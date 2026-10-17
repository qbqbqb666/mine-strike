import dev.minestrike.skins.*;
import net.minecraft.network.FriendlyByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.sounds.SoundSource;
import net.minecraft.client.resources.sounds.SoundInstance;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.util.*;
import java.nio.file.*;

public class MatchFeedbackTest {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static void near(double a,double b,String message){check(Math.abs(a-b)<1e-6,message+": "+a+" != "+b);}
    public static void main(String[] args)throws Exception {
        near(MatchDamage.units(20,10,20),50,"HUD100 units");near(MatchDamage.units(40,30,40),25,"Edited maximum health");
        near(MatchDamage.units(8,-100,20),40,"No lethal overkill");near(MatchDamage.units(0,-100,20),0,"No repeated post-death damage");
        near(MatchDamage.units(8,12,20),0,"No healing counted");near(MatchDamage.units(20,20,20),0,"Cancelled/fully absorbed damage");
        near(MatchDamage.units(20,10,0),0,"Invalid maximum");near(MatchDamage.units(Double.NaN,10,20),0,"Non-finite input");
        double split=MatchDamage.units(20,16,20)+MatchDamage.units(16,12,20);near(split,40,"Separate normal/AP components counted once each");
        for(boolean live:new boolean[]{false,true})for(boolean same:new boolean[]{false,true})for(boolean self:new boolean[]{false,true})
            for(String attacker:List.of("t","ct",""))for(String victim:List.of("t","ct",""))
                check(MatchDamage.eligible(live,same,attacker,victim,self)==(live&&same&&!self&&new HashSet<>(List.of(attacker,victim)).containsAll(Set.of("t","ct"))),"Only live same-map enemy participants");
        UUID player=UUID.randomUUID();var stats=new MatchFeedback.Stats("minecraft:overworld / dust2",Map.of(player,123.25));
        var b=new FriendlyByteBuf(Unpooled.buffer());try{MatchFeedback.Stats.encode(stats,b);check(stats.equals(MatchFeedback.Stats.decode(b)),"Stats packet round trip preserves fractional total");}finally{b.release();}
        var notice=new MatchFeedback.Notice(UUID.randomUUID(),"dust2");b=new FriendlyByteBuf(Unpooled.buffer());try{MatchFeedback.Notice.encode(notice,b);check(notice.equals(MatchFeedback.Notice.decode(b)),"Dedicated announcement packet round trip");}finally{b.release();}
        for(double bad:new double[]{-1,Double.NaN,Double.POSITIVE_INFINITY}) {
            boolean rejected=false;try{new MatchFeedback.Stats("dust2",Map.of(player,bad));}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Reject invalid client stats");
        }
        b=new FriendlyByteBuf(Unpooled.buffer());try{b.m_130072_("dust2",512);b.writeInt(1025);boolean rejected=false;try{MatchFeedback.Stats.decode(b);}catch(IllegalArgumentException e){rejected=true;}check(rejected,"Bound network row count");}finally{b.release();}
        check(MatchFeedback.Client.first(notice.id())&&!MatchFeedback.Client.first(notice.id()),"One playback per install notification");
        var field=MatchFeedback.Client.class.getDeclaredField("stats");field.setAccessible(true);field.set(null,stats);
        near(MatchFeedback.Client.damage(player),123.25,"TAB reads dedicated match totals");MatchFeedback.Client.logout(null);near(MatchFeedback.Client.damage(player),0,"Disconnect clears stale scoreboard");check(MatchFeedback.Client.first(notice.id()),"Disconnect clears announcement dedup");
        var sound=BombAudio.Client.plantedSound(BombAudio.LOUD_PLANTED);
        check(sound.m_7904_().toString().equals(BombAudio.LOUD_PLANTED)&&sound.m_8070_()==SoundSource.MASTER&&sound.m_7438_()==SoundInstance.Attenuation.NONE&&sound.m_7796_()&&!sound.m_7775_(),"Direct announcement is MASTER, relative, non-looping and unattenuated");
        System.out.println("PASS: HUD100 damage units, post-armor/overkill/absorption guards, same-map enemies, AP components, bounded packets, fractional snapshots, install dedup and non-positional master playback");
    }
    private static byte[] bytes(String name)throws Exception{try(var in=MatchFeedbackTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){return in.readAllBytes();}}
    private static long calls(MethodNode m,String suffix){return Arrays.stream(m.instructions.toArray()).filter(i->i instanceof MethodInsnNode c&&c.name.endsWith(suffix)).count();}
    public static void mixins(IMixinTransformer transformer)throws Exception {
        String player="net.minecraft.world.entity.player.Player";ClassNode before=new ClassNode(),after=new ClassNode();byte[] raw=bytes(player);
        new ClassReader(raw).accept(before,0);new ClassReader(transformer.transformClassBytes(player,player,raw)).accept(after,0);
        var old=before.methods.stream().filter(m->m.name.equals("m_6475_")).findFirst().orElseThrow();var now=after.methods.stream().filter(m->m.name.equals("m_6475_")).findFirst().orElseThrow();
        check(calls(now,"minestrike$damage")==1&&calls(old,"m_21153_")==1,"Wrap exactly the actual post-armor health deduction");
        for(String call:List.of("onLivingHurt","onLivingDamage","m_21161_","m_6515_","m_7911_","m_289194_"))check(calls(now,call)==calls(old,call),"Keep native event/armor/enchantment/absorption/combat tracker: "+call);
        var wrapper=after.methods.stream().filter(m->m.name.endsWith("minestrike$damage")).findFirst().orElseThrow();boolean deducted=false,recorded=false;
        for(var i:wrapper.instructions)if(i instanceof MethodInsnNode c){if(c.name.equals("m_21153_"))deducted=true;if(c.name.equals("record")){check(deducted,"Only record after real setter");recorded=true;}}
        check(recorded,"Actual damage recorder hook");
        String map="com.phasetranscrystal.blockoffensive.map.CSGameMap";ClassNode state=new ClassNode();new ClassReader(transformer.transformClassBytes(map,map,bytes(map))).accept(state,0);
        check(state.interfaces.contains("dev/minestrike/skins/MatchDamage$State"),"Live match phase accessor");
        check(state.methods.stream().filter(m->m.name.equals("startGame")).anyMatch(m->calls(m,"minestrike$start")==1),"New match reset");
        check(state.methods.stream().filter(m->m.name.equals("startNewRound")).noneMatch(m->calls(m,"minestrike$start")>0),"No sub-round reset");
        var live=state.methods.stream().filter(m->m.name.equals("minestrike$combatLive")).findFirst().orElseThrow();Set<String> flags=new HashSet<>();for(var i:live.instructions)if(i instanceof FieldInsnNode f)flags.add(f.name);check(flags.containsAll(Set.of("isWaiting","isWarmTime","isPause","isWaitingWinner")),"No warmup/countdown/pause/settled damage");
        String tab="com.phasetranscrystal.blockoffensive.client.screen.hud.CSGameTabRenderer";ClassNode display=new ClassNode();new ClassReader(transformer.transformClassBytes(tab,tab,bytes(tab))).accept(display,0);
        check(display.methods.stream().filter(m->m.name.equals("renderPlayerRow")).anyMatch(m->calls(m,"minestrike$damage")==1),"TAB damage cell uses new units");
        check(display.methods.stream().filter(m->m.name.equals("lambda$render$0")).anyMatch(m->calls(m,"minestrike$sortDamage")==2),"TAB comparator uses same stats");
        var util=new ClassNode();new ClassReader(bytes("com.phasetranscrystal.fpsmatch.util.FPSMUtil")).accept(util,0);
        check(util.methods.stream().filter(m->m.name.equals("sortPlayerInventory")).anyMatch(m->Arrays.stream(m.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.name.equals("MAIN_WEAPON_PREDICATE"))),"Native sorting consumes extensible main-weapon predicate");
        var drop=new ClassNode();new ClassReader(bytes("com.phasetranscrystal.fpsmatch.common.entity.drop.DropType")).accept(drop,0);
        check(drop.methods.stream().anyMatch(m->Arrays.stream(m.instructions.toArray()).anyMatch(i->i instanceof FieldInsnNode f&&f.name.equals("MAIN_WEAPON_PREDICATE"))),"Native replacement/drop rules share same primary classification");
        var mod=new ClassNode();new ClassReader(bytes("dev.minestrike.skins.MineStrikeSkins")).accept(mod,0);check(mod.methods.stream().anyMatch(m->calls(m,"addMainWeaponPredicate")>0),"Register BOCEK primary predicate in common setup");
        var feedback=new ClassNode();new ClassReader(bytes("dev.minestrike.skins.MatchFeedback")).accept(feedback,0);var announce=feedback.methods.stream().filter(m->m.name.equals("announce")).findFirst().orElseThrow();check(calls(announce,"getJoinedPlayersWithSpec")==1&&calls(announce,"send")==1,"Dedicated C4 notice reaches both teams and spectators");
        var fall=new ClassNode();new ClassReader(bytes("dev.minestrike.skins.FallDamage")).accept(fall,0);check(fall.methods.stream().flatMap(m->Arrays.stream(m.instructions.toArray())).anyMatch(i->i instanceof LdcInsnNode n&&Float.valueOf(.25f).equals(n.cst)),"Fall damage is a quarter of native");
        System.out.println("PASS: actual post-health damage Mixin, original defense/assist/MVP pipelines retained, match-only reset, phase guards, TAB row/comparator units, BOCEK shared native primary classification and quarter fall damage");
    }
}
