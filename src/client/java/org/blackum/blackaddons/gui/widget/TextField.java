package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

import java.util.function.Predicate;

public class TextField extends Widget {
    private String text = "";
    private String placeholder = "";
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastCursorBlink = 0;
    private boolean cursorVisible = true;
    private Animation focusAnimation;
    private Animation hoverAnimation;
    private int maxLength = 32;
    private Predicate<Character> charFilter = c -> true;

    public TextField(int x, int y, int width, String placeholder) {
        this(x, y, width, Theme.TEXTFIELD_HEIGHT, placeholder);
    }

    public TextField(int x, int y, int width, int height, String placeholder) {
        super(x, y, width, height);
        this.placeholder = placeholder;
        this.focusAnimation = new Animation(0, 1, Theme.ANIM_FOCUS, Easing::easeOut);
        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        float focusProgress = focusAnimation.getValue();

        RenderHelper.renderSurface(graphics, x, y, width, height,
                Theme.BORDER_RADIUS_SMALL, true);

        if (focusProgress > 0 || hoverAnimation.getValue() > 0) {
            float total = Math.max(focusProgress, hoverAnimation.getValue() * 0.5f);
            int borderColor = Theme.withAlpha(Theme.ACCENT, total * 0.5f);
            RenderHelper.renderRoundedOutline(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, borderColor);
        }

        int textX = x + Theme.PADDING_SMALL;
        int textY = y + (height - 8) / 2;

        if (text.isEmpty() && !focused) {
            int placeholderColor = Theme.withAlpha(Theme.TEXT_SECONDARY, 0.6f);
            graphics.drawString(Minecraft.getInstance().font, placeholder, textX, textY, placeholderColor);
        } else {
            graphics.drawString(Minecraft.getInstance().font, text, textX, textY, Theme.TEXT_PRIMARY);

            if (focused && cursorVisible) {
                int cursorX = textX + Minecraft.getInstance().font.width(text.substring(0, cursorPosition));
                graphics.fill(cursorX, textY, cursorX + 1, textY + 8, Theme.TEXT_PRIMARY);
            }
        }
    }

    @Override
    public void tick() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCursorBlink > 500) {
            cursorVisible = !cursorVisible;
            lastCursorBlink = currentTime;
        }

        if (focused && !focusAnimation.isRunning() && focusAnimation.getProgress() < 1) {
            focusAnimation = new Animation(focusAnimation.getValue(), 1, Theme.ANIM_FOCUS, Easing::easeOut);
            focusAnimation.start();
        } else if (!focused && !focusAnimation.isRunning() && focusAnimation.getProgress() > 0) {
            focusAnimation = new Animation(focusAnimation.getValue(), 0, Theme.ANIM_FOCUS, Easing::easeOut);
            focusAnimation.start();
        }

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
        return isMouseOver(mouseX, mouseY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused || !enabled)
            return false;

        if (keyCode == 259 && cursorPosition > 0) {
            text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
            cursorPosition--;
            return true;
        }

        if (keyCode == 261 && cursorPosition < text.length()) {
            text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            return true;
        }

        if (keyCode == 263 && cursorPosition > 0) {
            cursorPosition--;
            return true;
        }
        if (keyCode == 262 && cursorPosition < text.length()) {
            cursorPosition++;
            return true;
        }

        if (keyCode == 268) {
            cursorPosition = 0;
            return true;
        }
        if (keyCode == 269) {
            cursorPosition = text.length();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!focused || !enabled)
            return false;

        if (text.length() < maxLength && character >= 32 && charFilter.test(character)) {
            text = text.substring(0, cursorPosition) + character + text.substring(cursorPosition);
            cursorPosition++;
            return true;
        }

        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) {
            cursorVisible = true;
            lastCursorBlink = System.currentTimeMillis();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        this.cursorPosition = Math.min(cursorPosition, text.length());
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public void setCharFilter(Predicate<Character> charFilter) {
        this.charFilter = charFilter;
    }
}
