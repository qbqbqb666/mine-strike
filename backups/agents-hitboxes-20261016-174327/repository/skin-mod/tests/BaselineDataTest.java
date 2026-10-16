import dev.minestrike.skins.BaselineData;
import dev.minestrike.skins.MapBaseline;
import dev.minestrike.skins.MoneyCommands;
import com.phasetranscrystal.fpsmatch.core.shop.ShopData;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.nbt.*;
import java.nio.file.*;
import java.util.*;

public class BaselineDataTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var dispatcher = new CommandDispatcher<net.minecraft.commands.CommandSourceStack>();
        MapBaseline.commands(new net.minecraftforge.event.RegisterCommandsEvent(dispatcher, null, null));
        var command = dispatcher.getRoot().getChild("minestrike").getChild("baseline");
        for (String operation : List.of("save", "restore", "status"))
            check(command.getChild(operation) != null && command.getChild(operation).getCommand() != null,
                    "Registered baseline operation: " + operation);
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        var source = (net.minecraft.commands.CommandSourceStack) unsafe.allocateInstance(net.minecraft.commands.CommandSourceStack.class);
        var permission = net.minecraft.commands.CommandSourceStack.class.getDeclaredField("f_81291_"); permission.setAccessible(true);
        permission.setInt(source, 1); check(!command.canUse(source), "Non-admin baseline access rejected");
        permission.setInt(source, 2); check(command.canUse(source), "Admin baseline restore available");
        MoneyCommands.commands(new net.minecraftforge.event.RegisterCommandsEvent(dispatcher, null, null));
        check(dispatcher.getRoot().getChild("minestrike").getChild("baseline") == command, "Money command registration preserves baseline branch");
        var money = dispatcher.getRoot().getChild("minestrike").getChild("money");
        permission.setInt(source, 1); check(!money.canUse(source), "Non-admin money access rejected");
        permission.setInt(source, 2); check(money.canUse(source), "Admin money command available");
        var players = (ArgumentCommandNode<?, ?>) money.getChild("add").getChild("players");
        check(players.getType() instanceof net.minecraft.commands.arguments.EntityArgument, "Native player/selector argument registered");
        var amount = (ArgumentCommandNode<?, ?>) players.getChild("amount");
        check(amount.getCommand() != null, "Money command execution registered");
        amount.getType().parse(new StringReader("12500"));
        amount.getType().parse(new StringReader("2147483647"));
        for (String invalid : List.of("0", "-1", "2147483648")) {
            boolean rejected = false;
            try { amount.getType().parse(new StringReader(invalid)); } catch (CommandSyntaxException expected) { rejected = true; }
            check(rejected, "Reject invalid money amount: " + invalid);
        }
        var funds = (ShopData<?>) unsafe.allocateInstance(ShopData.class);
        var balance = ShopData.class.getDeclaredField("money"); balance.setAccessible(true); balance.setInt(funds, 800);
        check(MoneyCommands.addMoney(funds, 12500) == 12500 && funds.getMoney() == 13300, "Native shop funds increase");
        check(MoneyCommands.addMoney(funds, Integer.MAX_VALUE) == 2700 && funds.getMoney() == 16000, "Native cap retained without integer overflow");
        check(MoneyCommands.addMoney(funds, 12500) == 0 && funds.getMoney() == 16000, "Money cap remains stable");
        System.out.println("PASS: money command registration, existing baseline branch, admin permission, selectors, amounts and native capped funds");
        var bounds = new BaselineData.Bounds(-17, 4, -2, 3, 2, 4);
        Set<String> positions = new HashSet<>();
        for (int i = 0; i < bounds.volume(); i++) {
            int x = bounds.px(i), y = bounds.py(i), z = bounds.pz(i);
            check(bounds.contains(x, y, z), "Index inside bounds");
            check(positions.add(x + ":" + y + ":" + z), "No duplicate position");
        }
        check(positions.size() == 24 && !bounds.contains(-14, 4, -2), "Inclusive region and outside isolation");
        ListTag palette = new ListTag();
        for (String name : List.of("minecraft:air", "minecraft:stone", "minecraft:chest")) {
            CompoundTag tag = new CompoundTag(); tag.m_128359_("Name", name); palette.add(tag);
        }
        ListTag tiles = new ListTag(); CompoundTag chest = new CompoundTag();
        chest.m_128359_("id", "minecraft:chest"); chest.m_128405_("x", -17); chest.m_128405_("y", 4); chest.m_128405_("z", -2);
        chest.m_128359_("CustomName", "test chest");
        ListTag items = new ListTag(); CompoundTag item = new CompoundTag();
        item.m_128359_("id", "minecraft:diamond"); item.m_128344_("Count", (byte) 3); item.m_128344_("Slot", (byte) 0);
        items.add(item); chest.m_128365_("Items", items); tiles.add(chest);
        ListTag entities = new ListTag(); CompoundTag vehicle = new CompoundTag();
        vehicle.m_128359_("id", "superbwarfare:test_vehicle"); vehicle.m_128405_("Health", 200);
        CompoundTag modData = new CompoundTag(); modData.m_128405_("Fuel", 100); modData.m_128405_("Ammo", 30);
        vehicle.m_128365_("ForgeData", modData); entities.add(vehicle);
        int[] blocks = new int[bounds.volume()]; blocks[0] = 2; blocks[1] = 1;
        BaselineData first = new BaselineData("dust2", "minecraft:overworld", bounds, palette, blocks, tiles, entities);
        Path directory = Files.createTempDirectory("mine-strike-baseline-test-"); Path file = directory.resolve("map.nbt");
        try {
            first.write(file); BaselineData read = BaselineData.read(file);
            check(read.bounds().equals(bounds) && read.map().equals("dust2"), "Metadata round trip");
            check(Arrays.equals(read.blocks(), blocks), "Air and placed blocks retained");
            check(read.blockEntities().equals(tiles), "Container inventory and metadata retained");
            check(read.entities().equals(entities), "Mod vehicle state retained");
            byte[] initial = Files.readAllBytes(file);
            int[] changed = blocks.clone(); changed[2] = 1;
            new BaselineData("dust2", "minecraft:overworld", bounds, palette, changed, tiles, entities).write(file);
            check(Arrays.equals(initial, Files.readAllBytes(directory.resolve("map.nbt.previous"))), "Previous baseline backup");
            check(BaselineData.read(file).blocks()[2] == 1, "Updating baseline takes effect");
            byte[] saved = Files.readAllBytes(file); boolean rejected = false;
            try { new BaselineData("dust2", "minecraft:overworld", bounds, palette, new int[]{99}, tiles, entities).write(file); }
            catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected && Arrays.equals(saved, Files.readAllBytes(file)), "Invalid capture does not replace good baseline");
            try (var files = Files.list(directory)) { check(files.noneMatch(p -> p.toString().endsWith(".tmp")), "No leftover temporary files"); }
            CompoundTag corrupt = first.tag(); corrupt.m_128405_("Version", 99); NbtIo.m_128944_(corrupt, directory.resolve("bad.nbt").toFile());
            rejected = false; try { BaselineData.read(directory.resolve("bad.nbt")); } catch (java.io.IOException expected) { rejected = true; }
            check(rejected, "Unsupported format rejected");
            int[] badIds = blocks.clone(); badIds[2] = 99;
            rejected = false; try { new BaselineData("dust2", "minecraft:overworld", bounds, palette, badIds, tiles, entities).validate(); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Out of range palette rejected");
            rejected = false; try { new BaselineData.Bounds(0, 0, 0, 1000, 1000, 1000); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "Oversized region rejected");
        } finally { try (var files = Files.walk(directory)) { for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); } }
        System.out.println("PASS: save/restore/status command registration and permission level 2; region isolation/indexing, compressed baseline, air/blocks, containers, vehicle NBT, atomic update, previous backup, invalid-data protection");
    }
}
