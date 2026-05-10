package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;

@AutoModule(order = 405)
public class RotationHud implements HudElement {
    public static final int BASE_W = 145;
    public static final int BASE_H = 80;

    public static void register() {
        HudRegistry.register(new RotationHud());
    }

    @Override
    public String id() {
        return "rotation";
    }

    @Override
    public String displayName() {
        return "Rotation Debug";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.showRotationDebug;
    }

    @Override
    public int x() {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return ConfigManager.data.rotationOverlayX < 0
                ? screenW - width()
                : ConfigManager.data.rotationOverlayX;
    }

    @Override
    public int y() {
        int screenH = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return ConfigManager.data.rotationOverlayY < 0
                ? screenH - height()
                : ConfigManager.data.rotationOverlayY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.rotationOverlayX = x;
        ConfigManager.data.rotationOverlayY = y;
    }

    @Override
    public int width() {
        return Math.max(16, Math.round(BASE_W * ConfigManager.data.rotationOverlayScale));
    }

    @Override
    public int height() {
        return Math.max(16, Math.round(BASE_H * ConfigManager.data.rotationOverlayScale));
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float s = Math.max(0.5f, Math.min(3.0f, (float) width / BASE_W));
        ConfigManager.data.rotationOverlayScale = s;
    }

    @Override
    public void reset() {
        ConfigManager.data.rotationOverlayX = -1;
        ConfigManager.data.rotationOverlayY = -1;
        ConfigManager.data.rotationOverlayScale = 1.0f;
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
    }
}
