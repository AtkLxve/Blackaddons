package org.blackum.blackaddons.gui.widget.layout;

import org.blackum.blackaddons.gui.widget.base.*;
import org.blackum.blackaddons.gui.widget.input.*;
import org.blackum.blackaddons.gui.widget.layout.*;
import org.blackum.blackaddons.gui.widget.row.*;
import org.blackum.blackaddons.gui.widget.editor.*;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.render.Theme;
import org.blackum.blackaddons.gui.render.RenderHelper;

import java.util.List;

public class ResizableCard extends Card {

    private static final int RESIZE_HANDLE_SIZE = 12;
    private static final int TITLE_BAR_HEIGHT = 24;

    private boolean dragging = false;
    private boolean resizing = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragStartCardX = 0;
    private int dragStartCardY = 0;
    private int dragStartWidth = 0;
    private ResizeHandle activeHandle = ResizeHandle.NONE;

    private int initialWidth;

    private boolean collapsed = true;
    private int expandedHeight;
    private Animation hoverAnimation;
    private Animation activeAnimation;

    private int minX = Integer.MIN_VALUE;
    private int minY = Integer.MIN_VALUE;
    private int maxX = Integer.MAX_VALUE;
    private int maxY = Integer.MAX_VALUE;

    private enum ResizeHandle {
        NONE, BOTTOM_RIGHT, BOTTOM, RIGHT
    }

    private Runnable onLayoutChange;

    public void setOnLayoutChange(Runnable onLayoutChange) {
        this.onLayoutChange = onLayoutChange;
    }

    public ResizableCard(int x, int y, int width, int height, String title) {
        super(x, y, width, height, title);
        this.initialWidth = width;
        this.expandedHeight = height;
        this.hoverAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);
        this.activeAnimation = new Animation(0, 0, Theme.ANIM_HOVER, Easing::easeOut);

        if (collapsed) {
            float scale = (float) width / initialWidth;
            this.height = (int) (TITLE_BAR_HEIGHT * scale);
        }
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed)
            return;
        this.collapsed = collapsed;
        if (collapsed) {
            this.expandedHeight = this.height;
            float scale = (float) width / initialWidth;
            this.height = (int) (TITLE_BAR_HEIGHT * scale);
        } else {
            updateLayout();
        }
    }

    public void setDragBounds(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        this.hovered = isMouseOver(mouseX, mouseY);

        if (collapsed) {
            for (Widget child : getChildren()) {
                child.updateHoverState(-1, -1);
            }
            return;
        }

        float scale = (float) width / initialWidth;
        int scaledMouseX = (int) ((mouseX - x) / scale + x);
        int scaledMouseY = (int) ((mouseY - y) / scale + y);

        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                child.updateHoverState(scaledMouseX, scaledMouseY);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float hover = hoverAnimation.getValue();
        float active = activeAnimation.getValue();
        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS, false);
        if (hover > 0 || active > 0) {
            RenderHelper.renderRoundedRect(graphics, x, y, width, height, Theme.BORDER_RADIUS,
                    Theme.withAlpha(0xFF000000, hover * 0.12f + active * 0.20f));
            RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS,
                    Theme.withAlpha(Theme.ACCENT, hover * 0.28f + active * 0.55f));
        }

        float scale = (float) width / initialWidth;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate((float) -x, (float) -y);

        if (getTitle() != null && !getTitle().isEmpty()) {
            graphics.fill(x, y, x + initialWidth, y + TITLE_BAR_HEIGHT,
                    Theme.withAlpha(0xFF000000, 0.18f + hover * 0.10f + active * 0.16f));

            int titleColor = Theme.lerpColor(Theme.TEXT_PRIMARY, Theme.ACCENT, Math.max(hover * 0.55f, active));
            String arrow = collapsed ? "◀" : "▼";
            int titleOffset = Math.round(hover * 2.0f + active * 2.0f);

            graphics.drawString(Minecraft.getInstance().font,
                    getTitle(), x + getPadding() + titleOffset, y + (TITLE_BAR_HEIGHT - 8) / 2, titleColor);

            int arrowWidth = Minecraft.getInstance().font.width(arrow);
            graphics.drawString(Minecraft.getInstance().font,
                    arrow, x + initialWidth - getPadding() - arrowWidth, y + (TITLE_BAR_HEIGHT - 8) / 2, titleColor);
        }

        if (!collapsed) {
            for (Widget child : getChildren()) {
                if (child.isVisible()) {
                    child.render(graphics, (int) ((mouseX - x) / scale + x),
                            (int) ((mouseY - y) / scale + y), partialTick);
                }
            }
        }

        graphics.pose().popMatrix();

        if (resizing || (!collapsed && isOverResizeHandle(mouseX, mouseY) != ResizeHandle.NONE)) {
            int handleColor = resizing ? Theme.ACCENT : Theme.withAlpha(Theme.ACCENT, 0.5f);
            graphics.fill(x + width - RESIZE_HANDLE_SIZE, y + height - RESIZE_HANDLE_SIZE,
                    x + width, y + height, handleColor);
        }
    }

    @Override
    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, int rawMouseX, int rawMouseY,
            float partialTick) {
        if (!visible || collapsed)
            return;

        float scale = (float) width / initialWidth;

        graphics.pose().pushMatrix();
        graphics.pose().translate((float) x, (float) y);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate((float) -x, (float) -y);

        int scaledMouseX = (int) ((mouseX - x) / scale + x);
        int scaledMouseY = (int) ((mouseY - y) / scale + y);
        int scaledRawMouseX = (int) ((rawMouseX - x) / scale + x);
        int scaledRawMouseY = (int) ((rawMouseY - y) / scale + y);

        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                child.renderOverlay(graphics, scaledMouseX, scaledMouseY, scaledRawMouseX, scaledRawMouseY, partialTick);
            }
        }

        graphics.pose().popMatrix();
    }

    @Override
    public void tick() {
        super.tick();
        updateAnimationTarget(hoverAnimation, hovered || dragging || resizing, false);
        updateAnimationTarget(activeAnimation, dragging || resizing, true);
        if (!collapsed) {
            updateLayout();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        if (button == 0) {
            if (!collapsed) {
                ResizeHandle handle = isOverResizeHandle((int) mouseX, (int) mouseY);
                if (handle != ResizeHandle.NONE) {
                    resizing = true;
                    activeHandle = handle;
                    dragStartX = (int) mouseX;
                    dragStartY = (int) mouseY;
                    dragStartWidth = width;
                    return true;
                }
            }

            if (isOverTitleBar((int) mouseX, (int) mouseY)) {
                if (mouseX >= x + width - getPadding() - 15) {
                    setCollapsed(!collapsed);
                    if (onLayoutChange != null)
                        onLayoutChange.run();
                    return true;
                }

                dragging = true;
                dragStartX = (int) mouseX;
                dragStartY = (int) mouseY;
                dragStartCardX = x;
                dragStartCardY = y;
                return true;
            }
        }

        if (button == 1) {
            if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
                setCollapsed(!collapsed);
                if (onLayoutChange != null)
                    onLayoutChange.run();
                return true;
            }
        }

        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        double scaledMouseX = (mouseX - x) / scale + x;
        double scaledMouseY = (mouseY - y) / scale + y;

        List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.isMouseOver(scaledMouseX, scaledMouseY)) {
                if (child.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                    return true;
                }
            }
        }

        for (Widget child : children) {
            if (child.mouseClicked(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }

        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (dragging) {
                dragging = false;
                if (onLayoutChange != null)
                    onLayoutChange.run();
                return true;
            }
            if (resizing) {
                resizing = false;
                activeHandle = ResizeHandle.NONE;
                if (onLayoutChange != null)
                    onLayoutChange.run();
                return true;
            }
        }

        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        double scaledMouseX = (mouseX - x) / scale + x;
        double scaledMouseY = (mouseY - y) / scale + y;

        List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.mouseReleased(scaledMouseX, scaledMouseY, button)) {
                return true;
            }
        }

        return super.mouseReleased(mouseX, mouseY, button);
    }

    public void pack() {
        float scale = (float) width / initialWidth;
        int logicalUsedHeight = getPadding();
        if (getTitle() != null && !getTitle().isEmpty()) {
            logicalUsedHeight += TITLE_BAR_HEIGHT;
        }

        int childrenHeight = 0;
        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                childrenHeight += child.getHeight() + Theme.SPACING_SMALL;
            }
        }
        if (childrenHeight > 0) {
            childrenHeight -= Theme.SPACING_SMALL;
        }

        int targetHeight = (int) ((logicalUsedHeight + childrenHeight + getPadding()) * scale);

        this.expandedHeight = targetHeight;
        if (!collapsed) {
            this.height = targetHeight;
        }
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int deltaX = (int) mouseX - dragStartX;
            int deltaY = (int) mouseY - dragStartY;

            int gridSize = 10;
            int targetX = dragStartCardX + deltaX;
            int targetY = dragStartCardY + deltaY;

            if (isShiftDown()) {
                targetX = Math.round((float) targetX / gridSize) * gridSize;
                targetY = Math.round((float) targetY / gridSize) * gridSize;
            }

            targetX = Math.max(minX, Math.min(maxX - width, targetX));
            targetY = Math.max(minY, Math.min(maxY - height, targetY));

            x = targetX;
            y = targetY;
            updateLayout();
            return true;
        }

        if (resizing) {
            int deltaX = (int) mouseX - dragStartX;

            if (activeHandle == ResizeHandle.BOTTOM_RIGHT || activeHandle == ResizeHandle.RIGHT) {
                int newWidth = dragStartWidth + deltaX;
                if (isShiftDown()) {
                    newWidth = Math.round((float) newWidth / 10) * 10;
                }
                width = Math.max(50, newWidth);
            }

            updateLayout();
            return true;
        }

        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        double scaledMouseX = (mouseX - x) / scale + x;
        double scaledMouseY = (mouseY - y) / scale + y;

        double scaledDragX = dragX / scale;
        double scaledDragY = dragY / scale;

        List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.mouseDragged(scaledMouseX, scaledMouseY, button, scaledDragX, scaledDragY)) {
                return true;
            }
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isOverTitleBar(int mouseX, int mouseY) {
        if (getTitle() == null || getTitle().isEmpty())
            return false;
        float scale = (float) width / initialWidth;
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + (int) (TITLE_BAR_HEIGHT * scale);
    }

    private ResizeHandle isOverResizeHandle(int mouseX, int mouseY) {
        if (mouseX >= x + width - RESIZE_HANDLE_SIZE && mouseX <= x + width &&
                mouseY >= y + height - RESIZE_HANDLE_SIZE && mouseY <= y + height) {
            return ResizeHandle.BOTTOM_RIGHT;
        }
        if (mouseX >= x + width - RESIZE_HANDLE_SIZE && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height) {
            return ResizeHandle.RIGHT;
        }
        return ResizeHandle.NONE;
    }

    public void updateLayout() {
        int logicalPadding = getPadding();
        int logicalTitleHeight = (getTitle() != null && !getTitle().isEmpty()) ? TITLE_BAR_HEIGHT : 0;
        int currentLogicalY = y + logicalPadding + logicalTitleHeight;
        int logicalContentWidth = initialWidth - logicalPadding * 2;

        for (Widget child : getChildren()) {
            child.setX(x + logicalPadding);
            child.setY(currentLogicalY);
            child.setWidth(logicalContentWidth);
            if (child.isVisible()) {
                currentLogicalY += child.getHeight() + Theme.SPACING_SMALL;
            }
        }
        pack();
    }

    @Override
    public int getContentX() {
        return x + (int) (getPadding() * ((float) width / initialWidth));
    }

    @Override
    public int getContentY() {
        float scale = (float) width / initialWidth;
        int logicalContentY = getPadding();
        if (getTitle() != null && !getTitle().isEmpty()) {
            logicalContentY += TITLE_BAR_HEIGHT;
        }
        return y + (int) (logicalContentY * scale);
    }

    @Override
    public int getContentHeight() {
        return height - (getContentY() - y) - (int) (getPadding() * ((float) width / initialWidth));
    }

    public boolean isDragging() {
        return dragging;
    }

    public int getInitialWidth() {
        return initialWidth;
    }

    public void setInitialWidth(int initialWidth) {
        this.initialWidth = initialWidth;
    }

    public boolean isResizing() {
        return resizing;
    }

    private boolean isShiftDown() {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) ||
                InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    private void updateAnimationTarget(Animation animation, boolean targetActive, boolean activeTrack) {
        float current = animation.getValue();
        float target = targetActive ? 1.0f : 0.0f;
        if ((targetActive && current < 1.0f) || (!targetActive && current > 0.0f)) {
            Animation next = new Animation(current, target, Theme.ANIM_HOVER, Easing::easeOut);
            next.start();
            if (activeTrack) {
                activeAnimation = next;
            } else {
                hoverAnimation = next;
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (collapsed)
            return false;

        float scale = (float) width / initialWidth;
        double scaledMouseX = (mouseX - x) / scale + x;
        double scaledMouseY = (mouseY - y) / scale + y;

        List<Widget> children = getChildren();
        for (Widget child : children) {
            if (child.mouseScrolled(scaledMouseX, scaledMouseY, scrollX, scrollY)) {
                return true;
            }
        }

        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public void setX(int x) {
        super.setX(x);
        updateLayout();
    }

    @Override
    public void setY(int y) {
        super.setY(y);
        updateLayout();
    }

    public int getExpandedHeight() {
        return expandedHeight;
    }

    public void setExpandedHeight(int expandedHeight) {
        this.expandedHeight = expandedHeight;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (collapsed)
            return false;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (collapsed)
            return false;
        return super.charTyped(character, modifiers);
    }
}
