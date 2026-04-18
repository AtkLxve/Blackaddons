package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.util.mc.LocationUtils;

public class LocationDebugHud implements HudElement {
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;
    public static final int BASE_W = 170;
    public static final int BASE_H = 80;
    private static final int DEFAULT_Y = 65;

    public static void register() {
        HudRegistry.register(new LocationDebugHud());
    }

    @Override
    public String id() {
        return "location_debug";
    }

    @Override
    public String displayName() {
        return "Location Debug";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.showLocationDebug && !Minecraft.getInstance().options.hideGui;
    }

    @Override
    public int x() {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return ConfigManager.data.locationOverlayX < 0
                ? screenW - width()
                : ConfigManager.data.locationOverlayX;
    }

    @Override
    public int y() {
        return ConfigManager.data.locationOverlayY < 0
                ? DEFAULT_Y
                : ConfigManager.data.locationOverlayY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.locationOverlayX = x;
        ConfigManager.data.locationOverlayY = y;
    }

    @Override
    public void reset() {
        ConfigManager.data.locationOverlayX = -1;
        ConfigManager.data.locationOverlayY = DEFAULT_Y;
        ConfigManager.data.locationOverlayScale = 1.0f;
    }

    @Override
    public int width() {
        return Math.max(16, Math.round(BASE_W * ConfigManager.data.locationOverlayScale));
    }

    @Override
    public int height() {
        int lines = Math.max(1, LocationUtils.getDebugInfo().size());
        int raw = Math.max(BASE_H, lines * LINE_HEIGHT);
        return Math.max(16, Math.round(raw * ConfigManager.data.locationOverlayScale));
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float s = Math.max(0.5f, Math.min(3.0f, (float) width / BASE_W));
        ConfigManager.data.locationOverlayScale = s;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        float scale = ConfigManager.data.locationOverlayScale;
        int overlayX = x();
        int overlayY = y();
        graphics.pose().pushMatrix();
        graphics.pose().translate((float) overlayX, (float) overlayY);
        graphics.pose().scale(scale, scale);
        int yy = 0;
        for (String line : LocationUtils.getDebugInfo()) {
            graphics.drawString(mc.font, line, 0, yy, COLOR_WHITE);
            yy += LINE_HEIGHT;
        }
        graphics.pose().popMatrix();
    }
}
