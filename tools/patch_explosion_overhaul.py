"""Explosion Overhaul 0.2.3.0: auto-accept world scan/load on player login.

Usage: python3 tools/patch_explosion_overhaul.py
Only BlockIndexManager.class changes; effect settings and other JAR entries stay intact.
The original JAR is backed up outside the repository before replacement.
"""

import hashlib
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import zipfile


ROOT = Path(__file__).resolve().parents[1]
JAR = ROOT / "mods/Explosion-Overhaul-0.2.3.0-forge-.jar"
LIBRARIES = Path.home() / "Library/Application Support/minecraft/libraries"
ORIGINAL_SHA256 = "feac2ed262604f01e74609acdeba83c9b0bbe2b602ba0da40196fb1041f2c64f"
ENTRY = "com/vinlanx/explosionoverhaul/BlockIndexManager.class"

JAVA = r'''
import java.nio.file.*;
import java.lang.reflect.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

public class PatchExplosion implements Opcodes {
    static final String OWNER = "com/vinlanx/explosionoverhaul/BlockIndexManager";
    static final String HELPER = "mineStrikeAutoAcceptScan";

    static MethodNode helper() {
        MethodNode m = new MethodNode(ACC_PRIVATE | ACC_STATIC, HELPER, "()V", null, null);
        Label scan = new Label(), end = new Label();
        m.visitCode();
        m.visitFieldInsn(GETSTATIC, OWNER, "waitingForLoadDecision", "Z");
        m.visitJumpInsn(IFEQ, scan);
        m.visitMethodInsn(INVOKESTATIC, OWNER, "loadExistingData", "()V", false);
        m.visitInsn(RETURN);
        m.visitLabel(scan);
        m.visitFrame(F_SAME, 0, null, 0, null);
        m.visitFieldInsn(GETSTATIC, OWNER, "waitingForUserInput", "Z");
        m.visitJumpInsn(IFEQ, end);
        m.visitMethodInsn(INVOKESTATIC, OWNER, "startManualScan", "()V", false);
        m.visitLabel(end);
        m.visitFrame(F_SAME, 0, null, 0, null);
        m.visitInsn(RETURN);
        m.visitMaxs(1, 0);
        m.visitEnd();
        return m;
    }

    static AbstractInsnNode previous(AbstractInsnNode n) {
        do { n = n.getPrevious(); } while (n != null && n.getOpcode() < 0);
        return n;
    }

    static void check(ClassNode c) throws Exception {
        for (MethodNode m : c.methods) {
            if ((m.access & (ACC_ABSTRACT | ACC_NATIVE)) == 0)
                new Analyzer<BasicValue>(new BasicVerifier()).analyze(c.name, m);
        }
    }

    static void stateTests() throws Exception {
        ClassNode c = new ClassNode();
        c.version = V17; c.access = ACC_PUBLIC; c.name = OWNER;
        c.superName = "java/lang/Object";
        for (String f : new String[]{"waitingForLoadDecision", "waitingForUserInput"})
            c.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, f, "Z", null, null));
        for (String f : new String[]{"loads", "scans"})
            c.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, f, "I", null, null));
        for (String name : new String[]{"loadExistingData", "startManualScan"}) {
            String count = name.equals("loadExistingData") ? "loads" : "scans";
            String flag = name.equals("loadExistingData") ? "waitingForLoadDecision" : "waitingForUserInput";
            MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, name, "()V", null, null);
            m.visitFieldInsn(GETSTATIC, OWNER, count, "I");
            m.visitInsn(ICONST_1); m.visitInsn(IADD);
            m.visitFieldInsn(PUTSTATIC, OWNER, count, "I");
            m.visitInsn(ICONST_0);
            m.visitFieldInsn(PUTSTATIC, OWNER, flag, "Z");
            m.visitInsn(RETURN); m.visitMaxs(2, 0); c.methods.add(m);
        }
        c.methods.add(helper());
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        c.accept(writer);
        byte[] bytes = writer.toByteArray();
        Class<?> test = new ClassLoader() {
            Class<?> define() { return defineClass(null, bytes, 0, bytes.length); }
        }.define();
        Method accept = test.getDeclaredMethod(HELPER); accept.setAccessible(true);
        for (int state = 0; state < 4; state++) {
            boolean load = (state & 1) != 0, scan = (state & 2) != 0;
            test.getField("waitingForLoadDecision").setBoolean(null, load);
            test.getField("waitingForUserInput").setBoolean(null, scan);
            test.getField("loads").setInt(null, 0);
            test.getField("scans").setInt(null, 0);
            accept.invoke(null);
            int expectedLoad = load ? 1 : 0, expectedScan = !load && scan ? 1 : 0;
            if (test.getField("loads").getInt(null) != expectedLoad ||
                test.getField("scans").getInt(null) != expectedScan)
                throw new AssertionError("Wrong automatic decision: " + state);
            if (state != 3) {
                accept.invoke(null);
                if (test.getField("loads").getInt(null) != expectedLoad ||
                    test.getField("scans").getInt(null) != expectedScan)
                    throw new AssertionError("Repeated automatic decision: " + state);
            }
        }
        System.out.println("PASS: scan/load/idle decisions, load priority, no repeated decisions; JVM verification");
    }

    public static void main(String[] args) throws Exception {
        ClassNode c = new ClassNode();
        new ClassReader(Files.readAllBytes(Path.of(args[0]))).accept(c, 0);
        if (!c.name.equals(OWNER)) throw new IllegalArgumentException("Unexpected class");
        for (MethodNode m : c.methods)
            if (m.name.equals(HELPER)) throw new IllegalStateException("Already patched");
        MethodNode join = c.methods.stream().filter(m -> m.name.equals("onPlayerJoinServer"))
            .findFirst().orElseThrow();
        int hidden = 0;
        for (AbstractInsnNode n : join.instructions.toArray()) {
            if (!(n instanceof MethodInsnNode)) continue;
            MethodInsnNode call = (MethodInsnNode)n;
            boolean prompt = call.name.equals("<init>") && call.desc.equals("(Z)V") &&
                (call.owner.equals("com/vinlanx/explosionoverhaul/ScanPromptPacket") ||
                 call.owner.equals("com/vinlanx/explosionoverhaul/ScanLoadPromptPacket"));
            prompt |= call.owner.equals(OWNER) && call.name.equals("sendScanPromptPacket") &&
                call.desc.equals("(Z)V");
            if (prompt) {
                AbstractInsnNode value = previous(n);
                if (value.getOpcode() != ICONST_1) throw new IllegalStateException("Unexpected prompt flag");
                join.instructions.set(value, new InsnNode(ICONST_0));
                hidden++;
            }
        }
        if (hidden != 4) throw new IllegalStateException("Expected 4 login prompts, got " + hidden);
        AbstractInsnNode lastReturn = null;
        for (AbstractInsnNode n : join.instructions.toArray())
            if (n.getOpcode() == RETURN) lastReturn = n;
        if (lastReturn == null) throw new IllegalStateException("Missing return");
        join.instructions.insertBefore(lastReturn,
            new MethodInsnNode(INVOKESTATIC, OWNER, HELPER, "()V", false));
        c.methods.add(helper());
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        c.accept(writer);
        byte[] result = writer.toByteArray();
        ClassNode verified = new ClassNode(); new ClassReader(result).accept(verified, 0);
        check(verified); stateTests();
        Files.write(Path.of(args[1]), result);
        System.out.println("PASS: all class methods pass bytecode analysis; 4 login prompts suppressed");
    }
}
'''


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    if sha256(JAR) != ORIGINAL_SHA256:
        raise SystemExit("Expected the original 0.2.3.0 JAR; refusing to patch a changed/already patched JAR.")
    asm = LIBRARIES / "org/ow2/asm"
    classpath = os.pathsep.join(str(asm / name / "9.7.1" / f"{name}-9.7.1.jar")
                                 for name in ("asm", "asm-tree", "asm-analysis"))
    backup = Path.home() / ".codex/backups/mine-strike/explosion-overhaul" / f"{ORIGINAL_SHA256}.jar"
    backup.parent.mkdir(parents=True, exist_ok=True)
    if not backup.exists():
        shutil.copy2(JAR, backup)
    if sha256(backup) != ORIGINAL_SHA256:
        raise SystemExit("Backup checksum mismatch.")
    with tempfile.TemporaryDirectory(prefix="mine-strike-explosion-") as work:
        work = Path(work)
        source = work / "PatchExplosion.java"
        source.write_text(JAVA)
        with zipfile.ZipFile(JAR) as original:
            (work / "original.class").write_bytes(original.read(ENTRY))
        subprocess.run(["javac", "--release", "17", "-cp", classpath, str(source)], check=True)
        subprocess.run(["java", "-Xverify:all", "-cp", str(work) + os.pathsep + classpath,
                        "PatchExplosion", str(work / "original.class"), str(work / "patched.class")], check=True)
        output = work / JAR.name
        with zipfile.ZipFile(JAR) as original, zipfile.ZipFile(output, "w") as patched:
            patched.comment = original.comment
            for entry in original.infolist():
                content = (work / "patched.class").read_bytes() if entry.filename == ENTRY else original.read(entry)
                patched.writestr(entry, content)
        with zipfile.ZipFile(JAR) as original, zipfile.ZipFile(output) as patched:
            assert patched.testzip() is None
            assert original.namelist() == patched.namelist()
            changed = [name for name in original.namelist() if original.read(name) != patched.read(name)]
            assert changed == [ENTRY], changed
        shutil.copy2(output, JAR)
    print("PASS: only BlockIndexManager.class changed; all other JAR entries preserved")
    print(f"Patched: {JAR}\nSHA256: {sha256(JAR)}\nOriginal backup: {backup}")


if __name__ == "__main__":
    main()
