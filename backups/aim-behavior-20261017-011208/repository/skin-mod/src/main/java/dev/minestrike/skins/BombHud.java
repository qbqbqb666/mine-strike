package dev.minestrike.skins;

import net.minecraft.client.gui.GuiGraphics;

public final class BombHud {
    // Integral of a frequency ramp from 0.8 to 3.2 pulses/second: no phase jumps as the fuse changes.
    public static double cycles(double remainingTicks, int totalTicks) {
        if (totalTicks <= 0 || !Double.isFinite(remainingTicks)) return 0;
        double total = totalTicks / 20.0;
        double elapsed = Math.max(0, Math.min(total, (totalTicks - remainingTicks) / 20.0));
        return .8 * elapsed + .8 * elapsed * elapsed * elapsed / (total * total);
    }

    public static float pulse(double remainingTicks, int totalTicks) {
        return (float) Math.pow(.5 + .5 * Math.cos(2 * Math.PI * cycles(remainingTicks, totalTicks)), 4);
    }

    public static boolean visible(String text, boolean started, boolean finished, int time, int fuse, int total) {
        return "--:--".equals(text) && started && !finished && time == -1 && fuse > 0 && total > 0;
    }

    private static int color(int dark, int bright, float pulse) {
        int r = (int) ((dark >> 16 & 255) + ((bright >> 16 & 255) - (dark >> 16 & 255)) * pulse);
        int g = (int) ((dark >> 8 & 255) + ((bright >> 8 & 255) - (dark >> 8 & 255)) * pulse);
        int b = (int) ((dark & 255) + ((bright & 255) - (dark & 255)) * pulse);
        return 0xff000000 | r << 16 | g << 8 | b;
    }

    public static void draw(GuiGraphics g, int fuse, int total, float partialTick) {
        float pulse = pulse(Math.max(0, fuse - Math.max(0, Math.min(1, partialTick))), total);
        g.m_280509_(-12, -5, 11, 5, ((int) (5 + 18 * pulse) << 24) | 0xff3026);
        g.m_280509_(-11, -4, 10, 5, ((int) (8 + 30 * pulse) << 24) | 0xff3026);
        int casing = color(0x651c25, 0xd53c39, pulse);
        int charge = color(0xa02d38, 0xff6954, pulse);
        int light = color(0xc45151, 0xffd1a0, pulse);
        // Explosive sticks, binding straps, control unit, display, keypad and status LED.
        g.m_280509_(-10, -2, -7, 4, charge);
        g.m_280509_(-6, -2, -3, 4, charge);
        g.m_280509_(-10, -1, -3, 0, 0xff45151f);
        g.m_280509_(-10, 2, -3, 3, 0xff45151f);
        g.m_280509_(-9, -4, -8, -2, light);
        g.m_280509_(-9, -4, 2, -3, light);
        g.m_280509_(-2, -4, 9, 4, 0xff39131b);
        g.m_280509_(-1, -3, 8, 3, casing);
        g.m_280509_(0, -2, 5, 0, light);
        g.m_280509_(6, -2, 7, -1, color(0xb03338, 0xffeee0, pulse));
        g.m_280509_(0, 1, 1, 2, light);
        g.m_280509_(2, 1, 3, 2, light);
        g.m_280509_(4, 1, 5, 2, light);
        g.m_280509_(6, 1, 7, 2, light);
    }

    private BombHud() {}
}
