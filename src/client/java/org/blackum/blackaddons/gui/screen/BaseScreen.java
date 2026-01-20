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

    protected int containerX;
    protected int containerY;
    protected int containerWidth;
    protected int containerHeight;

    protected BaseScreen(Component title) {
        super(title);
    }

    @Override
    protected void init() {
        super.init();

        this.containerWidth = (int) (this.width * 0.8);
        this.containerHeight = (int) (this.height * 0.8);
        this.containerX = (this.width - this.containerWidth) / 2;
        this.containerY = (this.height - this.containerHeight) / 2;

        widgets.clear();
        initWidgets();
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
        super.render(graphics, mouseX, mouseY, partialTick);

        org.blackum.blackaddons.gui.util.RenderHelper.renderSurface(
                graphics, containerX, containerY, containerWidth, containerHeight,
                Theme.BORDER_RADIUS_LARGE, false);

        for (Widget widget : widgets) {
            if (widget.isVisible()) {
                widget.updateHoverState(mouseX, mouseY);
                widget.render(graphics, mouseX, mouseY, partialTick);
            }
        }

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

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean pressed) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double windowWidth = mc.getWindow().getScreenWidth();
        double windowHeight = mc.getWindow().getScreenHeight();
        double scaledWidth = this.width;
        double scaledHeight = this.height;

        double mouseX = mc.mouseHandler.xpos() * (scaledWidth / windowWidth);
        double mouseY = mc.mouseHandler.ypos() * (scaledHeight / windowHeight);
        int button = event.button();

        if (mc.player != null) {
            String msg = String.format("§e[Click] Scaled: %.1f,%.1f (Raw: %.1f,%.1f)", mouseX, mouseY,
                    mc.mouseHandler.xpos(), mc.mouseHandler.ypos());
            mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(msg), false);
        }

        for (Widget widget : widgets) {
            if (widget.isVisible() && widget.isEnabled() && widget.isMouseOver(mouseX, mouseY)) {
                if (widget.mouseClicked(mouseX, mouseY, button)) {
                    setFocusedWidget(widget);
                    return true;
                }
            }
        }

        setFocusedWidget(null);
        return super.mouseClicked(event, pressed);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        double mouseX = mc.mouseHandler.xpos() * ((double) this.width / mc.getWindow().getScreenWidth());
        double mouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
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
        double mouseY = mc.mouseHandler.ypos() * ((double) this.height / mc.getWindow().getScreenHeight());
        int button = event.button();

        if (getFocusedWidget() != null && getFocusedWidget().mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {

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

                if (mc.player != null) {
                    mc.player.displayClientMessage(net.minecraft.network.chat.Component
                            .literal("§e[Key] Key: " + key + " Scan: " + scancode + " Mod: " + modifiers), false);
                }

                if (getFocusedWidget().keyPressed(key, scancode, modifiers)) {
                    return true;
                }
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c[Key] Error: " + e.getMessage()), false);
                }
            }
        } else {
            if (mc.player != null) {
                mc.player.displayClientMessage(
                        net.minecraft.network.chat.Component.literal("§7[Key] No focused widget"), false);
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

                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal(
                                    "§e[Char] Char: " + character + " (" + (int) character + ") Mod: " + modifiers),
                            false);
                }

                if (getFocusedWidget().charTyped(character, modifiers)) {
                    return true;
                }
            } catch (Exception e) {
                if (mc.player != null) {
                    mc.player.displayClientMessage(
                            net.minecraft.network.chat.Component.literal("§c[Char] Error: " + e.getMessage()), false);
                }
            }
        }
        return super.charTyped(event);
    }

    protected void setFocusedWidget(Widget widget) {
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
