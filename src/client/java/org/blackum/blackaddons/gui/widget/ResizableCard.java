package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

public class ResizableCard extends Card {

    private static final int MIN_WIDTH = 200;
    private static final int RESIZE_HANDLE_SIZE = 12;
    private static final int TITLE_BAR_HEIGHT = 24;

    private boolean dragging = false;
    private boolean resizing = false;
    private int dragStartX = 0;
    private int dragStartY = 0;
    private int dragStartCardX = 0;
    private int dragStartCardY = 0;
    private int dragStartWidth = 0;
    private int dragStartHeight = 0;
    private ResizeHandle activeHandle = ResizeHandle.NONE;

    private int initialWidth;
    private int initialHeight;
    private boolean collapsed = true;
    private int expandedHeight;

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
        this.initialHeight = height;
        this.initialWidth = width;
        this.initialHeight = height;
        this.expandedHeight = height;

        if (collapsed) {
            this.height = TITLE_BAR_HEIGHT;
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
            this.height = TITLE_BAR_HEIGHT;
        } else {
            this.height = this.expandedHeight;
        }
    }

    @Override
    public void updateHoverState(int mouseX, int mouseY) {
        super.updateHoverState(mouseX, mouseY);

        float scale = Math.min(1.0f, (float) width / MIN_WIDTH);
        int contentX = getContentX();
        int contentY = getContentY();
        int scaledMouseX = (int) ((mouseX - contentX) / scale + contentX);
        int scaledMouseY = (int) ((mouseY - contentY) / scale + contentY);

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

        int shadowOffset = dragging ? 8 : 4;
        int shadowColor = Theme.withAlpha(Theme.SHADOW, dragging ? 0.4f : 0.2f);
        graphics.fill(x + shadowOffset, y + shadowOffset, x + width + shadowOffset, y + height + shadowOffset,
                shadowColor);

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS, false);

        if (getTitle() != null && !getTitle().isEmpty()) {
            graphics.fill(x, y, x + width, y + TITLE_BAR_HEIGHT, Theme.withAlpha(Theme.SURFACE_LIGHT, 0.5f));

            int titleColor = dragging ? Theme.ACCENT : Theme.TEXT_PRIMARY;
            String arrow = collapsed ? "◀" : "▼";

            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    getTitle(), x + getPadding(), y + (TITLE_BAR_HEIGHT - 8) / 2, titleColor);

            int arrowWidth = net.minecraft.client.Minecraft.getInstance().font.width(arrow);
            graphics.drawString(net.minecraft.client.Minecraft.getInstance().font,
                    arrow, x + width - getPadding() - arrowWidth, y + (TITLE_BAR_HEIGHT - 8) / 2, titleColor);
        }

        if (collapsed)
            return;

        float scale = Math.min(1.0f, (float) width / MIN_WIDTH);

        graphics.pose().pushMatrix();

        int contentX = getContentX();
        int contentY = getContentY();
        graphics.pose().translate((float) contentX, (float) contentY);
        graphics.pose().scale(scale, scale);
        graphics.pose().translate((float) -contentX, (float) -contentY);

        for (Widget child : getChildren()) {
            if (child.isVisible()) {
                child.render(graphics, (int) ((mouseX - contentX) / scale + contentX),
                        (int) ((mouseY - contentY) / scale + contentY), partialTick);
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
                    dragStartHeight = height;
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

        float scale = Math.min(1.0f, (float) width / MIN_WIDTH);
        int contentX = getContentX();
        int contentY = getContentY();
        double scaledMouseX = (mouseX - contentX) / scale + contentX;
        double scaledMouseY = (mouseY - contentY) / scale + contentY;

        for (Widget child : getChildren()) {
            if (child.mouseClicked(scaledMouseX, scaledMouseY, button)) {
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

        return false;
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
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (dragging) {
            int deltaX = (int) mouseX - dragStartX;
            int deltaY = (int) mouseY - dragStartY;
            x = dragStartCardX + deltaX;
            y = dragStartCardY + deltaY;
            updateChildPositions();
            return true;
        }

        if (resizing) {
            int deltaX = (int) mouseX - dragStartX;
            int deltaY = (int) mouseY - dragStartY;

            if (activeHandle == ResizeHandle.BOTTOM_RIGHT || activeHandle == ResizeHandle.RIGHT) {
                width = Math.max(initialWidth, dragStartWidth + deltaX);
            }
            if (activeHandle == ResizeHandle.BOTTOM_RIGHT || activeHandle == ResizeHandle.BOTTOM) {
                if (!collapsed) {
                    height = Math.max(initialHeight, dragStartHeight + deltaY);
                    expandedHeight = height;
                }
            }

            updateChildPositions();
            return true;
        }

        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    private boolean isOverTitleBar(int mouseX, int mouseY) {
        if (getTitle() == null || getTitle().isEmpty())
            return false;
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + TITLE_BAR_HEIGHT;
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
        if (mouseY >= y + height - RESIZE_HANDLE_SIZE && mouseY <= y + height &&
                mouseX >= x && mouseX <= x + width) {
            return ResizeHandle.BOTTOM;
        }
        return ResizeHandle.NONE;
    }

    private void updateChildPositions() {
        int contentX = getContentX();
        int contentY = getContentY();
        int contentWidth = getContentWidth();
        int currentY = contentY;

        for (Widget child : getChildren()) {
            child.setX(contentX);
            child.setY(currentY);
            child.setWidth(contentWidth);
            if (child.isVisible()) {
                currentY += child.getHeight() + Theme.SPACING_SMALL;
            }
        }
    }

    @Override
    public int getContentY() {
        int contentY = y + getPadding();
        if (getTitle() != null && !getTitle().isEmpty()) {
            contentY += TITLE_BAR_HEIGHT;
        }
        return contentY;
    }

    @Override
    public int getContentHeight() {
        int usedHeight = getPadding();
        if (getTitle() != null && !getTitle().isEmpty()) {
            usedHeight += TITLE_BAR_HEIGHT;
        }
        return height - usedHeight - getPadding();
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean isResizing() {
        return resizing;
    }
}
