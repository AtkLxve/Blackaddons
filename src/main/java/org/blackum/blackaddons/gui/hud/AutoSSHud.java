package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.feature.cheat.AutoSS;

@AutoModule(order = 404)
public class AutoSSHud implements HudElement {
    public static final int BASE_W = 180;
    public static final int BASE_H = 120;

    public static void register() {
        HudRegistry.register(new AutoSSHud());
    }

    @Override
    public String id() {
        return "autoss";
    }

    @Override
    public String displayName() {
        return "AutoSS Debug";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.AutoSSDebug;
    }

    @Override
    public int x() {
        return ConfigManager.data.AutoSSOverlayX < 0 ? 10 : ConfigManager.data.AutoSSOverlayX;
    }

    @Override
    public int y() {
        return ConfigManager.data.AutoSSOverlayY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.AutoSSOverlayX = x;
        ConfigManager.data.AutoSSOverlayY = y;
    }

    @Override
    public int width() {
        return Math.max(16, Math.round(BASE_W * ConfigManager.data.AutoSSOverlayScale));
    }

    @Override
    public int height() {
        return Math.max(16, Math.round(BASE_H * ConfigManager.data.AutoSSOverlayScale));
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float s = Math.max(0.5f, Math.min(3.0f, (float) width / BASE_W));
        ConfigManager.data.AutoSSOverlayScale = s;
    }

    @Override
    public void reset() {
        ConfigManager.data.AutoSSOverlayX = -1;
        ConfigManager.data.AutoSSOverlayY = 5;
        ConfigManager.data.AutoSSOverlayScale = 1.0f;
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        AutoSS.renderHud(graphics, tracker);
    }
}
