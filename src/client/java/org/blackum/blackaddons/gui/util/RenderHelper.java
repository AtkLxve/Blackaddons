package org.blackum.blackaddons.gui.util;

import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.theme.Theme;

public class RenderHelper {

    public static void renderSurface(GuiGraphics graphics, int x, int y, int width, int height, int radius,
            boolean pressed) {
        int fill = Theme.GLASS_FILL;
        if (pressed) {
            fill = Theme.withAlpha(Theme.GLASS_FILL, 0.7f);
        }

        renderRoundedRect(graphics, x, y, width, height, radius, fill);
        renderRoundedOutline(graphics, x, y, width, height, radius, Theme.GLASS_BORDER);
    }

    public static void renderRoundedOutline(GuiGraphics graphics, int x, int y, int width, int height, int radius,
            int color) {
        graphics.fill(x, y, x + width, y + 1, color);
        graphics.fill(x, y + height - 1, x + width, y + height, color);
        graphics.fill(x, y, x + 1, y + height, color);
        graphics.fill(x + width - 1, y, x + width, y + height, color);
    }

    public static void renderRoundedRect(GuiGraphics graphics, int x, int y, int width, int height, int radius,
            int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    public static int adjustAlpha(int color, float alphaMultiplier) {
        int a = (color >> 24) & 0xFF;
        if (a == 0 && color != 0)
            a = 255;
        int rgb = color & 0x00FFFFFF;
        int newAlpha = Math.min(255, Math.max(0, (int) (a * alphaMultiplier)));
        return (newAlpha << 24) | rgb;
    }
}
