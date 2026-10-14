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
for name, extra in [("SkinDataTest", [str(root / "src/main/resources/default-skins.json")]), ("SkinNetworkTest", []), ("PreviewGeometryTest", []), ("MatchVotesTest", []), ("PageVisibilityTest", [])]:
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
