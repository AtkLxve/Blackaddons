package org.blackum.blackaddons.gui.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("all")
public abstract class BaseScreen extends Screen {
    protected final List<Widget> widgets = new ArrayList<>();
    private Widget focusedWidget = null;

    public static boolean showHitboxes = false;
    public static boolean showDebugOverlay = false;
    public static int overlayX = 5;
    public static int overlayY = 5;
    public static float overlayScale = 1.0f;

    protected boolean isMovingOverlay = false;

    protected int gridStartX;
    protected int gridStartY;
    protected int gridWidth;
    protected int gridColumns;
    protected int gridRowHeight;
    protected int gridGap;
    protected int currentGridColumn = 0;
    protected int currentGridRow = 0;

    protected int containerX;
    protected int containerY;
    protected int containerWidth;
    protected int containerHeight;

    protected double scrollOffset = 0;
    protected int contentHeight = 0;
    protected double maxScroll = 0;
    protected boolean canScroll = false;

    protected BaseScreen(Component title) {
        super(title);
    }

    protected void initGrid(int x, int y, int width, int columns, int rowHeight, int gap) {
        this.gridStartX = x;
        this.gridStartY = y;
        this.gridWidth = width;
        this.gridColumns = columns;
        this.gridRowHeight = rowHeight;
        this.gridGap = gap;
        this.currentGridColumn = 0;
        this.currentGridRow = 0;
    }

    protected <T extends Widget> T addToGrid(T widget, int colSpan) {
        if (currentGridColumn + colSpan > gridColumns) {
            currentGridColumn = 0;
            currentGridRow++;
        }

        int cellWidth = (gridWidth - (gridColumns - 1) * gridGap) / gridColumns;
        int widgetWidth = cellWidth * colSpan + (colSpan - 1) * gridGap;

        int widgetX = gridStartX + currentGridColumn * (cellWidth + gridGap);
        int widgetY = gridStartY + currentGridRow * (gridRowHeight + gridGap);

        widget.setX(widgetX);
        widget.setY(widgetY);
        widget.setWidth(widgetWidth);

        widgets.add(widget);

        currentGridColumn += colSpan;
        if (currentGridColumn >= gridColumns) {
            currentGridColumn = 0;
            currentGridRow++;
        }

        return widget;
    }

    @Override
    protected void init() {
        super.init();

        this.containerWidth = (int) (this.width * 0.8);
        this.containerHeight = (int) (this.height * 0.8);
        this.containerX = (this.width - this.containerWidth) / 2;
        this.containerY = (this.height - this.containerHeight) / 2;

        widgets.clear();
        isMovingOverlay = false;
        initWidgets();

        int maxWidgetY = 0;
        for (Widget w : widgets) {
            int relativeBottom = (w.getY() + w.getHeight()) - this.containerY;
            if (relativeBottom > maxWidgetY) {
                maxWidgetY = relativeBottom;
            }
        }
        this.contentHeight = Math.max(this.contentHeight, maxWidgetY + 20);
    }

    protected abstract void initWidgets();

    protected <T extends Widget> T addWidget(T widget) {
        widgets.add(widget);
        return widget;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        double scaledWidth = this.width;
        double scaledHeight = this.height;

        int finalMouseX = (int) (mc.mouseHandler.xpos() * (scaledWidth / windowWidth));
        int finalMouseY = (int) (mc.mouseHandler.ypos() * (scaledHeight / windowHeight));

        super.render(graphics, mouseX, mouseY, partialTick);

        org.blackum.blackaddons.gui.util.RenderHelper.renderSurface(
                graphics, containerX, containerY, containerWidth, containerHeight,
                Theme.BORDER_RADIUS_LARGE, false);

        maxScroll = Math.max(0, contentHeight - (containerHeight - 40));
        canScroll = maxScroll > 0;

        if (scrollOffset < 0)
            scrollOffset = 0;
        if (scrollOffset > maxScroll)
            scrollOffset = maxScroll;

        graphics.enableScissor(containerX, containerY, containerX + containerWidth, containerY + containerHeight);

        graphics.pose().pushMatrix();
        graphics.pose().translate(0f, (float) -scrollOffset);

        renderScrolledContent(graphics, mouseX, (int) (mouseY + scrollOffset), partialTick);

        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.updateHoverState(mouseX, (int) (mouseY + scrollOffset));
                widget.render(graphics, finalMouseX, finalMouseY, partialTick);

                if (showHitboxes) {
                    graphics.fill(widget.getX(), widget.getY(), widget.getX() + widget.getWidth(), widget.getY() + 1,
                            0xFFFF0000); // Top
                    graphics.fill(widget.getX(), widget.getY() + widget.getHeight() - 1,
                            widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight(), 0xFFFF0000); // Bottom
                    graphics.fill(widget.getX(), widget.getY(), widget.getX() + 1, widget.getY() + widget.getHeight(),
                            0xFFFF0000); // Left
                    graphics.fill(widget.getX() + widget.getWidth() - 1, widget.getY(),
                            widget.getX() + widget.getWidth(), widget.getY() + widget.getHeight(), 0xFFFF0000); // Right
                }
            }
        }

        graphics.pose().popMatrix();
        graphics.disableScissor();

        if (canScroll) {
            int scrollBarHeight = (int) ((containerHeight / (double) contentHeight) * containerHeight);
            if (scrollBarHeight < 30)
                scrollBarHeight = 30;

            double progress = scrollOffset / maxScroll;
            int scrollBarY = (int) (containerY + (progress * (containerHeight - scrollBarHeight)));
            int scrollBarX = containerX + containerWidth - 6;

            // Track
            graphics.fill(scrollBarX, containerY, scrollBarX + 4, containerY + containerHeight, 0x80000000);

            // Thumb
            graphics.fill(scrollBarX, scrollBarY, scrollBarX + 4, scrollBarY + scrollBarHeight, 0xFFFFFFFF);
        }
    }

    protected void renderScrolledContent(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public void tick() {
        super.tick();
        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.tick();
            }
        }
    }

    private boolean isDraggingScrollbar = false;

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        double scaledWidth = this.width;
        double scaledHeight = this.height;

        double mouseX = mc.mouseHandler.xpos() * (scaledWidth / windowWidth);
        double rawMouseY = (mc.mouseHandler.ypos() * (scaledHeight / windowHeight));
        double mouseY = rawMouseY + scrollOffset;
        int button = event.button();

        if (mc.player != null) {
            String msg = String.format("§e[Click] Scaled: %.1f,%.1f (Raw: %.1f,%.1f)", mouseX, mouseY,
                    mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false);
        }

        if (canScroll) {
            int scrollBarX = containerX + containerWidth - 6;
            if (mouseX >= scrollBarX && mouseX <= scrollBarX + 4 &&
                    rawMouseY >= containerY && rawMouseY <= containerY + containerHeight) {
                isDraggingScrollbar = true;
                return true;
            }
        }

        boolean insideContainer = mouseX >= containerX && mouseX <= containerX + containerWidth &&
                rawMouseY >= containerY && rawMouseY <= containerY + containerHeight;

        if (insideContainer) {
            for (int i = widgets.size() - 1; i >= 0; i--) {
                Widget widget = widgets.get(i);
                if (widget.isVisible() && widget.isEnabled() && widget.isMouseOver(mouseX, mouseY)) {
                    if (widget.mouseClicked(mouseX, mouseY, button)) {
                        setFocusedWidget(widget);
                        return true;
                    }
                }
            }
        }

        setFocusedWidget(null);
        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        isDraggingScrollbar = false;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
        double rawMouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
        double mouseY = rawMouseY + scrollOffset;
        int button = event.button();

        for (Widget widget : widgets) {
            if (widget.isVisible() && widget.isEnabled()) {
                widget.mouseReleased(mouseX, mouseY, button);
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
        double rawMouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
        double mouseY = rawMouseY + scrollOffset;
        int button = event.button();

        if (isDraggingScrollbar && canScroll) {
            int scrollBarHeight = (int) ((containerHeight / (double) contentHeight) * containerHeight);
            if (scrollBarHeight < 30)
                scrollBarHeight = 30;

            double trackHeight = containerHeight - scrollBarHeight;
            double movement = dragY * ((double) maxScroll / trackHeight);

            scrollOffset += movement;
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > maxScroll)
                scrollOffset = maxScroll;
            return true;
        }

        if (getFocusedWidget() != null && getFocusedWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (canScroll) {
            scrollOffset -= scrollY * 20; // Scroll speed
            if (scrollOffset < 0)
                scrollOffset = 0;
            if (scrollOffset > maxScroll)
                scrollOffset = maxScroll;
            return true;
        }

        for (Widget widget : widgets) {
            if (widget.isVisible() && widget.isEnabled() && widget.isMouseOver(mouseX, mouseY)) {
                if (widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();

        if (getFocusedWidget() != null && getFocusedWidget().isVisible() && getFocusedWidget().isEnabled()) {
            try {
                int key = 0, scancode = 0, modifiers = 0;
                try {
                    java.lang.reflect.Field f = event.getClass().getDeclaredField("key");
                    f.setAccessible(true);
                    key = f.getInt(event);
                } catch (NoSuchFieldException e1) {
                    try {
                        java.lang.reflect.Field f = event.getClass().getDeclaredField("keyCode");
                        f.setAccessible(true);
                        key = f.getInt(event);
                    } catch (Exception e2) {
                    }
                }

                try {
                    java.lang.reflect.Field f = event.getClass().getDeclaredField("scancode");
                    f.setAccessible(true);
                    scancode = f.getInt(event);
                } catch (Exception e) {
                }

                try {
                    java.lang.reflect.Field f = event.getClass().getDeclaredField("modifiers");
                    f.setAccessible(true);
                    modifiers = f.getInt(event);
                } catch (Exception e) {
                }

                if (getFocusedWidget().keyPressed(key, scancode, modifiers)) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        if (getFocusedWidget() != null && getFocusedWidget().isVisible() && getFocusedWidget().isEnabled()) {
            try {
                char character = 0;
                int modifiers = 0;

                try {
                    java.lang.reflect.Field f = event.getClass().getDeclaredField("character");
                    f.setAccessible(true);
                    character = f.getChar(event);
                } catch (NoSuchFieldException e1) {
                    try {
                        java.lang.reflect.Field f = event.getClass().getDeclaredField("codepoint");
                        f.setAccessible(true);
                        character = (char) f.getInt(event);
                    } catch (Exception e2) {
                    }
                }

                try {
                    java.lang.reflect.Field f = event.getClass().getDeclaredField("modifiers");
                    f.setAccessible(true);
                    modifiers = f.getInt(event);
                } catch (Exception e) {
                }

                if (getFocusedWidget().charTyped(character, modifiers)) {
                    return true;
                }
            } catch (Exception e) {
            }
        }
        return super.charTyped(event);
    }

    protected void setFocusedWidget(Widget widget) {
        if (focusedWidget == widget)
            return;
        if (focusedWidget != null) {
            focusedWidget.setFocused(false);
        }
        focusedWidget = widget;
        if (focusedWidget != null) {
            focusedWidget.setFocused(true);
        }
    }

    protected Widget getFocusedWidget() {
        return focusedWidget;
    }
}
