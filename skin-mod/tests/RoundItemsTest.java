import dev.minestrike.skins.RoundItems;
import net.minecraft.nbt.*;
import java.io.*;
import java.util.*;

public class RoundItemsTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Set<String> active = new HashSet<>(List.of("map-a-round-1", "map-b-round-1"));
        CompoundTag normal = new CompoundTag(); normal.m_128359_("GunId", "tacz:ak47");
        CompoundTag before = normal.m_6426_();
        check(!RoundItems.expired(normal, Set.of()) && !RoundItems.expired(null, Set.of()), "Unmarked items survive cleanup");
        check(normal.equals(before), "Checking normal items does not change NBT");
        CompoundTag template = normal.m_6426_(); RoundItems.configure(template, true);
        check(template.m_128471_(RoundItems.ONLY), "Per-item flag saved");
        check(!RoundItems.expired(template, Set.of()), "Saved shop template has no runtime expiry");
        CompoundTag issued = template.m_6426_(); issued.m_128359_(RoundItems.EPOCH, "map-a-round-1");
        check(!RoundItems.expired(issued, active), "Issued items survive in their own round");
        CompoundTag handedOver = issued.m_6426_();
        active.remove("map-a-round-1");
        check(RoundItems.expired(issued, active) && RoundItems.expired(handedOver, active), "Transferred/dropped copies expire with original round");
        CompoundTag otherMap = template.m_6426_(); otherMap.m_128359_(RoundItems.EPOCH, "map-b-round-1");
        check(!RoundItems.expired(otherMap, active), "Ending one map leaves other map's items intact");
        active.add("map-a-round-2");
        check(RoundItems.expired(issued, active), "Old item cannot become valid in next round");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        NbtIo.m_128947_(issued, bytes);
        CompoundTag rejoined = NbtIo.m_128939_(new ByteArrayInputStream(bytes.toByteArray()));
        check(RoundItems.expired(rejoined, active), "Saved offline/unloaded item expires when loaded later");
        check(RoundItems.expired(otherMap, Set.of()), "Restart does not revive past issued items");
        CompoundTag copiedForEditor = issued.m_6426_(); RoundItems.configure(copiedForEditor, true);
        check(!copiedForEditor.m_128441_(RoundItems.EPOCH) && copiedForEditor.m_128471_(RoundItems.ONLY), "Editor copy retains policy without old runtime epoch");
        RoundItems.configure(copiedForEditor, false);
        check(copiedForEditor.equals(normal), "Disabling restores original NBT without changing item properties");
        check(!RoundItems.expired(template, Set.of()) && normal.equals(before), "Cleanup never edits persistent templates or unrelated items");
        System.out.println("PASS: round-only default, template safety, per-map expiry, transfer/drop copies, offline NBT, new rounds, restart and disabling");
    }
}
