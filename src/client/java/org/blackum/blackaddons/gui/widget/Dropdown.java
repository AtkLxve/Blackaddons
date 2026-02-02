package org.blackum.blackaddons.gui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import org.blackum.blackaddons.gui.animation.Animation;
import org.blackum.blackaddons.gui.animation.Easing;
import org.blackum.blackaddons.gui.theme.Theme;
import org.blackum.blackaddons.gui.util.RenderHelper;

import java.util.List;
import java.util.function.Consumer;

public class Dropdown extends Widget {

    private String label;
    private List<String> options;
    private int selectedIndex = -1;
    private boolean expanded = false;
    private Consumer<String> onSelect;

    private Animation hoverAnimation;
    private Animation expandAnimation;

    public Dropdown(int x, int y, int width, String label, List<String> options, Consumer<String> onSelect) {
        this(x, y, width, Theme.BUTTON_HEIGHT, label, options, onSelect);
    }

    public Dropdown(int x, int y, int width, int height, String label, List<String> options,
            Consumer<String> onSelect) {
        super(x, y, width, height);
        this.label = label;
        this.options = options;
        this.onSelect = onSelect;

        this.hoverAnimation = new Animation(0, 1, Theme.ANIM_HOVER, Easing::easeOut);
        this.expandAnimation = new Animation(0, 1, Theme.ANIM_CLICK, Easing::easeOutBack);
    }

    public int getSelectedIndex() {
        return selectedIndex;
    }

    public void setSelectedIndex(int selectedIndex) {
        if (selectedIndex < -1 || selectedIndex >= options.size()) {
            return;
        }
        this.selectedIndex = selectedIndex;
    }

    public void setSelectedOption(String option) {
        if (option == null)
            return;
        for (int i = 0; i < options.size(); i++) {
            if (option.equalsIgnoreCase(options.get(i))) {
                setSelectedIndex(i);
                return;
            }
        }
    }

    public boolean isExpanded() {
        return expanded;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        RenderHelper.renderSurface(graphics, x, y, width, height, Theme.BORDER_RADIUS_SMALL, false);

        int textColor = enabled ? Theme.TEXT_PRIMARY : Theme.TEXT_SECONDARY;
        String text = selectedIndex >= 0 && selectedIndex < options.size() ? options.get(selectedIndex) : label;

        if (Minecraft.getInstance().font.width(text) > width - 20) {
            text = Minecraft.getInstance().font.plainSubstrByWidth(text, width - 25) + "...";
        }

        int textY = y + (height - 8) / 2;
        graphics.drawString(Minecraft.getInstance().font, text, x + 10, textY, textColor);

        String arrow = expanded ? "▲" : "▼";
        int arrowWidth = Minecraft.getInstance().font.width(arrow);
        graphics.drawString(Minecraft.getInstance().font, arrow, x + width - 15 - arrowWidth / 2, textY,
                Theme.TEXT_SECONDARY);

        if (expanded) {
            int optionHeight = 25;
            int totalHeight = options.size() * optionHeight;
            int menuY = y + height + 2;

            graphics.fill(x - 3, menuY - 3, x + width + 3, menuY + totalHeight + 3, 0xFF000000);
            RenderHelper.renderSurface(graphics, x, menuY, width, totalHeight, Theme.BORDER_RADIUS_SMALL, false);
            RenderHelper.renderRoundedOutline(graphics, x, menuY, width, totalHeight, Theme.BORDER_RADIUS_SMALL,
                    Theme.withAlpha(Theme.BORDER, 0.5f));

            for (int i = 0; i < options.size(); i++) {
                String option = options.get(i);
                int optY = menuY + (i * optionHeight);

                boolean isOptHovered = mouseX >= x && mouseX <= x + width && mouseY >= optY
                        && mouseY < optY + optionHeight;

                if (isOptHovered) {
                    graphics.fill(x + 2, optY, x + width - 2, optY + optionHeight,
                            Theme.withAlpha(Theme.GLASS_HIGHLIGHT, 0.2f));
                }

                if (i == selectedIndex) {
                    graphics.drawString(Minecraft.getInstance().font, "§b" + option, x + 10,
                            optY + (optionHeight - 8) / 2, Theme.ACCENT);
                } else {
                    graphics.drawString(Minecraft.getInstance().font, option, x + 10, optY + (optionHeight - 8) / 2,
                            Theme.TEXT_PRIMARY);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!enabled || !visible)
            return false;

        if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height) {
            expanded = !expanded;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return true;
        }

        if (expanded) {
            int optionHeight = 25;
            int menuY = y + height + 2;
            int totalHeight = options.size() * optionHeight;

            if (mouseX >= x && mouseX <= x + width && mouseY >= menuY && mouseY <= menuY + totalHeight) {
                int clickedIndex = (int) ((mouseY - menuY) / optionHeight);
                if (clickedIndex >= 0 && clickedIndex < options.size()) {
                    selectedIndex = clickedIndex;
                    if (onSelect != null) {
                        onSelect.accept(options.get(clickedIndex));
                    }
                    expanded = false;
                    Minecraft.getInstance().getSoundManager()
                            .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    return true;
                }
            }
        }

        if (expanded) {
            expanded = false;
        }

        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        if (!visible)
            return false;
        boolean mainOver = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        if (mainOver)
            return true;

        if (expanded) {
            int optionHeight = 25;
            int menuY = y + height + 2;
            int menuHeight = options.size() * optionHeight;
            return mouseX >= x && mouseX <= x + width && mouseY >= menuY && mouseY <= menuY + menuHeight;
        }
        return false;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            expanded = false;
        }
    }

    @Override
    public void tick() {
        if (isMouseOver(
                Minecraft.getInstance().mouseHandler.xpos()
                        * ((double) Minecraft.getInstance().getWindow().getGuiScaledWidth()
                                / Minecraft.getInstance().getWindow().getScreenWidth()),
                Minecraft.getInstance().mouseHandler.ypos()
                        * ((double) Minecraft.getInstance().getWindow().getGuiScaledHeight()
                                / Minecraft.getInstance().getWindow().getScreenHeight()))) {
            if (hoverAnimation.getValue() < 1) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 1, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        } else {
            if (hoverAnimation.getValue() > 0) {
                hoverAnimation = new Animation(hoverAnimation.getValue(), 0, Theme.ANIM_HOVER, Easing::easeOut);
                hoverAnimation.start();
            }
        }
    }
}