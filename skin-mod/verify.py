from pathlib import Path
import argparse
import os
import subprocess
import zipfile
import json
import re

parser = argparse.ArgumentParser(description="Verify skin data, packets and exact BlockOffensive integration targets")
parser.add_argument("--libraries", type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent
pack = root.parent
subprocess.run(["python3", str(root / "build.py"), "--libraries", str(args.libraries)], check=True)
patched = args.libraries / "net/minecraftforge/forge/1.20.1-47.4.0/forge-1.20.1-47.4.0-client.jar"
jars = [patched] + sorted(args.libraries.rglob("*.jar")) + sorted((pack / "mods").glob("*.jar"))
classpath = os.pathsep.join([str(root / "build/classes"), *map(str, jars)])
tests = root / "build/test-classes"
tests.mkdir(parents=True, exist_ok=True)
runtime = root / "build/test-runtime"
runtime.mkdir(parents=True, exist_ok=True)
subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-proc:none", "-cp", classpath,
                "-d", str(tests), *map(str, sorted((root / "tests").glob("*.java")))], check=True)
for name, extra in [("SkinDataTest", [str(root / "src/main/resources/default-skins.json")]), ("SkinNetworkTest", []), ("PreviewGeometryTest", []), ("MatchVotesTest", []), ("PageVisibilityTest", []), ("BaselineDataTest", []), ("ShopRowsTest", []), ("ShopLifecycleTest", []), ("InventoryPlanTest", []), ("RoundItemsTest", []), ("ShopDeploymentsTest", [])]:
    subprocess.run(["java", "-ea", "-cp", classpath + os.pathsep + str(tests), name, *extra], cwd=runtime, check=True)

catalog = json.loads((root / "src/main/resources/default-skins.json").read_text(encoding="utf-8"))
ids = {s["id"] for group in catalog["groups"] for s in group["skins"]}
original = set(re.findall(r"(?:mcs2|cs2_wt):[a-z0-9_]+", (pack / "skin.txt").read_text(encoding="utf-8")))
assert ids == original, "Bundled catalog does not match skin.txt"
with zipfile.ZipFile(root / "build/libs/mine-strike-skins-0.1.0.jar") as jar:
    assert jar.testzip() is None
    assert json.loads(jar.read("pack.mcmeta"))["pack"]["pack_format"] == 15
    assert json.loads(jar.read("assets/minestrike_skins/font/pixel.json"))["providers"][1]["id"] == "minecraft:include/unifont"
    assert b"MixinConfigs: minestrike-skins.mixins.json" in jar.read("META-INF/MANIFEST.MF")
    for name in jar.namelist():
        if name.endswith(".class"):
            assert int.from_bytes(jar.read(name)[6:8], "big") == 61, "Not Java 17 bytecode"

bytecode = subprocess.run(["javap", "-classpath", classpath, "-c", "-p",
                          "com.phasetranscrystal.blockoffensive.map.CSGameMap"], capture_output=True, text=True, check=True).stdout
kit = "private void lambda$giveAllPlayersKits$73(net.minecraft.server.level.ServerPlayer, com.phasetranscrystal.fpsmatch.core.map.BaseTeam);"
assert kit in bytecode, "BlockOffensive kit method changed"
body = bytecode.split(kit, 1)[1].split("\n  private ", 1)[0]
assert body.count("Method net/minecraft/world/item/ItemStack.m_41777_:") == 1, "Kit copy target changed"
assert "public void startGame();" in bytecode and "public void resetGame();" in bytecode
cleanup = "private void lambda$cleanupMap$66(boolean, com.phasetranscrystal.fpsmatch.core.data.PlayerData, net.minecraft.server.level.ServerPlayer);"
assert cleanup in bytecode, "BlockOffensive round cleanup method changed"
cleanup_body = bytecode.split(cleanup, 1)[1].split("\n  private ", 1)[0]
assert cleanup_body.count("Method givePlayerKits:") == 2, "Death and team-switch kit paths changed"
assert "PlayerData.isLiving:()Z" in cleanup_body, "Dead player branch missing"
interface = subprocess.run(["javap", "-classpath", classpath, "-c", "-p",
                           "com.phasetranscrystal.fpsmatch.core.map.GiveStartKitsMap"], capture_output=True, text=True, check=True).stdout
single_kit = "private void lambda$givePlayerKits$2(net.minecraft.server.level.ServerPlayer, com.phasetranscrystal.fpsmatch.core.map.BaseTeam);"
assert single_kit in interface
single_body = interface.split(single_kit, 1)[1].split("\n  private ", 1)[0]
assert "Inventory.m_6211_:()V" in single_body, "Kit application must clear inventory before recoloring issued items"
print("PASS: skin.txt coverage, resources, Java 17, initial/death/team-switch kit integration targets")

for target in ["private void autoStartLogic();", "public void checkRoundVictory();", "public void checkBlastingVictory();"]:
    assert target in bytecode, "Lobby integration target changed: " + target
team_code = subprocess.run(["javap", "-classpath", classpath, "-c", "-p",
                           "com.phasetranscrystal.fpsmatch.core.map.BaseTeam"], capture_output=True, text=True, check=True).stdout
for target in ["public boolean randomSpawnPoints();", "public int getPlayerLimit();", "public int getRemainingLimit();"]:
    assert target in team_code, "Team integration target changed: " + target
with zipfile.ZipFile(root / "build/libs/mine-strike-skins-0.1.0.jar") as jar:
    assert "BaseTeamMixin" in json.loads(jar.read("minestrike-skins.mixins.json"))["mixins"]
    background = jar.read("assets/minestrike_skins/textures/gui/match-background.png")
    assert background.startswith(b"\x89PNG\r\n\x1a\n")
print("PASS: lobby automatic-start, single-side elimination, shared-spawn and team-capacity integration targets; match background")

assert "public void startNewRound();" in bytecode
assert "private void roundVictory(com.phasetranscrystal.fpsmatch.core.map.BaseTeam, com.phasetranscrystal.blockoffensive.map.CSGameMap$WinnerReason);" in bytecode
assert "public void cleanupMap();" in bytecode
base_code = subprocess.run(["javap", "-classpath", classpath, "-c", "-p",
                           "com.phasetranscrystal.fpsmatch.core.map.BaseMap"], capture_output=True, text=True, check=True).stdout
assert "public final void mapTick();" in base_code
with zipfile.ZipFile(root / "build/libs/mine-strike-skins-0.1.0.jar") as jar:
    assert "BaseMapMixin" in json.loads(jar.read("minestrike-skins.mixins.json"))["mixins"]
print("PASS: match-start/round-start reset and suspended map-tick integration targets")


def inspect_class(name):
    return subprocess.run(["javap", "-classpath", classpath, "-c", "-p", name],
                          capture_output=True, text=True, check=True).stdout

editor = inspect_class("com.phasetranscrystal.fpsmatch.common.client.screen.EditorShopContainer")
constructor = editor.split("public com.phasetranscrystal.fpsmatch.common.client.screen.EditorShopContainer(int, net.minecraft.world.entity.player.Inventory, net.minecraft.world.item.ItemStack);", 1)[1].split("private void addPlayerInventory", 1)[0]
assert constructor.count("// Method getAllSlots:()Ljava/util/List;") == 1
assert constructor.count("// class net/minecraftforge/items/SlotItemHandler") == 1
assert constructor.count("iconst_5") == 4, "Editor row/column/index constants changed"
assert "sipush        163" in constructor
assert "bipush        24" in editor.split("public void m_150399_", 1)[1].split("public void m_6877_", 1)[0]
assert "private final net.minecraft.world.item.ItemStack guiItemStack;" in editor
slot_editor = inspect_class("com.phasetranscrystal.fpsmatch.common.client.screen.EditShopSlotMenu")
assert "irem" in slot_editor and "idiv" in slot_editor and "replaceDefaultShopData" in slot_editor
opener = inspect_class("com.phasetranscrystal.fpsmatch.common.packet.shop.OpenEditorC2SPacket")
assert "private static void handleOpenEditor(net.minecraft.server.level.ServerPlayer);" in opener
tool = inspect_class("com.phasetranscrystal.fpsmatch.common.item.ShopEditTool")
tool_use = tool.split(" m_7203_(", 1)[1].split("public static void onLeftClickEmpty", 1)[0]
assert tool_use.count("Method net/minecraftforge/network/NetworkHooks.openScreen:") == 1
tool_mixin = inspect_class("dev.minestrike.skins.mixin.ShopToolOpenMixin")
assert "Method net/minecraft/world/entity/player/Player.m_21120_:" in tool_mixin
assert "Method dev/minestrike/skins/ShopExpansion.prepareTool:" in tool_mixin
category = inspect_class("com.phasetranscrystal.blockoffensive.map.shop.ItemType")
assert "ItemType[] $VALUES;" in category
assert "private com.phasetranscrystal.blockoffensive.map.shop.ItemType(int, boolean);" in category
bar = inspect_class("com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen$TypeBarLayout")
assert bar.split("public void", 1)[0].count("iconst_5") == 1
window = inspect_class("com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen$WindowLayout")
assert window.count("// float 550.0f") == 1
screen = inspect_class("com.phasetranscrystal.blockoffensive.client.screen.CSGameShopScreen")
assert "public static float calculateScaleFactor(int, int);" in screen
registration = inspect_class("com.phasetranscrystal.fpsmatch.common.client.screen.VanillaGuiRegister")
assert registration.count("Method net/minecraft/client/gui/screens/MenuScreens.m_96206_:") == 2
with zipfile.ZipFile(root / "build/libs/mine-strike-skins-0.1.0.jar") as jar:
    config = json.loads(jar.read("minestrike-skins.mixins.json"))
    for mixin in ["ShopDataMixin", "ShopBundleSlotMixin", "ShopDeployItemMixin", "FPSMShopMixin", "ShopEditorContainerMixin", "ShopEditorAccess", "ShopOpenEditorMixin", "ShopToolOpenMixin", "ShopCategoryMixin", "ShopSlotMenuAccess", "ShopSlotMenuMixin"]:
        assert mixin in config["mixins"]
    for mixin in ["ShopEditorScreenFactoryMixin", "ShopPurchaseOpenMixin"]:
        assert mixin in config["client"]
print("PASS: exact shop row, edit-index, pre-open sync, screen registration and client-only mixin targets")

services = tests / "META-INF/services"
services.mkdir(parents=True, exist_ok=True)
(services / "org.spongepowered.asm.service.IMixinService").write_text("ShopMixinTest$Service\n")
(services / "org.spongepowered.asm.service.IGlobalPropertyService").write_text("ShopMixinTest$Properties\n")
mixin_runtime = [args.libraries / "org/spongepowered/mixin/0.8.5/mixin-0.8.5.jar"]
mixin_runtime += sorted((args.libraries / "org/ow2/asm").glob("*/9.7.1/*.jar"))
mixin_classpath = os.pathsep.join([str(tests), str(root / "src/main/resources"), *map(str, mixin_runtime), classpath])
subprocess.run(["java", "-ea", "-cp", mixin_classpath, "ShopMixinTest"], cwd=runtime, check=True)
