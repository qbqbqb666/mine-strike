import dev.minestrike.skins.AimReturnState;
import dev.minestrike.skins.AimAnimations;
import java.util.Map;

public class AimReturnStateTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        var state = new AimReturnState();
        state.cycle("awp", true, 0, 100);
        check(Boolean.FALSE.equals(state.update("awp",false,false,50)),"Leave ADS during bolt duration even before native flag arrives");
        check(state.inputView(false),"Holding/toggle input observes saved aim intent during bolt");
        check(Boolean.FALSE.equals(state.update("awp",false,true,150)),"Wait for actual bolt/animation tail");
        check(Boolean.TRUE.equals(state.update("awp",false,false,200)),"Restore ADS after complete bolt");
        check(state.update("awp",false,false,201)==null,"Only restore once");
        state.cycle("awp",true,300,100); state.input(false);
        check(state.update("awp",false,false,500)==null,"Explicit user release/cancel prevents restore");
        state.cycle("awp",false,600,100);
        check(state.update("awp",false,false,800)==null,"Hip firing never forces ADS");
        state.cycle("awp",false,900,100);
        check(!state.input(true),"New aim input is deferred during bolt");
        check(Boolean.TRUE.equals(state.update("awp",false,false,1100)),"New aim intent restored when ready");
        state.cycle("awp",true,1200,100);state.reload("awp");
        check(Boolean.FALSE.equals(state.update("awp",true,false,1400)),"Reload overrides bolt restore and exits ADS");
        check(!state.input(true),"Reload blocks aiming");
        check(Boolean.FALSE.equals(state.update("awp",false,false,1600)),"Reload completion keeps hip view");
        check(!state.input(true)&&state.inputView(false),"Still-held key cannot immediately re-enter ADS");
        state.input(false);check(state.input(true),"Release then press manually re-enters ADS");
        state.reload("awp");state.update("awp",true,false,1800);state.release();
        check(state.update("awp",false,false,1900)==null&&state.input(true),"Toggle mode permits fresh manual press after reload");
        for(Object next:new Object[]{"other-slot", "new-player", "new-world", null}) {
            state.cycle("awp",true,2000,100);
            check(state.update(next,false,false,2200)==null,"Context switch/death cancels saved restore");
        }
        state.bind("semi");check(state.update("semi",false,false,3000)==null,"Uncycled semi-auto remains untouched");
        check(AimAnimations.cycleSeconds(0,null)==0,"Manual-action enum alone cannot classify M107 as bolt sniper");
        check(AimAnimations.cycleSeconds(0,Map.of("bolt_time",.55))==.55,"Script-controlled M870 pump timing included");
        check(AimAnimations.cycleSeconds(1.3f,null)>1.29,"Combined OPEN_BOLT AWP timing included");
        System.out.println("PASS: real ADS bolt/pump exit and single restore, user cancellation, reload/manual re-aim, hold/toggle input and context resets; M107/M870/AWP classification");
    }
}
