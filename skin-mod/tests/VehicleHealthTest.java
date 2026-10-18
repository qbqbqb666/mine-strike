import dev.minestrike.skins.LavAdHealth;
import com.atsuishio.superbwarfare.entity.vehicle.damage.DamageModify;
import com.google.gson.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipFile;

public class VehicleHealthTest {
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    private static void near(float actual, float expected, String message) {
        check(Math.abs(actual - expected) < .001f, message + ": " + actual + " != " + expected);
    }
    private static ClassNode node(byte[] data) {
        ClassNode node = new ClassNode(); new ClassReader(data).accept(node, 0); return node;
    }
    private static boolean calls(MethodNode method, String name) {
        return Arrays.stream(method.instructions.toArray()).anyMatch(i -> i instanceof MethodInsnNode c && c.name.equals(name));
    }
    private static JsonObject json(ZipFile jar, String path) throws Exception {
        return JsonParser.parseString(new String(jar.getInputStream(jar.getEntry(path)).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }
    private static float nativeDamage(JsonArray modifiers, String type, float input) {
        float result = input;
        for (JsonElement element : modifiers) {
            String rule = element.getAsString();
            if (rule.startsWith("All ") || rule.startsWith(type + " ") || rule.startsWith("@#superbwarfare:at_rocket ")) {
                String[] fields = rule.split("\\s+");
                var operation = fields[1].equals("-") ? DamageModify.ModifyType.REDUCE : DamageModify.ModifyType.MULTIPLY;
                var modifier = new DamageModify(operation, Float.parseFloat(fields[2]));
                result = modifier.compute(result);
            }
        }
        return result;
    }
    public static void main(String[] args) throws Exception {
        near(LavAdHealth.maximum("superbwarfare:ah_6"), 1000, "Global AH-6 health");
        near(LavAdHealth.maximum("superbwarfare:lav_ad"), 150, "Preserve LAV-AD");
        near(LavAdHealth.maximum("superbwarfare:ah_1z"), 0, "Other helicopter excluded");
        near(LavAdHealth.maximum("minecraft:player"), 0, "Other entity excluded");
        near(LavAdHealth.health(250, 250, 1000), 1000, "Full newly spawned AH-6");
        near(LavAdHealth.health(250, 100, 1000), 400, "Preserve damage ratio");
        near(LavAdHealth.health(1000, 400, 1000), 400, "Reload must not heal");
        near(LavAdHealth.health(250, 0, 1000), 0, "Dead stays dead");
        near(LavAdHealth.health(250, 300, 1000), 1000, "Clamp above maximum");
        near(LavAdHealth.health(0, 20, 1000), 20, "Preserve previous zero-maximum fallback");
        try (var jar = new ZipFile(args[0])) {
            JsonObject vehicle = json(jar, "data/superbwarfare/sbw/vehicles/ah_6.json");
            JsonObject gun = json(jar, "data/superbwarfare/sbw/guns/rpg.json");
            near(vehicle.get("MaxHealth").getAsFloat(), 250, "Installed original AH-6 baseline");
            near(gun.get("Damage").getAsFloat(), 300, "Installed RPG direct damage");
            near(gun.get("ExplosionDamage").getAsFloat(), 150, "Installed RPG explosion damage");
            check(gun.get("Projectile").getAsString().equals("superbwarfare:rpg_rocket_tbg"), "Current RPG projectile");
            String rockets = json(jar, "data/superbwarfare/tags/entity_types/at_rocket.json").getAsJsonArray("values").toString();
            check(rockets.contains("superbwarfare:rpg_rocket_tbg") && rockets.contains("superbwarfare:rpg_rocket_standard"), "Both RPG rocket entity tags");
            String ordinaryBullets = json(jar, "data/superbwarfare/tags/damage_type/projectile.json").getAsJsonArray("values").toString();
            check(!ordinaryBullets.contains("superbwarfare:projectile_hit") && !ordinaryBullets.contains("superbwarfare:custom_explosion"), "RPG is not ordinary bullet damage");
            JsonArray modifiers = vehicle.getAsJsonArray("DamageModifiers");
            float direct = nativeDamage(modifiers, "superbwarfare:projectile_hit", gun.get("Damage").getAsFloat());
            float blast = nativeDamage(modifiers, "superbwarfare:custom_explosion", gun.get("ExplosionDamage").getAsFloat());
            near(direct, 500.64f, "Real native direct modifier arithmetic");
            near(blast, 310.8f, "Real native full-exposure explosion modifier arithmetic");
            float maximum = LavAdHealth.maximum("superbwarfare:ah_6");
            check(direct + blast < maximum, "Survive one direct hit plus maximum native blast");
            check(2 * direct >= maximum, "Two direct hits lethal even without blast damage");
            ClassNode rocket = node(jar.getInputStream(jar.getEntry("com/atsuishio/superbwarfare/entity/projectile/RpgRocketTBGEntity.class")).readAllBytes());
            MethodNode hit = rocket.methods.stream().filter(m -> m.name.equals("m_5790_")).findFirst().orElseThrow();
            check(calls(hit, "causeProjectileHitDamage") && calls(hit, "forceHurt") && calls(hit, "getDamageValue") && calls(hit, "causeExplode"), "Actual RPG direct-then-explosion path");
            ClassNode builder = node(jar.getInputStream(jar.getEntry("com/atsuishio/superbwarfare/tools/CustomExplosion$Builder.class")).readAllBytes());
            check(calls(builder.methods.stream().filter(m -> m.name.equals("explode")).findFirst().orElseThrow(), "causeCustomExplosionDamage"), "Native custom explosion type; no vanilla 4x multiplier");
            ClassNode nativeVehicle = node(jar.getInputStream(jar.getEntry("com/atsuishio/superbwarfare/entity/vehicle/base/VehicleEntity.class")).readAllBytes());
            MethodNode hurt = nativeVehicle.methods.stream().filter(m -> m.name.equals("m_6469_")).findFirst().orElseThrow();
            check(calls(hurt, "getDamageModifier") && calls(hurt, "compute") && calls(hurt, "onHurt"), "Original defense and destruction path retained");
            System.out.printf(java.util.Locale.ROOT, "PASS: AH-6 1000 health, native RPG direct %.2f / maximum blast %.2f, first-hit survival and two-direct-hit kill threshold%n", direct, blast);
        }
        try (var in = VehicleHealthTest.class.getResourceAsStream("/dev/minestrike/skins/LavAdHealth.class")) {
            ClassNode health = node(in.readAllBytes());
            MethodNode join = health.methods.stream().filter(m -> m.name.equals("join")).findFirst().orElseThrow();
            check(calls(join, "getLevel") && calls(join, "getEntity") && calls(join, "maximum"), "Server join and exact entity type scope");
            check(Arrays.stream(join.instructions.toArray()).anyMatch(i -> i instanceof FieldInsnNode f && f.name.equals("f_46443_")), "Client-side guard retained");
            check(calls(join, "getOverride") && calls(join, "parseString") && calls(join, "addProperty") && calls(join, "setOverride") && calls(join, "update") && calls(join, "health") && calls(join, "setHealth"), "Existing override fields, VehicleData refresh and ratio-preserving health pipeline");
            Set<String> stringConstants = new HashSet<>();
            for (var i : join.instructions) if (i instanceof LdcInsnNode ldc && ldc.cst instanceof String s) stringConstants.add(s);
            check(stringConstants.equals(Set.of("MaxHealth")), "Only MaxHealth override property may be written; no map or other vehicle restriction");
        }
        System.out.println("PASS: AH-6/LAV type isolation, server-only join hook, damaged/reloaded health ratio, other override fields and native vehicle damage flow");
    }
}
