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
    private boolean dragging = false;
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
            if (hasSelection()) {
                int start = Math.min(selectionStart, selectionEnd);
                int end = Math.max(selectionStart, selectionEnd);
                int selStartX = textX + Minecraft.getInstance().font.width(text.substring(0, start));
                int selEndX = textX + Minecraft.getInstance().font.width(text.substring(0, end));
                graphics.fill(selStartX, textY - 1, selEndX, textY + 9, Theme.withAlpha(Theme.ACCENT, 0.4f));
            }

            graphics.drawString(Minecraft.getInstance().font, text, textX, textY, Theme.TEXT_PRIMARY);

            if (focused && cursorVisible && !hasSelection()) {
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

        if (isMouseOver(mouseX, mouseY)) {
            focused = true;
            if (button == 0) {
                int relativeX = (int) mouseX - (x + Theme.PADDING_SMALL);
                cursorPosition = getCursorPositionFromX(relativeX);
                clearSelection();
                dragging = true;
            }
            return true;
        } else {
            focused = false;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!focused || !enabled || !dragging)
            return false;

        int relativeX = (int) mouseX - (x + Theme.PADDING_SMALL);
        int newPosition = getCursorPositionFromX(relativeX);

        if (selectionStart == -1) {
            selectionStart = cursorPosition;
        }

        cursorPosition = newPosition;
        selectionEnd = newPosition;

        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused || !enabled)
            return false;

        boolean isCtrlPressed = (modifiers & 2) != 0;

        // Ctrl+A - Select All
        if (isCtrlPressed && keyCode == 65) {
            selectAll();
            return true;
        }

        // Ctrl+C - Copy
        if (isCtrlPressed && keyCode == 67) {
            if (hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedText());
            }
            return true;
        }

        // Ctrl+V - Paste
        if (isCtrlPressed && keyCode == 86) {
            String clipboard = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clipboard != null && !clipboard.isEmpty()) {
                if (hasSelection()) {
                    deleteSelection();
                }
                StringBuilder filtered = new StringBuilder();
                for (char c : clipboard.toCharArray()) {
                    if (c >= 32 && charFilter.test(c) && text.length() + filtered.length() < maxLength) {
                        filtered.append(c);
                    }
                }
                if (filtered.length() > 0) {
                    text = text.substring(0, cursorPosition) + filtered + text.substring(cursorPosition);
                    cursorPosition += filtered.length();
                }
            }
            return true;
        }

        // Ctrl+X - Cut
        if (isCtrlPressed && keyCode == 88) {
            if (hasSelection()) {
                Minecraft.getInstance().keyboardHandler.setClipboard(getSelectedText());
                deleteSelection();
            }
            return true;
        }

        // Backspace
        if (keyCode == 259) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPosition > 0) {
                text = text.substring(0, cursorPosition - 1) + text.substring(cursorPosition);
                cursorPosition--;
            }
            return true;
        }

        // Delete
        if (keyCode == 261) {
            if (hasSelection()) {
                deleteSelection();
            } else if (cursorPosition < text.length()) {
                text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
            }
            return true;
        }

        // Arrow keys
        if (keyCode == 263 && cursorPosition > 0) { // Left
            cursorPosition--;
            clearSelection();
            return true;
        }
        if (keyCode == 262 && cursorPosition < text.length()) { // Right
            cursorPosition++;
            clearSelection();
            return true;
        }

        // Home ??? idk maybe someone uses it
        if (keyCode == 268) {
            cursorPosition = 0;
            clearSelection();
            return true;
        }

        // End ??? idk too, like who is even using it?
        if (keyCode == 269) {
            cursorPosition = text.length();
            clearSelection();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char character, int modifiers) {
        if (!focused || !enabled)
            return false;

        if (character >= 32 && charFilter.test(character)) {
            if (hasSelection()) {
                deleteSelection();
            }
            if (text.length() < maxLength) {
                text = text.substring(0, cursorPosition) + character + text.substring(cursorPosition);
                cursorPosition++;
                return true;
            }
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

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private String getSelectedText() {
        if (!hasSelection())
            return "";
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        return text.substring(start, end);
    }

    private void deleteSelection() {
        if (!hasSelection())
            return;
        int start = Math.min(selectionStart, selectionEnd);
        int end = Math.max(selectionStart, selectionEnd);
        text = text.substring(0, start) + text.substring(end);
        cursorPosition = start;
        clearSelection();
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private void selectAll() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
    }

    private int getCursorPositionFromX(int relativeX) {
        if (text.isEmpty())
            return 0;

        int lastWidth = 0;

        for (int i = 0; i <= text.length(); i++) {
            int width = Minecraft.getInstance().font.width(text.substring(0, i));
            if (relativeX < width) {
                if (i > 0 && relativeX - lastWidth < width - relativeX) {
                    return i - 1;
                }
                return i;
            }
            lastWidth = width;
        }

        return text.length();
    }
}
