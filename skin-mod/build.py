from pathlib import Path
import argparse
import os
import shutil
import subprocess
import zipfile

parser = argparse.ArgumentParser(description="Build against the installed Minecraft 1.20.1 Forge SRG runtime")
parser.add_argument("--libraries", type=Path, required=True, help="Launcher .minecraft/libraries directory")
args = parser.parse_args()
root = Path(__file__).resolve().parent
pack = root.parent
patched = args.libraries / "net/minecraftforge/forge/1.20.1-47.4.0/forge-1.20.1-47.4.0-client.jar"
if not patched.is_file():
    raise SystemExit("Missing installed Forge 47.4.0 client runtime: " + str(patched))
jars = [patched] + sorted(args.libraries.rglob("*.jar")) + sorted((pack / "mods").glob("*.jar"))
classes = root / "build/classes"
if classes.exists():
    shutil.rmtree(classes)
classes.mkdir(parents=True, exist_ok=True)
sources = sorted((root / "src/main/java").rglob("*.java"))
subprocess.run(["javac", "--release", "17", "-encoding", "UTF-8", "-proc:none", "-classpath",
                os.pathsep.join(str(jar) for jar in jars), "-d", str(classes), *map(str, sources)], check=True)
output = root / "build/libs/mine-strike-skins-0.1.0.jar"
output.parent.mkdir(parents=True, exist_ok=True)
with zipfile.ZipFile(output, "w", zipfile.ZIP_DEFLATED) as jar:
    jar.writestr("META-INF/MANIFEST.MF", "Manifest-Version: 1.0\r\nMixinConfigs: minestrike-skins.mixins.json\r\n\r\n")
    for base in [classes, root / "src/main/resources"]:
        for file in sorted(base.rglob("*")):
            if file.is_file():
                jar.write(file, file.relative_to(base).as_posix())
print(output)
