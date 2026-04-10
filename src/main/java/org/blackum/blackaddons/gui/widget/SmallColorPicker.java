package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.ColorUtils;
import org.blackum.blackaddons.gui.render.RenderHelper;
import java.util.function.Consumer;

public class SmallColorPicker extends ColorPicker {
    public static final int S_WIDTH = 100;
    public static final int S_HEIGHT = 100;

    public SmallColorPicker(int x, int y, int initialColor, Consumer<Integer> onColorChange) {
        super(x, y, initialColor, onColorChange);
        this.width = S_WIDTH;
        this.height = S_HEIGHT;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);

        int currentY = y + 5;
        int sbWidth = width - 10;
        int sbHeight = 60;
        int innerX = x + 5;

        renderSBArea(graphics, innerX, currentY, sbWidth, sbHeight);
        currentY += sbHeight + 5;

        renderHueSlider(graphics, innerX, currentY, sbWidth, 6);
        currentY += 10;

        renderAlphaSlider(graphics, innerX, currentY, sbWidth, 6);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled) return false;

        int sbWidth = width - 10;
        int sbHeight = 60;
        int innerX = x + 5;
        int sbY = y + 5;

        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= sbY && mouseY <= sbY + sbHeight) {
            draggingSB = true;
            updateSB(mouseX, mouseY, innerX, sbY, sbWidth, sbHeight);
            return true;
        }

        int hueY = sbY + sbHeight + 5;
        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= hueY && mouseY <= hueY + 6) {
            draggingHue = true;
            updateHue(mouseX, innerX, sbWidth);
            return true;
        }

        int alphaY = hueY + 10;
        if (mouseX >= innerX && mouseX <= innerX + sbWidth && mouseY >= alphaY && mouseY <= alphaY + 6) {
            draggingAlpha = true;
            updateAlpha(mouseX, innerX, sbWidth);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int sbWidth = width - 10;
        int sbHeight = 60;
        int innerX = x + 5;
        int sbY = y + 5;

        if (draggingSB) {
            updateSB(mouseX, mouseY, innerX, sbY, sbWidth, sbHeight);
            return true;
        }
        if (draggingHue) {
            updateHue(mouseX, innerX, sbWidth);
            return true;
        }
        if (draggingAlpha) {
            updateAlpha(mouseX, innerX, sbWidth);
            return true;
        }
        return false;
    }

    private void updateHue(double mouseX, int innerX, int sbWidth) {
        hull = (float) ((mouseX - innerX) / sbWidth);
        hull = Math.max(0f, Math.min(1f, hull));
        notifyChange();
    }

    private void updateSB(double mouseX, double mouseY, int innerX, int sbY, int width, int height) {
        saturation = (float) ((mouseX - innerX) / width);
        brightness = 1f - (float) ((mouseY - sbY) / height);
        saturation = Math.max(0f, Math.min(1f, saturation));
        brightness = Math.max(0f, Math.min(1f, brightness));
        notifyChange();
    }

    private void updateAlpha(double mouseX, int innerX, int sbWidth) {
        alpha = (float) ((mouseX - innerX) / sbWidth);
        alpha = Math.max(0f, Math.min(1f, alpha));
        notifyChange();
    }
}
