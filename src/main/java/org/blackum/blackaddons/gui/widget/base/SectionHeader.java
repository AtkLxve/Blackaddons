package org.blackum.blackaddons.gui.widget.base;

import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.RenderHelper;
import org.blackum.blackaddons.gui.render.Theme;

public class SectionHeader extends Widget {
    private final String title;
    private Runnable onToggle;
    private Checkbox bulkCheckbox;
    private boolean collapsed;
    private Animation hoverAnimation;

    public SectionHeader(int width, String title) {
        super(0, 0, width, 25);
        this.title = ChatFormatting.BOLD + title;
        this.hoverAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);
    }

    public SectionHeader(int width, String title, boolean collapsed, Runnable onToggle) {
        this(width, title);
        this.collapsed = collapsed;
        this.onToggle = onToggle;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hover = hoverAnimation.getValue();
        if (hover > 0) {
            RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL,
                    Theme.withAlpha(0xFF000000, hover * 0.15f));
        }

        int currentX = x;
        if (onToggle != null) {
            String arrow = collapsed ? "▶ " : "▼ ";
            int arrowColor = Theme.lerpColor(Theme.TEXT_SECONDARY, Theme.ACCENT, hover);
            graphics.drawString(Minecraft.getInstance().font, arrow, currentX + 6, y + 8, arrowColor);
            currentX += 16;
        }

        if (bulkCheckbox != null) {
            bulkCheckbox.setX(currentX);
            bulkCheckbox.setY(y + 4);
            bulkCheckbox.render(graphics, mouseX, mouseY, partialTick);
            currentX += bulkCheckbox.getWidth() + 8;
        }

        graphics.drawString(Minecraft.getInstance().font, title, currentX + Math.round(hover * 2.0f), y + 8,
                Theme.ACCENT);

        int titleWidth = Minecraft.getInstance().font.width(title);
        int lineX = currentX + titleWidth + 10 + Math.round(hover * 2.0f);
        int lineW = width - (lineX - x);
        if (lineW > 0) {
            int centerY = y + 8 + 4;
            graphics.fill(lineX, centerY, x + width, centerY + 1,
                    Theme.withAlpha(Theme.TEXT_SECONDARY, 0.25f + hover * 0.25f));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible || !enabled)
            return false;

        if (bulkCheckbox != null && bulkCheckbox.isMouseOver(mouseX, mouseY)) {
            return bulkCheckbox.mouseClicked(mouseX, mouseY, button);
        }

        if (isMouseOver(mouseX, mouseY) && onToggle != null && button == 0) {
            collapsed = !collapsed;
            onToggle.run();
            return true;
        }
        return false;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);
        if (bulkCheckbox != null) {
            bulkCheckbox.updateHoverState(mouseX, mouseY);
        }
    }

    @Override
    public void tick() {
        boolean canHover = onToggle != null;
        if (canHover && hovered && hoverAnimation.getProgress() < 1
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() < 1)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        } else if ((!canHover || !hovered) && hoverAnimation.getProgress() > 0
                && (!hoverAnimation.isRunning() || hoverAnimation.getValue() > 0)) {
            hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
            hoverAnimation.start();
        }
        if (bulkCheckbox != null) {
            bulkCheckbox.tick();
        }
    }

    public void setBulkCheckbox(Checkbox checkbox) {
        this.bulkCheckbox = checkbox;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public void setToggleCallback(Runnable onToggle) {
        this.onToggle = onToggle;
    }
}
