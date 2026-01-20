package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

import java.util.function.Consumer;

public class RadioButton extends Widget {

    private boolean selected;
    private String label;
    private String groupName;
    private Consumer<Boolean> onSelect;
    private Animation selectAnimation;
    private Animation hoverAnimation;

    public RadioButton(int x, int y, String label, String groupName, boolean initialState,
            Consumer<Boolean> onSelect) {
        super(x, y, Theme.RADIO_SIZE + (label.isEmpty() ? 0 : Minecraft.getInstance().font.width(label) + 8),
                Theme.RADIO_SIZE);
        this.label = label;
        this.groupName = groupName;
        this.selected = initialState;
        this.onSelect = onSelect;
        this.selectAnimation = new Animation(initialState ? 1 : 0, initialState ? 1 : 0, Theme.ANIM_CLICK,
                Easing::easeOutBack);
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float selectProgress = selectAnimation.getValue();
        float hoverProgress = hoverAnimation.getValue();

        int radioRadius = Theme.RADIO_SIZE / 2;
        RenderHelper.renderSurface(graphics, x, y, Theme.RADIO_SIZE, Theme.RADIO_SIZE,
                radioRadius, selected);

        if (selectProgress > 0) {
            int innerSize = (int) ((Theme.RADIO_SIZE - 10) * selectProgress);
            int innerX = x + (Theme.RADIO_SIZE - innerSize) / 2;
            int innerY = y + (Theme.RADIO_SIZE - innerSize) / 2;
            int innerColor = Theme.withAlpha(Theme.ACCENT, selectProgress * 0.9f);

            graphics.fill(innerX, innerY, innerX + innerSize, innerY + innerSize, innerColor);
        }

        if (hoverProgress > 0) {
            int highlightColor = Theme.withAlpha(Theme.GLASS_HIGHLIGHT, hoverProgress * 0.3f);
            RenderHelper.renderRoundedRect(graphics, x, y, Theme.RADIO_SIZE, Theme.RADIO_SIZE,
                    radioRadius, highlightColor);
        }

        if (!label.isEmpty()) {
            int labelX = x + Theme.RADIO_SIZE + 8;
            int labelY = y + (Theme.RADIO_SIZE - 8) / 2;
            int labelColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
            graphics.drawString(Minecraft.getInstance().font, label, labelX, labelY, labelColor);
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
            select();
            return true;
        }
        return false;
    }

    private void select() {
        if (!selected) {
            deselectGroup(groupName, this);

            selected = true;
            selectAnimation = new Animation(selectAnimation.getValue(), 1, Theme.ANIM_CLICK,
                    Easing::easeOutBack);
            selectAnimation.start();

            if (onSelect != null) {
                onSelect.accept(true);
            }
        }
    }

    private void deselect() {
        if (selected) {
            selected = false;
            selectAnimation = new Animation(selectAnimation.getValue(), 0, Theme.ANIM_CLICK,
                    Easing::easeOutBack);
            selectAnimation.start();

            if (onSelect != null) {
                onSelect.accept(false);
            }
        }
    }

    private void deselectGroup(String groupName, RadioButton except) {
        if (Minecraft.getInstance().screen instanceof org.blackum.blackaddons.gui.screen.BaseScreen base) {
            for (Widget w : base.getWidgets()) {
                if (w instanceof RadioButton rb && rb.groupName.equals(groupName) && rb != except) {
                    rb.deselect();
                }
            }
        }
    }

    public boolean isSelected() {
        return selected;
    }
}
