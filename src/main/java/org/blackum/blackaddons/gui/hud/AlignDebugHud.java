package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.feature.waypoint.AlignUtils;

public class AlignDebugHud implements HudElement {
    public static final int BASE_W = 190;
    public static final int BASE_H = 60;
    private static final int LINE_HEIGHT = 10;

    public static void register() {
        HudRegistry.register(new AlignDebugHud());
    }

    @Override
    public String id() {
        return "align_debug";
    }

    @Override
    public String displayName() {
        return "Align Debug";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.showAlignDebug && !Minecraft.getInstance().options.hideGui;
    }

    @Override
    public int x() {
        int screenW = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return ConfigManager.data.alignOverlayX < 0
                ? screenW - width()
                : ConfigManager.data.alignOverlayX;
    }

    @Override
    public int y() {
        return ConfigManager.data.alignOverlayY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.alignOverlayX = x;
        ConfigManager.data.alignOverlayY = y;
    }

    @Override
    public void reset() {
        ConfigManager.data.alignOverlayX = -1;
        ConfigManager.data.alignOverlayY = 125;
        ConfigManager.data.alignOverlayScale = 1.0f;
    }

    @Override
    public int width() {
        return Math.max(16, Math.round(BASE_W * ConfigManager.data.alignOverlayScale));
    }

    @Override
    public int height() {
        int lines = Math.max(1, AlignUtils.getDebugInfo().size());
        int raw = Math.max(BASE_H, lines * LINE_HEIGHT);
        return Math.max(16, Math.round(raw * ConfigManager.data.alignOverlayScale));
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float s = Math.max(0.5f, Math.min(3.0f, (float) width / BASE_W));
        ConfigManager.data.alignOverlayScale = s;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        AlignUtils.renderDebug(graphics);
    }
}
