import dev.minestrike.skins.ShopAmmo;
import com.atsuishio.superbwarfare.data.gun.value.IntValue;
import net.minecraft.nbt.*;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;

public class ShopAmmoTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static ClassNode node(String name) throws Exception {
        var result = new ClassNode();
        try (var in = ShopAmmoTest.class.getClassLoader().getResourceAsStream(name.replace('.', '/') + ".class")) {
            new ClassReader(in).accept(result, 0);
        }
        return result;
    }
    private static MethodNode method(ClassNode type, String name) {
        return type.methods.stream().filter(m -> m.name.equals(name)).findFirst().orElseThrow();
    }
    private static boolean calls(MethodNode method, String owner, String name) {
        for (var ins : method.instructions) if (ins instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) return true;
        return false;
    }
    public static void main(String[] args) throws Exception {
        for (boolean chamber : new boolean[]{false, true}) for (int capacity : new int[]{0, 1, 2, 12, 30, 100})
            for (int total : new int[]{0, 1, 2, 3, 29, 30, 31, 120, 65536, 1000000}) {
                var load = ShopAmmo.split(total, capacity, chamber);
                check(load.magazine() + load.reserve() + (load.chamber() ? 1 : 0) == total, "No ammo lost or created, including chamber");
                check(load.magazine() >= 0 && load.reserve() >= 0 && load.magazine() + (load.chamber() ? 1 : 0) <= capacity, "Native capacity respected");
                check(load.chamber() == (chamber && capacity > 0 && total > 0), "Empty gun never receives a chamber round");
            }
        check(ShopAmmo.split(30, 1, false).equals(new ShopAmmo.Load(1,29,false)), "BOCEK 30 = 1 + 29");
        check(ShopAmmo.split(2, 1, false).equals(new ShopAmmo.Load(1,1,false)), "RPG 2 = 1 + 1");
        check(ShopAmmo.split(1, 30, true).equals(new ShopAmmo.Load(0,0,true)), "One TaCZ round stays one, rather than a full magazine");
        check(ShopAmmo.split(30, 0, false).equals(new ShopAmmo.Load(0,30,false)), "Backpack-fed weapons put entire total into reserve");
        for (int invalid : new int[]{-1, 1000001}) {
            try { ShopAmmo.split(invalid, 30, true); throw new AssertionError("Reject invalid totals"); }
            catch (IllegalArgumentException expected) {}
        }
        var tag = new CompoundTag(); tag.m_128405_(ShopAmmo.TOTAL, 1000000);
        var out = new ByteArrayOutputStream(); NbtIo.m_128947_(tag, out);
        check(NbtIo.m_128939_(new ByteArrayInputStream(out.toByteArray())).m_128451_(ShopAmmo.TOTAL) == 1000000, "Total persists as full integer NBT, without menu short truncation");
        var nativeTag = new CompoundTag();
        var loaded = new IntValue(nativeTag, "Ammo", 0);
        var reserve = new IntValue(nativeTag, "VirtualAmmo", 0);
        var bocek = ShopAmmo.split(30, 1, false);
        loaded.set(bocek.magazine()); reserve.set(bocek.reserve());
        int shots = 0;
        while (loaded.get() > 0) {
            loaded.add(-1); shots++;
            if (reserve.get() > 0) { reserve.add(-1); loaded.add(1); }
        }
        check(shots == 30 && reserve.get() == 0 && nativeTag.m_128451_("Ammo") == 0
                && nativeTag.m_128451_("VirtualAmmo") == 0, "Installed native IntValue fields persist and exhaust thirty single-shot loads");
        var adapter = node("dev.minestrike.skins.ShopAmmo");
        var configure = method(adapter, "configure");
        for (String name : List.of("setCurrentAmmoCount", "setBulletInBarrel", "setDummyAmmoAmount", "setMaxDummyAmmoAmount"))
            check(calls(configure, "com/tacz/guns/api/item/IGun", name), "TaCZ native setter " + name);
        int superbWrites = 0;
        for (var ins : configure.instructions) if (ins instanceof MethodInsnNode call && call.owner.equals("com/atsuishio/superbwarfare/data/gun/value/IntValue") && call.name.equals("set")) superbWrites++;
        check(superbWrites == 2, "Superb uses native loaded and virtual ammo setters");
        var prepare = method(adapter, "prepare");
        check(calls(prepare, "net/minecraft/world/item/ItemStack", "m_41777_"), "Preparing shop template must copy before resetting ammo");
        var save = method(node("dev.minestrike.skins.ShopExpansion"), "save");
        check(calls(save, "dev/minestrike/skins/ShopAmmo", "configure"), "Save actually invokes unified adapter");
        check(!calls(save, "com/phasetranscrystal/fpsmatch/util/FPSMUtil", "setTotalDummyAmmo"), "Remove TaCZ-only save path");
        var screen = node("dev.minestrike.skins.ShopItemEditorScreen");
        for (String name : List.of("m_7856_", "receiveItems")) check(calls(method(screen, name), "dev/minestrike/skins/ShopAmmo", "total"), "Initial and replacement-item editor values use unified totals");
        var nativeData = node("com.atsuishio.superbwarfare.data.gun.GunData");
        check(calls(method(nativeData, "reloadAmmo"), "com/atsuishio/superbwarfare/data/gun/GunData", "consumeBackupAmmo"), "Native reload consumes reserve");
        var consume = method(nativeData, "consumeBackupAmmo");
        check(calls(consume, "com/atsuishio/superbwarfare/data/gun/value/IntValue", "add"), "Native consumption decrements virtual reserve");
        System.out.println("PASS: BOCEK/RPG/TaCZ total conservation, chamber/zero/backpack bounds, integer persistence, editor/save adapter and installed native reload contracts");
    }
}
