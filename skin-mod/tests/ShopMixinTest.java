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
    public static void main(String[] args)throws Exception {
        MixinBootstrap.init();
        MixinEnvironment.getDefaultEnvironment().setSide(MixinEnvironment.Side.CLIENT);
        MixinEnvironment.getDefaultEnvironment().setOption(MixinEnvironment.Option.DEBUG_VERIFY,true);
        Mixins.addConfiguration("minestrike-skins.mixins.json");
        var constructor=Class.forName("org.spongepowered.asm.mixin.transformer.MixinTransformer").getDeclaredConstructor();constructor.setAccessible(true);
        IMixinTransformer transformer=(IMixinTransformer)constructor.newInstance();
        String prefix="com.phasetranscrystal.";
        String[] targets={"blockoffensive.map.shop.ItemType","blockoffensive.map.CSGameMap","fpsmatch.core.shop.ShopData","fpsmatch.core.shop.FPSMShop","fpsmatch.core.shop.slot.ShopSlot",
                "fpsmatch.common.client.screen.EditorShopContainer","fpsmatch.common.client.screen.EditShopSlotMenu",
                "fpsmatch.common.client.screen.VanillaGuiRegister","fpsmatch.common.packet.shop.OpenEditorC2SPacket","fpsmatch.common.item.ShopEditTool", "blockoffensive.client.key.OpenShopKey"};
        Map<String,byte[]> transformed=new HashMap<>();
        for(String target:targets){String name=prefix+target;byte[] before=resource(name),after=transformer.transformClassBytes(name,name,before);
            if(Arrays.equals(before,after))throw new AssertionError("Mixin did not apply: "+name);transformed.put(name,after);
        }
        ClassNode slotNode = new ClassNode();
        new ClassReader(transformed.get(prefix + "fpsmatch.core.shop.slot.ShopSlot")).accept(slotNode, 0);
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
        System.out.println("PASS: real Mixin application on eleven shop/round targets; sixth enum initialization and valueOf persistence key");
    }
}
