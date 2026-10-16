import dev.minestrike.skins.*;
import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.phasetranscrystal.blockoffensive.sound.MVPMusicManager;
import cpw.mods.jarhandling.SecureJar;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.VersionInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileParser;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraft.resources.ResourceLocation;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import java.nio.file.*;
import java.util.*;

public class MusicBoxesTest {
    private static void check(boolean value,String message) {if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        // Hide cross-mod class resources as Forge does, while retaining the real mod-file resource provider.
        var isolated = new ClassLoader(MusicBoxesTest.class.getClassLoader()) {
            @Override public java.net.URL getResource(String name) {
                return name.equals("assets/blockoffensive/sounds.json") ? null : super.getResource(name);
            }
            @Override protected Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException {
                if (!name.equals("dev.minestrike.skins.MusicBoxes") && !name.equals("dev.minestrike.skins.MusicBoxes$Track"))
                    return super.loadClass(name,resolve);
                var loaded=findLoadedClass(name);
                if(loaded==null)try(var in=getParent().getResourceAsStream(name.replace('.','/')+".class")) {
                    byte[] bytes=in.readAllBytes();loaded=defineClass(name,bytes,0,bytes.length);
                } catch(java.io.IOException e){throw new ClassNotFoundException(name,e);}
                if(resolve)resolveClass(loaded);return loaded;
            }
        };
        var isolatedMusic=isolated.loadClass("dev.minestrike.skins.MusicBoxes");
        check(isolatedMusic.getResourceAsStream("/assets/blockoffensive/sounds.json")==null,"Reproduce Forge cross-mod resource isolation");
        var version=FMLLoader.class.getDeclaredField("versionInfo");version.setAccessible(true);
        version.set(null,new VersionInfo("47.4.0","1.20.1","20230612.114412","net.minecraftforge"));
        var mod=new ModFile(SecureJar.from(Path.of(args[1])),null,ModFileParser::modsTomlParser);
        ModList.of(List.of(mod),mod.getModInfos().stream().map(info->(ModInfo)info).toList());
        var loaded=(List<?>)isolatedMusic.getMethod("tracks").invoke(null);
        check(loaded.size()==7,"Read seven music boxes through real Forge ModFile despite hidden class resource");
        var node=new ClassNode();
        try(var in=MusicBoxesTest.class.getResourceAsStream("/dev/minestrike/skins/MusicBoxes.class")){new ClassReader(in).accept(node,0);}
        var entry=node.methods.stream().filter(m->m.name.equals("tracks")).findFirst().orElseThrow();
        check(calls(entry,false).stream().anyMatch(c->c.startsWith("net/minecraftforge/fml/ModListgetModFileById")),"Runtime catalog resolves the owning mod through Forge");
        System.out.println("PASS: Forge-isolated class resource fails as reported; real Forge ModFile resource loading succeeds");
        var tracks=MusicBoxes.tracks();
        check(tracks.stream().map(MusicBoxes.Track::name).toList().containsAll(List.of("organism","withu","jimi","sail","drum","otto","sorrow")),"Selected seven JSON music names are available");
        var added=MusicBoxes.parse(JsonParser.parseString("{\"new_kit\":{\"subtitle\":\"music.for.mvp\",\"sounds\":[\"blockoffensive:new_audio\"]},\"ordinary_sound\":{\"sounds\":[\"blockoffensive:noise\"]}}").getAsJsonObject());
        check(added.equals(List.of(new MusicBoxes.Track("blockoffensive:new_kit","new_kit"))),"Future JSON music names need no hardcoded code list");
        var catalog=SkinCatalog.JSON.fromJson(Files.readString(Path.of(args[0])),SkinCatalog.class);
        AgentSkins.append(catalog);MusicBoxes.append(catalog);MusicBoxes.append(catalog);catalog.validate();
        check(catalog.groups.size()==8,"Keep five weapons and two agent groups, add one music group");
        check(catalog.checked(Map.of(MusicBoxes.KEY,"blockoffensive:sail")).size()==1,"Music selection accepted");
        check(catalog.checked(Map.of(MusicBoxes.KEY,"blockoffensive:voice_ct_win")).isEmpty(),"Win voice is not a music box");
        Map<String,ResourceLocation> original=new LinkedHashMap<>();UUID a=UUID.randomUUID(),b=UUID.randomUUID();
        original.put(a.toString(),new ResourceLocation("blockoffensive:organism"));
        original.put(b.toString(),new ResourceLocation("custom:old_music"));
        check(!MusicBoxes.update(original,a,null)&&original.size()==2,"Default preserves existing settings");
        check(MusicBoxes.update(original,a,"blockoffensive:sail"),"Update target UUID");
        check(original.get(b.toString()).toString().equals("custom:old_music"),"Other UUID including unknown music preserved");
        check(!MusicBoxes.update(original,a,"blockoffensive:sail"),"Unchanged choice does not cause a write");
        check(MusicBoxes.update(original,a,MusicBoxes.NONE)&&!original.containsKey(a.toString()),"Explicit no music clears only target");
        var nativeData=new MVPMusicManager(original);
        var encoded=MVPMusicManager.CODEC.encodeStart(JsonOps.INSTANCE,nativeData).result().orElseThrow();
        var restored=MVPMusicManager.CODEC.parse(JsonOps.INSTANCE,encoded).result().orElseThrow();
        check(!restored.playerHasMvpMusic(a.toString())&&restored.getMvpMusic(b.toString()).toString().equals("custom:old_music"),"Native JSON codec retains unrelated player and explicit reset");
        check(MvpMusicHud.caption("blockoffensive:sail",true).equals("正在播放：sail"),"Exact playing label");
        check(MvpMusicHud.caption("blockoffensive:sail",false)==null&&MvpMusicHud.caption("blockoffensive:voice_ct_win",true)==null,"Stopped/non-music sound has no playing label");
        for(int w:new int[]{320,640,1280,1920,3840}) {
            int h=w*9/16;float scale=Math.max(.5f,w/1920f);var row=MvpMusicHud.layout(w,h,scale);
            check(row.x()>=0&&row.x()+row.width()<=w&&row.y()>=0&&row.y()+row.height()<=h,"Music row within screen");
            check(row.height()>=row.icon(),"Icon fits music row");
        }
        System.out.println("PASS: seven JSON music names, catalog selection, UUID isolation, preserve/reset, actual-playing labels and HUD bounds");
    }
    private static List<String> calls(MethodNode method,boolean skipSoundPlay) {
        List<String> result=new ArrayList<>();
        for(AbstractInsnNode ins:method.instructions)if(ins instanceof MethodInsnNode call) {
            if(call.name.contains("minestrike$")||call.owner.startsWith("org/spongepowered/asm/mixin/injection/callback/"))continue;
            if(skipSoundPlay&&call.owner.equals("net/minecraft/client/sounds/SoundManager")&&call.name.equals("m_120367_"))continue;
            result.add(call.owner+call.name+call.desc);
        }
        return result;
    }
    public static void mixins(IMixinTransformer transformer)throws Exception {
        for(String name:List.of("com.phasetranscrystal.blockoffensive.sound.MVPMusicManager",
                "com.phasetranscrystal.fpsmatch.common.client.music.FPSClientMusicManager",
                "com.phasetranscrystal.blockoffensive.client.screen.hud.CSMvpHud")) {
            byte[] before;try(var in=MusicBoxesTest.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")){before=in.readAllBytes();}
            ClassNode old=new ClassNode(),actual=new ClassNode();new ClassReader(before).accept(old,0);
            new ClassReader(transformer.transformClassBytes(name,name,before)).accept(actual,0);
            if(name.endsWith("MVPMusicManager")) {
                check(actual.interfaces.contains("dev/minestrike/skins/mixin/MvpMusicAccess"),"Native map accessor applied");
            } else {
                String target=name.endsWith("FPSClientMusicManager")?"playMusic":"renderMVPInfoPanel";
                String hook=target.equals("playMusic")?"minestrike$playing":"minestrike$music";
                int hooks=0;
                for(MethodNode m:actual.methods)for(AbstractInsnNode ins:m.instructions)
                    if(ins instanceof MethodInsnNode call&&call.name.contains(hook))hooks++;
                check(hooks>=1,"Live music/HUD hook applied");
                for(MethodNode original:old.methods) {
                    MethodNode changed=actual.methods.stream().filter(m->m.name.equals(original.name)&&m.desc.equals(original.desc)).findFirst().orElseThrow();
                    check(calls(original,true).equals(calls(changed,true)),"Original playback/HUD calls retained: "+original.name);
                }
                if(target.equals("playMusic")) {
                    var wrapper=actual.methods.stream().filter(m->m.name.contains("minestrike$playing")).findFirst().orElseThrow();
                    check(calls(wrapper,false).stream().filter(c->c.startsWith("net/minecraft/client/sounds/SoundManagerm_120367_")).count()==1,"Native sound play delegated once");
                    var stop=actual.methods.stream().filter(m->m.name.equals("stopMusic")).findFirst().orElseThrow();
                    boolean clear=false;
                    for(AbstractInsnNode ins:stop.instructions)if(ins instanceof MethodInsnNode call)clear|=call.name.contains("minestrike$stopped");
                    check(clear,"Native music stop clears playing label");
                }
            }
        }
        System.out.println("PASS: real native music-map/play/stop/HUD Mixins, unchanged volume and sound construction, no duplicate playback");
    }
}
