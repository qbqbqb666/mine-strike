import dev.minestrike.skins.ShopDeployments;
import com.phasetranscrystal.fpsmatch.core.data.AreaData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

public class ShopDeploymentsTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        CompoundTag tag = new CompoundTag(); tag.m_128359_("CustomItemData", "keep");
        CompoundTag original = tag.m_6426_();
        check(!ShopDeployments.issued(tag), "Ordinary items cannot deploy");
        CompoundTag issuedWithoutFlag = tag.m_6426_(); issuedWithoutFlag.m_128359_(ShopDeployments.ISSUED, "dust2");
        check(!ShopDeployments.issued(issuedWithoutFlag), "Issued metadata alone cannot enable deployment");
        ShopDeployments.configure(tag, true);
        check(!ShopDeployments.issued(tag), "Editor flag alone must not grant runtime permission");
        tag.m_128359_(ShopDeployments.ISSUED, "dust2");
        check(ShopDeployments.issued(tag), "A purchased item carries deployment permission");
        ShopDeployments.configure(tag, true);
        check(!ShopDeployments.issued(tag), "Saving a draft strips old issued permission");
        ShopDeployments.configure(tag, false);
        check(tag.equals(original), "Disabling preserves unrelated NBT");
        AreaData map = new AreaData(new BlockPos(-20, 0, -20), new BlockPos(20, 30, 20));
        AreaData bomb = new AreaData(new BlockPos(5, 5, 5), new BlockPos(10, 9, 10));
        var spawns = List.of(new BlockPos(0, 5, 0));
        check(ShopDeployments.allowedPosition(new BlockPos(-10, 5, 0), map, List.of(bomb), spawns), "Clear position inside map is eligible");
        check(!ShopDeployments.allowedPosition(new BlockPos(21, 5, 0), map, List.of(bomb), spawns), "Outside map is blocked");
        check(!ShopDeployments.allowedPosition(new BlockPos(0, 31, 0), map, List.of(bomb), spawns), "Above map height is blocked");
        check(!ShopDeployments.allowedPosition(new BlockPos(5, 5, 5), map, List.of(bomb), spawns), "Bomb area boundary is protected");
        check(!ShopDeployments.allowedPosition(new BlockPos(10, 9, 10), map, List.of(bomb), spawns), "Opposite bomb area corner is protected");
        check(!ShopDeployments.allowedPosition(new BlockPos(2, 8, -2), map, List.of(bomb), spawns), "Spawn protection includes two blocks horizontally and three vertically");
        check(ShopDeployments.allowedPosition(new BlockPos(3, 5, 0), map, List.of(bomb), spawns), "Outside spawn margin is eligible");
        check(ShopDeployments.allowedPosition(new BlockPos(-20, 0, -20), map, List.of(bomb), spawns), "Inclusive map boundary");
        AreaData reversed = new AreaData(new BlockPos(10, 9, 10), new BlockPos(5, 5, 5));
        check(!ShopDeployments.allowedPosition(new BlockPos(10, 9, 10), map, List.of(reversed), spawns), "Reversed area corners retain protection");
        check(ShopDeployments.allowedPosition(new BlockPos(20, 30, 20), map, List.of(bomb), spawns), "Maximum map corner included");
        check(!ShopDeployments.nearSpawn(new BlockPos(Integer.MAX_VALUE, 0, 0), new BlockPos(Integer.MIN_VALUE, 0, 0)), "Distance calculation cannot overflow");
        System.out.println("PASS: deployment defaults, purchased-only permission, template NBT isolation, map boundaries, bomb areas and spawn protection");
    }
}
