package org.blackum.blackaddons.gui.hud;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.common.config.ConfigManager;
import org.blackum.blackaddons.common.module.AutoModule;
import org.blackum.blackaddons.common.util.mc.McCompat;
import org.blackum.blackaddons.feature.dungeon.listener.DungeonListener;
import org.blackum.blackaddons.gui.screen.overlay.OverlayEditScreen;

@AutoModule(order = 406)
public class KeyTimerHud implements HudElement {
    private static final int BASE_W = 100;
    private static final int BASE_H = 10;
    private static final int COLOR_WHITE = 0xFFFFFFFF;

    public static void register() {
        HudRegistry.register(new KeyTimerHud());
    }

    @Override
    public String id() {
        return "key_timer";
    }

    @Override
    public String displayName() {
        return "Auto Pickup Key Timer";
    }

    @Override
    public boolean enabled() {
        return ConfigManager.data.keyTimerEnabled;
    }

    @Override
    public int x() {
        return ConfigManager.data.keyTimerX;
    }

    @Override
    public int y() {
        return ConfigManager.data.keyTimerY;
    }

    @Override
    public void setPos(int x, int y) {
        ConfigManager.data.keyTimerX = x;
        ConfigManager.data.keyTimerY = y;
    }

    @Override
    public void reset() {
        ConfigManager.data.keyTimerX = 10;
        ConfigManager.data.keyTimerY = 150;
        ConfigManager.data.keyTimerScale = 1.0f;
    }

    @Override
    public boolean resizable() {
        return true;
    }

    @Override
    public void setSize(int width, int height) {
        float scale = Math.max(0.5f, Math.min(3.0f, (float) width / BASE_W));
        ConfigManager.data.keyTimerScale = scale;
    }

    @Override
    public int width() {
        return Math.round(BASE_W * ConfigManager.data.keyTimerScale);
    }

    @Override
    public int height() {
        return Math.round(BASE_H * ConfigManager.data.keyTimerScale);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        boolean isEditing = McCompat.getScreen(mc) instanceof OverlayEditScreen;

        if (DungeonListener.keyTimerTicks <= 0 && !isEditing) {
            return;
        }

        String type = DungeonListener.keyTimerTicks > 0 ? DungeonListener.keyTimerType : "Wither";
        float seconds = DungeonListener.keyTimerTicks > 0 ? (DungeonListener.keyTimerTicks / 20.0f) : 10.0f;

        String coloredText;
        if ("Blood".equalsIgnoreCase(type)) {
            coloredText = String.format("§cBlood Key: §f%.1fs", seconds);
        } else {
            coloredText = String.format("§8Wither Key: §f%.1fs", seconds);
        }

        float scale = ConfigManager.data.keyTimerScale;
        int overlayX = x();
        int overlayY = y();

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) overlayX, (float) overlayY);
        graphics.pose().scale(scale, scale);
        graphics.text(mc.font, coloredText, 0, 0, COLOR_WHITE);
        graphics.pose().popMatrix();
    }
}
