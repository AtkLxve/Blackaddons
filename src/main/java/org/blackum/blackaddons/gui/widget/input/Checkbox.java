package org.blackum.blackaddons.gui.widget.input;

import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.function.Consumer;

public class Checkbox extends Widget {
    private boolean checked;
    private String label;
    private Consumer<Boolean> onToggle;
    private Animation checkAnimation;
    private Animation hoverAnimation;

    public Checkbox(int x, int y, String label, boolean initialState, Consumer<Boolean> onToggle) {
        this(x, y, Theme.CHECKBOX_SIZE, label, initialState, onToggle);
    }

    public Checkbox(int x, int y, int height, String label, boolean initialState, Consumer<Boolean> onToggle) {
        super(x, y,
                height + (label.isEmpty() ? 0 : Minecraft.getInstance().font.width(label) + 8),
                height);
        this.label = label;
        this.checked = initialState;
        this.onToggle = onToggle;
        this.checkAnimation = new Animation(initialState ? 1 : 0, initialState ? 1 : 0, Theme.ANIM_NORMAL,
                Easing::easeOutBack);
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float checkProgress = Math.max(0.0f, Math.min(1.0f, checkAnimation.getValue()));
        float hoverProgress = hoverAnimation.getValue();

        RenderHelper.renderSurface(graphics, x, y, height, height,
                Theme.BORDER_RADIUS_SMALL, checked);

        if (checkProgress > 0) {
            int checkColor = Theme.withAlpha(Theme.ACCENT, 0.25f + checkProgress * 0.65f);
            int padding = 3;
            int checkSize = (int) ((height - padding * 2) * checkProgress);
            int checkX = x + padding + (height - padding * 2 - checkSize) / 2;
            int checkY = y + padding + (height - padding * 2 - checkSize) / 2;
            graphics.fill(checkX, checkY, checkX + checkSize, checkY + checkSize, checkColor);
        }

        if (hoverProgress > 0) {
            int highlightColor = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hoverProgress * 0.3f);
            RenderHelper.renderRoundedRect(graphics, x, y, height, height,
                    Theme.BORDER_RADIUS_SMALL, highlightColor);
        }

        if (!label.isEmpty()) {
            int labelX = x + height + 8;
            int labelY = y + (height - 8) / 2;
            int baseColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            int labelColor = Theme.lerpColor(baseColor, Theme.ACCENT, Math.max(checkProgress, hoverProgress * 0.35f));
            graphics.text(Minecraft.getInstance().font, label, labelX, labelY, labelColor);
        }
    }

    @Override
    public void tick() {
        if (hovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if (!hovered && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        if (isMouseOver(mouseX, mouseY) && button == 0) {
            toggle();
            return true;
        }
        return false;
    }

    private void toggle() {
        checked = !checked;
        checkAnimation = new Animation(checkAnimation.getValue(), checked ? 1 : 0, Theme.ANIM_NORMAL,
                Easing::easeOutBack);
        checkAnimation.start();

        if (onToggle != null) {
            onToggle.accept(checked);
        }
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        if (this.checked != checked) {
            toggle();
        }
    }
}
