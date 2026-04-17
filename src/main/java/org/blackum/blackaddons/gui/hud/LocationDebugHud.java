package org.blackum.blackaddons.gui.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.LocationUtils;

public class LocationDebugHud {
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;
    private static final int DEFAULT_X_OFFSET = 170;
    private static final int DEFAULT_Y = 65;

    public static void register() {
        HudRenderCallback.EVENT.register((graphics, partialTick) -> render(graphics));
    }

    private static void render(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (!ConfigManager.data.showLocationDebug || mc.options.hideGui) return;

        int screenW = mc.getWindow().getGuiScaledWidth();
        int overlayX = ConfigManager.data.locationOverlayX < 0
                ? screenW - DEFAULT_X_OFFSET
                : ConfigManager.data.locationOverlayX;
        int overlayY = ConfigManager.data.locationOverlayY < 0
                ? DEFAULT_Y
                : ConfigManager.data.locationOverlayY;

        int y = overlayY;
        for (String line : LocationUtils.getDebugInfo()) {
            graphics.drawString(mc.font, line, overlayX, y, COLOR_WHITE);
            y += LINE_HEIGHT;
        }
    }
}
