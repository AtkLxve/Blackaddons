package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.feature.dungeon.solver.puzzle.waterboard.WaterBoardSolver;

public class WaterBoardHud implements HudElement {

    public static void register() {
        HudRegistry.register(new WaterBoardHud());
    }

    @Override
    public String id() {
        return "waterboard";
    }

    @Override
    public String displayName() {
        return "Water Board";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.waterBoardHudEnabled;
    }

    @Override
    public int x() {
        return ConfigManager.data.waterBoardHudX;
    }

    @Override
    public int y() {
        return ConfigManager.data.waterBoardHudY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.waterBoardHudX = x;
        ConfigManager.data.waterBoardHudY = y;
    }

    @Override
    public void reset() {
        ConfigManager.data.waterBoardHudX = -1;
        ConfigManager.data.waterBoardHudY = -1;
        ConfigManager.data.waterBoardHudScale = 1.0f;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int w, int h) {
        float scale = Math.max(0.1f, Math.min(5.0f, w / 120.0f));
        ConfigManager.data.waterBoardHudScale = scale;
    }

    @Override
    public int width() {
        float scale = ConfigManager.data.waterBoardHudScale <= 0.0f ? 1.0f : ConfigManager.data.waterBoardHudScale;
        return (int) (120 * scale);
    }

    @Override
    public int height() {
        float scale = ConfigManager.data.waterBoardHudScale <= 0.0f ? 1.0f : ConfigManager.data.waterBoardHudScale;
        return (int) (60 * scale);
    }

    @Override
    public void render(GuiGraphics graphics, DeltaTracker tracker) {
        WaterBoardSolver.renderHUD(graphics);
    }
}
