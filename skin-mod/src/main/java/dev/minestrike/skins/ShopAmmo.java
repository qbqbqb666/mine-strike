package dev.minestrike.skins;

import com.atsuishio.superbwarfare.data.gun.GunData;
import com.atsuishio.superbwarfare.data.gun.GunProp;
import com.atsuishio.superbwarfare.item.gun.GunItem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.Bolt;
import net.minecraft.world.item.ItemStack;

public final class ShopAmmo {
    public static final String TOTAL = "MineStrikeShopTotalAmmo";
    public static boolean supported(ItemStack stack) {
        return stack.m_41720_() instanceof IGun || stack.m_41720_() instanceof GunItem;
    }
    public static int total(ItemStack stack) {
        if (stack.m_41619_()) return 0;
        var tag = stack.m_41783_();
        if (supported(stack) && tag != null && tag.m_128425_(TOTAL, 3)) return tag.m_128451_(TOTAL);
        if (stack.m_41720_() instanceof IGun gun)
            return Math.max(0, gun.getCurrentAmmoCount(stack)) + Math.max(0, gun.getDummyAmmoAmount(stack))
                    + (gun.hasBulletInBarrel(stack) && TimelessAPI.getCommonGunIndex(gun.getGunId(stack))
                        .map(index -> index.getGunData().getBolt() != Bolt.OPEN_BOLT).orElse(false) ? 1 : 0);
        if (stack.m_41720_() instanceof GunItem) {
            var data = GunData.from(stack);
            return Math.max(0, data.ammo.get()) + Math.max(0, data.virtualAmmo.get());
        }
        return 0;
    }
    public record Load(int magazine, int reserve, boolean chamber) {}
    public static Load split(int total, int capacity, boolean chamber) {
        if (total < 0 || total > 1000000 || capacity < 0) throw new IllegalArgumentException("弹药数值无效");
        int loaded = Math.min(total, capacity);
        boolean inBarrel = chamber && loaded > 0;
        return new Load(loaded - (inBarrel ? 1 : 0), total - loaded, inBarrel);
    }
    public static boolean configure(ItemStack stack, int total) {
        if (total < 0 || total > 1000000) return false;
        if (stack.m_41720_() instanceof IGun gun) {
            var index = TimelessAPI.getCommonGunIndex(gun.getGunId(stack));
            if (index.isEmpty()) return false;
            var data = index.get().getGunData();
            var load = split(total, data.getAmmoAmount(), data.getBolt() != Bolt.OPEN_BOLT);
            gun.setCurrentAmmoCount(stack, load.magazine());
            gun.setBulletInBarrel(stack, load.chamber());
            gun.setDummyAmmoAmount(stack, load.reserve());
            gun.setMaxDummyAmmoAmount(stack, load.reserve());
        } else if (stack.m_41720_() instanceof GunItem) {
            var data = GunData.from(stack);
            var load = split(total, Math.max(0, data.get(GunProp.MAGAZINE)), false);
            data.ammo.set(load.magazine());
            data.virtualAmmo.set(load.reserve());
        } else return false;
        stack.m_41784_().m_128405_(TOTAL, total);
        return true;
    }
    public static void restore(ItemStack stack) {
        var tag = stack.m_41783_();
        if (tag != null && tag.m_128425_(TOTAL, 3)) configure(stack, tag.m_128451_(TOTAL));
    }
    public static ItemStack prepare(ItemStack template) {
        var tag = template.m_41783_();
        if (tag == null || !tag.m_128425_(TOTAL, 3)) return template;
        ItemStack copy = template.m_41777_();
        restore(copy);
        return copy;
    }
    private ShopAmmo() {}
}
