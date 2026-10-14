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
for name, extra in [("SkinDataTest", [str(root / "src/main/resources/default-skins.json")]), ("SkinNetworkTest", [])]:
    subprocess.run(["java", "-ea", "-cp", classpath + os.pathsep + str(tests), name, *extra], cwd=runtime, check=True)

catalog = json.loads((root / "src/main/resources/default-skins.json").read_text(encoding="utf-8"))
ids = {s["id"] for group in catalog["groups"] for s in group["skins"]}
original = set(re.findall(r"(?:mcs2|cs2_wt):[a-z0-9_]+", (pack / "skin.txt").read_text(encoding="utf-8")))
assert ids == original, "Bundled catalog does not match skin.txt"
with zipfile.ZipFile(root / "build/libs/mine-strike-skins-0.1.0.jar") as jar:
    assert jar.testzip() is None
    assert json.loads(jar.read("pack.mcmeta"))["pack"]["pack_format"] == 15
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
print("PASS: skin.txt coverage, packaged resources, Java 17, exact Mixin target signatures")
